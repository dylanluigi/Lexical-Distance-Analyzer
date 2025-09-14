package model;

import model.algorithm.distance.DistanceAlgorithm;
import model.algorithm.distance.DistanceMatrix;
import model.dictionary.FileLoader;
import model.dictionary.Language;
import model.visualization.GraphData;
import model.visualization.TreeData;
import notification.NotificationService;
import notification.NotificationService.Event;
import notification.NotificationService.EventType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Implementació de la capa de model per a l'aplicació LexDistance.
 * 
 * Aquesta classe gestiona la càrrega de diccionaris, el càlcul de matrius de distància
 * entre llengües i la generació de dades per a visualitzacions. Utilitza un pool de fils
 * per optimitzar els càlculs computacionalment intensius.
 * 
 * @author Equip ProjecteDiccionaris
 * @version 1.0
 */
public class LexModel {
    private final NotificationService notificationService;
    private final ExecutorService executorService;
    private final FileLoader fileLoader;
    
    private Map<String, Language> languages;
    private DistanceMatrix distanceMatrix;
    
    private final List<ModelObserver> observers = new ArrayList<>();
    
    /**
     * Constructor que inicialitza el model amb un servei de notificacions.
     * 
     * Configura un pool de fils optimitzat per al processament en paral·lel dels càlculs
     * de distància, ajustant automàticament la mida segons els processadors disponibles.
     * 
     * @param notificationService servei per publicar esdeveniments del model
     */
    public LexModel(NotificationService notificationService) {
        this.notificationService = notificationService;
        

        int corePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        int maxPoolSize = Math.max(4, Runtime.getRuntime().availableProcessors());
        long keepAliveTime = 60L;

        this.executorService = new java.util.concurrent.ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            keepAliveTime,
            java.util.concurrent.TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(),
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        this.fileLoader = new FileLoader();
    }
    
    /**
     * Carrega diccionaris des de fitxers.
     * 
     * Processa els fitxers de diccionari proporcionats, detecta automàticament els codis
     * de llengua a partir dels noms dels fitxers i carrega el vocabulari de cada llengua.
     * Requereix un mínim de dos diccionaris per poder realitzar comparacions.
     * 
     * @param dicFiles array de rutes dels fitxers de diccionari
     * @return resum del corpus carregat amb informació sobre les llengües i mots
     * @throws Exception si hi ha un error carregant els diccionaris
     * @throws IllegalArgumentException si es proporcionen menys de dos diccionaris
     */
    public CorpusSummary loadDictionaries(Path[] dicFiles) throws Exception {
        if (dicFiles.length < 2) {
            throw new IllegalArgumentException("At least two dictionary files are required");
        }

        Map<Path, FileLoader.LanguageInfo> fileInfoMap = new HashMap<>();
        for (Path file : dicFiles) {
            fileInfoMap.put(file, fileLoader.detectLanguageFromFileName(file));
        }
        
        languages = fileLoader.loadDictionaries(fileInfoMap);

        CorpusSummary summary = new CorpusSummary(new ArrayList<>(languages.keySet()), 
            languages.values().stream().mapToInt(Language::getVocabularySize).sum());
        
        notificationService.publish(new CorpusLoadedEvent(summary));
        
        return summary;
    }
    
    /**
     * Construeix una matriu de distàncies per a les llengües carregades utilitzant l'algorisme
     * especificat amb paràmetres de mostreig per defecte.
     * 
     * Utilitza paràmetres predeterminats de 3 mostres amb una mida màxima de 200 paraules
     * per mostra. Activa totes les optimitzacions disponibles per defecte.
     * 
     * @param algorithm algorisme de distància a utilitzar
     * @param progressListener listener per informar del progrés del càlcul
     * @return Future que encapsula el càlcul asíncron de la matriu de distàncies
     * @throws IllegalStateException si no s'han carregat diccionaris prèviament
     */
    public Future<DistanceMatrix> buildDistanceMatrix(DistanceAlgorithm algorithm, 
                                                      DistanceMatrix.ProgressListener progressListener) {
        return buildDistanceMatrix(algorithm, progressListener, 3L, 200L);
    }
    
    /**
     * Construeix una matriu de distàncies per a les llengües carregades utilitzant l'algorisme
     * i paràmetres de mostreig especificats.
     * 
     * Permet controlar el nombre de mostres i la mida màxima de cada mostra per optimitzar
     * el temps de càlcul mantenint la precisió dels resultats.
     * 
     * @param algorithm algorisme de distància a utilitzar
     * @param progressListener listener per informar del progrés del càlcul
     * @param numSamples nombre de mostres a utilitzar per al càlcul de distàncies
     * @param maxSampleSize mida màxima de mostra per al càlcul de distàncies
     * @return Future que encapsula el càlcul asíncron de la matriu de distàncies
     * @throws IllegalStateException si no s'han carregat diccionaris prèviament
     */
    public Future<DistanceMatrix> buildDistanceMatrix(DistanceAlgorithm algorithm, 
                                                      DistanceMatrix.ProgressListener progressListener,
                                                      long numSamples, long maxSampleSize) {
        Map<String, Boolean> defaultFlags = new HashMap<>();
        defaultFlags.put("useParallelization", true);
        defaultFlags.put("useSampling", true);
        defaultFlags.put("useNormalizedFormMatching", true);
        defaultFlags.put("useSemanticBonus", true);
        
        return buildDistanceMatrix(algorithm, progressListener, numSamples, maxSampleSize, defaultFlags);
    }
    
    /**
     * Construeix una matriu de distàncies per a les llengües carregades utilitzant l'algorisme,
     * paràmetres de mostreig i banderes d'optimització especificats.
     * 
     * Proporciona control complet sobre el procés de càlcul, incloent-hi la possibilitat
     * d'activar o desactivar la paral·lelització, el mostreig, la coincidència de formes
     * normalitzades i el bonus semàntic.
     * 
     * @param algorithm algorisme de distància a utilitzar
     * @param progressListener listener per informar del progrés del càlcul
     * @param numSamples nombre de mostres a utilitzar per al càlcul de distàncies
     * @param maxSampleSize mida màxima de mostra per al càlcul de distàncies
     * @param optimizationFlags mapa de banderes d'optimització (useParallelization, useSampling, useNormalizedFormMatching, useSemanticBonus)
     * @return Future que encapsula el càlcul asíncron de la matriu de distàncies
     * @throws IllegalStateException si no s'han carregat diccionaris prèviament
     */
    public Future<DistanceMatrix> buildDistanceMatrix(DistanceAlgorithm algorithm, 
                                                      DistanceMatrix.ProgressListener progressListener,
                                                      long numSamples, long maxSampleSize,
                                                      Map<String, Boolean> optimizationFlags) {
        if (languages == null || languages.isEmpty()) {
            throw new IllegalStateException("No dictionaries loaded");
        }
        
        return executorService.submit(() -> {
            try {
                DistanceMatrix.ProgressListener wrappedListener = (percent, langA, langB) -> {
                    try {
                        if (progressListener != null) {
                            progressListener.onProgress(percent, langA, langB);
                        }

                        notificationService.publish(new DistanceMatrixProgressEvent(percent, langA, langB));
                    } catch (Exception e) {
                        System.err.println("Error in progress listener: " + e.getMessage());
                    }
                };

                if (optimizationFlags != null) {
                    distanceMatrix = new DistanceMatrix(languages, algorithm, true, wrappedListener, numSamples, maxSampleSize,
                        optimizationFlags.getOrDefault("useParallelization", true),
                        optimizationFlags.getOrDefault("useSampling", true),
                        optimizationFlags.getOrDefault("useNormalizedFormMatching", true),
                        optimizationFlags.getOrDefault("useSemanticBonus", true));
                } else {
                    distanceMatrix = new DistanceMatrix(languages, algorithm, true, wrappedListener, numSamples, maxSampleSize);
                }

                notificationService.publish(new DistanceMatrixReadyEvent(distanceMatrix));
                
                return distanceMatrix;
            } catch (Exception e) {
                System.err.println("Error calculating distance matrix: " + e.getMessage());
                e.printStackTrace();

                notificationService.publish(new ComputationErrorEvent("Error calculating distance matrix", e.getMessage()));

                throw e;
            }
        });
    }
    
    /**
     * Esdeveniment per a la notificació d'errors de computació.
     * 
     * S'utilitza per comunicar errors durant els càlculs de distàncies o
     * altres operacions computacionals intensives.
     */
    public static class ComputationErrorEvent implements Event {
        private final String title;
        private final String message;
        
        /**
         * Constructor de l'esdeveniment d'error de computació.
         * 
         * @param title títol descriptiu de l'error
         * @param message missatge detallat de l'error
         */
        public ComputationErrorEvent(String title, String message) {
            this.title = title;
            this.message = message;
        }
        
        /**
         * Obté el títol de l'error.
         * 
         * @return títol descriptiu de l'error
         */
        public String getTitle() {
            return title;
        }
        
        /**
         * Obté el missatge detallat de l'error.
         * 
         * @return missatge explicatiu de l'error
         */
        public String getMessage() {
            return message;
        }
        
        @Override
        public EventType getType() {
            return EventType.COMPUTATION_ERROR;
        }
    }
    
    /**
     * Obté la distància entre dues llengües.
     * 
     * Consulta la matriu de distàncies prèviament calculada per obtenir
     * la distància numèrica entre els dos codis de llengua especificats.
     * 
     * @param langA codi de la primera llengua
     * @param langB codi de la segona llengua
     * @return distància numèrica entre les llengües (0-1)
     * @throws IllegalStateException si la matriu de distàncies no s'ha calculat
     */
    public double getDistance(String langA, String langB) {
        if (distanceMatrix == null) {
            throw new IllegalStateException("Distance matrix not calculated");
        }
        
        return distanceMatrix.getDistance(langA, langB);
    }
    
    /**
     * Obté la matriu de distàncies actual.
     * 
     * @return la matriu de distàncies, o null si no s'ha calculat
     */
    public DistanceMatrix getDistanceMatrix() {
        return distanceMatrix;
    }
    
    /**
     * Obté totes les distàncies d'una llengua a totes les altres.
     * 
     * Proporciona un mapa complet de distàncies des de la llengua especificada
     * cap a totes les altres llengües del corpus carregat.
     * 
     * @param lang codi de la llengua de referència
     * @return mapa de codis de llengua a distàncies numèriques
     * @throws IllegalStateException si la matriu de distàncies no s'ha calculat
     */
    public Map<String, Double> getAllDistances(String lang) {
        if (distanceMatrix == null) {
            throw new IllegalStateException("Distance matrix not calculated");
        }
        
        return distanceMatrix.getAllDistances(lang);
    }
    
    /**
     * Construeix una representació gràfica de la matriu de distàncies amb un llindar.
     * 
     * Crea un graf on les llengües són nodes i les arestes representen relacions
     * de proximitat entre llengües. Només es creen arestes quan la distància
     * entre dues llengües és menor o igual al llindar especificat.
     * 
     * @param threshold distància màxima per crear arestes (0-1)
     * @return dades del graf amb nodes i arestes
     * @throws IllegalStateException si la matriu de distàncies no s'ha calculat
     */
    public GraphData buildDistanceGraph(double threshold) {
        if (distanceMatrix == null) {
            throw new IllegalStateException("Distance matrix not calculated");
        }

        GraphData graphData = new GraphData(distanceMatrix, threshold);

        notificationService.publish(new GraphDataReadyEvent(graphData));
        
        return graphData;
    }
    
    /**
     * Construeix un arbre filogenetíc a partir de la matriu de distàncies.
     * 
     * Utilitza l'algorisme UPGMA (Unweighted Pair Group Method with Arithmetic Mean)
     * per crear una representació hieràrquica de les relacions entre llengües
     * basada en les distàncies calculades.
     * 
     * @return dades de l'arbre amb nodes i estructura hieràrquica
     * @throws IllegalStateException si la matriu de distàncies no s'ha calculat
     */
    public TreeData buildPhyloTree() {
        if (distanceMatrix == null) {
            throw new IllegalStateException("Distance matrix not calculated");
        }

        TreeData treeData = new TreeData(distanceMatrix);

        notificationService.publish(new TreeDataReadyEvent(treeData));
        
        return treeData;
    }
    
    /**
     * Esdeveniment per a la notificació de dades d'arbre llestes.
     * 
     * S'emet quan s'ha completat la construcció de l'arbre filogenetíc
     * i les dades estan disponibles per a la visualització.
     */
    public static class TreeDataReadyEvent implements Event {
        private final TreeData treeData;
        
        /**
         * Constructor de l'esdeveniment de dades d'arbre llestes.
         * 
         * @param treeData dades de l'arbre filogenetíc generat
         */
        public TreeDataReadyEvent(TreeData treeData) {
            this.treeData = treeData;
        }
        
        /**
         * Obté les dades de l'arbre.
         * 
         * @return dades de l'arbre filogenetíc
         */
        public TreeData getTreeData() {
            return treeData;
        }
        
        @Override
        public EventType getType() {
            return EventType.GRAPH_DATA_READY;
        }
    }
    
    /**
     * Afegeix un observador al model.
     * 
     * Registra un observador que serà notificat dels esdeveniments
     * que es produeixin al model durant l'execució.
     * 
     * @param observer observador a afegir
     */
    public void addObserver(ModelObserver observer) {
        observers.add(observer);
    }
    
    /**
     * Elimina un observador del model.
     * 
     * Desregistra un observador per deixar de rebre notificacions
     * dels esdeveniments del model.
     * 
     * @param observer observador a eliminar
     */
    public void removeObserver(ModelObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * Neteja els recursos quan el model ja no és necessari.
     * 
     * Intenta tancar de manera elegant el servei d'executors amb un temps d'espera,
     * permetent que les tasques en curs es completin abans del tancament forçat.
     * Aquest mètode hauria de ser cridat abans de descartar el model per evitar
     * filtracions de recursos.
     */
    public void shutdown() {
        try {
            System.out.println("Shutting down executor service...");

            executorService.shutdown();

            if (!executorService.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                System.out.println("Forcing executor service shutdown...");

                executorService.shutdownNow();

                if (!executorService.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS)) {
                    System.err.println("Executor service did not terminate");
                }
            }
            
            System.out.println("Executor service shut down successfully");
        } catch (InterruptedException e) {
            System.err.println("Shutdown interrupted: " + e.getMessage());
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Interfície per als observadors del model.
     * 
     * Defineix el contracte que han de complir les classes que vulguin
     * rebre notificacions dels esdeveniments que es produeixin al model.
     */
    public interface ModelObserver {
        /**
         * Cridat quan es produeix un esdeveniment al model.
         * 
         * @param event esdeveniment que s'ha produït
         */
        void onEvent(Event event);
    }
    
    /**
     * Esdeveniment per a la notificació de corpus carregat.
     * 
     * S'emet quan s'ha completat la càrrega d'un corpus de diccionaris
     * i està disponible per a l'anàlisi.
     */
    public static class CorpusLoadedEvent implements Event {
        private final CorpusSummary summary;
        
        /**
         * Constructor de l'esdeveniment de corpus carregat.
         * 
         * @param summary resum del corpus carregat
         */
        public CorpusLoadedEvent(CorpusSummary summary) {
            this.summary = summary;
        }
        
        /**
         * Obté el resum del corpus.
         * 
         * @return resum amb informació del corpus carregat
         */
        public CorpusSummary getSummary() {
            return summary;
        }
        
        @Override
        public EventType getType() {
            return EventType.CORPUS_LOADED;
        }
    }
    
    /**
     * Esdeveniment per a la notificació del progrés de la matriu de distàncies.
     * 
     * S'emet periòdicament durant el càlcul de la matriu de distàncies
     * per informar del progrés i de les llengües que s'estan processant.
     */
    public static class DistanceMatrixProgressEvent implements Event {
        private final int percent;
        private final String currentLangA;
        private final String currentLangB;
        
        /**
         * Constructor de l'esdeveniment de progrés de la matriu de distàncies.
         * 
         * @param percent percentatge de completació (0-100)
         * @param currentLangA codi de la primera llengua que s'està processant
         * @param currentLangB codi de la segona llengua que s'està processant
         */
        public DistanceMatrixProgressEvent(int percent, String currentLangA, String currentLangB) {
            this.percent = percent;
            this.currentLangA = currentLangA;
            this.currentLangB = currentLangB;
        }
        
        /**
         * Obté el percentatge de progrés.
         * 
         * @return percentatge de completació (0-100)
         */
        public int getPercent() {
            return percent;
        }
        
        /**
         * Obté el codi de la primera llengua que s'està processant.
         * 
         * @return codi de la llengua A
         */
        public String getCurrentLangA() {
            return currentLangA;
        }
        
        /**
         * Obté el codi de la segona llengua que s'està processant.
         * 
         * @return codi de la llengua B
         */
        public String getCurrentLangB() {
            return currentLangB;
        }
        
        @Override
        public EventType getType() {
            return EventType.DISTANCE_MATRIX_PROGRESS;
        }
    }
    
    /**
     * Esdeveniment per a la notificació de matriu de distàncies llesta.
     * 
     * S'emet quan s'ha completat el càlcul de la matriu de distàncies
     * i està disponible per a consultes i visualitzacions.
     */
    public static class DistanceMatrixReadyEvent implements Event {
        private final DistanceMatrix matrix;
        
        /**
         * Constructor de l'esdeveniment de matriu de distàncies llesta.
         * 
         * @param matrix matriu de distàncies calculada
         */
        public DistanceMatrixReadyEvent(DistanceMatrix matrix) {
            this.matrix = matrix;
        }
        
        /**
         * Obté la matriu de distàncies.
         * 
         * @return matriu de distàncies calculada
         */
        public DistanceMatrix getMatrix() {
            return matrix;
        }
        
        @Override
        public EventType getType() {
            return EventType.DISTANCE_MATRIX_READY;
        }
    }
    
    /**
     * Esdeveniment per a la notificació de dades de graf llestes.
     * 
     * S'emet quan s'ha completat la construcció del graf de proximitat
     * entre llengües i està disponible per a la visualització.
     */
    public static class GraphDataReadyEvent implements Event {
        private final GraphData graphData;
        
        /**
         * Constructor de l'esdeveniment de dades de graf llestes.
         * 
         * @param graphData dades del graf generat
         */
        public GraphDataReadyEvent(GraphData graphData) {
            this.graphData = graphData;
        }
        
        /**
         * Obté les dades del graf.
         * 
         * @return dades del graf de proximitat entre llengües
         */
        public GraphData getGraphData() {
            return graphData;
        }
        
        @Override
        public EventType getType() {
            return EventType.GRAPH_DATA_READY;
        }
    }
    
    /**
     * Registre per al resum del corpus.
     * 
     * Conté informació resum del corpus carregat, incloent-hi la llista
     * de llengües disponibles i el nombre total de paraules.
     * 
     * @param languages llista de codis de llengües carregades
     * @param wordsTotal nombre total de paraules al corpus
     */
    public record CorpusSummary(List<String> languages, int wordsTotal) {}
}
package controller;

import model.LexModel;
import model.algorithm.distance.DistanceAlgorithm;
import model.algorithm.distance.DistanceMatrix;
import model.visualization.GraphData;
import model.visualization.TreeData;
import notification.NotificationService;
import notification.NotificationService.Event;
import notification.NotificationService.EventType;
import notification.NotificationServiceImpl.UINotificationHandler;
import view.LexView;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

/**
 * Controlador principal per a l'aplicació LexDistance que gestiona la lògica de negoci
 * i coordina la comunicació entre el model (LexModel) i la vista (LexView).
 * 
 * Aquest controlador s'encarrega de:
 * - Gestionar la càrrega de diccionaris des de fitxers
 * - Coordinar el càlcul de matrius de distància entre llengües
 * - Manejar consultes de distància individuals i globals
 * - Generar visualitzacions gràfiques i arbres filogenètics
 * - Exportar resultats a formats CSV
 * - Gestionar notificacions i esdeveniments del sistema
 * 
 * Implementa els patrons Observer per rebre esdeveniments del model i
 * UINotificationHandler per gestionar notificacions de la interfície d'usuari.
 * 
 * @author Dylan Canning
 * @version 1.0
 * @since 1.0
 */
public class LexController implements LexModel.ModelObserver, UINotificationHandler {
    
    private static final Logger logger = Logger.getLogger(LexController.class.getName());
    
    private LexModel model;
    private LexView view;
    private NotificationService notificationService;
    
    private Future<?> currentTask;
    
    /**
     * Inicialitza el controlador amb el model, la vista i el servei de notificacions.
     * Estableix les connexions necessàries entre els components i configura
     * els observadors per als esdeveniments del sistema.
     * 
     * Aquest mètode:
     * - Assigna les referències del model, vista i servei de notificacions
     * - Configura el controlador a la vista
     * - Registra el controlador com a observador del model
     * - Subscriu el controlador a tots els tipus d'esdeveniments
     * - Configura el sistema de logging
     * 
     * @param model El model de dades que conté la lògica de negoci i les dades de diccionaris
     * @param view La vista de la interfície d'usuari per mostrar informació i rebre interaccions
     * @param notificationService El servei de notificacions per gestionar esdeveniments del sistema
     * @throws IllegalArgumentException si algun dels paràmetres és nul
     */
    public void init(LexModel model, LexView view, NotificationService notificationService) {
        this.model = model;
        this.view = view;
        this.notificationService = notificationService;
        

        if (view != null) {
            view.setController(this);
            view.setNotificationService(notificationService);
        }
        model.addObserver(this);
        

        for (EventType eventType : EventType.values()) {
            notificationService.subscribe(eventType, this);
        }
        

        setupLogging();
    }
    
    /**
     * Configura el sistema de logging per escriure els registres a un fitxer.
     * Crea un FileHandler que dirigeix els missatges de log al fitxer 'logs/app.log'.
     * 
     * En cas d'error durant la configuració del logging, es mostra un missatge
     * d'error a la sortida estàndard d'errors sense interrompre l'execució.
     * 
     * @throws SecurityException si no hi ha permisos per crear o escriure al fitxer de log
     */
    private void setupLogging() {
        try {
            FileHandler fileHandler = new FileHandler("logs/app.log");
            logger.addHandler(fileHandler);
        } catch (Exception e) {
            System.err.println("Could not set up logging: " + e.getMessage());
        }
    }
    
    /**
     * Gestiona la selecció de fitxers de diccionari per part de l'usuari.
     * Carrega els diccionaris especificats utilitzant el model i gestiona
     * qualsevol error que pugui ocórrer durant el procés de càrrega.
     * 
     * Els fitxers han de contenir diccionaris en format compatible amb
     * el sistema (típicament fitxers de text amb paraules per línia).
     * 
     * @param files Array de camins als fitxers de diccionari seleccionats
     * @throws IllegalArgumentException si l'array de fitxers és nul o buit
     * @see LexModel#loadDictionaries(Path[])
     */
    public void onUserFileSelection(Path[] files) {
        try {
            model.loadDictionaries(files);
        } catch (Exception e) {
            logger.severe("Error loading dictionaries: " + e.getMessage());
            notificationService.publish(new ComputationErrorEvent(ErrorCode.LOADING_ERROR, e.getMessage()));
        }
    }
    
    /**
     * Gestiona la sol·licitud d'inici de càlcul de matriu de distància per part de l'usuari
     * utilitzant configuració per defecte d'optimitzacions.
     * 
     * Aquest mètode utilitza les següents optimitzacions per defecte:
     * - Paral·lelització activada
     * - Mostreig activat
     * - Coincidència de formes normalitzades activada
     * - Bonificació semàntica activada
     * 
     * @param algo L'algoritme de distància a utilitzar per al càlcul
     * @param numSamples Nombre de mostres a utilitzar per llengua (mínim 1)
     * @param sampleSize Mida màxima de cada mostra en nombre de paraules
     * @throws IllegalArgumentException si numSamples < 1 o sampleSize < 1
     * @see #onUserStartComputation(DistanceAlgorithm, long, long, Map)
     */
    public void onUserStartComputation(DistanceAlgorithm algo, long numSamples, long sampleSize) {

        Map<String, Boolean> defaultFlags = new HashMap<>();
        defaultFlags.put("useParallelization", true);
        defaultFlags.put("useSampling", true);
        defaultFlags.put("useNormalizedFormMatching", true);
        defaultFlags.put("useSemanticBonus", true);
        
        onUserStartComputation(algo, numSamples, sampleSize, defaultFlags);
    }
    
    /**
     * Gestiona la sol·licitud d'inici de càlcul de matriu de distància amb configuració
     * personalitzada d'optimitzacions.
     * 
     * Cancel·la qualsevol tasca de càlcul en curs abans d'iniciar la nova computació.
     * Crea un listener de progrés per monitoritzar l'estat del càlcul i inicia
     * el procés de construcció de la matriu de distància de forma asíncrona.
     * 
     * Les optimitzacions disponibles inclouen:
     * - useParallelization: Utilitza múltiples fils per accelerar el càlcul
     * - useSampling: Utilitza mostreig per reduir la complexitat computacional
     * - useNormalizedFormMatching: Compara formes normalitzades de paraules
     * - useSemanticBonus: Aplica bonificacions per similituds semàntiques
     * 
     * @param algo L'algoritme de distància a utilitzar (Levenshtein, Jaro-Winkler, etc.)
     * @param numSamples Nombre de mostres a generar per cada llengua
     * @param sampleSize Nombre màxim de paraules per mostra
     * @param optimizationFlags Mapa amb les optimitzacions a aplicar
     * @throws IllegalArgumentException si algun paràmetre és invàlid
     * @throws IllegalStateException si el model no està inicialitzat
     */
    public void onUserStartComputation(DistanceAlgorithm algo, long numSamples, long sampleSize, Map<String, Boolean> optimizationFlags) {

        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
        
        try {

            DistanceMatrix.ProgressListener progressListener = (percent, langA, langB) -> {

            };
            

            currentTask = model.buildDistanceMatrix(algo, progressListener, numSamples, sampleSize, optimizationFlags);
        } catch (Exception e) {
            logger.severe("Error starting computation: " + e.getMessage());
            notificationService.publish(new ComputationErrorEvent(ErrorCode.COMPUTATION_ERROR, e.getMessage()));
        }
    }
    
    /**
     * Gestiona la sol·licitud de cancel·lació de la computació actual.
     * 
     * Intenta cancel·lar la tasca de càlcul en curs i publica un esdeveniment
     * de notificació amb el resultat de l'operació. Si no hi ha cap computació
     * en curs, informa que no hi ha res a cancel·lar.
     * 
     * La cancel·lació és de tipus interruptiu, la qual cosa significa que
     * intentarà interrompre el fil d'execució si és necessari.
     * 
     * @see Future#cancel(boolean)
     */
    public void onUserCancelComputation() {
        if (currentTask != null && !currentTask.isDone()) {

            boolean cancelled = currentTask.cancel(true);
            
            if (cancelled) {
                notificationService.publish(new ComputationErrorEvent(ErrorCode.CANCELLED, "Computation cancelled by user"));
            } else {
                notificationService.publish(new ComputationErrorEvent(ErrorCode.COMPUTATION_ERROR, "Could not cancel computation"));
            }
        } else {
            notificationService.publish(new ComputationErrorEvent(ErrorCode.SUCCESS, "No computation to cancel"));
        }
    }
    
    /**
     * Gestiona la sol·licitud d'inici de càlcul utilitzant paràmetres de mostreig per defecte.
     * 
     * Utilitza valors per defecte de 3 mostres per llengua amb una mida màxima
     * de 200 paraules per mostra. Aquests valors ofereixen un bon equilibri
     * entre precisió i velocitat de càlcul per a la majoria d'aplicacions.
     * 
     * @param algo L'algoritme de distància a utilitzar per al càlcul
     * @see #onUserStartComputation(DistanceAlgorithm, long, long)
     */
    public void onUserStartComputation(DistanceAlgorithm algo) {
        onUserStartComputation(algo, 3L, 200L);
    }
    
    /**
     * Gestiona la consulta de distància entre dues llengües específiques.
     * 
     * Obté la distància calculada entre les dues llengües especificades
     * des del model i publica un esdeveniment amb el resultat. Si la matriu
     * de distància no ha estat calculada prèviament, es produirà un error.
     * 
     * @param langA Codi de la primera llengua (per exemple, "ca" per català)
     * @param langB Codi de la segona llengua (per exemple, "es" per espanyol)
     * @throws IllegalArgumentException si els codis de llengua són nuls o buits
     * @throws IllegalStateException si la matriu de distància no ha estat calculada
     */
    public void onUserQuerySingle(String langA, String langB) {
        try {
            double distance = model.getDistance(langA, langB);
            notificationService.publish(new QueryAnsweredEvent(langA, langB, distance));
        } catch (Exception e) {
            logger.severe("Error querying distance: " + e.getMessage());
            notificationService.publish(new ComputationErrorEvent(ErrorCode.QUERY_ERROR, e.getMessage()));
        }
    }
    
    /**
     * Gestiona la consulta de totes les distàncies des d'una llengua específica
     * cap a totes les altres llengües del corpus.
     * 
     * Obté un mapa amb totes les distàncies calculades des de la llengua
     * especificada i publica esdeveniments individuals per a cada parella
     * de llengües. Això permet mostrar una vista completa de les relacions
     * lingüístiques des d'una perspectiva específica.
     * 
     * @param lang Codi de la llengua de referència
     * @throws IllegalArgumentException si el codi de llengua és nul o buit
     * @throws IllegalStateException si la matriu de distància no ha estat calculada
     */
    public void onUserQueryAll(String lang) {
        try {
            Map<String, Double> distances = model.getAllDistances(lang);


            for (Map.Entry<String, Double> entry : distances.entrySet()) {
                notificationService.publish(new QueryAnsweredEvent(lang, entry.getKey(), entry.getValue()));
            }
        } catch (Exception e) {
            logger.severe("Error querying all distances: " + e.getMessage());
            notificationService.publish(new ComputationErrorEvent(ErrorCode.QUERY_ERROR, e.getMessage()));
        }
    }
    
    /**
     * Gestiona la sol·licitud de mostrar un graf de distàncies amb un llindar específic.
     * 
     * Genera un graf on només es mostren les connexions (arestes) entre llengües
     * que tenen una distància inferior o igual al llindar especificat. Això
     * permet visualitzar agrupacions de llengües relacionades filtrant
     * les connexions més dèbils.
     * 
     * El graf resultant pot ser utilitzat per identificar famílies lingüístiques
     * i relacions de proximitat entre llengües.
     * 
     * @param threshold Distància màxima per mostrar connexions (valor entre 0.0 i 1.0)
     * @throws IllegalArgumentException si el llindar no està entre 0.0 i 1.0
     * @throws IllegalStateException si la matriu de distància no ha estat calculada
     */
    public void onUserShowGraph(double threshold) {
        try {
            GraphData graphData = model.buildDistanceGraph(threshold);
            notificationService.publish(new RenderRequestEvent(RenderType.GRAPH, graphData));
        } catch (Exception e) {
            logger.severe("Error building graph: " + e.getMessage());
            notificationService.publish(new ComputationErrorEvent(ErrorCode.VISUALIZATION_ERROR, e.getMessage()));
        }
    }
    
    /**
     * Gestiona la sol·licitud de mostrar un arbre filogenètic de les llengües.
     * 
     * Inicia el procés de construcció d'un arbre filogenètic basat en les
     * distàncies calculades entre llengües. L'arbre resultant mostra les
     * relacions jerràrquiques i evolutives entre les llengües del corpus.
     * 
     * El model publicarà un esdeveniment TreeDataReadyEvent quan l'arbre
     * estigui llest per ser visualitzat.
     * 
     * @throws IllegalStateException si la matriu de distància no ha estat calculada
     * @see LexModel#buildPhyloTree()
     */
    public void onUserShowTree() {
        try {
            model.buildPhyloTree();

        } catch (Exception e) {
            logger.severe("Error building phylogenetic tree: " + e.getMessage());
            notificationService.publish(new ComputationErrorEvent(ErrorCode.VISUALIZATION_ERROR, e.getMessage()));
        }
    }
    
    /**
     * Gestiona la sol·licitud d'exportar la matriu de distància a format CSV.
     * 
     * Exporta la matriu de distància calculada a un fitxer CSV amb format
     * tabular, on les files i columnes representen llengües i els valors
     * representen les distàncies entre elles. El fitxer inclou una capçalera
     * amb els codis de llengua.
     * 
     * El format de sortida és compatible amb fulls de càlcul i altres
     * eines d'anàlisi de dades.
     * 
     * @param target Camí del fitxer de destinació per l'exportació CSV
     * @throws IllegalStateException si el model o la matriu de distància no estan inicialitzats
     * @throws IOException si hi ha problemes d'escriptura al fitxer
     */
    public void onUserExportCSV(Path target) {
        try {
            if (model == null) {
                throw new IllegalStateException("Model not initialized");
            }
            
            if (model.getDistanceMatrix() == null) {
                throw new IllegalStateException("Distance matrix not calculated");
            }
            

            DistanceMatrix matrix = model.getDistanceMatrix();
            

            exportDistanceMatrixToCSV(matrix, target);
            

            notificationService.publish(new ComputationErrorEvent(ErrorCode.SUCCESS, "CSV exported to " + target));
        } catch (Exception e) {
            logger.severe("Error exporting CSV: " + e.getMessage());
            notificationService.publish(new ComputationErrorEvent(ErrorCode.EXPORT_ERROR, e.getMessage()));
        }
    }
    
    /**
     * Exporta la matriu de distància a un fitxer CSV amb format tabular.
     * 
     * Crea un fitxer CSV on:
     * - La primera fila conté els codis de llengua com a capçalera
     * - Cada fila subsegüent representa una llengua
     * - Cada columna conté la distància cap a la llengua corresponent
     * - Els valors de distància es formaten amb 6 decimals de precisió
     * 
     * El fitxer resultant pot ser importat fàcilment en aplicacions
     * de full de càlcul o eines d'anàlisi estadística.
     * 
     * @param matrix La matriu de distància a exportar
     * @param target Camí del fitxer de destinació
     * @throws IOException si hi ha errors durant l'escriptura del fitxer
     * @throws IllegalArgumentException si la matriu és nul·la o buida
     */
    private void exportDistanceMatrixToCSV(DistanceMatrix matrix, Path target) throws Exception {
        List<String> languages = matrix.getLanguageCodes();
        double[][] distances = matrix.getMatrix();
        

        StringBuilder csv = new StringBuilder();
        

        csv.append("Language");
        for (String lang : languages) {
            csv.append(",").append(lang);
        }
        csv.append("\n");
        

        for (int i = 0; i < languages.size(); i++) {
            csv.append(languages.get(i));
            
            for (int j = 0; j < languages.size(); j++) {
                csv.append(",").append(String.format("%.6f", distances[i][j]));
            }
            
            csv.append("\n");
        }
        

        java.nio.file.Files.writeString(target, csv.toString());
    }
    
    /**
     * Gestiona els esdeveniments rebuts del model i del sistema de notificacions.
     * 
     * Aquest mètode és l'entrada principal per processar tots els esdeveniments
     * del sistema, incloent:
     * - Càrrega de corpus completada
     * - Progrés de càlcul de matriu de distància
     * - Matriu de distància llesta
     * - Resposta a consultes
     * - Dades de graf i arbre llestes
     * - Sol·licituds de renderització
     * - Errors de computació
     * 
     * Cada tipus d'esdeveniment es dirigeix al mètode apropiat de la vista
     * per actualitzar la interfície d'usuari corresponent.
     * 
     * @param event L'esdeveniment a processar
     * @throws ClassCastException si l'esdeveniment no és del tipus esperat
     */
    @Override
    public void onEvent(Event event) {

        if (view == null) {
            return;
        }
        
        switch (event.getType()) {
            case CORPUS_LOADED:
                LexModel.CorpusLoadedEvent corpusEvent = (LexModel.CorpusLoadedEvent) event;
                view.showCorpusSummary(corpusEvent.getSummary());
                break;
                
            case DISTANCE_MATRIX_PROGRESS:
                LexModel.DistanceMatrixProgressEvent progressEvent = (LexModel.DistanceMatrixProgressEvent) event;
                view.updateProgress(progressEvent.getPercent(), 
                    progressEvent.getCurrentLangA() + " → " + progressEvent.getCurrentLangB());
                break;
                
            case DISTANCE_MATRIX_READY:
                LexModel.DistanceMatrixReadyEvent matrixEvent = (LexModel.DistanceMatrixReadyEvent) event;
                notificationService.publish(new RenderRequestEvent(RenderType.TABLE, matrixEvent.getMatrix()));
                break;
                
            case QUERY_ANSWERED:
                QueryAnsweredEvent queryEvent = (QueryAnsweredEvent) event;
                view.showQueryResult(new QueryResult(queryEvent.getLangA(), queryEvent.getLangB(), queryEvent.getDistance()));
                break;
                
            case GRAPH_DATA_READY:

                if (event instanceof LexModel.GraphDataReadyEvent) {
                    LexModel.GraphDataReadyEvent graphEvent = (LexModel.GraphDataReadyEvent) event;
                    notificationService.publish(new RenderRequestEvent(RenderType.GRAPH, graphEvent.getGraphData()));
                } else if (event instanceof LexModel.TreeDataReadyEvent) {
                    LexModel.TreeDataReadyEvent treeEvent = (LexModel.TreeDataReadyEvent) event;
                    notificationService.publish(new RenderRequestEvent(RenderType.TREE, treeEvent.getTreeData()));
                }
                break;
                
            case RENDER_REQUEST:
                RenderRequestEvent renderEvent = (RenderRequestEvent) event;
                switch (renderEvent.getRenderType()) {
                    case TABLE:
                        view.showDistanceTable((DistanceMatrix) renderEvent.getPayload());
                        break;
                    case GRAPH:
                        view.renderGraph((GraphData) renderEvent.getPayload());
                        break;
                    case TREE:
                        view.renderTree((TreeData) renderEvent.getPayload());
                        break;
                }
                break;
                
            case COMPUTATION_ERROR:
                ComputationErrorEvent errorEvent = (ComputationErrorEvent) event;
                view.showError(errorEvent.getMessage());
                break;
        }
    }
    
    /**
     * Esdeveniment per notificar que s'ha respost a una consulta de distància.
     * 
     * Aquest esdeveniment encapsula la informació d'una consulta de distància
     * entre dues llengües, incloent els codis de llengua i la distància calculada.
     * S'utilitza per comunicar els resultats de consultes individuals entre
     * components del sistema.
     * 
     * @since 1.0
     */
    public static class QueryAnsweredEvent implements Event {
        private final String langA;
        private final String langB;
        private final double distance;
        
        /**
         * Crea un nou esdeveniment de consulta resposta.
         * 
         * @param langA Codi de la primera llengua
         * @param langB Codi de la segona llengua  
         * @param distance Distància calculada entre les llengües (0.0 a 1.0)
         */
        public QueryAnsweredEvent(String langA, String langB, double distance) {
            this.langA = langA;
            this.langB = langB;
            this.distance = distance;
        }
        
        /**
         * Obté el codi de la primera llengua de la consulta.
         * 
         * @return Codi de la primera llengua
         */
        public String getLangA() {
            return langA;
        }
        
        /**
         * Obté el codi de la segona llengua de la consulta.
         * 
         * @return Codi de la segona llengua
         */
        public String getLangB() {
            return langB;
        }
        
        /**
         * Obté la distància calculada entre les dues llengües.
         * 
         * @return Distància entre llengües (valor entre 0.0 i 1.0)
         */
        public double getDistance() {
            return distance;
        }
        
        @Override
        public EventType getType() {
            return EventType.QUERY_ANSWERED;
        }
    }
    
    /**
     * Esdeveniment per sol·licitar la renderització d'elements visuals.
     * 
     * Aquest esdeveniment encapsula una sol·licitud de renderització que inclou
     * el tipus de visualització desitjada (taula, graf, arbre) i les dades
     * necessàries per generar la visualització.
     * 
     * S'utilitza per comunicar entre el controlador i la vista quan cal
     * mostrar elements visuals complexos com matrius, grafs o arbres.
     * 
     * @since 1.0
     */
    public static class RenderRequestEvent implements Event {
        private final RenderType renderType;
        private final Object payload;
        
        /**
         * Crea un nou esdeveniment de sol·licitud de renderització.
         * 
         * @param renderType Tipus de renderització sol·licitada
         * @param payload Dades necessàries per a la renderització
         */
        public RenderRequestEvent(RenderType renderType, Object payload) {
            this.renderType = renderType;
            this.payload = payload;
        }
        
        /**
         * Obté el tipus de renderització sol·licitada.
         * 
         * @return Tipus de renderització (TABLE, GRAPH, TREE)
         */
        public RenderType getRenderType() {
            return renderType;
        }
        
        /**
         * Obté les dades necessàries per a la renderització.
         * 
         * @return Objecte amb les dades de renderització (DistanceMatrix, GraphData, TreeData)
         */
        public Object getPayload() {
            return payload;
        }
        
        @Override
        public EventType getType() {
            return EventType.RENDER_REQUEST;
        }
    }
    
    /**
     * Esdeveniment per notificar errors durant processos de computació.
     * 
     * Aquest esdeveniment encapsula informació sobre errors que ocorren
     * durant operacions del sistema, incloent càrrega de fitxers,
     * càlculs de distància, consultes, visualitzacions i exportacions.
     * 
     * Inclou un codi d'error per categoritzar el tipus d'error i un
     * missatge descriptiu per proporcionar detalls específics.
     * 
     * @since 1.0
     */
    public static class ComputationErrorEvent implements Event {
        private final ErrorCode code;
        private final String message;
        
        /**
         * Crea un nou esdeveniment d'error de computació.
         * 
         * @param code Codi que categoritza el tipus d'error
         * @param message Missatge descriptiu de l'error
         */
        public ComputationErrorEvent(ErrorCode code, String message) {
            this.code = code;
            this.message = message;
        }
        
        /**
         * Obté el codi d'error que categoritza el tipus d'error ocorregut.
         * 
         * @return Codi d'error del tipus ErrorCode
         */
        public ErrorCode getCode() {
            return code;
        }
        
        /**
         * Obté el missatge descriptiu de l'error.
         * 
         * @return Missatge amb detalls de l'error ocorregut
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
     * Enumerat que defineix els tipus de renderització disponibles al sistema.
     * 
     * Aquest enumerat s'utilitza per especificar quin tipus de visualització
     * es vol generar quan es processen dades de distància entre llengües.
     * 
     * @since 1.0
     */
    public enum RenderType {
        /** Renderització en format taula/matriu per mostrar distàncies en format tabular */
        TABLE,
        /** Renderització com a graf de nodes i arestes per visualitzar connexions entre llengües */
        GRAPH,
        /** Renderització com a arbre filogenètic per mostrar relacions jeràrquiques */
        TREE
    }
    
    /**
     * Enumerat que defineix els codis d'error utilitzats al sistema.
     * 
     * Aquests codis permeten categoritzar i identificar els diferents
     * tipus d'errors que poden ocórrer durant l'execució de l'aplicació,
     * facilitant el maneig específic de cada situació d'error.
     * 
     * @since 1.0
     */
    public enum ErrorCode {
        /** Operació completada amb èxit */
        SUCCESS,
        /** Error durant la càrrega de fitxers de diccionari */
        LOADING_ERROR,
        /** Error durant el càlcul de matrius de distància */
        COMPUTATION_ERROR,
        /** Error durant l'execució de consultes de distància */
        QUERY_ERROR,
        /** Error durant la generació de visualitzacions */
        VISUALIZATION_ERROR,
        /** Error durant l'exportació de dades */
        EXPORT_ERROR,
        /** Operació cancel·lada per l'usuari */
        CANCELLED
    }
    
    /**
     * Registre immutable que encapsula el resultat d'una consulta de distància.
     * 
     * Aquest record proporciona una manera convenient i immutable d'emmagatzemar
     * i transportar els resultats de consultes entre llengües, incloent els
     * codis de les llengües consultades i la distància calculada entre elles.
     * 
     * @param langA Codi de la primera llengua de la consulta
     * @param langB Codi de la segona llengua de la consulta
     * @param distance Distància calculada entre les llengües (0.0 a 1.0)
     * @since 1.0
     */
    public record QueryResult(String langA, String langB, double distance) {}
}
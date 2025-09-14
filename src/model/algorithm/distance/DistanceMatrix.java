package model.algorithm.distance;

import model.dictionary.Language;
import model.dictionary.StringNormalizer;
import model.dictionary.Word;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Representa una matriu de distàncies entre llengües.
 * 
 * Aquesta classe encapsula el càlcul i emmagatzematge d'una matriu simètrica
 * de distàncies entre totes les parelles de llengües d'un corpus. Proporciona
 * múltiples opcions d'optimització incloent-hi paral·lelització, mostreig,
 * coincidència de formes normalitzades i bonus semàntic.
 * 
 * La matriu es calcula utilitzant un algorisme de distància especificat i
 * pot ser configurada per utilitzar diferents estratègies d'optimització
 * depenent dels requisits de rendiment i precisió.
 * 
 * @author Equip ProjecteDiccionaris
 * @version 1.0
 */
public class DistanceMatrix {
    private final Map<String, Language> languages;
    private final DistanceAlgorithm algorithm;
    private final double[][] matrix;
    private final List<String> languageCodes;
    private final boolean normalized;
    private final long numSamples;
    private final long maxSampleSize;
    
    // Optimization flags
    private final boolean useParallelization;
    private final boolean useSampling;
    private final boolean useNormalizedFormMatching;
    private final boolean useSemanticBonus;
    
    /**
     * Crea una nova matriu de distàncies amb totes les opcions especificades.
     * 
     * Aquest constructor proporciona control complet sobre el procés de càlcul,
     * permetent configurar totes les optimitzacions i paràmetres disponibles.
     * 
     * @param languages mapa de codis de llengua a objectes Language
     * @param algorithm algorisme de distància a utilitzar
     * @param normalized si les distàncies han de ser normalitzades
     * @param progressListener listener per informar del progrés
     * @param numSamples nombre de mostres per al càlcul de distàncies
     * @param maxSampleSize mida màxima de mostra per al càlcul
     * @param useParallelization si utilitzar processament en paral·lel
     * @param useSampling si utilitzar mostreig per a la selecció de paraules
     * @param useNormalizedFormMatching si utilitzar coincidència de formes normalitzades
     * @param useSemanticBonus si aplicar bonus per coincidències semàntiques
     */
    public DistanceMatrix(Map<String, Language> languages, DistanceAlgorithm algorithm, 
                          boolean normalized, ProgressListener progressListener,
                          long numSamples, long maxSampleSize,
                          boolean useParallelization, boolean useSampling, 
                          boolean useNormalizedFormMatching, boolean useSemanticBonus) {
        this.languages = languages;
        this.algorithm = algorithm;
        this.normalized = normalized;
        this.languageCodes = new ArrayList<>(languages.keySet());
        this.numSamples = numSamples > 0 ? numSamples : 3L; // Default to 3 if invalid
        this.maxSampleSize = maxSampleSize > 0 ? maxSampleSize : 200L; // Default to 200 if invalid
        
        // Set optimization flags
        this.useParallelization = useParallelization;
        this.useSampling = useSampling;
        this.useNormalizedFormMatching = useNormalizedFormMatching;
        this.useSemanticBonus = useSemanticBonus;
        
        int size = languageCodes.size();
        this.matrix = new double[size][size];
        
        // Calculate the matrix
        calculateMatrix(progressListener);
    }
    
    /**
     * Crea una nova matriu de distàncies amb configuració de mostreig i optimitzacions per defecte.
     * 
     * Totes les optimitzacions s'activen per defecte per proporcionar el millor
     * rendiment possible amb la configuració de mostreig especificada.
     * 
     * @param languages mapa de codis de llengua a objectes Language
     * @param algorithm algorisme de distància a utilitzar
     * @param normalized si les distàncies han de ser normalitzades
     * @param progressListener listener per informar del progrés
     * @param numSamples nombre de mostres per al càlcul de distàncies
     * @param maxSampleSize mida màxima de mostra per al càlcul
     */
    public DistanceMatrix(Map<String, Language> languages, DistanceAlgorithm algorithm, 
                          boolean normalized, ProgressListener progressListener,
                          long numSamples, long maxSampleSize) {
        this(languages, algorithm, normalized, progressListener, numSamples, maxSampleSize,
             true, true, true, true); // Enable all optimizations by default
    }
    
    /**
     * Crea una nova matriu de distàncies amb configuració de mostreig i optimitzacions per defecte.
     * 
     * Utilitza 3 mostres amb una mida màxima de 200 paraules i activa totes
     * les optimitzacions per defecte.
     * 
     * @param languages mapa de codis de llengua a objectes Language
     * @param algorithm algorisme de distància a utilitzar
     * @param normalized si les distàncies han de ser normalitzades
     * @param progressListener listener per informar del progrés
     */
    public DistanceMatrix(Map<String, Language> languages, DistanceAlgorithm algorithm, 
                          boolean normalized, ProgressListener progressListener) {
        this(languages, algorithm, normalized, progressListener, 3L, 200L);
    }
    
    /**
     * Crea una nova matriu de distàncies amb banderes d'optimització especificades.
     * 
     * Permet control detallat sobre quines optimitzacions utilitzar mitjançant
     * un mapa de banderes booleans.
     * 
     * @param languages mapa de codis de llengua a objectes Language
     * @param algorithm algorisme de distància a utilitzar
     * @param normalized si les distàncies han de ser normalitzades
     * @param progressListener listener per informar del progrés
     * @param optimizationFlags mapa amb banderes d'optimització, les claus han de ser:
     *                         "useParallelization", "useSampling", 
     *                         "useNormalizedFormMatching", "useSemanticBonus"
     */
    public DistanceMatrix(Map<String, Language> languages, DistanceAlgorithm algorithm, 
                          boolean normalized, ProgressListener progressListener,
                          Map<String, Boolean> optimizationFlags) {
        this(languages, algorithm, normalized, progressListener, 3L, 200L,
             optimizationFlags.getOrDefault("useParallelization", true),
             optimizationFlags.getOrDefault("useSampling", true),
             optimizationFlags.getOrDefault("useNormalizedFormMatching", true),
             optimizationFlags.getOrDefault("useSemanticBonus", true));
    }
    
    /**
     * Obté la distància entre dues llengües.
     * 
     * Consulta la matriu de distàncies per obtenir el valor calculat
     * entre els dos codis de llengua especificats.
     * 
     * @param langA codi de la primera llengua
     * @param langB codi de la segona llengua
     * @return distància entre les llengües
     * @throws IllegalArgumentException si algun dels codis de llengua no es troba a la matriu
     */
    public double getDistance(String langA, String langB) {
        int indexA = languageCodes.indexOf(langA);
        int indexB = languageCodes.indexOf(langB);
        
        if (indexA == -1 || indexB == -1) {
            throw new IllegalArgumentException("Language code not found in matrix");
        }
        
        return matrix[indexA][indexB];
    }
    
    /**
     * Obté totes les distàncies d'una llengua a totes les altres.
     * 
     * Proporciona un mapa complet de distàncies des de la llengua especificada
     * cap a totes les altres llengües de la matriu.
     * 
     * @param lang codi de la llengua
     * @return mapa de codis de llengua a distàncies
     * @throws IllegalArgumentException si el codi de llengua no es trova a la matriu
     */
    public Map<String, Double> getAllDistances(String lang) {
        int index = languageCodes.indexOf(lang);
        
        if (index == -1) {
            throw new IllegalArgumentException("Language code not found in matrix");
        }
        
        Map<String, Double> distances = new ConcurrentHashMap<>();
        for (int i = 0; i < languageCodes.size(); i++) {
            if (i != index) {
                distances.put(languageCodes.get(i), matrix[index][i]);
            }
        }
        
        return distances;
    }
    
    /**
     * Obté les dades de la matriu.
     * 
     * @return matriu de distàncies com un array 2D
     */
    public double[][] getMatrix() {
        return matrix;
    }
    
    /**
     * Obté els codis de llengua en l'ordre que apareixen a la matriu.
     * 
     * @return llista de codis de llengua
     */
    public List<String> getLanguageCodes() {
        return languageCodes;
    }
    
    /**
     * Obté l'algorisme utilitzat per calcular les distàncies.
     * 
     * @return algorisme de distància
     */
    public DistanceAlgorithm getAlgorithm() {
        return algorithm;
    }
    
    /**
     * Obté el nombre de mostres utilitzades per als càlculs de distàncies.
     * 
     * @return nombre de mostres
     */
    public long getNumSamples() {
        return numSamples;
    }
    
    /**
     * Obté la mida màxima de mostra utilitzada per als càlculs de distàncies.
     * 
     * @return mida màxima de mostra
     */
    public long getMaxSampleSize() {
        return maxSampleSize;
    }
    
    /**
     * Comprova si les distàncies estan normalitzades.
     * 
     * @return true si estan normalitzades, false en cas contrari
     */
    public boolean isNormalized() {
        return normalized;
    }
    
    /**
     * Comprova si la paral·lelització està activada.
     * 
     * @return true si està activada, false en cas contrari
     */
    public boolean isParallelizationEnabled() {
        return useParallelization;
    }
    
    /**
     * Comprova si el mostreig està activat.
     * 
     * @return true si està activat, false en cas contrari
     */
    public boolean isSamplingEnabled() {
        return useSampling;
    }
    
    /**
     * Comprova si la coincidència de formes normalitzades està activada.
     * 
     * @return true si està activada, false en cas contrari
     */
    public boolean isNormalizedFormMatchingEnabled() {
        return useNormalizedFormMatching;
    }
    
    /**
     * Comprova si el bonus semàntic està activat.
     * 
     * @return true si està activat, false en cas contrari
     */
    public boolean isSemanticBonusEnabled() {
        return useSemanticBonus;
    }
    
    /**
     * Calcula la matriu de distàncies utilitzant una aproximació paral·lela o seqüencial
     * segons la configuració.
     * 
     * Utilitza ForkJoinPool per a la implementació paral·lela, dividint la feina
     * en tasques més petites per optimitzar l'ús dels processadors disponibles.
     * 
     * @param progressListener listener per informar del progrés
     */
    private void calculateMatrix(ProgressListener progressListener) {
        int size = languageCodes.size();
        long totalComparisons = ((long)size * (size - 1)) / 2;
        AtomicInteger completedComparisons = new AtomicInteger(0);
        
        // Set diagonal to 0 (distance to self)
        for (int i = 0; i < size; i++) {
            matrix[i][i] = 0;
        }
        
        if (useParallelization) {
            // Parallel implementation using ForkJoin
            // Define the task for computing a range of matrix cells
            class DistanceMatrixTask extends RecursiveAction {
                private final int startRow;
                private final int endRow;
                private final int threshold;
                
                public DistanceMatrixTask(int startRow, int endRow, int threshold) {
                    this.startRow = startRow;
                    this.endRow = endRow;
                    this.threshold = threshold;
                }
                
                @Override
                protected void compute() {
                    // If the task is small enough, compute it directly
                    if (endRow - startRow <= threshold) {
                        computeDirectly();
                    } else {
                        // Otherwise, split it into subtasks
                        int midRow = startRow + (endRow - startRow) / 2;
                        
                        // Create and fork subtasks
                        DistanceMatrixTask leftTask = new DistanceMatrixTask(startRow, midRow, threshold);
                        DistanceMatrixTask rightTask = new DistanceMatrixTask(midRow, endRow, threshold);
                        
                        // Fork one task and compute the other directly
                        leftTask.fork();
                        rightTask.compute();
                        
                        // Wait for the forked task
                        leftTask.join();
                    }
                }
                
                private void computeDirectly() {
                    // Calculate distances for each row in our range
                    for (int i = startRow; i < endRow; i++) {
                        // Calculate for each column j > i (upper triangular matrix)
                        for (int j = i + 1; j < size; j++) {
                            try {
                                String langCodeA = languageCodes.get(i);
                                String langCodeB = languageCodes.get(j);
                                
                                Language langA = languages.get(langCodeA);
                                Language langB = languages.get(langCodeB);
                                
                                double distance = calculateLanguageDistance(langA, langB);
                                
                                // Store distance in matrix (both positions since it's symmetric)
                                matrix[i][j] = distance;
                                matrix[j][i] = distance;
                                
                                // Update progress
                                int completed = completedComparisons.incrementAndGet();
                                if (progressListener != null) {
                                    int percent = (int)((completed * 100L) / totalComparisons);
                                    progressListener.onProgress(percent, langCodeA, langCodeB);
                                }
                            } catch (Exception e) {
                                // Log error but continue with other calculations
                                System.err.println("Error calculating distance between " +
                                    languageCodes.get(i) + " and " + languageCodes.get(j) +
                                    ": " + e.getMessage());
                            }
                        }
                    }
                }
            }
            

            int parallelism = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
            ForkJoinPool pool = new ForkJoinPool(parallelism);
            
            try {


                int threshold = Math.max(1, Math.min(5, size / (parallelism * 2)));
                

                pool.invoke(new DistanceMatrixTask(0, size, threshold));
            } finally {

                pool.shutdown();
            }
        } else {

            for (int i = 0; i < size; i++) {
                for (int j = i + 1; j < size; j++) {
                    try {
                        String langCodeA = languageCodes.get(i);
                        String langCodeB = languageCodes.get(j);
                        
                        Language langA = languages.get(langCodeA);
                        Language langB = languages.get(langCodeB);
                        
                        double distance = calculateLanguageDistance(langA, langB);
                        

                        matrix[i][j] = distance;
                        matrix[j][i] = distance;
                        

                        int completed = completedComparisons.incrementAndGet();
                        if (progressListener != null) {
                            int percent = (int)((completed * 100L) / totalComparisons);
                            progressListener.onProgress(percent, langCodeA, langCodeB);
                        }
                    } catch (Exception e) {

                        System.err.println("Error calculating distance between " + 
                            languageCodes.get(i) + " and " + languageCodes.get(j) + 
                            ": " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * Calcula la distancia entre dos llenguates basats en el tipus de calcul espcificat.
     *
     * @param langA Primer Llenguatge
     * @param langB Segon Llenguatge
     * @return Distancia mitjana entre les dues llengües
     */
    private double calculateLanguageDistance(Language langA, Language langB) {

        List<String> wordsA = new ArrayList<>(langA.getWordKeys());
        List<String> wordsB = new ArrayList<>(langB.getWordKeys());
        
        if (useSampling) {

            return calculateLanguageDistanceWithSampling(langA, langB, wordsA, wordsB);
        } else {

            return calculateSampleDistance(langA, langB, wordsA, wordsB);
        }
    }
    
    /**
     * Calcula les distancies amb sampling.
     */
    private double calculateLanguageDistanceWithSampling(Language langA, Language langB, 
                                                      List<String> wordsA, List<String> wordsB) {

        java.util.Random rand = new java.util.Random(langA.getName().hashCode() + langB.getName().hashCode());
        

        java.util.Collections.shuffle(wordsA, rand);
        java.util.Collections.shuffle(wordsB, rand);
        

        long totalWordsA = wordsA.size();
        long totalWordsB = wordsB.size();
        

        long maxUniqueWordsA = Math.min(totalWordsA, this.maxSampleSize);
        long maxUniqueWordsB = Math.min(totalWordsB, this.maxSampleSize);
        

        long sampleSizeA = maxUniqueWordsA / this.numSamples;
        long sampleSizeB = maxUniqueWordsB / this.numSamples;
        

        sampleSizeA = Math.max(sampleSizeA, 10L);
        sampleSizeB = Math.max(sampleSizeB, 10L);
        

        sampleSizeA = Math.min(sampleSizeA, totalWordsA / this.numSamples);
        sampleSizeB = Math.min(sampleSizeB, totalWordsB / this.numSamples);
        

        List<Long> sampleIndices = new ArrayList<>();
        for (long sample = 0; sample < this.numSamples; sample++) {

            if (sample * sampleSizeA >= totalWordsA || sample * sampleSizeB >= totalWordsB) {
                break;
            }
            sampleIndices.add(sample);
        }
        

        if (useParallelization) {

            long finalSampleSizeB = sampleSizeB;
            long finalSampleSizeA = sampleSizeA;
            double[] result = sampleIndices.parallelStream()
                .map(sample -> {

                    int startA = (int)(sample * finalSampleSizeA);
                    int startB = (int)(sample * finalSampleSizeB);
                    

                    int endA = (int)Math.min(startA + finalSampleSizeA, totalWordsA);
                    int endB = (int)Math.min(startB + finalSampleSizeB, totalWordsB);
                    

                    if (endA - startA < 5 || endB - startB < 5) {
                        return new double[] { 0.0, 0.0 }; // [distance, count]
                    }
                    

                    double sampleDistance = calculateSampleDistance(
                            langA, langB, 
                            wordsA.subList(startA, endA),
                            wordsB.subList(startB, endB));
                    
                    return new double[] { sampleDistance, 1.0 };
                })
                .reduce(
                    new double[] { 0.0, 0.0 },
                    (acc, val) -> new double[] { acc[0] + val[0], acc[1] + val[1] }, // accumulator
                    (a, b) -> new double[] { a[0] + b[0], a[1] + b[1] } // combiner
                );
            
            double totalDistance = result[0];
            double totalComparisons = result[1];
            

            return totalComparisons > 0 ? totalDistance / totalComparisons : 1.0;
        } else {

            double totalDistance = 0;
            long totalComparisons = 0;
            
            for (Long sample : sampleIndices) {

                int startA = (int)(sample * sampleSizeA);
                int startB = (int)(sample * sampleSizeB);
                

                int endA = (int)Math.min(startA + sampleSizeA, totalWordsA);
                int endB = (int)Math.min(startB + sampleSizeB, totalWordsB);
                

                if (endA - startA < 5 || endB - startB < 5) continue;
                

                double sampleDistance = calculateSampleDistance(
                        langA, langB, 
                        wordsA.subList(startA, endA),
                        wordsB.subList(startB, endB));
                
                totalDistance += sampleDistance;
                totalComparisons++;
            }
            

            return totalComparisons > 0 ? totalDistance / totalComparisons : 1.0;
        }
    }
    
    /**
     * Calcula la distància entre dues mostres de llengües.
     * 
     * Implementa opcions per utilitzar o desactivar la coincidència de formes
     * normalitzades i el bonus semàntic. Aquesta funció és el nucli del càlcul
     * de distàncies entre llengües, optimitzada per al processament concurrent.
     * 
     * @param langA primera llengua a comparar
     * @param langB segona llengua a comparar
     * @param sampleA mostra de paraules de la llengua A
     * @param sampleB mostra de paraules de la llengua B
     * @return la distància mitjana entre les mostres (0.0 = idèntiques, 1.0 = màximament diferents)
     */
    private double calculateSampleDistance(Language langA, Language langB, 
                                          List<String> sampleA, List<String> sampleB) {
        // Use ConcurrentHashMap for thread-safe access if using parallelization
        final Map<String, Word> sampleBWords = useParallelization ? 
            new ConcurrentHashMap<>() : new java.util.HashMap<>();
        
        // Pre-load all words from sample B to avoid repeated lookups
        for (String wordKeyB : sampleB) {
            Word wordB = langB.getWord(wordKeyB);
            if (wordB != null && wordB.getOriginal().toLowerCase().length() >= 3) {
                sampleBWords.put(wordKeyB, wordB);
            }
        }
        
        // Process sample A
        if (useParallelization) {
            // Parallel stream approach
            double[] result = sampleA.parallelStream()
                .map(langA::getWord)
                .filter(wordA -> wordA != null && wordA.getOriginal().toLowerCase().length() >= 3)
                .map(wordA -> calculateWordDistance(wordA, langB, sampleBWords))
                .reduce(
                    new double[] { 0.0, 0.0 }, // identity: [totalDistance, comparisons]
                    (acc, val) -> new double[] { acc[0] + val[0], acc[1] + val[1] }, // accumulator
                    (a, b) -> new double[] { a[0] + b[0], a[1] + b[1] } // combiner
                );
            
            double totalDistance = result[0];
            double comparisons = result[1];
            
            // If no comparisons were made, languages are maximally distant
            if (comparisons == 0) {
                return 1.0;
            }
            
            // If the average distance is very high (above 0.9), languages are likely unrelated
            double avgDistance = totalDistance / comparisons;
            if (avgDistance > 0.9) {
                return 1.0; // Consider them maximally distant
            }
            
            return avgDistance;
        } else {
            // Sequential approach
            double totalDistance = 0;
            long comparisons = 0;
            
            for (String wordKeyA : sampleA) {
                Word wordA = langA.getWord(wordKeyA);
                if (wordA == null) continue;
                
                String originalA = wordA.getOriginal().toLowerCase();
                
                // Skip very short words to reduce noise
                if (originalA.length() < 3) continue;
                
                double[] result = calculateWordDistance(wordA, langB, sampleBWords);
                totalDistance += result[0];
                comparisons += result[1];
            }
            
            // If no comparisons were made, languages are maximally distant
            if (comparisons == 0) {
                return 1.0;
            }
            
            // If the average distance is very high (above 0.9), languages are likely unrelated
            double avgDistance = totalDistance / (double)comparisons;
            if (avgDistance > 0.9) {
                return 1.0; // Consider them maximally distant
            }
            
            return avgDistance;
        }
    }
    
    /**
     * Calcula la distància per a una sola paraula contra una llengua.
     * 
     * Aquest mètode troba la distància mínima entre una paraula donada i totes
     * les paraules d'una mostra d'una altra llengua. Primer intenta trobar una
     * coincidència de forma normalitzada si està activada aquesta optimització.
     * 
     * @param wordA la paraula a comparar
     * @param langB la llengua contra la qual comparar
     * @param sampleBWords mapa de paraules en la mostra de la llengua B
     * @return array amb [distància, comptador] on la distància és la mínima trobada
     */
    private double[] calculateWordDistance(Word wordA, Language langB, Map<String, Word> sampleBWords) {
        if (useNormalizedFormMatching) {
            // Try to find a match using normalized form first
            String normalizedFormA = StringNormalizer.normalize(wordA.getOriginal());
            
            // Check for normalized form match in langB
            Word wordB_matched = langB.getRepresentativeWordByNormalizedForm(normalizedFormA);
            
            if (wordB_matched != null) {
                // A word in langB normalizes to the same form as wordA
                double normalizedDistance = algorithm.calculateNormalizedDistance(wordA, wordB_matched);
                return new double[] { normalizedDistance, 1.0 }; // [distance, count]
            }
        }
        
        // No normalized match or normalized matching is disabled
        // Find the closest word in sample B
        double minDistance = 1.0;  // Start with maximum distance
        
        if (useParallelization) {
            // Parallel approach to find minimum distance
            minDistance = sampleBWords.values().parallelStream()
                .map(wordB -> calculateWordPairDistance(wordA, wordB))
                .min(Double::compare)
                .orElse(1.0); // Default to max distance if no words to compare
        } else {
            // Sequential approach
            for (Word wordB : sampleBWords.values()) {
                double distance = calculateWordPairDistance(wordA, wordB);
                minDistance = Math.min(minDistance, distance);
            }
        }
        
        return new double[] { minDistance, 1.0 }; // [distance, count]
    }
    
    /**
     * Calcula la distància entre una parella de paraules, aplicant opcionalment el bonus semàntic.
     * 
     * Aquest mètode utilitza l'algorisme de distància seleccionat per calcular la
     * distància bàsica entre dues paraules. Si el bonus semàntic està activat,
     * aplica una reducció del 20% de la distància per a paraules amb glosses coincidents.
     * 
     * @param wordA primera paraula a comparar
     * @param wordB segona paraula a comparar
     * @return la distància calculada entre les dues paraules (0.0 = idèntiques, 1.0 = màximament diferents)
     */
    private double calculateWordPairDistance(Word wordA, Word wordB) {
        if (useSemanticBonus) {
            // Check if glosses match (semantic alignment, if available)
            boolean glossMatch = false;
            if (wordA.getGloss() != null && !wordA.getGloss().isEmpty() && 
                wordB.getGloss() != null && !wordB.getGloss().isEmpty()) {
                glossMatch = wordA.getGloss().equalsIgnoreCase(wordB.getGloss());
            }
            
            // Calculate distance, giving preference to words with matching glosses
            double distance = algorithm.calculateNormalizedDistance(wordA, wordB);
            if (glossMatch) {
                // Apply bonus for semantic matches
                return Math.max(0.0, distance * 0.8); // 20% bonus for semantic matches
            } else {
                return distance;
            }
        } else {
            // No semantic bonus
            return algorithm.calculateNormalizedDistance(wordA, wordB);
        }
    }
    
    /**
     * Interfície per a la notificació de progrés.
     * 
     * Defineix el contracte per rebre actualitzacions del progrés durant
     * el càlcul de la matriu de distàncies.
     */
    public interface ProgressListener {
        /**
         * Cridat quan es fa progrés en el càlcul.
         * 
         * @param percent percentatge de completació (0-100)
         * @param currentLangA llengua A que s'està processant actualment
         * @param currentLangB llengua B que s'està processant actualment
         */
        void onProgress(int percent, String currentLangA, String currentLangB);
    }
}
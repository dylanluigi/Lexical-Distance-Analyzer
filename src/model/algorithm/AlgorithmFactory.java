package model.algorithm;

import model.algorithm.distance.*;

/**
 * Fàbrica per crear instàncies d'algorismes de distància.
 * 
 * Proporciona un punt centralitzat per crear i obtenir algorismes de distància
 * basant-se en el tipus especificat. Utilitza el patró Factory per encapsular
 * la lògica de creació d'objectes i facilitar l'extensibilitat del sistema.
 * 
 * @author Equip ProjecteDiccionaris
 * @version 1.0
 */
public class AlgorithmFactory {
    
    /**
     * Crea un algorisme de distància basat en el tipus especificat.
     * 
     * Utilitza una expressió switch per instanciar l'algorisme apropiat
     * segons el tipus proporcionat. Garanteix que tots els tipus d'algorisme
     * definits a AlgorithmType tenen una implementació corresponent.
     * 
     * @param type tipus d'algorisme a crear
     * @return instància de l'algorisme de distància
     * @throws IllegalArgumentException si el tipus d'algorisme no és compatible
     */
    public static DistanceAlgorithm createAlgorithm(AlgorithmType type) {
        return switch (type) {
            case LEVENSHTEIN -> new LevenshteinDistance();
            case DAMERAU_LEVENSHTEIN -> new DamerauLevenshteinDistance();
            case JARO_WINKLER -> new JaroWinklerDistance();
            case LCS -> new LongestCommonSubsequence();
        };
    }
    
    /**
     * Obté un array de tots els tipus d'algorisme disponibles.
     * 
     * Retorna tots els valors de l'enumeració AlgorithmType, proporcionant
     * una manera convenient d'obtenir la llista completa d'algorismes
     * suportats per l'aplicació.
     * 
     * @return array de tipus d'algorisme disponibles
     */
    public static AlgorithmType[] getAvailableAlgorithms() {
        return AlgorithmType.values();
    }
}
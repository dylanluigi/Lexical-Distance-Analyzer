package model.algorithm;

/**
 * Enumeració per als tipus d'algorismes de distància.
 * 
 * Defineix tots els algorismes de distància disponibles a l'aplicació,
 * incloent-hi el seu nom d'visualització i una descripció detallada de
 * les seves característiques i aplicacions.
 * 
 * @author Equip ProjecteDiccionaris
 * @version 1.0
 */
public enum AlgorithmType {
    /** Algorisme de distància de Levenshtein clàssic */
    LEVENSHTEIN("Levenshtein", "Distància d'edició clàssica que mesura el nombre mínim d'operacions d'un sol caràcter necessaries per transformar una cadena en una altra."),
    
    /** Algorisme de distància de Damerau-Levenshtein amb transposicions */
    DAMERAU_LEVENSHTEIN("Damerau-Levenshtein", "Extensió de la distància de Levenshtein que també considera les transposicions de caràcters adjacents com una sola operació d'edició."),
    
    /** Algorisme de distància de Jaro-Winkler amb bonus per prefixos */
    JARO_WINKLER("Jaro-Winkler", "Dona més pes als prefixos comuns, fent-lo adequat per comparar paraules en llengües relacionades on els prefixos sovint es conserven."),
    
    /** Algorisme basat en la Subseqüència Comú Més Llarga */
    LCS("Longest Common Subsequence", "Troba la seqüència més llarga de caràcters que apareixen en el mateix ordre en ambdues cadenes, centrant-se en patrons de caràcters compartits independentment d'insercions o supressions.");
    
    private final String name;
    private final String description;
    
    /**
     * Constructor per als valors de l'enumeració.
     * 
     * @param name nom d'visualització de l'algorisme
     * @param description descripció detallada de l'algorisme
     */
    AlgorithmType(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    /**
     * Obté el nom d'visualització de l'algorisme.
     * 
     * @return nom de l'algorisme
     */
    public String getName() {
        return name;
    }
    
    /**
     * Obté la descripció de l'algorisme.
     * 
     * Proporciona una explicació detallada de com funciona l'algorisme
     * i quines són les seves característiques principals.
     * 
     * @return descripció de l'algorisme
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Obté la representació en cadena de l'algorisme.
     * 
     * @return nom de l'algorisme
     */
    @Override
    public String toString() {
        return name;
    }
}
package model.algorithm.distance;

import model.dictionary.Word;

/**
 * Interfície base per a tots els algorismes de càlcul de distàncies.
 * 
 * Defineix el contracte que han de complir tots els algorismes de distància
 * utilitzats per comparar paraules en l'aplicació. Proporciona mètodes per
 * calcular tant distàncies absolutes com normalitzades.
 * 
 * @author Equip ProjecteDiccionaris
 * @version 1.0
 */
public interface DistanceAlgorithm {
    
    /**
     * Calcula la distància entre dues paraules.
     * 
     * Aquest mètode calcula la distància absoluta segons l'algorisme específic.
     * El valor retornat depèn de la implementació concreta de l'algorisme.
     * 
     * @param word1 primera paraula
     * @param word2 segona paraula
     * @return distància entre les paraules
     */
    double calculateDistance(Word word1, Word word2);
    
    /**
     * Calcula la distància normalitzada entre dues paraules.
     * 
     * El resultat és un valor entre 0 i 1, on 0 significa paraules idèntiques
     * i 1 significa la distància màxima possible. Aquesta normalització permet
     * comparar distàncies entre paraules de longituds diferents de manera
     * equitativa.
     * 
     * @param word1 primera paraula
     * @param word2 segona paraula
     * @return distància normalitzada (entre 0 i 1)
     */
    double calculateNormalizedDistance(Word word1, Word word2);
    
    /**
     * Obté el nom de l'algorisme.
     * 
     * @return nom de l'algorisme
     */
    String getName();
    
    /**
     * Obté la descripció de l'algorisme.
     * 
     * Proporciona una descripció detallada del funcionament i característiques
     * de l'algorisme per a la interfície d'usuari.
     * 
     * @return descripció de l'algorisme
     */
    String getDescription();
}
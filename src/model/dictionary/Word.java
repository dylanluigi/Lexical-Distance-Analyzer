package model.dictionary;

/**
 * Representa una paraula en una llengua específica amb el seu significat.
 * 
 * Aquesta classe immutable encapsula una paraula original i la seva glossa
 * (traducció o significat). S'utilitza com a unitat bàsica per als càlculs
 * de distància entre paraules de diferents llengües.
 * 
 * @author Equip ProjecteDiccionaris
 * @version 1.0
 */
public class Word {
    private final String original;
    private final String gloss;
    
    /**
     * Constructor per crear una nova paraula.
     * 
     * @param original forma original de la paraula
     * @param gloss glossa o significat de la paraula (pot ser null)
     */
    public Word(String original, String gloss) {
        this.original = original;
        this.gloss = gloss;
    }
    
    /**
     * Obté la forma original de la paraula.
     * 
     * @return forma original de la paraula
     */
    public String getOriginal() {
        return original;
    }
    
    /**
     * Obté la glossa (traducció o significat) de la paraula.
     * 
     * @return glossa de la paraula, o null si no està disponible
     */
    public String getGloss() {
        return gloss;
    }
    
    /**
     * Comprova la igualtat entre dues paraules.
     * 
     * Dues paraules es consideren iguals si tenen la mateixa forma original,
     * independentment de la seva glossa.
     * 
     * @param o objecte a comparar
     * @return true si les paraules són iguals, false en cas contrari
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Word word = (Word) o;
        return original.equals(word.original);
    }
    
    /**
     * Calcula el codi hash de la paraula.
     * 
     * El codi hash es basa únicament en la forma original de la paraula.
     * 
     * @return codi hash de la paraula
     */
    @Override
    public int hashCode() {
        return original.hashCode();
    }
    
    /**
     * Retorna una representació en cadena de la paraula.
     * 
     * Inclou la forma original i, si està disponible, la glossa entre parèntesis.
     * 
     * @return representació en cadena de la paraula
     */
    @Override
    public String toString() {
        return original + (gloss != null && !gloss.isEmpty() ? " (" + gloss + ")" : "");
    }
}
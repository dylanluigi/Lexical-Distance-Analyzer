package model.dictionary;

import java.util.*;

/**
 * Representa una llengua amb el seu vocabulari.
 * 
 * Aquesta classe encapsula el vocabulari d'una llengua específica, proporcionant
 * mètodes per afegir paraules, cercar-les i indexar-les de diverses maneres.
 * Manté dos índexs: un per la forma original de les paraules i un altre per
 * les formes normalitzades per facilitar la comparació entre llengües.
 * 
 * @author Equip ProjecteDiccionaris
 * @version 1.0
 */
public class Language {
    private final String code;
    private final String name;
    private final Map<String, Word> vocabulary;
    private final Map<String, List<Word>> normalizedFormIndex;
    
    /**
     * Constructor per crear una nova llengua.
     * 
     * Inicialitza el vocabulari i l'índex de formes normalitzades buits.
     * 
     * @param code codi de la llengua (ex: "EN", "ES", "CA")
     * @param name nom complet de la llengua
     */
    public Language(String code, String name) {
        this.code = code;
        this.name = name;
        this.vocabulary = new HashMap<>();
        this.normalizedFormIndex = new HashMap<>();
    }
    
    /**
     * Obté el codi de la llengua.
     * 
     * @return codi de la llengua
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Obté el nom de la llengua.
     * 
     * @return nom complet de la llengua
     */
    public String getName() {
        return name;
    }

    /**
     * Afegeix una paraula al vocabulari de la llengua.
     * 
     * Utilitza la forma original com a clau per preservar paraules diferents.
     * També indexa per forma normalitzada per facilitar la comparació entre llengües.
     * Les paraules nul·les o buides s'ignoren.
     * 
     * @param word paraula a afegir
     */
    public void addWord(Word word) {
        if (word == null || word.getOriginal() == null || word.getOriginal().isEmpty()) {
            return;
        }
        
        // Use original string as the primary key
        String originalKey = word.getOriginal();
        
        // Add to primary vocabulary (keyed by original string)
        // This ensures "café" and "cafe" are stored as distinct entries
        vocabulary.put(originalKey, word);
        
        // Add to the normalized form index for cross-language matching
        String normalizedKey = StringNormalizer.normalize(originalKey);
        normalizedFormIndex.computeIfAbsent(normalizedKey, k -> new ArrayList<>()).add(word);
    }
    
    /**
     * Obté una paraula del vocabulari de la llengua per la seva forma original.
     * 
     * @param original forma original de la paraula
     * @return objecte Word, o null si no es troba
     */
    public Word getWord(String original) {
        if (original == null) return null;
        return vocabulary.get(original);
    }
    
    /**
     * Comprova si la llengua conté una paraula amb la forma original donada.
     * 
     * @param original forma original de la paraula
     * @return true si la paraula existeix al vocabulari
     */
    public boolean containsKey(String original) {
        if (original == null) return false;
        return vocabulary.containsKey(original);
    }

    /**
     * Obté el conjunt de claus de paraules (formes originals) d'aquest vocabulari.
     * 
     * @return conjunt no modificable de formes originals de paraules
     */
    public Set<String> getWordKeys() {
        return Collections.unmodifiableSet(vocabulary.keySet());
    }
    
    /**
     * Troba una paraula representativa d'aquesta llengua la forma original de la qual
     * es normalitza a la cadena normalitzada donada.
     * 
     * Si múltiples paraules originals es normalitzen a la mateixa cadena
     * (ex: "Strasse", "straße"), aquest mètode retorna la primera trobada
     * durant el procés d'indexació.
     *
     * @param normalizedForm cadena normalitzada a cercar
     * @return objecte Word si es troba una coincidència, altrament null
     */
    public Word getRepresentativeWordByNormalizedForm(String normalizedForm) {
        if (normalizedForm == null || normalizedForm.isEmpty()) {
            return null;
        }
        List<Word> matchingWords = normalizedFormIndex.get(normalizedForm);
        if (matchingWords != null && !matchingWords.isEmpty()) {
            return matchingWords.get(0); // Return the first (representative) word
        }
        return null;
    }
    
    /**
     * Obté totes les paraules que comparteixen la mateixa forma normalitzada.
     *
     * @param normalizedForm cadena normalitzada a cercar
     * @return llista d'objectes Word amb la mateixa forma normalitzada, o llista buida si no se'n troba cap
     */
    public List<Word> getWordsByNormalizedForm(String normalizedForm) {
        if (normalizedForm == null || normalizedForm.isEmpty()) {
            return Collections.emptyList();
        }
        List<Word> matchingWords = normalizedFormIndex.get(normalizedForm);
        if (matchingWords == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(matchingWords);
    }
    
    /**
     * Comprova si la llengua conté alguna paraula que es normalitzi a la forma donada.
     * 
     * @param original paraula a normalitzar i cercar
     * @return true si alguna paraula del vocabulari es normalitza a aquesta forma
     */
    public boolean containsNormalizedForm(String original) {
        if (original == null) return false;
        String normalizedKey = StringNormalizer.normalize(original);
        return normalizedFormIndex.containsKey(normalizedKey);
    }
    
    /**
     * Crea un índex de paraules per la seva glossa (significat/traducció).
     * 
     * Això permet trobar paraules amb el mateix significat entre llengües.
     * Les glosses es normalitzen (trim i minúscules) per millorar la comparació.
     * 
     * @return mapa de glossa a llista de paraules que tenen aquesta glossa
     */
    public Map<String, List<Word>> indexByGloss() {
        Map<String, List<Word>> glossIndex = new HashMap<>();
        
        for (Word word : vocabulary.values()) {
            if (word.getGloss() != null && !word.getGloss().isEmpty()) {
                // Normalize the gloss by trimming and converting to lowercase
                String normalizedGloss = word.getGloss().trim().toLowerCase();
                
                // Skip empty glosses
                if (normalizedGloss.isEmpty()) continue;
                
                // Add the word to the list for this gloss
                glossIndex.computeIfAbsent(normalizedGloss, k -> new ArrayList<>()).add(word);
            }
        }
        
        return glossIndex;
    }
    
    /**
     * Obté el nombre de paraules originals diferents del vocabulari.
     * 
     * @return mida del vocabulari
     */
    public int getVocabularySize() {
        return vocabulary.size();
    }
    
    /**
     * Comprova la igualtat entre dues llengües.
     * 
     * Dues llengües es consideren iguals si tenen el mateix codi.
     * 
     * @param o objecte a comparar
     * @return true si les llengües són iguals, false en cas contrari
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Language language = (Language) o;
        return code.equals(language.code);
    }
    
    /**
     * Calcula el codi hash de la llengua.
     * 
     * El codi hash es basa en el codi de la llengua.
     * 
     * @return codi hash de la llengua
     */
    @Override
    public int hashCode() {
        return code.hashCode();
    }
    
    /**
     * Retorna una representació en cadena de la llengua.
     * 
     * Format: "nom (codi)"
     * 
     * @return representació en cadena de la llengua
     */
    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}
package model.dictionary;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Classe d'utilitat per normalitzar cadenes i millorar la comparació de paraules entre llengües.
 * 
 * Proporciona mètodes estàtics per normalitzar cadenes eliminant diacrítics,
 * convertint a minúscules i altres transformacions que faciliten la comparació
 * de paraules similars entre diferents llengües.
 * 
 * @author Equip ProjecteDiccionaris
 * @version 1.0
 */
public class StringNormalizer {
    
    private static final Pattern DIACRITICS_AND_FRIENDS = Pattern.compile("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+");
    
    /**
     * Normalitza una cadena mitjançant:
     * 1. Conversió a minúscules
     * 2. Eliminació de marques diacrítiques (accents, dièresis, etc.)
     * 
     * Utilitza la normalització Unicode NFD (Canonical Decomposition) per separar
     * els caràcters base dels seus diacrítics i després elimina aquests últims.
     * 
     * @param input cadena a normalitzar
     * @return cadena normalitzada
     */
    public static String normalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // First convert to lowercase
        String lowercase = input.toLowerCase();
        
        // Then normalize and remove diacritical marks
        String normalized = Normalizer.normalize(lowercase, Normalizer.Form.NFD);
        return DIACRITICS_AND_FRIENDS.matcher(normalized).replaceAll("");
    }
    
    /**
     * Comprova si dues cadenes són similars després de la normalització.
     * 
     * Normalitza ambdues cadenes i les compara per igualtat. Això permet
     * identificar paraules que són essencialment iguals però difereixen
     * en l'ús d'accents o majúscules.
     * 
     * @param str1 primera cadena
     * @param str2 segona cadena
     * @return true si les versions normalitzades són iguals
     */
    public static boolean isSimilarAfterNormalization(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return str1 == str2; // Both null = true, one null = false
        }
        
        return normalize(str1).equals(normalize(str2));
    }
}
package model.dictionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsable de carregar fitxers de diccionari des del disc.
 * 
 * Aquesta classe proporciona funcionalitat per carregar diccionaris de diversos
 * formats (.txt, .csv, .dic) i detectar automàticament els codis de llengua
 * a partir dels noms dels fitxers. Aplica normalització Unicode i gestió
 * de codificacions per garantir la compatibilitat amb diferents llengües.
 * 
 * @author Equip ProjecteDiccionaris
 * @version 1.0
 */
public class FileLoader {

    /**
     * Carrega un diccionari des d'un fitxer.
     * 
     * Suporta múltiples formats de fitxer incloent .txt, .csv i .dic.
     * Detecta automàticament el format i aplica el mètode de càrrega apropiat.
     *
     * @param filePath ruta al fitxer de diccionari
     * @param langCode codi de la llengua
     * @param langName nom de la llengua
     * @return objecte Language que conté les paraules carregades
     * @throws IOException si hi ha un error llegint el fitxer
     */
    public Language loadDictionary(Path filePath, String langCode, String langName) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".dic")) {
            return loadDicDictionary(filePath, langCode, langName);
        } else {
            return loadTextDictionary(filePath, langCode, langName);
        }
    }

    /**
     * Carrega un diccionari des d'un fitxer de text (txt, csv).
     * 
     * Suporta múltiples formats de separador per a major flexibilitat.
     * Reconeix tabuladors, comes i punts i comes com a separadors.
     * Aplica normalització a les paraules i ignora línies buides i comentaris.
     *
     * @param filePath ruta al fitxer de diccionari
     * @param langCode codi de la llengua
     * @param langName nom de la llengua
     * @return objecte Language que conté les paraules carregades
     * @throws IOException si hi ha un error llegint el fitxer
     */
    private Language loadTextDictionary(Path filePath, String langCode, String langName) throws IOException {
        Language language = new Language(langCode, langName);

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                // Try different separators (tab, comma, semicolon)
                String[] parts;
                if (line.contains("\t")) {
                    parts = line.split("\\t", 2);
                } else if (line.contains(";")) {
                    parts = line.split(";", 2);
                } else if (line.contains(",")) {
                    parts = line.split(",", 2);
                } else {
                    parts = new String[]{line};
                }

                String rawWord = parts[0].trim();
                if (rawWord.isEmpty()) continue;
                String word = normalizeWord(rawWord);
                String gloss = parts.length > 1 ? parts[1].trim() : "";

                language.addWord(new Word(word, gloss));
            }
        }

        return language;
    }

    /**
     * Carrega un diccionari des d'un fitxer .dic.
     * 
     * Gestiona el format especial dels fitxers .dic que pot incloure metadades
     * i anotacions. Utilitza codificació UTF-8 i anàlisi robusta de capçaleres.
     * Aplica normalització a les paraules i filtra entrades no vàlides.
     *
     * @param filePath ruta al fitxer de diccionari
     * @param langCode codi de la llengua
     * @param langName nom de la llengua
     * @return objecte Language que conté les paraules carregades
     * @throws IOException si hi ha un error llegint el fitxer
     */
    private Language loadDicDictionary(Path filePath, String langCode, String langName) throws IOException {
        Language language = new Language(langCode, langName);
        Charset dicCharset = StandardCharsets.UTF_8; // Use UTF-8 for better compatibility

        try (BufferedReader reader = Files.newBufferedReader(filePath, dicCharset)) {
            String line;
            boolean inHeader = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (inHeader) {
                    // Detect entry count line (just a number on the first line)
                    if (line.matches("\\d+")) {
                        inHeader = false;
                    }
                    continue;
                }

                // Skip single-letter section markers (e.g., "A" or "a")
                if (line.length() == 1 && Character.isLetter(line.charAt(0))) {
                    continue;
                }

                // Process valid entries: handle Unicode letters and various formats
                // Skip single-letter section markers and numbers
                if (line.length() > 1 && !line.matches("\\d+.*")) {
                    String[] parts = line.split("/", 2);
                    String rawWord = parts[0].trim();
                    String metadata = parts.length > 1 ? parts[1].trim() : "";
                    
                    // Filter out entries that are just numbers or punctuation
                    if (rawWord.matches(".*[\\p{L}]{2,}.*")) {
                        String word = normalizeWord(rawWord);
                        if (!word.isEmpty() && word.length() > 1) {
                            language.addWord(new Word(word, metadata));
                        }
                    }
                }
            }
        }

        return language;
    }

    /**
     * Normalitza una paraula: minúscules, preserva accents, col·lapsa geminació catalana.
     * 
     * Elimina els marcadors de geminació catalans ('·' i '.'), converteix a minúscules
     * i normalitza a NFC per preservar els accents correctament.
     * 
     * @param word paraula a normalitzar
     * @return paraula normalitzada
     */
    private String normalizeWord(String word) {
        // Collapse Catalan gemination markers '·' and '.' into nothing
        String collapsed = word.replace("·", "").replace(".", "");
        // Lowercase and normalize to NFC to preserve accents
        return Normalizer.normalize(collapsed.toLowerCase(), Form.NFC);
    }

    /**
     * Carrega múltiples diccionaris des de fitxers.
     * 
     * Processa cada fitxer del mapa proporcionat i crea un mapa de llengües
     * resultant. Si algun fitxer falla, es propaga l'excepció.
     *
     * @param filePaths mapa de rutes de fitxers a codis i noms de llengües
     * @return mapa de codis de llengua a objectes Language
     * @throws IOException si hi ha un error llegint algun dels fitxers
     */
    public Map<String, Language> loadDictionaries(Map<Path, LanguageInfo> filePaths) throws IOException {
        Map<String, Language> languages = new HashMap<>();

        for (Map.Entry<Path, LanguageInfo> entry : filePaths.entrySet()) {
            Path filePath = entry.getKey();
            LanguageInfo info = entry.getValue();
            Language language = loadDictionary(filePath, info.code(), info.name());
            languages.put(language.getCode(), language);
        }

        return languages;
    }

    /**
     * Detecta el codi de llengua a partir del nom d'un fitxer.
     * 
     * Utilitza una heurística simple que cerca codis de llengua comuns
     * al nom del fitxer. Suporta variants regionals i dialectals de llengues
     * principals com anglès, alemany, portuguès, armenì, noruec, serbi i romanès.
     *
     * @param filePath ruta al fitxer de diccionari
     * @return objecte LanguageInfo amb el codi de llengua detectat i el nom
     */
    public LanguageInfo detectLanguageFromFileName(Path filePath) {
        String fileName = filePath.getFileName().toString();

        if (fileName.contains(".")) {
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        }

        // Better language detection based on common patterns
        String code;
        String name = fileName;
        
        // Handle specific patterns in the filename
        if (fileName.toLowerCase().contains("english")) {
            if (fileName.contains("American")) code = "EN_US";
            else if (fileName.contains("British")) code = "EN_GB";
            else if (fileName.contains("Australian")) code = "EN_AU";
            else if (fileName.contains("Canadian")) code = "EN_CA";
            else if (fileName.contains("South African")) code = "EN_ZA";
            else code = "EN";
        } else if (fileName.toLowerCase().contains("german")) {
            if (fileName.contains("de_AT")) code = "DE_AT";
            else if (fileName.contains("de_CH")) code = "DE_CH";
            else if (fileName.contains("de_DE")) code = "DE_DE";
            else if (fileName.contains("OLDSPELL")) code = "DE_OLD";
            else code = "DE";
        } else if (fileName.toLowerCase().contains("portuguese")) {
            if (fileName.contains("Brazilian")) code = "PT_BR";
            else if (fileName.contains("European")) {
                if (fileName.contains("Before OA")) code = "PT_EU_OLD";
                else code = "PT_EU";
            } else code = "PT";
        } else if (fileName.toLowerCase().contains("armenian")) {
            if (fileName.contains("Eastern")) code = "HY_EA";
            else if (fileName.contains("Western")) code = "HY_WE";
            else code = "HY";
        } else if (fileName.toLowerCase().contains("norwegian")) {
            if (fileName.contains("Bokmal")) code = "NO_BO";
            else if (fileName.contains("Nynorsk")) code = "NO_NY";
            else code = "NO";
        } else if (fileName.toLowerCase().contains("serbian")) {
            if (fileName.contains("Cyrillic")) code = "SR_CY";
            else if (fileName.contains("Latin")) code = "SR_LA";
            else code = "SR";
        } else if (fileName.toLowerCase().contains("romanian")) {
            if (fileName.contains("Ante1993")) code = "RO_OLD";
            else if (fileName.contains("Modern")) code = "RO_MOD";
            else code = "RO";
        } else {
            // For other languages, try to extract a meaningful code
            String cleanName = fileName.replaceAll("[^a-zA-Z]", "");
            if (cleanName.length() >= 3) {
                code = cleanName.substring(0, Math.min(3, cleanName.length())).toUpperCase();
            } else {
                code = cleanName.toUpperCase();
            }
        }

        return new LanguageInfo(code, name);
    }

    /**
     * Registre per contenir codi de llengua i nom.
     * 
     * @param code codi de la llengua
     * @param name nom de la llengua
     */
    public record LanguageInfo(String code, String name) {}
}
//package model.algorithm.distance;
//
//import model.dictionary.Word;
//
///**
// * Implementation of the Jaro-Winkler distance algorithm.
// * This algorithm gives more weight to common prefixes, which is useful for comparing
// * related languages where words often share prefixes.
// */
//public class JaroWinklerDistance implements DistanceAlgorithm {
//
//    private static final double DEFAULT_SCALING_FACTOR = 0.1;
//    private static final int MAX_PREFIX_LENGTH = 4;
//    private static final double SCALING_FACTOR = 0.1;
//    private static final double PREFIX_THRESHOLD = 0.7;
//
//    @Override
//    public double calculateDistance(Word w1, Word w2) {
//        String s1 = w1 == null || w1.getOriginal() == null ? "" : w1.getOriginal().toLowerCase();
//        String s2 = w2 == null || w2.getOriginal() == null ? "" : w2.getOriginal().toLowerCase();
//        double sim = calculateSimilarity(s1, s2);
//        System.out.println(s1 + " " + s2 + " " + sim);
//        return 1.0 - sim;
//    }
//
//    @Override
//    public double calculateNormalizedDistance(Word w1, Word w2) {
//        return calculateDistance(w1, w2);
//    }
//
//    private double calculateSimilarity(String s1, String s2) {
//        double j = calculateJaroSimilarity(s1, s2);
//        int prefix = commonPrefixLength(s1, s2);
//        if (j > PREFIX_THRESHOLD && prefix > 0) {
//            j += prefix * SCALING_FACTOR * (1 - j);
//        }
//        return j;
//    }
//
//    private int commonPrefixLength(String s1, String s2) {
//        int n = Math.min(Math.min(s1.length(), s2.length()), MAX_PREFIX_LENGTH);
//        int i = 0;
//        while (i < n && s1.charAt(i) == s2.charAt(i)) i++;
//        return i;
//    }
//
//    @Override
//    public String getName() {
//        return "Jaro-Winkler";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Gives more weight to common prefixes, making it suitable for comparing words in related languages where prefixes are often preserved.";
//    }
//
//
//    /**
//     * Calculates the Jaro similarity between two strings.
//     *
//     * @param s1 First string
//     * @param s2 Second string
//     * @return The Jaro similarity (between 0 and 1)
//     */
//    private double calculateJaroSimilarity(String s1, String s2) {
//        int len1 = s1.length(), len2 = s2.length();
//        if (len1 == 0 && len2 == 0) return 1.0;
//        if (len1 == 0 || len2 == 0) return 0.0;
//        int matchWindow = Math.max(len1, len2) / 2 - 1;
//        boolean[] match1 = new boolean[len1];
//        boolean[] match2 = new boolean[len2];
//        int matches = 0;
//        for (int i = 0; i < len1; i++) {
//            int start = Math.max(0, i - matchWindow);
//            int end = Math.min(len2 - 1, i + matchWindow);
//            for (int j = start; j <= end; j++) {
//                if (!match2[j] && s1.charAt(i) == s2.charAt(j)) {
//                    match1[i] = match2[j] = true;
//                    matches++;
//                    break;
//                }
//            }
//        }
//        if (matches == 0) return 0.0;
//        int t = 0;
//        for (int i = 0, j = 0; i < len1; i++) {
//            if (match1[i]) {
//                while (!match2[j]) j++;
//                if (s1.charAt(i) != s2.charAt(j)) t++;
//                j++;
//            }
//        }
//        double transpositions = t / 2.0;
//        return (
//                (matches / (double) len1) +
//                        (matches / (double) len2) +
//                        ((matches - transpositions) / matches)
//        ) / 3.0;
//    }
//}

package model.algorithm.distance;

import model.dictionary.Word;

/**
 * Implementació de l'algorisme de distància de Jaro-Winkler.
 * 
 * Calcula la distància entre dues cadenes en el rang [0,1], donant més pes
 * als prefixos comuns. Això el fa especialment adequat per comparar paraules
 * en llengües relacionades on els prefixos sovint es conserven.
 * 
 * L'algorisme es basa en la similitud de Jaro i aplica un bonus de Winkler
 * per als prefixos comuns quan la similitud de Jaro supera un llindar.
 * 
 * Paràmetres utilitzats:
 * - P = 0.25: factor d'escala per al bonus de prefix
 * - PREFIX_MAX = 4: longitud màxima del prefix considerat
 * - BOOST_THRESHOLD = 0.7: llindar per aplicar el bonus de Winkler
 * 
 * @author Equip ProjecteDiccionaris
 * @version 1.0
 */
public class JaroWinklerDistance implements DistanceAlgorithm {

    private static final double P = 0.25;          // 0 < P ≤ 0.25
    private static final int    PREFIX_MAX = 4;
    private static final double BOOST_THRESHOLD = 0.7;

    /**
     * Calcula la distància de Jaro-Winkler entre dues paraules.
     * 
     * Primer calcula la similitud de Jaro i després aplica el bonus de Winkler
     * si la similitud supera el llindar i hi ha un prefix comú. El resultat
     * final es converteix a distància (1 - similitud).
     * 
     * @param w1 primera paraula a comparar (pot ser null)
     * @param w2 segona paraula a comparar (pot ser null)
     * @return distància de Jaro-Winkler (0-1)
     */
    @Override
    public double calculateDistance(Word w1, Word w2) {
        String s1 = w1 == null ? "" : w1.getOriginal().toLowerCase();
        String s2 = w2 == null ? "" : w2.getOriginal().toLowerCase();

        if (s1.equals(s2)) return 0.0;             // exact match
        if (s1.isEmpty() || s2.isEmpty()) return 1.0;

        double jaro = jaroSimilarity(s1, s2);
        int prefix = commonPrefixLength(s1, s2);

        if (jaro > BOOST_THRESHOLD && prefix > 0) {
            jaro += prefix * P * (1.0 - jaro);     // Winkler boost
        }
        return 1.0 - jaro;                         // convert to distance
    }

    /**
     * Calcula la distància de Jaro-Winkler normalitzada entre dues paraules.
     * 
     * Com que Jaro-Winkler ja retorna valors normalitzats (0-1), aquest mètode
     * simplement crida calculateDistance sense cap transformació addicional.
     * 
     * @param w1 primera paraula a comparar
     * @param w2 segona paraula a comparar
     * @return distància normalitzada (0-1)
     */
    @Override
    public double calculateNormalizedDistance(Word w1, Word w2) {
        return calculateDistance(w1, w2);
    }

    // ---------- mètodes auxiliars ----------

    /**
     * Calcula la longitud del prefix comú entre dues cadenes.
     * 
     * Compara caràcter per caràcter des de l'inici fins trobar una differència
     * o arribar al límit màxim de longitud del prefix.
     * 
     * @param a primera cadena
     * @param b segona cadena
     * @return longitud del prefix comú (màxim PREFIX_MAX)
     */
    private static int commonPrefixLength(String a, String b) {
        int n = Math.min(Math.min(a.length(), b.length()), PREFIX_MAX);
        int i = 0;
        while (i < n && a.charAt(i) == b.charAt(i)) i++;
        return i;
    }

    /**
     * Calcula la similitud de Jaro entre dues cadenes.
     * 
     * La similitud de Jaro es basa en el nombre de caràcters coincidents
     * i transposicions dins d'una finestra de coincidència definida.
     * 
     * @param s1 primera cadena
     * @param s2 segona cadena
     * @return similitud de Jaro (0-1)
     */
    private static double jaroSimilarity(String s1, String s2) {
        int n1 = s1.length(), n2 = s2.length();
        int window = Math.max(Math.max(n1, n2) / 2 - 1, 0);

        boolean[] m1 = new boolean[n1];
        boolean[] m2 = new boolean[n2];
        int matches = 0;

        // pass 1: find matches
        for (int i = 0; i < n1; i++) {
            int start = Math.max(0, i - window);
            int end   = Math.min(n2 - 1, i + window);
            for (int j = start; j <= end; j++) {
                if (!m2[j] && s1.charAt(i) == s2.charAt(j)) {
                    m1[i] = m2[j] = true;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 0) return 0.0;

        // pass 2: count transpositions
        int t = 0, j = 0;
        for (int i = 0; i < n1; i++) {
            if (m1[i]) {
                while (!m2[j]) j++;
                if (s1.charAt(i) != s2.charAt(j)) t++;
                j++;
            }
        }
        double transpositions = t / 2.0;
        return ( (matches / (double) n1)
                + (matches / (double) n2)
                + (matches - transpositions) / (double) matches ) / 3.0;
    }

    /**
     * Obté el nom de l'algorisme.
     * 
     * @return nom de l'algorisme
     */
    @Override public String getName()        { return "Jaro-Winkler"; }
    
    /**
     * Obté la descripció de l'algorisme.
     * 
     * @return descripció detallada de l'algorisme
     */
    @Override public String getDescription() { return "Distància que dona bonus als prefixos comuns (p = 0.25, ℓ ≤ 4)."; }
}

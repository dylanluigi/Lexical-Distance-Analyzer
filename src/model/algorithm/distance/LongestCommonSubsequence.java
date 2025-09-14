//package model.algorithm.distance;
//
//import model.dictionary.Word;
//
///**
// * Implementation of the Longest Common Subsequence (LCS) distance algorithm.
// * This algorithm finds the longest sequence of characters that appear in the same order
// * in both strings, but not necessarily consecutively.
// */
//public class LongestCommonSubsequence implements DistanceAlgorithm {
//
//    @Override
//    public double calculateDistance(Word w1, Word w2) {
//        String s1 = w1 == null || w1.getOriginal() == null ? "" : w1.getOriginal();
//        String s2 = w2 == null || w2.getOriginal() == null ? "" : w2.getOriginal();
//        int len1 = s1.length(), len2 = s2.length();
//        if (len1 == 0 || len2 == 0) return len1 + len2;
//        // Ensure s1 is the shorter string
//        if (len1 > len2) {
//            String tmp = s1; s1 = s2; s2 = tmp;
//            int tmpLen = len1; len1 = len2; len2 = tmpLen;
//        }
//        int[] prev = new int[len1 + 1];
//        int[] curr = new int[len1 + 1];
//        for (int j = 1; j <= len2; j++) {
//            for (int i = 1; i <= len1; i++) {
//                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
//                    curr[i] = prev[i - 1] + 1;
//                } else {
//                    curr[i] = Math.max(prev[i], curr[i - 1]);
//                }
//            }
//            int[] tmp = prev; prev = curr; curr = tmp;
//        }
//        int lcs = prev[len1];
//        System.out.println(s1 + " " + s2 + " " + lcs);
//        return (len1 + len2) - 2 * lcs;
//    }
//    @Override
//    public double calculateNormalizedDistance(Word w1, Word w2) {
//        double d = calculateDistance(w1, w2);
//        int total = (
//                (w1 == null || w1.getOriginal() == null ? 0 : w1.getOriginal().length()) +
//                        (w2 == null || w2.getOriginal() == null ? 0 : w2.getOriginal().length())
//        );
//        return total == 0 ? 0 : 2 * d / (d + total);
//    }
//    @Override
//    public String getName() {
//        return "LCS";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Finds the longest sequence of characters that appear in the same order in both strings, focusing on shared character patterns regardless of insertions or deletions.";
//    }
//
//    /**
//     * Calculates the length of the longest common subsequence of two strings.
//     *
//     * @param str1 First string
//     * @param str2 Second string
//     * @return The length of the LCS
//     */
//    private int lcs(String str1, String str2) {
//        int m = str1.length();
//        int n = str2.length();
//
//        int[][] dp = new int[m + 1][n + 1];
//
//        for (int i = 0; i <= m; i++) {
//            for (int j = 0; j <= n; j++) {
//                if (i == 0 || j == 0) {
//                    dp[i][j] = 0;
//                } else if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
//                    dp[i][j] = dp[i - 1][j - 1] + 1;
//                } else {
//                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
//                }
//            }
//        }
//
//        return dp[m][n];
//    }
//}
package model.algorithm.distance;

import model.dictionary.Word;

/**
 * Implementació de l'algorisme de distància basat en la Subseqüència Comú Més Llarga (LCS).
 * 
 * Calcula la distància entre dues paraules basant-se en la longitud de la subseqüència
 * comú més llarga. Una subseqüència és una seqüència de caràcters que apareixen
 * en el mateix ordre en ambdues cadenes, però no necessariament consecutius.
 * 
 * La distància es calcula com: (longitud1 + longitud2) - 2 * LCS
 * Això representa el nombre d'operacions d'inserció i supressió necessaries
 * per transformar una cadena en l'altra.
 * 
 * L'algorisme utilitza programació dinàmica optimitzada en espai, utilitzant
 * només dues files en lloc de la matriu completa per reduir l'ús de memòria.
 * 
 * @author Equip ProjecteDiccionaris
 * @version 1.0
 */
public class LongestCommonSubsequence implements DistanceAlgorithm {

    /**
     * Calcula la distància basada en LCS entre dues paraules.
     * 
     * Utilitza programació dinàmica optimitzada per trobar la longitud de la
     * subseqüència comú més llarga i calcula la distància en base a aquesta.
     * S'assegura que la cadena més curta sigui sempre al bucle interior per
     * optimitzar l'ús de memòria.
     * 
     * @param w1 primera paraula a comparar (pot ser null)
     * @param w2 segona paraula a comparar (pot ser null)
     * @return distància basada en LCS
     */
    @Override
    public double calculateDistance(Word w1, Word w2) {
        String s1 = w1 == null || w1.getOriginal() == null ? "" : w1.getOriginal().toLowerCase();
        String s2 = w2 == null || w2.getOriginal() == null ? "" : w2.getOriginal().toLowerCase();

        // Fast paths
        if (s1.equals(s2))               return 0;
        if (s1.isEmpty())                return s2.length();
        if (s2.isEmpty())                return s1.length();

        // Ensure s1 is the shorter string to minimise memory
        if (s1.length() > s2.length()) { String tmp = s1; s1 = s2; s2 = tmp; }
        int m = s1.length(), n = s2.length();

        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];

        for (int j = 1; j <= n; j++) {
            curr[0] = 0;                     // first column is always 0
            char cj = s2.charAt(j - 1);
            for (int i = 1; i <= m; i++) {
                if (s1.charAt(i - 1) == cj) {
                    curr[i] = prev[i - 1] + 1;
                } else {
                    curr[i] = Math.max(prev[i], curr[i - 1]);
                }
            }

            int[] tmp = prev; prev = curr; curr = tmp;
        }

        int lcs = prev[m];
        return (m + n) - 2 * lcs;
    }

    /**
     * Calcula la distància basada en LCS normalitzada entre dues paraules.
     * 
     * Normalitza la distància dividint-la per la suma de les longituds de
     * les dues paraules. Això proporciona un valor entre 0 i 1, on 0 indica
     * paraules idèntiques i 1 indica que no comparteixen cap subseqüència.
     * 
     * @param w1 primera paraula a comparar
     * @param w2 segona paraula a comparar
     * @return distància normalitzada (0-1)
     */
    @Override
    public double calculateNormalizedDistance(Word w1, Word w2) {
        double distance = calculateDistance(w1, w2);
        int len1 = w1 == null ? 0 : w1.getOriginal().length();
        int len2 = w2 == null ? 0 : w2.getOriginal().length();
        long totalLen = (long) len1 + len2;
        if (totalLen == 0) return 0.0;
        return (double) distance / totalLen;

    }

    /**
     * Obté el nom de l'algorisme.
     * 
     * @return nom de l'algorisme
     */
    @Override public String getName()        { return "LCS"; }
    
    /**
     * Obté la descripció de l'algorisme.
     * 
     * @return descripció detallada de l'algorisme
     */
    @Override public String getDescription() { return "Distància basada en la subseqüència comú més llarga (només inserció/supressió)."; }
}

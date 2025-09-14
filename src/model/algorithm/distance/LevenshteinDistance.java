//    package model.algorithm.distance;
//
//    import model.dictionary.Word;
//
//    /**
//     * Implementation of the Levenshtein distance algorithm.
//     * Calculates the minimum number of single-character edits (insertions, deletions, or substitutions)
//     * required to change one word into another.
//     */
//    public class LevenshteinDistance implements DistanceAlgorithm {
//
//        @Override
//        public double calculateDistance(Word word1, Word word2) {
//            String str1 = word1.getOriginal().toLowerCase();
//            String str2 = word2.getOriginal().toLowerCase();
//
//            int[][] dp = new int[str1.length() + 1][str2.length() + 1];
//
//            for (int i = 0; i <= str1.length(); i++) {
//                dp[i][0] = i;
//            }
//
//            for (int j = 0; j <= str2.length(); j++) {
//                dp[0][j] = j;
//            }
//
//            for (int i = 1; i <= str1.length(); i++) {
//                for (int j = 1; j <= str2.length(); j++) {
//                    int cost = (str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1;
//                    dp[i][j] = Math.min(
//                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
//                        dp[i - 1][j - 1] + cost
//                    );
//                }
//            }
//
//            System.out.println(str1 + " " + str2 + " " + dp[str1.length()][str2.length()]);
//            return dp[str1.length()][str2.length()];
//        }
//
//        @Override
//        public double calculateNormalizedDistance(Word word1, Word word2) {
//            double distance = calculateDistance(word1, word2);
//            int maxLength = Math.max(word1.getOriginal().length(), word2.getOriginal().length());
//
//
//            if (maxLength == 0) return 0;
//
//            return 2.0 * distance / (distance + maxLength);
//        }
//
//        @Override
//        public String getName() {
//            return "Levenshtein";
//        }
//
//        @Override
//        public String getDescription() {
//            return "Classic edit distance measuring the minimum number of single-character operations required to transform one string into another.";
//        }
//    }

package model.algorithm.distance;

import model.dictionary.Word;

/**
 * Implementació de l'algorisme de distància de Levenshtein clàssic.
 * 
 * Calcula la distància d'edició entre dues paraules, que representa el nombre mínim
 * d'operacions d'edició d'un sol caràcter (inserció, supressió o substitució)
 * necessaries per transformar una paraula en una altra.
 * 
 * L'algorisme utilitza programació dinàmica amb optimització d'espai, utilitzant
 * només dues files de la matriu en lloc de la matriu completa per reduir
 * l'ús de memòria de O(mn) a O(min(m,n)).
 * 
 * @author Equip ProjecteDiccionaris
 * @version 1.0
 */
public class LevenshteinDistance implements DistanceAlgorithm {

    /**
     * Calcula la distància de Levenshtein entre dues paraules.
     * 
     * Utilitza programació dinàmica optimitzada per calcular el nombre mínim
     * d'operacions d'edició necessaries. S'assegura que la paraula més curta
     * sempre sigui al bucle interior per optimitzar l'ús de memòria.
     * 
     * @param w1 primera paraula a comparar
     * @param w2 segona paraula a comparar
     * @return distància de Levenshtein (nombre d'edicions)
     */
    @Override
    public double calculateDistance(Word w1, Word w2) {
        String s = w1.getOriginal().toLowerCase();
        String t = w2.getOriginal().toLowerCase();

        // Fast paths
        if (s.equals(t)) return 0;
        if (s.isEmpty())  return t.length();
        if (t.isEmpty())  return s.length();

        // Ensure we always keep the *shorter* string in the inner loop
        if (s.length() > t.length()) {
            String tmp = s; s = t; t = tmp;
        }

        int m = s.length(), n = t.length();
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];

        for (int j = 0; j <= n; j++) prev[j] = j;

        for (int i = 1; i <= m; i++) {
            curr[0] = i;
            char sc = s.charAt(i - 1);

            for (int j = 1; j <= n; j++) {
                int cost = (sc == t.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(prev[j] + 1,      // deletion
                                curr[j - 1] + 1), // insertion
                        prev[j - 1] + cost);       // substitution
            }
            // swap rows
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[n];
    }

    /**
     * Calcula la distància de Levenshtein normalitzada entre dues paraules.
     * 
     * Normalitza la distància utilitzant la fórmula: (2 * distància) / (longitud1 + longitud2)
     * Això proporciona un valor entre 0 i 1, on 0 indica paraules idèntiques
     * i 1 indica distància màxima.
     * 
     * @param w1 primera paraula a comparar
     * @param w2 segona paraula a comparar
     * @return distància normalitzada (0-1)
     */
    @Override
    public double calculateNormalizedDistance(Word w1, Word w2) {
        double distance = calculateDistance(w1, w2);
        int sum = w1.getOriginal().length() + w2.getOriginal().length();
        if (sum == 0) {
            return 0.0;
        }
        return (2.0 * distance) / sum;
//        return maxLength == 0 ? 0.0 : distance / (double) maxLength;

    }

    /**
     * Obté el nom de l'algorisme.
     * 
     * @return nom de l'algorisme
     */
    @Override public String getName()        { return "Levenshtein"; }
    
    /**
     * Obté la descripció de l'algorisme.
     * 
     * @return descripció detallada de l'algorisme
     */
    @Override public String getDescription() { return "Edicions mínimes d'un sol caràcter per transformar una cadena en una altra (inserir, eliminar, substituir)."; }
}

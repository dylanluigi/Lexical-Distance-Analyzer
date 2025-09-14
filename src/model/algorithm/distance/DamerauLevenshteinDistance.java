package model.algorithm.distance;

import model.dictionary.Word;

/**
 * Implementació de l'algorisme de distància de Damerau-Levenshtein.
 * 
 * Extén la distància de Levenshtein permetent transposicions de caràcters adjacents
 * com una sola operació d'edició. Això és especialment útil per detectar errors
 * tipogràfics comuns on dos caràcters adjacents s'intercanvien.
 * 
 * L'algorisme considera quatre tipus d'operacions:
 * - Inserció d'un caràcter
 * - Supressió d'un caràcter
 * - Substitució d'un caràcter
 * - Transposició de dos caràcters adjacents
 * 
 * @author Equip ProjecteDiccionaris
 * @version 1.0
 */
public class DamerauLevenshteinDistance implements DistanceAlgorithm {

    /**
     * Calcula la distància de Damerau-Levenshtein entre dues paraules.
     * 
     * Utilitza programació dinàmica per calcular el nombre mínim d'operacions
     * d'edició necessaries, incloent-hi les transposicions de caràcters adjacents.
     * Gestiona casos especials com paraules buides o nul·les.
     * 
     * @param w1 primera paraula a comparar (pot ser null)
     * @param w2 segona paraula a comparar (pot ser null)
     * @return distància de Damerau-Levenshtein (nombre d'edicions)
     */
    @Override
    public double calculateDistance(Word w1, Word w2) {
        String s1 = w1 == null || w1.getOriginal() == null ? "" : w1.getOriginal().toLowerCase();
        String s2 = w2 == null || w2.getOriginal() == null ? "" : w2.getOriginal().toLowerCase();
        int len1 = s1.length(), len2 = s2.length();
        if (len1 == 0) return len2;
        if (len2 == 0) return len1;
        int[][] dp = new int[len1 + 1][len2 + 1];
        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
                if (i > 1 && j > 1
                        && s1.charAt(i - 1) == s2.charAt(j - 2)
                        && s1.charAt(i - 2) == s2.charAt(j - 1)) {
                    dp[i][j] = Math.min(dp[i][j], dp[i - 2][j - 2] + 1);
                }
            }
        }
//        System.out.println(s1 + " " + s2 + " " + dp[len1][len2]);
        return dp[len1][len2];
    }
    /**
     * Calcula la distància de Damerau-Levenshtein normalitzada entre dues paraules.
     * 
     * Normalitza la distància dividint-la per la longitud de la paraula més llarga.
     * Això proporciona un valor entre 0 i 1, on 0 indica paraules idèntiques
     * i 1 indica distància màxima possible.
     * 
     * @param w1 primera paraula a comparar (pot ser null)
     * @param w2 segona paraula a comparar (pot ser null)
     * @return distància normalitzada (0-1)
     */
    @Override
    public double calculateNormalizedDistance(Word w1, Word w2) {
        double d = calculateDistance(w1, w2);
        int maxLen = Math.max(
                w1 == null || w1.getOriginal() == null ? 0 : w1.getOriginal().length(),
                w2 == null || w2.getOriginal() == null ? 0 : w2.getOriginal().length()
        );
        return maxLen == 0 ? 0.0 : d / (double) maxLen;
    }
    
    /**
     * Obté el nom de l'algorisme.
     * 
     * @return nom de l'algorisme
     */
    @Override
    public String getName() {
        return "Damerau-Levenshtein";
    }
    
    /**
     * Obté la descripció de l'algorisme.
     * 
     * @return descripció detallada de l'algorisme
     */
    @Override
    public String getDescription() {
        return "Extensió de la distància de Levenshtein que també considera les transposicions de caràcters adjacents com una sola operació d'edició.";
    }
}
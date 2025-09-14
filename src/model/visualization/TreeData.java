package model.visualization;

import model.algorithm.distance.DistanceMatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Representa dades d'arbre de clustering hieràrquic per a visualització.
 * 
 * Aquesta classe encapsula la informació necessaria per visualitzar un arbre
 * filogenetíc que mostra les relacions entre llengües basant-se en les seves
 * distàncies. Utilitza l'algorisme UPGMA (Unweighted Pair Group Method with
 * Arithmetic Mean) per construir l'arbre hieràrquic.
 * 
 * @author Equip ProjecteDiccionaris
 * @version 1.0
 */
public class TreeData {
    private final Node root;
    private final List<String> languageCodes;
    
    /**
     * Crea un nou objecte de dades d'arbre a partir d'una matriu de distàncies.
     * 
     * Construeix un arbre filogenetíc utilitzant l'algorisme UPGMA per agrupar
     * progressivament les llengües segons les seves similituds.
     * 
     * @param matrix matriu de distàncies entre llengües
     */
    public TreeData(DistanceMatrix matrix) {
        this.languageCodes = matrix.getLanguageCodes();
        this.root = buildUpgmaTree(matrix);
    }
    
    /**
     * Obté el node arrel de l'arbre.
     * 
     * @return node arrel de l'arbre filogenetíc
     */
    public Node getRoot() {
        return root;
    }
    
    /**
     * Obté els codis de llengua.
     * 
     * @return llista de codis de llengua de l'arbre
     */
    public List<String> getLanguageCodes() {
        return languageCodes;
    }
    
    /**
     * Construeix un arbre UPGMA (Unweighted Pair Group Method with Arithmetic Mean)
     * a partir d'una matriu de distàncies.
     * 
     * L'algorisme UPGMA crea un arbre ultrametric agrupant progressivament els
     * elements més propers i calculant noves distàncies com la mitjana aritmètica.
     * 
     * @param matrix matriu de distàncies
     * @return node arrel de l'arbre construït
     */
    private Node buildUpgmaTree(DistanceMatrix matrix) {
        System.out.println("Building UPGMA tree for " + languageCodes.size() + " languages");
        
        // Special case: if there's only one language, return a leaf node
        if (languageCodes.size() <= 1) {
            if (languageCodes.isEmpty()) {
                return new Node("Unknown", 0.0);
            }
            return new Node(languageCodes.get(0), 0.0);
        }
        
        int n = languageCodes.size();
        double[][] distances = matrix.getMatrix();
        
        // Initialize clusters - initially one per language
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            nodes.add(new Node(languageCodes.get(i), 0.0));
        }
        
        // Create a copy of distances that we'll update as we merge
        double[][] currentDistances = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                currentDistances[i][j] = distances[i][j];
            }
        }
        
        // Keep track of active nodes (not yet merged into other clusters)
        boolean[] active = new boolean[n];
        Arrays.fill(active, true);
        int activeCount = n;
        
        // For debugging
        System.out.println("Initial distance matrix:");
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                System.out.printf("%.3f ", currentDistances[i][j]);
            }
            System.out.println();
        }
        
        // Keep merging until only one cluster remains
        while (activeCount > 1) {
            // Find closest pair of clusters
            int minI = -1;
            int minJ = -1;
            double minDist = Double.MAX_VALUE;
            
            for (int i = 0; i < nodes.size(); i++) {
                if (!active[i]) continue;
                
                for (int j = i + 1; j < nodes.size(); j++) {
                    if (!active[j]) continue;
                    
                    double d = currentDistances[i][j];
                    if (d < minDist) {
                        minDist = d;
                        minI = i;
                        minJ = j;
                    }
                }
            }
            
            // If no valid pair found, break
            if (minI == -1 || minJ == -1 || minI == minJ) {
                System.out.println("No valid pair found to merge");
                break;
            }
            
            System.out.printf("Merging %s and %s with distance %.3f\n", 
                    nodes.get(minI).getLanguageCode() != null ? nodes.get(minI).getLanguageCode() : "Internal",
                    nodes.get(minJ).getLanguageCode() != null ? nodes.get(minJ).getLanguageCode() : "Internal",
                    minDist);
            
            // Create a new internal node (height is half the distance between clusters)
            Node newNode = new Node(null, minDist / 2.0);
            newNode.left = nodes.get(minI);
            newNode.right = nodes.get(minJ);
            
            // Add the new node to our list
            nodes.add(newNode);
            int newIndex = nodes.size() - 1;
            
            // Expand the distance matrix to include the new node
            double[][] newDistances = new double[nodes.size()][nodes.size()];
            // Copy existing distances
            for (int i = 0; i < currentDistances.length; i++) {
                System.arraycopy(currentDistances[i], 0, newDistances[i], 0, currentDistances[i].length);
            }
            
            // Calculate distances from new cluster to all other active clusters
            // using UPGMA formula (average of distances from each merged cluster)
            for (int k = 0; k < newIndex; k++) {
                if (!active[k]) continue;
                if (k == minI || k == minJ) continue; // Skip merged clusters
                
                double newDist = (currentDistances[minI][k] + currentDistances[minJ][k]) / 2.0;
                newDistances[newIndex][k] = newDist;
                newDistances[k][newIndex] = newDist; // Distance matrix is symmetric
            }
            
            currentDistances = newDistances;
            
            // Mark merged nodes as inactive and new node as active
            active[minI] = false;
            active[minJ] = false;
            
            // Expand active array if needed
            if (newIndex >= active.length) {
                boolean[] newActive = new boolean[nodes.size()];
                System.arraycopy(active, 0, newActive, 0, active.length);
                active = newActive;
            }
            
            active[newIndex] = true;
            
            // Update active count
            activeCount = 0;
            for (int i = 0; i < active.length; i++) {
                if (active[i]) activeCount++;
            }
        }
        
        // Find the last active node (root)
        Node root = null;
        for (int i = 0; i < nodes.size(); i++) {
            if (i < active.length && active[i]) {
                root = nodes.get(i);
                break;
            }
        }
        
        // If we didn't find a valid root (unlikely), use the last node
        if (root == null && !nodes.isEmpty()) {
            root = nodes.get(nodes.size() - 1);
        }
        
        System.out.println("UPGMA tree building complete. Root: " + (root != null));
        
        return root;
    }
    
    /**
     * Classe auxiliar per fer seguiment dels clusters durant l'algorisme UPGMA.
     * 
     * Encapsula un node i el seu identificador per facilitar la gestió
     * dels clusters durant el procés de construcció de l'arbre.
     */
    private static class Cluster {
        final Node node;
        final int id;
        
        /**
         * Constructor del cluster.
         * 
         * @param node node associat al cluster
         * @param id identificador del cluster
         */
        Cluster(Node node, int id) {
            this.node = node;
            this.id = id;
        }
    }
    
    /**
     * Representa un node de l'arbre.
     * 
     * Cada node pot ser una fulla (llengua individual) o un node intern
     * (grup de llengües). Els nodes tenen una alçada que indica el nivell
     * de distància al qual es va formar el grup.
     */
    public static class Node {
        private final String languageCode;
        private final double height;
        private Node left;
        private Node right;
        private double x;
        private double y;
        
        /**
         * Crea un nou node.
         * 
         * @param languageCode codi de llengua (null per a nodes interns)
         * @param height alçada al arbre (distància)
         */
        public Node(String languageCode, double height) {
            this.languageCode = languageCode;
            this.height = height;
            this.x = 0;
            this.y = 0;
        }
        
        /**
         * Obté el codi de llengua del node.
         * 
         * @return codi de llengua o null si és un node intern
         */
        public String getLanguageCode() {
            return languageCode;
        }
        
        /**
         * Obté l'alçada del node a l'arbre.
         * 
         * @return alçada (distància) del node
         */
        public double getHeight() {
            return height;
        }
        
        /**
         * Obté el fill esquerre del node.
         * 
         * @return node fill esquerre o null si és una fulla
         */
        public Node getLeft() {
            return left;
        }
        
        /**
         * Obté el fill dret del node.
         * 
         * @return node fill dret o null si és una fulla
         */
        public Node getRight() {
            return right;
        }
        
        /**
         * Comprova si el node és una fulla.
         * 
         * @return true si el node no té fills (representa una llengua individual)
         */
        public boolean isLeaf() {
            return left == null && right == null;
        }
        
        /**
         * Obté la coordenada X per a visualització.
         * 
         * @return coordenada X
         */
        public double getX() {
            return x;
        }
        
        /**
         * Estableix la coordenada X per a visualització.
         * 
         * @param x nova coordenada X
         */
        public void setX(double x) {
            this.x = x;
        }
        
        /**
         * Obté la coordenada Y per a visualització.
         * 
         * @return coordenada Y
         */
        public double getY() {
            return y;
        }
        
        /**
         * Estableix la coordenada Y per a visualització.
         * 
         * @param y nova coordenada Y
         */
        public void setY(double y) {
            this.y = y;
        }
    }
}
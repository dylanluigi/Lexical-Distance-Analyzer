package model.visualization;

import model.algorithm.distance.DistanceMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa dades de graf per a visualització.
 * 
 * Aquesta classe encapsula la informació necessaria per visualitzar un graf
 * de proximitat entre llengües basant-se en una matriu de distàncies i un llindar.
 * Crea nodes per a cada llengua i arestes per a parelles de llengües amb
 * distància inferior al llindar especificat.
 * 
 * @author Equip ProjecteDiccionaris
 * @version 1.0
 */
public class GraphData {
    private final List<Node> nodes;
    private final List<Edge> edges;
    private final double threshold;
    
    /**
     * Crea un nou objecte de dades de graf a partir d'una matriu de distàncies amb un llindar.
     * 
     * Genera nodes per a cada llengua de la matriu i crea arestes només entre
     * parelles de llengües amb distància menor o igual al llindar especificat.
     * 
     * @param matrix matriu de distàncies
     * @param threshold distància màxima per a arestes (0-1)
     * @throws IllegalArgumentException si el llindar no està entre 0 i 1
     */
    public GraphData(DistanceMatrix matrix, double threshold) {
        if (threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException("Threshold must be between 0 and 1");
        }
        
        this.threshold = threshold;
        
        List<String> langCodes = matrix.getLanguageCodes();
        int size = langCodes.size();
        
        // Create nodes
        nodes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String langCode = langCodes.get(i);
            nodes.add(new Node(i, langCode, langCode));
        }
        
        // Create edges where distance <= threshold
        edges = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                double distance = matrix.getMatrix()[i][j];
                if (distance <= threshold) {
                    edges.add(new Edge(i, j, distance));
                }
            }
        }
    }
    
    /**
     * Obté els nodes del graf.
     * 
     * @return llista de nodes
     */
    public List<Node> getNodes() {
        return nodes;
    }
    
    /**
     * Obté les arestes del graf.
     * 
     * @return llista d'arestes
     */
    public List<Edge> getEdges() {
        return edges;
    }
    
    /**
     * Obté el llindar utilitzat per aquest graf.
     * 
     * @return llindar de distància
     */
    public double getThreshold() {
        return threshold;
    }
    
    /**
     * Representa un node del graf.
     * 
     * Cada node correspon a una llengua del corpus i té un identificador únic,
     * una etiqueta per a visualització i coordenades espacials.
     */
    public static class Node {
        private final int id;
        private final String label;
        private final String languageCode;
        private double x;
        private double y;
        
        /**
         * Crea un nou node.
         * 
         * @param id identificador del node
         * @param label etiqueta del node
         * @param languageCode codi de llengua
         */
        public Node(int id, String label, String languageCode) {
            this.id = id;
            this.label = label;
            this.languageCode = languageCode;
        }
        
        /**
         * Obté l'identificador del node.
         * 
         * @return identificador únic del node
         */
        public int getId() {
            return id;
        }
        
        /**
         * Obté l'etiqueta del node.
         * 
         * @return etiqueta per a visualització
         */
        public String getLabel() {
            return label;
        }
        
        /**
         * Obté el codi de llengua del node.
         * 
         * @return codi de llengua associat
         */
        public String getLanguageCode() {
            return languageCode;
        }
        
        /**
         * Obté la coordenada X del node.
         * 
         * @return coordenada X per a visualització
         */
        public double getX() {
            return x;
        }
        
        /**
         * Estableix la coordenada X del node.
         * 
         * @param x nova coordenada X
         */
        public void setX(double x) {
            this.x = x;
        }
        
        /**
         * Obté la coordenada Y del node.
         * 
         * @return coordenada Y per a visualització
         */
        public double getY() {
            return y;
        }
        
        /**
         * Estableix la coordenada Y del node.
         * 
         * @param y nova coordenada Y
         */
        public void setY(double y) {
            this.y = y;
        }
    }
    
    /**
     * Representa una aresta del graf.
     * 
     * Cada aresta connecta dos nodes (llengües) i té un pes que representa
     * la distància entre les llengües corresponents.
     */
    public static class Edge {
        private final int source;
        private final int target;
        private final double weight;
        
        /**
         * Crea una nova aresta.
         * 
         * @param source identificador del node origen
         * @param target identificador del node destí
         * @param weight pes de l'aresta (distància)
         */
        public Edge(int source, int target, double weight) {
            this.source = source;
            this.target = target;
            this.weight = weight;
        }
        
        /**
         * Obté l'identificador del node origen.
         * 
         * @return identificador del node origen
         */
        public int getSource() {
            return source;
        }
        
        /**
         * Obté l'identificador del node destí.
         * 
         * @return identificador del node destí
         */
        public int getTarget() {
            return target;
        }
        
        /**
         * Obté el pes de l'aresta.
         * 
         * @return pes de l'aresta (distància entre llengües)
         */
        public double getWeight() {
            return weight;
        }
    }
}
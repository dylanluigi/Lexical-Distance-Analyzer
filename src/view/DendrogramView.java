package view;    // ← change to match your package

import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class DendrogramView extends Pane {
    public static class Node {
        public final String label;
        public final List<Node> children = new ArrayList<>();
        

        private double x;
        private double y;
        
        public Node(String label, Node... kids) {
            this.label = label;
            for (Node c: kids) children.add(c);
        }
        
        public void setPosition(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        public double getX() {
            return x;
        }
        
        public double getY() {
            return y;
        }
    }

    private final Node root;
    private final double leafGap  = 40;
    private final double levelGap = 100;
    private final double nodeDiameter = 6;

    public DendrogramView(Node root) {
        this.root = root;
        draw();
        

        List<Node> leaves = new ArrayList<>();
        collectLeaves(root, leaves);
        

        int depth = calculateTreeDepth(root);
        

        double prefWidth = Math.max(800, 50 + depth * levelGap + 200);
        double prefHeight = Math.max(400, 100 + leaves.size() * leafGap + 100);
        
        setPrefSize(prefWidth, prefHeight);
        setMinSize(prefWidth, prefHeight);
    }
    
    private int calculateTreeDepth(Node node) {
        if (node == null || node.children.isEmpty()) {
            return 0;
        }
        
        int maxChildDepth = 0;
        for (Node child : node.children) {
            maxChildDepth = Math.max(maxChildDepth, calculateTreeDepth(child));
        }
        
        return 1 + maxChildDepth;
    }

    private void draw() {
        getChildren().clear();
        Group g = new Group();


        javafx.scene.text.Text title = new javafx.scene.text.Text(50, 20, "Arbre Filogenètic (UPGMA)");
        title.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
        g.getChildren().add(title);
        

        javafx.scene.text.Text legend = new javafx.scene.text.Text(50, 40, 
            "Les longituds de branca representen la distància lexical entre idiomes");
        legend.setFont(javafx.scene.text.Font.font(12));
        g.getChildren().add(legend);


        List<Node> leaves = new ArrayList<>();
        collectLeaves(root, leaves);
        

        if (leaves.isEmpty()) {

            javafx.scene.text.Text noLeaves = new javafx.scene.text.Text(300, 300, 
                "No es pot crear l'arbre filogenètic: No hi ha nodes fulla");
            noLeaves.setFont(javafx.scene.text.Font.font(14));
            g.getChildren().add(noLeaves);
            getChildren().add(g);
            return;
        }
        

        double scaleLength = 100;
        double scaleX = 50;
        double scaleY = 50 + (leaves.size() * leafGap) + 50;
        javafx.scene.shape.Line scaleLine = new javafx.scene.shape.Line(scaleX, scaleY, scaleX + scaleLength, scaleY);
        scaleLine.setStroke(javafx.scene.paint.Color.BLACK);
        scaleLine.setStrokeWidth(2);
        g.getChildren().add(scaleLine);
        

        javafx.scene.text.Text scaleLabel = new javafx.scene.text.Text(scaleX + scaleLength/2, scaleY + 20, "Escala de Distància");
        scaleLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        scaleLabel.setFont(javafx.scene.text.Font.font(10));
        g.getChildren().add(scaleLabel);
        

        calculateNodePositions(root, 0, 0, leaves.size());
        

        drawTree(root, g);

        getChildren().add(g);
    }
    
    /**
     * Calculate positions for all nodes in the tree
     */
    private void calculateNodePositions(Node node, int depth, int start, int end) {
        if (node == null) return;
        
        // Base position
        double x = 50 + depth * levelGap;
        
        if (node.children.isEmpty()) {
            // Leaf node
            double y = 50 + start * leafGap;
            node.setPosition(x, y);
            return;
        }
        
        // Calculate positions for children
        int totalLeaves = countLeaves(node);
        if (totalLeaves == 0) totalLeaves = 1; // Safety check
        
        double yTop = 50 + start * leafGap;
        double yBottom = 50 + (end - 1) * leafGap;
        
        // Position the internal node at the middle
        node.setPosition(x, (yTop + yBottom) / 2);
        
        // Calculate positions for children
        if (node.children.size() == 2) {
            // Binary tree case
            Node left = node.children.get(0);
            Node right = node.children.get(1);
            
            int leftLeaves = countLeaves(left);
            if (leftLeaves == 0) leftLeaves = 1;
            
            // Calculate positions for left and right subtrees
            calculateNodePositions(left, depth + 1, start, start + leftLeaves);
            calculateNodePositions(right, depth + 1, start + leftLeaves, end);
        } else if (!node.children.isEmpty()) {
            // N-ary tree case
            int curStart = start;
            for (Node child : node.children) {
                int childLeaves = countLeaves(child);
                if (childLeaves == 0) childLeaves = 1;
                
                calculateNodePositions(child, depth + 1, curStart, curStart + childLeaves);
                curStart += childLeaves;
            }
        }
    }
    
    /**
     * Draw the entire tree recursively
     */
    private void drawTree(Node node, Group g) {
        if (node == null) return;
        
        // Draw this node
        drawNode(node, g);
        
        // Draw connections to children
        for (Node child : node.children) {
            if (child == null) continue;
            
            // Draw the connection
            drawConnection(node, child, g);
            
            // Recurse
            drawTree(child, g);
        }
    }
    
    /**
     * Draw a single node with its label
     */
    private void drawNode(Node node, Group g) {
        if (node == null) return;
        
        if (node.children.isEmpty()) {
            // Leaf node (draw as a colored circle with the language code)
            javafx.scene.shape.Circle leafCircle = new javafx.scene.shape.Circle(
                node.getX(), node.getY(), nodeDiameter/2);
            leafCircle.setFill(javafx.scene.paint.Color.DODGERBLUE);
            leafCircle.setStroke(javafx.scene.paint.Color.BLACK);
            leafCircle.setStrokeWidth(0.5);
            g.getChildren().add(leafCircle);
            
            // Draw leaf label (language code)
            Text leafLabel = new Text(node.getX() + 10, node.getY() + 5, node.label);
            leafLabel.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 14));
            leafLabel.setFill(javafx.scene.paint.Color.DARKBLUE);
            g.getChildren().add(leafLabel);
        } else {
            // Internal node
            javafx.scene.shape.Circle internalCircle = new javafx.scene.shape.Circle(
                node.getX(), node.getY(), nodeDiameter/2 - 1);
            internalCircle.setFill(javafx.scene.paint.Color.BLACK);
            g.getChildren().add(internalCircle);
            
            // Draw height label for internal nodes if not empty
            if (node.label != null && !node.label.isEmpty()) {
                Text heightLabel = new Text(node.getX() - 15, node.getY() - 10, node.label);
                heightLabel.setFont(Font.font(10));
                heightLabel.setFill(javafx.scene.paint.Color.GRAY);
                g.getChildren().add(heightLabel);
            }
        }
    }
    
    /**
     * Draw the connection between a parent node and its child
     */
    private void drawConnection(Node parent, Node child, Group g) {
        if (parent == null || child == null) return;
        
        // Determine color based on which side (left or right in binary trees)
        javafx.scene.paint.Color lineColor;
        if (parent.children.size() == 2) {
            if (parent.children.get(0) == child) {
                // Left child in a binary tree
                lineColor = javafx.scene.paint.Color.DARKBLUE;
            } else {
                // Right child in a binary tree
                lineColor = javafx.scene.paint.Color.DARKRED;
            }
        } else {
            // General case - alternate colors for visual distinction
            int childIndex = parent.children.indexOf(child);
            lineColor = (childIndex % 2 == 0) ? 
                javafx.scene.paint.Color.DARKBLUE : 
                javafx.scene.paint.Color.DARKRED;
        }
        
        // Draw horizontal line to midpoint
        double midX = (parent.getX() + child.getX()) / 2;
        Line hLine1 = new Line(parent.getX(), parent.getY(), midX, parent.getY());
        hLine1.setStroke(lineColor);
        hLine1.setStrokeWidth(1.5);
        g.getChildren().add(hLine1);
        
        // Draw vertical line from parent's level to child's level
        Line vLine = new Line(midX, parent.getY(), midX, child.getY());
        vLine.setStroke(lineColor);
        vLine.setStrokeWidth(1.5);
        g.getChildren().add(vLine);
        
        // Draw horizontal line from midpoint to child
        Line hLine2 = new Line(midX, child.getY(), child.getX(), child.getY());
        hLine2.setStroke(lineColor);
        hLine2.setStrokeWidth(1.5);
        g.getChildren().add(hLine2);
    }

    private void collectLeaves(Node n, List<Node> out) {
        if (n.children.isEmpty()) out.add(n);
        else for (Node c: n.children) collectLeaves(c, out);
    }

    private int countLeaves(Node n) {
        if (n.children.isEmpty()) return 1;
        int sum = 0;
        for (Node c: n.children) sum += countLeaves(c);
        return sum;
    }

}

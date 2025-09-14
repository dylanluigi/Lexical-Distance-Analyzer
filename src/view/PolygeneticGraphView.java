package view;    // ← change to match your package

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.List;
import java.util.Map;

public class PolygeneticGraphView extends Pane {
    public static class Edge {
        public final String from, to;
        public final double distance; // in [0 – 1]
        public final boolean dashed;
        public Edge(String from, String to, double distance, boolean dashed) {
            this.from = from; this.to = to;
            this.distance = distance; this.dashed = dashed;
        }
    }

    private final Map<String, Point2D> positions;
    private final List<Edge> edges;

    public PolygeneticGraphView(Map<String, Point2D> positions, List<Edge> edges) {
        this.positions = positions;
        this.edges     = edges;
        draw();
    }

    private void draw() {
        getChildren().clear();
        Group edgesLayer = new Group();
        Group nodesLayer = new Group();

        // 1) edges + labels
        for (Edge e : edges) {
            Point2D A = positions.get(e.from);
            Point2D B = positions.get(e.to);

            Line line = new Line(A.getX(), A.getY(), B.getX(), B.getY());
            line.setStrokeWidth(2);
            if (e.dashed) line.getStrokeDashArray().addAll(8.0, 6.0);
            line.setStroke(Color.BLACK);

            // label at the midpoint
            double mx = (A.getX() + B.getX())/2;
            double my = (A.getY() + B.getY())/2;
            Text lbl = new Text(mx, my, String.format("0'%03.0f", e.distance * 1000));
            lbl.setFont(Font.font(14));
            // offset it a bit perpendicular to the edge
            double dx = B.getY() - A.getY();
            double dy = A.getX() - B.getX();
            double len = Math.hypot(dx, dy);
            dx = dx/len * 8;
            dy = dy/len * 8;
            lbl.setTranslateX(dx);
            lbl.setTranslateY(dy);

            edgesLayer.getChildren().addAll(line, lbl);
        }

        // 2) nodes + text
        for (Map.Entry<String, Point2D> ent : positions.entrySet()) {
            String name = ent.getKey();
            Point2D p   = ent.getValue();

            javafx.scene.shape.Circle c = new javafx.scene.shape.Circle(p.getX(), p.getY(), 25);
            c.setFill(Color.WHITE);
            c.setStroke(Color.BLACK);
            c.setStrokeWidth(2);
            Tooltip.install(c, new Tooltip(name));

            Text text = new Text(p.getX()-18, p.getY()+6, name);
            text.setFont(Font.font(16));

            nodesLayer.getChildren().addAll(c, text);
        }

        getChildren().addAll(edgesLayer, nodesLayer);
    }
}

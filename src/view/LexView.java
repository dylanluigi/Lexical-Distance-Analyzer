package view;

import controller.LexController;
import controller.LexController.QueryResult;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.LexModel;
import model.LexModel.CorpusSummary;
import model.algorithm.distance.DistanceAlgorithm;
import model.algorithm.distance.DistanceMatrix;
import model.algorithm.distance.LevenshteinDistance;
import model.visualization.GraphData;
import model.visualization.TreeData;
import notification.NotificationService;
import notification.NotificationServiceImpl;
import notification.NotificationServiceImpl.UINotificationHandler;
import javafx.geometry.Point2D;
import view.PolygeneticGraphView;
import view.DendrogramView;

import java.util.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main view for the LexDistance application.
 */
public class LexView extends Application implements UINotificationHandler {
    
    // Static references for communication between main thread and JavaFX thread
    private static LexModel model;
    private static LexController controller;
    private static NotificationService notificationService;
    
    private Stage primaryStage;
    private BorderPane mainLayout;
    private ListView<String> languageListView;
    private ProgressBar progressBar;
    private Label statusLabel;
    private TabPane tabPane;
    
    @Override
    public void init() {
        // This runs before the JavaFX UI is shown
        // Access the static references that were set up in main
        if (controller != null) {
            // Connect the view to the controller
            controller.init(model, this, notificationService);
        }
    }
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Distància Lexical");
        
        initializeUI();
        
        primaryStage.setScene(new Scene(mainLayout, 1024, 768));
        primaryStage.show();
        
        // Add close handler to properly shut down resources
        primaryStage.setOnCloseRequest(event -> {
            if (model != null) {
                model.shutdown();
            }
        });
    }
    
    @Override
    public void stop() {
        // This is called when the application is stopping
        if (model != null) {
            model.shutdown();
        }
    }
    
    /**
     * Initializes the UI components.
     */
    private void initializeUI() {
        mainLayout = new BorderPane();
        
        // Top panel with file selection and computation controls
        HBox topPanel = createTopPanel();
        mainLayout.setTop(topPanel);
        
        // Left panel with language list
        VBox leftPanel = createLeftPanel();
        mainLayout.setLeft(leftPanel);
        
        // Center panel with tabs for different visualizations
        tabPane = new TabPane();
        mainLayout.setCenter(tabPane);
        
        // Bottom panel with status and progress
        HBox bottomPanel = createBottomPanel();
        mainLayout.setBottom(bottomPanel);
    }
    
    /**
     * Creates the top panel with file selection and computation controls.
     * 
     * @return The top panel
     */
    private HBox createTopPanel() {
        HBox panel = new HBox(10);
        panel.setPadding(new Insets(10));
        
        // File selection button
        Button openButton = new Button("Obrir Diccionaris");
        openButton.setOnAction(e -> {
            if (controller != null) {
                openDictionaries();
            } else {
                showError("Controller not initialized");
            }
        });
        
        // Algorithm selection
        ComboBox<String> algorithmComboBox = new ComboBox<>();
        algorithmComboBox.getItems().addAll("Levenshtein", "Damerau-Levenshtein", "Jaro-Winkler", "LCS");
        algorithmComboBox.setValue("Levenshtein");
        
        // Sample settings
        Label samplesLabel = new Label("Mostres:");
        Spinner<Integer> samplesSpinner = new Spinner<>(1, 1000, 3);
        samplesSpinner.setEditable(true);
        samplesSpinner.setPrefWidth(80);
        
        Label sampleSizeLabel = new Label("Mida:");
        Spinner<Integer> sampleSizeSpinner = new Spinner<>(50, 50000, 200);
        sampleSizeSpinner.setEditable(true);
        sampleSizeSpinner.setPrefWidth(100);
        
        // Create optimization toggles
        VBox optimizationBox = new VBox(5);
        optimizationBox.setPadding(new Insets(5));
        
        CheckBox parallelizationToggle = new CheckBox("Usar Paral·lelització");
        parallelizationToggle.setSelected(true);
        
        CheckBox samplingToggle = new CheckBox("Usar Mostreig");
        samplingToggle.setSelected(true);
        
        CheckBox normalizedFormToggle = new CheckBox("Comparació de Forma Normalitzada");
        normalizedFormToggle.setSelected(true);
        
        CheckBox semanticBonusToggle = new CheckBox("Aplicar Bonificació Semàntica");
        semanticBonusToggle.setSelected(true);
        
        optimizationBox.getChildren().addAll(
            new Label("Configuració d'Optimització:"),
            parallelizationToggle,
            samplingToggle,
            normalizedFormToggle,
            semanticBonusToggle
        );
        
        // Compute button
        Button computeButton = new Button("Calcular");
        computeButton.setOnAction(e -> {
            if (controller != null) {
                String selectedAlgorithm = algorithmComboBox.getValue();
                long numSamples = samplesSpinner.getValue().longValue();
                long sampleSize = sampleSizeSpinner.getValue().longValue();
                
                // Use appropriate algorithm based on selection
                DistanceAlgorithm algorithm;
                switch (selectedAlgorithm) {
                    case "Damerau-Levenshtein":
                        algorithm = new model.algorithm.distance.DamerauLevenshteinDistance();
                        break;
                    case "Jaro-Winkler":
                        algorithm = new model.algorithm.distance.JaroWinklerDistance();
                        break;
                    case "LCS":
                        algorithm = new model.algorithm.distance.LongestCommonSubsequence();
                        break;
                    default:
                        algorithm = new LevenshteinDistance();
                }
                
                // Create optimization flags map
                Map<String, Boolean> optimizationFlags = new HashMap<>();
                optimizationFlags.put("useParallelization", parallelizationToggle.isSelected());
                optimizationFlags.put("useSampling", samplingToggle.isSelected());
                optimizationFlags.put("useNormalizedFormMatching", normalizedFormToggle.isSelected());
                optimizationFlags.put("useSemanticBonus", semanticBonusToggle.isSelected());
                
                // Start computation with the selected parameters and optimization flags
                controller.onUserStartComputation(algorithm, numSamples, sampleSize, optimizationFlags);
            } else {
                showError("Controller not initialized");
            }
        });
        
        // Export button
        Button exportButton = new Button("Exportar CSV");
        exportButton.setOnAction(e -> {
            if (controller != null) {
                exportDistanceMatrix();
            } else {
                showError("Controller not initialized");
            }
        });
        
        HBox settingsBox = new HBox(20);
        settingsBox.getChildren().addAll(
            new VBox(5, new Label("Algorisme:"), algorithmComboBox),
            new VBox(5, samplesLabel, samplesSpinner),
            new VBox(5, sampleSizeLabel, sampleSizeSpinner),
            optimizationBox
        );
        
        panel.getChildren().addAll(
            openButton, 
            settingsBox,
            computeButton,
            exportButton
        );
        
        return panel;
    }
    
    /**
     * Opens a file chooser dialog to export the distance matrix as CSV.
     */
    private void exportDistanceMatrix() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Matriu de Distàncies");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Arxius CSV", "*.csv")
        );
        fileChooser.setInitialFileName("distance_matrix.csv");
        
        File selectedFile = fileChooser.showSaveDialog(primaryStage);
        if (selectedFile != null) {
            controller.onUserExportCSV(selectedFile.toPath());
        }
    }
    
    /**
     * Creates the left panel with language list.
     * 
     * @return The left panel
     */
    private VBox createLeftPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(200);
        
        languageListView = new ListView<>();
        languageListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        Button queryButton = new Button("Comparar Seleccionats");
        queryButton.setOnAction(e -> {
            if (controller != null) {
                List<String> selected = languageListView.getSelectionModel().getSelectedItems();
                if (selected.size() == 2) {
                    controller.onUserQuerySingle(selected.get(0), selected.get(1));
                } else if (selected.size() == 1) {
                    controller.onUserQueryAll(selected.get(0));
                }
            } else {
                showError("Controller not initialized");
            }
        });
        
        Slider thresholdSlider = new Slider(0, 1, 0.5);
        thresholdSlider.setShowTickLabels(true);
        thresholdSlider.setShowTickMarks(true);
        
        Button graphButton = new Button("Mostrar Graf de Xarxa");
        graphButton.setOnAction(e -> {
            if (controller != null) {
                controller.onUserShowGraph(thresholdSlider.getValue());
            } else {
                showError("Controller not initialized");
            }
        });
        
        Button treeButton = new Button("Mostrar Arbre Filogenètic");
        treeButton.setOnAction(e -> {
            if (controller != null) {
                controller.onUserShowTree();
            } else {
                showError("Controller not initialized");
            }
        });
        
        panel.getChildren().addAll(
            new Label("Idiomes:"),
            languageListView,
            queryButton,
            new Label("Visualització:"),
            new Label("Llindar de Xarxa:"),
            thresholdSlider,
            graphButton,
            treeButton
        );
        
        return panel;
    }
    
    /**
     * Creates the bottom panel with status and progress.
     * 
     * @return The bottom panel
     */
    private HBox createBottomPanel() {
        HBox panel = new HBox(10);
        panel.setPadding(new Insets(10));
        
        statusLabel = new Label("Ready");
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        
        // Cancel button
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> {
            if (controller != null) {
                controller.onUserCancelComputation();
            } else {
                showError("Controller not initialized");
            }
        });
        
        panel.getChildren().addAll(statusLabel, progressBar, cancelButton);
        
        return panel;
    }
    
    /**
     * Opens a file chooser dialog to select dictionary files.
     */
    private void openDictionaries() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Dictionary Files");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Dictionary Files", "*.txt", "*.csv", "*.dic", "*.dict"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("Dictionary Files", "*.dic", "*.dict"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage);
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            Path[] paths = selectedFiles.stream()
                .map(File::toPath)
                .toArray(Path[]::new);
            
            controller.onUserFileSelection(paths);
        }
    }
    
    /**
     * Sets the controller for this view.
     * 
     * @param controller The controller
     */
    public void setController(LexController controller) {
        // This is called by the controller during initialization
        // Store it in the instance field
        if (controller != null) {
            this.controller = controller;
        }
    }
    
    /**
     * Sets the notification service for this view.
     * 
     * @param notificationService The notification service
     */
    public void setNotificationService(NotificationService notificationService) {
        // This is called by the controller during initialization
        // Store it in the instance field and subscribe to events
        if (notificationService != null) {
            this.notificationService = notificationService;
            notificationService.subscribe(NotificationService.EventType.RENDER_REQUEST, this);
        }
    }
    
    /**
     * Shows a summary of the loaded corpus.
     * 
     * @param summary The corpus summary
     */
    public void showCorpusSummary(CorpusSummary summary) {
        Platform.runLater(() -> {
            languageListView.getItems().clear();
            languageListView.getItems().addAll(summary.languages());
            
            statusLabel.setText("Loaded " + summary.wordsTotal() + " words from " + summary.languages().size() + " languages");
        });
    }
    
    /**
     * Updates the progress bar and status.
     * 
     * @param percent Percentage of completion (0-100)
     * @param statusText Status text
     */
    public void updateProgress(int percent, String statusText) {
        Platform.runLater(() -> {
            progressBar.setProgress(percent / 100.0);
            statusLabel.setText(statusText + " (" + percent + "%)");
        });
    }
    
    /**
     * Shows the distance matrix in a table.
     * Updates existing tab if one already exists.
     * 
     * @param matrix The distance matrix
     */
    public void showDistanceTable(DistanceMatrix matrix) {
        Platform.runLater(() -> {
            TableView<List<String>> tableView = new TableView<>();
            
            // Add a row header column for language codes
            List<String> langCodes = matrix.getLanguageCodes();
            
            // First column is the row header (language code)
            TableColumn<List<String>, String> rowHeaderColumn = new TableColumn<>("Language");
            rowHeaderColumn.setCellValueFactory(data -> {
                String value = data.getValue().get(0); // First element is the language code
                return new javafx.beans.property.SimpleStringProperty(value);
            });
            rowHeaderColumn.setPrefWidth(80); // Make it a bit wider for language codes
            rowHeaderColumn.setStyle("-fx-alignment: CENTER-LEFT;"); // Left align
            tableView.getColumns().add(rowHeaderColumn);
            
            // Create data columns
            for (int i = 0; i < langCodes.size(); i++) {
                final int colIndex = i + 1; // +1 because index 0 is now the language code
                TableColumn<List<String>, String> column = new TableColumn<>(langCodes.get(i));
                column.setCellValueFactory(data -> {
                    String value = data.getValue().get(colIndex);
                    return new javafx.beans.property.SimpleStringProperty(value);
                });
                // Center align data columns
                column.setStyle("-fx-alignment: CENTER;");
                tableView.getColumns().add(column);
            }
            
            // Add rows
            double[][] distances = matrix.getMatrix();
            for (int i = 0; i < langCodes.size(); i++) {
                List<String> row = new ArrayList<>();
                // First add the language code as the row header
                row.add(langCodes.get(i));
                
                // Then add all the distance values
                for (int j = 0; j < langCodes.size(); j++) {
                    // Display as distance percentage
                    double distancePercent = distances[i][j] * 100.0;
                    row.add(String.format("%.2f%%", distancePercent));
                }
                tableView.getItems().add(row);
            }
            
            // Configure table appearance
            tableView.setFixedCellSize(30);  // Fixed row height
            tableView.setStyle("-fx-font-size: 12px;");
            
            // Highlight the diagonal cells (distance to self = 0)
            tableView.setRowFactory(tv -> new TableRow<List<String>>() {
                @Override
                protected void updateItem(List<String> item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (item == null || empty) return;
                    
                    // Get row index
                    int rowIndex = getIndex();
                    
                    // Add a style class to this row for potential row-level styling
                    getStyleClass().add("matrix-row");
                    
                    // Add CSS style to highlight on hover
                    setStyle("-fx-background-color: transparent;");
                    
                    // When mouse enters the row, highlight it
                    setOnMouseEntered(e -> {
                        setStyle("-fx-background-color: #f0f0f0;");
                    });
                    
                    // When mouse exits, remove highlight
                    setOnMouseExited(e -> {
                        setStyle("-fx-background-color: transparent;");
                    });
                }
            });
            
            // Create a tab for the table
            Tab tableTab = new Tab("Distance Matrix");
            tableTab.setContent(tableView);
            tableTab.setClosable(false);
            
            // Add the tab or update and select it if it already exists
            boolean tabExists = false;
            for (Tab tab : tabPane.getTabs()) {
                if (tab.getText().equals(tableTab.getText())) {
                    // Update the existing tab with the new table content
                    tab.setContent(tableView);
                    tabPane.getSelectionModel().select(tab);
                    tabExists = true;
                    break;
                }
            }
            
            // If tab doesn't exist, add it
            if (!tabExists) {
                tabPane.getTabs().add(tableTab);
                tabPane.getSelectionModel().select(tableTab);
            }
            
            // Update status to show which algorithm was used
            statusLabel.setText("Distance matrix calculated using " + matrix.getAlgorithm().getName() + 
                                " with " + matrix.getNumSamples() + " samples of size " + 
                                matrix.getMaxSampleSize());
        });
    }
    
    /**
     * Shows the result of a query.
     * 
     * @param result The query result
     */
    public void showQueryResult(QueryResult result) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Query Result");
            alert.setHeaderText("Distance between " + result.langA() + " and " + result.langB());
            double distancePercent = result.distance() * 100.0;
            alert.setContentText(String.format("The distance is: %.2f%%", distancePercent));
            alert.showAndWait();
        });
    }

    /**
     * Renders a network graph visualization.
     *
     * @param graphData The graph data
     */
    /**
     * Renders the network graph using PolygeneticGraphView.
     */

    public void renderGraph(GraphData graphData) {
        Platform.runLater(() -> {
            // 1) collect language codes
            List<String> codes = new ArrayList<>();
            for (GraphData.Node n : graphData.getNodes()) {
                codes.add(n.getLanguageCode());
            }
            int n = codes.size();
            if (n == 0) return;

            // 2) circle layout inside an 800×600 area (matches your old Canvas)
            double w = 800, h = 600;
            double cx = w / 2, cy = h / 2, r = Math.min(w, h) * 0.4;

            Map<String, Point2D> positions = new HashMap<>();
            for (int i = 0; i < n; i++) {
                double theta = 2 * Math.PI * i / n - Math.PI/2;
                positions.put(codes.get(i),
                        new Point2D(cx + r * Math.cos(theta),
                                cy + r * Math.sin(theta)));
            }

            // 3) build edge list
            List<PolygeneticGraphView.Edge> edges = new ArrayList<>();
            for (GraphData.Edge e : graphData.getEdges()) {
                String a = graphData.getNodes().get(e.getSource()).getLanguageCode();
                String b = graphData.getNodes().get(e.getTarget()).getLanguageCode();
                edges.add(new PolygeneticGraphView.Edge(a, b, e.getWeight(), false));
            }

            // 4) create the view
            PolygeneticGraphView poly = new PolygeneticGraphView(positions, edges);
            poly.setPrefSize(w, h);

            // 5) install into a tab
            Tab graphTab = new Tab("Network Graph", poly);
            graphTab.setClosable(false);
            // replace old tab if present
            tabPane.getTabs().removeIf(t -> "Network Graph".equals(t.getText()));
            tabPane.getTabs().add(graphTab);
            tabPane.getSelectionModel().select(graphTab);

            statusLabel.setText("Network graph updated");
        });
    }


    public void renderTree(TreeData treeData) {
        Platform.runLater(() -> {
            try {
                // Get root node and validate
                TreeData.Node srcRoot = treeData.getRoot();
                if (srcRoot == null) {
                    statusLabel.setText("Cannot create tree: No root node found");
                    System.err.println("Tree root is null");
                    return;
                }
                
                // Enhanced debugging
                System.out.println("===== TREE STRUCTURE DEBUG =====");
                System.out.println("Tree root: " + srcRoot);
                System.out.println("Root height: " + srcRoot.getHeight());
                
                if (srcRoot.getLeft() != null) {
                    System.out.println("Left child: " + (srcRoot.getLeft().getLanguageCode() != null ? 
                        srcRoot.getLeft().getLanguageCode() : "Internal node with height " + srcRoot.getLeft().getHeight()));
                } else {
                    System.out.println("Left child: null");
                }
                
                if (srcRoot.getRight() != null) {
                    System.out.println("Right child: " + (srcRoot.getRight().getLanguageCode() != null ? 
                        srcRoot.getRight().getLanguageCode() : "Internal node with height " + srcRoot.getRight().getHeight()));
                } else {
                    System.out.println("Right child: null");
                }
                
                // Count leaves and print language codes for debugging
                int leafCount = countLeaves(srcRoot);
                System.out.println("Leaf count: " + leafCount);
                System.out.println("Language codes: " + treeData.getLanguageCodes());
                
                if (leafCount == 0) {
                    statusLabel.setText("Cannot create tree: No leaf nodes found");
                    System.err.println("Tree has no leaf nodes");
                    return;
                }
                
                // Display more tree structure for debugging
                System.out.println("Tree structure:");
                printTreeStructure(srcRoot, 0);
                System.out.println("===== END TREE DEBUG =====");

                // Convert TreeData into DendrogramView.Node
                DendrogramView.Node root = convertTree(srcRoot);
                
                // Debug the converted tree
                System.out.println("Converted tree structure:");
                printDendrogramNodeStructure(root, 0);

                // Create the view (it will size itself based on content)
                DendrogramView treeView = new DendrogramView(root);
                
                // Wrap in a scroll pane to make it scrollable
                ScrollPane scrollPane = new ScrollPane(treeView);
                scrollPane.setFitToWidth(false);
                scrollPane.setFitToHeight(false);
                scrollPane.setPrefSize(800, 600);
                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

                // Install into a tab
                Tab treeTab = new Tab("Arbre Filogenètic", scrollPane);
                treeTab.setClosable(false);
                tabPane.getTabs().removeIf(t -> "Arbre Filogenètic".equals(t.getText()));
                tabPane.getTabs().add(treeTab);
                tabPane.getSelectionModel().select(treeTab);

                statusLabel.setText("Phylogenetic tree updated with " + leafCount + " languages");
            } catch (Exception e) {
                System.err.println("Error rendering tree: " + e.getMessage());
                e.printStackTrace();
                statusLabel.setText("Error rendering tree: " + e.getMessage());
            }
        });
    }
    
    /**
     * Helper method to print the tree structure recursively for debugging
     */
    private void printTreeStructure(TreeData.Node node, int depth) {
        if (node == null) return;
        
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) indent.append("  ");
        
        if (node.isLeaf()) {
            System.out.println(indent + "Leaf: " + node.getLanguageCode());
        } else {
            System.out.println(indent + "Internal Node (height=" + node.getHeight() + ")");
            System.out.println(indent + "  Left:");
            printTreeStructure(node.getLeft(), depth + 2);
            System.out.println(indent + "  Right:");
            printTreeStructure(node.getRight(), depth + 2);
        }
    }
    
    /**
     * Helper method to print the dendrogram node structure recursively for debugging
     */
    private void printDendrogramNodeStructure(DendrogramView.Node node, int depth) {
        if (node == null) return;
        
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) indent.append("  ");
        
        System.out.println(indent + "Node: " + node.label + " (children: " + node.children.size() + ")");
        
        for (DendrogramView.Node child : node.children) {
            printDendrogramNodeStructure(child, depth + 1);
        }
    }
    

    /** Helper to convert TreeData.Node into DendrogramView.Node */
    private DendrogramView.Node convertTree(TreeData.Node src) {
        if (src == null) {
            return new DendrogramView.Node(""); // Empty node as fallback
        }
        
        if (src.isLeaf()) {
            return new DendrogramView.Node(src.getLanguageCode() != null ? src.getLanguageCode() : "");
        } else {
            // Create an internal node with empty label initially
            DendrogramView.Node viewNode = new DendrogramView.Node(String.format("%.2f", src.getHeight()));
            
            // Add left child if it exists
            if (src.getLeft() != null) {
                DendrogramView.Node leftChild = convertTree(src.getLeft());
                viewNode.children.add(leftChild);
            }
            
            // Add right child if it exists
            if (src.getRight() != null) {
                DendrogramView.Node rightChild = convertTree(src.getRight());
                viewNode.children.add(rightChild);
            }
            
            return viewNode;
        }
    }


    /**
     * Recursively convert your model.TreeData.Node into the view.Node
     */
    private DendrogramView.Node toViewNode(TreeData.Node n) {
        if (n.isLeaf()) {
            return new DendrogramView.Node(n.getLanguageCode());
        }
        return new DendrogramView.Node(
                "",
                toViewNode(n.getLeft()),
                toViewNode(n.getRight())
        );
    }
    /**
     * Positions nodes for graph visualization using a force-directed layout algorithm.
     * 
     * @param graphData The graph data
     */
    private void positionNodesForceDirected(GraphData graphData) {
        List<GraphData.Node> nodes = graphData.getNodes();
        List<GraphData.Edge> edges = graphData.getEdges();
        int nodeCount = nodes.size();
        
        // Initialize node positions in a circle
        for (int i = 0; i < nodeCount; i++) {
            double angle = 2 * Math.PI * i / nodeCount;
            double radius = 300;
            
            GraphData.Node node = nodes.get(i);
            node.setX(400 + radius * Math.cos(angle));
            node.setY(300 + radius * Math.sin(angle));
        }
        
        // Simple force-directed layout
        // Attractive forces along edges, repulsive forces between all nodes
        double k = 300.0; // Optimal distance
        int iterations = 50;
        
        for (int iter = 0; iter < iterations; iter++) {
            // Calculate forces
            double[][] forces = new double[nodeCount][2];
            
            // Repulsive forces
            for (int i = 0; i < nodeCount; i++) {
                GraphData.Node nodeI = nodes.get(i);
                for (int j = 0; j < nodeCount; j++) {
                    if (i == j) continue;
                    
                    GraphData.Node nodeJ = nodes.get(j);
                    double dx = nodeI.getX() - nodeJ.getX();
                    double dy = nodeI.getY() - nodeJ.getY();
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    if (distance < 1) distance = 1;
                    
                    // Repulsive force is proportional to 1/distance^2
                    double force = k * k / distance;
                    forces[i][0] += dx / distance * force;
                    forces[i][1] += dy / distance * force;
                }
            }
            
            // Attractive forces
            for (GraphData.Edge edge : edges) {
                int source = edge.getSource();
                int target = edge.getTarget();
                
                GraphData.Node nodeSource = nodes.get(source);
                GraphData.Node nodeTarget = nodes.get(target);
                
                double dx = nodeSource.getX() - nodeTarget.getX();
                double dy = nodeSource.getY() - nodeTarget.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);
                
                if (distance < 1) distance = 1;
                
                // Attractive force is proportional to distance^2 / k
                double force = distance * distance / k;
                
                // Scale by edge weight (1-weight because lower weight = stronger connection)
                force *= (1.0 - edge.getWeight());
                
                forces[source][0] -= dx / distance * force;
                forces[source][1] -= dy / distance * force;
                forces[target][0] += dx / distance * force;
                forces[target][1] += dy / distance * force;
            }
            
            // Apply forces
            double damping = 0.9;
            for (int i = 0; i < nodeCount; i++) {
                GraphData.Node node = nodes.get(i);
                node.setX(node.getX() + forces[i][0] * damping);
                node.setY(node.getY() + forces[i][1] * damping);
                
                // Keep nodes within bounds
                node.setX(Math.max(50, Math.min(750, node.getX())));
                node.setY(Math.max(50, Math.min(550, node.getY())));
            }
        }
    }
    
    /**
     * Draws a network graph visualization.
     * 
     * @param gc The graphics context
     * @param graphData The graph data
     */
    private void drawNetworkGraph(GraphicsContext gc, GraphData graphData) {
        List<GraphData.Node> nodes = graphData.getNodes();
        List<GraphData.Edge> edges = graphData.getEdges();
        
        // Clear canvas
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, 800, 600);
        
        // Draw edges
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1.0);
        
        for (GraphData.Edge edge : edges) {
            GraphData.Node source = nodes.get(edge.getSource());
            GraphData.Node target = nodes.get(edge.getTarget());
            
            // Line width based on weight (lower weight = stronger connection)
            double lineWidth = 3.0 * (1.0 - edge.getWeight());
            gc.setLineWidth(Math.max(0.5, lineWidth));
            
            gc.strokeLine(source.getX(), source.getY(), target.getX(), target.getY());
            
            // Draw weight as text near the middle of the edge
            double midX = (source.getX() + target.getX()) / 2;
            double midY = (source.getY() + target.getY()) / 2;
            
            // Only show weight for significant connections
            if (edge.getWeight() <= 0.5) {
                gc.setFill(Color.DARKGRAY);
                gc.setFont(new Font(10));
                gc.fillText(String.format("%.1f%%", edge.getWeight() * 100), midX, midY);
            }
        }
        
        // Draw nodes
        gc.setFont(new Font(12));
        for (GraphData.Node node : nodes) {
            // Draw node
            gc.setFill(Color.DODGERBLUE);
            gc.fillOval(node.getX() - 5, node.getY() - 5, 10, 10);
            
            // Draw node label
            gc.setFill(Color.BLACK);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(node.getLanguageCode(), node.getX(), node.getY() - 10);
        }
        
        // Draw title
        gc.setFont(new Font(16));
        gc.setFill(Color.BLACK);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Language Distance Network", 400, 30);
    }
    
    /**
     * Draws a phylogenetic tree visualization.
     * 
     * @param gc The graphics context
     * @param treeData The tree data
     */
    private void drawPhylogeneticTree(GraphicsContext gc, TreeData treeData) {
        try {
            // Clear canvas
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, 800, 600);
            
            // Draw title
            gc.setFont(new Font(16));
            gc.setFill(Color.BLACK);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("Arbre Poligenetic", 400, 30);
            
            // Get the root node safely
            TreeData.Node root = treeData.getRoot();
            if (root == null) {
                gc.fillText("Unable to generate tree - insufficient data", 400, 300);
                return;
            }
            
            // Calculate tree dimensions
            double treeHeight = calculateTreeHeight(root);
            if (treeHeight <= 0) treeHeight = 1.0; // Avoid division by zero
            
            int leafCount = countLeaves(root);
            if (leafCount <= 0) leafCount = 1; // Avoid division by zero
            
            double verticalScale = 500.0 / Math.max(leafCount, treeData.getLanguageCodes().size());
            double horizontalScale = 700.0 / treeHeight;
            
            // Draw tree
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.5);
            
            // Start with root at left
            TreeLayout layout = new TreeLayout();
            layout.totalLeaves = leafCount;
            layout.currentLeaf = 0;
            
            // Draw tree vertically (root on left, leaves on right)
            drawVerticalTree(gc, root, 100, 400, horizontalScale, verticalScale, layout);
        } catch (Exception e) {
            // Handle any exceptions gracefully
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, 800, 600);
            
            gc.setFont(new Font(16));
            gc.setFill(Color.RED);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("Error drawing phylogenetic tree", 400, 280);
            gc.setFont(new Font(12));
            gc.fillText(e.getMessage(), 400, 320);
            
            e.printStackTrace();
        }
    }
    
    /**
     * Helper class for tree layout.
     */
    private static class TreeLayout {
        int totalLeaves;
        int currentLeaf;
    }
    
    /**
     * Calculates the height of a tree.
     * 
     * @param node The root node
     * @return The tree height
     */
    private double calculateTreeHeight(TreeData.Node node) {
        if (node == null) return 0;
        return node.getHeight();
    }
    
    /**
     * Counts the number of leaf nodes in a tree.
     * 
     * @param node The root node
     * @return The number of leaf nodes
     */
    private int countLeaves(TreeData.Node node) {
        if (node == null) return 0;
        if (node.isLeaf()) return 1;
        return countLeaves(node.getLeft()) + countLeaves(node.getRight());
    }
    
    /**
     * Draws a vertical phylogenetic tree (root on left, leaves on right).
     * 
     * @param gc The graphics context
     * @param node The root node
     * @param x The x coordinate of the root
     * @param y The y coordinate of the root
     * @param horizontalScale The horizontal scale
     * @param verticalScale The vertical scale
     * @param layout The tree layout
     */
    private void drawVerticalTree(GraphicsContext gc, TreeData.Node node, double x, double y, 
                               double horizontalScale, double verticalScale, TreeLayout layout) {
        if (node == null) return;
        
        // Draw root
        gc.setFill(Color.BLACK);
        gc.fillOval(x - 4, y - 4, 8, 8);
        
        // Calculate positions and tree layout
        calculateNodePositions(node, x, 50, 750, 700.0, horizontalScale);
        
        // Draw tree recursively
        drawTreeRecursive(gc, node);
        
        // Draw labels for all nodes
        drawNodeLabels(gc, node);
    }
    
    /**
     * Calculates positions for all nodes in the tree.
     * 
     * @param node The node
     * @param rootX The root x coordinate
     * @param minY The minimum y coordinate
     * @param maxY The maximum y coordinate
     * @param width The width of the tree
     * @param horizontalScale The horizontal scale
     * @return The number of leaves in this subtree
     */
    private int calculateNodePositions(TreeData.Node node, double rootX, double minY, double maxY, 
                                     double width, double horizontalScale) {
        if (node == null) return 0;
        
        // Set x coordinate based on height (distance from root)
        node.setX(rootX + node.getHeight() * horizontalScale);
        
        // If leaf, set y coordinate directly
        if (node.isLeaf()) {
            node.setY((minY + maxY) / 2);
            return 1;
        }
        
        // Calculate positions for left and right children
        int leftLeaves = calculateNodePositions(node.getLeft(), 
                                             rootX, minY, (minY + maxY) / 2, width, horizontalScale);
        int rightLeaves = calculateNodePositions(node.getRight(), 
                                              rootX, (minY + maxY) / 2, maxY, width, horizontalScale);
        
        // Set y coordinate as average of children
        double leftY = node.getLeft() != null ? node.getLeft().getY() : minY;
        double rightY = node.getRight() != null ? node.getRight().getY() : maxY;
        node.setY((leftY + rightY) / 2);
        
        return leftLeaves + rightLeaves;
    }
    
    /**
     * Recursively draws the tree structure (lines and nodes).
     * 
     * @param gc The graphics context
     * @param node The node to draw
     */
    private void drawTreeRecursive(GraphicsContext gc, TreeData.Node node) {
        if (node == null) return;
        
        // Draw this node
        if (node.isLeaf()) {
            // Draw leaf node
            gc.setFill(Color.DODGERBLUE);
            gc.fillOval(node.getX() - 4, node.getY() - 4, 8, 8);
        } else {
            // Draw internal node
            gc.setFill(Color.BLACK);
            gc.fillOval(node.getX() - 3, node.getY() - 3, 6, 6);
            
            // Draw height
            gc.setFill(Color.DARKGRAY);
            gc.setFont(new Font(9));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(String.format("%.2f", node.getHeight()), node.getX(), node.getY() - 10);
            
            // Draw lines to children
            if (node.getLeft() != null) {
                // Draw horizontal lines
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(1.0);
                
                // Horizontal lines to children's x positions
                gc.strokeLine(node.getX(), node.getY(), node.getLeft().getX(), node.getY());
                
                // Vertical line to left child
                gc.strokeLine(node.getLeft().getX(), node.getY(), node.getLeft().getX(), node.getLeft().getY());
            }
            
            if (node.getRight() != null) {
                // Draw horizontal lines
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(1.0);
                
                // Horizontal lines to children's x positions
                gc.strokeLine(node.getX(), node.getY(), node.getRight().getX(), node.getY());
                
                // Vertical line to right child
                gc.strokeLine(node.getRight().getX(), node.getY(), node.getRight().getX(), node.getRight().getY());
            }
            
            // Recurse on children
            drawTreeRecursive(gc, node.getLeft());
            drawTreeRecursive(gc, node.getRight());
        }
    }
    
    /**
     * Draws labels for all nodes in the tree.
     * 
     * @param gc The graphics context
     * @param node The node
     */
    private void drawNodeLabels(GraphicsContext gc, TreeData.Node node) {
        if (node == null) return;
        
        if (node.isLeaf()) {
            // Draw leaf label
            gc.setFill(Color.BLACK);
            gc.setFont(new Font(12));
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(node.getLanguageCode(), node.getX() + 10, node.getY() + 4);
        }
        
        // Recurse on children
        drawNodeLabels(gc, node.getLeft());
        drawNodeLabels(gc, node.getRight());
    }
    
    /**
     * Shows an error message.
     * 
     * @param message The error message
     */
    public void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("An error occurred");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    @Override
    public void onEvent(NotificationService.Event event) {
        // Events are handled by the controller and call the appropriate methods
    }
    
    /**
     * Launches the application with the required JavaFX modules.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            // Create components for initialization
            notificationService = new NotificationServiceImpl();
            model = new LexModel(notificationService);
            controller = new LexController();
            
            // Launch the JavaFX application
            launch(args);
        } catch (Exception e) {
            System.err.println("Error launching application: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
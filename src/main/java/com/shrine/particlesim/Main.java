package com.shrine.particlesim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

public class Main extends Application {
    private static final ArrayList<Particle> particles = new ArrayList<>();
    private static final ArrayList<Sphere> particleSpheres = new ArrayList<>();
    private static ExecutorService executor;
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    // JavaFx
    private static final double WINDOW_WIDTH = 1000;
    private static final double WINDOW_HEIGHT = 1000;

    private static final double CAMERA_NEAR_CLIP = 0.1;
    private static final double CAMERA_FAR_CLIP = 100000.0; // Increased from 10000 to 100000
    private static final double CAMERA_INITIAL_Z = -500;

    private static final double BASE_MOVE_SPEED = 100.0;
    private static final double MOUSE_SENSITIVITY = 0.1;

    private PerspectiveCamera camera;
    private Group root;
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

    // Variables for mouse dragging
    private double lastMouseX = -1;
    private double lastMouseY = -1;

    // Constants
    private static final double STRONG_FORCE_CONSTANT = 6e5;
    private static final double GRAVITY_CONSTANT = 1e1;
    private static final double COULOMB_CONSTANT = 4e5;
    private static final double STRONG_FORCE_INNER_RADIUS = 9;
    private static final double STRONG_FORCE_OUTER_RADIUS = 27; 
    private static final double MAX_SPEED = 1e4; 
    private static final double FPS = 60;
    
    // Random
    private static Random rand;

    // Electron
    protected static final double electronCharge = -1; // Original = -1.6-19 
    protected static final double electronMass = 10; // Original = 9.109e-31
    // Proton
    protected static final double protonCharge = 1; // Original = 1.6e-19
    protected static final double protonMass = 100; // Original = 1.67262158e-29

    // Neutron
    protected static final double neutronCharge = 0;
    protected static final double neutronMass = 100; // Original = 1.67492749804e-29

    private static PhongMaterial electronMaterial;
    private static PhongMaterial protonMaterial;
    private static PhongMaterial neutronMaterial;

    private static double boundarySize = 500; // Default boundary size (half of window size)
    private TextField boundarySizeField;

    // UI Controls
    private TextField particleCountField;
    private ComboBox<String> particleTypeComboBox;
    private Button spawnButton;
    
    private void initializeMaterials() {
        electronMaterial = new PhongMaterial();
        electronMaterial.setDiffuseColor(Color.BLUE);
        electronMaterial.setSpecularColor(Color.LIGHTBLUE);
        electronMaterial.setSpecularPower(32);

        protonMaterial = new PhongMaterial();
        protonMaterial.setDiffuseColor(Color.RED);
        protonMaterial.setSpecularColor(Color.PINK);
        protonMaterial.setSpecularPower(32);

        neutronMaterial = new PhongMaterial();
        neutronMaterial.setDiffuseColor(Color.GRAY);
        neutronMaterial.setSpecularColor(Color.WHITE);
        neutronMaterial.setSpecularPower(32);
    }
    
    public static double getMaxSpeed() {
        return MAX_SPEED; // Default max speed
    }

    public static double getStrongForceOuterRadius() {
        return STRONG_FORCE_OUTER_RADIUS; // Default strong force outer radius
    }

    public static double getStrongForceInnerRadius() {
        return STRONG_FORCE_INNER_RADIUS; // Default strong force inner radius
    }
    
    public static double getFPS() {
        return FPS; // Default FPS
    }

    public static double getCoulombConstant() {
        return COULOMB_CONSTANT; // Default Coulomb constant
    }
    
    public static double getStrongForceConstant() {
        return STRONG_FORCE_CONSTANT; // Default strong force constant
    }
    
    public static double getGravityConstant() {
        return GRAVITY_CONSTANT; // Default gravity constant
    }

    public static double getBoundarySize() {
        return boundarySize;
    }
    
    private double getScaledMoveSpeed() {
        return BASE_MOVE_SPEED * (boundarySize / 500.0); // Scale with boundary size relative to default
    }

    @Override
    public void start(Stage primaryStage) {
        executor = Executors.newFixedThreadPool(NUM_THREADS);
        
        initializeMaterials();
        root = new Group();
        rand = new Random();

        // Create UI controls
        VBox controls = new VBox(10); // 10 pixels spacing
        controls.setAlignment(Pos.TOP_LEFT);
        controls.setPadding(new Insets(10));
        controls.setStyle("-fx-background-color: rgba(255, 255, 255, 0.7);");
        controls.setMaxWidth(200);
        controls.setMouseTransparent(false);
        
        Label countLabel = new Label("Number of Particles:");
        particleCountField = new TextField("10");
        particleCountField.setPrefWidth(100);
        
        Label typeLabel = new Label("Particle Type:");
        particleTypeComboBox = new ComboBox<>();
        particleTypeComboBox.getItems().addAll("electron", "proton", "neutron");
        particleTypeComboBox.setValue("electron");

        Label boundaryLabel = new Label("Boundary Size:");
        boundarySizeField = new TextField(String.valueOf((int)boundarySize));
        boundarySizeField.setPrefWidth(100);
        boundarySizeField.setOnAction(e -> updateBoundarySize());
        boundarySizeField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                updateBoundarySize();
            }
        });
        
        spawnButton = new Button("Spawn Particles");
        spawnButton.setOnAction(e -> spawnParticles());

        Button clearButton = new Button("Clear Particles");
        clearButton.setOnAction(e -> clearParticles());
        
        controls.getChildren().addAll(
            countLabel,
            particleCountField,
            typeLabel,
            particleTypeComboBox,
            boundaryLabel,
            boundarySizeField,
            spawnButton,
            clearButton
        );

        // Reduce the number of particles for better performance
        for (int i = 0; i < 300; i++) {
            double x = rand.nextDouble() * boundarySize * 2 - boundarySize;
            double y = rand.nextDouble() * boundarySize * 2 - boundarySize;
            double z = rand.nextDouble() * boundarySize * 2 - boundarySize;

            // Add particles with more spacing
            particles.add(new Particle(x, y, z, rand.nextDouble(-5, 5), rand.nextDouble(-5, 5), rand.nextDouble(-5, 5), electronCharge, electronMass, "electron"));
            particles.add(new Particle(x + 100, y + 100, z + 100, rand.nextDouble(-5, 5), rand.nextDouble(-5, 5), rand.nextDouble(-5, 5), protonCharge, protonMass, "proton"));
            particles.add(new Particle(x - 100, y - 100, z - 100, rand.nextDouble(-5, 5), rand.nextDouble(-5, 5), rand.nextDouble(-5, 5), neutronCharge, neutronMass, "neutron"));
        }

        // Pre-create and configure all sphere objects
        for (Particle p : particles) {
            Sphere particleSphere = new Sphere(p.getRadius());
            if (p.getParticleType().equals("electron")) {
                particleSphere.setMaterial(electronMaterial);
            } else if (p.getParticleType().equals("proton")) {
                particleSphere.setMaterial(protonMaterial);
            } else {
                particleSphere.setMaterial(neutronMaterial);
            }
            particleSpheres.add(particleSphere);
            root.getChildren().add(particleSphere);
        }

        // Add multiple light sources for better depth perception
        AmbientLight ambientLight = new AmbientLight(Color.rgb(50, 50, 50));
        root.getChildren().add(ambientLight);
        
        // Add directional lights from different angles
        PointLight mainLight = new PointLight(Color.WHITE);
        mainLight.setTranslateX(boundarySize * 2);
        mainLight.setTranslateY(-boundarySize * 2);
        mainLight.setTranslateZ(-boundarySize * 2);
        
        PointLight fillLight = new PointLight(Color.rgb(200, 200, 255));
        fillLight.setTranslateX(-boundarySize * 2);
        fillLight.setTranslateY(boundarySize);
        fillLight.setTranslateZ(boundarySize * 2);
        
        root.getChildren().addAll(mainLight, fillLight);

        // --- 3. Setup the Camera ---
        camera = new PerspectiveCamera(true); // true = fixed eye at (0,0,0) initially
        camera.setNearClip(CAMERA_NEAR_CLIP);
        camera.setFarClip(CAMERA_FAR_CLIP);

        Translate pivot = new Translate(); // Used for positioning
        camera.getTransforms().addAll(
                pivot, // Controls position
                rotateY, // Controls rotation around Y
                rotateX // Controls rotation around X
        );
        pivot.setZ(CAMERA_INITIAL_Z);

        // Create a StackPane to hold both the 3D content and the 2D overlay
        StackPane stackPane = new StackPane();
        SubScene subScene = new SubScene(root, WINDOW_WIDTH, WINDOW_HEIGHT, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.LIGHTGRAY);
        subScene.setCamera(camera);
        
        stackPane.getChildren().addAll(subScene, controls);
        StackPane.setAlignment(controls, Pos.TOP_LEFT);
        
        // Create the main scene with the StackPane
        Scene scene = new Scene(stackPane, WINDOW_WIDTH, WINDOW_HEIGHT);

        // --- 5. Setup Input Handlers ---
        setupKeyHandlers(scene, pivot);
        setupMouseHandlers(subScene);
        setupScrollHandler(subScene, pivot);

        AnimationTimer gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;
            private static final long FRAME_INTERVAL = 16_666_666L; // 60 FPS in nanoseconds

            @Override
            public void handle(long now) {
                if (now - lastUpdate < FRAME_INTERVAL) {
                    return; // Skip this frame if not enough time has passed
                }
                lastUpdate = now;

                // Reset forces
                particles.parallelStream().forEach(Particle::resetForce);

                // Calculate forces in parallel
                List<Future<Void>> futures = new ArrayList<>();
                int chunkSize = particles.size() / NUM_THREADS;
                for (int i = 0; i < NUM_THREADS; i++) {
                    int startIndex = i * chunkSize;
                    int endIndex = (i == NUM_THREADS - 1) ? particles.size() : startIndex + chunkSize;
                    futures.add(executor.submit(new ForceCalculationTask(startIndex, endIndex, particles)));
                }

                // Wait for force calculations to complete
                for (Future<Void> future : futures) {
                    try {
                        future.get();
                    } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                        Thread.currentThread().interrupt();
                        // Log the error appropriately instead of printStackTrace
                        System.err.println("Error in force calculation: " + e.getMessage());
                    }
                }

                // Update velocities and positions
                for (Particle p : particles) {
                    p.updateVelocity();
                    p.updatePos();
                }

                // Update sphere positions on the JavaFX thread
                for (int i = 0; i < particles.size(); i++) {
                    Particle p = particles.get(i);
                    Sphere particleSphere = particleSpheres.get(i);
                    particleSphere.setTranslateX(p.getxCor());
                    particleSphere.setTranslateY(p.getyCor());
                    particleSphere.setTranslateZ(p.getzCor());
                }
            }
        };
        gameLoop.start();
        // --- 6. Setup and Show the Stage ---
        primaryStage.setTitle("Particle Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Bind subscene size to window size
        subScene.widthProperty().bind(stackPane.widthProperty());
        subScene.heightProperty().bind(stackPane.heightProperty());
    }

    @Override
    public void stop() throws Exception {
        // Shutdown the thread pool gracefully
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
        super.stop();
    }

    private void setupKeyHandlers(Scene scene, Translate pivot) {
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            double currentMoveSpeed = getScaledMoveSpeed();
    
            // Get the current rotation angles
            double yaw = Math.toRadians(rotateY.getAngle());
            double pitch = Math.toRadians(rotateX.getAngle());
    
            // Movement vector components
            double forwardX = Math.sin(yaw);
            double forwardZ = Math.cos(yaw);
            double forwardY = Math.sin(pitch);
    
            double rightX = Math.cos(yaw);
            double rightZ = -Math.sin(yaw);
    
            switch (code) {
                case W:
                    pivot.setX(pivot.getX() + forwardX * currentMoveSpeed);
                    pivot.setY(pivot.getY() - forwardY * currentMoveSpeed);
                    pivot.setZ(pivot.getZ() + forwardZ * currentMoveSpeed);
                    break;
                case S:
                    pivot.setX(pivot.getX() - forwardX * currentMoveSpeed);
                    pivot.setY(pivot.getY() + forwardY * currentMoveSpeed);
                    pivot.setZ(pivot.getZ() - forwardZ * currentMoveSpeed);
                    break;
                case A:
                    pivot.setX(pivot.getX() - rightX * currentMoveSpeed);
                    pivot.setZ(pivot.getZ() - rightZ * currentMoveSpeed);
                    break;
                case D:
                    pivot.setX(pivot.getX() + rightX * currentMoveSpeed);
                    pivot.setZ(pivot.getZ() + rightZ * currentMoveSpeed);
                    break;
                case SPACE:
                    pivot.setY(pivot.getY() - currentMoveSpeed);
                    break;
                case SHIFT:
                    pivot.setY(pivot.getY() + currentMoveSpeed);
                    break;
                default:
                    break;
            }
        });
    }

     private void setupMouseHandlers(SubScene subScene) {
        subScene.setOnMousePressed((MouseEvent event) -> {
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        });

        subScene.setOnMouseDragged((MouseEvent event) -> {
            double deltaX = event.getSceneX() - lastMouseX;
            double deltaY = event.getSceneY() - lastMouseY;

            double newAngleY = rotateY.getAngle() + deltaX * MOUSE_SENSITIVITY;
            rotateY.setAngle(newAngleY);

            double newAngleX = rotateX.getAngle() - deltaY * MOUSE_SENSITIVITY;

            // Clamp pitch to avoid flipping upside down
             if (newAngleX > 90) {
                newAngleX = 90;
            } else if (newAngleX < -90) {
                newAngleX = -90;
            } 
            rotateX.setAngle(newAngleX);


            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        });
    }

    private void setupScrollHandler(SubScene subScene, Translate pivot) {
        subScene.setOnScroll(event -> {
            double currentMoveSpeed = getScaledMoveSpeed();
            double zoomFactor = event.getDeltaY() > 0 ? currentMoveSpeed : -currentMoveSpeed;
            pivot.setZ(pivot.getZ() + zoomFactor);
        });
    }

    private void spawnParticles() {
        try {
            int count = Integer.parseInt(particleCountField.getText());
            String type = particleTypeComboBox.getValue();
            
            for (int i = 0; i < count; i++) {
                double x = rand.nextDouble() * boundarySize * 2 - boundarySize;
                double y = rand.nextDouble() * boundarySize * 2 - boundarySize;
                double z = rand.nextDouble() * boundarySize * 2 - boundarySize;
                
                Particle newParticle;
                switch (type) {
                    case "electron":
                        newParticle = new Particle(x, y, z, rand.nextDouble(-5, 5), 
                            rand.nextDouble(-5, 5), rand.nextDouble(-5, 5), 
                            electronCharge, electronMass, "electron");
                        break;
                    case "proton":
                        newParticle = new Particle(x, y, z, rand.nextDouble(-5, 5), 
                            rand.nextDouble(-5, 5), rand.nextDouble(-5, 5), 
                            protonCharge, protonMass, "proton");
                        break;
                    default: // neutron
                        newParticle = new Particle(x, y, z, rand.nextDouble(-5, 5), 
                            rand.nextDouble(-5, 5), rand.nextDouble(-5, 5), 
                            neutronCharge, neutronMass, "neutron");
                        break;
                }
                
                particles.add(newParticle);
                
                Sphere particleSphere = new Sphere(newParticle.getRadius());
                if (type.equals("electron")) {
                    particleSphere.setMaterial(electronMaterial);
                } else if (type.equals("proton")) {
                    particleSphere.setMaterial(protonMaterial);
                } else {
                    particleSphere.setMaterial(neutronMaterial);
                }
                particleSpheres.add(particleSphere);
                root.getChildren().add(particleSphere);
            }
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText(null);
            alert.setContentText("Please enter a valid number of particles.");
            alert.showAndWait();
        }
    }

    private void clearParticles() {
        // Remove all sphere objects from the 3D scene
        root.getChildren().removeAll(particleSpheres);
        // Clear the collections
        particles.clear();
        particleSpheres.clear();
    }

    private void updateBoundarySize() {
        try {
            double newSize = Double.parseDouble(boundarySizeField.getText());
            if (newSize > 0) {
                boundarySize = newSize;
            } else {
                boundarySizeField.setText(String.valueOf((int)boundarySize));
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Input");
                alert.setHeaderText(null);
                alert.setContentText("Boundary size must be greater than 0");
                alert.showAndWait();
            }
        } catch (NumberFormatException e) {
            boundarySizeField.setText(String.valueOf((int)boundarySize));
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText(null);
            alert.setContentText("Please enter a valid number.");
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
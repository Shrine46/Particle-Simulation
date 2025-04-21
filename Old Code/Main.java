package com.shrine.particlesim;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Main extends Application {
    // Lists and collections
    private ArrayList<Particle> particles = new ArrayList<>();
    private Group particleGroup = new Group();

    // 3D scene components
    private SubScene simulationSpace;
    private Group root3D;
    private PerspectiveCamera camera;

    // Camera control variables
    private double mouseOldX, mouseOldY;
    private double mousePosX, mousePosY;
    private final double cameraDistance = 1000;
    private double cameraXAngle = 0;
    private double cameraYAngle = 0;

    // Random generator
    private static Random rand = new Random();

    // Animation
    private AnimationTimer timer;

    // Simulation constants
    // Electron
    protected static final double electronCharge = -1; // Original = -1.6-19
    protected static final double electronMass = 10; // Original = 9.109e-31
    // Proton
    protected static final double protonCharge = 1; // Original = 1.6e-19
    protected static final double protonMass = 100; // Original = 1.67262158e-29
    // Neutron
    protected static final double neutronCharge = 0;
    protected static final double neutronMass = 100; // Original = 1.67492749804e-29

    // Simulation parameters with default values
    private static double maxSpeed = 1e7;
    private static double strongForceOuterRadius = 27;
    private static double strongForceInnerRadius = 9;
    private static double coulombConstant = 4e5;
    private static double strongForceConstant = 6e5;
    private static double gravityConstant = 1e1;

    // UI controls
    private Slider chargeSlider;
    private Slider massSlider;
    private Slider decaySlider;

    @Override
    public void start(Stage primaryStage) {
        // Set up the 3D environment
        setupScene(primaryStage);

        // Set up UI controls
        setupControls(primaryStage);

        // Set up the animation loop
        setupAnimationLoop();

        // Display the stage
        primaryStage.show();
    }

    private void setupScene(Stage primaryStage) {
        // Create main BorderPane layout
        BorderPane mainLayout = new BorderPane();

        // Create 3D content
        root3D = new Group();

        // Add a simple sphere as a reference point
        Sphere referenceSphere = new Sphere(5);
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.GRAY);
        referenceSphere.setMaterial(material);
        root3D.getChildren().add(referenceSphere);

        // Add particle group to root
        root3D.getChildren().add(particleGroup);

        // Create camera
        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setTranslateZ(-cameraDistance);

        // Create the 3D subscene
        simulationSpace = new SubScene(root3D, 800, 600, true, javafx.scene.SceneAntialiasing.BALANCED);
        simulationSpace.setFill(Color.BLACK);
        simulationSpace.setCamera(camera);

        // Set up camera rotation with mouse
        setupCameraControls();

        // Add simulation space to the center of the layout
        mainLayout.setCenter(simulationSpace);

        // Create the main scene
        Scene scene = new Scene(mainLayout, 1200, 800);

        // Add key bindings for particle creation
        setupKeyBindings(scene);

        // Configure the primary stage
        primaryStage.setTitle("3D Particle Simulation");
        primaryStage.setScene(scene);
    }

    private void setupCameraControls() {
        simulationSpace.setOnMousePressed(event -> {
            mouseOldX = event.getSceneX();
            mouseOldY = event.getSceneY();
        });

        simulationSpace.setOnMouseDragged(event -> {
            mousePosX = event.getSceneX();
            mousePosY = event.getSceneY();

            double deltaX = (mousePosX - mouseOldX);
            double deltaY = (mousePosY - mouseOldY);

            // Explicitly check for primary (left) button
            if (event.isPrimaryButtonDown()) {
                // Rotate camera
                cameraXAngle -= deltaY * 0.1;
                cameraYAngle += deltaX * 0.1;

                updateCameraPosition();
            }

            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
        });

        simulationSpace.setOnScroll(event -> {
            // Zoom camera
            double delta = event.getDeltaY() * 5;
            camera.setTranslateZ(camera.getTranslateZ() + delta);
        });
    }

    private void updateCameraPosition() {
        // Limit vertical rotation
        cameraXAngle = Math.max(-85, Math.min(85, cameraXAngle));

        // Convert spherical to Cartesian coordinates
        double radXAngle = Math.toRadians(cameraXAngle);
        double radYAngle = Math.toRadians(cameraYAngle);

        double x = cameraDistance * Math.cos(radXAngle) * Math.sin(radYAngle);
        double y = cameraDistance * Math.sin(radXAngle);
        double z = cameraDistance * Math.cos(radXAngle) * Math.cos(radYAngle);

        // Update camera position
        camera.setTranslateX(x);
        camera.setTranslateY(y);
        camera.setTranslateZ(z);

        // Create a Transform to point the camera at the origin
        lookAtOrigin();
    }

    private void lookAtOrigin() {
        // Get the camera's position
        double x = camera.getTranslateX();
        double y = camera.getTranslateY();
        double z = camera.getTranslateZ();

        // Calculate the direction to the origin
        double dirX = -x;
        double dirY = -y;
        double dirZ = -z;

        // Normalize the direction
        double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX /= length;
        dirY /= length;
        dirZ /= length;

        // Calculate the rotation around Y axis (yaw)
        double yaw = Math.toDegrees(Math.atan2(dirX, dirZ));

        // Calculate the rotation around X axis (pitch)
        double pitch = Math.toDegrees(Math.asin(-dirY));

        // Apply rotations - JavaFX camera needs to be oriented correctly
        camera.setRotate(0); // Reset rotation
        camera.setRotationAxis(javafx.scene.transform.Rotate.Y_AXIS);
        camera.setRotate(yaw);

        // Create a rotate transform for pitch
        javafx.scene.transform.Rotate pitchRotate =
                new javafx.scene.transform.Rotate(pitch, javafx.scene.transform.Rotate.X_AXIS);

        // Apply the transforms
        camera.getTransforms().clear();
        camera.getTransforms().add(pitchRotate);
    }

    private void setupControls(Stage primaryStage) {
        VBox controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setStyle("-fx-background-color: #f0f0f0;");

        // Add clear particles button
        Button clearButton = new Button("Clear Particles");
        clearButton.setOnAction(e -> {
            particles.clear();
            particleGroup.getChildren().clear();
        });
        controlPanel.getChildren().add(clearButton);

        // Add particle property sliders
        chargeSlider = createSlider("Charge", -100, 100, 0);
        massSlider = createSlider("Mass", 1, 2000, 1);
        decaySlider = createSlider("Decay Speed", 0, 100, 0);

        controlPanel.getChildren().addAll(
                new Label("Particle Properties:"),
                chargeSlider,
                massSlider,
                decaySlider
        );

        // Create physics parameter sliders
        Slider fpsSlider = createSlider("FPS", 1, 120, 60);
        fpsSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // FPS changes handled in animation timer
        });

        Slider strongForceOuterRadiusSlider = createSlider("Strong Force Outer Radius", 10, 50, 27);
        strongForceOuterRadiusSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                strongForceOuterRadius = newVal.doubleValue());

        Slider strongForceInnerRadiusSlider = createSlider("Strong Force Inner Radius", 1, 20, 9);
        strongForceInnerRadiusSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                strongForceInnerRadius = newVal.doubleValue());

        // Create exponential sliders for constants
        HBox maxSpeedBox = createExponentialSlider("Max Speed", 1, 9, 1, 1, 50, 7,
                (base, exp) -> maxSpeed = base * Math.pow(10, exp));

        HBox coulombBox = createExponentialSlider("Coulomb Constant", 1, 9, 4, 1, 50, 5,
                (base, exp) -> coulombConstant = base * Math.pow(10, exp));

        HBox strongForceBox = createExponentialSlider("Strong Force Constant", 1, 9, 6, 1, 50, 5,
                (base, exp) -> strongForceConstant = base * Math.pow(10, exp));

        HBox gravityBox = createExponentialSlider("Gravity Constant", 1, 9, 1, 1, 50, 1,
                (base, exp) -> gravityConstant = base * Math.pow(10, exp));

        controlPanel.getChildren().addAll(
                new Separator(),
                new Label("Physics Parameters:"),
                fpsSlider,
                strongForceOuterRadiusSlider,
                strongForceInnerRadiusSlider,
                maxSpeedBox,
                coulombBox,
                strongForceBox,
                gravityBox
        );

        // Add scroll pane for controls
        ScrollPane scrollPane = new ScrollPane(controlPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefWidth(300);

        // Add to main layout
        BorderPane mainLayout = (BorderPane) ((Scene) primaryStage.getScene()).getRoot();
        mainLayout.setRight(scrollPane);

        // Make 3D scene resize with window
        simulationSpace.widthProperty().bind(
                mainLayout.widthProperty().subtract(scrollPane.getPrefWidth()));
        simulationSpace.heightProperty().bind(mainLayout.heightProperty());
    }

    private Slider createSlider(String name, double min, double max, double defaultValue) {
        Label label = new Label(name + ": " + defaultValue);
        Slider slider = new Slider(min, max, defaultValue);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            label.setText(name + ": " + Math.round(newVal.doubleValue() * 100) / 100.0);
        });

        VBox sliderBox = new VBox(5, label, slider);

        return slider;
    }

    private HBox createExponentialSlider(
            String name,
            double baseMin, double baseMax, double baseDefault,
            double expMin, double expMax, double expDefault,
            ExponentialValueListener listener) {

        VBox container = new VBox(5);

        Label titleLabel = new Label(name + ": " + baseDefault + "e" + expDefault);

        Slider baseSlider = new Slider(baseMin, baseMax, baseDefault);
        baseSlider.setShowTickMarks(true);
        baseSlider.setShowTickLabels(true);
        baseSlider.setMajorTickUnit((baseMax - baseMin) / 8);

        Slider expSlider = new Slider(expMin, expMax, expDefault);
        expSlider.setShowTickMarks(true);
        expSlider.setShowTickLabels(true);
        expSlider.setMajorTickUnit((expMax - expMin) / 10);

        Label baseLabel = new Label("Base: " + baseDefault);
        Label expLabel = new Label("Exponent: e" + expDefault);

        baseSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double base = Math.round(newVal.doubleValue() * 10) / 10.0;
            double exp = expSlider.getValue();
            baseLabel.setText("Base: " + base);
            titleLabel.setText(name + ": " + base + "e" + exp);
            listener.onValueChanged(base, exp);
        });

        expSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double base = baseSlider.getValue();
            double exp = newVal.intValue();
            expLabel.setText("Exponent: e" + exp);
            titleLabel.setText(name + ": " + base + "e" + exp);
            listener.onValueChanged(base, exp);
        });

        container.getChildren().addAll(
                titleLabel,
                new HBox(5, new Label("Base:"), baseSlider),
                new HBox(5, new Label("Exponent:"), expSlider)
        );

        return new HBox(container);
    }

    private interface ExponentialValueListener {
        void onValueChanged(double base, double exponent);
    }

    private void setupKeyBindings(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.E) {
                createParticle("electron", electronCharge, electronMass);
            } else if (event.getCode() == KeyCode.P) {
                createParticle("proton", protonCharge, protonMass);
            } else if (event.getCode() == KeyCode.N) {
                createParticle("neutron", neutronCharge, neutronMass);
            }
        });

        simulationSpace.setOnMouseClicked(event -> {
            // Only create particles with right mouse button
            if (event.isSecondaryButtonDown() && chargeSlider != null && massSlider != null) {
                double charge = chargeSlider.getValue();
                double mass = massSlider.getValue();
                createParticleAtMousePosition(event, "custom", charge, mass);
            }
        });
    }

    private void createParticle(String type, double charge, double mass) {
        // Create particle at random position
        double x = rand.nextDouble() * 400 - 200;
        double y = rand.nextDouble() * 400 - 200;
        double z = rand.nextDouble() * 400 - 200;

        double vx = rand.nextDouble() * 8 - 4;
        double vy = rand.nextDouble() * 8 - 4;
        double vz = rand.nextDouble() * 8 - 4;

        Particle particle = new Particle(x, y, z, vx, vy, vz, charge, mass, type);
        particles.add(particle);

        // Create 3D representation
        addParticleToScene(particle);
    }

    private void createParticleAtMousePosition(MouseEvent event, String type, double charge, double mass) {
        // Get 3D coordinates from mouse click (simplified)
        double x = (event.getX() - simulationSpace.getWidth() / 2) * 0.5;
        double y = (event.getY() - simulationSpace.getHeight() / 2) * 0.5;
        double z = 0; // Default z position

        // Random velocity
        double vx = rand.nextDouble() * 8 - 4;
        double vy = rand.nextDouble() * 8 - 4;
        double vz = rand.nextDouble() * 8 - 4;

        Particle particle = new Particle(x, y, z, vx, vy, vz, charge, mass, type);
        particles.add(particle);

        // Create 3D representation
        addParticleToScene(particle);
    }

    private void addParticleToScene(Particle particle) {
        Sphere sphere = new Sphere(particle.getRadius());

        // Set color based on particle type
        PhongMaterial material = new PhongMaterial();
        if (particle.getCharge() == 0) {
            material.setDiffuseColor(Color.GRAY); // Neutron
        } else if (particle.getCharge() > 0) {
            material.setDiffuseColor(Color.RED); // Positive charge
        } else {
            material.setDiffuseColor(Color.BLUE); // Negative charge
        }
        sphere.setMaterial(material);

        // Set initial position
        sphere.setTranslateX(particle.getxCor());
        sphere.setTranslateY(particle.getyCor());
        sphere.setTranslateZ(particle.getzCor());

        // Store reference to the sphere in the particle's userData
        particle.setUserData(sphere);

        // Add to scene
        particleGroup.getChildren().add(sphere);
    }

    private void setupAnimationLoop() {
        timer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                // Control frame rate (approx 60 FPS)
                if (lastUpdate == 0 || now - lastUpdate >= 16_666_666) { // ~60 FPS in nanoseconds
                    updateParticles();
                    lastUpdate = now;
                }
            }
        };
        timer.start();
    }

    private void updateParticles() {
        // Reset forces
        for (Particle p : particles) {
            p.resetForce();
        }

        // Calculate forces between particles
        for (int i = 0; i < particles.size(); i++) {
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p1 = particles.get(i);
                Particle p2 = particles.get(j);

                double[] forceOnP1 = p1.calculateForces(p2);
                p1.addForce(forceOnP1[0], forceOnP1[1], forceOnP1[2]);
                p2.addForce(-forceOnP1[0], -forceOnP1[1], -forceOnP1[2]);

                // Handle collisions
                handleCollision(p1, p2);
            }
        }

        // Update velocities and positions
        for (Particle p : particles) {
            p.updateVelocity();
        }

        for (Particle p : particles) {
            p.updatePos();

            // Update visual representation
            Sphere sphere = (Sphere) p.getUserData();
            if (sphere != null) {
                sphere.setTranslateX(p.getxCor());
                sphere.setTranslateY(p.getyCor());
                sphere.setTranslateZ(p.getzCor());
            }
        }
    }

    private void handleCollision(Particle p1, Particle p2) {
        double distX = p2.getxCor() - p1.getxCor();
        double distY = p2.getyCor() - p1.getyCor();
        double distZ = p2.getzCor() - p1.getzCor();
        double distSq = distX * distX + distY * distY + distZ * distZ;

        double collisionRadiusSum = p1.getRadius() + p2.getRadius();
        double collisionRadiusSumSq = collisionRadiusSum * collisionRadiusSum;

        if (distSq < collisionRadiusSumSq && distSq > 1e-9) {
            double dist = Math.sqrt(distSq);

            double overlap = collisionRadiusSum - dist;
            double dirX = distX / dist;
            double dirY = distY / dist;
            double dirZ = distZ / dist;

            double totalMass = p1.getMass() + p2.getMass();
            double pushFactor1 = (p2.getMass() / totalMass) * overlap;
            double pushFactor2 = (p1.getMass() / totalMass) * overlap;

            p1.setxCor(p1.getxCor() - dirX * pushFactor1);
            p1.setyCor(p1.getyCor() - dirY * pushFactor1);
            p1.setzCor(p1.getzCor() - dirZ * pushFactor1);

            p2.setxCor(p2.getxCor() + dirX * pushFactor2);
            p2.setyCor(p2.getyCor() + dirY * pushFactor2);
            p2.setzCor(p2.getzCor() + dirZ * pushFactor2);

            // Handle velocity changes in collision
            double relativeVelX = p2.getxVel() - p1.getxVel();
            double relativeVelY = p2.getyVel() - p1.getyVel();
            double relativeVelZ = p2.getzVel() - p1.getzVel();

            double dotProduct = relativeVelX * dirX + relativeVelY * dirY + relativeVelZ * dirZ;

            if (dotProduct < 0) {
                double elasticity = 0.7;

                double collisionScale = (1.0 + elasticity) * dotProduct / totalMass;
                double impulseFactorX = collisionScale * dirX;
                double impulseFactorY = collisionScale * dirY;
                double impulseFactorZ = collisionScale * dirZ;

                p1.setxVel(p1.getxVel() + impulseFactorX * p2.getMass());
                p1.setyVel(p1.getyVel() + impulseFactorY * p2.getMass());
                p1.setzVel(p1.getzVel() + impulseFactorZ * p2.getMass());

                p2.setxVel(p2.getxVel() - impulseFactorX * p1.getMass());
                p2.setyVel(p2.getyVel() - impulseFactorY * p1.getMass());
                p2.setzVel(p2.getzVel() - impulseFactorZ * p1.getMass());
            }
        }
    }

    // Getter methods for simulation parameters
    public static double getMaxSpeed() {
        return maxSpeed;
    }

    public static double getCoulombConstant() {
        return coulombConstant;
    }

    public static double getStrongForceOuterRadius() {
        return strongForceOuterRadius;
    }

    public static double getStrongForceInnerRadius() {
        return strongForceInnerRadius;
    }

    public static double getStrongForceConstant() {
        return strongForceConstant;
    }

    public static double getGravityConstant() {
        return gravityConstant;
    }
}
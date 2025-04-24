package com.shrine.particlesim;

import java.util.ArrayList;
import java.util.Random;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

public class Main extends Application {
    private static ArrayList<Particle> particles = new ArrayList<>();
    private static ArrayList<Sphere> particleSpheres = new ArrayList<>();

    // JavaFx
    private static final double WINDOW_WIDTH = 2000;
    private static final double WINDOW_HEIGHT = 2000;

    private static final double CAMERA_NEAR_CLIP = 0.1;
    private static final double CAMERA_FAR_CLIP = 10000.0;
    private static final double CAMERA_INITIAL_Z = -1000; // Start back from the origin

    private static final double MOVE_SPEED = 100.0; // How fast the camera moves
    private static final double MOUSE_SENSITIVITY = 0.1; // How sensitive mouse rotation is

    private PerspectiveCamera camera;
    private Group root;
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

    // Variables for mouse dragging
    private double lastMouseX = -1;
    private double lastMouseY = -1;

    // Slider Values
    private static Slider maxSpeedSlider;
    private static Slider maxSpeedMultSlider;
    private static Slider strongForceInnerRadiusSlider;
    private static Slider FPSSlider;
    private static Slider strongForceOuterRadiusSlider;
    private static Slider kConstantSlider;
    private static Slider kConstantMultSlider;
    private static Slider strongForceConstantSlider;
    private static Slider strongForceConstantMultSlider;
    private static Slider gravityConstantSlider;
    private static Slider gravityConstantMultSlider;

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

    public static double getMaxSpeed() {
        if (maxSpeedSlider == null || maxSpeedMultSlider == null) {
            return 1e; // Default max speed
        }
        return maxSpeedSlider.getValue() * Math.pow(10, maxSpeedMultSlider.getValue());
    }

    public static double getStrongForceOuterRadius() {
        if (strongForceOuterRadiusSlider == null) {
            return 27; // Default strong force outer radius
        }
        return strongForceOuterRadiusSlider.getValue();
    }

    public static double getStrongForceInnerRadius() {
        if (strongForceInnerRadiusSlider == null) {
            return 9; // Default strong force inner radius
        }
        return strongForceInnerRadiusSlider.getValue();
    }
    
    public static double getFPS() {
        if (FPSSlider == null) {
            return 60; // Default FPS
        }
        return FPSSlider.getValue();
    }

    public static double getCoulombConstant() {
        if (kConstantSlider == null || kConstantMultSlider == null) {
            return 4e5; // Default Coulomb constant
        }
        return kConstantSlider.getValue() * Math.pow(10, kConstantMultSlider.getValue());
    }
    
    public static double getStrongForceConstant() {
        if (strongForceConstantSlider == null || strongForceConstantMultSlider == null) {
            return 6e5; // Default strong force constant
        }
        return strongForceConstantSlider.getValue() * Math.pow(10, strongForceConstantMultSlider.getValue());
    }
    
    public static double getGravityConstant() {
        if (gravityConstantSlider == null || gravityConstantMultSlider == null) {
            return 1e1; // Default gravity constant
        }
        return gravityConstantSlider.getValue() * Math.pow(10, gravityConstantMultSlider.getValue());
    }
    
    @Override
    public void start(Stage primaryStage) {
        root = new Group();
        rand = new Random();

        // Add 20 of each particle type within the bounds
        for (int i = 0; i < 20; i++) {
            double x = rand.nextDouble() * WINDOW_WIDTH - WINDOW_WIDTH / 2;
            double y = rand.nextDouble() * WINDOW_HEIGHT - WINDOW_HEIGHT / 2;
            double z = rand.nextDouble() * 1000 - 500; // Random Z within a range

            // Add electrons
            particles.add(new Particle(x, y, z, rand.nextDouble(-5, 5), rand.nextDouble(-5, 5), rand.nextDouble(-5, 5), electronCharge, electronMass, "electron"));

            // Add protons
            particles.add(new Particle(x, y, z, rand.nextDouble(-5, 5), rand.nextDouble(-5, 5), rand.nextDouble(-5, 5), protonCharge, protonMass, "proton"));

            // Add neutrons
            particles.add(new Particle(x, y, z, rand.nextDouble(-5, 5), rand.nextDouble(-5, 5), rand.nextDouble(-5, 5), neutronCharge, neutronMass, "neutron"));
        }

        for (Particle p : particles) {
            Sphere particleSphere = new Sphere(p.getRadius());
            particleSpheres.add(particleSphere);
            root.getChildren().add(particleSphere);
        }

        AmbientLight light = new AmbientLight(Color.rgb(150, 150, 150)); // Soft white light
        root.getChildren().add(light);

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


        // --- 4. Create the Scene ---
        // IMPORTANT: Use true for the depthBuffer parameter to enable 3D rendering
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.LIGHTGRAY); // Background color
        scene.setCamera(camera);

        // --- 5. Setup Input Handlers ---
        setupKeyHandlers(scene, pivot);
        setupMouseHandlers(scene);
        setupScrollHandler(scene, pivot);

        AnimationTimer gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;
            private final long frameInterval = 1_000_000_000 / 60; // Target FPS (e.g., 60 FPS)

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= frameInterval) {
                    lastUpdate = now;

                    for (Particle p : particles) {
                        p.resetForce();
                    }

                    for (int i = 0; i < particles.size(); i++) {
                        for (int j = i + 1; j < particles.size(); j++) {
                            Particle p1 = particles.get(i);
                            Particle p2 = particles.get(j);
                            
                            double[] forceOnP1 = p1.calculateForces(p2);
                            p1.addForce(forceOnP1[0], forceOnP1[1], forceOnP1[2]);
                            p2.addForce(-forceOnP1[0], -forceOnP1[1], forceOnP1[2]);
                            double distX = p2.getxCor() - p1.getxCor();
                            double distY = p2.getyCor()  - p1.getyCor();
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
                                    
                                    p1.setxVel(p1.getxVel() - impulseFactorX * p2.getMass());
                                    p1.setyVel(p1.getyVel() - impulseFactorY * p2.getMass());
                                    p1.setzVel(p1.getzVel() - impulseFactorZ * p2.getMass());
                                    
                                    p2.setxVel(p2.getxVel() + impulseFactorX * p1.getMass());
                                    p2.setyVel(p2.getyVel() + impulseFactorY * p1.getMass());
                                    p2.setzVel(p2.getzVel() + impulseFactorZ * p1.getMass());
                                }
                            }
                        }
                    }
                    for (Particle p : particles) {
                        p.updateVelocity();
                    }
                    
                    for (Particle p : particles) {
                        p.updatePos();
                    }
                    
                    for (int i = 0; i < particles.size(); i++) {
                        Particle p = particles.get(i);
                        Sphere particleSphere = particleSpheres.get(i);
                    
                        particleSphere.setTranslateX(p.getxCor());
                        particleSphere.setTranslateY(p.getyCor());
                        particleSphere.setTranslateZ(p.getzCor());
                    
                        if (p.getCharge() == 0) {
                            particleSphere.setMaterial(new PhongMaterial(Color.GRAY)); // Neutral particles
                        } else if (p.getCharge() > 0) {
                            particleSphere.setMaterial(new PhongMaterial(Color.RED)); // Positive charge
                        } else {
                            particleSphere.setMaterial(new PhongMaterial(Color.BLUE)); // Negative charge
                        }
                    }
                }
            }
                
        };
        gameLoop.start();
        // --- 6. Setup and Show the Stage ---
        primaryStage.setTitle("Simple Empty 3D Scene");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupKeyHandlers(Scene scene, Translate pivot) {
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
    
            // Get the current rotation angles
            double yaw = Math.toRadians(rotateY.getAngle()); // Rotation around Y-axis (horizontal)
            double pitch = Math.toRadians(rotateX.getAngle()); // Rotation around X-axis (vertical)
    
            // Movement vector components
            double forwardX = Math.sin(yaw);
            double forwardZ = Math.cos(yaw);
            double forwardY = Math.sin(pitch);
    
            double rightX = Math.cos(yaw);
            double rightZ = -Math.sin(yaw);
    
            switch (code) {
                case W: // Move forward
                    pivot.setX(pivot.getX() + forwardX * MOVE_SPEED);
                    pivot.setY(pivot.getY() - forwardY * MOVE_SPEED); // Y is inverted
                    pivot.setZ(pivot.getZ() + forwardZ * MOVE_SPEED);
                    break;
                case S: // Move backward
                    pivot.setX(pivot.getX() - forwardX * MOVE_SPEED);
                    pivot.setY(pivot.getY() + forwardY * MOVE_SPEED); // Y is inverted
                    pivot.setZ(pivot.getZ() - forwardZ * MOVE_SPEED);
                    break;
                case A: // Strafe left
                    pivot.setX(pivot.getX() - rightX * MOVE_SPEED);
                    pivot.setZ(pivot.getZ() - rightZ * MOVE_SPEED);
                    break;
                case D: // Strafe right
                    pivot.setX(pivot.getX() + rightX * MOVE_SPEED);
                    pivot.setZ(pivot.getZ() + rightZ * MOVE_SPEED);
                    break;
                case SPACE: // Move up
                    pivot.setY(pivot.getY() - MOVE_SPEED); // Y is inverted
                    break;
                case SHIFT: // Move down
                    pivot.setY(pivot.getY() + MOVE_SPEED);
                    break;
                default:
                    break;
            }
        });
    }

     private void setupMouseHandlers(Scene scene) {
        scene.setOnMousePressed((MouseEvent event) -> {
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        });

        scene.setOnMouseDragged((MouseEvent event) -> {
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

    private void setupScrollHandler(Scene scene, Translate pivot) {
        scene.setOnScroll(event -> {
            double zoomFactor = event.getDeltaY() > 0 ? MOVE_SPEED : -MOVE_SPEED; // Zoom in or out
            pivot.setZ(pivot.getZ() + zoomFactor);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
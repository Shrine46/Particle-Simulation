package com.shrine.particlesim;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;




public class MainFX extends Application {

    // JavaFx
    private static final double WINDOW_WIDTH = 800;
    private static final double WINDOW_HEIGHT = 600;

    private static final double CAMERA_NEAR_CLIP = 0.1;
    private static final double CAMERA_FAR_CLIP = 10000.0;
    private static final double CAMERA_INITIAL_Z = -1000; // Start back from the origin

    private static final double MOVE_SPEED = 20.0; // How fast the camera moves
    private static final double MOUSE_SENSITIVITY = 0.2; // How sensitive mouse rotation is

    private PerspectiveCamera camera;
    private Group root;
    private Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

    // Variables for mouse dragging
    private double lastMouseX = -1;
    private double lastMouseY = -1;

    @Override
    public void start(Stage primaryStage) {
        root = new Group();
        // Create a ground plane
        Box ground = new Box(2000, 10, 2000); // Width, height, depth
        ground.setTranslateY(5); // Position it slightly below the origin
        PhongMaterial groundMaterial = new PhongMaterial();
        groundMaterial.setDiffuseColor(Color.DARKGREEN); // Set ground color
        ground.setMaterial(groundMaterial);
        root.getChildren().add(ground);

        // Create a big box
        Box bigBox = new Box(200, 200, 200); // Width, height, depth
        bigBox.setTranslateY(-100); // Position it above the ground
        bigBox.setTranslateX(0); // Center it on the X-axis
        bigBox.setTranslateZ(0); // Center it on the Z-axis
        PhongMaterial boxMaterial = new PhongMaterial();
        boxMaterial.setDiffuseColor(Color.RED); // Set box color
        bigBox.setMaterial(boxMaterial);
        root.getChildren().add(bigBox);

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
        scene.setFill(Color.BLUE); // Background color
        scene.setCamera(camera);

        // --- 5. Setup Input Handlers ---
        setupKeyHandlers(scene, pivot);
        setupMouseHandlers(scene);
        setupScrollHandler(scene, pivot);

        AnimationTimer gameLoop = new AnimationTimer() {
        @Override
        public void handle(long now) {
            // Update logic here (e.g., move particles, handle collisions)
            
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
            // Note: These movements are relative to the *world* axes, not camera direction
            // For simple exploration, this is often okay.
            switch (code) {
                case W: // Move forward (along camera's local Z - approximated)
                    // A more accurate forward would involve sin/cos based on rotateY.getAngle()
                    // For simplicity, we move along global Z here.
                    pivot.setZ(pivot.getZ() + MOVE_SPEED);
                    break;
                case S: // Move backward
                     pivot.setZ(pivot.getZ() - MOVE_SPEED);
                    break;
                case A: // Strafe left
                     pivot.setX(pivot.getX() - MOVE_SPEED);
                    break;
                case D: // Strafe right
                     pivot.setX(pivot.getX() + MOVE_SPEED);
                    break;
                case SPACE: // Move up
                     pivot.setY(pivot.getY() - MOVE_SPEED); // Y is inverted in screen coords
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

            // Update Y rotation (Yaw) - Rotate around the world's Y axis
            double newAngleY = rotateY.getAngle() + deltaX * MOUSE_SENSITIVITY;
            rotateY.setAngle(newAngleY);

            // Update X rotation (Pitch) - Rotate around the camera's local X axis
            double newAngleX = rotateX.getAngle() - deltaY * MOUSE_SENSITIVITY; // Invert Y for natural feel

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

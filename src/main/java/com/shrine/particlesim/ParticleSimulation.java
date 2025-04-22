package com.shrine.particlesim; // Adjust package name if needed

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ParticleSimulation extends Application {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9565;
    private static final int CANVAS_WIDTH = 1000;
    private static final int CANVAS_HEIGHT = 800;

    private Canvas canvas;
    private GraphicsContext gc;
    private List<Particle> particles = new ArrayList<>(); // Use volatile or synchronized list if accessed by other threads
    private final AtomicBoolean running = new AtomicBoolean(true); // To signal shutdown
    private Thread clientThread;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);

        primaryStage.setTitle("Particle Simulation Viewer");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Close request received. Shutting down.");
            running.set(false); // Signal the client thread to stop
            if (clientThread != null) {
                clientThread.interrupt(); // Interrupt the thread if blocked
            }
        });
        primaryStage.show();

        // Start the client thread
        clientThread = new Thread(this::connectToServer);
        clientThread.setDaemon(true); // Allow JVM to exit if this is the last thread
        clientThread.start();
    }

    private void connectToServer() {
        while (running.get()) { // Keep trying to connect if running
            System.out.println("Attempting to connect to server " + HOST + ":" + PORT + "...");
            try (Socket socket = new Socket(HOST, PORT);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                System.out.println("Connected to the server!");

                String jsonData;
                // Keep reading lines as long as running flag is true and connection is open
                while (running.get() && (jsonData = in.readLine()) != null) {
                    try {
                        // Parse the JSON data received from the server
                        JSONArray particleArray = new JSONArray(jsonData);
                        List<Particle> newParticles = new ArrayList<>(); // Build new list

                        for (int i = 0; i < particleArray.length(); i++) {
                            JSONObject pData = particleArray.getJSONObject(i);
                            String type = pData.getString("type");
                            JSONArray position = pData.getJSONArray("position");
                            double radius = pData.getDouble("radius");

                            double x = position.getDouble(0);
                            double y = position.getDouble(1);
                            // Note: We only get 2D position and radius from this server

                            newParticles.add(new Particle(type, x, y, radius));
                        }

                        // Update the shared particle list and request redraw on FX thread
                        synchronized (particles) { // Synchronize access if needed elsewhere
                            particles = newParticles;
                        }
                        // Schedule drawing update on the JavaFX Application Thread
                        Platform.runLater(this::drawParticles);

                    } catch (JSONException e) {
                        System.err.println("Error parsing JSON data: " + e.getMessage());
                        System.err.println("Received data: " + jsonData);
                        // Continue reading next line
                    } catch (Exception e) {
                        System.err.println("Error processing received data: " + e.getMessage());
                        e.printStackTrace();
                        // Consider breaking if error is persistent
                    }
                } // End of readLine loop

                if (running.get()) {
                     System.out.println("Server disconnected (readLine returned null).");
                }

            } catch (ConnectException e) {
                System.err.println("Connection failed: Server not found or refused connection.");
                // Wait before retrying connection
                try {
                    Thread.sleep(3000); // Wait 3 seconds before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    System.out.println("Connection retry wait interrupted.");
                }
            } catch (SocketException e) {
                if (running.get()) { // Only log error if not intentionally shutting down
                     System.err.println("Socket Error: " + e.getMessage());
                     // Often happens during shutdown or if server closes connection abruptly
                }
            } catch (IOException e) {
                 if (running.get()) {
                     System.err.println("IOException during connection: " + e.getMessage());
                     e.printStackTrace();
                 }
            } catch (Exception e) {
                 if (running.get()) {
                     System.err.println("An unexpected error occurred in client thread: " + e.getMessage());
                     e.printStackTrace();
                 }
            }

            if (running.get()) {
                // Brief pause before attempting to reconnect again if connection lost/failed
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        } // End of main running loop
        System.out.println("Client connection thread finished.");
    }

    private void drawParticles() {
        // Ensure drawing happens only if gc is ready
        if (gc == null) return;

        // Clear canvas with a background color
        gc.setFill(Color.rgb(10, 10, 30)); // Dark blue background
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        List<Particle> particlesToDraw;
        // Get a stable copy of the particles list
        synchronized(particles){
            particlesToDraw = new ArrayList<>(particles);
        }


        for (Particle p : particlesToDraw) {
            // Set color based on particle type
            switch (p.type) {
                case "u": gc.setFill(Color.RED); break;
                case "d": gc.setFill(Color.BLUE); break;
                case "proton": gc.setFill(Color.ORANGE); break; // Distinct color
                case "neutron": gc.setFill(Color.LIGHTGRAY); break; // Distinct color
                case "e": gc.setFill(Color.CYAN); break; // Electron color
                default: gc.setFill(Color.LIMEGREEN); break; // Other particles
            }

            // Draw particle as a filled circle centered at (x, y) with diameter = radius * 2
            double diameter = Math.max(1.0, p.radius * 2.0); // Ensure minimum diameter of 1 pixel
            double drawX = p.x - diameter / 2.0;
            double drawY = p.y - diameter / 2.0;
            gc.fillOval(drawX, drawY, diameter, diameter);

            // Optional: Draw outline
            // gc.setStroke(Color.WHITE);
            // gc.setLineWidth(0.5);
            // gc.strokeOval(drawX, drawY, diameter, diameter);
        }
    }

    // Simple inner class to hold particle data for visualization
    private static class Particle {
        final String type;
        final double x;
        final double y;
        final double radius; // Visual radius in pixels

        Particle(String type, double x, double y, double radius) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }
}
import java.awt.*;
import javax.swing.*;
import java.util.ArrayList;
import java.awt.event.*;
import java.util.Random;

public class Main {
    public static ArrayList<Particle> particles = new ArrayList<>();
    private static JFrame frame;
    private static JPanel panel;
    private static Random rand;
    private static Timer timer;

    // Slider Values
    private static JSlider maxSpeedSlider;
    private static JSlider maxSpeedMultSlider;
    private static JSlider strongForceInnerRadiusSlider;
    private static JSlider FPSSlider;
    private static JSlider strongForceOuterRadiusSlider;
    private static JSlider kConstantSlider;
    private static JSlider kConstantMultSlider;
    private static JSlider strongForceConstantSlider;
    private static JSlider strongForceConstantMultSlider;
    private static JSlider gravityConstantSlider;
    private static JSlider gravityConstantMultSlider;

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
            return 1e7; // Default max speed
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
    
    public static int getFPS() {
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
    
    

    public static void display() {
        frame = new JFrame("Particle Playground");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1920, 1080);

        rand = new Random();



        // Drawing Particles -----------------------------------------------------------------------------------------------------------------



        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                for (Particle p : particles) {
                    if (p.getCharge() == 0) {
                        g2d.setColor(Color.GRAY);
                    } else if (p.getCharge() > 0) {
                        g2d.setColor(Color.RED); // Positive charge
                    } else {
                        g2d.setColor(Color.BLUE); // Negative charge
                    }
                    int diameter = (int) (p.radius * 2);
                    int topLeftX = (int) (p.getxCor() - p.radius);
                    int topLeftY = (int) (p.getyCor() - p.radius);

                    g2d.fillOval(topLeftX, topLeftY, diameter, diameter);
                }
            }
        };
        panel.setBackground(Color.LIGHT_GRAY);
        panel.setFocusable(true);



        // Sliders and Labels -----------------------------------------------------------------------------------------------------------------


        
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setOpaque(false);

        JPanel topRightPanel = new JPanel();
        topRightPanel.setLayout(new BoxLayout(topRightPanel, BoxLayout.Y_AXIS));
        topRightPanel.setOpaque(false);


        // Charge Slider
        JLabel chargeLabel = new JLabel("Charge = 0");
        JSlider chargeSlider = new JSlider(-100, 100, 0);
        chargeSlider.setOrientation(SwingConstants.HORIZONTAL);
        chargeSlider.setPreferredSize(new Dimension(300, 50));
        panel.setLayout(new BorderLayout());

        chargeSlider.addChangeListener(e -> {
            String str = ("Charge = " +chargeSlider.getValue());
            chargeLabel.setText(str);
        });

        topRightPanel.add(chargeLabel);
        topRightPanel.add(chargeSlider);

        // Mass Slider
        JLabel massLabel = new JLabel("1");
        JSlider massSlider = new JSlider(1, 2000, 1);

        massSlider.setOrientation(SwingConstants.HORIZONTAL);
        massSlider.setPreferredSize(new Dimension(300, 50));
        massSlider.addChangeListener(e -> {
            String str = "Mass = " + massSlider.getValue() * 1;
            massLabel.setText(str);
        });

        topRightPanel.add(massLabel);
        topRightPanel.add(massSlider);

        // Decay Slider
        JLabel decayLabel = new JLabel("Decay Speed = 0");
        JSlider decaySlider = new JSlider(0, 100, 0);
        decaySlider.setOrientation(SwingConstants.HORIZONTAL);
        decaySlider.setPreferredSize(new Dimension(300, 50));
        decaySlider.addChangeListener(e -> {
            String str = "Decay Speed = " + decaySlider.getValue();
            decayLabel.setText(str);
        });

        topRightPanel.add(decayLabel);
        topRightPanel.add(decaySlider);

        // FPS Slider
        JLabel FPSLabel = new JLabel("FPS = " + 60);
        FPSSlider = new JSlider(1, 540, 60);
        FPSSlider.setOrientation(SwingConstants.HORIZONTAL);
        FPSSlider.setPreferredSize(new Dimension(300, 50));
        FPSSlider.addChangeListener(e -> {
            FPSLabel.setText("FPS = " + FPSSlider.getValue());

            timer.stop();

            timer = new Timer(1000 / FPSSlider.getValue(), timer.getActionListeners()[0]);
            timer.start();
        });

        topRightPanel.add(FPSLabel);
        topRightPanel.add(FPSSlider);
        
        // Strong Force Outer Radius Slider
        JLabel strongForceOuterRadiusLabel = new JLabel("Strong Force Outer Radius = 27");
        strongForceOuterRadiusSlider = new JSlider(10, 50, 27);
        strongForceOuterRadiusSlider.setOrientation(SwingConstants.HORIZONTAL);
        strongForceOuterRadiusSlider.setPreferredSize(new Dimension(300, 50));
        strongForceOuterRadiusSlider.addChangeListener(e -> {
            strongForceOuterRadiusLabel.setText("Strong Force Outer Radius = " + strongForceOuterRadiusSlider.getValue());
        });

        topRightPanel.add(strongForceOuterRadiusLabel);
        topRightPanel.add(strongForceOuterRadiusSlider);

        // Strong Force Inner Radius Slider
        JLabel strongForceInnerRadiusLabel = new JLabel("Strong Force Inner Radius = 9");
        strongForceInnerRadiusSlider = new JSlider(1, 20, 9);
        strongForceInnerRadiusSlider.setOrientation(SwingConstants.HORIZONTAL);
        strongForceInnerRadiusSlider.setPreferredSize(new Dimension(300, 50));
        strongForceInnerRadiusSlider.addChangeListener(e -> {
            strongForceInnerRadiusLabel.setText("Strong Force Inner Radius = " + strongForceInnerRadiusSlider.getValue());
        });

        topRightPanel.add(strongForceInnerRadiusLabel);
        topRightPanel.add(strongForceInnerRadiusSlider);

        // Max Speed Slider & Multiplyer
        JLabel maxSpeedLabel = new JLabel("Max Speed = 1e7");
        maxSpeedSlider = new JSlider(1, 9, 1);
        JLabel maxSpeedMultLabel = new JLabel("Max Speed Multiplyer = e7");
        maxSpeedMultSlider = new JSlider(1, 50, 7);

        maxSpeedSlider.setOrientation(SwingConstants.HORIZONTAL);
        maxSpeedSlider.setPreferredSize(new Dimension(300, 50));
        maxSpeedMultSlider.setOrientation(SwingConstants.HORIZONTAL);
        maxSpeedMultSlider.setPreferredSize(new Dimension(300, 50));
    
        maxSpeedMultSlider.addChangeListener(e -> {
            maxSpeedMultLabel.setText("Max Speed = e" + maxSpeedMultSlider.getValue());
            maxSpeedLabel.setText("Max Speed = " + maxSpeedSlider.getValue() + "e" + maxSpeedMultSlider.getValue());
        });
        maxSpeedSlider.addChangeListener(e -> {
            maxSpeedLabel.setText("Max Speed = " + maxSpeedSlider.getValue() + "e" + maxSpeedMultSlider.getValue());
        });

        topRightPanel.add(maxSpeedLabel);
        topRightPanel.add(maxSpeedSlider);
        topRightPanel.add(maxSpeedMultLabel);
        topRightPanel.add(maxSpeedMultSlider);

        // Coulomb Constant Slider & Multiplier
        JLabel kConstantLabel = new JLabel("Coulomb Constant (k) = 4e5");
        kConstantSlider = new JSlider(1, 9, 4);
        JLabel kConstantMultLabel = new JLabel("Coulomb Constant Multiplier = e5");
        kConstantMultSlider = new JSlider(1, 50, 5);

        kConstantSlider.setOrientation(SwingConstants.HORIZONTAL);
        kConstantSlider.setPreferredSize(new Dimension(300, 50));
        kConstantMultSlider.setOrientation(SwingConstants.HORIZONTAL);
        kConstantMultSlider.setPreferredSize(new Dimension(300, 50));

        kConstantMultSlider.addChangeListener(e -> {
            kConstantMultLabel.setText("Coulomb Constant Multiplier = e" + kConstantMultSlider.getValue());
            kConstantLabel.setText("Coulomb Constant (k) = " + kConstantSlider.getValue() + "e" + kConstantMultSlider.getValue());
        });
        kConstantSlider.addChangeListener(e -> {
            kConstantLabel.setText("Coulomb Constant (k) = " + kConstantSlider.getValue() + "e" + kConstantMultSlider.getValue());
        });

        topRightPanel.add(kConstantLabel);
        topRightPanel.add(kConstantSlider);
        topRightPanel.add(kConstantMultLabel);
        topRightPanel.add(kConstantMultSlider);

        // Strong Force Constant Slider & Multiplier
        JLabel strongForceConstantLabel = new JLabel("Strong Force Constant = 6e5");
        strongForceConstantSlider = new JSlider(1, 9, 6);
        JLabel strongForceConstantMultLabel = new JLabel("Strong Force Constant Multiplier = e5");
        strongForceConstantMultSlider = new JSlider(1, 50, 5);

        strongForceConstantSlider.setOrientation(SwingConstants.HORIZONTAL);
        strongForceConstantSlider.setPreferredSize(new Dimension(300, 50));
        strongForceConstantMultSlider.setOrientation(SwingConstants.HORIZONTAL);
        strongForceConstantMultSlider.setPreferredSize(new Dimension(300, 50));

        strongForceConstantMultSlider.addChangeListener(e -> {
            strongForceConstantMultLabel.setText("Strong Force Constant Multiplier = e" + strongForceConstantMultSlider.getValue());
            strongForceConstantLabel.setText("Strong Force Constant = " + strongForceConstantSlider.getValue() + "e" + strongForceConstantMultSlider.getValue());
        });
        strongForceConstantSlider.addChangeListener(e -> {
            strongForceConstantLabel.setText("Strong Force Constant = " + strongForceConstantSlider.getValue() + "e" + strongForceConstantMultSlider.getValue());
        });

        topRightPanel.add(strongForceConstantLabel);
        topRightPanel.add(strongForceConstantSlider);
        topRightPanel.add(strongForceConstantMultLabel);
        topRightPanel.add(strongForceConstantMultSlider);

        // Gravity Constant Slider & Multiplier
        JLabel gravityConstantLabel = new JLabel("Gravity Constant = 1e1");
        gravityConstantSlider = new JSlider(1, 9, 1);
        JLabel gravityConstantMultLabel = new JLabel("Gravity Constant Multiplier = e1");
        gravityConstantMultSlider = new JSlider(1, 50, 2);

        gravityConstantSlider.setOrientation(SwingConstants.HORIZONTAL);
        gravityConstantSlider.setPreferredSize(new Dimension(300, 50));
        gravityConstantMultSlider.setOrientation(SwingConstants.HORIZONTAL);
        gravityConstantMultSlider.setPreferredSize(new Dimension(300, 50));

        gravityConstantMultSlider.addChangeListener(e -> {
            gravityConstantMultLabel.setText("Gravity Constant Multiplier = e" + gravityConstantMultSlider.getValue());
            gravityConstantLabel.setText("Gravity Constant = " + gravityConstantSlider.getValue() + "e" + gravityConstantMultSlider.getValue());
        });
        gravityConstantSlider.addChangeListener(e -> {
            gravityConstantLabel.setText("Gravity Constant = " + gravityConstantSlider.getValue() + "e" + gravityConstantMultSlider.getValue());
        });

        topRightPanel.add(gravityConstantLabel);
        topRightPanel.add(gravityConstantSlider);
        topRightPanel.add(gravityConstantMultLabel);
        topRightPanel.add(gravityConstantMultSlider);


        topPanel.add(topRightPanel);
        panel.add(topPanel, BorderLayout.NORTH);



        // Key bindings -----------------------------------------------------------------------------------------------------------------
        
        

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "spawnElectron");
        panel.getActionMap().put("spawnElectron", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(p, panel);
                double offsetX = rand.nextDouble(-5, 5);
                double offsetY = rand.nextDouble(-5, 5);
                Particle electron = new Particle(p.x + offsetX, p.y + offsetY, rand.nextDouble(-4, 4), rand.nextDouble(-4, 4), electronCharge, electronMass, "electron"); 
                particles.add(electron);
            }
        });

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "spawnProton");
        panel.getActionMap().put("spawnProton", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(p, panel);
                double offsetX = rand.nextDouble(-5, 5);
                double offsetY = rand.nextDouble(-5, 5);
                Particle proton = new Particle(p.x + offsetX, p.y + offsetY, rand.nextDouble(-4, 4), rand.nextDouble(-4, 4), protonCharge, protonMass, "proton");
                particles.add(proton);
            }
        });

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0), "spawnNeutron");
        panel.getActionMap().put("spawnNeutron", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(p, panel);
                double offsetX = rand.nextDouble(-5, 5);
                double offsetY = rand.nextDouble(-5, 5);
                Particle neutron = new Particle(p.x + offsetX, p.y + offsetY, rand.nextDouble(-4, 4), rand.nextDouble(-4, 4), neutronCharge, neutronMass, "neutron");
                particles.add(neutron);
            }
        });

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                panel.requestFocusInWindow();
                Point p = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(p, panel);
                double offsetX = rand.nextDouble(-5, 5);
                double offsetY = rand.nextDouble(-5, 5);
                Particle chargedParticle = new Particle(p.x + offsetX, p.y + offsetY, rand.nextDouble(-4, 4), rand.nextDouble(-4, 4), chargeSlider.getValue(), massSlider.getValue(), "chargeParticle"); // Original = -1.6-19 AND 1.67262158e-27
                particles.add(chargedParticle);
            }
        });

        frame.add(panel);
        frame.setVisible(true);
        SwingUtilities.invokeLater(() -> panel.requestFocusInWindow());



        // Main Loop ---------------------------------------------------------------------------------------------------



        timer = new Timer(1000/FPSSlider.getValue(), e -> {
            // Reset
            for (Particle p : particles) {
                p.resetForce();
            }

            for (int i = 0; i < particles.size(); i++) {
                for (int j = i + 1; j < particles.size(); j++) {
                    Particle p1 = particles.get(i);
                    Particle p2 = particles.get(j);

                    double[] forceOnP1 = p1.calculateForces(p2);
                    p1.addForce(forceOnP1[0], forceOnP1[1]);
                    p2.addForce(-forceOnP1[0], -forceOnP1[1]);

                    double distX = p2.xCor - p1.xCor;
                    double distY = p2.yCor - p1.yCor;
                    double distSq = distX * distX + distY * distY;

                    double collisionRadiusSum = p1.radius + p2.radius;
                    double collisionRadiusSumSq = collisionRadiusSum * collisionRadiusSum;

                    if (distSq < collisionRadiusSumSq && distSq > 1e-9) { 
                        double dist = Math.sqrt(distSq);

                        double overlap = collisionRadiusSum - dist;
                        double dirX = distX / dist;
                        double dirY = distY / dist;

                        double totalMass = p1.mass + p2.mass;
                        double pushFactor1 = (p2.mass / totalMass) * overlap;
                        double pushFactor2 = (p1.mass / totalMass) * overlap;

                        p1.xCor -= dirX * pushFactor1;
                        p1.yCor -= dirY * pushFactor1;
                        p2.xCor += dirX * pushFactor2;
                        p2.yCor += dirY * pushFactor2;


                        double relativeVelX = p2.xVel - p1.xVel;
                        double relativeVelY = p2.yVel - p1.yVel;

                        double dotProduct = relativeVelX * dirX + relativeVelY * dirY;

                        if (dotProduct < 0) {
                            double restitution = 0.7;

                            double collisionScale = (1.0 + restitution) * dotProduct / totalMass;
                            double impulseFactorX = collisionScale * dirX;
                            double impulseFactorY = collisionScale * dirY;

                            p1.xVel += impulseFactorX * p2.mass; 
                            p1.yVel += impulseFactorY * p2.mass;
                            p2.xVel -= impulseFactorX * p1.mass;
                            p2.yVel -= impulseFactorY * p1.mass;
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

            frame.repaint();
        });
        timer.start();
    }

    public static void main(String[] args) {
        display();
    }
}
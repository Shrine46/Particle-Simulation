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

    // Constants

    // Electron
    protected static final double electronCharge = -1; // Original = -1.6-19 
    protected static final double electronMass = 10; // Original = 9.109e-31
    // Proton
    protected static final double protonCharge = 1; // Original = 1.6e-19
    protected static final double protonMass = 100; // Original = 1.67262158e-29

    // Neutron
    protected static final double neutronCharge = 0;
    protected static final double neutronMass = 100; // Original = 1.67492749804e-29
    
    

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

        
        JLabel chargeLabel = new JLabel("Charge = 0");
        JSlider chargeSlider = new JSlider(-100, 100, 0);
        chargeSlider.setMajorTickSpacing(10);
        chargeSlider.setMinorTickSpacing(1);
        chargeSlider.setOrientation(SwingConstants.HORIZONTAL);
        chargeSlider.setPreferredSize(new Dimension(300, 50));
        panel.setLayout(new BorderLayout());

        JLabel massLabel = new JLabel("1");
        JSlider massSlider = new JSlider(1, 2000, 1);
        massSlider.setMajorTickSpacing(100);
        massSlider.setMinorTickSpacing(10);
        massSlider.setOrientation(SwingConstants.HORIZONTAL);
        massSlider.setPreferredSize(new Dimension(300, 50));

        chargeSlider.addChangeListener(e -> {
            String str = ("Charge = " +chargeSlider.getValue());
            chargeLabel.setText(str);
        });

        massSlider.addChangeListener(e -> {
            String str = "Mass = " + massSlider.getValue() * 1;
            massLabel.setText(str);
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setOpaque(false);

        JPanel topRightPanel = new JPanel();
        topRightPanel.setLayout(new BoxLayout(topRightPanel, BoxLayout.Y_AXIS));
        topRightPanel.setOpaque(false);

        topRightPanel.add(chargeLabel);
        topRightPanel.add(chargeSlider);

        topRightPanel.add(massLabel);
        topRightPanel.add(massSlider);

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



        new Timer(16, e -> {
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
        }).start();
    }

    public static void main(String[] args) {
        display();
    }
}
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
    protected static final double electronMass = 5.4459; // Original = 9.109e-31
    // Proton
    protected static final double protonCharge = 1; // Original = 1.6e-19
    protected static final double protonMass = 100; // Original = 1.67262158e-29

    // Neutron
    protected static final double neutronCharge = 0;
    protected static final double neutronMass = 120; // Original = 1.67492749804e-29
    
    

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
                for (Particle p : particles) {
                    if (p.getCharge() == 0) {
                        g.setColor(Color.GRAY);
                    }
                    else if (p.getCharge() > 0) {
                        g.setColor(Color.RED); // Positive charge
                    } 
                    else {
                        g.setColor(Color.BLUE); // Negative charge
                    }
                    g.fillOval((int) p.getxCor(), (int) p.getyCor(), 10, 10);
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

                    // Collisions
                    double radius_minimum = 10.0;
                    double distSq = (p2.xCor - p1.xCor)*(p2.xCor - p1.xCor) + (p2.yCor - p1.yCor)*(p2.yCor - p1.yCor);
                    if (distSq > 1e-12 && distSq < radius_minimum * radius_minimum) {
                        double dist = Math.sqrt(distSq);
                        double overlap = radius_minimum - dist;
                        double dirX = (p2.xCor - p1.xCor) / dist;
                        double dirY = (p2.yCor - p1.yCor) / dist;

                        double pushFactor = overlap / 2.0;
                        p1.xCor -= dirX * pushFactor;
                        p1.yCor -= dirY * pushFactor;
                        p2.xCor += dirX * pushFactor;
                        p2.yCor += dirY * pushFactor;
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
        System.out.println(9.109e-31/1.67262158e-29);
    }
}
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

    public static void display() {
        frame = new JFrame("Particle Playground");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1920, 1080);

        rand = new Random();

        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                for (Particle p : particles) {
                    if (p.getCharge() > 0) {
                        g.setColor(Color.RED); // Positive charge
                    } else {
                        g.setColor(Color.BLUE); // Negative charge
                    }
                    g.fillOval((int) p.getxCor(), (int) p.getyCor(), 10, 10);
                }
            }
        };
        panel.setBackground(Color.LIGHT_GRAY);
        panel.setFocusable(true);

        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));

        
        JLabel chargeLabel = new JLabel("Charge = 0");
        JSlider chargeSlider = new JSlider(-100, 100, 0);
        chargeSlider.setMajorTickSpacing(10);
        chargeSlider.setMinorTickSpacing(1);
        chargeSlider.setOrientation(SwingConstants.HORIZONTAL);
        chargeSlider.setPreferredSize(new Dimension(300, 50));
        panel.setLayout(new BorderLayout());

        JLabel massLabel = new JLabel("Mass = 1e-29");
        JSlider massSlider = new JSlider(1, 2000, 1);
        massSlider.setMajorTickSpacing(100);
        massSlider.setMinorTickSpacing(10);
        massSlider.setOrientation(SwingConstants.HORIZONTAL);
        massSlider.setPreferredSize(new Dimension(300, 50));

        chargeSlider.addChangeListener(e -> {
            String str = ("Charge = " +chargeSlider.getValue() * 1e-15);
            // Keep label size small
            String strTemp = str.substring(str.length()-4, str.length());
            str = str.substring(0, 13);
            str += strTemp;
            chargeLabel.setText(str);
        });

        massSlider.addChangeListener(e -> {
            String str = "Mass = " + massSlider.getValue() * 1e-29;
            String strTemp = str.substring(str.length()-4);
            str = str.substring(0, 11);
            str += strTemp;
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

        // Key bindings for spawning particles
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "spawnElectron");
        panel.getActionMap().put("spawnElectron", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(p, panel);
                // Add slight random offset to avoid exact overlap
                double offsetX = rand.nextDouble(-5, 5);
                double offsetY = rand.nextDouble(-5, 5);
                Particle electron = new Particle(p.x + offsetX, p.y + offsetY, rand.nextDouble(-4, 4), rand.nextDouble(-4, 4), -1.6e-15, 9.109e-29); // Original = -1.6-19 AND 9.109e-31
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
                Particle proton = new Particle(p.x + offsetX, p.y + offsetY, rand.nextDouble(-4, 4), rand.nextDouble(-4, 4), 1.6e-15, 1.67262158e-27); // Original = -1.6-19 AND 1.67262158e-29
                particles.add(proton);
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
                Particle chargedParticle = new Particle(p.x + offsetX, p.y + offsetY, rand.nextDouble(-4, 4), rand.nextDouble(-4, 4), chargeSlider.getValue() * 1e-15, massSlider.getValue() * 1e-29); // Original = -1.6-19 AND 1.67262158e-29
                particles.add(chargedParticle);
            }
        });

        frame.add(panel);
        frame.setVisible(true);

        SwingUtilities.invokeLater(() -> panel.requestFocusInWindow());

        // Timer for physics update and repaint
        new Timer(16, e -> {
            // Update physics
            for (int i = 0; i < particles.size(); i++) {
                for (int j = i + 1; j < particles.size(); j++) {
                    particles.get(i).culombLaw(particles.get(j));
                }
            }
            for (Particle p : particles) {
                p.updatePos();
            }
            // Repaint
            frame.repaint();
        }).start();
    }

    public static void main(String[] args) {
        display();
    }
}
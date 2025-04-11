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

        rand = new Random(); // Initialize the Random object

        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                for (Particle part : particles) {
                    g.drawRoundRect((int) part.getxCor(), (int) part.getyCor(), 3, 3, 3, 3);
                }
            }
        };
        panel.setBackground(Color.BLACK);
        panel.setFocusable(true); // Ensure the panel is focusable

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "spawnElectron");
        panel.getActionMap().put("spawnElectron", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(p, panel);
                Particle electron = new Particle(p.x, p.y, rand.nextDouble(-4, 4), rand.nextDouble(-4, 4), 1.6e-19);
                particles.add(electron);
            }
        });

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "spawnProton");
        panel.getActionMap().put("spawnProton", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(p, panel);
                Particle proton = new Particle(p.x, p.y, rand.nextDouble(-4, 4), rand.nextDouble(-4, 4), 1.6e-19);
                particles.add(proton);
                System.out.println("proton");
            }
        });

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                panel.requestFocusInWindow();
            }
        });

        frame.add(panel);
        frame.setVisible(true);

        // Use SwingUtilities.invokeLater to ensure focus is requested after the frame is visible
        SwingUtilities.invokeLater(() -> {
            panel.requestFocusInWindow();
            System.out.println("Panel focusable: " + panel.isFocusable());
            System.out.println("Panel has focus: " + panel.hasFocus());
        });
        panel.requestFocusInWindow();
    }

    public static void main(String[] args) {
        display();
        new Timer(16, e -> frame.repaint()).start(); // ~60 FPS
    }
}
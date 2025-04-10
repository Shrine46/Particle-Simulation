import java.awt.*;
import javax.swing.*;
import java.util.ArrayList;
import javax.swing.text.GapContent;

public class Main {
    public static ArrayList<Particle> particles = new ArrayList<>();
    private static JFrame frame;
    private static JPanel panel;

    public static void display() {
        frame = new JFrame("Particle Playground");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1920, 1080);

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

        frame.add(panel);
        frame.setVisible(true);
    }



    public static void main(String[] args) {
        display();
    }
}
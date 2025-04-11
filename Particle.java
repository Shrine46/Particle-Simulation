public class Particle {
    protected double xCor; // Use double for smoother motion
    protected double yCor;
    protected double xVel;
    protected double yVel;
    protected double mass;
    protected double charge;
    private static final double DT = 0.016; // Time step (~60 FPS)

    public Particle(double xCor, double yCor, double xVel, double yVel, double charge, double mass) {
        this.xCor = xCor;
        this.yCor = yCor;
        this.xVel = xVel;
        this.yVel = yVel;
        this.charge = charge;
        this.mass = mass;
    }

    public void updatePos() {
        xVel *= .99; // Drag
        yVel *= .99;
        xCor += xVel * DT;
        yCor += yVel * DT;

        if (xCor < 0 || xCor > 1920) xCor = xCor%1920;
        if (yCor < 0 || yCor > 1080) yCor = yCor%1080;
    }

    public void culombLaw(Particle p2) {
        Particle p1 = this;
        double charge1 = p1.getCharge();
        double charge2 = p2.getCharge();
        double dist = Math.sqrt(Math.pow(p2.getxCor() - p1.getxCor(), 2) + Math.pow(p2.getyCor() - p1.getyCor(), 2));

        // Prevent division by zero and cap minimum distance
        if (dist < 1e-9) {
            dist = 1e-9;
            System.out.println("Distance was near zero");
        }

        double k = 8.99e7; // Original = 8.99e9
        double force = (k * charge1 * charge2) / Math.pow(dist, 2); // Note: Includes sign for attraction/repulsion
        double angle = Math.atan2(p2.getyCor() - p1.getyCor(), p2.getxCor() - p1.getxCor());

        // Force components
        double xForce = force * Math.cos(angle);
        double yForce = force * Math.sin(angle);

        // Apply acceleration to both particles (Newton's third law)
        double xAccel1 = -xForce / p1.getMass();
        double yAccel1 = -yForce / p1.getMass();
        double xAccel2 = xForce / p2.getMass(); // Opposite direction for p2
        double yAccel2 = yForce / p2.getMass();

        // Update velocities with time step
        p1.setxVel(p1.getxVel() + xAccel1 * DT);
        p1.setyVel(p1.getyVel() + yAccel1 * DT);
        p2.setxVel(p2.getxVel() + xAccel2 * DT);
        p2.setyVel(p2.getyVel() + yAccel2 * DT);
    }

    // Getters and setters
    public double getxCor() {
        return xCor;
    }

    public void setxCor(double xCor) {
        this.xCor = xCor;
    }

    public double getyCor() {
        return yCor;
    }

    public void setyCor(double yCor) {
        this.yCor = yCor;
    }

    public double getxVel() {
        return xVel;
    }

    public void setxVel(double xVel) {
        this.xVel = xVel;
    }

    public double getyVel() {
        return yVel;
    }

    public void setyVel(double yVel) {
        this.yVel = yVel;
    }

    public double getMass() {
        return mass;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    public double getCharge() {
        return charge;
    }

    public void setCharge(double charge) {
        this.charge = charge;
    }
}
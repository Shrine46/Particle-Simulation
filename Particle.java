

public class Particle {
    protected int xCor; // x coordinate of the particle
    protected int yCor; // y coordinate of the particle
    protected double xVel; // x velocity of the particle
    protected double yVel; // y velocity of the particle
    protected double mass;
    protected double angle;
    protected double charge;

    public Particle(int xCor, int yCor, double xVel, double yVel, double charge) {
        this.xCor = xCor;
        this.yCor = yCor;
        this.xVel = xVel;
        this.yVel = yVel;
        this.charge = charge;
        this.angle = Math.atan2(yVel, xVel);
    }

    public void culombLaw(Particle p2) { // Culomb's law = (k * q1 * q2) / r^2
        Particle p1 = this;
        double charge1 = p1.getCharge();
        double charge2 = p2.getCharge();
        double dist = Math.sqrt(Math.pow(p2.getxCor() - p1.getxCor(), 2) + Math.pow(p2.getyCor() - p1.getyCor(), 2));
    
        if (dist == 0) {
            dist = 1e-9;
            System.out.println("Distance was 0");
        }
    
        double k = 8.99e9;
        double force = (k * Math.abs(charge1) * Math.abs(charge2)) / Math.pow(dist, 2);
        double angle = Math.atan2(p2.getyCor() - p1.getyCor(), p2.getxCor() - p1.getxCor());
        double xForce = force * Math.cos(angle);
        double yForce = force * Math.sin(angle);
    
        if (charge1 * charge2 < 0) { // Opposite charges
            p1.setAngle(angle);
        } 
        else {
            p2.setAngle(angle * -1);
        }
    
        double xAccel = xForce / p1.getMass();
        double yAccel = yForce / p1.getMass();
        p1.setxVel(p1.getxVel() + xAccel);
        p1.setyVel(p1.getyVel() + yAccel);
    }

    public double getxCor() {
        return xCor;
    }

    public void setxCor(int xCor) {
        this.xCor = xCor;
    }

    public double getyCor() {
        return yCor;
    }

    public void setyCor(int yCor) {
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

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public double getCharge() {
        return charge;
    }

    public void setCharge(double charge) {
        this.charge = charge;
    }

    
}

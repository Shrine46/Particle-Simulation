public class Particle {
    protected double xCor; // Use double for smoother motion
    protected double yCor;
    protected double xVel;
    protected double yVel;
    protected double mass;
    protected double charge;
    protected String particleType;

    // Constants
    public static final double DT = 0.016; // Time step (~60 FPS)
    protected static final double MAX_SPEED = 1000;
    protected double kConstant = 8.99e7; // Original = 8.99e9
    protected double gConstant = 0.3; // Coupling Strength
    protected double muConstant = 0.1; // Constant
    protected double StrongForceAdjustment = 1e-21;





    public Particle(double xCor, double yCor, double xVel, double yVel, double charge, double mass, String particleType) {
        this.xCor = xCor;
        this.yCor = yCor;
        this.xVel = xVel;
        this.yVel = yVel;
        this.charge = charge;
        this.mass = mass;
        this.particleType = particleType;
    }

    public void updatePos() {
        double speed = Math.sqrt(xVel*xVel + yVel*yVel);
        if (speed > MAX_SPEED) {
            xVel *= MAX_SPEED / speed;
            yVel *= MAX_SPEED / speed;
        }
        xVel *= .99; // Drag
        yVel *= .99;
        xCor += xVel * DT;
        yCor += yVel * DT;

        if (xCor < 0 || xCor > 1920) xCor = (xCor + 1920) % 1920;
        if (yCor < 0 || yCor > 1080) yCor = (yCor + 1080) % 1080;
    }

    public void culombLaw(Particle p2) {
        Particle p1 = this;
        double charge1 = p1.getCharge();
        double charge2 = p2.getCharge();
        double dist = Math.sqrt(Math.pow(p2.getxCor() - p1.getxCor(), 2) + Math.pow(p2.getyCor() - p1.getyCor(), 2));

        if (dist < 1e-9) {
            dist = 1e-4;
            System.out.println("Distance was near zero");
        }
        double force = (kConstant * charge1 * charge2) / (Math.pow(dist, 2) + Math.pow(5, 2));
        double angle = Math.atan2(p2.getyCor() - p1.getyCor(), p2.getxCor() - p1.getxCor());

        force *= .5; // Adjust Force

        double xForce = force * Math.cos(angle);
        double yForce = force * Math.sin(angle);

        double xAccel1 = -xForce / p1.getMass();
        double yAccel1 = -yForce / p1.getMass();
        double xAccel2 = xForce / p2.getMass();
        double yAccel2 = yForce / p2.getMass();

        p1.setxVel(p1.getxVel() + xAccel1 * DT);
        p1.setyVel(p1.getyVel() + yAccel1 * DT);
        p2.setxVel(p2.getxVel() + xAccel2 * DT);
        p2.setyVel(p2.getyVel() + yAccel2 * DT);
    }

    public void strongNuclearForce (Particle p2) {
        Particle p1 = this;
        double dist = Math.sqrt(Math.pow(p2.getxCor() - p1.getxCor(), 2) + Math.pow(p2.getyCor() - p1.getyCor(), 2));
        if (dist > 30) {
            return;
        } if (dist < 1) {
             dist = 0.1;
        }

        double angle = Math.atan2(p2.getyCor() - p1.getyCor(), p2.getxCor() - p1.getxCor());


        double force = gConstant * gConstant * (Math.exp(-muConstant * dist) / (dist * dist) + muConstant * Math.exp(-muConstant * dist) / dist);
        double repellingForce = 900 / Math.pow(dist, 6);

        force -= repellingForce;

        force *= StrongForceAdjustment; // Scale it down or it goes superfast



        double xForce = force * Math.cos(angle);
        double yForce = force * Math.sin(angle);

        double xAccel1 = xForce / p1.getMass();
        double yAccel1 = yForce / p1.getMass();
        double xAccel2 = -xForce / p2.getMass();
        double yAccel2 = -yForce / p2.getMass();

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

    public String getParticleType() {
        return particleType;
    }

    public void setParticleType(String particleType) {
        this.particleType = particleType;
    }
}
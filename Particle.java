public class Particle {
    protected double xCor; // Use double for smoother motion
    protected double yCor;
    protected double xVel;
    protected double yVel;
    protected double mass;
    protected double charge;
    protected String particleType;
    protected double radius;

    // Store forces to update
    protected double netX;
    protected double netY;

    // Constants
    public static final double DT = 0.016 * 10; // Time step (~60 FPS)
    protected static final double MAX_SPEED = 1000;

    // Coulomb Law
    protected double kConstant = 400000; // Original = 8.99e9

    // Strong Force
    protected double strongForceOuterRadius = 27;
    protected double strongForceInnerRadius = 9;
    protected double strongForceConstant = 600000;

    // Gravity
    protected double gravityConstant = 10;

    public Particle(double xCor, double yCor, double xVel, double yVel, double charge, double mass, String particleType) {
        this.xCor = xCor;
        this.yCor = yCor;
        this.xVel = xVel;
        this.yVel = yVel;
        this.charge = charge;
        this.mass = mass;
        this.particleType = particleType;

        if (this.isNucleon()) {this.radius = 9.0;}
        else if (this.isElectron()) {this.radius = 3.0;}
        else {this.radius = 3 + ((mass + charge) * .05);}
    }

    public void updatePos() {
        double speed = Math.sqrt(xVel * xVel + yVel * yVel);
        if (speed > MAX_SPEED) {
            xVel *= MAX_SPEED / speed;
            yVel *= MAX_SPEED / speed;
        }
        xVel *= 0.90; // Drag
        yVel *= 0.90;
        xCor += xVel * DT;
        yCor += yVel * DT;

        // Bounce off walls
        if (xCor < radius) {
            xCor = radius;
            xVel = -xVel;
        } else if (xCor > 1920 - radius) {
            xCor = 1920 - radius; 
            xVel = -xVel; 
        }

        if (yCor < radius) {
            yCor = radius; 
            yVel = -yVel; 
        } else if (yCor > 1080 - radius) {
            yCor = 1080 - radius; 
            yVel = -yVel; 
        }
    }

    public void updateVelocity() {
        if (this.mass == 0) return; 
        double accelX = this.netX / this.mass;
        double accelY = this.netY / this.mass;
        this.xVel += accelX * DT;
        this.yVel += accelY * DT;
    }

    public double[] calculateForces(Particle p2) {
        Particle p1 = this;
        double forceX = 0;
        double forceY = 0;

        // Distances
        double distX = p2.getxCor() - p1.getxCor();
        double distY = p2.getyCor() - p1.getyCor();
        double dist = Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2));

        if (dist < 1e-8) {
            return new double[] {0,0};
        }

        double dirX = distX / dist;
        double dirY = distY / dist;

        // Charges
        double charge1 = p1.getCharge();
        double charge2 = p2.getCharge();

        // Culomb Force
        double coulombForce = (kConstant * charge1 * charge2) / (Math.pow(dist, 2));
        forceX -= coulombForce * dirX;
        forceY -= coulombForce * dirY;

        // Swirl effect

        if (p1.isElectron() && charge2 > 0) {
            double combinedRadius = p1.radius + p2.radius;
            if (dist < combinedRadius + 20) {
                double tangentialForce = coulombForce * .7; 
                double tangentX = -dirY;
                double tangentY = dirX;

                if ((p1.getxVel() * tangentX + p1.getyVel() * tangentY) < 0) {
                    tangentX = -tangentX;
                    tangentY = -tangentY;
                }

                forceX += tangentialForce * tangentX;
                forceY += tangentialForce * tangentY;
                forceX = -forceX;
                forceY = -forceY;
            }
        }
        
        

        // Strong Force
        if ((p1.isNucleon() && p2.isNucleon()) && dist <= strongForceOuterRadius) {
            double strongForce = strongForceConstant / Math.pow(dist, 3);
            double strongForceX;
            double strongForceY;

            if (dist < strongForceInnerRadius) {
                // Repulsive
                strongForceX = -strongForce * dirX;
                strongForceY = -strongForce * dirY;
            } else {
                // Attractive
                strongForceX = strongForce * dirX;
                strongForceY = strongForce * dirY;
            }
            forceX += strongForceX;
            forceY += strongForceY;
        }

        // Gravity
        double gravityForce = (gravityConstant * p1.getMass() * p2.getMass()) / Math.pow(dist, 2);
        forceX += gravityForce * dirX;
        forceY += gravityForce * dirY;

        return new double[] {forceX, forceY};
    }


    // Getters and setters

    public void resetForce() {
        this.netX = 0.0;
        this.netY = 0.0;
    }

    public void addForce(double forceX, double forceY) {
        this.netX += forceX;
        this.netY += forceY;
    }

    public boolean isNucleon() {
        return particleType.equals("proton") || particleType.equals("neutron");
    }

    public boolean isElectron() {
        return particleType.equals("electron");
    }

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
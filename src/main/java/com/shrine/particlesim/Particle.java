package com.shrine.particlesim;

public class Particle {
    protected double xCor; // Use double for smoother motion
    protected double yCor;
    protected double xVel;
    protected double yVel;
    protected double zVel;
    protected double zCor;
    protected double mass;
    protected double charge;
    protected String particleType;
    protected double radius;

    // Store forces to update
    protected double netX;
    protected double netY;
    protected double netZ;

    public Particle(double xCor, double yCor, double zCor, double xVel, double yVel, double zVel, double charge, double mass, String particleType) {
        this.xCor = xCor;
        this.yCor = yCor;
        this.zCor = zCor;
        this.xVel = xVel;
        this.yVel = yVel;
        this.zVel = zVel;
        this.charge = charge;
        this.mass = mass;
        this.particleType = particleType;

        if (this.isNucleon()) {this.radius = 9.0;}
        else if (this.isElectron()) {this.radius = 3.0;}
        else {this.radius = 3 + ((mass + charge) * .05);}
    }

    public void updatePos() {
        // may come back to this but will keep time as 60fps
        // double time = (1.0 / Main.getFPS()) * 10;
        double time = 0.166666666667; // mult by 10 for sim is super slow
        double maxSpeed = Main.getMaxSpeed();

        double speed = Math.sqrt(xVel * xVel + yVel * yVel + zVel * zVel);
        if (speed > maxSpeed) {
            xVel *= maxSpeed / speed;
            yVel *= maxSpeed / speed;
            zVel *= maxSpeed / speed;
        }
        xVel *= .90; // Drag
        yVel *= .90;
        xCor += xVel * time;
        yCor += yVel * time;
        zCor += zVel * time;

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
        // may come back to this but will keep time as 60fps
        // double time = (1.0 / Main.getFPS()) * 10;

        double time = 0.166666666667; // mult by 10 for sim is super slow

        if (this.mass == 0) return;
        double accelX = this.netX / this.mass;
        double accelY = this.netY / this.mass;
        double accelZ = this.netZ / this.mass;
        this.xVel += accelX * time;
        this.yVel += accelY * time;
        this.zVel += accelZ * time;
    }

    public double[] calculateForces(Particle p2) {
        double kConstant = Main.getCoulombConstant();
        double strongForceOuterRadius = Main.getStrongForceOuterRadius();
        double strongForceInnerRadius = Main.getStrongForceInnerRadius();
        double strongForceConstant = Main.getStrongForceConstant();
        double gravityConstant = Main.getGravityConstant();

        Particle p1 = this;
        double forceX = 0;
        double forceY = 0;
        double forceZ = 0;

        // Distances
        double distX = p2.getxCor() - p1.getxCor();
        double distY = p2.getyCor() - p1.getyCor();
        double distZ = p2.getzCor() - p1.getzCor();
        double distsq = distX * distX + distY * distY + distZ * distZ;
        double dist = Math.sqrt(distsq);

        if (dist < 1e-8) {
            return new double[] {0,0,0};
        }

        double dirX = distX / dist;
        double dirY = distY / dist;
        double dirZ = distZ / dist;

        // Charges
        double charge1 = p1.getCharge();
        double charge2 = p2.getCharge();

        // Culomb Force
        double coulombForce = (kConstant * charge1 * charge2) / distsq;
        forceX -= coulombForce * dirX;
        forceY -= coulombForce * dirY;
        forceZ -= coulombForce * dirZ;

        // Swirl effect
        if ((p1.isElectron() && charge2 > 0) || (p2.isElectron() && charge1 > 0)) {
            double combinedRadius = p1.radius + p2.radius;
            if (dist < combinedRadius + 20) {
                forceX = -forceX;
                forceY = -forceY;
                forceZ = -forceZ;

                double helperX = 0;
                double helperY = 1;
                double helperZ = 0;

                // Cross product: tangent = dir × helper
                double tangentX = dirY * helperZ - dirZ * helperY;
                double tangentY = dirZ * helperX - dirX * helperZ;
                double tangentZ = dirX * helperY - dirY * helperX;

                // Normalize tangent
                double mag = Math.sqrt(tangentX * tangentX + tangentY * tangentY + tangentZ * tangentZ);
                if (mag != 0) {
                    tangentX /= mag;
                    tangentY /= mag;
                    tangentZ /= mag;

                    // Apply tangential force
                    double tangentialForce = coulombForce * 0.5; // tune this
                    forceX += tangentialForce * tangentX;
                    forceY += tangentialForce * tangentY;
                    forceZ += tangentialForce * tangentZ;
                }
            }
        }



        // Strong Force
        if ((p1.isNucleon() && p2.isNucleon()) && dist <= strongForceOuterRadius) {
            double strongForce = strongForceConstant / (distsq * dist);
            double strongForceX, strongForceY, strongForceZ;

            if (dist < strongForceInnerRadius) {
                // Repulsive
                strongForceX = -strongForce * dirX;
                strongForceY = -strongForce * dirY;
                strongForceZ = -strongForce * dirZ;
            } else {
                // Attractive
                strongForceX = strongForce * dirX;
                strongForceY = strongForce * dirY;
                strongForceZ = strongForce * dirZ;
            }
            forceX += strongForceX;
            forceY += strongForceY;
            forceZ += strongForceZ;
        }

        // Gravity
        double gravityForce = (gravityConstant * p1.getMass() * p2.getMass()) / distsq;
        forceX += gravityForce * dirX;
        forceY += gravityForce * dirY;
        forceZ += gravityForce * dirZ;

        return new double[] {forceX, forceY, forceZ};
    }


    // Getters and setters

    public void resetForce() {
        this.netX = 0.0;
        this.netY = 0.0;
        this.netZ = 0.0;
    }

    public void addForce(double forceX, double forceY, double forceZ) {
        this.netX += forceX;
        this.netY += forceY;
        this.netZ += forceZ;
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
    public double getzVel() {
        return zVel;
    }

    public void setzVel(double zVel) {
        this.zVel = zVel;
    }

    public double getzCor() {
        return zCor;
    }

    public void setzCor(double zCor) {
        this.zCor = zCor;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getNetX() {
        return netX;
    }

    public void setNetX(double netX) {
        this.netX = netX;
    }

    public double getNetY() {
        return netY;
    }

    public void setNetY(double netY) {
        this.netY = netY;
    }

    public double getNetZ() {
        return netZ;
    }

    public void setNetZ(double netZ) {
        this.netZ = netZ;
    }
}
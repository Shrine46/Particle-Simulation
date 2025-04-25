package com.shrine.particlesim;

import java.util.List;
import java.util.concurrent.Callable;

public class ForceCalculationTask implements Callable<Void> {
    private final int startIndex;
    private final int endIndex;
    private final List<Particle> particles;
    private final double[] batchForceX;
    private final double[] batchForceY;
    private final double[] batchForceZ;

    public ForceCalculationTask(int startIndex, int endIndex, List<Particle> particles) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.particles = particles;
        this.batchForceX = new double[particles.size()];
        this.batchForceY = new double[particles.size()];
        this.batchForceZ = new double[particles.size()];
    }

    @Override
    public Void call() {
        for (int i = startIndex; i < endIndex; i++) {
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p1 = particles.get(i);
                Particle p2 = particles.get(j);
                
                double[] forceOnP1 = p1.calculateForces(p2);
                
                // Accumulate forces in local arrays
                batchForceX[i] += forceOnP1[0];
                batchForceY[i] += forceOnP1[1];
                batchForceZ[i] += forceOnP1[2];
                batchForceX[j] -= forceOnP1[0];
                batchForceY[j] -= forceOnP1[1];
                batchForceZ[j] -= forceOnP1[2];

                // Handle collisions
                double distX = p2.getxCor() - p1.getxCor();
                double distY = p2.getyCor() - p1.getyCor();
                double distZ = p2.getzCor() - p1.getzCor();
                double distSq = distX * distX + distY * distY + distZ * distZ;
                
                double collisionRadiusSum = p1.getRadius() + p2.getRadius();
                double collisionRadiusSumSq = collisionRadiusSum * collisionRadiusSum;
                
                if (distSq < collisionRadiusSumSq && distSq > 1e-9) {
                    handleCollision(p1, p2, distX, distY, distZ, distSq);
                }
            }
        }

        // Apply accumulated forces in one synchronized block per particle
        for (int i = startIndex; i < endIndex; i++) {
            Particle p = particles.get(i);
            synchronized (p) {
                p.addForce(batchForceX[i], batchForceY[i], batchForceZ[i]);
            }
        }
        return null;
    }

    private void handleCollision(Particle p1, Particle p2, double distX, double distY, double distZ, double distSq) {
        double dist = Math.sqrt(distSq);
        double overlap = (p1.getRadius() + p2.getRadius()) - dist;
        
        double dirX = distX / dist;
        double dirY = distY / dist;
        double dirZ = distZ / dist;
        
        double totalMass = p1.getMass() + p2.getMass();
        double pushFactor1 = (p2.getMass() / totalMass) * overlap;
        double pushFactor2 = (p1.getMass() / totalMass) * overlap;
        
        synchronized (p1) {
            p1.setxCor(p1.getxCor() - dirX * pushFactor1);
            p1.setyCor(p1.getyCor() - dirY * pushFactor1);
            p1.setzCor(p1.getzCor() - dirZ * pushFactor1);
        }
        
        synchronized (p2) {
            p2.setxCor(p2.getxCor() + dirX * pushFactor2);
            p2.setyCor(p2.getyCor() + dirY * pushFactor2);
            p2.setzCor(p2.getzCor() + dirZ * pushFactor2);
        }
        
        double relativeVelX = p2.getxVel() - p1.getxVel();
        double relativeVelY = p2.getyVel() - p1.getyVel();
        double relativeVelZ = p2.getzVel() - p1.getzVel();
        
        double dotProduct = relativeVelX * dirX + relativeVelY * dirY + relativeVelZ * dirZ;
        
        if (dotProduct < 0) {
            double elasticity = 0.7;
            double collisionScale = (1.0 + elasticity) * dotProduct / totalMass;
            double impulseFactorX = collisionScale * dirX;
            double impulseFactorY = collisionScale * dirY;
            double impulseFactorZ = collisionScale * dirZ;
            
            synchronized (p1) {
                p1.setxVel(p1.getxVel() + impulseFactorX * p2.getMass());
                p1.setyVel(p1.getyVel() + impulseFactorY * p2.getMass());
                p1.setzVel(p1.getzVel() + impulseFactorZ * p2.getMass());
            }
            
            synchronized (p2) {
                p2.setxVel(p2.getxVel() - impulseFactorX * p1.getMass());
                p2.setyVel(p2.getyVel() - impulseFactorY * p1.getMass());
                p2.setzVel(p2.getzVel() - impulseFactorZ * p1.getMass());
            }
        }
    }
}
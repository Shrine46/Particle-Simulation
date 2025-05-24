package com.shrine.particlesim;

import java.util.ArrayList;
import java.util.List;

public class Nucleus {
    private List<Particle> nucleons;
    private double centerX, centerY, centerZ;
    
    public Nucleus() {
        this.nucleons = new ArrayList<>();
    }
    
    public void addNucleon(Particle nucleon) {
        if (!nucleons.contains(nucleon)) {
            nucleons.add(nucleon);
            updateCenter();
        }
    }
    
    public boolean containsNucleon(Particle nucleon) {
        return nucleons.contains(nucleon);
    }
    
    public List<Particle> getNucleons() {
        return nucleons;
    }
    
    public void updateCenter() {
        double sumX = 0, sumY = 0, sumZ = 0;
        for (Particle p : nucleons) {
            sumX += p.getxCor();
            sumY += p.getyCor();
            sumZ += p.getzCor();
        }
        centerX = sumX / nucleons.size();
        centerY = sumY / nucleons.size();
        centerZ = sumZ / nucleons.size();
    }
    
    public boolean isCloseToNucleus(Particle particle) {
        // Check if the particle is close to any nucleon in this nucleus
        double radius = particle.getRadius() * 2; // Same distance as nucleus detection
        for (Particle nucleon : nucleons) {
            double distX = particle.getxCor() - nucleon.getxCor();
            double distY = particle.getyCor() - nucleon.getyCor();
            double distZ = particle.getzCor() - nucleon.getzCor();
            double distSq = distX * distX + distY * distY + distZ * distZ;
            if (distSq <= radius * radius) {
                return true;
            }
        }
        return false;
    }
    
    public int getProtonCount() {
        return (int) nucleons.stream()
            .filter(p -> p.getParticleType().equals("proton"))
            .count();
    }
    
    public int getNeutronCount() {
        return (int) nucleons.stream()
            .filter(p -> p.getParticleType().equals("neutron"))
            .count();
    }
    
    public void removeNucleon(Particle nucleon) {
        nucleons.remove(nucleon);
        if (!nucleons.isEmpty()) {
            updateCenter();
        }
    }
    
    public boolean isEmpty() {
        return nucleons.isEmpty();
    }
}

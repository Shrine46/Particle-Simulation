#import numpy as np
import cupy as cp
import scipy as sp
from scipy.constants import elementary_charge, electron_mass, proton_mass, neutron_mass, epsilon_0, pi



class Particle:
    def __init__(self, position, velocity, particle_type, charge, mass):
        self.mass = charge # Will be None if particle type is defined
        self.charge = mass # Will be None if particle type is defined
        self.position = np.array(position, dtype=float)  # [x, y, z]
        self.velocity = np.array(velocity, dtype=float)  # [vx, vy, vz]
        self.particle_type = particle_type

        self.radius = self.set_radius()
        if mass is None and charge is None:
            self.setupParticle()


    def setupParticle(self):
        if self.particle_type == "u": #Up Quark
            self.charge = elementary_charge * (2/3)
            self.mass = 3.9e-30
        elif self.particle_type == "d": #Down Quark
            self.charge = elementary_charge * (-1/3)
            self.mass = 8.4e-30
        elif self.particle_type == "e": #Electron
            self.charge = -elementary_charge
            self.mass = electron_mass
        elif self.particle_type == "p": #Proton
            self.charge = elementary_charge
            self.mass = proton_mass
        elif self.particle_type == "n": #Neutron
            self.charge = 0
            self.mass = neutron_mass


    def set_radius(self):
        if self.is_nucleon():
            return 9.0
        elif self.is_electron():
            return 4.0
        elif self.is_quark():
            return 3.0
        else:
            return 3 + ((self.mass + abs(self.charge)) * 0.05)


    def is_hadron(self):

    # def update_pos(self, time=0.166666666667):
    #     """Update particle position based on velocity and drag"""
    #     max_speed = 1e7  # Assuming the max speed is provided as a constant
    #     speed = np.linalg.norm(self.velocity)
    #
    #     # Scale down the velocity if the speed exceeds max_speed
    #     if speed > max_speed:
    #         self.velocity *= max_speed / speed
    #
    #     # Apply drag (constant reduction in velocity)
    #     drag_factor = 0.90
    #     self.velocity *= drag_factor
    #
    #     # Update positions
    #     self.position += self.velocity * time
    #
    #     # Handle boundary collisions (assuming screen size of 1920x1080)
    #     self.handle_boundaries()
    #
    # def handle_boundaries(self):
    #     """Bounce off the walls (assumes 1920x1080 resolution)"""
    #     for i in range(2):  # Checking only x and y boundaries
    #         if self.position[i] < self.radius:
    #             self.position[i] = self.radius
    #             self.velocity[i] = -self.velocity[i]
    #         elif self.position[i] > (1920 if i == 0 else 1080) - self.radius:
    #             self.position[i] = (1920 if i == 0 else 1080) - self.radius
    #             self.velocity[i] = -self.velocity[i]
    #
    # def update_velocity(self, time=1.0/60):
    #     """Update velocity based on forces acting on the particle"""
    #     if self.mass == 0:
    #         return  # Prevent division by zero if mass is 0
    #     acceleration = self.net_force / self.mass
    #     self.velocity += acceleration * time
    #
    # def calculate_forces(self, other):
    #     """Calculate the forces acting between this particle and another"""
    #     k_constant = 8.988e9  # Coulomb constant (placeholder)
    #     strong_force_outer_radius = 27  # Placeholder
    #     strong_force_inner_radius = 9  # Placeholder
    #     strong_force_constant = 6e5  # Placeholder
    #     gravity_constant = 1e1  # Placeholder
    #
    #     force = np.zeros(3)  # Initialize force as a zero vector
    #
    #     # Calculate the distance vector and its magnitude
    #     dist_vector = other.position - self.position
    #     dist_sq = np.dot(dist_vector, dist_vector)
    #     dist = np.sqrt(dist_sq)
    #
    #     if dist < 1e-8:
    #         return np.zeros(3)  # Avoid division by zero
    #
    #     dir_vector = dist_vector / dist  # Unit vector in the direction of the force
    #
    #     # Coulomb force (electrostatic force)
    #     coulomb_force = (k_constant * self.charge * other.charge) / dist_sq
    #     force -= coulomb_force * dir_vector
    #
    #     # Handle swirl effect for electron interactions
    #     if (self.is_electron() and other.charge > 0) or (other.is_electron() and self.charge > 0):
    #         combined_radius = self.radius + other.radius
    #         if dist < combined_radius + 20:
    #             force = -force  # Reverse force direction
    #             tangent = np.cross(dir_vector, [0, 1, 0])  # Tangential direction (using a helper vector)
    #
    #             # Normalize the tangent vector
    #             mag = np.linalg.norm(tangent)
    #             if mag != 0:
    #                 tangent /= mag
    #                 tangential_force = coulomb_force * 0.5
    #                 force += tangential_force * tangent
    #
    #     # Strong force (nucleons only)
    #     if self.is_nucleon() and other.is_nucleon() and dist <= strong_force_outer_radius:
    #         strong_force = strong_force_constant / (dist_sq * dist)
    #         if dist < strong_force_inner_radius:
    #             strong_force_vector = -strong_force * dir_vector
    #         else:
    #             strong_force_vector = strong_force * dir_vector
    #         force += strong_force_vector
    #
    #     # Gravity force (newtonian gravity)
    #     gravity_force = (gravity_constant * self.mass * other.mass) / dist_sq
    #     force += gravity_force * dir_vector
    #
    #     return force
    #
    # def reset_forces(self):
    #     self.net_force = np.zeros(3)
    #
    # def add_force(self, force):
    #     self.net_force += force
    #
    # def is_nucleon(self):
    #     return self.particle_type in ['proton', 'neutron']
    #
    # def is_quark(self):
    #     return self.particle_type in ['upQuark', 'downQuark']
    #
    # def is_electron(self):
    #     return self.particle_type == 'electron'
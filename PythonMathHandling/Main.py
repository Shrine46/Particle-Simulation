import itertools
import numpy as np
# import cupy as cp # Optional GPU acceleration
import scipy as sp
from scipy.constants import elementary_charge, electron_mass, proton_mass, neutron_mass, epsilon_0, pi, hbar, c
import time
import socket
import json
import threading

# --- Configuration ---
HOST = '127.0.0.1'
PORT = 9565
WIDTH, HEIGHT, DEPTH = 1000, 800, 800 # Visual boundaries in pixels
VISUAL_SCALE = 100  # Pixels per femtometer (1 pixel = 1e-17 m)
TIME_STEP = 1e-22   # Simulation time step in seconds (extremely small for nuclear scales!)
DRAG_FACTOR = 0.999 # Damping factor per step
MAX_FORCE = 1e7     # Limit force magnitude (Newtons) to prevent instability

# --- Physical Constants ---
COULOMB_CONSTANT = 1 / (4 * pi * epsilon_0)
GRAVITY_CONSTANT = 6.67430e-11

# --- Strong Force Parameters ---
# Using fm for radii definition, converted to meters internally
STRONG_FORCE_OUTER_RADIUS_FM = 2.5 # fm
STRONG_FORCE_INNER_RADIUS_FM = 0.8 # fm
STRONG_FORCE_OUTER_RADIUS_M = STRONG_FORCE_OUTER_RADIUS_FM * 1e-15
STRONG_FORCE_INNER_RADIUS_M = STRONG_FORCE_INNER_RADIUS_FM * 1e-15

# Yukawa potential V(r) = -g^2 * exp(-mu*r) / r
# Force F(r) = -dV/dr = -g^2 * exp(-mu*r) * (mu/r + 1/r^2)
# mu is related to the mediator mass (pion ~140 MeV/c^2)
# mu = m_pion * c / hbar approx 1 / (1.4 fm)
PION_MASS_APPROX = 140e6 * elementary_charge / c**2 # kg
MU = PION_MASS_APPROX * c / hbar # approx 1 / (1.4e-15 m)

# g^2 relates to the strong coupling constant alpha_s = g^2 / (4*pi*hbar*c)
# alpha_s ~ 0.1 to 1 at low energies. Let's try alpha_s = 0.5
ALPHA_S = 0.5
STRONG_FORCE_G2 = ALPHA_S * (4 * pi * hbar * c) # approx 1e-26 to 1e-27 range

# Distance threshold for *checking* hadron formation (in pixels)
STRONG_FORCE_DISTANCE_THRESHOLD_PX = STRONG_FORCE_OUTER_RADIUS_FM * VISUAL_SCALE * 1.5 # Check slightly beyond radius

# --- Hadron Definitions ---
stable_hadrons = {
    "proton": {"quarks": sorted(["u", "u", "d"]), "charge": elementary_charge, "mass": proton_mass},
    "neutron": {"quarks": sorted(["u", "d", "d"]), "charge": 0, "mass": neutron_mass}
}

# --- Global list for particles (accessible by server thread) ---
particles = []
particle_lock = threading.Lock() # To safely access particles from multiple threads

class Particle:
    def __init__(self, position_px, velocity_mps, particle_type, charge=None, mass=None):
        # position_px is the initial VISUAL position in pixels
        # velocity_mps is the initial PHYSICAL velocity in m/s
        self.position_px = np.array(position_px, dtype=float) # Visual position (pixels)
        self.velocity_mps = np.array(velocity_mps, dtype=float) # Physical velocity (m/s)
        self.particle_type = particle_type
        self.is_bound = False # if its in a hadron
        self.net_force_N = np.zeros(3) # Physical force (Newtons)

        self.charge = charge # For custom particles
        self.mass_kg = mass # For custom particles
        if self.mass_kg is None or self.charge is None:
            self.setupParticle()

        # Convert pixel scale to meters scale for internal use if needed
        # Position in meters = position_px / VISUAL_SCALE * 1e-15
        # However, we primarily work with pixel positions for rendering
        # and calculate forces based on converted distances.

        self.radius_fm = self.set_radius_fm() # Radius in femtometers
        self.radius_px = self.radius_fm * VISUAL_SCALE # Radius in pixels

    def setupParticle(self):
        if self.particle_type == "u": # Up Quark
            self.charge = elementary_charge * (2/3)
            self.mass_kg = 2.2e6 * elementary_charge / c**2 # MeV/c^2 to kg (~3.9e-30 kg)
        elif self.particle_type == "d": # Down Quark
            self.charge = elementary_charge * (-1/3)
            self.mass_kg = 4.7e6 * elementary_charge / c**2 # MeV/c^2 to kg (~8.4e-30 kg)
        elif self.particle_type == "e": # Electron
            self.charge = -elementary_charge
            self.mass_kg = electron_mass
        elif self.particle_type == "proton":
            self.charge = elementary_charge
            self.mass_kg = proton_mass
        elif self.particle_type == "neutron":
            self.charge = 0
            self.mass_kg = neutron_mass
        else: # Default for custom particles if not specified
             self.charge = 0
             self.mass_kg = 1e-27 # Assign a default mass

    def set_radius_fm(self):
        if self.is_nucleon():
            return 0.84 # Charge radius fm
        elif self.is_electron():
            # Classical electron radius is ~2.8 fm, but point-like in QFT. Use small value.
            return 0.05 # fm (visual representation)
        elif self.is_quark():
             # Quarks are point-like, but give them a small visual size
             return 0.1 # fm (visual representation)
        else: # Estimate for custom particles
            base_radius_fm = 0.84
            # Rough scaling based on mass (very approximate)
            mass_ratio = self.mass_kg / proton_mass if proton_mass > 0 else 1
            mass_effect_fm = np.cbrt(mass_ratio) * 0.1 # Scale by cube root of mass ratio
            return base_radius_fm + mass_effect_fm

    def get_display_radius_px(self):
        # Ensure minimum visual radius
        return max(1.0, self.radius_px)

    def calculate_forces(self, other):
        """ Calculates the physical force in Newtons exerted by 'other' on 'self'. """
        force_N = np.zeros(3)

        # Vector from self to other in PIXELS
        dist_vector_px = other.position_px - self.position_px
        dist_px = np.linalg.norm(dist_vector_px)

        # Prevent division by zero if particles are exactly at the same position
        if dist_px < 1e-9:
             return force_N # No force

        # Convert pixel distance to physical distance in METERS
        # dist_m = (dist_px / VISUAL_SCALE) * 1e-15 # fm -> m
        dist_m = dist_px * (1e-15 / VISUAL_SCALE) # More direct: pixels * (fm/pixel) * (m/fm)


        # Prevent division by zero for physical distance
        if dist_m < 1e-18: # Avoid extremely small distances
             dist_m = 1e-18

        dist_sq_m = dist_m * dist_m
        # Normalized direction vector (pointing from self to other)
        dir_vector = dist_vector_px / dist_px

        # 1. Electromagnetic Force (Coulomb)
        if self.charge != 0 and other.charge != 0:
            # F = k * q1 * q2 / r^2
            # Negative force means attractive (opposite charges)
            # Positive force means repulsive (like charges)
            coulomb_force_mag_N = (COULOMB_CONSTANT * self.charge * other.charge) / dist_sq_m
            force_N += coulomb_force_mag_N * dir_vector

        # 2. Strong Force (Yukawa for Nucleons)
        # Residual strong force between nucleons
        if self.is_nucleon() and other.is_nucleon():
            # Only applies within a certain range
            if STRONG_FORCE_INNER_RADIUS_M < dist_m < STRONG_FORCE_OUTER_RADIUS_M:
                # F(r) = -g^2 * exp(-mu*r) * (mu/r + 1/r^2)
                # This force is attractive
                exp_term = np.exp(-MU * dist_m)
                yukawa_force_mag_N = -STRONG_FORCE_G2 * exp_term * ( (MU / dist_m) + (1.0 / dist_sq_m) )
                # Add attractive force (pointing towards other)
                force_N += yukawa_force_mag_N * dir_vector
            elif dist_m <= STRONG_FORCE_INNER_RADIUS_M:
                 # Add a repulsive core (simple model: strong repulsion)
                 # Scale roughly inverse cubic or higher power
                 repulsive_mag = STRONG_FORCE_G2 * 100 / (dist_m**3) # Needs tuning
                 force_N -= repulsive_mag * dir_vector # Repulsive force


        # 3. Gravity (Usually negligible at this scale, but include for completeness)
        if self.mass_kg > 0 and other.mass_kg > 0:
             # F = G * m1 * m2 / r^2 (Always attractive)
             gravity_force_mag_N = (GRAVITY_CONSTANT * self.mass_kg * other.mass_kg) / dist_sq_m
             force_N += gravity_force_mag_N * dir_vector # Attractive force

        # --- Swirl Effect (Visual enhancement, not strictly physical) ---
        # Keep interaction range check in pixels for visual proximity
        if (self.is_electron() and other.charge > 0) or \
           (other.is_electron() and self.charge > 0):
            combined_radius_px = self.radius_px + other.radius_px
            if dist_px < combined_radius_px + 20: # Within 20 pixels visual range
                 coulomb_force_mag = 0
                 if self.charge != 0 and other.charge != 0:
                     # Recalculate magnitude for effect scaling (can be simplified)
                     coulomb_force_mag = abs((COULOMB_CONSTANT * self.charge * other.charge) / dist_sq_m)

                 if coulomb_force_mag > 0:
                     # Add a tangential component for swirl
                     # Tangent vector (2D simplification for visualization)
                     tangent = np.array([-dir_vector[1], dir_vector[0], 0])
                     tangent_mag = np.linalg.norm(tangent)
                     if tangent_mag > 1e-9:
                         tangent /= tangent_mag
                         # Scale tangential force relative to coulomb, apply perpendicular to separation
                         swirl_strength = 0.05 # Tunable parameter
                         tangential_force_N = coulomb_force_mag * swirl_strength * tangent
                         force_N += tangential_force_N
                         # Optional: Slightly reduce radial attraction to simulate orbit
                         # force_N -= coulomb_force_mag * 0.1 * dir_vector


        # Force Limiting
        force_mag_N = np.linalg.norm(force_N)
        if force_mag_N > MAX_FORCE:
            # print(f"Warning: Force limit hit between {self.particle_type} and {other.particle_type}. Mag: {force_mag_N:.2e}")
            force_N = force_N * (MAX_FORCE / force_mag_N)

        # Return the physical force in Newtons acting ON self FROM other
        return force_N

    def update_velocity(self, time_step):
        """ Updates physical velocity based on net physical force. """
        if self.mass_kg <= 1e-35: # Avoid division by zero / issues with massless particles
            # print(f"Warning: Particle {self.particle_type} has near-zero mass.")
            return # Cannot accelerate

        # Physical acceleration: a = F/m (m/s^2)
        acceleration_mps2 = self.net_force_N / self.mass_kg

        # Update physical velocity: v = v0 + a*t (m/s)
        self.velocity_mps += acceleration_mps2 * time_step

        # Apply drag factor to physical velocity
        self.velocity_mps *= DRAG_FACTOR

    def update_position(self, time_step, xBound, yBound, zBound):
        """ Updates visual position based on physical velocity. """
        # Physical displacement: delta_x = v * t (meters)
        delta_pos_m = self.velocity_mps * time_step

        # Convert physical displacement (meters) to visual displacement (pixels)
        # delta_pixels = delta_m / (m/fm) / (fm/pixel) = delta_m / (1e-15 / VISUAL_SCALE)
        delta_pos_px = delta_pos_m * (VISUAL_SCALE / 1e-15)

        # Update visual position
        self.position_px += delta_pos_px

        # --- Speed Limiting (Optional, applied visually) ---
        # max_speed_pixels_per_step = 5.0 # Max visual movement per step
        # visual_speed_pps = np.linalg.norm(delta_pos_px) / time_step # Not quite right units-wise
        # visual_velocity_pps = delta_pos_px / time_step
        # speed_pps = np.linalg.norm(visual_velocity_pps)
        # if speed_pps > max_speed_pixels_per_step:
        #     scale = max_speed_pixels_per_step / speed_pps
        #     self.position_px = self.position_px - delta_pos_px + (delta_pos_px * scale) # Correct position based on limited visual speed


        # Handle boundaries using visual coordinates
        self.handle_boundaries(xBound, yBound, zBound)

    def handle_boundaries(self, xBound, yBound, zBound):
        """ Keep particles within visual bounds and reflect velocity. """
        bounds = np.array([xBound, yBound, zBound])
        bounce_factor = -0.5 # Energy loss on collision with wall

        for i in range(3): # Check x, y, z
            if self.position_px[i] < self.radius_px:
                self.position_px[i] = self.radius_px # Place particle just inside boundary
                self.velocity_mps[i] *= bounce_factor # Reverse velocity component
            elif self.position_px[i] > bounds[i] - self.radius_px:
                self.position_px[i] = bounds[i] - self.radius_px # Place particle just inside
                self.velocity_mps[i] *= bounce_factor # Reverse velocity component

    def reset_forces(self):
        """ Resets the net physical force accumulation for the next step. """
        self.net_force_N = np.zeros(3)

    def add_force(self, force_N):
        """ Adds a physical force (Newtons) to the net force. """
        self.net_force_N += force_N

    # --- Type Checkers ---
    def is_nucleon(self):
        return self.particle_type in ['proton', 'neutron']

    def is_quark(self):
        return self.particle_type in ['u', 'd'] # Extend later if needed

    def is_electron(self):
        return self.particle_type == 'e'

# --- Hadron Formation Logic ---
def form_hadron(quarks, stable_hadrons_def):
    """
    Attempt to form a stable hadron (proton or neutron) from a list of 3 quarks.
    Returns a new hadron Particle if successful, or None otherwise.
    Assumes input 'quarks' is a list of 3 Particle objects.
    """
    # 1. Check basic conditions (already done mostly before calling)
    if len(quarks) != 3: return None
    if any(q.is_bound for q in quarks): return None

    # 2. Check if the quarks are close enough (using visual pixel distance threshold)
    # This is a necessary condition for the strong force to be effective
    positions_px = [q.position_px for q in quarks]
    distances_px = [np.linalg.norm(p1 - p2) for p1, p2 in itertools.combinations(positions_px, 2)]

    if any(d > STRONG_FORCE_DISTANCE_THRESHOLD_PX for d in distances_px):
        # print(f"Debug: Quarks too far for hadron formation. Max dist: {max(distances_px):.1f} > Threshold: {STRONG_FORCE_DISTANCE_THRESHOLD_PX:.1f}")
        return None

    # 3. Check if the quark types match a known stable hadron
    quark_types = sorted([q.particle_type for q in quarks])

    for hadron_name, properties in stable_hadrons_def.items():
        if quark_types == properties["quarks"]:
            print(f"Success: Forming {hadron_name} from quarks!")

            # Mark constituent quarks as bound (they will be removed later)
            for q in quarks:
                q.is_bound = True

            # Calculate properties of the new hadron
            # Use PHYSICAL mass and velocity for calculations
            total_mass_kg = sum(q.mass_kg for q in quarks)
            if total_mass_kg <= 1e-35:
                 print("Warning: Cannot form hadron, zero total quark mass.")
                 for q in quarks: q.is_bound = False # Revert bound state
                 return None

            # Center of mass (VISUAL position, weighted by PHYSICAL mass)
            center_of_mass_px = sum(q.position_px * q.mass_kg for q in quarks) / total_mass_kg

            # Total momentum (PHYSICAL: kg * m/s)
            total_momentum_kgmps = sum(q.velocity_mps * q.mass_kg for q in quarks)

            # Resulting hadron's PHYSICAL mass and charge
            hadron_mass_kg = properties["mass"]
            hadron_charge = properties["charge"]

            # Initial PHYSICAL velocity of hadron (Conservation of Momentum: p=mv -> v=p/m)
            velocity_mps = total_momentum_kgmps / hadron_mass_kg if hadron_mass_kg > 0 else np.zeros(3)

            # Create the new hadron particle
            new_hadron = Particle(
                position_px=center_of_mass_px,
                velocity_mps=velocity_mps,
                particle_type=hadron_name,
                charge=hadron_charge,
                mass=hadron_mass_kg
            )
            return new_hadron

    # If no matching hadron found
    # print(f"Debug: Quark combination {quark_types} does not form a known stable hadron.")
    return None

# --- Data Serialization for Client ---
def serialize_particles_for_client(particles_list):
    """ Convert particle data into a JSON-serializable format for the client. """
    particle_data = []
    for p in particles_list:
        if not p.is_bound: # Only send active particles
            particle_data.append({
                "type": p.particle_type,
                # Send visual position (rounded for less data)
                "position": [round(p.position_px[0], 2), round(p.position_px[1], 2)],
                # Send visual radius
                "radius": round(p.get_display_radius_px(), 2)
            })
    # Add a newline character for reliable reading by client
    return json.dumps(particle_data) + '\n'

# --- Server Thread ---
def server_thread_func(host, port):
    """ Starts the socket server and sends particle data periodically. """
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1) # Allow address reuse
    try:
        server_socket.bind((host, port))
        server_socket.listen(1) # Listen for one connection
        print(f"Server listening on {host}:{port}")

        while True: # Keep accepting connections if one drops
            try:
                conn, addr = server_socket.accept()
                with conn:
                    print(f"Connected by {addr}")
                    client_alive = True
                    while client_alive:
                        # Get current particle state safely
                        with particle_lock:
                            if not particles: # Check if list is empty
                                data_to_send = "[]\n"
                            else:
                                # Create a copy to avoid issues if list modified during serialization
                                current_particles = list(particles)
                                data_to_send = serialize_particles_for_client(current_particles)

                        # Send data
                        try:
                            conn.sendall(data_to_send.encode('utf-8'))
                        except (BrokenPipeError, ConnectionResetError):
                            print(f"Client {addr} disconnected.")
                            client_alive = False # Exit inner loop
                        except Exception as e:
                            print(f"Error sending data: {e}")
                            client_alive = False # Exit inner loop

                        # Control send rate (adjust as needed)
                        # This rate should ideally match or be slightly faster than simulation steps
                        # but slower than client rendering capability.
                        time.sleep(1 / 60.0) # Target ~60 updates per second

            except ConnectionAbortedError:
                print("Server socket accept aborted (likely server shutdown).")
                break # Exit outer loop if server socket closed
            except Exception as e:
                print(f"Error accepting connection: {e}")
                time.sleep(1) # Wait before retrying accept

    except OSError as e:
         print(f"!!! Server Error: Could not bind to {host}:{port}. Is it already in use? ({e})")
    except Exception as e:
        print(f"!!! Server Thread Error: {e}")
    finally:
        print("Closing server socket.")
        server_socket.close()

# ==============================
# --- Main Simulation Setup ---
# ==============================

# Example: Create some initial quarks (adjust positions for larger scale)
# Arguments: position_px (pixels), velocity_mps (m/s), type
particles.append(Particle([WIDTH*0.2, HEIGHT*0.5, DEPTH*0.5], [5e4, -2e4, 0], "u"))
particles.append(Particle([WIDTH*0.2 + 30, HEIGHT*0.5 + 10, DEPTH*0.5 + 5], [-5e4, 2e4, 1e4], "u"))
particles.append(Particle([WIDTH*0.2 + 15, HEIGHT*0.5 - 15, DEPTH*0.5 - 5], [1e4, 5e4, -1e4], "d")) # Proton candidate

particles.append(Particle([WIDTH*0.7, HEIGHT*0.5, DEPTH*0.5], [-4e4, -3e4, 0], "d"))
particles.append(Particle([WIDTH*0.7 + 30, HEIGHT*0.5 + 15, DEPTH*0.5 + 10], [4e4, 1e4, -2e4], "d"))
particles.append(Particle([WIDTH*0.7 + 5, HEIGHT*0.5 - 10, DEPTH*0.5 - 10], [0, -4e4, 2e4], "u")) # Neutron candidate

particles.append(Particle([WIDTH*0.5, HEIGHT*0.2, DEPTH*0.5], [0, 0, 0], "e")) # Add an electron

# Start the server in a separate thread
server_thread = threading.Thread(target=server_thread_func, args=(HOST, PORT), daemon=True)
server_thread.start()

# ===========================
# --- Main Simulation Loop ---
# ===========================
running = True
step_count = 0
max_steps = 10000000000000 # Limit simulation duration for testing
last_report_time = time.time()

print("Starting simulation loop...")
try:
    while running and step_count < max_steps:
        start_step_time = time.perf_counter()

        # --- Thread-safe access to particles ---
        with particle_lock:
            # Make a working copy for this step
            current_particles = list(particles)
            num_particles = len(current_particles)

            # 1. Reset forces for all particles
            for p in current_particles:
                p.reset_forces()

            # 2. Calculate pairwise forces
            for i in range(num_particles):
                p1 = current_particles[i]
                if p1.is_bound: continue # Skip bound quarks/particles

                for j in range(i + 1, num_particles):
                    p2 = current_particles[j]
                    if p2.is_bound: continue

                    # Calculate physical force (Newtons)
                    force_on_p1_N = p1.calculate_forces(p2)

                    # Apply forces according to Newton's 3rd Law
                    p1.add_force(force_on_p1_N)
                    p2.add_force(-force_on_p1_N) # Equal and opposite

            # 3. Update Velocities & Positions
            particles_to_remove_indices = [] # Store indices of bound quarks
            for idx, particle in enumerate(current_particles):
                 if not particle.is_bound:
                     particle.update_velocity(TIME_STEP)
                     # Pass visual boundaries
                     particle.update_position(TIME_STEP, WIDTH, HEIGHT, DEPTH)
                 elif particle.is_bound and particle.is_quark():
                     # Mark bound quarks for removal if formation was successful
                     particles_to_remove_indices.append(idx)


            # --- Hadron Formation Check ---
            unbound_quarks = [p for p in current_particles if p.is_quark() and not p.is_bound]
            formed_hadrons_this_step = []
            quarks_indices_to_bind = set() # Keep track of indices used this step

            if len(unbound_quarks) >= 3:
                # Get indices relative to the *current_particles* list
                unbound_indices_map = {p: idx for idx, p in enumerate(current_particles) if p in unbound_quarks}

                # Iterate through combinations of *unbound quarks*
                for q1, q2, q3 in itertools.combinations(unbound_quarks, 3):
                    # Check if any of these quarks were already assigned to a hadron this step
                    idx1 = unbound_indices_map[q1]
                    idx2 = unbound_indices_map[q2]
                    idx3 = unbound_indices_map[q3]
                    if idx1 in quarks_indices_to_bind or idx2 in quarks_indices_to_bind or idx3 in quarks_indices_to_bind:
                        continue

                    # Attempt formation
                    new_hadron = form_hadron([q1, q2, q3], stable_hadrons)

                    if new_hadron:
                        formed_hadrons_this_step.append(new_hadron)
                        # Mark original quarks' indices as bound for removal/update later
                        quarks_indices_to_bind.add(idx1)
                        quarks_indices_to_bind.add(idx2)
                        quarks_indices_to_bind.add(idx3)
                        # Ensure the quark objects themselves are marked bound (should happen in form_hadron)
                        q1.is_bound = True
                        q2.is_bound = True
                        q3.is_bound = True


            # 4. Update master particle list (critical section)
            new_particle_list = []
            removed_count = 0
            for idx, p in enumerate(current_particles):
                if idx in quarks_indices_to_bind:
                    # This quark was successfully bound into a hadron, don't add it back
                    removed_count += 1
                    continue
                # Keep other particles (unbound quarks, electrons, existing hadrons)
                new_particle_list.append(p)

            # Add the newly formed hadrons
            new_particle_list.extend(formed_hadrons_this_step)

            # Atomically update the global list
            particles = new_particle_list

            # --- End of thread-safe block ---

        # --- Loop timing and reporting ---
        end_step_time = time.perf_counter()
        step_duration = end_step_time - start_step_time
        step_count += 1

        current_time = time.time()
        if current_time - last_report_time >= 5.0: # Report every 5 seconds
            print(f"Step: {step_count}, Particles: {len(particles)}, Step Time: {step_duration*1000:.2f} ms")
            last_report_time = current_time
            # Optional: Print detailed particle info periodically
            # with particle_lock:
            #     for p in particles[:5]: # Print first 5
            #         print(f"  Type: {p.particle_type}, Pos_px: {p.position_px.round(1)}, Vel_mps: {p.velocity_mps.round(1)}")


        # --- Control loop speed (optional, server thread has its own timing) ---
        # If step calculation is very fast, avoid busy-waiting
        # time.sleep(max(0, (1/100.0) - step_duration)) # Target ~100 physics steps/sec max


except KeyboardInterrupt:
    print("Simulation interrupted by user.")
finally:
    print("Simulation Finished.")
    running = False # Signal server thread to potentially stop (though it might need explicit closing)
    print(f"Total steps: {step_count}")
    # Wait briefly for server thread to potentially finish sending last message
    time.sleep(0.5)
    # Final particle count
    with particle_lock:
        print(f"Final particle count: {len(particles)}")
        # for p in particles:
        #     print(f" - Type: {p.particle_type}, Pos_px: {p.position_px.round(1)}, Vel_mps: {p.velocity_mps.round(1)}, Bound: {p.is_bound}")

print("Exiting main thread.")
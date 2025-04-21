from dataclasses import dataclass

@dataclass
class Constants:
    # Old (non-scientific) Constants
    max_speed: float = 1e7
    strong_force_outer_radius: float = 27.0
    strong_force_inner_radius: float = 9.0
    fps: int = 60
    coulomb_constant: float = 4e5
    strong_force_constant: float = 6e5
    gravity_constant: float = 1e1




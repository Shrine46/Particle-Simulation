class PhysicsScaler:
    def __init__(self,
                 distance_scale=1e15,   # 1 pixel = 1 femtometer
                 mass_scale=1.0,        # may need to tweak
                 time_scale=1e18,       # 1 second = 1 attosecond
                 force_scale=None):     # Automatically derived if None
        self.distance_scale = distance_scale
        self.mass_scale = mass_scale
        self.time_scale = time_scale

        # Derive force scale: F = m * a = kg * (m/s²)
        self.force_scale = force_scale or (
                mass_scale * distance_scale / (time_scale ** 2)
        )

    def scale_distance(self, value):
        return value * self.distance_scale

    def unscale_distance(self, value):
        return value / self.distance_scale

    def scale_time(self, value):
        return value * self.time_scale

    def unscale_time(self, value):
        return value / self.time_scale

    def scale_force(self, value):
        return value / self.force_scale

    def unscale_force(self, value):
        return value * self.force_scale

package tmc.enderdragon;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;

public class TMCEnderDragonShieldRenderer {

    private static final double RADIUS = 9.0D;
    private static final int POINTS = 240;
    private static final int STRIDE = 3;
    private static final double SPIN_PER_TICK = 0.02;

    private static final ParticleOptions DRAGON_BREATH = PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F);

    private static final Vec3[] UNIT_SPHERE = buildFibonacciSphere(POINTS);

    private static Vec3[] buildFibonacciSphere(int n)
    {
        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
        Vec3[] points = new Vec3[n];
        for (int i = 0; i < n; i++)
        {
            double y = 1.0 - 2.0 * (i + 0.5) / n;
            double r = Math.sqrt(1.0 - y * y);
            double theta = goldenAngle * i;
            points[i] = new Vec3(Math.cos(theta) * r, y, Math.sin(theta) * r);
        }
        return points;
    }

    public static void renderShield(ServerLevel level, EnderDragon dragon, int tick)
    {
        Vec3 center = dragon.getBoundingBox().getCenter();
        double radius = RADIUS + 0.3 * Math.sin(tick * 0.1);
        double angle = tick * SPIN_PER_TICK;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        for (int i = tick % STRIDE; i < POINTS; i++)
        {
            Vec3 p = UNIT_SPHERE[i];
            double x = center.x + (p.x * cos - p.z * sin) * radius;
            double y = center.y + p.y * radius;
            double z = center.z + (p.x * sin + p.z * cos) * radius;

            ParticleOptions particle = switch (i % 4)
            {
                case 0 -> DRAGON_BREATH;
                case 1 -> ParticleTypes.LARGE_SMOKE;
                default -> ParticleTypes.SMOKE;
            };

            level.sendParticles(particle, true, false, x, y, z, 2, 0.15, 0.15, 0.15, 0.0);
        }
    }

    private TMCEnderDragonShieldRenderer() {}
}

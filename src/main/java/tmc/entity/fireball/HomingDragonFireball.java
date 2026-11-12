package tmc.entity.fireball;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.Vec3;
import tmc.entity.enderdragon.TMCEnderDragonRenderer;

public class HomingDragonFireball extends DragonFireball {

    public static HomingDragonFireball launch(ServerLevel serverLevel, EnderDragon shooter, LivingEntity target, Settings settings)
    {
        Vec3 look = shooter.getViewVector(1.0f);
//        Vec3 spawn = new Vec3(shooter.head.getX() - look.x, shooter.head.getY(0.5) + 0.5, shooter.head.getZ() - look.z);
        Vec3 spawn = TMCEnderDragonRenderer.getWarningHeadPos(shooter, 0);

        HomingDragonFireball fb = new HomingDragonFireball(serverLevel, shooter, target, settings);
        fb.snapTo(spawn, 0.0f, 0.0f);
        fb.setDeltaMovement(aimPoint(target).subtract(spawn).normalize().scale(settings.speed()));
        serverLevel.addFreshEntity(fb);
        if (!shooter.isSilent())
        {
            serverLevel.levelEvent(null, LevelEvent.SOUND_DRAGON_FIREBALL, shooter.blockPosition(), 0);
        }
        return fb;
    }

    private static Vec3 aimPoint(LivingEntity livingEntity)
    {
        return livingEntity.position().add(0.0, livingEntity.getBbHeight() * 0.5, 0.0);
    }

    private static Vec3 rotateTowards(Vec3 from, Vec3 to, double maxAngle)
    {
        double angle = Math.acos(Mth.clamp(from.dot(to), -1.0, 1.0));
        if (angle <= maxAngle)
        {
            return to;
        }

        Vec3 axis = from.cross(to);
        if (axis.lengthSqr() < 1.0E-8)
        {
            axis = from.cross(new Vec3(0, 1, 0));
            if (axis.lengthSqr() < 1.0E-8)
            {
                axis = from.cross(new Vec3(1, 0, 0));
            }
        }
        axis = axis.normalize();

        double c = Math.cos(maxAngle);
        double s = Math.sin(maxAngle);
        return from.scale(c).add(axis.cross(from).scale(s)).normalize();
    }

    private final LivingEntity target;
    private final Settings settings;
    private int homingAge;
    private boolean homing = true;

    public HomingDragonFireball(Level level, LivingEntity mob, LivingEntity target, Settings settings)
    {
        super(level, mob, Vec3.ZERO);
        this.target = target;
        this.settings = settings;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.level().isClientSide() || !this.homing || this.isRemoved())
        {
            return;
        }
        if (this.homingAge++ > this.settings.maxHomingTicks())
        {
            this.homing = false;
            return;
        }

        if (this.target == null || !this.target.isAlive())
        {
            this.homing = false;
            return;
        }
        if (this.target instanceof ServerPlayer serverPlayer)
        {
            if (serverPlayer.isSpectator() || serverPlayer.isCreative())
            {
                this.homing = false;
                return;
            }
        }

        Vec3 v = this.getDeltaMovement();
        Vec3 toTarget = aimPoint(target).subtract(this.position()).normalize();
        Vec3 current = v.lengthSqr() < 1.0E-6 ? toTarget : v.normalize();

        Vec3 newDir = this.homingAge < this.settings.armDelayTicks() ? current : rotateTowards(current, toTarget, Math.toRadians(this.settings.maxTurnDegrees()));

        // Override vanilla's acceleration step, so next tick's move is exactly 'speed'
        this.setDeltaMovement(newDir.scale(this.settings.speed()));

        // DRAGON_FIREBALL only syncs position every 10 ticks. Need to send velocity every tick
        // so the client's prediction curves with the server.
        this.syncVelocity = true;
    }

    public record Settings(double speed, double maxTurnDegrees, int armDelayTicks, int maxHomingTicks)
    {
        public static final Settings DEFAULT = new Settings(0.8, 4.0, 10, 100);
    }
}

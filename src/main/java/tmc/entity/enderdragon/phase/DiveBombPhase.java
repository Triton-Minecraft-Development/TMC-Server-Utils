package tmc.entity.enderdragon.phase;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.enderdragon.DiveBombData;
import tmc.entity.enderdragon.TMCEnderDragonManager;
import tmc.util.PlayerClusterScanner;

import java.util.Optional;

/**
 * Dive bomb attack for the Ender Dragon.
 * Uses a DBSCAN (Density-Based Spatial Cluster of Applications with Noise) to find the most condensed group
 * of players and dive in on them. If a grouping that satisfies the requirements for a group dive cannot be
 * found, she can dive in on an individual player.
 */
public final class DiveBombPhase extends AbstractDragonPhaseInstance {

    /**
     * Blocks above the target before diving
     */
    private static final double APEX_HEIGHT = 35.0;

    /**
     * Horizontal offset, so the dive comes in at an angle
     */
    private static final double APEX_BACKOFF = 25.0;

    /**
     * Speed in blocks/tick
     */
    private static final double ASCEND_SPEED = 1.4;

    /**
     * How quickly velocity bends towards the desired direction
     */
    private static final double ASCEND_AGILITY = 0.12;

    private static final int ASCEND_TIMEOUT = 140;
    private static final int ASCEND_RESCAN = 20;

    /**
     * Dodge window
     */
    private static final int TELEGRAPH_TICKS = 30;
    private static final int TELEGRAPH_RESCAN = 5;

    private static final double DIVE_START_SPEED = 0.6;
    private static final double DIVE_MAX_SPEED = 3.2;
    private static final double DIVE_ACCEL = 0.2;
    private static final int DIVE_TIMEOUT = 80;

    /**
     * Height just above the target to stop the dive. This is considered
     * complete when her underside is just above the target.
     */
    private static final double IMPACT_HEIGHT_OFFSET = 2.0;

    private static final double IMPACT_RADIUS = 8.0;

    /**
     * Impact damage from the center. Falls of linearly to 0 at the edge
     */
    private static final float IMPACT_DAMAGE = 20.0f;

    private static final double IMPACT_KNOCKBACK = 2.2;

    private static final int RECOVER_TICKS = 25;

    private static final int COOLDOWN_TICKS = 260;

    public DiveBombPhase(EnderDragon dragon)
    {
        super(dragon);
    }

    @Override
    public void begin()
    {
        this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA, divebombData ->
                // Carry momentum from previous phase
                divebombData.setStage(Stage.ASCEND).resetStageTicks().setTargetPos(null).setApexPos(null).setVelocity(this.dragon.getDeltaMovement())
        );
    }

    @Override
    public void doServerTick(ServerLevel level)
    {
        DiveBombData divebombData = this.dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA);
        if (divebombData.getTargetPos().isEmpty() && !this.retarget(level, divebombData))
        {
            this.abort();
            return;
        }

        this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA, DiveBombData::updateStageTicks);
        divebombData = this.dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA);
        switch (divebombData.getStage())
        {
            case ASCEND -> this.tickAscend(level, divebombData);
            case TELEGRAPH -> this.tickTelegraph(level, divebombData);
            case DIVE -> this.tickDive(level, divebombData);
            case RECOVER -> this.tickRecover(divebombData);
        }
    }

    @Override
    public @Nullable Vec3 getFlyTargetLocation()
    {
        return null;
    }

    @Override
    public EnderDragonPhase<? extends DragonPhaseInstance> getPhase()
    {
        return TMCEnderDragonManager.Phases.DIVE_BOMB_PHASE;
    }

    private void tickAscend(ServerLevel level, DiveBombData divebombData)
    {
        if (divebombData.getStageTicks() % ASCEND_RESCAN == 0)
        {
            this.retarget(level, divebombData);
            divebombData = this.dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA);
        }
        if (divebombData.getApexPos().isEmpty())
        {
            return;
        }
        this.steerToward(divebombData.getApexPos().get(), ASCEND_SPEED, ASCEND_AGILITY);
        this.applyMovement(0.15f);

        if (this.dragon.position().distanceToSqr(divebombData.getApexPos().get()) < 16.0 || divebombData.getStageTicks() >= ASCEND_TIMEOUT)
        {
            this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA, data -> data.setStage(Stage.TELEGRAPH));
            level.playSound(null, this.dragon.getX(), this.dragon.getY(), this.dragon.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 6.0f, 0.7f);
        }
    }

    private void tickTelegraph(ServerLevel level, DiveBombData divebombData)
    {
        // Keep tracking during the warning so the ring follows the group.
        // Point locks when the dive starts. This makes the attack dodgeable

        if (divebombData.getStageTicks() % TELEGRAPH_RESCAN == 0)
        {
            this.retarget(level, divebombData);
        }

        this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA, data -> data.setVelocity(data.getVelocity().scale(0.75)));
        divebombData = this.dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA);
        this.applyMovement(0.0f);

        if (divebombData.getTargetPos().isEmpty())
        {
            return;
        }

        this.faceToward(divebombData.getTargetPos().get().subtract(this.dragon.position()), 0.3f);

        this.drawWarningRing(level);

        if (divebombData.getStageTicks() >= TELEGRAPH_TICKS)
        {
            this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA, data ->
            {
                if (data.getTargetPos().isEmpty())
                {
                    return data;
                }
                return data.setVelocity(data.getTargetPos().get().subtract(this.dragon.position()).normalize().scale(DIVE_START_SPEED)).setStage(Stage.DIVE);
            });
            level.playSound(null, this.dragon.getX(), this.dragon.getY(), this.dragon.getZ(), SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 6.0f, 0.5f);
        }
    }

    private void tickDive(ServerLevel level, DiveBombData divebombData)
    {
        if (divebombData.getTargetPos().isEmpty())
        {
            return;
        }
        Vec3 aimPoint = divebombData.getTargetPos().get().add(0, IMPACT_HEIGHT_OFFSET, 0);
        Vec3 toAim = aimPoint.subtract(this.dragon.position());
        double dist = toAim.length();

        double speed = Math.min(DIVE_MAX_SPEED, divebombData.getVelocity().length() + DIVE_ACCEL);

        // Don't overshoot on last tick
        this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA, data -> data.setVelocity(toAim.scale(Math.min(speed, dist) / Math.max(dist, 1.0E-4))));

        this.applyMovement(0.5f);

        level.sendParticles(ParticleTypes.CLOUD, this.dragon.getX(), this.dragon.getY() + 2, this.dragon.getZ(), 4, 1.5, 1.0, 1.5, 0.02);

        if (dist <= speed + 0.5 || divebombData.getStageTicks() >= DIVE_TIMEOUT)
        {
            this.impact(level, divebombData);
            this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA, data ->
            {
                Vec3 velocity = data.getVelocity();
                return data.setVelocity(new Vec3(velocity.x * 0.2, 0.6, velocity.z * 0.2)).setStage(Stage.RECOVER);
            });
        }
    }

    private void tickRecover(DiveBombData divebombData)
    {
        Vec3 velocity = divebombData.getVelocity();
        Vec3 forward = new Vec3(velocity.x, 0, velocity.z);
        Vec3 desired = forward.lengthSqr() > 1.0E-4 ? forward.normalize().scale(0.6).add(0, 1, 0) : new Vec3(0, 1, 0);
        this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA, data -> data.setVelocity(velocity.lerp(desired, 0.1)));
        this.applyMovement(0.1f);

        if (divebombData.getStageTicks() >= RECOVER_TICKS)
        {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            Integer stage2Ticks = this.dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_STAGE_2_TIMER);
            this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA, data -> data.setCooldown(stage2Ticks + COOLDOWN_TICKS));
        }
    }

    private void impact(ServerLevel level, DiveBombData divebombData)
    {
        if (divebombData.getTargetPos().isEmpty())
        {
            return;
        }
        Vec3 targetPos = divebombData.getTargetPos().get();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, targetPos.x, targetPos.y + 1, targetPos.z, 2, 1.5, 0.5, 1.5, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, targetPos.x, targetPos.y + 1, targetPos.z, 200, IMPACT_RADIUS / 2, 0.5, IMPACT_RADIUS / 2, 0.15);
        level.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 6.0f, 0.6f);

        double r2 = IMPACT_RADIUS * IMPACT_RADIUS;
        for (ServerPlayer serverPlayer : level.getPlayers(player -> player.isAlive() && !player.isSpectator() && !player.isCreative() && player.position().distanceToSqr(targetPos) <= r2))
        {
            double d = serverPlayer.position().distanceTo(targetPos);
            double falloff = 1.0 - d / IMPACT_RADIUS;

            serverPlayer.hurtServer(level, level.damageSources().explosion(this.dragon, this.dragon), (float) (IMPACT_DAMAGE * falloff));

            Vec3 away = new Vec3(serverPlayer.getX() - targetPos.x, 0, serverPlayer.getZ() - targetPos.z);
            if (away.lengthSqr() <= 1.0E-4)
            {
                float a = level.getRandom().nextFloat() * Mth.TWO_PI;
                away = new Vec3(Mth.cos(a), 0, Mth.sin(a));
            }

            Vec3 push = away.normalize().scale(IMPACT_KNOCKBACK * falloff).add(0, 0.5 + 0.7 * falloff, 0);
            serverPlayer.setDeltaMovement(serverPlayer.getDeltaMovement().add(push));
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private boolean retarget(ServerLevel level, DiveBombData divebombData)
    {
        Optional<PlayerClusterScanner.DiveTarget> found = PlayerClusterScanner.findDiveTarget(level, PlayerClusterScanner.SCAN_RADIUS_ENDER_DRAGON, PlayerClusterScanner.EPS_ENDER_DRAGON_DIVE, PlayerClusterScanner.MIN_PTS_ENDER_DRAGON_DIVE, PlayerClusterScanner.FALLBACK_TO_SINGLE_PLAYER_ENDER_DRAGON_DIVE);
        if (found.isEmpty())
        {
            return divebombData.getTargetPos().isPresent(); // Keep old point if everyone briefly dropped out
        }

        this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA, data ->
        {
            data.setTargetPos(found.get().point());
            if (data.getStage() == Stage.ASCEND)
            {
                data.setApexPos(this.computeApex(data.getTargetPos().get())); // target pos was just set; this is safe
            }
            return data;
        });
        return true;
    }

    private Vec3 computeApex(Vec3 targetPos)
    {
        Vec3 back = new Vec3(this.dragon.getX() - targetPos.x, 0, this.dragon.getZ() - targetPos.z);
        back = back.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : back.normalize();
        return targetPos.add(back.scale(APEX_BACKOFF)).add(0, APEX_HEIGHT, 0);
    }

    private void steerToward(Vec3 point, double maxSpeed, double agility)
    {
        Vec3 desired = point.subtract(this.dragon.position());
        double dist = desired.length();
        if (dist > 1.0E-4)
        {
            desired = desired.scale(Math.min(maxSpeed, dist * 0.25) / dist);
        }
        final Vec3 d = desired;
        this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA, data -> data.setVelocity(data.getVelocity().lerp(d, agility)));
    }

    private void applyMovement(float turnLerp)
    {
        DiveBombData divebombData = this.dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA);
        this.dragon.setDeltaMovement(divebombData.getVelocity());
        this.dragon.move(MoverType.SELF, divebombData.getVelocity());
        if (turnLerp > 0)
        {
            this.faceToward(divebombData.getVelocity(), turnLerp);
        }
    }

    private void faceToward(Vec3 dir, float lerp)
    {
        if (dir.horizontalDistanceSqr() < 1.0E-4)
        {
            return;
        }
        @SuppressWarnings("SuspiciousNameCombination")
        float desiredYaw = 180.0f - (float) Mth.atan2(dir.x, dir.z) * Mth.DEG_TO_RAD;
        this.dragon.setYRot(Mth.rotLerp(lerp, this.dragon.getYRot(), desiredYaw));
    }

    private void drawWarningRing(ServerLevel level)
    {
        DiveBombData divebombData = this.dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA);
        if (divebombData.getTargetPos().isEmpty())
        {
            return;
        }
        Vec3 targetPos = divebombData.getTargetPos().get();
        int points = 40;
        double spin = divebombData.getStageTicks() * 0.08;
        for (int i = 0; i < points; i++)
        {
            double a = Mth.TWO_PI * i / points + spin;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, targetPos.x + Math.cos(a) * IMPACT_RADIUS, targetPos.y + 0.2, targetPos.z + Math.sin(a) * IMPACT_RADIUS, 1, 0, 0, 0, 0);
        }

        // A shrinking inner ring shows remaining warning time
        double inner = IMPACT_RADIUS * (1.0 - (double) divebombData.getStageTicks() / TELEGRAPH_TICKS);
        for (int i = 0; i < 16; i++)
        {
            double a = Mth.TWO_PI * i / 16;
            level.sendParticles(ParticleTypes.END_ROD, targetPos.x + Math.cos(a) * inner, targetPos.y + 0.2, targetPos.z + Math.sin(a) * inner, 1, 0, 0, 0, 0);
        }
    }

    private void abort()
    {
        this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
    }

    public enum Stage
    {
        ASCEND, TELEGRAPH, DIVE, RECOVER;
    }
}

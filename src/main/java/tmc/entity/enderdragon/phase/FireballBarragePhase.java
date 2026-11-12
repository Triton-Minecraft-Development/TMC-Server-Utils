package tmc.entity.enderdragon.phase;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import tmc.TMCServerUtils;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.enderdragon.DialogueCooldownData;
import tmc.codec.enderdragon.FireballBarrageData;
import tmc.entity.enderdragon.EnderDragonStage;
import tmc.entity.enderdragon.TMCEnderDragonManager;
import tmc.entity.enderdragon.TMCEnderDragonRenderer;
import tmc.entity.fireball.BlindnessFireball;
import tmc.entity.fireball.ChorusFireball;
import tmc.entity.fireball.HomingDragonFireball;
import tmc.util.PlayerClusterScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class FireballBarragePhase extends AbstractDragonPhaseInstance {

    private static final double MIN_HORIZONTAL = 18;
    private static final double MAX_HORIZONTAL = 70;

    private static final double MIN_HEIGHT = 8;
    private static final double MAX_HEIGHT = 45;
    private static final double FIRE_DISTANCE = 38;
    private static final double FIRE_HEIGHT = 22;
    private static final double APPROACH_SPEED = 1.3;
    private static final double APPROACH_AGILITY = 0.12;
    private static final int APPROACH_TIMEOUT = 100;
    private static final int APPROACH_RESCAN = 20;

    private static final double BRAKE = 0.8;
    private static final double BOB_AMPLITUDE = 0.04;

    private static final int WINDUP_TICKS = 15;
    private static final int FIREBALL_COUNT = 12;
    private static final int FIRE_INTERVAL = 4;
    private static final int LINGER_TICKS = 20;

    private static final int BARRAGE_RESCAN = 10;

    private static final double BASE_SPREAD = 9.0;
    private static final double SPREAD_PER_CLUSTER_SPREAD = 1.5;
    private static final double MAX_SPREAD = 16;
    private static final double JITTER = 0.8;

    private static final double MAX_GROUND_DIFF = 10;
    private static final double MUZZLE_FORWARD = 1.5;

    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    private static final int COOLDOWN_TICKS = 260;

    private static List<Vec3> buildPattern(RandomSource randomSource)
    {
        List<Vec3> pattern = new ArrayList<>(FIREBALL_COUNT);
        pattern.add(Vec3.ZERO);

        int rest = FIREBALL_COUNT - 1;
        double rotation = randomSource.nextDouble() * Mth.TWO_PI;
        for (int i = 0; i < rest; i++)
        {
            double r = Math.sqrt((i * 0.5) / rest);
            double a = rotation + i * GOLDEN_ANGLE;
            pattern.add(new Vec3(Math.cos(a) * r, 0, Math.sin(a) * r));
        }

        for (int i = FIREBALL_COUNT - 1; i > 1; i--)
        {
            int j = 1 + randomSource.nextInt(i);
            Vec3 tmp = pattern.get(i);
            pattern.set(i, pattern.get(j));
            pattern.set(j, tmp);
        }
        return pattern;
    }

    private static double groundY(ServerLevel level, Vec3 center, double x, double z)
    {
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
        if (ground <= level.getMinY() || Math.abs(ground - center.y) > MAX_GROUND_DIFF)
        {
            return center.y;
        }
        return ground;
    }

    public static Vec3 getMuzzlePos(EnderDragon dragon)
    {
        Vec3 headCenter = TMCEnderDragonRenderer.getWarningHeadPos(dragon, -dragon.head.getBbHeight());

        float yawRad = (float) Math.toRadians(dragon.flightHistory.get(3).yRot());
        Vec3 forward = new Vec3(Math.sin(yawRad), 0.0, -Math.cos(yawRad));

        return headCenter.add(forward.scale(MUZZLE_FORWARD));
    }

    public FireballBarragePhase(EnderDragon dragon)
    {
        super(dragon);
    }

    @Override
    public void begin()
    {
        FireballBarrageData fireballBarrageData = this.getData();
        if (!fireballBarrageData.isActive())
        {
            fireballBarrageData.start(this.dragon.getDeltaMovement(), buildPattern(this.dragon.getRandom()));
        }

        Level level = this.dragon.level();
        if (level.isClientSide())
        {
            return;
        }
        if (level instanceof ServerLevel serverLevel)
        {
            DialogueCooldownData dialogueCooldownData = this.dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_COOLDOWN_DATA);
            int ticks = TMCEnderDragonManager.getStageTimerTicks(this.dragon, EnderDragonStage.STAGE_2);
            if (!dialogueCooldownData.isAttackReady(ticks))
            {
                return;
            }
            Random random = new Random();
            String dialogue = TMCEnderDragonManager.DIALOGUE_FIREBALL_BARRAGE[random.nextInt(TMCEnderDragonManager.DIALOGUE_FIREBALL_BARRAGE.length)];
            TMCServerUtils.messageAllPlayers(serverLevel, player -> TMCServerUtils.isPlayerInDimension(player, ServerLevel.END),
                    TMCEnderDragonManager.createEnderDragonDialogueComponent(dialogue));

            this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_COOLDOWN_DATA, data -> data.setAttackCooldown(ticks + DialogueCooldownData.ATTACK_COOLDOWN_TICKS));
        }
    }

    @Override
    public void end()
    {
        this.getData().setActive(false);
    }

    @Override
    public void doServerTick(ServerLevel level)
    {
        FireballBarrageData data = this.getData();

        if (data.getCenterPos().isEmpty() && !this.retarget(level, data))
        {
            this.abort();
            return;
        }

        data.updateTicks();
        switch (data.getStage())
        {
            case APPROACH -> this.tickApproach(level, data);
            case WINDUP -> this.tickWindup(level, data);
            case BARRAGE -> this.tickBarrage(level, data);
            case LINGER -> this.tickLinger(data);
        }
    }

    @Override
    @Nullable
    public Vec3 getFlyTargetLocation()
    {
        return null;
    }

    @Override
    public EnderDragonPhase<? extends DragonPhaseInstance> getPhase()
    {
        return TMCEnderDragonManager.Phases.FIREBALL_BARRAGE_PHASE;
    }

    private FireballBarrageData getData()
    {
        return this.dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_FIREBALL_BARRAGE_DATA);
    }

    private void tickApproach(ServerLevel level, FireballBarrageData data)
    {
        if (data.getStageTicks() % APPROACH_RESCAN == 0)
        {
            this.retarget(level, data);
        }
        Vec3 center = data.getCenterPos().orElseThrow();

        if (this.inFiringBand(center) || data.getStageTicks() >= APPROACH_TIMEOUT)
        {
            data.setStage(Stage.WINDUP);
            level.playSound(null, this.dragon.getX(), this.dragon.getY(), this.dragon.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 6.0f, 1.1f);
            return;
        }

        this.steerToward(data, this.firingSpot(center), APPROACH_SPEED, APPROACH_AGILITY);
        this.applyMovement(data);
        this.faceToward(data.getVelocity(), 0.15f);
    }

    private void tickWindup(ServerLevel level, FireballBarrageData data)
    {
        Vec3 center = data.getCenterPos().orElseThrow();
        this.hover(data);
        this.faceToward(center.subtract(this.dragon.position()), 0.25f);

        level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.dragon.head.getX(), this.dragon.head.getY(0.5), this.dragon.head.getZ(), 6, 0.4, 0.4, 0.4, 0.05);

        if (data.getStageTicks() >= WINDUP_TICKS)
        {
            data.setSpreadRadius(Math.min(MAX_SPREAD, BASE_SPREAD + data.getClusterSpread() * SPREAD_PER_CLUSTER_SPREAD))
                    .resetShotsFired()
                    .setStage(Stage.BARRAGE);
        }
    }

    private void tickBarrage(ServerLevel level, FireballBarrageData data)
    {
        if (data.getStageTicks() % BARRAGE_RESCAN == 0)
        {
            this.retarget(level, data);
        }
        Vec3 center = data.getCenterPos().orElseThrow();

        this.hover(data);
        this.faceToward(center.subtract(this.dragon.position()), 0.25f);

        if ((data.getStageTicks() - 1) % FIRE_INTERVAL == 0)
        {
            this.fire(level, data, center);
            data.incrementShotsFired();
        }

        if (data.getShotsFired() >= FIREBALL_COUNT)
        {
            data.setStage(Stage.LINGER);
        }
    }

    private void tickLinger(FireballBarrageData data)
    {
        this.hover(data);
        if (data.getStageTicks() >= LINGER_TICKS)
        {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            Integer stage2Ticks = this.dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_STAGE_2_TIMER);
            this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_FIREBALL_BARRAGE_DATA, fireballBarrageData -> fireballBarrageData.setCooldown(stage2Ticks + COOLDOWN_TICKS));
        }
    }

    private void fire(ServerLevel serverLevel, FireballBarrageData data, Vec3 center)
    {
        List<Vec3> pattern = data.getPattern();
        int shot = data.getShotsFired();
        if (shot >= pattern.size())
        {
            return;
        }

        RandomSource randomSource = this.dragon.getRandom();
        Vec3 offset = pattern.get(shot);
        double radius = data.getSpreadRadius();

        double x = center.x + offset.x * radius + (randomSource.nextDouble() * 2 - 1) * JITTER;
        double z = center.z + offset.z * radius + (randomSource.nextDouble() * 2 - 1) * JITTER;
        Vec3 aim = new Vec3(x, groundY(serverLevel, center, x, z), z);

        Vec3 muzzle = getMuzzlePos(this.dragon);
        Vec3 dir = aim.subtract(muzzle);
        if (dir.lengthSqr() < 1.0E-4)
        {
            return;
        }
        dir = dir.normalize();
        // Fire off random fireballs from choices:
        // - Normal Dragon fireball
        // - Blindness fireball
        // - Chorus fireball
        // - Homing fireball
        Random random = new Random();
        int choice = random.nextInt(4);
        switch (choice)
        {
            case 0 -> {
                this.launchNormalFireball(serverLevel, muzzle, dir);
            }
            case 1 -> {
                BlindnessFireball.launch(serverLevel, this.dragon, dir);
            }
            case 2 -> {
                ChorusFireball.launch(serverLevel, this.dragon, dir);
            }
            case 3 -> {
                ServerPlayer nearestPlayer = (ServerPlayer) serverLevel.getNearestPlayer(this.dragon, PlayerClusterScanner.SCAN_RADIUS_ENDER_DRAGON);
                if (nearestPlayer == null)
                {
                    this.launchNormalFireball(serverLevel, muzzle, dir);
                    return;
                }
                HomingDragonFireball.launch(serverLevel, this.dragon, nearestPlayer, HomingDragonFireball.Settings.DEFAULT);
            }
        }
    }

    private void launchNormalFireball(ServerLevel level, Vec3 muzzle, Vec3 dir)
    {
        DragonFireball fireball = new DragonFireball(level, this.dragon, dir);
        fireball.snapTo(muzzle, 0.0f, 0.0f);
        level.addFreshEntity(fireball);
        if (!this.dragon.isSilent())
        {
            level.levelEvent(null, LevelEvent.SOUND_DRAGON_FIREBALL, dragon.blockPosition(), 0);
        }
    }

    private boolean retarget(ServerLevel level, FireballBarrageData data)
    {
        Optional<PlayerClusterScanner.DiveTarget> found = PlayerClusterScanner.findDiveTarget(level, PlayerClusterScanner.SCAN_RADIUS_ENDER_DRAGON, PlayerClusterScanner.EPS_ENDER_DRAGON_DIVE, PlayerClusterScanner.MIN_PTS_ENDER_DRAGON_DIVE, PlayerClusterScanner.FALLBACK_TO_SINGLE_PLAYER_ENDER_DRAGON_DIVE);
        if (found.isEmpty())
        {
            return data.getCenterPos().isPresent();
        }

        data.setCenterPos(found.get().point()).setClusterSpread(found.get().spread());
        return true;
    }

    private boolean inFiringBand(Vec3 center)
    {
        double dx = this.dragon.getX() - center.x;
        double dz = this.dragon.getZ() - center.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double height = this.dragon.getY() - center.y;
        return horizontal >= MIN_HORIZONTAL && horizontal <= MAX_HORIZONTAL
                && height >= MIN_HEIGHT && height <= MAX_HEIGHT;
    }

    private Vec3 firingSpot(Vec3 center)
    {
        Vec3 back = new Vec3(this.dragon.getX() - center.x, 0, this.dragon.getZ() - center.z);
        back = back.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : back.normalize();
        return center.add(back.scale(FIRE_DISTANCE)).add(0, FIRE_HEIGHT, 0);
    }

    private void hover(FireballBarrageData data)
    {
        Vec3 v = data.getVelocity();
        double bob = Math.cos(data.getTotalTicks() * 0.15) * BOB_AMPLITUDE;
        data.setVelocity(new Vec3(v.x *BRAKE, v.y * BRAKE + bob, v.z * BRAKE));
        this.applyMovement(data);
    }

    private void steerToward(FireballBarrageData data, Vec3 point, double maxSpeed, double agility)
    {
        Vec3 desired = point.subtract(this.dragon.position());
        double dist = desired.length();
        if (dist > 1.0E-4)
        {
            desired = desired.scale(Math.min(maxSpeed, dist * 0.25) / dist);
        }
        data.setVelocity(data.getVelocity().lerp(desired, agility));
    }

    private void applyMovement(FireballBarrageData data)
    {
        Vec3 v = data.getVelocity();
        this.dragon.setDeltaMovement(v);
        this.dragon.move(MoverType.SELF, v);
    }

    private void faceToward(Vec3 dir, float lerp)
    {
        if (dir.horizontalDistanceSqr() < 1.0E-4)
        {
            return;
        }

        @SuppressWarnings("SuspiciousNameCombination")
        float desiredYaw = 180.0f - (float) Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG;
        this.dragon.setYRot(Mth.rotLerp(lerp, this.dragon.getYRot(), desiredYaw));
    }

    private void abort()
    {
        this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
    }

    public enum Stage
    {
        APPROACH, WINDUP, BARRAGE, LINGER;
    }
}

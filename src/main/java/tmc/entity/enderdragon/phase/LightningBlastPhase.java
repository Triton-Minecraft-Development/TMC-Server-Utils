package tmc.entity.enderdragon.phase;

import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tmc.TMCServerUtils;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.enderdragon.DialogueCooldownData;
import tmc.codec.enderdragon.LightningBlastData;
import tmc.damagetypes.TMCDamageTypes;
import tmc.entity.enderdragon.EnderDragonStage;
import tmc.entity.enderdragon.TMCEnderDragonManager;
import tmc.entity.enderdragon.WarningManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LightningBlastPhase extends AbstractDragonPhaseInstance {

    public static final String MARKER_TAG = "tmc_lightning_marker";

    private static final BlockState MARKER_BLOCK = Blocks.END_GATEWAY.defaultBlockState();
    private static final float MARKER_THICKNESS = 0.05F;
    private static final double MARKER_LIFT = 0.01;
    private static final double MARKER_STACK = 0.002;
    private static final float MARKER_VIEW_RANGE = 4.0F;


    private static final double PERCH_HEIGHT = 40.0;
    private static final double PERCH_TOLERANCE = 6.0;
    private static final double APPROACH_SPEED = 1.3;
    private static final double APPROACH_AGILITY = 0.12;
    private static final int APPROACH_TIMEOUT = 120;

    private static final double BRAKE = 0.8;
    private static final double BOB_AMPLITUDE = 0.04;

    public static final int WINDUP_TICKS = 20;
    public static final int CHARGE_TICKS = 60;
    public static final int LINGER_TICKS = 20;
    private static final int SPARK_INTERVAL = 4;
    public static final int COOLDOWN_TICKS = 600;

    // ---- Zones ----
    private static final int ZONE_SIZE = 5;
    private static final int ZONE_GAP = 1;
    public static final double TARGET_RANGE = 200;
    private static final int FILLER_PER_PLAYER = 10;
    private static final int FILLER_SPREAD = 12;
    private static final int FILLER_ATTEMPTS = 12;
    private static final int MAX_ZONES = 96;
    private static final double MAX_GROUND_DIFF = 10.0;

    /// Starts this far above the player, so raised ground still counts
    private static final int FLOOR_SEARCH_UP = 4;

    /// A player further up than this gets no zone
    private static final int FLOOR_SEARCH_DOWN = 48;
    private static final double SUPPORT_TOLERANCE = 0.5;
    private static final double MIN_PLAYER_ZONE_SUPPORT = 0.3;
    private static final double MIN_FILLER_ZONE_SUPPORT = 0.8;

    private static final float LIGHTNING_DAMAGE_MAX_HEALTH = 0.5F;
    private static final double SHARED_BOLT_RANGE = 2.0;
    private static final int IMPACT_SOUND_LIMIT = 6;

    @Nullable
    private static LightningBolt nearestBolt(List<LightningBolt> bolts, Vec3 pos)
    {
        for (LightningBolt bolt : bolts)
        {
            if (bolt.position().distanceToSqr(pos) <= SHARED_BOLT_RANGE * SHARED_BOLT_RANGE) return bolt;
        }
        return null;
    }

    @Nullable
    private static LightningBolt spawnBolt(ServerLevel level, Vec3 pos)
    {
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null)
        {
            return null;
        }
        bolt.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
        return bolt;
    }

    private static boolean tooClose(List<Vec3> zones, int x, int z)
    {
        int min = ZONE_SIZE + ZONE_GAP;
        for (Vec3 c : zones)
        {
            if (Math.abs(c.x() - x) < min && Math.abs(c.z() - z) < min) return true;
        }
        return false;
    }

//    /** Top of the ground at the zone's centre, or the player's own height over the void / pillars / roofs. */
//    private static int groundY(ServerLevel level, int x, int z, double referenceY)
//    {
//        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
//        if (ground <= level.getMinY() || Math.abs(ground - referenceY) > MAX_GROUND_DIFF) return Mth.floor(referenceY);
//        return ground;
//    }

    private static Vec3 zonesCentroid(List<Vec3> zones)
    {
        double x = 0, y = 0, z = 0;
        for (Vec3 c : zones)
        {
            x += c.x() + ZONE_SIZE * 0.5;
            y += c.y();
            z += c.z() + ZONE_SIZE * 0.5;
        }
        int n = Math.max(1, zones.size());
        return new Vec3(x / n, y / n, z / n);
    }

    private static boolean isValidTarget(ServerPlayer player)
    {
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    private static <T> void shuffle(List<T> list, RandomSource random)
    {
        for (int i = list.size() - 1; i > 0; i--)
        {
            int j = random.nextInt(i + 1);
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }

    /**
     * Finds the floor under a whole zone, or null if there isn't enough of one.
     * <p>
     * Every one of the ZONE_SIZE x ZONE_SIZE columns is scanned for its own floor. The zone then sits
     * at the height most of its columns share, and is rejected if too few columns are at that height
     * (void, an island edge, a pillar side, a big crater, unloaded chunks). It never falls back to
     * the player's own Y, which is what used to leave markers floating under flying or falling players.
     *
     * @return the zone's min corner, with Y = the exact floor surface (slab and snow-layer tops included)
     */
    @Nullable
    private static Vec3 anchorZone(ServerLevel level, int minX, int minZ, double referenceY, double minSupport)
    {
        int columns = ZONE_SIZE * ZONE_SIZE;
        double[] floors = new double[columns];
        int found = 0;
        for (int dx = 0; dx < ZONE_SIZE; dx++)
        {
            for (int dz = 0; dz < ZONE_SIZE; dz++)
            {
                double floor = floorAt(level, minX + dx, minZ + dz, referenceY);
                if (!Double.isNaN(floor))
                {
                    floors[found++] = floor;
                }
            }
        }

        if (found == 0)
        {
            return null;
        }

        double best = Double.NaN;
        int bestCount = 0;
        for (int i = 0; i < found; i++)
        {
            int count = 0;
            for (int j = 0; j < found; j++)
            {
                if (Math.abs(floors[j] - floors[i]) <= SUPPORT_TOLERANCE)
                {
                    count++;
                }
            }
            if (count > bestCount || (count == bestCount && Math.abs(floors[i] - referenceY) < Math.abs(best - referenceY)))
            {
                best = floors[i];
                bestCount = count;
            }
        }

        if ((double) bestCount / columns < minSupport)
        {
            return null;
        }
        return new Vec3(minX, best, minZ);
    }

    private static double floorAt(ServerLevel level, int x, int z, double referenceY)
    {
        int top = Math.min(Mth.floor(referenceY) + FLOOR_SEARCH_UP, level.getMaxY());
        int bottom = Math.min(Mth.floor(referenceY) - FLOOR_SEARCH_DOWN, level.getMinY());

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
        if (!level.isLoaded(pos))
        {
            return Double.NaN; // Don't force-load chunks just to place a marker
        }

        boolean openAbove = false;
        for (int y = top; y >= bottom; y--)
        {
            pos.setY(y);
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (shape.isEmpty())
            {
                openAbove = true;
            } else if (openAbove)
            {
                return y + Math.min(1.0, shape.max(Direction.Axis.Y)); // Fences, etc. count as a full block
            }
        }

        return Double.NaN;
    }

    // Not saved: rebuilt from the zones in the attachment if they go missing (reload, chunk unload)
    private final List<Display.BlockDisplay> markers = new ArrayList<>();

    public LightningBlastPhase(EnderDragon dragon)
    {
        super(dragon);
    }

    private LightningBlastData getData()
    {
        return this.dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_LIGHTNING_BLAST_DATA);
    }

    /** Same reload-safe pattern as the barrage: only start fresh if an attack isn't already in progress. */
    @Override
    public void begin()
    {
        LightningBlastData lightningBlastData = this.getData();
        if (!lightningBlastData.isActive())
        {
            lightningBlastData.start(this.dragon.getDeltaMovement());
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
            String dialogue = TMCEnderDragonManager.DIALOGUE_LIGHTNING_PHASE[random.nextInt(TMCEnderDragonManager.DIALOGUE_LIGHTNING_PHASE.length)];
            TMCServerUtils.messageAllPlayers(serverLevel, player -> TMCServerUtils.isPlayerInDimension(player, ServerLevel.END),
                    TMCEnderDragonManager.createEnderDragonDialogueComponent(dialogue));

            this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_COOLDOWN_DATA, data -> data.setAttackCooldown(ticks + DialogueCooldownData.ATTACK_COOLDOWN_TICKS));
        }
    }

    @Override
    public void end()
    {
        this.getData().setActive(false);
        this.removeMarkers();
    }

    @Override
    public void doServerTick(ServerLevel level)
    {
        LightningBlastData data = this.getData();

        // begin() has no level, so the perch point is worked out here
        if (data.getPerchPos().isEmpty())
        {
            data.setPerchPos(this.computePerch(level));
        }

        data.updateTicks();
        int totalTicks = data.getTotalTicks();
        level.getPlayers(p -> p.distanceTo(this.dragon) <= TARGET_RANGE).forEach(p ->
        {
            WarningManager.warnPlayerLightningBlast(p, totalTicks, p.distanceTo(this.dragon));
        });
        switch (data.getStage())
        {
            case APPROACH -> tickApproach(level, data);
            case WINDUP -> tickWindup(level, data);
            case CHARGE -> tickCharge(level, data);
            case LINGER -> tickLinger(data);
        }
    }

    @Override
    @Nullable
    public Vec3 getFlyTargetLocation()
    {
        return null; // we move her ourselves; null makes aiStep skip vanilla steering
    }

    @Override
    public EnderDragonPhase<LightningBlastPhase> getPhase()
    {
        return TMCEnderDragonManager.Phases.LIGHTNING_BLAST_PHASE;
    }

    private void tickApproach(ServerLevel level, LightningBlastData data)
    {
        Vec3 perch = data.getPerchPos().orElseThrow();

        if (dragon.position().distanceToSqr(perch) <= PERCH_TOLERANCE * PERCH_TOLERANCE
                || data.getStageTicks() >= APPROACH_TIMEOUT)
        {
            data.setStage(Stage.WINDUP);
            level.playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(),
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 6.0F, 0.8F);
            return;
        }

        this.steerToward(data, perch, APPROACH_SPEED, APPROACH_AGILITY);
        this.applyMovement(data);
        this.faceToward(data.getVelocity(), 0.15F);
    }

    private void tickWindup(ServerLevel level, LightningBlastData data)
    {
        this.hover(data);

        // Static building up around her head
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                dragon.head.getX(), dragon.head.getY(0.5), dragon.head.getZ(),
                8, 0.8, 0.8, 0.8, 0.1);

        if (data.getStageTicks() >= WINDUP_TICKS)
        {
            List<Vec3> zones = this.buildZones(level);
            if (zones.isEmpty())
            {
                this.abort(); // nobody in range
                return;
            }
            data.setZones(zones).setStage(Stage.CHARGE);
            this.spawnMarkers(level, zones);
        }
    }

    private void tickCharge(ServerLevel level, LightningBlastData data)
    {
        this.hover(data);
        List<Vec3> zones = data.getZones();
        this.ensureMarkers(level, zones);
        this.faceToward(zonesCentroid(zones).subtract(dragon.position()), 0.2F);

        if (data.getStageTicks() % SPARK_INTERVAL == 0)
        {
            RandomSource random = this.dragon.getRandom();
            for (Vec3 corner : zones)
            {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        corner.x() + random.nextDouble() * ZONE_SIZE, corner.y() + 0.2,
                        corner.z() + random.nextDouble() * ZONE_SIZE,
                        2, 0.1, 0.1, 0.1, 0.05);
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    dragon.head.getX(), dragon.head.getY(0.5), dragon.head.getZ(),
                    4, 0.8, 0.8, 0.8, 0.1);
        }

        if (data.getStageTicks() >= CHARGE_TICKS)
        {
            this.strike(level, data);
            this.removeMarkers();
            data.setStage(Stage.LINGER);
        }
    }

    private void tickLinger(LightningBlastData data)
    {
        this.hover(data);
        if (data.getStageTicks() >= LINGER_TICKS)
        {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            Integer stage2Ticks = this.dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_STAGE_2_TIMER);
            this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_LIGHTNING_BLAST_DATA, lightningBlastData -> lightningBlastData.setCooldown(stage2Ticks + COOLDOWN_TICKS).resetTotalTicks());
        }
    }

    private void strike(ServerLevel level, LightningBlastData data)
    {
        List<Vec3> zones = data.getZones();
        boolean[] occupied = new boolean[zones.size()];
        List<LightningBolt> bolts = new ArrayList<>();

        for (ServerPlayer player : level.getPlayers(LightningBlastPhase::isValidTarget))
        {
            int zone = zoneIndexOf(zones, player.getX(), player.getZ());
            if (zone < 0)
            {
                continue;
            }
            occupied[zone] = true;

            LightningBolt bolt = nearestBolt(bolts, player.position());
            if (bolt == null)
            {
                bolt = spawnBolt(level, player.position());
                if (bolt == null)
                {
                    continue;
                }
                bolts.add(bolt);
            }

            player.invulnerableTime = 0;
            float damage = player.getMaxHealth() * LIGHTNING_DAMAGE_MAX_HEALTH;
            player.hurtServer(level, TMCDamageTypes.getDamageSource(TMCDamageTypes.ENDER_DRAGON_MAGIC, level, player, this.dragon), damage);
            player.invulnerableTime = 0;
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100));
        }

        // Empty zones: sparks everywhere, an impact crack at a few of them
        RandomSource random = this.dragon.getRandom();
        List<Vec3> empty = new ArrayList<>();
        for (int i = 0; i < zones.size(); i++)
        {
            if (!occupied[i])
            {
                empty.add(zones.get(i));
            }
        }
        for (Vec3 corner : empty)
        {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    corner.x() + ZONE_SIZE * 0.5, corner.y() + 0.5, corner.z() + ZONE_SIZE * 0.5,
                    20, ZONE_SIZE * 0.3, 0.3, ZONE_SIZE * 0.3, 0.3);
        }
        for (int i = 0; i < Math.min(IMPACT_SOUND_LIMIT, empty.size()); i++)
        {
            Vec3 corner = empty.get(random.nextInt(empty.size()));
            level.playSound(null, corner.x() + ZONE_SIZE * 0.5, corner.y(), corner.z() + ZONE_SIZE * 0.5,
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 2.0F, 0.5F + random.nextFloat() * 0.2F);
        }

        // A real bolt plays its own thunder on every client. If none spawned, play one for everyone:
        // volume 10000 is what vanilla uses, and it carries across the whole dimension.
        if (bolts.isEmpty())
        {
            Vec3 perch = data.getPerchPos().orElse(this.dragon.position());
            level.playSound(null, perch.x, perch.y, perch.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 10000.0F, 0.8F + random.nextFloat() * 0.2F);
        }
    }

    /**
     * One zone centred under every player in range (skipped if another player's zone already
     * covers them), then FILLER_PER_PLAYER filler zones scattered around each player. Filler zones
     * always leave a ZONE_GAP lane to every other zone, so there's a safe way out.
     */
    private List<Vec3> buildZones(ServerLevel level)
    {
        RandomSource random = this.dragon.getRandom();
        BlockPos origin = this.dragon.getFightOrigin();
        List<ServerPlayer> targets = new ArrayList<>(level.getPlayers(p -> isValidTarget(p)
                && Mth.square(p.getX() - origin.getX()) + Mth.square(p.getZ() - origin.getZ()) <= TARGET_RANGE * TARGET_RANGE));
        shuffle(targets, random); // so MAX_ZONES doesn't always favor the same players

        List<Vec3> zones = new ArrayList<>();
        int half = ZONE_SIZE / 2;

        for (ServerPlayer p : targets)
        {
            if (zones.size() >= MAX_ZONES) return zones;
            if (zoneIndexOf(zones, p.getX(), p.getZ()) >= 0)
            {
                continue;
            }
            Vec3 zone = anchorZone(level, p.getBlockX() - half, p.getBlockZ() - half, p.getY(), MIN_PLAYER_ZONE_SUPPORT);
            if (zone == null)
            {
                continue;
            }
            zones.add(zone);
//            int x = p.getBlockX() - half;
//            int z = p.getBlockZ() - half;
//            zones.add(new BlockPos(x, groundY(level, x + half, z + half, p.getY()), z));
        }

        for (ServerPlayer p : targets)
        {
            for (int n = 0; n < FILLER_PER_PLAYER; n++)
            {
                if (zones.size() >= MAX_ZONES)
                {
                    return zones;
                }
                for (int attempt = 0; attempt < FILLER_ATTEMPTS; attempt++)
                {
                    int x = p.getBlockX() - half + random.nextIntBetweenInclusive(-FILLER_SPREAD, FILLER_SPREAD);
                    int z = p.getBlockZ() - half + random.nextIntBetweenInclusive(-FILLER_SPREAD, FILLER_SPREAD);
                    if (tooClose(zones, x, z))
                    {
                        continue;
                    }
                    Vec3 zone = anchorZone(level, x, z, p.getY(), MIN_FILLER_ZONE_SUPPORT);
                    if (zone == null)
                    {
                        continue;
                    }
                    zones.add(zone);
//                    zones.add(new BlockPos(x, groundY(level, x + half, z + half, p.getY()), z));
                    break;
                }
            }
        }
        return zones;
    }

    /** Index of a zone whose X/Z square contains the point, or -1. Height is ignored on purpose. */
    private static int zoneIndexOf(List<Vec3> zones, double x, double z)
    {
        for (int i = 0; i < zones.size(); i++)
        {
            Vec3 c = zones.get(i);
            if (x >= c.x() && x < c.x() + ZONE_SIZE && z >= c.z() && z < c.z() + ZONE_SIZE)
            {
                return i;
            }
        }
        return -1;
    }

    private void spawnMarkers(ServerLevel level, List<Vec3> zones)
    {
        removeMarkers();
        for (int i = 0; i < zones.size(); i++)
        {
            Vec3 corner = zones.get(i);
            Display.BlockDisplay marker = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
            if (marker == null)
            {
                continue;
            }

            // A block display draws its block from (0,0,0) to (1,1,1) of its position, so placing it at
            // the zone's corner and scaling X/Z by ZONE_SIZE covers the zone exactly.
            marker.setBlockState(MARKER_BLOCK);
            marker.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(ZONE_SIZE, MARKER_THICKNESS, ZONE_SIZE), new Quaternionf()));
            marker.setBrightnessOverride(Brightness.FULL_BRIGHT);
            marker.setViewRange(MARKER_VIEW_RANGE);
            marker.addTag(MARKER_TAG);
            marker.snapTo(corner.x(), corner.y() + MARKER_LIFT + i * MARKER_STACK, corner.z(), 0.0F, 0.0F);
            marker.setGlowingTag(true);
            marker.setGlowColorOverride(TextColor.LIGHT_PURPLE.getValue());

            level.addFreshEntity(marker);
            this.markers.add(marker);
        }
    }

    /** Respawns the markers if any went missing (world reload mid-charge, chunk unload). */
    private void ensureMarkers(ServerLevel level, List<Vec3> zones)
    {
        boolean missing = this.markers.size() != zones.size();
        for (Display.BlockDisplay marker : this.markers)
        {
            if (marker.isRemoved())
            {
                missing = true;
            }
        }
        if (missing) spawnMarkers(level, zones);
    }

    private void removeMarkers()
    {
        for (Display.BlockDisplay marker : this.markers)
        {
            marker.discard();
        }
        this.markers.clear();
    }

    private Vec3 computePerch(ServerLevel level)
    {
        BlockPos podium = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.dragon.getFightOrigin());
        return Vec3.atBottomCenterOf(podium).add(0.0, PERCH_HEIGHT, 0.0);
    }

    private void hover(LightningBlastData data)
    {
        Vec3 v = data.getVelocity();
        double bob = Math.cos(data.getTotalTicks() * 0.15) * BOB_AMPLITUDE;
        data.setVelocity(new Vec3(v.x * BRAKE, v.y * BRAKE + bob, v.z * BRAKE));
        applyMovement(data);
    }

    private void steerToward(LightningBlastData data, Vec3 point, double maxSpeed, double agility)
    {
        Vec3 desired = point.subtract(dragon.position());
        double dist = desired.length();
        if (dist > 1.0E-4)
        {
            desired = desired.scale(Math.min(maxSpeed, dist * 0.25) / dist); // ease in on arrival
        }
        data.setVelocity(data.getVelocity().lerp(desired, agility));
    }

    private void applyMovement(LightningBlastData data)
    {
        Vec3 v = data.getVelocity();
        this.dragon.setDeltaMovement(v);
        this.dragon.move(MoverType.SELF, v);
    }

    /** Same yaw convention as EnderDragon.aiStep (the model faces -Z). */
    private void faceToward(Vec3 dir, float lerp)
    {
        if (dir.horizontalDistanceSqr() < 1.0E-4)
        {
            return;
        }
        @SuppressWarnings("SuspiciousNameCombination")
        float desiredYaw = 180.0F - (float) Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG;
        this.dragon.setYRot(Mth.rotLerp(lerp, this.dragon.getYRot(), desiredYaw));
    }

    private void abort()
    {
        this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
    }

    public enum Stage { APPROACH, WINDUP, CHARGE, LINGER }
}

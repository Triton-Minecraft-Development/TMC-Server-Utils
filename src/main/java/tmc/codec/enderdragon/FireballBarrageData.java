package tmc.codec.enderdragon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import tmc.entity.enderdragon.phase.FireballBarragePhase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FireballBarrageData {

    public static final Codec<FireballBarrageData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.BOOL.fieldOf("active")
                            .forGetter(FireballBarrageData::isActive),
                    Codec.INT.fieldOf("stage")
                            .forGetter(data -> data.getStage().ordinal()),
                    Codec.INT.fieldOf("stage_ticks")
                            .forGetter(FireballBarrageData::getStageTicks),
                    Codec.INT.fieldOf("total_ticks")
                            .forGetter(FireballBarrageData::getTotalTicks),
                    Codec.INT.fieldOf("shots_fired")
                            .forGetter(FireballBarrageData::getShotsFired),
                    Vec3.CODEC.optionalFieldOf("center_pos")
                            .forGetter(FireballBarrageData::getCenterPos),
                    Codec.DOUBLE.fieldOf("cluster_spread")
                            .forGetter(FireballBarrageData::getClusterSpread),
                    Codec.DOUBLE.fieldOf("spread_radius")
                            .forGetter(FireballBarrageData::getSpreadRadius),
                    Vec3.CODEC.listOf().fieldOf("pattern")
                            .forGetter(FireballBarrageData::getPattern),
                    Vec3.CODEC.fieldOf("velocity")
                            .forGetter(FireballBarrageData::getVelocity),
                    Codec.INT.fieldOf("cooldown_ticks")
                            .forGetter(FireballBarrageData::getCooldownTimestampTicks)
            ).apply(instance, (active, stage, stageTicks, totalTicks, shotsFired, centerPos, clusterSpread, spreadRadius, pattern, velocity, cooldownTicks) ->
                    new FireballBarrageData(active, FireballBarragePhase.Stage.values()[stage], stageTicks, totalTicks, shotsFired,
                            centerPos.orElse(null), clusterSpread, spreadRadius, pattern, velocity, cooldownTicks))
    );

    private boolean active;
    private FireballBarragePhase.Stage stage;
    private int stageTicks;
    private int totalTicks;
    private int shotsFired;
    @Nullable
    private Vec3 centerPos;
    private double clusterSpread;
    private double spreadRadius;
    private List<Vec3> pattern;
    private Vec3 velocity;
    private int cooldownTimestampTicks;


    public FireballBarrageData()
    {
        this.active = false;
        this.stage = FireballBarragePhase.Stage.APPROACH;
        this.stageTicks = 0;
        this.totalTicks = 0;
        this.shotsFired = 0;
        this.centerPos = null;
        this.clusterSpread = 0.0;
        this.spreadRadius = 0.0;
        this.pattern = new ArrayList<>();
        this.velocity = Vec3.ZERO;
        this.cooldownTimestampTicks = 0;
    }

    public FireballBarrageData(boolean active, FireballBarragePhase.Stage stage, int stageTicks, int totalTicks, int shotsFired,
                               @Nullable Vec3 centerPos, double clusterSpread, double spreadRadius, List<Vec3> pattern,
                               Vec3 velocity, int cooldownTimestampTicks)
    {
        this.active = active;
        this.stage = stage;
        this.stageTicks = stageTicks;
        this.totalTicks = totalTicks;
        this.shotsFired = shotsFired;
        this.centerPos = centerPos;
        this.clusterSpread = clusterSpread;
        this.spreadRadius = spreadRadius;
        this.pattern = new ArrayList<>(pattern); // codec lists are immutable
        this.velocity = velocity;
        this.cooldownTimestampTicks = cooldownTimestampTicks;
    }

    /**
     * Resets everything except the cooldown and marks the attack as in progress.
     */
    public FireballBarrageData start(Vec3 velocity, List<Vec3> pattern)
    {
        this.active = true;
        this.stage = FireballBarragePhase.Stage.APPROACH;
        this.stageTicks = 0;
        this.totalTicks = 0;
        this.shotsFired = 0;
        this.centerPos = null;
        this.clusterSpread = 0.0;
        this.spreadRadius = 0.0;
        this.pattern = new ArrayList<>(pattern);
        this.velocity = velocity;
        return this;
    }

    public FireballBarrageData setActive(boolean active)
    {
        this.active = active;
        return this;
    }

    public boolean isActive()
    {
        return this.active;
    }

    public FireballBarrageData setStage(FireballBarragePhase.Stage stage)
    {
        this.stage = stage;
        this.stageTicks = 0;
        return this;
    }

    public FireballBarragePhase.Stage getStage()
    {
        return this.stage;
    }

    /** Advances both the stage tick counter and the whole-attack counter. */
    public FireballBarrageData updateTicks()
    {
        this.stageTicks++;
        this.totalTicks++;
        return this;
    }

    public int getStageTicks()
    {
        return this.stageTicks;
    }

    public int getTotalTicks()
    {
        return this.totalTicks;
    }

    public FireballBarrageData incrementShotsFired()
    {
        this.shotsFired++;
        return this;
    }

    public FireballBarrageData resetShotsFired()
    {
        this.shotsFired = 0;
        return this;
    }

    public int getShotsFired()
    {
        return this.shotsFired;
    }

    public FireballBarrageData setCenterPos(Vec3 pos)
    {
        this.centerPos = pos;
        return this;
    }

    public Optional<Vec3> getCenterPos()
    {
        return this.centerPos != null ? Optional.of(this.centerPos) : Optional.empty();
    }

    public FireballBarrageData setClusterSpread(double clusterSpread)
    {
        this.clusterSpread = clusterSpread;
        return this;
    }

    public double getClusterSpread()
    {
        return this.clusterSpread;
    }

    public FireballBarrageData setSpreadRadius(double spreadRadius)
    {
        this.spreadRadius = spreadRadius;
        return this;
    }

    public double getSpreadRadius()
    {
        return this.spreadRadius;
    }

    public List<Vec3> getPattern()
    {
        return this.pattern;
    }

    public FireballBarrageData setVelocity(Vec3 velocity)
    {
        this.velocity = velocity;
        return this;
    }

    public Vec3 getVelocity()
    {
        return this.velocity;
    }

    public FireballBarrageData setCooldown(int tickTimestamp)
    {
        this.cooldownTimestampTicks = tickTimestamp;
        return this;
    }

    public int getCooldownTimestampTicks()
    {
        return this.cooldownTimestampTicks;
    }

    public boolean isCooldownReady(int currentTicks)
    {
        return currentTicks > this.cooldownTimestampTicks;
    }
}

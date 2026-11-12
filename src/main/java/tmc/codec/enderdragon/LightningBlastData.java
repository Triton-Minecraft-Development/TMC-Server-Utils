package tmc.codec.enderdragon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import tmc.entity.enderdragon.phase.LightningBlastPhase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LightningBlastData {

    public static final Codec<LightningBlastData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.BOOL.fieldOf("active")
                            .forGetter(LightningBlastData::isActive),
                    Codec.INT.fieldOf("stage")
                            .forGetter(data -> data.getStage().ordinal()),
                    Codec.INT.fieldOf("stage_ticks")
                            .forGetter(LightningBlastData::getStageTicks),
                    Codec.INT.fieldOf("total_ticks")
                            .forGetter(LightningBlastData::getTotalTicks),
                    Vec3.CODEC.optionalFieldOf("perch_pos")
                            .forGetter(LightningBlastData::getPerchPos),
                    Vec3.CODEC.fieldOf("velocity")
                            .forGetter(LightningBlastData::getVelocity),
                    BlockPos.CODEC.listOf().fieldOf("zones") // each is a zone's min corner, Y = ground level
                            .forGetter(LightningBlastData::getZones),
                    Codec.INT.fieldOf("cooldown_ticks")
                            .forGetter(LightningBlastData::getCooldownTimestampTicks)
            ).apply(instance, (active, stage, stageTicks, totalTicks, perchPos, velocity, zones, cooldownTicks) ->
                    new LightningBlastData(active, LightningBlastPhase.Stage.values()[stage], stageTicks, totalTicks,
                            perchPos.orElse(null), velocity, zones, cooldownTicks))
    );

    private boolean active;
    private LightningBlastPhase.Stage stage;
    private int stageTicks;
    private int totalTicks;
    @Nullable
    private Vec3 perchPos;
    private Vec3 velocity;
    private List<BlockPos> zones;
    private int cooldownTimestampTicks;


    public LightningBlastData()
    {
        this.active = false;
        this.stage = LightningBlastPhase.Stage.APPROACH;
        this.stageTicks = 0;
        this.totalTicks = 0;
        this.perchPos = null;
        this.velocity = Vec3.ZERO;
        this.zones = new ArrayList<>();
        this.cooldownTimestampTicks = 0;
    }

    public LightningBlastData(boolean active, LightningBlastPhase.Stage stage, int stageTicks, int totalTicks, @Nullable Vec3 perchPos, Vec3 velocity, List<BlockPos> zones, int cooldownTimestampTicks)
    {
        this.active = active;
        this.stage = stage;
        this.stageTicks = stageTicks;
        this.totalTicks = totalTicks;
        this.perchPos = perchPos;
        this.velocity = velocity;
        this.zones = new ArrayList<>(zones); // codec lists are immutable
        this.cooldownTimestampTicks = cooldownTimestampTicks;
    }

    /**
     * Resets everything except the cooldown and marks the attack as in progress.
     */
    public LightningBlastData start(Vec3 velocity)
    {
        this.active = true;
        this.stage = LightningBlastPhase.Stage.APPROACH;
        this.stageTicks = 0;
        this.totalTicks = 0;
        this.perchPos = null;
        this.velocity = velocity;
        this.zones = new ArrayList<>();
        return this;
    }

    public LightningBlastData setActive(boolean active)
    {
        this.active = active;
        return this;
    }

    public boolean isActive()
    {
        return this.active;
    }

    public LightningBlastData setStage(LightningBlastPhase.Stage stage)
    {
        this.stage = stage;
        this.stageTicks = 0;
        return this;
    }

    public LightningBlastPhase.Stage getStage()
    {
        return this.stage;
    }

    /** Advances both the stage tick counter and the whole-attack counter. */
    public LightningBlastData updateTicks()
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

    public LightningBlastData setPerchPos(Vec3 pos)
    {
        this.perchPos = pos;
        return this;
    }

    public Optional<Vec3> getPerchPos()
    {
        return Optional.ofNullable(this.perchPos);
    }

    public LightningBlastData setVelocity(Vec3 velocity)
    {
        this.velocity = velocity;
        return this;
    }

    public Vec3 getVelocity()
    {
        return this.velocity;
    }

    public LightningBlastData setZones(List<BlockPos> zones)
    {
        this.zones = new ArrayList<>(zones);
        return this;
    }

    public List<BlockPos> getZones()
    {
        return this.zones;
    }

    public LightningBlastData setCooldown(int tickTimestamp)
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

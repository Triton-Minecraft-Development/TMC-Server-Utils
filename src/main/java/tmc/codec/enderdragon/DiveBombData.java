package tmc.codec.enderdragon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import tmc.enderdragon.phase.DiveBombPhase;

import java.util.Optional;

public class DiveBombData {

    public static final Codec<DiveBombData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.fieldOf("stage")
                                    .forGetter(diveBombData -> diveBombData.getStage().ordinal()),
                    Codec.INT.fieldOf("stage_ticks")
                            .forGetter(DiveBombData::getStageTicks),
                    Vec3.CODEC.optionalFieldOf("target_pos")
                            .forGetter(DiveBombData::getTargetPos),
                    Vec3.CODEC.optionalFieldOf("apex_pos")
                            .forGetter(DiveBombData::getApexPos),
                    Vec3.CODEC.fieldOf("velocity")
                            .forGetter(DiveBombData::getVelocity),
                    Codec.INT.fieldOf("cooldown_ticks")
                            .forGetter(DiveBombData::getCooldownTimestampTicks)
            ).apply(instance, (stage, stageTicks, targetPos, apexPos, velocity, cooldownTicks) ->
                    new DiveBombData(DiveBombPhase.Stage.values()[stage], stageTicks, targetPos.orElse(null), apexPos.orElse(null), velocity, cooldownTicks))
    );

    private DiveBombPhase.Stage stage;
    private int stageTicks;
    @Nullable
    private Vec3 targetPos;
    @Nullable
    private Vec3 apexPos;
    private Vec3 velocity;
    private int cooldownTimestampTicks;


    public DiveBombData()
    {
        this.stage = DiveBombPhase.Stage.ASCEND;
        this.stageTicks = 0;
        this.targetPos = null;
        this.apexPos = null;
        this.velocity = Vec3.ZERO;
        this.cooldownTimestampTicks = 0;
    }

    public DiveBombData(DiveBombPhase.Stage stage, int stageTicks, @Nullable Vec3 targetPos, @Nullable Vec3 apexPos, Vec3 velocity, int cooldownTimestampTicks)
    {
        this.stage = stage;
        this.stageTicks = stageTicks;
        this.targetPos = targetPos;
        this.apexPos = apexPos;
        this.velocity = velocity;
        this.cooldownTimestampTicks = cooldownTimestampTicks;
    }

    public DiveBombData setStage(DiveBombPhase.Stage stage)
    {
        this.stage = stage;
        this.stageTicks = 0;
        return this;
    }

    public DiveBombPhase.Stage getStage()
    {
        return this.stage;
    }

    public DiveBombData updateStageTicks()
    {
        this.stageTicks++;
        return this;
    }

    public DiveBombData resetStageTicks()
    {
        this.stageTicks = 0;
        return this;
    }

    public int getStageTicks()
    {
        return this.stageTicks;
    }

    public DiveBombData setTargetPos(Vec3 pos)
    {
        this.targetPos = pos;
        return this;
    }

    public Optional<Vec3> getTargetPos()
    {
        return this.targetPos != null ? Optional.of(this.targetPos) : Optional.empty();
    }

    public DiveBombData setApexPos(Vec3 pos)
    {
        this.apexPos = pos;
        return this;
    }

    public Optional<Vec3> getApexPos()
    {
        return this.apexPos != null ? Optional.of(this.apexPos) : Optional.empty();
    }

    public DiveBombData setVelocity(Vec3 velocity)
    {
        this.velocity = velocity;
        return this;
    }

    public Vec3 getVelocity()
    {
        return this.velocity;
    }

    public DiveBombData setCooldown(int tickTimestamp)
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

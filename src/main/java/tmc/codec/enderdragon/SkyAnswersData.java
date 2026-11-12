package tmc.codec.enderdragon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

public class SkyAnswersData {

    public static final Codec<SkyAnswersData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.BOOL.fieldOf("used")
                            .forGetter(SkyAnswersData::isUsed),
                    Codec.BOOL.fieldOf("active")
                            .forGetter(SkyAnswersData::isActive),
                    Codec.INT.fieldOf("ticks")
                            .forGetter(SkyAnswersData::getTicks),
                    Vec3.CODEC.fieldOf("velocity")
                            .forGetter(SkyAnswersData::getVelocity)
            ).apply(instance, SkyAnswersData::new)
    );

    private boolean used;   // the attack happens once per fight (per dragon entity)
    private boolean active;
    private int ticks;
    private Vec3 velocity;


    public SkyAnswersData()
    {
        this(false, false, 0, Vec3.ZERO);
    }

    public SkyAnswersData(boolean used, boolean active, int ticks, Vec3 velocity)
    {
        this.used = used;
        this.active = active;
        this.ticks = ticks;
        this.velocity = velocity;
    }

    /** Marks the attack as used and in progress, and resets the timeline. */
    public SkyAnswersData start(Vec3 velocity)
    {
        this.used = true;
        this.active = true;
        this.ticks = 0;
        this.velocity = velocity;
        return this;
    }

    public boolean isUsed()
    {
        return this.used;
    }

    public SkyAnswersData setActive(boolean active)
    {
        this.active = active;
        return this;
    }

    public boolean isActive()
    {
        return this.active;
    }

    public SkyAnswersData setTicks(int ticks)
    {
        this.ticks = ticks;
        return this;
    }

    public int getTicks()
    {
        return this.ticks;
    }

    public SkyAnswersData setVelocity(Vec3 velocity)
    {
        this.velocity = velocity;
        return this;
    }

    public Vec3 getVelocity()
    {
        return this.velocity;
    }
}

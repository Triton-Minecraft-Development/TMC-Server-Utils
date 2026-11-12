package tmc.entity.areaeffectcloud;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.BiConsumer;

public class TMCAreaEffectCloud extends AreaEffectCloud {

    private final BiConsumer<Entity, Integer> onVictimTick;
    private final HashMap<UUID, Integer> tmcVictims = new HashMap<>();
    private int reapplicationDelayTicks = 20;

    public TMCAreaEffectCloud(Level level, double x, double y, double z, BiConsumer<Entity, Integer> onVictimTick)
    {
        super(level, x, y, z);
        this.onVictimTick = onVictimTick;
    }

    public void setReapplicationDelayTicks(int ticks)
    {
        this.reapplicationDelayTicks = ticks;
    }

    @Override
    protected void serverTick(ServerLevel serverLevel)
    {
        super.serverTick(serverLevel);

        if (this.isRemoved() || this.isWaiting() || this.tickCount % 5 != 0)
        {
            return;
        }

        this.tmcVictims.entrySet().removeIf(e -> this.tickCount >= e.getValue());

        float radius = this.getRadius();
        Entity owner = this.getOwner();

        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox()))
        {
            if ((owner != null && entity.getUUID() == owner.getUUID()) || this.tmcVictims.containsKey(entity.getUUID()))
            {
                continue;
            }
            double dx = entity.getX() - this.getX();
            double dz = entity.getZ() - this.getZ();

            if (dx * dx + dz * dz > radius * radius)
            {
                continue;
            }
            this.tmcVictims.put(entity.getUUID(), this.tickCount + this.reapplicationDelayTicks);
            this.onVictimTick.accept(entity, this.tickCount);
        }
    }


}

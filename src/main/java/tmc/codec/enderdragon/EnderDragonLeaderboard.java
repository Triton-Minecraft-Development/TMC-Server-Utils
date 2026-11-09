package tmc.codec.enderdragon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.HashMap;
import java.util.UUID;

public record EnderDragonLeaderboard(HashMap<UUID, Float> playerLifetimeDamages, HashMap<UUID, Integer> dragonsSlayed) {

    public static final Codec<EnderDragonLeaderboard> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.unboundedMap(UUIDUtil.CODEC, Codec.FLOAT)
                            .xmap(HashMap::new, map -> map)
                            .fieldOf("player_damages")
                            .forGetter(EnderDragonLeaderboard::playerLifetimeDamages),
                    Codec.unboundedMap(UUIDUtil.CODEC, Codec.INT)
                            .xmap(HashMap::new, map -> map)
                            .fieldOf("dragons_slayed")
                            .forGetter(EnderDragonLeaderboard::dragonsSlayed)
            ).apply(instance, EnderDragonLeaderboard::new)
    );

    public EnderDragonLeaderboard()
    {
        this(new HashMap<>(), new HashMap<>());
    }

    public void updateLifetimeDamage(UUID uuid, float damageToAdd)
    {
        if (this.playerLifetimeDamages.containsKey(uuid) && this.playerLifetimeDamages.get(uuid) != null)
        {
            this.playerLifetimeDamages.compute(uuid, (_, storedLifetimeDamage) ->
            {
                if (storedLifetimeDamage == null)
                {
                    return null;
                }
                if (storedLifetimeDamage.isInfinite())
                {
                    return Float.POSITIVE_INFINITY;
                }
                return storedLifetimeDamage + damageToAdd;
            });
            return;
        }
        this.playerLifetimeDamages.put(uuid, damageToAdd);
    }

    public boolean wipeLifetimeDamage(UUID uuid)
    {
        if (!this.playerLifetimeDamages.containsKey(uuid) || this.playerLifetimeDamages.get(uuid) == null)
        {
            return false;
        }
        this.playerLifetimeDamages.remove(uuid);
        return true;
    }

    public void updateDragonsSlayed(UUID uuid)
    {
        if (this.dragonsSlayed.containsKey(uuid) && this.dragonsSlayed.get(uuid) != null)
        {
            this.dragonsSlayed.compute(uuid, (_, slayed) ->
            {
                if (slayed == null)
                {
                    return 0;
                }
                return slayed + 1;
            });
        }
    }

    public boolean wipeDragonsSlayed(UUID uuid)
    {
        if (!this.dragonsSlayed.containsKey(uuid) || this.dragonsSlayed.get(uuid) == null)
        {
            return false;
        }
        this.dragonsSlayed.remove(uuid);
        return true;
    }
}

package tmc.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.*;

public record AttackerHistory(HashMap<UUID, Float> attackerUuids) {

    public static final Codec<AttackerHistory> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.FLOAT)
                            .xmap(HashMap::new, map -> map)
                            .fieldOf("attacker_history")
                            .forGetter(AttackerHistory::attackerUuids)
            ).apply(instance, AttackerHistory::new)
    );

    public AttackerHistory()
    {
        this(new HashMap<>());
    }

    public void updateAttacker(UUID uuid, float damage)
    {
        if (this.attackerUuids.containsKey(uuid) && this.attackerUuids.get(uuid) != null)
        {
            this.attackerUuids.compute(uuid, (_, storedDamage) -> {
                if (storedDamage != null)
                {
                    return storedDamage + damage;
                }
                return null;
            });
            return;
        }
        this.attackerUuids.put(uuid, damage);
    }
}

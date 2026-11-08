package tmc.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public record AttackerHistory(HashSet<UUID> attackerUuids) {

    public static final Codec<AttackerHistory> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    UUIDUtil.CODEC.listOf()
                            .xmap(HashSet::new, List::copyOf)
                            .fieldOf("attackers")
                            .forGetter(AttackerHistory::attackerUuids)
            ).apply(instance, AttackerHistory::new)
    );

    public AttackerHistory()
    {
        this(new HashSet<>());
    }

    public void addAttacker(UUID uuid)
    {
        this.attackerUuids.add(uuid);
    }
}

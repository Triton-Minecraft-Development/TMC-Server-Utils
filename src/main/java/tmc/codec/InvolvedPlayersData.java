package tmc.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.UUID;

public record InvolvedPlayersData(HashMap<UUID, Integer> players) {

    public static final Codec<InvolvedPlayersData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)
                            .xmap(HashMap::new, map -> map)
                            .fieldOf("involved_players")
                            .forGetter(InvolvedPlayersData::players)
            ).apply(instance, InvolvedPlayersData::new)
    );

    public static final int DURATION_TICKS = 18000; // 15 minutes

    public InvolvedPlayersData()
    {
        this(new HashMap<>());
    }

    public InvolvedPlayersData logPlayer(ServerPlayer player, int tickTimestamp)
    {
        this.players.put(player.getUUID(), tickTimestamp);
        return this;
    }

    public boolean isPlayerInvolved(ServerPlayer player)
    {
        return this.isPlayerUUIDInvolved(player.getUUID());
    }

    public boolean isPlayerUUIDInvolved(UUID uuid)
    {
        return this.players.containsKey(uuid) && this.players.get(uuid) != null;
    }

    public InvolvedPlayersData removePlayerOnTick(ServerPlayer player, int ticks)
    {
        return this.removePlayerUUIDOnTick(player.getUUID(), ticks);
    }

    public InvolvedPlayersData removePlayerUUIDOnTick(UUID uuid, int ticks)
    {
        if (!this.isPlayerUUIDInvolved(uuid))
        {
            return this;
        }
        if (ticks > this.players.get(uuid))
        {
            this.players.remove(uuid);
        }

        return this;
    }

    public InvolvedPlayersData removePlayer(ServerPlayer player)
    {
        return removePlayerUUID(player.getUUID());
    }

    public InvolvedPlayersData removePlayerUUID(UUID uuid)
    {
        this.players.remove(uuid);
        return this;
    }
}

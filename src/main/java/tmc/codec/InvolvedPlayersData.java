package tmc.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

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

    public InvolvedPlayersData logPlayer(ServerPlayer player, int tickTimestamp, Consumer<ServerPlayer> onFirstInvolvement)
    {
        if (isPlayerInvolved(player))
        {
            return this;
        }
        onFirstInvolvement.accept(player);
        return this.logPlayer(player, tickTimestamp);
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

    public InvolvedPlayersData removePlayerOnTick(ServerPlayer player, int ticks, Consumer<ServerPlayer> onRemoval)
    {
        if (!this.isPlayerInvolved(player))
        {
            return this;
        }
        if (ticks > this.players.get(player.getUUID()))
        {
            onRemoval.accept(player);
            this.players.remove(player.getUUID());
        }

        return this;
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
        return this.removePlayerUUID(player.getUUID());
    }

    public InvolvedPlayersData removePlayer(ServerPlayer player, Consumer<ServerPlayer> onRemoval)
    {
        if (!this.players.containsKey(player.getUUID()))
        {
            return this;
        }
        onRemoval.accept(player);
        return this.removePlayer(player);
    }

    public InvolvedPlayersData removeAllPlayers(MinecraftServer server, Consumer<ServerPlayer> onRemoval)
    {
        this.players.keySet().removeIf(uuid ->
        {
           ServerPlayer player = server.getPlayerList().getPlayer(uuid);
           if (player == null)
           {
               return true;
           }
           onRemoval.accept(player);
           return true;
        });
        return this;
    }

    public InvolvedPlayersData removePlayerUUID(UUID uuid)
    {
        this.players.remove(uuid);
        return this;
    }
}

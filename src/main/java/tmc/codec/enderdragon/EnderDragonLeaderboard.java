package tmc.codec.enderdragon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import tmc.attachments.TMCDataAttachments;

import java.util.HashMap;
import java.util.UUID;

/**
 * Global leaderboard stats for the Ender Dragon
 * @param playerLifetimeDamages The lifetime damageDealt against the Ender Dragon for all players
 * @param dragonsSlayed The lifetime slays of the Ender Dragon for all players
 * @param bestDamage The best damageDealt a player has dealt to the Ender Dragon in a single fight
 */
public record EnderDragonLeaderboard(HashMap<UUID, Float> playerLifetimeDamages, HashMap<UUID, Integer> dragonsSlayed, HashMap<UUID, Float> bestDamage) {

    public static final Codec<EnderDragonLeaderboard> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.unboundedMap(UUIDUtil.CODEC, Codec.FLOAT)
                            .xmap(HashMap::new, map -> map)
                            .fieldOf("player_damages")
                            .forGetter(EnderDragonLeaderboard::playerLifetimeDamages),
                    Codec.unboundedMap(UUIDUtil.CODEC, Codec.INT)
                            .xmap(HashMap::new, map -> map)
                            .fieldOf("dragons_slayed")
                            .forGetter(EnderDragonLeaderboard::dragonsSlayed),
                    Codec.unboundedMap(UUIDUtil.CODEC, Codec.FLOAT)
                            .xmap(HashMap::new, map -> map)
                            .fieldOf("best_dragon_damage")
                            .forGetter(EnderDragonLeaderboard::bestDamage)
            ).apply(instance, EnderDragonLeaderboard::new)
    );

    public static void update(ServerLevel serverLevel, ServerPlayer player, float damage)
    {
        ServerLevel overworld = serverLevel.getServer().overworld();

        overworld.modifyAttached(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD, leaderboard -> leaderboard.updateDragonsSlayed(player).updateLifetimeDamage(player, damage).updateBestDamage(player, damage));
    }

    public EnderDragonLeaderboard()
    {
        this(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public EnderDragonLeaderboard updateLifetimeDamage(ServerPlayer player, float damageToAdd)
    {
        UUID uuid = player.getUUID();
        if (this.playerLifetimeDamages.containsKey(uuid) && this.playerLifetimeDamages.get(uuid) != null)
        {
            this.playerLifetimeDamages.compute(uuid, (_, storedLifetimeDamage) ->
            {
                if (storedLifetimeDamage == null)
                {
                    return 0f;
                }
                if (storedLifetimeDamage.isInfinite())
                {
                    return Float.POSITIVE_INFINITY;
                }
                return storedLifetimeDamage + damageToAdd;
            });
            return this;
        }
        this.playerLifetimeDamages.put(uuid, damageToAdd);
        return this;
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

    public EnderDragonLeaderboard updateDragonsSlayed(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        this.dragonsSlayed.putIfAbsent(uuid, 0);
        this.dragonsSlayed.compute(uuid, (_, slayed) ->
        {
            if (slayed == null)
            {
                return 0;
            }
            return slayed + 1;
        });
        return this;
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

    public EnderDragonLeaderboard updateBestDamage(ServerPlayer player, float damageDealt)
    {
        UUID uuid = player.getUUID();
        this.bestDamage.putIfAbsent(uuid, damageDealt);
        if (this.bestDamage.containsKey(uuid) && this.bestDamage.get(uuid) != null)
        {
            this.bestDamage.compute(uuid, (_, bestDamageDealt) ->
            {
                if (bestDamageDealt == null)
                {
                    return 0f;
                }
                return Math.max(bestDamageDealt, damageDealt);
            });
        }
        return this;
    }

    public boolean wipeBestDamageDealt(UUID uuid)
    {
        if (!this.bestDamage.containsKey(uuid) || this.bestDamage.get(uuid) == null)
        {
            return false;
        }
        this.bestDamage.remove(uuid);
        return true;
    }

    public PlayerStats getPlayerStats(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        Float lifetimeDamage = this.playerLifetimeDamages.getOrDefault(uuid, 0f);
        Integer slays = this.dragonsSlayed.getOrDefault(uuid, 0);
        Float bestDamage = this.bestDamage.getOrDefault(uuid, 0f);
        return new PlayerStats(lifetimeDamage, slays, bestDamage);
    }

    public record PlayerStats(float lifetimeDamage, int slays, float bestDamage) {}
}

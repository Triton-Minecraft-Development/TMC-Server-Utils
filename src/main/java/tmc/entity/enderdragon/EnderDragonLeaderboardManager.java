package tmc.entity.enderdragon;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import tmc.TMCServerUtils;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.AttackerHistoryData;
import tmc.codec.enderdragon.EnderDragonLeaderboard;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Leaderboard management for Ender Dragon
 */
public class EnderDragonLeaderboardManager {

    private static final int TOP_N = 10;
    private static final DecimalFormat DAMAGE_FORMAT = new DecimalFormat("#,##0.00");

    private static final Component DIVIDER = Component.literal(ChatFormatting.GREEN + "" + ChatFormatting.BOLD + "---------------------------------------------");
    private static final Component DAMAGE_LEADERBOARD_HEADER = Component.literal(ChatFormatting.BOLD + "          " + ChatFormatting.LIGHT_PURPLE + "Top 10 All-Time Ender Dragon Damage Dealers     ");
    private static final Component FIGHT_LEADERBOARD_HEADER = Component.literal(ChatFormatting.BOLD + "          " + ChatFormatting.LIGHT_PURPLE + "Top 10 Ender Dragon Damage Dealers     ");
    private static final Component SLAYER_LEADERBOARD_HEADER = Component.literal(ChatFormatting.BOLD + "          " + ChatFormatting.LIGHT_PURPLE + "Top 10 All-Time Ender Dragon Slayers     ");

    public static Component getDragonsKilledMessage(int kills)
    {
        if (kills == 1)
        {
            return Component.literal(ChatFormatting.LIGHT_PURPLE + "You have slayed the Ender Dragon " + ChatFormatting.GOLD + kills + ChatFormatting.LIGHT_PURPLE + " time!");
        }
        return Component.literal(ChatFormatting.LIGHT_PURPLE + "You have slayed the Ender Dragon " + ChatFormatting.GOLD + kills + ChatFormatting.LIGHT_PURPLE + " times!");
    }

    public static Component getLifetimeDragonDamageMessage(float damage, int kills)
    {
        return Component.literal(ChatFormatting.LIGHT_PURPLE + "You have dealt " + ChatFormatting.GOLD + DAMAGE_FORMAT.format(damage) + ChatFormatting.LIGHT_PURPLE + " damage across " + ChatFormatting.GOLD + kills + ChatFormatting.LIGHT_PURPLE + " Ender Dragons!");
    }

    public static Component getBestDamageMessage(float damage)
    {
        return Component.literal(ChatFormatting.LIGHT_PURPLE + "Highest damage to an Ender Dragon: " + ChatFormatting.GOLD + DAMAGE_FORMAT.format(damage) + ChatFormatting.LIGHT_PURPLE + " damage.");
    }

    private static Component getSelfDamageComponent(float damage, int position)
    {
        return Component.literal(ChatFormatting.GRAY + "Your damage: " + DAMAGE_FORMAT.format(damage) + " damage (Position: #" + position + ")");
    }

    private static Component getSelfSlayComponent(int slays, int position)
    {
        if (slays == 1)
        {
            return Component.literal(ChatFormatting.GRAY + "Your slays: " + slays + " slay (Position: #" + position + ")");
        }
        return Component.literal(ChatFormatting.GRAY + "Your slays: " + slays + " slays (Position: #" + position + ")");
    }

    private static Component getDamageLeaderboardEntryComponent(String username, float damage, int position)
    {
        TextColor color = switch (position)
        {
            case 1 -> TextColor.GOLD;
            case 2 -> TextColor.WHITE;
            case 3 -> TextColor.fromRgb(13535558);
            default -> TextColor.GRAY;
        };
        if (position <= 3)
        {
            return Component.literal("#" + position + ": " + username + " - " + DAMAGE_FORMAT.format(damage) + " damage").withColor(color).withStyle(ChatFormatting.BOLD);
        }
        return Component.literal("#" + position + ": " + username + " - " + DAMAGE_FORMAT.format(damage) + " damage").withColor(color);
    }

    private static Component getSlayLeaderboardEntryComponent(String username, int slays, int position)
    {
        TextColor color = switch (position)
        {
            case 1 -> TextColor.GOLD;
            case 2 -> TextColor.WHITE;
            case 3 -> TextColor.fromRgb(13535558);
            default -> TextColor.GRAY;
        };

        String slayStr = slays == 1 ? " slay" : " slays";
        if (position <= 3)
        {
            return Component.literal("#" + position + ": " + username + " - " + slays + slayStr).withColor(color).withStyle(ChatFormatting.BOLD);
        }
        return Component.literal("#" + position + ": " + username + " - " + slays + slayStr).withColor(color);
    }

    /**
     * Outputs the lifetime damage to the Ender Dragon leaderboard to the specified player
     * @param serverLevel The {@linkplain ServerLevel serverl level} the player is present on
     * @param serverPlayer The {@linkplain ServerPlayer player} to output towards
     */
    public static void printLifetimeDamageLeaderboard(ServerLevel serverLevel, ServerPlayer serverPlayer)
    {
        ServerLevel overworld = serverLevel.getServer().overworld();
        EnderDragonLeaderboard leaderboard = overworld.getAttachedOrCreate(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD);

        List<Map.Entry<UUID, Float>> fullList = leaderboard.playerLifetimeDamages().entrySet().stream()
                .sorted(Map.Entry.<UUID, Float>comparingByValue().reversed())
                .filter(uuidIntegerEntry -> !leaderboard.isPlayerBanned(uuidIntegerEntry.getKey()))
                .toList();

        serverPlayer.sendSystemMessage(DIVIDER);
        serverPlayer.sendSystemMessage(DAMAGE_LEADERBOARD_HEADER);
        serverPlayer.sendSystemMessage(Component.empty());
        int selfRank = 0;
        for (int rank = 0; rank < fullList.size(); rank++)
        {
            Map.Entry<UUID, Float> entry = fullList.get(rank);

            if (rank < TOP_N)
            {
                String username = TMCServerUtils.getUsernameFromUUID(overworld, entry.getKey());
                serverPlayer.sendSystemMessage(getDamageLeaderboardEntryComponent(username, entry.getValue(), rank + 1));
            }
            if (entry.getKey().equals(serverPlayer.getUUID()))
            {
                selfRank = rank + 1;
            }
        }
        if (selfRank == 0)
        {
            serverPlayer.sendSystemMessage(Component.empty());
            serverPlayer.sendSystemMessage(DIVIDER);
            return;
        }

        serverPlayer.sendSystemMessage(Component.empty());

        float damage = leaderboard.playerLifetimeDamages().getOrDefault(serverPlayer.getUUID(), 0f);
        serverPlayer.sendSystemMessage(getSelfDamageComponent(damage, selfRank));
        serverPlayer.sendSystemMessage(Component.empty());
        serverPlayer.sendSystemMessage(DIVIDER);
    }

    /**
     * Outputs the lifetime damage to the Ender Dragon leaderboard to the given {@linkplain CommandSourceStack command source stack}.
     * Will output to the player if the command was executed by a player.
     * @param serverLevel The {@linkplain ServerLevel server level} the command takes place on
     * @param sourceStack The {@linkplain CommandSourceStack source stack} of the command
     */
    public static void printLifetimeDamageLeaderboard(ServerLevel serverLevel, CommandSourceStack sourceStack)
    {
        if (sourceStack.isPlayer() && sourceStack.getEntity() instanceof ServerPlayer serverPlayer)
        {
            printLifetimeDamageLeaderboard(serverLevel, serverPlayer);
            return;
        }

        ServerLevel overworld = serverLevel.getServer().overworld();
        EnderDragonLeaderboard leaderboard = overworld.getAttachedOrCreate(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD);

        sourceStack.sendSystemMessage(DIVIDER);
        sourceStack.sendSystemMessage(DAMAGE_LEADERBOARD_HEADER);
        sourceStack.sendSystemMessage(Component.empty());
        List<Map.Entry<UUID, Float>> topN = leaderboard.playerLifetimeDamages().entrySet().stream()
                .sorted(Map.Entry.<UUID, Float>comparingByValue().reversed())
                .filter(uuidIntegerEntry -> !leaderboard.isPlayerBanned(uuidIntegerEntry.getKey()))
                .limit(TOP_N)
                .toList();

        int rank = 1;
        for (Map.Entry<UUID, Float> entry : topN)
        {
            String username = TMCServerUtils.getUsernameFromUUID(overworld, entry.getKey());

            sourceStack.sendSystemMessage(getDamageLeaderboardEntryComponent(username, entry.getValue(), rank));
            rank++;
        }

        sourceStack.sendSystemMessage(Component.empty());
        sourceStack.sendSystemMessage(DIVIDER);
    }

    /**
     * Outputs the total Slayed Ender Dragons leaderboard to the specified player
     * @param serverLevel The {@linkplain ServerLevel server level} the player is present on
     * @param serverPlayer The {@linkplain ServerPlayer server player} to output towards
     */
    public static void printSlayedLeaderboard(ServerLevel serverLevel, ServerPlayer serverPlayer)
    {
        ServerLevel overworld = serverLevel.getServer().overworld();
        EnderDragonLeaderboard leaderboard = overworld.getAttachedOrCreate(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD);

        List<Map.Entry<UUID, Integer>> fullList = leaderboard.dragonsSlayed().entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .filter(uuidIntegerEntry -> !leaderboard.isPlayerBanned(uuidIntegerEntry.getKey()))
                .toList();

        serverPlayer.sendSystemMessage(DIVIDER);
        serverPlayer.sendSystemMessage(SLAYER_LEADERBOARD_HEADER);
        serverPlayer.sendSystemMessage(Component.empty());

        int selfRank = 0;
        for (int rank = 0; rank < fullList.size(); rank++)
        {
            Map.Entry<UUID, Integer> entry = fullList.get(rank);

            if (rank < TOP_N)
            {
                String username = TMCServerUtils.getUsernameFromUUID(overworld, entry.getKey());
                serverPlayer.sendSystemMessage(getSlayLeaderboardEntryComponent(username, entry.getValue(), rank + 1));
            }
            if (entry.getKey().equals(serverPlayer.getUUID()))
            {
                selfRank = rank + 1;
            }
        }
        if (selfRank == 0)
        {
            serverPlayer.sendSystemMessage(Component.empty());
            serverPlayer.sendSystemMessage(DIVIDER);
            return;
        }

        serverPlayer.sendSystemMessage(Component.empty());

        int slays = leaderboard.dragonsSlayed().getOrDefault(serverPlayer.getUUID(), 0);
        serverPlayer.sendSystemMessage(getSelfSlayComponent(slays, selfRank));
        serverPlayer.sendSystemMessage(Component.empty());
        serverPlayer.sendSystemMessage(DIVIDER);
    }

    /**
     * Outputs the total Slayed Ender Dragons leaderboard to the specified {@linkplain CommandSourceStack command source stack}
     * @param serverLevel The {@linkplain ServerLevel server level} the command takes place on
     * @param sourceStack The {@linkplain CommandSourceStack source stack} of the command
     */
    public static void printSlayedLeaderboard(ServerLevel serverLevel, CommandSourceStack sourceStack)
    {
        if (sourceStack.isPlayer() && sourceStack.getEntity() instanceof ServerPlayer serverPlayer)
        {
            printSlayedLeaderboard(serverLevel, serverPlayer);
            return;
        }

        ServerLevel overworld = serverLevel.getServer().overworld();
        EnderDragonLeaderboard leaderboard = overworld.getAttachedOrCreate(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD);

        sourceStack.sendSystemMessage(DIVIDER);
        sourceStack.sendSystemMessage(SLAYER_LEADERBOARD_HEADER);
        sourceStack.sendSystemMessage(Component.empty());

        List<Map.Entry<UUID, Integer>> topN = leaderboard.dragonsSlayed().entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .filter(uuidIntegerEntry -> !leaderboard.isPlayerBanned(uuidIntegerEntry.getKey()))
                .limit(TOP_N)
                .toList();

        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : topN)
        {
            String username = TMCServerUtils.getUsernameFromUUID(overworld, entry.getKey());

            sourceStack.sendSystemMessage(getSlayLeaderboardEntryComponent(username, entry.getValue(), rank));
            rank++;
        }

        sourceStack.sendSystemMessage(Component.empty());
        sourceStack.sendSystemMessage(DIVIDER);
    }

    /**
     * Outputs the Ender Dragon fight results leaderboard. Should only be called when
     * the Ender Dragon is defeated
     * @param dragon The {@linkplain EnderDragon Ender Dragon} being defeated
     * @param serverLevel The {@linkplain ServerLevel server level}
     * @param serverPlayer the {@linkplain ServerPlayer player} to output the leaderboard to
     */
    public static void printFightLeaderboard(EnderDragon dragon, ServerLevel serverLevel, ServerPlayer serverPlayer)
    {
        ServerLevel overworld = serverLevel.getServer().overworld();

        AttackerHistoryData leaderboard = dragon.getAttachedOrCreate(TMCDataAttachments.LOGGED_ATTACKERS);
        EnderDragonLeaderboard enderDragonLeaderboard = overworld.getAttachedOrCreate(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD);
        List<Map.Entry<UUID, Float>> fullList = leaderboard.attackerUuids().entrySet().stream()
                .sorted(Map.Entry.<UUID, Float>comparingByValue().reversed())
                .filter(uuidIntegerEntry -> !enderDragonLeaderboard.isPlayerBanned(uuidIntegerEntry.getKey()))
                .toList();

        serverPlayer.sendSystemMessage(DIVIDER);
        serverPlayer.sendSystemMessage(FIGHT_LEADERBOARD_HEADER);
        serverPlayer.sendSystemMessage(Component.empty());

        int selfRank = 0;
        for (int rank = 0; rank < fullList.size(); rank++)
        {
            Map.Entry<UUID, Float> entry = fullList.get(rank);
            if (rank < TOP_N)
            {
                String username = TMCServerUtils.getUsernameFromUUID(overworld, entry.getKey());
                serverPlayer.sendSystemMessage(getDamageLeaderboardEntryComponent(username, entry.getValue(), rank + 1));
            }
            if (entry.getKey().equals(serverPlayer.getUUID()))
            {
                selfRank = rank + 1;
            }
        }
        if (selfRank == 0)
        {
            serverPlayer.sendSystemMessage(Component.empty());
            serverPlayer.sendSystemMessage(DIVIDER);
            return;
        }
        serverPlayer.sendSystemMessage(Component.empty());

        float damage = leaderboard.attackerUuids().getOrDefault(serverPlayer.getUUID(), 0f);
        serverPlayer.sendSystemMessage(getSelfDamageComponent(damage, selfRank));
        serverPlayer.sendSystemMessage(Component.empty());
        serverPlayer.sendSystemMessage(DIVIDER);
    }

}

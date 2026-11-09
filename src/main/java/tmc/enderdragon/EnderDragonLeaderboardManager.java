package tmc.enderdragon;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import tmc.TMCServerUtils;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.AttackerHistory;
import tmc.codec.enderdragon.EnderDragonLeaderboard;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EnderDragonLeaderboardManager {

    private static final int TOP_N = 10;
    private static final DecimalFormat DAMAGE_FORMAT = new DecimalFormat("#,##0.00");

    private static final Component DIVIDER = Component.literal(ChatFormatting.GREEN + "" + ChatFormatting.BOLD + "---------------------------------------------");
    private static final Component DAMAGE_LEADERBOARD_HEADER = Component.literal(ChatFormatting.BOLD + "          " + ChatFormatting.LIGHT_PURPLE + "Top 10 All-Time Ender Dragon Damage     ");
    private static final Component FIGHT_LEADERBOARD_HEADER = Component.literal("");
    private static final Component CONTRIBUTION_LEADERBOARD_HEADER = Component.literal(ChatFormatting.BOLD + "          " + ChatFormatting.LIGHT_PURPLE + "Top 10 All-Time Ender Dragon Slayers     ");

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

    private static Component getSelfDamageComponent(float damage, int position)
    {
        return Component.literal(ChatFormatting.GRAY + "Your damage: " + DAMAGE_FORMAT.format(damage) + " damage (Position: #" + position + ")");
    }

    private static Component getSelfContributionComponent(int contributions, int position)
    {
        if (contributions == 1)
        {
            return Component.literal(ChatFormatting.GRAY + "Your contributions: " + contributions + " contribution (Position: #" + position + ")");
        }
        return Component.literal(ChatFormatting.GRAY + "Your contributions: " + contributions + " contributions (Position: #" + position + ")");
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

    private static Component getContributionLeaderboardEntryComponent(String username, int contributions, int position)
    {
        TextColor color = switch (position)
        {
            case 1 -> TextColor.GOLD;
            case 2 -> TextColor.WHITE;
            case 3 -> TextColor.fromRgb(13535558);
            default -> TextColor.GRAY;
        };

        String contributionStr = contributions == 1 ? " contribution" : " contributions";
        if (position <= 3)
        {
            return Component.literal("#" + position + ": " + username + " - " + contributions + contributionStr).withColor(color).withStyle(ChatFormatting.BOLD);
        }
        return Component.literal("#" + position + ": " + username + " - " + contributions + contributionStr).withColor(color);
    }

    public static void printLifetimeDamageLeaderboard(ServerLevel serverLevel, ServerPlayer serverPlayer)
    {
        ServerLevel overworld = serverLevel.getServer().overworld();
        EnderDragonLeaderboard leaderboard = overworld.getAttachedOrCreate(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD);

        List<Map.Entry<UUID, Float>> fullList = leaderboard.playerLifetimeDamages().entrySet().stream()
                .sorted(Map.Entry.<UUID, Float>comparingByValue().reversed())
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

    public static void printContributionsLeaderboard(ServerLevel serverLevel, ServerPlayer serverPlayer)
    {
        ServerLevel overworld = serverLevel.getServer().overworld();
        EnderDragonLeaderboard leaderboard = overworld.getAttachedOrCreate(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD);

        List<Map.Entry<UUID, Integer>> fullList = leaderboard.dragonsSlayed().entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .toList();

        serverPlayer.sendSystemMessage(DIVIDER);
        serverPlayer.sendSystemMessage(CONTRIBUTION_LEADERBOARD_HEADER);
        serverPlayer.sendSystemMessage(Component.empty());

        int selfRank = 0;
        for (int rank = 0; rank < fullList.size(); rank++)
        {
            Map.Entry<UUID, Integer> entry = fullList.get(rank);

            if (rank < TOP_N)
            {
                String username = TMCServerUtils.getUsernameFromUUID(overworld, entry.getKey());
                serverPlayer.sendSystemMessage(getContributionLeaderboardEntryComponent(username, entry.getValue(), rank + 1));
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

        int contributions = leaderboard.dragonsSlayed().getOrDefault(serverPlayer.getUUID(), 0);
        serverPlayer.sendSystemMessage(getSelfContributionComponent(contributions, selfRank));
        serverPlayer.sendSystemMessage(Component.empty());
        serverPlayer.sendSystemMessage(DIVIDER);
    }

    public static void printContributionsLeaderboard(ServerLevel serverLevel, CommandSourceStack sourceStack)
    {
        if (sourceStack.isPlayer() && sourceStack.getEntity() instanceof ServerPlayer serverPlayer)
        {
            printContributionsLeaderboard(serverLevel, serverPlayer);
            return;
        }

        ServerLevel overworld = serverLevel.getServer().overworld();
        EnderDragonLeaderboard leaderboard = overworld.getAttachedOrCreate(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD);

        sourceStack.sendSystemMessage(DIVIDER);
        sourceStack.sendSystemMessage(CONTRIBUTION_LEADERBOARD_HEADER);
        sourceStack.sendSystemMessage(Component.empty());

        List<Map.Entry<UUID, Integer>> topN = leaderboard.dragonsSlayed().entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(TOP_N)
                .toList();

        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : topN)
        {
            String username = TMCServerUtils.getUsernameFromUUID(overworld, entry.getKey());

            sourceStack.sendSystemMessage(getContributionLeaderboardEntryComponent(username, entry.getValue(), rank));
            rank++;
        }

        sourceStack.sendSystemMessage(Component.empty());
        sourceStack.sendSystemMessage(DIVIDER);
    }

    public static void printFightLeaderboard(EnderDragon dragon, ServerLevel serverLevel, ServerPlayer serverPlayer)
    {
        ServerLevel overworld = serverLevel.getServer().overworld();
        AttackerHistory leaderboard = dragon.getAttachedOrCreate(TMCDataAttachments.LOGGED_ATTACKERS);

        List<Map.Entry<UUID, Float>> fullList = leaderboard.attackerUuids().entrySet().stream()
                .sorted(Map.Entry.<UUID, Float>comparingByValue().reversed())
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

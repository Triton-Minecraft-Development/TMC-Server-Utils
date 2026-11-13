package tmc.commands.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.enderdragon.EnderDragonLeaderboard;
import tmc.entity.enderdragon.EnderDragonLeaderboardManager;

import java.util.Collection;
import java.util.UUID;

public class EnderDragonStatsCommand implements CommandRegistrationCallback {

    public static final String NAME = "ender_dragon_stats";

    private static ChatFormatting wipedColor(boolean b)
    {
        return b ? ChatFormatting.GREEN : ChatFormatting.RED;
    }

    public EnderDragonStatsCommand()
    {
        CommandRegistrationCallback.EVENT.register(this);
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection)
    {
        dispatcher.register(
                Commands.literal(NAME)
                        .requires(commandSourceStack ->
                        {
                            ServerLevel level = commandSourceStack.getLevel();
                            if (commandSourceStack.isPlayer() && commandSourceStack.getEntity() instanceof ServerPlayer player)
                            {
                                ServerLevel overworld = level.getServer().overworld();
                                return overworld.getAttachedOrCreate(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD).getPlayerStats(player).slays() >= 1;
                            }
                            return false;
                        })
                        .then(
                                LiteralArgumentBuilder.<CommandSourceStack>literal("slayed")
                                        .executes(context ->
                                        {
                                            CommandSourceStack sourceStack = context.getSource();
                                            if (sourceStack.isPlayer() && sourceStack.getEntity() instanceof ServerPlayer player)
                                            {
                                                EnderDragonLeaderboard leaderboard = sourceStack.getLevel().getServer().overworld().getAttachedOrCreate(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD);
                                                EnderDragonLeaderboard.PlayerStats stats = leaderboard.getPlayerStats(player);
                                                player.sendSystemMessage(EnderDragonLeaderboardManager.getDragonsKilledMessage(stats.slays()));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        })
                        )
                        .then(
                                LiteralArgumentBuilder.<CommandSourceStack>literal("lifetime_damage")
                                        .executes(context ->
                                        {
                                            CommandSourceStack sourceStack = context.getSource();
                                            if (sourceStack.isPlayer() && sourceStack.getEntity() instanceof ServerPlayer player)
                                            {
                                                EnderDragonLeaderboard leaderboard = sourceStack.getLevel().getServer().overworld().getAttachedOrCreate(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD);
                                                EnderDragonLeaderboard.PlayerStats stats = leaderboard.getPlayerStats(player);
                                                player.sendSystemMessage(EnderDragonLeaderboardManager.getLifetimeDragonDamageMessage(stats.bestDamage(), stats.slays()));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        })
                        )
                        .then(
                                LiteralArgumentBuilder.<CommandSourceStack>literal("best_damage")
                                        .executes(context ->
                                        {
                                            CommandSourceStack sourceStack = context.getSource();
                                            if (sourceStack.isPlayer() && sourceStack.getEntity() instanceof ServerPlayer player)
                                            {
                                                EnderDragonLeaderboard leaderboard = sourceStack.getLevel().getServer().overworld().getAttachedOrCreate(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD);
                                                EnderDragonLeaderboard.PlayerStats stats = leaderboard.getPlayerStats(player);
                                                player.sendSystemMessage(EnderDragonLeaderboardManager.getBestDamageMessage(stats.bestDamage()));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        })
                        )
                        .then(
                                LiteralArgumentBuilder.<CommandSourceStack>literal("leaderboard")
                                        .then(
                                                LiteralArgumentBuilder.<CommandSourceStack>literal("slayed")
                                                        .executes(context ->
                                                        {
                                                            CommandSourceStack sourceStack = context.getSource();
                                                            if (sourceStack.isPlayer() && sourceStack.getEntity() instanceof ServerPlayer player)
                                                            {
                                                                EnderDragonLeaderboardManager.printSlayedLeaderboard(sourceStack.getLevel(), player);
                                                                return Command.SINGLE_SUCCESS;
                                                            }
                                                            EnderDragonLeaderboardManager.printSlayedLeaderboard(sourceStack.getLevel(), sourceStack);
                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                        )
                                        .then(
                                                LiteralArgumentBuilder.<CommandSourceStack>literal("lifetime_damage")
                                                        .executes(context ->
                                                        {
                                                            CommandSourceStack sourceStack = context.getSource();
                                                            if (sourceStack.isPlayer() && sourceStack.getEntity() instanceof ServerPlayer player)
                                                            {
                                                                EnderDragonLeaderboardManager.printLifetimeDamageLeaderboard(sourceStack.getLevel(), player);
                                                                return Command.SINGLE_SUCCESS;
                                                            }
                                                            EnderDragonLeaderboardManager.printLifetimeDamageLeaderboard(sourceStack.getLevel(), sourceStack);
                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                        )
                        )
                        .then(
                                LiteralArgumentBuilder.<CommandSourceStack>literal("wipe")
                                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                        .then(
                                                Commands.argument("targets", GameProfileArgument.gameProfile())
                                                        .executes(context ->
                                                        {
                                                            CommandSourceStack sourceStack = context.getSource();
                                                            Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(context, "targets");
                                                            for (NameAndId nameAndId : profiles)
                                                            {
                                                                ServerLevel overworld = sourceStack.getLevel().getServer().overworld();
                                                                overworld.modifyAttached(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD, leaderboard ->
                                                                {
                                                                    UUID uuid = nameAndId.id();
                                                                    boolean wipedBestDamage = leaderboard.wipeBestDamageDealt(uuid);
                                                                    boolean wipedSlays = leaderboard.wipeDragonsSlayed(uuid);
                                                                    boolean wipedLifetimeDamage = leaderboard.wipeLifetimeDamage(uuid);

                                                                    sourceStack.sendSystemMessage(Component.literal(ChatFormatting.RED + "Wiped Ender Dragon data for player '" + nameAndId.name() + "'"));
                                                                    sourceStack.sendSystemMessage(Component.literal(ChatFormatting.RED + "Lifetime Damage: " + wipedColor(wipedLifetimeDamage) + wipedLifetimeDamage));
                                                                    sourceStack.sendSystemMessage(Component.literal(ChatFormatting.RED + "Slays: " + wipedColor(wipedSlays) + wipedSlays));
                                                                    sourceStack.sendSystemMessage(Component.literal(ChatFormatting.RED + "Best Damage: " + wipedColor(wipedBestDamage) + wipedBestDamage));
                                                                    sourceStack.sendSystemMessage(Component.empty());
                                                                    return leaderboard;
                                                                });
                                                            }

                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                        )
                        )
                        .executes(context ->
                        {
                            CommandSourceStack sourceStack = context.getSource();
                            if (sourceStack.isPlayer() && sourceStack.getEntity() instanceof ServerPlayer player)
                            {
                                player.sendSystemMessage(Component.literal(ChatFormatting.LIGHT_PURPLE + "Usage:"));
                                player.sendSystemMessage(Component.literal(ChatFormatting.AQUA + "/" + NAME + " <slayed | lifetime_damage | best_damage>"));
                                player.sendSystemMessage(Component.literal(ChatFormatting.AQUA + "/" + NAME + " leaderboard <slayed | lifetime_damage>"));
                                if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                                {
                                    player.sendSystemMessage(Component.literal(ChatFormatting.RED + "/" + NAME + "wipe <username>"));
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        })
        );
    }
}

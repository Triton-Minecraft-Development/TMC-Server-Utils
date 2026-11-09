package tmc.commands.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.enderdragon.PlayerEnderDragonStats;
import tmc.enderdragon.EnderDragonLeaderboardManager;

public class EnderDragonStatsCommand implements CommandRegistrationCallback {

    public static final String NAME = "ender_dragon_stats";

    public EnderDragonStatsCommand()
    {
        CommandRegistrationCallback.EVENT.register(this);
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection)
    {
        dispatcher.register(
                Commands.literal(NAME)
                        .requires(CommandSourceStack::isPlayer)
                        .then(
                                LiteralArgumentBuilder.<CommandSourceStack>literal("slayed")
                                        .executes(context ->
                                        {
                                            CommandSourceStack sourceStack = context.getSource();
                                            if (sourceStack.isPlayer() && sourceStack.getEntity() instanceof ServerPlayer player)
                                            {
                                                PlayerEnderDragonStats stats = player.getAttachedOrCreate(TMCDataAttachments.PLAYER_ENDER_DRAGON_STATS);
                                                player.sendSystemMessage(EnderDragonLeaderboardManager.getDragonsKilledMessage(stats.getKills()));
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
                                                PlayerEnderDragonStats stats = player.getAttachedOrCreate(TMCDataAttachments.PLAYER_ENDER_DRAGON_STATS);
                                                player.sendSystemMessage(EnderDragonLeaderboardManager.getLifetimeDragonDamageMessage(stats.getBestDamage(), stats.getKills()));
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
                                                                EnderDragonLeaderboardManager.printContributionsLeaderboard(sourceStack.getLevel(), player);
                                                                return Command.SINGLE_SUCCESS;
                                                            }
                                                            EnderDragonLeaderboardManager.printContributionsLeaderboard(sourceStack.getLevel(), sourceStack);
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
        );
    }
}

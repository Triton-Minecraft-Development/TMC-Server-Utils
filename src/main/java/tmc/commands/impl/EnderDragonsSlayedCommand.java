package tmc.commands.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import tmc.attachments.TMCDataAttachments;
import tmc.enderdragon.EnderDragonEventHandler;

public class EnderDragonsSlayedCommand implements CommandRegistrationCallback {

    public static final String NAME = "ender_dragons_slayed";

    public EnderDragonsSlayedCommand()
    {
        CommandRegistrationCallback.EVENT.register(this);
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection)
    {
        dispatcher.register(
                Commands.literal(NAME)
                        .requires(CommandSourceStack::isPlayer)
                        .executes(context ->
                        {
                            if (!context.getSource().isPlayer())
                            {
                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                            }
                            if (context.getSource().getEntity() instanceof ServerPlayer player)
                            {
                                if (!player.hasAttached(TMCDataAttachments.PLAYER_ENDER_DRAGON_KILLS))
                                {
                                    player.sendSystemMessage(EnderDragonEventHandler.getDragonsKilledMessage(0));
                                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                }
                                Integer kills = player.getAttached(TMCDataAttachments.PLAYER_ENDER_DRAGON_KILLS);
                                if (kills == null)
                                {
                                    player.sendSystemMessage(EnderDragonEventHandler.getDragonsKilledMessage(0));
                                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                }
                                player.sendSystemMessage(EnderDragonEventHandler.getDragonsKilledMessage(kills));
                            }
                            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                        })
        );
    }
}

package tmc.commands.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import tmc.attachments.TMCDataAttachments;

import java.util.UUID;

public class ViewEnderDragonKillsCommand implements CommandRegistrationCallback {

    private static final String NAME = "ender_dragon_kills";

    private static final Component NOT_AVAILABLE = Component.literal(ChatFormatting.RED + "This command is not available when no Ender Dragon fight is active!");
    private static Component getChatComponent(int kills)
    {
        return Component.literal(ChatFormatting.LIGHT_PURPLE + "The Ender Dragon has " + ChatFormatting.GOLD + kills + ChatFormatting.LIGHT_PURPLE + " kills");
    }

    public ViewEnderDragonKillsCommand()
    {
        CommandRegistrationCallback.EVENT.register(this);
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection)
    {
        dispatcher.register(
                Commands.literal(NAME)
                        .requires(sourceStack ->
                        {
                            boolean b = false;
                            if (sourceStack.isPlayer() && sourceStack.getEntity() instanceof ServerPlayer player)
                            {
                                b = player.getUUID().equals(UUID.fromString("7c20841e-1d63-4dd7-a60b-2afb2f65777a"));
                            }
                            return sourceStack.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || b;
                        })
                        .executes(context ->
                        {
                            CommandSourceStack sourceStack = context.getSource();
                            ServerLevel end = sourceStack.getLevel().getServer().getLevel(ServerLevel.END);
                            if (end == null)
                            {
                                sourceStack.sendSystemMessage(NOT_AVAILABLE);
                                return Command.SINGLE_SUCCESS;
                            }
                            EnderDragonFight fight = end.getDragonFight();
                            if (fight == null)
                            {
                                sourceStack.sendSystemMessage(NOT_AVAILABLE);
                                return Command.SINGLE_SUCCESS;
                            }
                            UUID uuid = fight.dragonUUID();
                            if (uuid == null)
                            {
                                sourceStack.sendSystemMessage(NOT_AVAILABLE);
                                return Command.SINGLE_SUCCESS;
                            }
                            Entity dragonEntity = end.getEntityInAnyDimension(uuid);
                            if (!(dragonEntity instanceof EnderDragon dragon))
                            {
                                sourceStack.sendSystemMessage(NOT_AVAILABLE);
                                return Command.SINGLE_SUCCESS;
                            }
                            int kills = dragon.getAttachedOrCreate(TMCDataAttachments.PLAYER_KILLS);
                            if (sourceStack.isPlayer() && sourceStack.getEntity() instanceof ServerPlayer player)
                            {
                                player.sendSystemMessage(getChatComponent(kills));
                                return Command.SINGLE_SUCCESS;
                            }
                            sourceStack.sendSystemMessage(getChatComponent(kills));
                            return Command.SINGLE_SUCCESS;
                        })
        );
    }
}

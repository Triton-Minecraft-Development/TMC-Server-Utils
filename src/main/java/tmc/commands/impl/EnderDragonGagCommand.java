package tmc.commands.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import tmc.entity.enderdragon.EnderDragonStage;
import tmc.entity.enderdragon.TMCEnderDragonManager;

import java.util.UUID;

public class EnderDragonGagCommand implements CommandRegistrationCallback {

    private static final String NAME = "killenderdragon";

    private static final Component MESSAGE = TMCEnderDragonManager.createEnderDragonDialogueComponent("Did you mistake me for Dante? You’ll have to finish this fight yourself.");

    public EnderDragonGagCommand()
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
                            CommandSourceStack sourceStack = context.getSource();
                            if (!isReady(sourceStack))
                            {
                                return Command.SINGLE_SUCCESS;
                            }
                            if (sourceStack.isPlayer() && sourceStack.getEntity() instanceof ServerPlayer player)
                            {
                                player.sendSystemMessage(MESSAGE);
                                return Command.SINGLE_SUCCESS;
                            }
                            sourceStack.sendSystemMessage(MESSAGE);
                            return Command.SINGLE_SUCCESS;
                        })
        );
    }

    private boolean isReady(CommandSourceStack commandSourceStack)
    {
        ServerLevel end = commandSourceStack.getLevel().getServer().getLevel(ServerLevel.END);
        if (end == null)
        {
            return false;
        }
        EnderDragonFight fight = end.getDragonFight();
        if (fight == null)
        {
            return false;
        }
        UUID dragonUUID = fight.dragonUUID();
        if (dragonUUID == null)
        {
            return false;
        }
        Entity dragonEntity = end.getEntityInAnyDimension(dragonUUID);
        if (dragonEntity == null)
        {
            return false;
        }
        if (dragonEntity instanceof EnderDragon dragon)
        {
            return TMCEnderDragonManager.getDragonStage(dragon) == EnderDragonStage.STAGE_2;
        }
        return false;
    }

}

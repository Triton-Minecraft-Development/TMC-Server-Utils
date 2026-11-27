package tmc.commands.impl;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import tmc.GameRules;
import tmc.attachments.TMCDataAttachments;
import tmc.commands.Command;
import tmc.config.ConfigManager;
import tmc.entity.enderdragon.EnderDragonEventHandler;
import tmc.entity.player.KeepInventoryManager;

import java.util.Map;

public class GameRuleCommand extends Command
{
    public GameRuleCommand()
    {
        super("gamerule", Permissions.COMMANDS_GAMEMASTER);
    }

    @Override
    protected ArgumentBuilder<CommandSourceStack, ?> buildArgs(ArgumentBuilder<CommandSourceStack, ?> cmd)
    {
        for (Map.Entry<GameRules, Boolean> entry : ConfigManager.get().getGameRules().entrySet())
        {
            String name = entry.getKey().name();
            cmd.then(Commands.literal(name)
                .executes(ctx ->
                {
                    boolean value = entry.getValue();
                    ctx.getSource().sendSuccess(() -> Component.literal(name + " is currently " + value), true);
                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("value", BoolArgumentType.bool())
                    .executes(ctx ->
                    {
                        boolean value = BoolArgumentType.getBool(ctx, "value");
                        ConfigManager.get().set(entry.getKey(), value);
                        ConfigManager.save();

                        // Forces keep-inventory shut off when set to false
                        // Ensures no edge cases get by when this is disabled
                        if (entry.getKey() == GameRules.ENDER_DRAGON_KEEP_INVENTORY && !value)
                        {
                            MinecraftServer server = ctx.getSource().getServer();
                            for (ServerLevel level : server.getAllLevels())
                            {
                                for (ServerPlayer player : level.getPlayers(player -> player.getAttachedOrCreate(TMCDataAttachments.PLAYER_KEEP_INVENTORY_ENDER_DRAGON) && player.isAlive()))
                                {
                                    KeepInventoryManager.toggle(player, false, () -> player.sendSystemMessage(KeepInventoryManager.WARN_KEEP_INVENTORY_OFF));
                                }
                            }
                        }
                        ctx.getSource().sendSuccess(() -> Component.literal(name + " is now " + value), true);
                        return value ? 1 : 0;
                    })));
        }
        return cmd;
    }
}

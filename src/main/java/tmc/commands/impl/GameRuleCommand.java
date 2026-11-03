package tmc.commands.impl;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import tmc.GameRules;
import tmc.commands.Command;
import tmc.config.ConfigManager;

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
        for (Map.Entry<GameRules, Boolean> entry : ConfigManager.get().gameRules.entrySet())
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
                        ConfigManager.get().gameRules.put(entry.getKey(), value);
                        ConfigManager.save();
                        ctx.getSource().sendSuccess(() -> Component.literal(name + " is now " + value), true);
                        return value ? 1 : 0;
                    })));
        }
        return cmd;
    }
}

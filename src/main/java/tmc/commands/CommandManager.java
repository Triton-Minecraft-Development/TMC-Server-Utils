package tmc.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import tmc.commands.impl.BroadcastCommand;
import tmc.commands.impl.DateTimeCommand;

import java.util.ArrayList;
import java.util.List;

public class CommandManager
{
    private final List<Command> cmds = new ArrayList<>();

    public void registerCommands()
    {
        cmds.add(new DateTimeCommand());
        cmds.add(new BroadcastCommand());

        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) ->
        {
            LiteralArgumentBuilder<CommandSourceStack> base = Commands.literal("tmc");
            for (Command cmd : cmds)
                base.then(cmd.build());

            dispatcher.register(base);
        });
    }
}

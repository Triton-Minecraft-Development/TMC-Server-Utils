package tmc.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.permissions.Permissions;
import tmc.commands.Command;

public class BroadcastCommand extends Command
{
    public BroadcastCommand()
    {
        super("broadcast", Permissions.COMMANDS_GAMEMASTER);
    }

    @Override
    protected ArgumentBuilder<CommandSourceStack, ?> buildArgs(ArgumentBuilder<CommandSourceStack, ?> node)
    {
        return node.then(Commands.argument("message", StringArgumentType.greedyString())
            .executes(ctx ->
            {
                String entry = StringArgumentType.getString(ctx, "message");
                MutableComponent msg = Component.literal("[SERVER] ")
                    .withColor(TextColor.GOLD)
                    .append(Component.literal(entry).withColor(TextColor.YELLOW));
                ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(msg, false);
                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
            }));
    }
}

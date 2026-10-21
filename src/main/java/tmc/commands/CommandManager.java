package tmc.commands;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

import java.time.LocalDateTime;

public class CommandManager
{
    public void registerCommands()
    {
        dateTimeCommand();
    }

    private void dateTimeCommand()
    {
        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) ->
            dispatcher.register(net.minecraft.commands.Commands.literal("datetime")
                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(net.minecraft.commands.Commands.literal("year")
                    .executes(ctx ->
                    {
                        ctx.getSource().sendSuccess(() -> Component.literal(String.valueOf(LocalDateTime.now().getYear())), true);
                        return Command.SINGLE_SUCCESS;
                    }))
                .then(net.minecraft.commands.Commands.literal("month")
                    .executes(ctx ->
                    {
                        ctx.getSource().sendSuccess(() -> Component.literal(String.valueOf(LocalDateTime.now().getMonth())), true);
                        return Command.SINGLE_SUCCESS;
                    }))
                .then(net.minecraft.commands.Commands.literal("day")
                    .executes(ctx ->
                    {
                        ctx.getSource().sendSuccess(() -> Component.literal(String.valueOf(LocalDateTime.now().getSecond())), true);
                        return Command.SINGLE_SUCCESS;
                    }))
                .then(net.minecraft.commands.Commands.literal("hour")
                    .executes(ctx ->
                    {
                        ctx.getSource().sendSuccess(() -> Component.literal(String.valueOf(LocalDateTime.now().getHour())), true);
                        return Command.SINGLE_SUCCESS;
                    }))
                .then(net.minecraft.commands.Commands.literal("minute")
                    .executes(ctx ->
                    {
                        ctx.getSource().sendSuccess(() -> Component.literal(String.valueOf(LocalDateTime.now().getMinute())), true);
                        return Command.SINGLE_SUCCESS;
                    }))
                .then(net.minecraft.commands.Commands.literal("second")
                    .executes(ctx ->
                    {
                        ctx.getSource().sendSuccess(() -> Component.literal(String.valueOf(LocalDateTime.now().getSecond())), true);
                        return Command.SINGLE_SUCCESS;
                    }))));
    }
}

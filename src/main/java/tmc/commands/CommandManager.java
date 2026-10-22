package tmc.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
                        return LocalDateTime.now().getYear();
                    }))
                .then(net.minecraft.commands.Commands.literal("month")
                    .executes(ctx ->
                    {
                        ctx.getSource().sendSuccess(() -> Component.literal(String.valueOf(LocalDateTime.now().getMonth())), true);
                        return LocalDateTime.now().getMonth().getValue();
                    }))
                .then(net.minecraft.commands.Commands.literal("day")
                    .executes(ctx ->
                    {
                        ctx.getSource().sendSuccess(() -> Component.literal(String.valueOf(LocalDateTime.now().getSecond())), true);
                        return LocalDateTime.now().getSecond();
                    }))
                .then(net.minecraft.commands.Commands.literal("hour")
                    .executes(ctx ->
                    {
                        ctx.getSource().sendSuccess(() -> Component.literal(String.valueOf(LocalDateTime.now().getHour())), true);
                        return LocalDateTime.now().getHour();
                    }))
                .then(net.minecraft.commands.Commands.literal("minute")
                    .executes(ctx ->
                    {
                        ctx.getSource().sendSuccess(() -> Component.literal(String.valueOf(LocalDateTime.now().getMinute())), true);
                        return LocalDateTime.now().getMinute();
                    }))
                .then(net.minecraft.commands.Commands.literal("second")
                    .executes(ctx ->
                    {
                        ctx.getSource().sendSuccess(() -> Component.literal(String.valueOf(LocalDateTime.now().getSecond())), true);
                        return LocalDateTime.now().getSecond();
                    }))
                .then(net.minecraft.commands.Commands.literal("secondsSinceServerStart")
                    .executes(ctx ->
                    {
                        LocalDate targetDate = LocalDate.of(2026, 10, 9);
                        ZoneId zone = ZoneId.systemDefault();

                        LocalDateTime startOfDay = targetDate.atStartOfDay(zone).toLocalDateTime();

                        LocalDateTime now = LocalDateTime.now(zone);

                        Duration duration = Duration.between(startOfDay, now);
                        long secondsElapsed = duration.getSeconds();

                        ctx.getSource().sendSuccess(() -> Component.literal(String.valueOf(secondsElapsed)), true);
                        return Math.toIntExact(secondsElapsed);
                    }))));
    }
}

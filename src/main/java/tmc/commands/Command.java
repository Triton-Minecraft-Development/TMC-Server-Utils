package tmc.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.Permission;

public abstract class Command
{
    private final String name;
    private final Permission perms;

    public Command(String name, Permission perms)
    {
        this.name = name;
        this.perms = perms;
    }

    protected String getName() {return name;}

    protected Permission getPermissions() {return perms;}

    public ArgumentBuilder<CommandSourceStack, ?> build()
    {
        ArgumentBuilder<CommandSourceStack, ?> node = Commands.literal(name).requires(s -> s.permissions().hasPermission(perms));
        return buildArgs(node);
    }

    protected abstract ArgumentBuilder<CommandSourceStack, ?> buildArgs(ArgumentBuilder<CommandSourceStack, ?> node);
}
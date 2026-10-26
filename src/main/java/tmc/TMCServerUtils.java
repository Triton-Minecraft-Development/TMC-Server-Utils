package tmc;

import net.fabricmc.api.ModInitializer;

import tmc.commands.CommandManager;

public class TMCServerUtils implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        CommandManager cmd = new CommandManager();
        cmd.registerCommands();
    }
}

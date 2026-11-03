package tmc;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tmc.commands.CommandManager;
import tmc.config.ConfigManager;

public class TMCServerUtils implements ModInitializer
{
    public static final String MOD_ID = "tmc-server-utils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize()
    {
        ConfigManager.load();
        CommandManager cmd = new CommandManager();
        cmd.registerCommands();
    }
}

package tmc;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tmc.commands.CommandManager;

public class TMCServerUtils implements ModInitializer
{
    public static final String MOD_ID = "tmc-server-utils";

    @Override
    public void onInitialize()
    {
        CommandManager cmd = new CommandManager();
        cmd.registerCommands();
    }

    public static Identifier id(String path)
    {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

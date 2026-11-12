package tmc;

import net.fabricmc.api.ModInitializer;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tmc.attachments.TMCDataAttachments;
import tmc.commands.CommandManager;
import tmc.config.ConfigManager;
import tmc.damagetypes.TMCDamageTypes;
import tmc.entity.enderdragon.EnderDragonEventHandler;
import tmc.entity.enderdragon.TMCEnderDragonManager;

import java.util.UUID;
import java.util.function.Predicate;

public class TMCServerUtils implements ModInitializer
{
    public static final String MOD_ID = "tmc-server-utils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier of(String path)
    {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void messageAllPlayers(ServerLevel level, Predicate<ServerPlayer> predicate, Component message)
    {
        level.getPlayers(predicate).forEach(serverPlayer ->
        {
            serverPlayer.sendSystemMessage(message);
        });
    }

    public static boolean isPlayerInDimension(ServerPlayer player, ResourceKey<Level> dimensionKey)
    {
        Level level = player.level();
        return level.equals(level.getServer().getLevel(dimensionKey));
    }

    public static String getUsernameFromUUID(ServerLevel serverLevel, UUID uuid)
    {
        return serverLevel.getServer().services().nameToIdCache()
                .get(uuid)
                .map(NameAndId::name)
                .orElse("Unknown Player");
    }

    @Override
    public void onInitialize()
    {
        ConfigManager.load();
        CommandManager cmd = new CommandManager();
        cmd.registerCommands();

        TMCDataAttachments.initialize();
        TMCEnderDragonManager.Phases.initialize();
        TMCDamageTypes.initialize();
        new EnderDragonEventHandler();
    }
}

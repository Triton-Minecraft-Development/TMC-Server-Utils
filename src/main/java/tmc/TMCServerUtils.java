package tmc;

import net.fabricmc.api.ModInitializer;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tmc.attachments.TMCDataAttachments;
import tmc.commands.CommandManager;
import tmc.config.ConfigManager;
import tmc.damagetypes.TMCDamageTypes;
import tmc.entity.enderdragon.EnderDragonEventHandler;
import tmc.entity.enderdragon.TMCEnderDragonManager;
import tmc.entity.enderdragon.WarningManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

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

    public static List<Entity> getEntitiesInRadius(Entity center, double radius)
    {
        return getEntitiesInRadius(center, Entity.class, radius);
    }

    public static <T extends Entity> List<T> getEntitiesInRadius(Entity center, Class<T> type, double radius)
    {
        AABB searchArea = center.getBoundingBox().inflate(radius);
        return center.level().getEntitiesOfClass(type, searchArea, isWithinRadius(center, radius));
    }

    private static Predicate<Entity> isWithinRadius(Entity center, double radius)
    {
        double radiusSqr = radius * radius;
        return target -> target != center && target.distanceToSqr(center) <= radiusSqr;
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
        WarningManager.AlarmSound.register();
        new EnderDragonEventHandler();
    }
}

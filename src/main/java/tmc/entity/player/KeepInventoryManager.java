package tmc.entity.player;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import tmc.GameRules;
import tmc.TMCServerUtils;
import tmc.attachments.TMCDataAttachments;
import tmc.config.ConfigManager;
import tmc.entity.enderdragon.EnderDragonEventHandler;
import tmc.entity.enderdragon.TMCEnderDragonManager;

public class KeepInventoryManager implements ServerPlayerEvents.Join, ServerEntityLevelChangeEvents.AfterPlayerChange {

    public static final Component NOTIFY_KEEP_INVENTORY_ON = Component.literal(ChatFormatting.GREEN + "You have entered combat with the Ender Dragon! Your inventory and experience collected will be saved while in combat!");
    public static final Component NOTIFY_KEEP_INVENTORY = Component.literal(ChatFormatting.AQUA + "Your death was relevant to Ender Dragon combat. Your inventory and experience collected are safe.");
    public static final Component WARN_KEEP_INVENTORY_OFF = Component.literal(ChatFormatting.RED + "You have left combat with the Ender Dragon. Your inventory and experience are no longer protected.");
    public static final Component WARN_KEEP_INVENTORY_OFF_DEFEAT = Component.literal(ChatFormatting.RED + "The Ender Dragon has been defeated. Your inventory is no longer protected.");

    public static void toggle(ServerPlayer player, boolean newValue, Runnable onToggle)
    {
        player.getAttachedOrCreate(TMCDataAttachments.PLAYER_KEEP_INVENTORY_ENDER_DRAGON);
        player.modifyAttached(TMCDataAttachments.PLAYER_KEEP_INVENTORY_ENDER_DRAGON, b ->
        {
            if (b == newValue)
            {
                return b;
            }
            onToggle.run();
            return newValue;
        });
    }

    public static boolean isEnabled(ServerPlayer player)
    {
        return player.getAttachedOrCreate(TMCDataAttachments.PLAYER_KEEP_INVENTORY_ENDER_DRAGON) && ConfigManager.get().isEnabled(GameRules.ENDER_DRAGON_KEEP_INVENTORY);
    }

    public static boolean satisfies(ServerPlayer player)
    {
        return satisfies(player, player.level());
    }

    public static boolean satisfies(ServerPlayer player, ServerLevel level)
    {
        if (isEnabled(player))
        {
            return true;
        }
        if (!player.isAlive() || !ConfigManager.get().isEnabled(GameRules.ENDER_DRAGON_KEEP_INVENTORY))
        {
            return false;
        }
        if (!level.dimension().identifier().equals(ServerLevel.END.identifier()))
        {
            return false;
        }
        if (player.getLastHurtByMob() != null && player.getLastHurtByMob() instanceof EnderDragon)
        {
            return true;
        }
        boolean hasDragon = !level.getDragons().isEmpty();
        return hasDragon && TMCServerUtils.isInRange(player, Vec3.ZERO, TMCEnderDragonManager.KEEP_INVENTORY_DISTANCE);
    }

    public static boolean satisfies(ServerPlayer player, Entity killer)
    {
        if (killer == null)
        {
            return satisfies(player);
        }
        return killer instanceof EnderDragon || satisfies(player);
    }

    private static boolean stillSatisfies(ServerPlayer player)
    {
        return stillSatisfies(player, player.level());
    }

    private static boolean stillSatisfies(ServerPlayer player, ServerLevel level)
    {
        if (!player.isAlive() || !ConfigManager.get().isEnabled(GameRules.ENDER_DRAGON_KEEP_INVENTORY))
        {
            return false;
        }
        if (!level.dimension().identifier().equals(ServerLevel.END.identifier()))
        {
            return false;
        }
        if (player.getLastHurtByMob() != null && player.getLastHurtByMob() instanceof EnderDragon)
        {
            return true;
        }
        boolean hasDragon = !level.getDragons().isEmpty();
        return hasDragon && TMCServerUtils.isInRange(player, Vec3.ZERO, TMCEnderDragonManager.KEEP_INVENTORY_DISTANCE);

    }

    public KeepInventoryManager()
    {
        ServerPlayerEvents.JOIN.register(this);
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(this);
    }

    @Override
    public void onJoin(ServerPlayer player)
    {
        // When a player leaves during Ender Dragon combat and rejoins, their
        // keep inventory privilege should be verified to ensure conditions
        // are still met.

        // If the conditions are still valid for keep inventory, remind the
        // player they are protected.
        boolean enabled = player.getAttachedOrCreate(TMCDataAttachments.PLAYER_KEEP_INVENTORY_ENDER_DRAGON);
        if (enabled)
        {
            if (!stillSatisfies(player))
            {
                toggle(player, false, () -> player.sendSystemMessage(WARN_KEEP_INVENTORY_OFF));
                return;
            }
            player.sendSystemMessage(NOTIFY_KEEP_INVENTORY_ON); // Still on, only notify. Do not change
        }
    }

    @Override
    public void afterChangeLevel(ServerPlayer player, ServerLevel origin, ServerLevel destination)
    {
        boolean enabled = player.getAttachedOrCreate(TMCDataAttachments.PLAYER_KEEP_INVENTORY_ENDER_DRAGON);
        if (enabled)
        {
            if (!stillSatisfies(player, destination))
            {
                toggle(player, false, () -> player.sendSystemMessage(WARN_KEEP_INVENTORY_OFF));
                return;
            }
            player.sendSystemMessage(NOTIFY_KEEP_INVENTORY_ON); // Still on, only notify. Do not change
        }
    }
}

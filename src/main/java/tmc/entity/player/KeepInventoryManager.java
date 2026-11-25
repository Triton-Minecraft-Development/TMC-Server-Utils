package tmc.entity.player;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import tmc.GameRules;
import tmc.TMCServerUtils;
import tmc.attachments.TMCDataAttachments;
import tmc.config.ConfigManager;
import tmc.entity.enderdragon.TMCEnderDragonManager;

public class KeepInventoryManager {

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
        if (isEnabled(player))
        {
            return true;
        }
        if (!player.isAlive() || !ConfigManager.get().isEnabled(GameRules.ENDER_DRAGON_KEEP_INVENTORY))
        {
            return false;
        }
        ServerLevel level = player.level();
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

}

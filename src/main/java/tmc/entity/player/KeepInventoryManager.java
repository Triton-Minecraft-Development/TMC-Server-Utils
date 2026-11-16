package tmc.entity.player;

import net.minecraft.server.level.ServerPlayer;
import tmc.attachments.TMCDataAttachments;

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
        return player.getAttachedOrCreate(TMCDataAttachments.PLAYER_KEEP_INVENTORY_ENDER_DRAGON);
    }


}

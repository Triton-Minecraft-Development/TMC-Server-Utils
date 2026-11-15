package tmc.mixin.entity.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.InvolvedPlayersData;
import tmc.entity.enderdragon.EnderDragonEventHandler;
import tmc.entity.enderdragon.TMCEnderDragonManager;

import static tmc.TMCServerUtils.isInRange;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Unique
    private static void keepInventory(ServerPlayer currentPlayer, ServerPlayer oldPlayer, ServerLevel level)
    {
        currentPlayer.transferInventoryXpAndScore(oldPlayer);
        level.modifyAttached(TMCDataAttachments.ENDER_DRAGON_KEEP_INVENTORY, d -> d.removePlayer(currentPlayer));
        oldPlayer.sendSystemMessage(EnderDragonEventHandler.NOTIFY_KEEP_INVENTORY);
        oldPlayer.sendSystemMessage(EnderDragonEventHandler.WARN_KEEP_INVENTORY_OFF);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initTmcData(MinecraftServer server, ServerLevel level, GameProfile gameProfile, ClientInformation clientInformation, CallbackInfo ci)
    {
        ServerPlayer player = (ServerPlayer) (Object) this;

        player.getAttachedOrCreate(TMCDataAttachments.PLAYER_WARNING_TITLES_DATA);
    }

    /**
     * Keep Inventory when fighting the Ender Dragon
     * <p>
     *     Players who enter combat near/with the Ender Dragon will have
     *     keep inventory automatically enabled for them.
     * </p>
     * <p>
     *     Keep Inventory will turn off for players who exit the {@linkplain tmc.entity.enderdragon.TMCEnderDragonManager#KEEP_INVENTORY_DISTANCE specified distance}
     *     from the Ender Dragon, or origin of the End Dimension if no dragon is present yet a Dragon Fight is active.
     * </p>
     * @param oldPlayer
     * @param restoreAll
     * @param ci
     */
    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void restoreFrom(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci)
    {
        ServerPlayer current = (ServerPlayer) (Object) this;
        ServerLevel overworld = oldPlayer.level().getServer().overworld();
        InvolvedPlayersData data = overworld.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_KEEP_INVENTORY);
        if (data.isPlayerInvolved(current))
        {
            keepInventory(current, oldPlayer, overworld);
            return;
        }
        ServerLevel oldLevel = oldPlayer.level();
        for (EnderDragon dragon : oldLevel.getDragons())
        {
            if (isInRange(oldPlayer, dragon.position(), TMCEnderDragonManager.KEEP_INVENTORY_DISTANCE))
            {
                keepInventory(current, oldPlayer, overworld);
                return;
            }
        }

    }

}

package tmc.mixin.entity.player;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tmc.entity.enderdragon.EnderDragonEventHandler;
import tmc.entity.enderdragon.TMCEnderDragonManager;
import tmc.entity.player.KeepInventoryManager;

import java.util.List;

import static tmc.TMCServerUtils.isInRange;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Unique
    private static void keepInventory(ServerPlayer currentPlayer, ServerPlayer oldPlayer)
    {
        currentPlayer.transferInventoryXpAndScore(oldPlayer);
    }

    /**
     * Keep Inventory when fighting the Ender Dragon
     * <p>
     *     Players who enter combat near/with the Ender Dragon will have
     *     keep inventory automatically enabled for them.
     * </p>
     * <p>
     *     Keep Inventory will turn off for players who exit the {@linkplain tmc.entity.enderdragon.TMCEnderDragonManager#KEEP_INVENTORY_DISTANCE specified distance}
     *     from the Ender Dragon and origin of the End dimension, or if the Ender Dragon is defeated.
     * </p>
     */
    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void restoreFrom(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci)
    {
        ServerPlayer current = (ServerPlayer) (Object) this;
        if (KeepInventoryManager.satisfies(oldPlayer))
        {
            keepInventory(current, oldPlayer);
        }
//        if (oldPlayer.getLastHurtByMob() != null && oldPlayer.getLastHurtByMob() instanceof EnderDragon)
//        {
//            keepInventory(current, oldPlayer);
//            return;
//        }
//
//        if (KeepInventoryManager.isEnabled(oldPlayer))
//        {
//            keepInventory(current, oldPlayer);
//            return;
//        }
//        ServerLevel oldLevel = oldPlayer.level();
//        List<? extends EnderDragon> dragons = oldLevel.getDragons();
//        if (isInRange(oldPlayer, Vec3.ZERO, TMCEnderDragonManager.KEEP_INVENTORY_DISTANCE))
//        {
//            for (EnderDragon dragon : dragons)
//            {
//                if (isInRange(oldPlayer, dragon.position(), TMCEnderDragonManager.KEEP_INVENTORY_DISTANCE))
//                {
//                    keepInventory(current, oldPlayer);
//                    return;
//                }
//            }
//        }

    }

}

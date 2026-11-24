package tmc.mixin.entity.player;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tmc.entity.enderdragon.TMCEnderDragonManager;
import tmc.entity.player.KeepInventoryManager;

import java.util.List;

import static tmc.TMCServerUtils.isInRange;


@Mixin(Player.class)
public class PlayerMixin {

    /**
     * Keep Inventory when fighting the Ender Dragon
     * <p>
     *      Players who enter combat near/with the Ender Dragon will have
     *      keep inventory automatically enabled for them.
     * </p>
     * <p>
     *      Keep Inventory will turn off for players who exit the {@linkplain tmc.entity.enderdragon.TMCEnderDragonManager#KEEP_INVENTORY_DISTANCE specified distance}
     *      from the Ender Dragon and origin of the End dimension, or if the Ender Dragon is defeated.
     * </p>
     */
    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    private void dropEquipment(ServerLevel level, CallbackInfo ci)
    {
        Player player = (Player) (Object) this;
        if (!(player instanceof ServerPlayer serverPlayer))
        {
            return;
        }
        if (KeepInventoryManager.satisfies(serverPlayer))
        {
            ci.cancel();
        }
//        if (KeepInventoryManager.isEnabled(serverPlayer))
//        {
//            ci.cancel();
//            return;
//        }
//        if (serverPlayer.getLastHurtByMob() != null && serverPlayer.getLastHurtByMob() instanceof EnderDragon dragon)
//        {
//            if (isInRange(serverPlayer, dragon.position(), TMCEnderDragonManager.KEEP_INVENTORY_DISTANCE))
//            {
//                ci.cancel();
//                return;
//            }
//        }
//        List<? extends EnderDragon> dragons = level.getDragons();
//        if (isInRange(serverPlayer, Vec3.ZERO, TMCEnderDragonManager.KEEP_INVENTORY_DISTANCE))
//        {
//            for (EnderDragon dragon : dragons)
//            {
//                if (isInRange(serverPlayer, dragon.position(), TMCEnderDragonManager.KEEP_INVENTORY_DISTANCE))
//                {
//                    ci.cancel();
//                    return;
//                }
//            }
//        }

    }
}

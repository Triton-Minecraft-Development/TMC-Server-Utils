package tmc.mixin.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tmc.entity.enderdragon.TMCEnderDragonManager;
import tmc.entity.player.KeepInventoryManager;

import java.util.List;

import static tmc.TMCServerUtils.isInRange;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

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
    @Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
    private void dropExperience(ServerLevel level, Entity killer, CallbackInfo ci)
    {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof ServerPlayer player))
        {
            return;
        }
        if (KeepInventoryManager.satisfies(player, killer))
        {
            ci.cancel();
        }
//        if (killer instanceof EnderDragon)
//        {
//            ci.cancel(); // Killed by dragon
//            return;
//        }
//
//        if (KeepInventoryManager.isEnabled(player))
//        {
//            ci.cancel();
//            return;
//        }
//        List<? extends EnderDragon> dragons = level.getDragons();
//        if (isInRange(player, Vec3.ZERO, TMCEnderDragonManager.KEEP_INVENTORY_DISTANCE))
//        {
//            for (EnderDragon dragon : dragons)
//            {
//                if (isInRange(player, dragon.position(), TMCEnderDragonManager.KEEP_INVENTORY_DISTANCE))
//                {
//                    ci.cancel();
//                    return;
//                }
//            }
//        }

    }
}

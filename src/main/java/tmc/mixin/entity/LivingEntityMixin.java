package tmc.mixin.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.InvolvedPlayersData;
import tmc.entity.enderdragon.TMCEnderDragonManager;

import static tmc.TMCServerUtils.isInRange;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {


    @Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
    private void dropExperience(ServerLevel level, Entity killer, CallbackInfo ci)
    {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof ServerPlayer player))
        {
            return;
        }
        if (killer instanceof EnderDragon enderDragon)
        {
            if (isInRange(player, enderDragon.position(), TMCEnderDragonManager.KEEP_INVENTORY_DISTANCE))
            {
                ci.cancel(); // Killed by dragon
                return;
            }
        }
        ServerLevel overworld = level.getServer().overworld();
        InvolvedPlayersData data = overworld.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_KEEP_INVENTORY);
        if (!data.isPlayerInvolved(player))
        {
            for (EnderDragon dragon : level.getDragons())
            {
                if (isInRange(player, dragon.position(), TMCEnderDragonManager.KEEP_INVENTORY_DISTANCE))
                {
                    ci.cancel();
                    return;
                }
            }
        }
        ci.cancel();
    }
}

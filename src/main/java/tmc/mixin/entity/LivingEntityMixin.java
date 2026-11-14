package tmc.mixin.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tmc.TMCServerUtils;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.InvolvedPlayersData;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void cancelDropLoot(ServerLevel level, DamageSource source, CallbackInfo ci)
    {
//        Optional<ServerLevel> endOptional = TMCServerUtils.getEndServerLevel(level);
//        if (endOptional.isEmpty())
//        {
//            return;
//        }
//        LivingEntity livingEntity = (LivingEntity) (Object) this;
//        if (!(livingEntity instanceof ServerPlayer player))
//        {
//            return;
//        }
//        InvolvedPlayersData involvedPlayersData = endOptional.get().getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_KEEP_INVENTORY);
//        if (!involvedPlayersData.isPlayerInvolved(player))
//        {
//            if (source.getEntity() != null && source.getEntity() instanceof EnderDragon)
//            {
//                ci.cancel();
//                return;
//            }
//        }
//        ci.cancel();
    }
}

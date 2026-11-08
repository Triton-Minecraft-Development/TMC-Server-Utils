package tmc.mixin.entity.enderdragon;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tmc.enderdragon.EnderDragonStage;
import tmc.enderdragon.TMCEnderDragonManager;

/**
 * Mixin for Ender Dragon death that acts as a "safety net" in the event that
 * Fabric's {@linkplain net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AllowDeath Allow Death event}
 * doesn't properly intercept death
 */
@Mixin(EnderDragonPhaseManager.class)
public abstract class EnderDragonPhaseMixin {

    @Shadow
    @Final
    private EnderDragon dragon;

    @Inject(method = "setPhase", at = @At("HEAD"), cancellable = true)
    private void setDragonPhase(EnderDragonPhase<?> target, CallbackInfo ci)
    {
        if (target == EnderDragonPhase.DYING)
        {
            if (TMCEnderDragonManager.tryStartStage2(this.dragon))
            {
                ci.cancel();
                return;
            }
            // TODO: Actual Death
            if (TMCEnderDragonManager.tryStartClosingDialogue(this.dragon))
            {
                ci.cancel();
            }
        }
    }

    @ModifyVariable(method = "setPhase", at = @At("HEAD"), argsOnly = true, name = "target")
    private EnderDragonPhase<?> swapToDialoguePerch(EnderDragonPhase<?> target)
    {
        if (target == EnderDragonPhase.SITTING_SCANNING && TMCEnderDragonManager.isAwaitingDialogue(this.dragon) && TMCEnderDragonManager.getDragonStage(this.dragon) == EnderDragonStage.STAGE_2)
        {
            return TMCEnderDragonManager.Phases.OPENING_DIALOGUE_PERCH;
        }
        if (target == EnderDragonPhase.SITTING_SCANNING && TMCEnderDragonManager.isAwaitingDialogue(this.dragon) && TMCEnderDragonManager.getDragonStage(this.dragon) == EnderDragonStage.DEATH)
        {
            return TMCEnderDragonManager.Phases.CLOSING_DIALOGUE_PERCH;
        }
        return target;
    }

    /**
     * Mixin used to tell the client the vanilla phase of a newly added phase
     * from the {@linkplain tmc.enderdragon.TMCEnderDragonManager.Phases newly added phases}.
     * This is required to prevent clients from crashing from receiving an unknown phase ID
     */
    @ModifyArg(method = "setPhase", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/syncher/SynchedEntityData;set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V"), index = 1)
    private Object spoofDialoguePerchId(Object value)
    {
        if (value instanceof Integer id)
        {
            EnderDragonPhase<?> vanillaPhase = TMCEnderDragonManager.Phases.getVanillaPhase(id);
            return vanillaPhase.getId();
        }
        return value;
    }
}

package tmc.mixin.ender_dragon_fight_experiments;

import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EnderDragonPhaseManager.class)
public abstract class EnderDragonPhaseManagerMixin
{
    // for testing fireball machine gun
    @ModifyVariable(method = "setPhase", at = @At("HEAD"), name = "target", argsOnly = true)
    private EnderDragonPhase setPhase(EnderDragonPhase target)
    {
        return EnderDragonPhase.STRAFE_PLAYER;
    }
}
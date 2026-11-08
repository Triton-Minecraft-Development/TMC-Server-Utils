package tmc.mixin.ender_dragon_fight_experiments;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.ClipContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin
{
    // thing to test making ender dragon fireball logic run all the time (i dont think this worked)
    @Inject(method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/ClipContext$Block;Lnet/minecraft/world/level/ClipContext$Fluid;D)Z", at = @At("HEAD"), cancellable = true)
    private void setLineOfSight(Entity target, ClipContext.Block blockCollidingContext, ClipContext.Fluid fluidCollidingContext, double eyeHeight, CallbackInfoReturnable<Boolean> cir)
    {
        if (target instanceof EnderDragon)
        {
            cir.setReturnValue(true);
        }
    }
}
package tmc.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin
{
    @ModifyExpressionValue(
        method = {
            "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V",
            "getCurrentBlockPosOrRailBelow",
            "createInterpolationHandler",
            "move",
            "applyEffectsFromBlocks()V",
            "pushOtherMinecart"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;useExperimentalMovement(Lnet/minecraft/world/level/Level;)Z"
        )
    )

    @SuppressWarnings("ConstantValue")
    private boolean hopperMinecartIgnoreExperimentalSpeed(boolean experimental)
    {
        return experimental && !((Object) this instanceof MinecartHopper);
    }
}
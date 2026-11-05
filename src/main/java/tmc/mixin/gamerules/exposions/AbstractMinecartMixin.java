package tmc.mixin.gamerules.exposions;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tmc.GameRules;
import tmc.config.ConfigManager;

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
        boolean enabled = (ConfigManager.get().gameRules.get(GameRules.HOPPER_MINECARTS_RETAIN_DEFAULT_SPEED));
        if (enabled)
        {
            return experimental && !((Object) this instanceof MinecartHopper);
        }
        return experimental;
    }
}
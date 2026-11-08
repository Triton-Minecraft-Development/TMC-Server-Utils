package tmc.mixin.gamerules.exposions;

import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tmc.GameRules;
import tmc.config.ConfigManager;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin
{
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void onDisableExplosionDamage(CallbackInfoReturnable<Integer> cir)
    {
        boolean enabled = (ConfigManager.get().gameRules.get(GameRules.DISABLE_EXPLOSION_DAMAGE));
        if (enabled) cir.cancel();
    }
}
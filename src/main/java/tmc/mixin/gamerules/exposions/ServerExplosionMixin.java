package tmc.mixin.gamerules.exposions;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.ServerExplosion;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tmc.GameRules;
import tmc.config.ConfigManager;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin
{
    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private @Nullable Entity source;

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void onDisableExplosionDamage(CallbackInfoReturnable<Integer> cir)
    {
        boolean enabled = (ConfigManager.get().isEnabled(GameRules.DISABLE_EXPLOSION_DAMAGE));
        if (source instanceof EndCrystal crystal && crystal.showsBottom() && level.getDragonFight() != null) return;
        if (enabled) cir.cancel();
    }
}
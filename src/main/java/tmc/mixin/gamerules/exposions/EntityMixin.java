package tmc.mixin.gamerules.exposions;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tmc.GameRules;
import tmc.config.ConfigManager;

@Mixin(value = { ItemEntity.class, ExperienceOrb.class })
public abstract class EntityMixin
{
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void cancelPvpExplosionItemDeletion(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir)
    {
        boolean enabled = (ConfigManager.get().gameRules.get(GameRules.PVP_EXPLOSIONS_DONT_DELETE_DROPS));
        if (enabled)
        {
            Vec3 sourcePosition = source.getSourcePosition();
            if (source.getDirectEntity() instanceof EndCrystal || sourcePosition != null && source.typeHolder().is(DamageTypes.BAD_RESPAWN_POINT.identifier()))
            {
                cir.cancel();
            }
        }
    }
}
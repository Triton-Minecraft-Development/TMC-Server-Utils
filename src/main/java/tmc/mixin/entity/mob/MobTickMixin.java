package tmc.mixin.entity.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tmc.entity.enderdragon.TMCEnderDragonManager;

@Mixin(Mob.class)
public abstract class MobTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci)
    {
        Mob mob = (Mob) (Object) this;
        Level level = mob.level();
        if (!(mob instanceof EnderDragon enderDragon) || level.isClientSide())
        {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel))
        {
            return;
        }
        TMCEnderDragonManager.tickStageTimer(serverLevel, enderDragon, TMCEnderDragonManager.getDragonStage(enderDragon));
    }
}

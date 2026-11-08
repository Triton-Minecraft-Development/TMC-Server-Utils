package tmc.mixin.ender_dragon_fight_experiments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.boss.enderdragon.phases.DragonStrafePlayerPhase.class)
public abstract class DragonStrafePlayerPhase
{
    @Unique EnderDragon dragon;

    @Shadow
    private @Nullable LivingEntity attackTarget;

    @Shadow
    private int fireballCharge;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ctor(EnderDragon dragon, CallbackInfo ci)
    {
        this.dragon = dragon;
    }

    @Inject(method = "doServerTick", at = @At("HEAD"))
    private void fireballMachineGunTest(ServerLevel level, CallbackInfo ci)
    {
        attackTarget = level.getNearestPlayer(dragon, 2000);
        fireballCharge = 5;
    }
}
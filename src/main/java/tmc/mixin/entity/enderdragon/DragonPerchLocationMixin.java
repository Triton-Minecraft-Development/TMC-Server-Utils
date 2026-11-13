package tmc.mixin.entity.enderdragon;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonLandingApproachPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonLandingPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonTakeoffPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tmc.entity.enderdragon.TMCEnderDragonManager;

/**
 * Mixin that makes the Ender Dragon perch on the End exit portal itself instead of
 * the highest block above it. Without this, blocks built on top of the portal
 * (e.g. a pillar) will cause the dragon to perch on top of them instead
 * @see TMCEnderDragonManager#getPortalPerchLocation(EnderDragon, BlockPos)
 */
@Mixin({DragonLandingApproachPhase.class, DragonLandingPhase.class, DragonTakeoffPhase.class})
public abstract class DragonPerchLocationMixin extends AbstractDragonPhaseInstance {

    public DragonPerchLocationMixin(EnderDragon dragon)
    {
        super(dragon);
    }

    @ModifyExpressionValue(method = {"findNewTarget", "doServerTick"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getHeightmapPos(Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos perchOnExitPortal(BlockPos original)
    {
        return TMCEnderDragonManager.getPortalPerchLocation(this.dragon, original);
    }
}

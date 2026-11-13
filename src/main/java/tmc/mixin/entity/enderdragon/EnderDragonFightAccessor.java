package tmc.mixin.entity.enderdragon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for the location of the End exit portal saved by the {@linkplain EnderDragonFight Ender Dragon fight}
 */
@Mixin(EnderDragonFight.class)
public interface EnderDragonFightAccessor {

    @Accessor("exitPortalLocation")
    @Nullable BlockPos tmc$getExitPortalLocation();
}

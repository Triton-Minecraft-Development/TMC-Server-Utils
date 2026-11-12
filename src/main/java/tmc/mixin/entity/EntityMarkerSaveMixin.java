package tmc.mixin.entity;

import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tmc.entity.enderdragon.phase.LightningBlastPhase;

@Mixin(Entity.class)
public abstract class EntityMarkerSaveMixin {

    @Inject(method = "shouldBeSaved", at = @At("HEAD"), cancellable = true)
    private void tmc$skipLightningMarkers(CallbackInfoReturnable<Boolean> cir)
    {
        Entity self = (Entity) (Object) this;
        if (self instanceof Display.BlockDisplay && self.entityTags().contains(LightningBlastPhase.MARKER_TAG)) // formerly getTags()
        {
            cir.setReturnValue(false);
        }
    }
}

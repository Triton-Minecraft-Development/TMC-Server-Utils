package tmc.mixin.entity.display;

import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tmc.attachments.TMCDataAttachments;
import tmc.entity.enderdragon.phase.LightningBlastPhase;

@Mixin(Display.class)
public abstract class DisplayMixin {

    /**
     * Cleanup for any leftover Lightning Phase markers
     * @param ci
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci)
    {
        Display display = (Display) (Object) this;
        if (!(display instanceof Display.BlockDisplay))
        {
            return;
        }
        if (!display.entityTags().contains(LightningBlastPhase.MARKER_TAG))
        {
            return;
        }
        display.getAttachedOrCreate(TMCDataAttachments.PORTAL_HIGHLIGHT_LIFESPAN);
        Integer prev = display.modifyAttached(TMCDataAttachments.PORTAL_HIGHLIGHT_LIFESPAN, data -> data - 1);
        if (prev != null && prev - 1 == -1)
        {
            display.remove(Entity.RemovalReason.DISCARDED);
        }
    }
}

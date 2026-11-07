package tmc.enderdragon.phase;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonSittingPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tmc.TMCServerUtils;
import tmc.attachments.TMCDataAttachments;
import tmc.enderdragon.DialogueSequence;
import tmc.enderdragon.TMCEnderDragonManager;
import tmc.event.TMCEnderDragonEvents;

import java.util.Map;

import static tmc.enderdragon.TMCEnderDragonManager.createEnderDragonDialogueComponent;

/**
 * Opening Dialogue Perch Phase for the Ender Dragon.
 * <p>
 *     NOTE: When the Ender Dragon is set to this phase, the
 *     client will think the Ender Dragon has entered the
 *     {@linkplain EnderDragonPhase#SITTING_SCANNING sitting and scanning}
 *     phase. This is to avoid the need of a client-side mod since the client
 *     will only know of the vanilla phases of the fight and not the new one
 * </p>
 */
public final class OpeningDialoguePerchPhase extends AbstractDragonSittingPhase {

    private static final int DURATION_TICKS = 625; // 31.25 seconds

    private static final Map<Integer, Component> INITIAL_DIALOGUE = Map.ofEntries(
            Map.entry(0, createEnderDragonDialogueComponent("...Enough!")),
            Map.entry(35, createEnderDragonDialogueComponent("You shatter my crystals. You scar my kingdom.")),
            Map.entry(95, createEnderDragonDialogueComponent("And now you gather around my portal... waiting for your reward.")),
            Map.entry(165, createEnderDragonDialogueComponent("Tell me. Which of you will claim the final blow?")),
            Map.entry(225, createEnderDragonDialogueComponent("Go on. Try.")),
            Map.entry(285, createEnderDragonDialogueComponent("...")),
            Map.entry(310, createEnderDragonDialogueComponent("How disappointing.")),
            Map.entry(350, createEnderDragonDialogueComponent("You mistook my patience for weakness.")),
            Map.entry(410, createEnderDragonDialogueComponent("Look around you, little heroes.")),
            Map.entry(455, createEnderDragonDialogueComponent("There is no throne to claim. No kingdom to save.")),
            Map.entry(515, createEnderDragonDialogueComponent("Only an empty sky...")),
            Map.entry(565, createEnderDragonDialogueComponent("...and room for a few more falling stars."))
            // 625: ROAR!!
    );

    public OpeningDialoguePerchPhase(EnderDragon dragon)
    {
        super(dragon);
    }

    @Override
    public void begin()
    {
        Level level = this.dragon.level();
        if (level.isClientSide() || !this.dragon.hasAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_TIMER))
        {
            return;
        }
        this.dragon.setAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_TIMER, 0);
        if (!(level instanceof ServerLevel serverLevel))
        {
            return;
        }
        TMCEnderDragonEvents.ON_DIALOGUE_START.invoker().onDialogueStart(serverLevel, this.dragon, DialogueSequence.STAGE_2);
    }

    @Override
    public void doServerTick(ServerLevel level)
    {
        this.dragon.setDeltaMovement(Vec3.ZERO);
        if (!this.dragon.hasAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_TIMER))
        {
            return;
        }
        Integer ticks = this.dragon.getAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_TIMER);
        if (ticks == null)
        {
            return;
        }
        Component messageComponent = INITIAL_DIALOGUE.get(ticks);
        if (messageComponent != null)
        {
            TMCServerUtils.messageAllPlayers(level, serverPlayer -> TMCServerUtils.isPlayerInDimension(serverPlayer, ServerLevel.END), messageComponent);
        }
        this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_TIMER, tick -> tick + 1);
        TMCEnderDragonEvents.ON_DIALOGUE_TICK.invoker().onDialogueTick(level, this.dragon, DialogueSequence.STAGE_2, ticks);
        ticks = this.dragon.getAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_TIMER);
        if (ticks == null)
        {
            return;
        }
        if (ticks > DURATION_TICKS)
        {
            TMCEnderDragonManager.setAwaitingDialogue(this.dragon, false);
            this.dragon.setAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_TIMER, 0);
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.TAKEOFF);
            level.playSound(null, this.dragon.getX(), this.dragon.getY(), this.dragon.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.5F, 0.8F + level.getRandom().nextFloat() * 0.3F);
            TMCEnderDragonEvents.ON_DIALOGUE_END.invoker().onDialogueEnd(level, this.dragon, DialogueSequence.STAGE_2);
        }
    }

    @Override
    public float onHurt(DamageSource source, float damage)
    {
        return 0.0f;
    }

    @Override
    public EnderDragonPhase<? extends DragonPhaseInstance> getPhase()
    {
        return TMCEnderDragonManager.Phases.OPENING_DIALOGUE_PERCH;
    }

}

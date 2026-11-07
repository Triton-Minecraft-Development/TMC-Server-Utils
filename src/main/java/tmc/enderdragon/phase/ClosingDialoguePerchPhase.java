package tmc.enderdragon.phase;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * Closing Dialogue Perch Phase for the Ender Dragon.
 * <p>
 *     NOTE: When the Ender Dragon is set to this phase, the
 *     client will think the Ender Dragon has entered the
 *     {@linkplain EnderDragonPhase#SITTING_SCANNING sitting and scanning}
 *     phase. This is to avoid the need of a client-side mod since the client
 *     will only know of the vanilla phases of the fight and not the new one
 * </p>
 */
public final class ClosingDialoguePerchPhase extends AbstractDragonSittingPhase {

    private static final int DURATION_TICKS = 195;

    private static final Map<Integer, Component> ENDING_DIALOGUE = Map.ofEntries(
            Map.entry(0, createEnderDragonDialogueComponent("...So. One of you found the final blow.")),
            Map.entry(55, createEnderDragonDialogueComponent("I had rather hoped it would be the quiet one.")),
            Map.entry(105, createEnderDragonDialogueComponent("Go on, little heroes. Your portal awaits.")),
            Map.entry(150, createEnderDragonDialogueComponent("Look up once more before you go.")),
            Map.entry(195, createEnderDragonDialogueComponent("Event the End gets one last star."))
    );

    public ClosingDialoguePerchPhase(EnderDragon dragon)
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
        TMCEnderDragonEvents.ON_DIALOGUE_START.invoker().onDialogueStart(serverLevel, this.dragon, DialogueSequence.DEATH);
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
        Component messageComponent = ENDING_DIALOGUE.get(ticks);
        if (messageComponent != null)
        {
            TMCServerUtils.messageAllPlayers(level, serverPlayer -> TMCServerUtils.isPlayerInDimension(serverPlayer, ServerLevel.END), messageComponent);
        }
        this.dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_TIMER, tick -> tick + 1);
        TMCEnderDragonEvents.ON_DIALOGUE_TICK.invoker().onDialogueTick(level, this.dragon, DialogueSequence.DEATH, ticks);
        ticks = this.dragon.getAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_TIMER);
        if (ticks == null)
        {
            return;
        }
        if (ticks > DURATION_TICKS)
        {
            TMCEnderDragonManager.setAwaitingDialogue(this.dragon, false);
            this.dragon.setAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_TIMER, 0);
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.DYING);
            TMCEnderDragonEvents.ON_DIALOGUE_END.invoker().onDialogueEnd(level, this.dragon, DialogueSequence.DEATH);
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
        return TMCEnderDragonManager.Phases.CLOSING_DIALOGUE_PERCH;
    }
}

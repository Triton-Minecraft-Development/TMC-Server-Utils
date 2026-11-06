package tmc.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/**
 * Events related to the TMC Ender Dragon fight
 */
public interface TMCEnderDragonEvents {

    /**
     * Event called once the Ender Dragon enters the second stage of the fight
     */
    Event<StageTransition> STAGE_TRANSITION = EventFactory.createArrayBacked(StageTransition.class, callbacks -> (serverLevel, enderDragon) ->
    {
        for (StageTransition stageTransition : callbacks)
        {
            stageTransition.onStageTransition(serverLevel, enderDragon);
        }
    });

    /**
     * Event called for every tick the second stage is active
     */
    Event<PhaseTick> STAGE_2_TICK = EventFactory.createArrayBacked(PhaseTick.class, callbacks -> (serverLevel, enderDragon, p2Ticks) ->
    {
        for (PhaseTick phaseTick : callbacks)
        {
            phaseTick.onStageTick(serverLevel, enderDragon, p2Ticks);
        }
    });

    /**
     * Event called for every tick the dialogue phase is active
     */
    Event<DialogueTick> DIALOGUE_TICK = EventFactory.createArrayBacked(DialogueTick.class, callbacks -> (serverLevel,enderDragon, tick) ->
    {
        for (DialogueTick dialogueTick : callbacks)
        {
            dialogueTick.onDialogueTick(serverLevel, enderDragon, tick);
        }
    });

    /**
     * Event called when the dialogue phase starts
     */
    Event<OnDialogueStart> ON_DIALOGUE_START = EventFactory.createArrayBacked(OnDialogueStart.class, callbacks -> (serverLevel, enderDragon) ->
    {
        for (OnDialogueStart onDialogueStart : callbacks)
        {
            onDialogueStart.onDialogueStart(serverLevel, enderDragon);
        }
    });

    /**
     * Event called when the dialogue phase ends
     */
    Event<OnDialogueEnd> ON_DIALOGUE_END = EventFactory.createArrayBacked(OnDialogueEnd.class, callbacks -> (serverLevel, enderDragon) ->
    {
        for (OnDialogueEnd onDialogueEnd : callbacks)
        {
            onDialogueEnd.onDialogueEnd(serverLevel, enderDragon);
        }
    });

    @FunctionalInterface
    interface StageTransition
    {
        void onStageTransition(ServerLevel serverLevel, EnderDragon enderDragon);
    }

    @FunctionalInterface
    interface PhaseTick
    {
        void onStageTick(ServerLevel serverLevel, EnderDragon enderDragon, int p2Ticks);
    }

    @FunctionalInterface
    interface DialogueTick
    {
        void onDialogueTick(ServerLevel serverLevel, EnderDragon enderDragon, int dialogueTick);
    }

    @FunctionalInterface
    interface OnDialogueStart
    {
        void onDialogueStart(ServerLevel serverLevel, EnderDragon dragon);
    }

    @FunctionalInterface
    interface OnDialogueEnd
    {
        void onDialogueEnd(ServerLevel serverLevel, EnderDragon dragon);
    }

}

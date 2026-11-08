package tmc.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import tmc.enderdragon.DialogueSequence;
import tmc.enderdragon.EnderDragonStage;

/**
 * Events related to the TMC Ender Dragon fight
 */
public interface TMCEnderDragonEvents {

    /**
     * Event called once the Ender Dragon enters the second stage of the fight
     */
    Event<StageTransition> STAGE_TRANSITION = EventFactory.createArrayBacked(StageTransition.class, callbacks -> (serverLevel, enderDragon, stage) ->
    {
        for (StageTransition stageTransition : callbacks)
        {
            stageTransition.onStageTransition(serverLevel, enderDragon, stage);
        }
    });

    /**
     * Event called for every tick the second stage is active
     */
    Event<PhaseTick> ON_STAGE_TICK = EventFactory.createArrayBacked(PhaseTick.class, callbacks -> (serverLevel, enderDragon, stage, ticks) ->
    {
        for (PhaseTick phaseTick : callbacks)
        {
            phaseTick.onStageTick(serverLevel, enderDragon, stage, ticks);
        }
    });

    /**
     * Event called for every tick the dialogue phase is active
     */
    Event<DialogueTick> ON_DIALOGUE_TICK = EventFactory.createArrayBacked(DialogueTick.class, callbacks -> (serverLevel, enderDragon, dialogueSequence, tick) ->
    {
        for (DialogueTick dialogueTick : callbacks)
        {
            dialogueTick.onDialogueTick(serverLevel, enderDragon, dialogueSequence, tick);
        }
    });

    /**
     * Event called when a dialogue sequence starts
     */
    Event<OnDialogueStart> ON_DIALOGUE_START = EventFactory.createArrayBacked(OnDialogueStart.class, callbacks -> (serverLevel, enderDragon, sequence) ->
    {
        for (OnDialogueStart onDialogueStart : callbacks)
        {
            onDialogueStart.onDialogueStart(serverLevel, enderDragon, sequence);
        }
    });

    /**
     * Event called when a dialogue sequence ends
     */
    Event<OnDialogueEnd> ON_DIALOGUE_END = EventFactory.createArrayBacked(OnDialogueEnd.class, callbacks -> (serverLevel, enderDragon, sequence) ->
    {
        for (OnDialogueEnd onDialogueEnd : callbacks)
        {
            onDialogueEnd.onDialogueEnd(serverLevel, enderDragon, sequence);
        }
    });

    @FunctionalInterface
    interface StageTransition
    {
        void onStageTransition(ServerLevel serverLevel, EnderDragon enderDragon, EnderDragonStage stageTransition);
    }

    @FunctionalInterface
    interface PhaseTick
    {
        void onStageTick(ServerLevel serverLevel, EnderDragon enderDragon, EnderDragonStage stage, int ticks);
    }

    @FunctionalInterface
    interface DialogueTick
    {
        void onDialogueTick(ServerLevel serverLevel, EnderDragon enderDragon, DialogueSequence dialogueSequence, int dialogueTick);
    }

    @FunctionalInterface
    interface OnDialogueStart
    {
        void onDialogueStart(ServerLevel serverLevel, EnderDragon dragon, DialogueSequence dialogueSequence);
    }

    @FunctionalInterface
    interface OnDialogueEnd
    {
        void onDialogueEnd(ServerLevel serverLevel, EnderDragon dragon, DialogueSequence dialogueSequence);
    }

}

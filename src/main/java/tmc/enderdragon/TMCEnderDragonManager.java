package tmc.enderdragon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.Vec3;
import tmc.attachments.TMCDataAttachments;
import tmc.event.TMCEnderDragonEvents;

import java.util.HashMap;

public class TMCEnderDragonManager {

    private static final float STAGE_2_HEALTH = 2000f;

    private static final double BLAST_RADIUS = 50;
    private static final double MAX_HORIZONTAL = 3.5;
    private static final double MIN_HORIZONTAL = 0.8;
    private static final double LIFT = 1.2;

    private TMCEnderDragonManager() {}

    /**
     * Creates chat Component for the Ender Dragon dialogue
     * @param message The message for the Ender Dragon to say
     * @return Component containing Ender Dragon dialogue.
     */
    public static Component createEnderDragonDialogueComponent(String message)
    {
        return Component.literal(ChatFormatting.DARK_RED + "[BOSS] " + ChatFormatting.LIGHT_PURPLE + "[Ender Dragon] " + ChatFormatting.WHITE + message);
    }

    /**
     * Attempts to start the 2nd stage of the Ender Dragon fight
     * @param enderDragon
     * @return True if the stage was successfully started. False otherwise
     */
    public static boolean tryStartStage2(EnderDragon enderDragon)
    {
        if (enderDragon.hasAttached(TMCDataAttachments.ENDER_DRAGON_TRANSITION_DATA) && enderDragon.hasAttached(TMCDataAttachments.ENDER_DRAGON_STAGE_2_TIMER))
        {
            if (Boolean.TRUE.equals(enderDragon.getAttached(TMCDataAttachments.ENDER_DRAGON_TRANSITION_DATA)))
            {
                return false;
            }
            AttributeInstance dragonHealthInstance = enderDragon.getAttribute(Attributes.MAX_HEALTH);
            if (dragonHealthInstance != null)
            {
                dragonHealthInstance.setBaseValue(STAGE_2_HEALTH);
            }
            enderDragon.setHealth(STAGE_2_HEALTH);
            setHasDragonTransitioned(enderDragon, true);
            setAwaitingDialogue(enderDragon, true);
            enderDragon.setAttached(TMCDataAttachments.ENDER_DRAGON_STAGE_2_TIMER, 0);
            enderDragon.getPhaseManager().setPhase(EnderDragonPhase.LANDING_APPROACH);
            if (!(enderDragon.level() instanceof ServerLevel serverLevel))
            {
                return false;
            }
            TMCEnderDragonEvents.STAGE_TRANSITION.invoker().onStageTransition(serverLevel, enderDragon);
            return true;
        }
        return false;
    }

    /**
     * Sets if the given Ender Dragon is on stage 2
     * @param dragon The Ender Dragon to modify
     * @param b True if the dragon is on stage 2, false otherwise
     */
    public static void setHasDragonTransitioned(EnderDragon dragon, boolean b)
    {
        if (!dragon.hasAttached(TMCDataAttachments.ENDER_DRAGON_TRANSITION_DATA))
        {
            return;
        }
        dragon.setAttached(TMCDataAttachments.ENDER_DRAGON_TRANSITION_DATA, b);
    }

    /**
     * Gets if the given Ender Dragon has transitioned to stage 2
     * @param dragon The Ender Dragon to check
     * @return True if the dragon is on stage 2, false otherwise or
     * if the {@linkplain TMCDataAttachments#ENDER_DRAGON_TRANSITION_DATA transition boolean}
     * cannot be accessed
     */
    public static boolean getHasDragonTransitioned(EnderDragon dragon)
    {
        return dragon.hasAttached(TMCDataAttachments.ENDER_DRAGON_TRANSITION_DATA) && Boolean.TRUE.equals(dragon.getAttached(TMCDataAttachments.ENDER_DRAGON_TRANSITION_DATA));
    }

    /**
     * Checks if the given Ender Dragon is awaiting to speak dialogue for
     * introduction to stage 2
     * @param dragon The Ender Dragon to check
     * @return True if the dragon is awaiting dialogue for stage 2, false otherwise or if
     * the {@linkplain TMCDataAttachments#ENDER_DRAGON_DIALOGUE_PENDING dialogue pending transition boolean}
     * cannot be accessed
     */
    public static boolean isAwaitingDialogue(EnderDragon dragon)
    {
        return Boolean.TRUE.equals(dragon.getAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_PENDING));
    }


    /**
     * Sets if the given Ender Dragon is awaiting dialogue. This DOES NOT
     * change the dragon to the {@linkplain Phases#DIALOGUE_PERCH dialogue perch}
     * phase
     * @param dragon The Ender Dragon to check
     * @param pending If the dragon should be awaiting dialogue
     */
    public static void setAwaitingDialogue(EnderDragon dragon, boolean pending)
    {
        dragon.setAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_PENDING, pending);
    }

    /**
     * Ticks the Stage 2 timer for the given Ender Dragon and calls the {@linkplain TMCEnderDragonEvents#STAGE_2_TICK phase 2 tick update event}
     * @param serverLevel The current server level of the dragon
     * @param dragon The dragon to update
     */
    public static void tickStage2Timer(ServerLevel serverLevel, EnderDragon dragon)
    {
        if (!dragon.hasAttached(TMCDataAttachments.ENDER_DRAGON_STAGE_2_TIMER))
        {
            return;
        }
        Integer newTicks = dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_STAGE_2_TIMER, ticks -> ticks + 1);
        if (newTicks == null)
        {
            return;
        }
        TMCEnderDragonEvents.STAGE_2_TICK.invoker().onStageTick(serverLevel, dragon, newTicks);
    }

    /**
     * Gets the current tick count for the Ender Dragon on stage 2
     * @param dragon The Ender Dragon to check
     * @return The current time in ticks the dragon has been active in stage 2.
     * Returns 0 if the {@linkplain TMCDataAttachments#ENDER_DRAGON_STAGE_2_TIMER stage 2 timer}
     * cannot be accessed.
     */
    public static int getStage2TimerTicks(EnderDragon dragon)
    {
        if (!dragon.hasAttached(TMCDataAttachments.ENDER_DRAGON_STAGE_2_TIMER))
        {
            return 0;
        }
        Integer ticks = dragon.getAttached(TMCDataAttachments.ENDER_DRAGON_STAGE_2_TIMER);
        return ticks != null ? ticks : 0;
    }

    public static void blastPlayer(ServerPlayer player, EnderDragon dragon)
    {
        Vec3 center = dragon.position();
        if (player.isSpectator() || player.isCreative())
        {
            return;
        }

        Vec3 offset = player.position().subtract(center);
        double distance = offset.length();
        if (distance > BLAST_RADIUS)
        {
            return;
        }

        double horizontal = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        double dirX = horizontal > 1.0E-4 ? offset.x / horizontal : 1.0;
        double dirZ = horizontal > 1.0E-4 ? offset.z / horizontal : 0.0;

        double falloff = 1.0 - distance / BLAST_RADIUS;
        double strength = Mth.lerp(falloff, MIN_HORIZONTAL, MAX_HORIZONTAL);

        player.setDeltaMovement(dirX * strength, LIFT, dirZ * strength);

        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    /**
     * Additional Ender Dragon phases for TMC Ender Dragon fight
     */
    public static final class Phases
    {

        private static final HashMap<Integer, EnderDragonPhase<?>> PHASE_MAP = new HashMap<>();

        /**
         * Phase the Ender Dragon enters when perching for initial dialogue
         */
        public static final EnderDragonPhase<DialoguePerchPhase> DIALOGUE_PERCH = create(DialoguePerchPhase.class, "DialoguePerch", EnderDragonPhase.SITTING_SCANNING);

        /**
         * Registers a new {@linkplain EnderDragonPhase Ender Dragon Phase}
         * @param clazz The {@linkplain Class class} of the phase
         * @param name The name of the phase
         * @param vanillaPhase The vanilla phase that should be sent to the client when this phase is reached.
         * @return The new {@linkplain EnderDragonPhase Ender Dragon Phase}
         * @param <T> {@link DragonPhaseInstance}
         */
        private static <T extends DragonPhaseInstance> EnderDragonPhase<T> create(Class<T> clazz, String name, EnderDragonPhase<?> vanillaPhase)
        {
            EnderDragonPhase<T> newPhase = EnderDragonPhase.create(clazz, name);
            PHASE_MAP.put(newPhase.getId(), vanillaPhase);
            return newPhase;
        }

        public static EnderDragonPhase<?> getVanillaPhase(EnderDragonPhase<?> phase)
        {
            return getVanillaPhase(phase.getId());
        }

        public static EnderDragonPhase<?> getVanillaPhase(Integer id)
        {
            // EnderDragonPhase#create internally increases its array size when a new ID is registered.
            // Return values MUST have their IDs clamped to the vanilla default to prevent clients
            // from crashing

            if (!PHASE_MAP.containsKey(id) || PHASE_MAP.get(id) == null)
            {
                return EnderDragonPhase.getById(clampIdVanilla(id));
            }
            EnderDragonPhase<?> vanillaPhase = PHASE_MAP.get(id);

            return EnderDragonPhase.getById(clampIdVanilla(vanillaPhase.getId()));
        }

        private static Integer clampIdVanilla(Integer id)
        {
            return Math.clamp(id, 0, 10);
        }

        public static void initialize() {}
    }
}

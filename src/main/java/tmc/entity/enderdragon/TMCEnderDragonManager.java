package tmc.entity.enderdragon;

import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.phys.Vec3;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.InvolvedPlayersData;
import tmc.damagetypes.TMCDamageTypes;
import tmc.entity.enderdragon.phase.*;
import tmc.event.TMCEnderDragonEvents;
import tmc.mixin.entity.enderdragon.EnderDragonFightAccessor;

import java.util.HashMap;
import java.util.UUID;

public class TMCEnderDragonManager {

    private static final float STAGE_2_HEALTH = 1024f; // Attribute limit

    private static final double BLAST_RADIUS = 50;
    private static final double MAX_HORIZONTAL = 3.5;
    private static final double MIN_HORIZONTAL = 0.8;
    private static final double LIFT = 1.2;

    public static final String[] DIALOGUE_TAKE_DAMAGE = new String[] {
            "You have my attention. You will regret that.",
            "Another arrow. Surely this one will change everything."
    };

    public static final String[] DIALOGUE_KILL_PLAYER = new String[] {
            "Which of you is next to feel heroic?",
            "Your numbers were more impressive a moment ago."
    };

    public static final String[] DIALOGUE_TAUNT = new String[] {
            "You've come so far to accomplish so little."
    };

    public static final String[] DIALOGUE_FIREBALL_BARRAGE = new String[] {
            "How considerate. All in one place.",
            "You brought arrows. I brought an answer",
            "Let's see how much ground you're willing to surrender",
    };

    public static final String[] DIALOGUE_DIVE_BOMB = new String[] {
            "How considerate. All in one place.",
            "Stand your ground. I insist",
            "You wished to stand above me?"
    };

    public static final String[] DIALOGUE_LIGHTNING_PHASE = new String[] {
            "Your courage appears to be leaving without you.",
            "Do you imagine I've forgotten where you are?"
    };

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

    public static void logPlayerAttacker(EnderDragon dragon, ServerPlayer player, float damage)
    {
        if (!dragon.hasAttached(TMCDataAttachments.LOGGED_ATTACKERS) ||
                !dragon.hasAttached(TMCDataAttachments.INVOLVED_PLAYERS_DATA) ||
                !dragon.hasAttached(TMCDataAttachments.ENDER_DRAGON_STAGE_2_TIMER))
        {
            return;
        }
        dragon.modifyAttached(TMCDataAttachments.LOGGED_ATTACKERS, attackerHistoryData -> attackerHistoryData.updateAttacker(player.getUUID(), damage));
        int ticks = getStageTimerTicks(dragon, EnderDragonStage.STAGE_2);
        dragon.modifyAttached(TMCDataAttachments.INVOLVED_PLAYERS_DATA, data -> data.logPlayer(player, InvolvedPlayersData.DURATION_TICKS + ticks));
    }

    public static HashMap<UUID, Float> getAllAttackers(EnderDragon dragon)
    {
        return new HashMap<>(dragon.getAttachedOrCreate(TMCDataAttachments.LOGGED_ATTACKERS).attackerUuids());

    }

    private static Integer getActiveAttackerCount(EnderDragon dragon)
    {
        return dragon.getAttachedOrCreate(TMCDataAttachments.INVOLVED_PLAYERS_DATA).players().size();
    }

    /**
     * Melee damage scalar for Ender Dragon during Stage 2
     * @param dragon
     * @return The scalar to be applied for melee damage
     */
    public static float getMeleeScalar(EnderDragon dragon)
    {
        int active = getActiveAttackerCount(dragon);

        // Damage scaling for this damage type is based on the following formula:
        // (x / (x + 5)) * 73.4
        // Where x = the # of active attackers
        // https://www.desmos.com/calculator/mnn0sxhoyj

        float f = ((float) active / (active + 5f)) * 73.4f;
        return (f / 100) + 1;
    }

    public static float getMagicScalar(EnderDragon dragon)
    {
        int active = getActiveAttackerCount(dragon);

        // Damage scaling for this damage type is based on the following formula:
        // (x / (x + 4.2)) * 19
        // Where x = the # of active attackers
        // https://www.desmos.com/calculator/wpafrhrgus

        float f = ((float) active / (active + 4.2f)) * 19f;
        return (f / 100) + 1;
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
            if (getDragonStage(enderDragon) != EnderDragonStage.VANILLA)
            {
                return false;
            }
            AttributeInstance dragonHealthInstance = enderDragon.getAttribute(Attributes.MAX_HEALTH);
            if (dragonHealthInstance != null)
            {
                dragonHealthInstance.setBaseValue(STAGE_2_HEALTH);
            }
            enderDragon.setHealth(STAGE_2_HEALTH);
            setDragonStage(enderDragon, EnderDragonStage.STAGE_2);
            setAwaitingDialogue(enderDragon, true);
            enderDragon.setAttached(TMCDataAttachments.ENDER_DRAGON_STAGE_2_TIMER, 0);
            enderDragon.getPhaseManager().setPhase(EnderDragonPhase.LANDING_APPROACH);
            if (!(enderDragon.level() instanceof ServerLevel serverLevel))
            {
                return false;
            }
            TMCEnderDragonEvents.STAGE_TRANSITION.invoker().onStageTransition(serverLevel, enderDragon, EnderDragonStage.STAGE_2);
            return true;
        }
        return false;
    }

    /**
     * Attempts to start the closing dialogue stage of the Ender Dragon fight
     * @param enderDragon
     * @return True if the stage was successfully started. False otherwise
     */
    public static boolean tryStartClosingDialogue(EnderDragon enderDragon)
    {
        if (!enderDragon.hasAttached(TMCDataAttachments.ENDER_DRAGON_TRANSITION_DATA) || getDragonStage(enderDragon) != EnderDragonStage.STAGE_2)
        {
            return false;
        }
        if (!(enderDragon.level() instanceof ServerLevel serverLevel))
        {
            return false;
        }
        // Health must be restored, otherwise vanilla EnderDragon#hurt will see the dragon as dead and force the DYING phase
        enderDragon.setHealth(1.0f);
        setAwaitingDialogue(enderDragon, true);
        setDragonStage(enderDragon, EnderDragonStage.DEATH);
        enderDragon.setAttached(TMCDataAttachments.ENDER_DRAGON_DEATH_STAGE_TIMER, 0);
        enderDragon.getPhaseManager().setPhase(EnderDragonPhase.LANDING_APPROACH);
        TMCEnderDragonEvents.STAGE_TRANSITION.invoker().onStageTransition(serverLevel, enderDragon, EnderDragonStage.DEATH);
        return true;
    }

    /**
     * Sets the current boss fight stage on the given Ender Dragon
     * @param dragon The Ender Dragon to modify
     * @param stage The {@linkplain EnderDragonStage stage} of the boss fight
     */
    public static void setDragonStage(EnderDragon dragon, EnderDragonStage stage)
    {
        if (!dragon.hasAttached(TMCDataAttachments.ENDER_DRAGON_TRANSITION_DATA))
        {
            return;
        }
        dragon.setAttached(TMCDataAttachments.ENDER_DRAGON_TRANSITION_DATA, stage.ordinal());
    }

    /**
     * Gets if the given Ender Dragon has transitioned to stage 2
     * @param dragon The Ender Dragon to check
     * @return True if the dragon is on stage 2, false otherwise or
     * if the {@linkplain TMCDataAttachments#ENDER_DRAGON_TRANSITION_DATA transition boolean}
     * cannot be accessed
     */
    public static EnderDragonStage getDragonStage(EnderDragon dragon)
    {
        if (!dragon.hasAttached(TMCDataAttachments.ENDER_DRAGON_TRANSITION_DATA))
        {
            return EnderDragonStage.VANILLA;
        }
        Integer v = dragon.getAttached(TMCDataAttachments.ENDER_DRAGON_TRANSITION_DATA);
        if (v == null)
        {
            return EnderDragonStage.VANILLA;
        }
        return EnderDragonStage.values()[v];
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
     * change the dragon to the {@linkplain Phases#OPENING_DIALOGUE_PERCH opening dialogue perch}
     * phase or the {@linkplain Phases#CLOSING_DIALOGUE_PERCH closing dialogue perch} phase
     * @param dragon The Ender Dragon to check
     * @param pending If the dragon should be awaiting dialogue
     */
    public static void setAwaitingDialogue(EnderDragon dragon, boolean pending)
    {
        dragon.setAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_PENDING, pending);
    }

    /**
     * Ticks the timer for the given Ender Dragon and {@linkplain EnderDragonStage Ender Dragon fight stage} and calls the {@linkplain TMCEnderDragonEvents#ON_STAGE_TICK stage tick update event}
     * @param serverLevel The current server level of the dragon
     * @param dragon The dragon to update
     * @param stage The current {@linkplain EnderDragonStage Ender Dragon Stage} to tick
     */
    public static void tickStageTimer(ServerLevel serverLevel, EnderDragon dragon, EnderDragonStage stage)
    {
        AttachmentType<Integer> attachment = switch (stage)
        {
            case VANILLA -> TMCDataAttachments.ENDER_DRAGON_VANILLA_TIMER;
            case STAGE_2 -> TMCDataAttachments.ENDER_DRAGON_STAGE_2_TIMER;
            case DEATH -> TMCDataAttachments.ENDER_DRAGON_DEATH_STAGE_TIMER;
        };
        if (!dragon.hasAttached(attachment))
        {
            return;
        }
        Integer newTicks = dragon.modifyAttached(attachment, ticks -> ticks + 1);
        if (newTicks == null)
        {
            return;
        }
        TMCEnderDragonEvents.ON_STAGE_TICK.invoker().onStageTick(serverLevel, dragon, stage, newTicks);
    }

    /**
     * Gets the current tick count for the Ender Dragon on the given stage
     * @param dragon The Ender Dragon to check
     * @param stage The {@linkplain EnderDragonStage stage} of the Ender Dragon fight
     * @return The current time in ticks the dragon has been active in stage 2.
     * Returns 0 if the specified {@linkplain EnderDragonStage stage} timer
     * cannot be accessed.
     */
    public static int getStageTimerTicks(EnderDragon dragon, EnderDragonStage stage)
    {
        AttachmentType<Integer> attachment = switch (stage)
        {
            case VANILLA -> TMCDataAttachments.ENDER_DRAGON_VANILLA_TIMER;
            case STAGE_2 -> TMCDataAttachments.ENDER_DRAGON_STAGE_2_TIMER;
            case DEATH -> TMCDataAttachments.ENDER_DRAGON_DEATH_STAGE_TIMER;
        };
        if (!dragon.hasAttached(attachment))
        {
            return 0;
        }
        Integer ticks = dragon.getAttached(attachment);
        return ticks != null ? ticks : 0;
    }

    /**
     * Gets the location the Ender Dragon should perch at, which is directly on top of the
     * End exit portal's bedrock pillar, ignoring any blocks placed above it
     * @param dragon The Ender Dragon that will perch
     * @param heightmapPos The vanilla perch location (the highest block above the exit portal)
     * @return The location on top of the exit portal's bedrock pillar, or the given heightmap
     * location if the exit portal could not be found
     */
    public static BlockPos getPortalPerchLocation(EnderDragon dragon, BlockPos heightmapPos)
    {
        EnderDragonFight fight = dragon.getDragonFight();
        if (fight != null)
        {
            // The exit portal location is the center of the portal. The bedrock pillar is 4 blocks tall above it
            BlockPos exitPortal = ((EnderDragonFightAccessor) fight).tmc$getExitPortalLocation();
            if (exitPortal != null)
            {
                return heightmapPos.atY(exitPortal.getY() + 4);
            }
        }

        // Fall back to the highest bedrock block in the column (e.g. dragons not tied to the fight)
        Level level = dragon.level();
        BlockPos.MutableBlockPos pos = heightmapPos.mutable();
        while (pos.getY() > level.getMinY())
        {
            pos.move(Direction.DOWN);
            if (level.getBlockState(pos).is(Blocks.BEDROCK))
            {
                return pos.above();
            }
        }
        return heightmapPos;
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

        /*
        Damage is calculated as follows:
        - Distance < 23.37 blocks
          damage = -5 * e^(0.1 * distance) + 105
        - Distance >= 23.37 blocks
          damage = -2 * distance + 100

         damage is dealt as a percentage of the player's max health

         This will (ideally) insta-kill players camping under/close
         to the dragon, or at least severely injure them

         Graph on Desmos: https://www.desmos.com/calculator/cbtgrycarb
         */

        double damage;
        if (distance < 23.37)
        {
            damage = -5 * Math.exp(0.1 * distance) + 105;
        } else
        {
            damage = -2 * distance + 100;
        }
        damage = Math.clamp(player.getMaxHealth() * (damage / 100), 0, player.getMaxHealth());

        // Remove invulnerability time to let damage through. Don't let players abuse I-frames for this
        // Why not apply bypasses_cooldown tag?
        // - This is the only instance that should not be affected by cooldown
        // - This damage type bypasses armor, enchantments, effects/resistance, and shields
        // - Having this kind of damage not have a cooldown is way too strong for vanilla gear

        // Damage scales based on difficulty! At closest range:
        // Peaceful = 0 (always)
        // Easy = ~50% of max health
        // Medium = 100% of max health
        // Hard = 150% of max health
        // Scaling from distance does not change

        player.invulnerableTime = 0;
        ServerLevel serverLevel = player.level();
        player.hurtServer(serverLevel, TMCDamageTypes.getDamageSource(TMCDamageTypes.ENDER_DRAGON_MAGIC, serverLevel, player, dragon), (float) damage);
        player.invulnerableTime = 0;
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
        public static final EnderDragonPhase<OpeningDialoguePerchPhase> OPENING_DIALOGUE_PERCH = create(OpeningDialoguePerchPhase.class, "OpeningDialoguePerch", EnderDragonPhase.SITTING_SCANNING);

        /**
         * Phase the Ender Dragon enters when perching for final dialogue
         */
        public static final EnderDragonPhase<ClosingDialoguePerchPhase> CLOSING_DIALOGUE_PERCH = create(ClosingDialoguePerchPhase.class, "ClosingDialoguePerch", EnderDragonPhase.SITTING_SCANNING);

        /**
         * Phase the Ender Dragon enters when diving into players
         */
        public static final EnderDragonPhase<DiveBombPhase> DIVE_BOMB_PHASE = create(DiveBombPhase.class, "DiveBomb", EnderDragonPhase.CHARGING_PLAYER);

        /**
         * Phase the Ender Dragon enters when performing a fireball barrage attack
         */
        public static final EnderDragonPhase<FireballBarragePhase> FIREBALL_BARRAGE_PHASE = create(FireballBarragePhase.class, "FireballBarrage", EnderDragonPhase.HOVERING);

        /**
         * Phase the Ender Dragon enters when performing a lightning blast attack
         */
        public static final EnderDragonPhase<LightningBlastPhase> LIGHTNING_BLAST_PHASE = create(LightningBlastPhase.class, "LightningBlast", EnderDragonPhase.HOVERING);

        /**
         * Phase the Ender Dragon enters when performing her "The Sky Answers to Me" dialogue
         */
        public static final EnderDragonPhase<SkyAnswersPhase> SKY_ANSWERS_PHASE = create(SkyAnswersPhase.class, "SkyAnswers", EnderDragonPhase.HOVERING);

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

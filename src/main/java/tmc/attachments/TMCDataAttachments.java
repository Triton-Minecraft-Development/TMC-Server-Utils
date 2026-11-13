package tmc.attachments;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import tmc.TMCServerUtils;
import tmc.codec.AttackerHistoryData;
import tmc.codec.InvolvedPlayersData;
import tmc.codec.enderdragon.*;
import tmc.entity.enderdragon.EnderDragonStage;

public class TMCDataAttachments {

    /**
     * Ender Dragon data for checking the current stage of the fight.
     * <p>
     *     There should only be 3 possible values, all of which come from the ordinals of {@link EnderDragonStage}
     * </p>
     */
    public static final AttachmentType<Integer> ENDER_DRAGON_TRANSITION_DATA = AttachmentRegistry.create(
            TMCServerUtils.of("ender_dragon_transition_data"),
            integerBuilder -> integerBuilder
                    .initializer(EnderDragonStage.VANILLA::ordinal)
                    .persistent(Codec.INT)
    );

    /**
     * Ender dragon data for checking if the dragon is
     * awaiting to send dialogue
     */
    public static final AttachmentType<Boolean> ENDER_DRAGON_DIALOGUE_PENDING = AttachmentRegistry.create(
            TMCServerUtils.of("ender_dragon_awaiting_dialogue"),
            booleanBuilder -> booleanBuilder
                    .initializer(() -> false)
                    .persistent(Codec.BOOL)
    );

    /**
     * Ender Dragon data for tracking time in ticks on the {@linkplain EnderDragonStage#VANILLA vanilla stage}
     */
    public static final AttachmentType<Integer> ENDER_DRAGON_VANILLA_TIMER = AttachmentRegistry.create(
            TMCServerUtils.of("ender_dragon_vanilla_timer"),
            integerBuilder -> integerBuilder
                    .initializer(() -> 0)
                    .persistent(Codec.INT)
    );

    /**
     * Ender Dragon data for tracking time in ticks on {@linkplain EnderDragonStage#STAGE_2 Stage 2}
     */
    public static final AttachmentType<Integer> ENDER_DRAGON_STAGE_2_TIMER = AttachmentRegistry.create(
            TMCServerUtils.of("ender_dragon_stage_2_timer"),
            integerBuilder -> integerBuilder
                    .initializer(() -> 0)
                    .persistent(Codec.INT)
    );

    /**
     * Ender Dragon data for tracking time in ticks on the {@linkplain EnderDragonStage#DEATH death stage}
     */
    public static final AttachmentType<Integer> ENDER_DRAGON_DEATH_STAGE_TIMER = AttachmentRegistry.create(
            TMCServerUtils.of("ender_dragon_death_stage_timer"),
            integerBuilder -> integerBuilder
                    .initializer(() -> 0)
                    .persistent(Codec.INT)
    );

    /**
     * Ender Dragon data for tracking time in ticks for the dialogue phases
     */
    public static final AttachmentType<Integer> ENDER_DRAGON_DIALOGUE_TIMER = AttachmentRegistry.create(
            TMCServerUtils.of("ender_dragon_dialogue_timer"),
            integerBuilder -> integerBuilder
                    .initializer(() -> 0)
                    .persistent(Codec.INT)
    );

    /**
     * Attachment used for tracking UUIDs of entities that attacked this one, alongside the damage they deal to it.
     * Does not persist on death.
     */
    public static final AttachmentType<AttackerHistoryData> LOGGED_ATTACKERS = AttachmentRegistry.create(
            TMCServerUtils.of("logged_attackers"),
            attackerHistoryBuilder -> attackerHistoryBuilder
                    .initializer(AttackerHistoryData::new)
                    .persistent(AttackerHistoryData.CODEC)
    );

    /**
     * Attachment used for tracking data during the dive bomb phase of the Ender Dragon in Stage 2
     */
    public static final AttachmentType<DiveBombData> ENDER_DRAGON_DIVE_BOMB_DATA = AttachmentRegistry.create(
            TMCServerUtils.of("ender_dragon_dive_bomb_data"),
            builder -> builder
                    .initializer(DiveBombData::new)
                    .persistent(DiveBombData.CODEC)
    );

    /**
     * Attachment used for tracking data during the fireball barrage phase of the Ender Dragon in Stage 2
     */
    public static final AttachmentType<FireballBarrageData> ENDER_DRAGON_FIREBALL_BARRAGE_DATA = AttachmentRegistry.create(
            TMCServerUtils.of("ender_dragon_fireball_barrage_data"),
            builder -> builder
                    .initializer(FireballBarrageData::new)
                    .persistent(FireballBarrageData.CODEC)
    );

    /**
     * Attachment used for tracking data during the lightning blast phase of the Ender Dragon in Stage 2
     */
    public static final AttachmentType<LightningBlastData> ENDER_DRAGON_LIGHTNING_BLAST_DATA = AttachmentRegistry.create(
            TMCServerUtils.of("ender_dragon_lightning_blast_data"),
            builder -> builder
                    .initializer(LightningBlastData::new)
                    .persistent(LightningBlastData.CODEC)
    );

    /**
     * Attachment used for tracking data during the "The Sky Answers to Me" attack at 50% HP during Stage 2
     */
    public static final AttachmentType<SkyAnswersData> ENDER_DRAGON_SKY_ANSWERS_DATA = AttachmentRegistry.create(
            TMCServerUtils.of("ender_dragon_sky_answers_data"),
            builder -> builder
                    .initializer(SkyAnswersData::new)
                    .persistent(SkyAnswersData.CODEC)
    );

    /**
     * Attachments used for tracking data during miscellaneous dialogue throughout Stage 2
     */
    public static final AttachmentType<DialogueCooldownData> ENDER_DRAGON_DIALOGUE_COOLDOWN_DATA = AttachmentRegistry.create(
            TMCServerUtils.of("ender_dragon_misc_dialogue_cooldown_data"),
            builder -> builder
                    .initializer(DialogueCooldownData::new)
                    .persistent(DialogueCooldownData.CODEC)
    );

    /**
     * Attachment used for tracking leaderboard stats for the Ender Dragon
     */
    public static final AttachmentType<EnderDragonLeaderboard> SERVER_ENDER_DRAGON_LEADERBOARD = AttachmentRegistry.create(
            TMCServerUtils.of("ender_dragon_leaderboard"),
            enderDragonLeaderboardBuilder -> enderDragonLeaderboardBuilder
                    .initializer(EnderDragonLeaderboard::new)
                    .persistent(EnderDragonLeaderboard.CODEC)
    );

    /**
     * Attachment used for tracking who has warning titles currently.
     * <p>Returns true if the player has warnings on their screen</p>
     * <p>Returns false otherwise</p>
     */
    public static final AttachmentType<Boolean> PLAYER_WARNING_TITLES_DATA = AttachmentRegistry.create(
            TMCServerUtils.of("player_warning_titles_data"),
            builder -> builder
                    .initializer(() -> false)
                    .persistent(Codec.BOOL)
    );

    /**
     * Attachment used for tracking players under a tick timestamp.
     * <p>Primarily used for tracking involved players during the Ender Dragon fight</p>
     */
    public static final AttachmentType<InvolvedPlayersData> INVOLVED_PLAYERS_DATA = AttachmentRegistry.create(
            TMCServerUtils.of("involved_players_data"),
            builder -> builder
                    .initializer(InvolvedPlayersData::new)
                    .persistent(InvolvedPlayersData.CODEC)
    );

    public static final AttachmentType<Integer> PLAYER_KILLS = AttachmentRegistry.create(
            TMCServerUtils.of("player_kills"),
            builder -> builder
                    .initializer(() -> 0)
                    .persistent(Codec.INT)
    );

    public static void initialize() {}
}

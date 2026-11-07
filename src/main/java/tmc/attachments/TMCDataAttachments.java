package tmc.attachments;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import tmc.TMCServerUtils;
import tmc.enderdragon.EnderDragonStage;

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
     * awaiting sending dialogue to welcome the
     * players to phase 2
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
     * Ender Dragon data for tracking time in ticks on {@linkplain EnderDragonStage#STAGE_2 stage 2}
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
     * Ender Dragon data for tracking time in ticks for the dialogue phase
     */
    public static final AttachmentType<Integer> ENDER_DRAGON_DIALOGUE_TIMER = AttachmentRegistry.create(
            TMCServerUtils.of("ender_dragon_dialogue_timer"),
            integerBuilder -> integerBuilder
                    .initializer(() -> 0)
                    .persistent(Codec.INT)
    );

    public static void initialize()
    {

    }
}

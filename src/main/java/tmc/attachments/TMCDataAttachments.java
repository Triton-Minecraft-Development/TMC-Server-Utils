package tmc.attachments;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import tmc.TMCServerUtils;

public class TMCDataAttachments {

    /**
     * Ender Dragon data for checking if the dragon is on stage 2.
     * <p>
     *     Returns false if the Dragon is still in stage 1 (vanilla)
     * </p>
     * Returns true if the Dragon is on stage 2
     */
    public static final AttachmentType<Boolean> ENDER_DRAGON_TRANSITION_DATA = AttachmentRegistry.create(
            TMCServerUtils.of("ender_dragon_transition_data"),
            booleanBuilder -> booleanBuilder
                    .initializer(() -> false)
                    .persistent(Codec.BOOL)
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
     * Ender Dragon data for tracking time in ticks on stage 2.
     */
    public static final AttachmentType<Integer> ENDER_DRAGON_STAGE_2_TIMER = AttachmentRegistry.create(
            TMCServerUtils.of("ender_dragon_stage_2_timer"),
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

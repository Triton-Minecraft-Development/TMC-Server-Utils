package tmc.entity.enderdragon;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import tmc.attachments.TMCDataAttachments;
import tmc.entity.enderdragon.phase.LightningBlastPhase;

import java.util.*;
import java.util.function.Predicate;

public class WarningManager {

    private static final int FADE_IN = 5;
    private static final int FADE_OUT = 5;
    private static final int STAGE_2_DURATION = 100;
    private static final int LIGHTNING_BLAST_DURATION = LightningBlastPhase.WINDUP_TICKS + LightningBlastPhase.CHARGE_TICKS + LightningBlastPhase.LINGER_TICKS;
    private static final int SKY_ANSWERS_DURATION = 100;
    private static final Component TITLE_WARNING = Component.literal("DANGER").withStyle(ChatFormatting.BOLD, ChatFormatting.RED);
    private static final Component STAGE_2_SUBTITLE_WARNING_1 = createSubtitleWarning("STEP AWAY NOW", true);
    private static final Component STAGE_2_SUBTITLE_WARNING_2 = createSubtitleWarning("STEP AWAY NOW", false);
    private static final Component LIGHTNING_BLAST_SUBTITLE_WARNING_1 = createSubtitleWarning("WATCH YOUR FOOTING", true);
    private static final Component LIGHTNING_BLAST_SUBTITLE_WARNING_2 = createSubtitleWarning("WATCH YOUR FOOTING", false);

    private static Component createSubtitleWarning(String message, boolean flashed)
    {
        ChatFormatting warningColor = flashed ? ChatFormatting.WHITE : ChatFormatting.YELLOW;
        return Component.literal(warningColor + "⚠ " + ChatFormatting.RED + message + warningColor + " ⚠");
    }

    public static void warnPlayerStage2Opening(ServerPlayer player, int tick, double distance)
    {
        if (!AlarmSound.isPlaying(player))
        {
            AlarmSound.play(player, AlarmSound.ringLengthTicks());
        }
        if (distance > 50)
        {
            AlarmSound.stop(player);
            return;
        }

        sendWarningTitles(player, TITLE_WARNING, STAGE_2_DURATION);
        sendFlashingWarningTitles(player, STAGE_2_SUBTITLE_WARNING_1, STAGE_2_SUBTITLE_WARNING_2, tick);
    }

    public static void warnPlayerLightningBlast(ServerPlayer player, int tick)
    {
        if (!AlarmSound.isPlaying(player))
        {
            AlarmSound.play(player, AlarmSound.ringLengthTicks());
        }

        if (tick == 0)
        {
            sendWarningTitles(player, TITLE_WARNING, LIGHTNING_BLAST_DURATION);
        }
        sendFlashingWarningTitles(player, LIGHTNING_BLAST_SUBTITLE_WARNING_1, LIGHTNING_BLAST_SUBTITLE_WARNING_2, tick);
    }

    public static void warnPlayerSkyAnswers(ServerPlayer player, int tick)
    {
        if (!AlarmSound.isPlaying(player))
        {
            AlarmSound.play(player, AlarmSound.ringLengthTicks());
        }

    }

    private static void sendWarningTitles(ServerPlayer player, Component title, int duration)
    {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(FADE_IN, duration, FADE_OUT));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 10, 0));
    }

    private static void sendFlashingWarningTitles(ServerPlayer player, Component subtitle1, Component subtitle2, int tick)
    {
        int interval = tick % 20;
        if (interval < 10)
        {
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle1));
            return;
        }
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle2));
    }

    private static void onWarningTick(int ticksElapsed, int duration, Component title, Component subtitle1, Component subtitle2, ServerPlayer player, Predicate<ServerPlayer> condition)
    {
        boolean isFlashTick = ticksElapsed % 10 == 0;
        boolean isFinalTick = ticksElapsed >= duration;

        Component currentSubtitle = ((ticksElapsed / 10) % 2 == 0) ? subtitle1 : subtitle2;

        boolean active = condition.test(player);
        boolean alreadyTracking = player.getAttachedOrCreate(TMCDataAttachments.PLAYER_WARNING_TITLES_DATA);

        if (!active)
        {
            player.modifyAttached(TMCDataAttachments.PLAYER_WARNING_TITLES_DATA, _ -> false);
            AlarmSound.stop(player);
            return;
        }
        AlarmSound.playNoReplace(player, AlarmSound.ringLengthTicks());

        // Player just met predicate
        if (!alreadyTracking && !isFinalTick)
        {
            player.modifyAttached(TMCDataAttachments.PLAYER_WARNING_TITLES_DATA, _ -> true);
            int remainingStay = Math.max(0, duration - ticksElapsed);

            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, remainingStay, 10));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(currentSubtitle));

            return;
        }

        // Final tick -> trigger 10-tick fade-out for meeting predicate
        if (alreadyTracking && isFinalTick)
        {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 0, 10));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(currentSubtitle));
            return;
        }

        // Player is already watching -> only send packets on flash interval
        if (alreadyTracking && isFlashTick && ticksElapsed >= 10)
        {
            int remainingStay = Math.max(0, duration - ticksElapsed);

            player.connection.send(new ClientboundSetTitlesAnimationPacket(0, remainingStay, 10));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(currentSubtitle));
        }
    }

    public static class AlarmSound
    {
        /** Main tone. PLING is the bright, phone-like note block sound. */
        private static final Holder<SoundEvent> SOUND = SoundEvents.NOTE_BLOCK_PLING;

        /** HOSTILE so players can tell it apart from music/ambient in their volume settings. */
        private static final SoundSource SOURCE = SoundSource.HOSTILE;

        private static final float VOLUME = 1.0f;

        /** Note block semitones (0 = F#3, 12 = F#4, 24 = F#5). The trill alternates between these two. */
        private static final int HIGH_NOTE = 22; // E5
        private static final int LOW_NOTE  = 18; // C5

        /** Ticks between trill notes. 1 = as fast as the server can go (20 notes/sec). */
        private static final int NOTE_SPACING = 1;

        /** Notes in each trill. */
        private static final int NOTES_PER_TRILL = 6;

        /** Silent ticksElapsed between the two trills in one ring. */
        private static final int GAP_BETWEEN_TRILLS = 2;

        /** Silent ticksElapsed after the second trill before the next ring starts. */
        private static final int PAUSE_AFTER_RING = 6;

        private static final List<Note> PATTERN = new ArrayList<>();
        private static final int RING_LENGTH;

        static
        {
            int t = 0;
            for (int trill = 0; trill < 2; trill++)
            {
                for (int i = 0; i < NOTES_PER_TRILL; i++)
                {
                    PATTERN.add(new Note(t, pitchOf(i % 2 == 0 ? HIGH_NOTE : LOW_NOTE)));
                    t += NOTE_SPACING;
                }
                if (trill == 0)
                {
                    t += GAP_BETWEEN_TRILLS;
                }
            }
            RING_LENGTH = t + PAUSE_AFTER_RING;
        }

        private static final HashMap<UUID, ActiveAlarm> ACTIVE = new HashMap<>();

        public static void register()
        {
            ServerTickEvents.END_SERVER_TICK.register(AlarmSound::tick);
        }

        /** Rings {@code rings} times for one player. Replaces any alarm already playing for them. */
        public static void play(ServerPlayer player, int rings)
        {
            if (rings <= 0)
            {
                return;
            }
            ACTIVE.put(player.getUUID(), new ActiveAlarm(rings));
        }

        /** Rings {@code rings} times for one player. Does not replace any alarm already playing for them. */
        public static void playNoReplace(ServerPlayer player, int rings)
        {
            if (rings <= 0)
            {
                return;
            }
            ACTIVE.putIfAbsent(player.getUUID(), new ActiveAlarm(rings));
        }

        public static void play(Collection<? extends ServerPlayer> players, int rings)
        {
            for (ServerPlayer player : players)
            {
                play(player, rings);
            }
        }

        public static void stop(ServerPlayer player)
        {
            ACTIVE.remove(player.getUUID());
        }

        public static boolean isPlaying(ServerPlayer player)
        {
            return ACTIVE.containsKey(player.getUUID());
        }

        /**
         * Length of one ring in ticksElapsed, useful for timing an attack to land right after the warning.
         */
        public static int ringLengthTicks()
        {
            return RING_LENGTH;
        }

        private static void tick(MinecraftServer server)
        {
            if (ACTIVE.isEmpty())
            {
                return;
            }

            Iterator<Map.Entry<UUID, ActiveAlarm>> it = ACTIVE.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry<UUID, ActiveAlarm> entry = it.next();
                ActiveAlarm alarm = entry.getValue();
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

                if (player == null || alarm.age >= alarm.totalTicks)
                {
                    it.remove();
                    continue;
                }

                int tickInRing = alarm.age % RING_LENGTH;
                for (Note note : PATTERN)
                {
                    if (note.tick() == tickInRing)
                    {
                        send(player, note.pitch());
                    }
                    alarm.age++;
                }
            }
        }

        /**
         * Sends the sound only to this player, at their position, so it's always audible to them.
         */
        private static void send(ServerPlayer player, float pitch)
        {
            player.connection.send(new ClientboundSoundPacket(
                    SOUND, SOURCE, player.getX(), player.getY(), player.getZ(), VOLUME, pitch, player.getRandom().nextLong()
            ));
        }

        /**
         * Converts a note block semitone (0..24) to a sound pitch (0.5..2.0).
         */
        private static float pitchOf(int semitone)
        {
            return (float) Math.pow(2.0, (semitone - 12) / 12.0);
        }

        private AlarmSound() {}

        private static final class ActiveAlarm
        {
            int age = 0;
            final int totalTicks;

            ActiveAlarm(int rings)
            {
                this.totalTicks = rings * RING_LENGTH;
            }
        }

        private record Note(int tick, float pitch) {}
    }

}

package tmc.enderdragon;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import tmc.TMCServerUtils;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.enderdragon.DiveBombData;
import tmc.codec.enderdragon.EnderDragonLeaderboard;
import tmc.commands.impl.EnderDragonStatsCommand;
import tmc.event.TMCEnderDragonEvents;
import tmc.util.PlayerClusterScanner;

/**
 * Event handlers for Ender Dragon
 */
public class EnderDragonEventHandler implements ServerLivingEntityEvents.AllowDeath,
        TMCEnderDragonEvents.StageTransition, TMCEnderDragonEvents.DialogueTick, TMCEnderDragonEvents.OnDialogueEnd,
        TMCEnderDragonEvents.PhaseTick {

    private static final int TITLE_FADE_IN_TIME_TICKS = 10;
    private static final int INITIAL_TITLE_DURATION_TICKS = 60;
    private static final int FINAL_TITLE_DURATION_TICKS = 60;
    private static final int TITLE_FADE_OUT_TIME_TICKS = 10;


    private static final Component STAGE_2_TRANSITION_MESSAGE = Component.literal(ChatFormatting.GRAY + "" + ChatFormatting.ITALIC + "You feel the ground start to tremble. Something feels off...");
    private static final Component DEATH_STAGE_TRANSITION_MESSAGE = Component.literal(ChatFormatting.GRAY + "" + ChatFormatting.ITALIC + "The Ender Dragon grows weak, and flies back to the exit portal...");

    private static final Component INITIAL_TITLE = Component.literal(ChatFormatting.BOLD + "" + ChatFormatting.LIGHT_PURPLE + "ENDER DRAGON - PHASE II");
    private static final Component INITIAL_SUBTITLE = Component.literal(ChatFormatting.LIGHT_PURPLE + "The End has awakened");
    private static final Component INITIAL_CHAT_MESSAGE = Component.literal(ChatFormatting.LIGHT_PURPLE + "The " + ChatFormatting.BOLD + "Ender Dragon" + ChatFormatting.RESET + ChatFormatting.LIGHT_PURPLE + " has entered Phase II!");

    private static final Component FINAL_TITLE = Component.literal(ChatFormatting.BOLD + "" + ChatFormatting.LIGHT_PURPLE + "ENDER DRAGON DEFEATED");
    private static final Component FINAL_SUBTITLE = Component.literal(ChatFormatting.LIGHT_PURPLE + "The End has fallen");
    private static final Component FINAL_CHAT_MESSAGE = Component.literal(ChatFormatting.BOLD + "" + ChatFormatting.LIGHT_PURPLE + "The " + ChatFormatting.BOLD + "Ender Dragon" + ChatFormatting.RESET + ChatFormatting.LIGHT_PURPLE + " has been defeated! " + ChatFormatting.GOLD + ChatFormatting.BOLD + "Congratulations!");

    private static final Component COMMAND_MESSAGE = Component.literal(ChatFormatting.WHITE + "You can check your stats against the Ender Dragon using \"" + ChatFormatting.AQUA + "/" + EnderDragonStatsCommand.NAME + ChatFormatting.WHITE + "\"");

    private static final Component FIRST_DRAGON_KILL = Component.literal(ChatFormatting.BOLD + "" + ChatFormatting.GOLD + "Congratulations!" + ChatFormatting.RESET + ChatFormatting.LIGHT_PURPLE + " This was your first Ender Dragon kill!");

    public EnderDragonEventHandler()
    {
        ServerLivingEntityEvents.ALLOW_DEATH.register(this);
        TMCEnderDragonEvents.STAGE_TRANSITION.register(this);
        TMCEnderDragonEvents.ON_DIALOGUE_TICK.register(this);
        TMCEnderDragonEvents.ON_DIALOGUE_END.register(this);
        TMCEnderDragonEvents.ON_STAGE_TICK.register(this);
    }

    @Override
    public boolean allowDeath(LivingEntity entity, DamageSource damageSource, float damageAmount)
    {
        if (!(entity instanceof EnderDragon enderDragon) || !enderDragon.hasAttached(TMCDataAttachments.ENDER_DRAGON_TRANSITION_DATA))
        {
            return true;
        }

        return !TMCEnderDragonManager.tryStartStage2(enderDragon);
    }

    @Override
    public void onStageTransition(ServerLevel serverLevel, EnderDragon enderDragon, EnderDragonStage stage)
    {
        // Ender Dragon must enter landing mode
        enderDragon.getPhaseManager().setPhase(EnderDragonPhase.LANDING);

        // Send dialogue to player
        switch (stage)
        {
            case STAGE_2 -> TMCServerUtils.messageAllPlayers(serverLevel, serverPlayer -> TMCServerUtils.isPlayerInDimension(serverPlayer, ServerLevel.END), STAGE_2_TRANSITION_MESSAGE);
            case DEATH -> TMCServerUtils.messageAllPlayers(serverLevel, serverPlayer -> TMCServerUtils.isPlayerInDimension(serverPlayer, ServerLevel.END), DEATH_STAGE_TRANSITION_MESSAGE);
        }
    }

    @Override
    public void onDialogueTick(ServerLevel serverLevel, EnderDragon enderDragon, DialogueSequence sequence, int dialogueTick)
    {
        TMCEnderDragonRenderer.renderShield(serverLevel, enderDragon, dialogueTick);
        if (sequence == DialogueSequence.STAGE_2 && dialogueTick >= 525)
        {
            TMCEnderDragonRenderer.renderWarning(serverLevel, enderDragon, dialogueTick);
        }
    }

    @Override
    public void onDialogueEnd(ServerLevel serverLevel, EnderDragon dragon, DialogueSequence sequence)
    {
        switch (sequence)
        {
            case STAGE_2 -> {
                // Particle Explosion, knockback all players + Announce Dragon Phase 2!
                serverLevel.playSound(null, dragon.getOnPos(), SoundEvents.END_PORTAL_SPAWN, SoundSource.MASTER, 2.0f, 1.0f);
                serverLevel.getPlayers(serverPlayer -> TMCServerUtils.isPlayerInDimension(serverPlayer, ServerLevel.END)).forEach(player ->
                {
                    TMCEnderDragonManager.blastPlayer(player, dragon);

                    player.connection.send(new ClientboundSetTitlesAnimationPacket(TITLE_FADE_IN_TIME_TICKS, INITIAL_TITLE_DURATION_TICKS, TITLE_FADE_OUT_TIME_TICKS));
                    player.connection.send(new ClientboundSetTitleTextPacket(INITIAL_TITLE));
                    player.connection.send(new ClientboundSetSubtitleTextPacket(INITIAL_SUBTITLE));

                    LightningBolt lightningBolt = new LightningBolt(EntityTypes.LIGHTNING_BOLT, serverLevel);

                    lightningBolt.setPos(player.position());

                    serverLevel.addFreshEntity(lightningBolt);

                    player.sendSystemMessage(INITIAL_CHAT_MESSAGE);
                });
            }
            case DEATH -> {

                serverLevel.playSound(null, dragon.getOnPos(), SoundEvents.WITHER_DEATH, SoundSource.MASTER, 2.0f, 1.0F);
                serverLevel.getPlayers(serverPlayer -> TMCServerUtils.isPlayerInDimension(serverPlayer, ServerLevel.END)).forEach(player ->
                {
                    player.connection.send(new ClientboundSetTitlesAnimationPacket(TITLE_FADE_IN_TIME_TICKS, FINAL_TITLE_DURATION_TICKS, TITLE_FADE_OUT_TIME_TICKS));
                    player.connection.send(new ClientboundSetTitleTextPacket(FINAL_TITLE));
                    player.connection.send(new ClientboundSetSubtitleTextPacket(FINAL_SUBTITLE));

                    player.sendSystemMessage(FINAL_CHAT_MESSAGE);

                });
                TMCEnderDragonManager.getAllAttackers(dragon).forEach((uuid, damage) ->
                {
                    ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(uuid);
                    if (player == null)
                    {
                        return;
                    }
                    EnderDragonLeaderboard.update(serverLevel, player, damage);
                    EnderDragonLeaderboardManager.printFightLeaderboard(dragon, serverLevel, player);
                    EnderDragonLeaderboard leaderboard = serverLevel.getServer().overworld().getAttachedOrCreate(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD);

                    EnderDragonLeaderboard.PlayerStats stats = leaderboard.getPlayerStats(player);
                    int newKills = stats.slays();
                    if (newKills == 1)
                    {
                        player.sendSystemMessage(FIRST_DRAGON_KILL);
                    } else
                    {
                        player.sendSystemMessage(EnderDragonLeaderboardManager.getDragonsKilledMessage(newKills));
                    }
                    player.sendSystemMessage(COMMAND_MESSAGE);
                });
            }
        }
    }

    @Override
    public void onStageTick(ServerLevel serverLevel, EnderDragon enderDragon, EnderDragonStage stage, int ticks)
    {
        if (stage != EnderDragonStage.STAGE_2)
        {
            return;
        }

        if (enderDragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.HOLDING_PATTERN)
        {
            DiveBombData diveBombData = enderDragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA);
            if (!diveBombData.isCooldownReady(ticks))
            {
                return;
            }
            if (serverLevel.getRandom().nextFloat() >= 0.2f)
            {
                return;
            }
            if (PlayerClusterScanner.findDiveTarget(serverLevel, PlayerClusterScanner.SCAN_RADIUS_ENDER_DRAGON, PlayerClusterScanner.EPS_ENDER_DRAGON_DIVE, PlayerClusterScanner.MIN_PTS_ENDER_DRAGON_DIVE, PlayerClusterScanner.FALLBACK_TO_SINGLE_PLAYER_ENDER_DRAGON_DIVE).isEmpty())
            {
                return;
            }
            enderDragon.getPhaseManager().setPhase(TMCEnderDragonManager.Phases.DIVE_BOMB_PHASE);
        }
    }
}

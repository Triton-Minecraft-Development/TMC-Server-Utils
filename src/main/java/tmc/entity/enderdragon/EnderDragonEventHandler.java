package tmc.entity.enderdragon;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import tmc.TMCServerUtils;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.enderdragon.*;
import tmc.commands.impl.EnderDragonStatsCommand;
import tmc.event.TMCEnderDragonEvents;
import tmc.util.PlayerClusterScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Event handlers for Ender Dragon
 */
public class EnderDragonEventHandler implements ServerLivingEntityEvents.AllowDeath,
        TMCEnderDragonEvents.StageTransition, TMCEnderDragonEvents.DialogueTick, TMCEnderDragonEvents.OnDialogueEnd,
        TMCEnderDragonEvents.PhaseTick, ServerLivingEntityEvents.AfterDeath, ServerPlayerEvents.Leave {

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
        ServerLivingEntityEvents.AFTER_DEATH.register(this);
    }

    @Override
    public boolean allowDeath(LivingEntity entity, DamageSource damageSource, float damageAmount)
    {
        if (!(entity instanceof EnderDragon enderDragon) || !enderDragon.hasAttached(TMCDataAttachments.ENDER_DRAGON_TRANSITION_DATA))
        {
            return true;
        }

        EnderDragonStage stage = TMCEnderDragonManager.getDragonStage(enderDragon);
        return switch (stage)
        {
            case VANILLA -> !TMCEnderDragonManager.tryStartStage2(enderDragon);
            case STAGE_2 -> !TMCEnderDragonManager.tryStartClosingDialogue(enderDragon);
            case DEATH -> true;
        };
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
            for (ServerPlayer player : serverLevel.getPlayers(p -> p.distanceTo(enderDragon) <= 51))
            {
                WarningManager.warnPlayerStage2Opening(player, dialogueTick - 525, player.distanceTo(enderDragon));
            }
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
                    lightningBolt.setVisualOnly(true);
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
                    if (leaderboard.isPlayerBanned(player.getUUID()))
                    {
                        return;
                    }
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
        for (ServerPlayer serverPlayer : serverLevel.getServer().getPlayerList().getPlayers())
        {
            enderDragon.modifyAttached(TMCDataAttachments.INVOLVED_PLAYERS_DATA, data -> data.removePlayerOnTick(serverPlayer, ticks));
        }
        SkyAnswersData skyAnswersData = enderDragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_SKY_ANSWERS_DATA);
        EnderDragonPhase<?> current = enderDragon.getPhaseManager().getCurrentPhase().getPhase();
        if (!skyAnswersData.isUsed() && enderDragon.getHealth() <= enderDragon.getMaxHealth() * 0.5f && current != EnderDragonPhase.DYING)
        {
            enderDragon.getPhaseManager().setPhase(TMCEnderDragonManager.Phases.SKY_ANSWERS_PHASE);
            return;
        }
        if (enderDragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.HOLDING_PATTERN)
        {
            DiveBombData diveBombData = enderDragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA);
            FireballBarrageData fireballBarrageData = enderDragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_FIREBALL_BARRAGE_DATA);
            LightningBlastData lightningBlastData = enderDragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_LIGHTNING_BLAST_DATA);
            if (serverLevel.getRandom().nextFloat() >= 0.2f)
            {
                return;
            }
            List<EnderDragonPhase<?>> readyAttacks = this.readyAttacks(
                    diveBombData.isCooldownReady(ticks),
                    fireballBarrageData.isCooldownReady(ticks),
                    lightningBlastData.isCooldownReady(ticks)
            );
            if (readyAttacks.isEmpty())
            {
                return;
            }
            Random random = new Random();
            EnderDragonPhase<?> chosenAttack = readyAttacks.get(random.nextInt(readyAttacks.size()));
            if (this.isTargetingAttack(chosenAttack))
            {
                if (PlayerClusterScanner.findDiveTarget(serverLevel, PlayerClusterScanner.SCAN_RADIUS_ENDER_DRAGON, PlayerClusterScanner.EPS_ENDER_DRAGON_DIVE, PlayerClusterScanner.MIN_PTS_ENDER_DRAGON_DIVE, PlayerClusterScanner.FALLBACK_TO_SINGLE_PLAYER_ENDER_DRAGON_DIVE).isEmpty())
                {
                    return;
                }
            }

            enderDragon.getPhaseManager().setPhase(chosenAttack);
        }
    }

    @Override
    public void afterDeath(LivingEntity entity, DamageSource damageSource)
    {
        if (damageSource.getEntity() == null)
        {
            return;
        }
        if (damageSource.getEntity() instanceof EnderDragon dragon)
        {
            if (!(entity instanceof ServerPlayer serverPlayer))
            {
                return;
            }
            dragon.modifyAttached(TMCDataAttachments.PLAYER_KILLS, integer -> integer + 1);
            if (TMCEnderDragonManager.getDragonStage(dragon) != EnderDragonStage.STAGE_2)
            {
                return;
            }
            int ticks = TMCEnderDragonManager.getStageTimerTicks(dragon, EnderDragonStage.STAGE_2);
            DialogueCooldownData dialogueCooldownData = dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_COOLDOWN_DATA);
            if (!dialogueCooldownData.isKillPlayerReady(ticks))
            {
                return;
            }
            Random random = new Random();
            int choice = random.nextInt(TMCEnderDragonManager.DIALOGUE_KILL_PLAYER.length);
            String dialogue = TMCEnderDragonManager.DIALOGUE_KILL_PLAYER[choice];
            TMCServerUtils.messageAllPlayers(serverPlayer.level(), player -> TMCServerUtils.isPlayerInDimension(player, ServerLevel.END),
                    TMCEnderDragonManager.createEnderDragonDialogueComponent(dialogue));

            dragon.modifyAttached(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_COOLDOWN_DATA, data -> data.setKillPlayerCooldown(ticks + DialogueCooldownData.KILL_PLAYER_COOLDOWN_TICKS));
        }
    }

    @Override
    public void onLeave(ServerPlayer player)
    {
        ServerLevel endLevel = player.level().getServer().getLevel(ServerLevel.END);
        if (endLevel == null)
        {
            return;
        }
        EnderDragonFight fight = endLevel.getDragonFight();
        if (fight == null)
        {
            return;
        }
        UUID dragonUUID = fight.dragonUUID();
        if (dragonUUID == null)
        {
            return;
        }
        Entity dragonEntity = endLevel.getEntity(dragonUUID);
        if (!(dragonEntity instanceof EnderDragon dragon))
        {
            return;
        }
        dragon.modifyAttached(TMCDataAttachments.INVOLVED_PLAYERS_DATA, data -> data.removePlayer(player));
    }

    private List<EnderDragonPhase<?>> readyAttacks(boolean diveBomb, boolean fireballBarrage, boolean lightningBlast)
    {
        List<EnderDragonPhase<?>> attacks = new ArrayList<>(3);
        if (diveBomb)
        {
            attacks.add(TMCEnderDragonManager.Phases.DIVE_BOMB_PHASE);
        }
        if (fireballBarrage)
        {
            attacks.add(TMCEnderDragonManager.Phases.FIREBALL_BARRAGE_PHASE);
        }
        if (lightningBlast)
        {
            attacks.add(TMCEnderDragonManager.Phases.LIGHTNING_BLAST_PHASE);
        }
        return attacks;
    }

    private boolean isTargetingAttack(EnderDragonPhase<?> phase)
    {
        return phase.equals(TMCEnderDragonManager.Phases.DIVE_BOMB_PHASE) || phase.equals(TMCEnderDragonManager.Phases.FIREBALL_BARRAGE_PHASE);
    }

}

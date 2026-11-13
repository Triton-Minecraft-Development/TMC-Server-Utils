package tmc.entity.enderdragon.phase;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.enderdragon.SkyAnswersData;
import tmc.damagetypes.TMCDamageTypes;
import tmc.entity.enderdragon.DialogueSequence;
import tmc.entity.enderdragon.TMCEnderDragonManager;
import tmc.entity.enderdragon.WarningManager;
import tmc.event.TMCEnderDragonEvents;

import java.util.ArrayList;
import java.util.List;

public class SkyAnswersPhase extends AbstractDragonPhaseInstance {

    private static final String[] WORDS = { "THE", "SKY", "ANSWERS", "TO", "ME." };
    private static final int[] WORD_TICKS = { 0, 15, 35, 55, 70 };
    private static final int RUMBLE_TICK = 15;
    private static final int TARGETS_TICK = 35;
    private static final int INTENSIFY_TICK = 55;
    private static final int STRIKE_TICK = 100;
    private static final int END_TICK = 120;
    private static final int TITLE_FADE_OUT = 10;
    private static final ChatFormatting[] TITLE_STYLE = { ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD };

    private static final int WARNING_START_TICK = 0;
    private static final int WARNING_FLASH_INTERVAL = 10;
    private static final Component WARNING_LABEL_1 = WarningManager.createSubtitleWarning("TAKE COVER!", true);
    private static final Component WARNING_LABEL_2 = WarningManager.createSubtitleWarning("TAKE COVER!", false);

    private static final double RANGE = 200.0;
    private static final int COVER_HEIGHT = 20;
    private static final float LIGHTNING_DAMAGE_MAX_HEALTH = 0.8f;
    private static final double SHARED_BOLT_RANGE = 2.0;
    private static final int ACTION_BAR_INTERVAL = 5;

    private static final double BRAKE = 0.8;
    private static final double BOB_AMPLITUDE = 0.04;

    /**
     * Covered = some block between the head and COVER_HEIGHT blocks above it has a full, solid
     * underside (stone, planks, slabs, leaves, glass...). Fences, walls, panes, trapdoors and
     * carpets don't count. Only the column at the player's centre is checked, like Storm.
     */
    private static boolean isCovered(ServerLevel level, ServerPlayer player)
    {
        int x = player.getBlockX();
        int z = player.getBlockZ();
        int headY = Mth.floor(player.getBoundingBox().maxY); // block the top of the head is in

        // Fast path: nothing motion-blocking at or above the head in this column
        if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) <= headY) return false;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int top = Math.min(headY + COVER_HEIGHT, level.getMaxY());
        for (int y = headY; y <= top; y++)
        {
            pos.set(x, y, z);
            if (level.getBlockState(pos).isFaceSturdy(level, pos, Direction.DOWN)) return true;
        }
        return false;
    }

    @Nullable
    private static LightningBolt nearestBolt(List<LightningBolt> bolts, Vec3 pos)
    {
        for (LightningBolt bolt : bolts)
        {
            if (bolt.position().distanceToSqr(pos) <= SHARED_BOLT_RANGE * SHARED_BOLT_RANGE) return bolt;
        }
        return null;
    }

    @Nullable
    private static LightningBolt spawnBolt(ServerLevel level, Vec3 pos)
    {
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return null;
        bolt.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        bolt.setVisualOnly(true);         // no 3-block area damage (would reach sheltered players), no ground fire
        level.addFreshEntity(bolt);
        return bolt;
    }

    private static Component subtitleAt(int t)
    {
        if (t < WARNING_START_TICK || t >= STRIKE_TICK)
        {
            return Component.empty();
        }
        boolean flash = ((t - WARNING_START_TICK) / WARNING_FLASH_INTERVAL) % 2 == 0;
        return flash ? WARNING_LABEL_1 : WARNING_LABEL_2;
    }

    private int shownWord = -1; // not saved: after a reload the current word is simply shown again

    public SkyAnswersPhase(EnderDragon dragon)
    {
        super(dragon);
    }

    @Override
    public void begin()
    {
        SkyAnswersData data = this.getData();
        if (!data.isActive())
        {
            data.start(dragon.getDeltaMovement()); // also marks the attack as used for this fight
        }
        shownWord = -1;
    }

    @Override
    public void end()
    {
        SkyAnswersData data = this.getData();
        // Interrupted mid-speech (death, /kill, other code): don't leave a word hanging on screen
        if (data.isActive() && data.getTicks() < STRIKE_TICK && this.dragon.level() instanceof ServerLevel level)
        {
            for (ServerPlayer player : playersInRange(level))
            {
                player.connection.send(new ClientboundClearTitlesPacket(true));
                WarningManager.AlarmSound.stop(player);
            }
        }
        data.setActive(false);
    }

    @Override
    public void doServerTick(ServerLevel level)
    {
        SkyAnswersData data = this.getData();
        int t = data.getTicks();
        this.hover(data);

        // "THE" - she stops and spreads her wings
        if (t == 0)
        {
            TMCEnderDragonEvents.ON_DIALOGUE_START.invoker().onDialogueStart(level, this.dragon, DialogueSequence.SKY_ANSWERS);
            level.playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(),
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 8.0F, 0.7F);
        }
        TMCEnderDragonEvents.ON_DIALOGUE_TICK.invoker().onDialogueTick(level, this.dragon, DialogueSequence.SKY_ANSWERS, t);

        for (ServerPlayer player : level.getPlayers(p -> p.distanceTo(this.dragon) <= RANGE))
        {
            WarningManager.onWarningAlarmOnlyTick(player, player.distanceTo(this.dragon) <= RANGE);
        }
        // "SKY" - thunder rumbles (volume 10000 carries across the whole End, like vanilla thunder)
        if (t == RUMBLE_TICK)
        {
            playThunder(level, 0.5F);
        }

        // "TO" - the charge intensifies
        if (t == INTENSIFY_TICK)
        {
            playThunder(level, 0.7F);
            level.playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(),
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 8.0F, 0.9F);
        }

        if (t < STRIKE_TICK)
        {
            updateTitle(level, t);
            chargeParticles(level, t);
        }

        // "ANSWERS" onward - mark exposed players
        if (t >= TARGETS_TICK && t < STRIKE_TICK)
        {
            markTargets(level, t);
        }

        if (t == STRIKE_TICK)
        {
            strike(level);
            TMCEnderDragonEvents.ON_DIALOGUE_END.invoker().onDialogueEnd(level, this.dragon, DialogueSequence.SKY_ANSWERS);
        }

        if (t >= END_TICK)
        {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            return;
        }

        data.setTicks(t + 1);
    }

    @Override
    public EnderDragonPhase<? extends DragonPhaseInstance> getPhase()
    {
        return TMCEnderDragonManager.Phases.SKY_ANSWERS_PHASE;
    }

    private SkyAnswersData getData()
    {
        return this.dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_SKY_ANSWERS_DATA);
    }

    private void updateTitle(ServerLevel level, int t)
    {
        int word = -1;
        for (int i = 0; i < WORD_TICKS.length; i++)
        {
            if (t >= WORD_TICKS[i]) word = i;
        }
        if (word < 0 || word == shownWord) return;
        shownWord = word;

        // Each word stays up exactly until the next one replaces it; the last one until the strike.
        boolean last = word == WORDS.length - 1;
        int nextTick = last ? STRIKE_TICK : WORD_TICKS[word + 1];
        int stay = Math.max(1, nextTick - t);

        Component text = Component.literal(WORDS[word]).withStyle(TITLE_STYLE);
        Component subtitle = subtitleAt(t);

        for (ServerPlayer player : playersInRange(level))
        {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(0, stay, last ? TITLE_FADE_OUT : 0));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle)); // clear leftovers from other dialogue
            player.connection.send(new ClientboundSetTitleTextPacket(text));
        }
    }

    private void sendSubtitle(ServerLevel level, Component subtitle)
    {
        ClientboundSetSubtitleTextPacket packet = new ClientboundSetSubtitleTextPacket(subtitle);
        for (ServerPlayer player : this.playersInRange(level))
        {
            player.connection.send(packet);
        }
    }

    private void chargeParticles(ServerLevel level, int t)
    {
        boolean intense = t >= INTENSIFY_TICK;
        int interval = intense ? 1 : 3;
        if (t < RUMBLE_TICK || t % interval != 0) return;

        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                dragon.getX(), dragon.getY() + 2.0, dragon.getZ(),
                intense ? 30 : 12, 4.0, 2.0, 4.0, 0.2);
    }

    private void markTargets(ServerLevel level, int t)
    {
        boolean intense = t >= INTENSIFY_TICK;
        boolean actionBar = t % ACTION_BAR_INTERVAL == 0;

        for (ServerPlayer player : playersInRange(level))
        {
            boolean covered = isCovered(level, player);

            if (!covered && t % (intense ? 2 : 4) == 0)
            {
                // A column of sparks from the head up, so others can see who's exposed too
                double top = player.getBoundingBox().maxY;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), top + 1.5, player.getZ(),
                        intense ? 10 : 5, 0.25, 1.5, 0.25, 0.02);
            }

            if (actionBar)
            {
                player.sendSystemMessage(covered
                                ? Component.literal("Sheltered").withStyle(ChatFormatting.GREEN)
                                : Component.literal("⚡ EXPOSED - get under cover! ⚡").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                        true);
            }
        }
    }

    private void strike(ServerLevel level)
    {
        List<LightningBolt> bolts = new ArrayList<>();

        for (ServerPlayer player : playersInRange(level))
        {
            if (isCovered(level, player)) continue;

            LightningBolt bolt = nearestBolt(bolts, player.position());
            if (bolt == null)
            {
                bolt = spawnBolt(level, player.position());
                if (bolt == null)
                {
                    continue;
                }
                bolts.add(bolt);
            }
            player.invulnerableTime = 0;
            float damage = player.getMaxHealth() * LIGHTNING_DAMAGE_MAX_HEALTH;
            player.hurtServer(level, TMCDamageTypes.getDamageSource(TMCDamageTypes.ENDER_DRAGON_MAGIC, level, player, this.dragon), damage);
            player.invulnerableTime = 0;
        }

        // Every real bolt plays its own thunder on the clients; otherwise play one for everyone
        if (bolts.isEmpty())
        {
            playThunder(level, 0.8F + dragon.getRandom().nextFloat() * 0.2F);
        }
    }

    private void playThunder(ServerLevel level, float pitch)
    {
        level.playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 10000.0F, pitch);
    }

    private List<ServerPlayer> playersInRange(ServerLevel level)
    {
        return level.getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
                && p.distanceToSqr(dragon) <= RANGE * RANGE);
    }

    private void hover(SkyAnswersData data)
    {
        Vec3 v = data.getVelocity();
        double bob = Math.cos(data.getTicks() * 0.15) * BOB_AMPLITUDE;
        data.setVelocity(new Vec3(v.x * BRAKE, v.y * BRAKE + bob, v.z * BRAKE));
        dragon.setDeltaMovement(data.getVelocity());
        dragon.move(MoverType.SELF, data.getVelocity());
    }
}

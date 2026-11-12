package tmc.mixin.entity.enderdragon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tmc.attachments.TMCDataAttachments;
import tmc.entity.enderdragon.EnderDragonStage;
import tmc.entity.enderdragon.TMCEnderDragonManager;

/**
 * Mixin for adding Data Attachments to the Ender Dragon
 * @see TMCDataAttachments#ENDER_DRAGON_TRANSITION_DATA
 * @see TMCDataAttachments#ENDER_DRAGON_DIALOGUE_PENDING
 * @see TMCDataAttachments#ENDER_DRAGON_VANILLA_TIMER
 * @see TMCDataAttachments#ENDER_DRAGON_STAGE_2_TIMER
 * @see TMCDataAttachments#ENDER_DRAGON_DEATH_STAGE_TIMER
 * @see TMCDataAttachments#ENDER_DRAGON_DIALOGUE_TIMER
 * @see TMCDataAttachments#ENDER_DRAGON_DIVE_BOMB_DATA
 * @see TMCDataAttachments#ENDER_DRAGON_FIREBALL_BARRAGE_DATA
 * @see TMCDataAttachments#ENDER_DRAGON_LIGHTNING_BLAST_DATA
 * @see TMCDataAttachments#ENDER_DRAGON_SKY_ANSWERS_DATA
 * @see TMCDataAttachments#LOGGED_ATTACKERS
 */
@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {

    @Unique
    private static final float MAX_DAMAGE_PER_HIT = 50;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initTmcData(EntityType<? extends EnderDragon> type, Level level, CallbackInfo ci)
    {
        EnderDragon dragon = (EnderDragon) (Object) this;

        if (dragon.level().isClientSide())
        {
            return;
        }

        dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_TRANSITION_DATA);
        dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_PENDING);
        dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_VANILLA_TIMER);
        dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_STAGE_2_TIMER);
        dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_DEATH_STAGE_TIMER);
        dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_DIALOGUE_TIMER);
        dragon.getAttachedOrCreate(TMCDataAttachments.LOGGED_ATTACKERS);
        dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_DIVE_BOMB_DATA);
        dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_FIREBALL_BARRAGE_DATA);
        dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_LIGHTNING_BLAST_DATA);
        dragon.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_SKY_ANSWERS_DATA);
    }

    @Inject(method = "hurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/boss/enderdragon/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void onHurt(ServerLevel level, EnderDragonPart part, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir)
    {
        EnderDragon dragon = (EnderDragon) (Object) this;

        if (dragon.level().isClientSide())
        {
            return;
        }

        if (TMCEnderDragonManager.isAwaitingDialogue(dragon))
        {
            Entity entity = source.getEntity();
            if (entity != null)
            {
                if (entity instanceof ServerPlayer player)
                {
                    player.sendSystemMessage(Component.literal(ChatFormatting.RED + "A mystical force prevents you from damaging the Ender Dragon."));
                }
            }
            cir.cancel();
        }
    }

    /**
     * Halves the amount of damage and applies a damage cap to the Ender Dragon. This is only
     * active during {@linkplain EnderDragonStage#STAGE_2 stage 2}
     * @param damage The original amount of damage received
     * @param level The {@linkplain ServerLevel server level} the Ender dragon present
     * @param source The damage source of the inflicted damage
     * @return The adjusted amount of damage the dragon will receive.
     */
    @ModifyVariable(method = "reallyHurt", at = @At("HEAD"), argsOnly = true, name = "damage")
    private float onReallyHurt(float damage, ServerLevel level, DamageSource source)
    {
        if (damage <= 0f)
        {
            return damage;
        }
        EnderDragon dragon = (EnderDragon) (Object) this;
        EnderDragonStage stage = TMCEnderDragonManager.getDragonStage(dragon);
        if (stage == EnderDragonStage.STAGE_2)
        {
            damage = Math.min(damage / 2, MAX_DAMAGE_PER_HIT);
        } else
        {
            damage = Math.min(damage, MAX_DAMAGE_PER_HIT);
        }

        Entity entity = source.getEntity();
        if (entity != null)
        {
            if (entity instanceof ServerPlayer player)
            {
                TMCEnderDragonManager.logPlayerAttacker(dragon, player, damage);
            } else if (entity instanceof Arrow arrow)
            {
                if (arrow.getOwner() != null && arrow.getOwner() instanceof ServerPlayer player)
                {
                    TMCEnderDragonManager.logPlayerAttacker(dragon, player, damage);
                }
            }
        }
        return damage;
    }

}

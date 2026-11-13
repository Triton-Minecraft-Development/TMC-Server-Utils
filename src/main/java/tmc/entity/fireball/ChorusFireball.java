package tmc.entity.fireball;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.Vec3;
import tmc.damagetypes.TMCDamageTypes;
import tmc.entity.enderdragon.TMCEnderDragonManager;
import tmc.entity.enderdragon.TMCEnderDragonRenderer;
import tmc.util.ItemUtils;

import java.util.List;
import java.util.function.BiConsumer;

public class ChorusFireball extends AbstractAreaEffectCloudFireball {

    private static final String TEXTURE_HASH = "30655d638e4b6219fe655f7bea34f9949541dd207764b36088ed1bffd1cc3e16";

    public static ChorusFireball launch(ServerLevel serverLevel, EnderDragon shooter, Vec3 dir)
    {
        Vec3 spawn = TMCEnderDragonRenderer.getWarningHeadPos(shooter, 0);

        ChorusFireball fb = new ChorusFireball(serverLevel, shooter, dir.normalize());
        fb.snapTo(spawn, 0.0f, 0.0f);
        serverLevel.addFreshEntity(fb);
        if (!shooter.isSilent())
        {
            serverLevel.levelEvent(null, LevelEvent.SOUND_DRAGON_FIREBALL, shooter.blockPosition(), 0);
        }
        return fb;
    }

    public ChorusFireball(Level level, LivingEntity mob, Vec3 direction)
    {
        super(level, mob, direction, 0);
    }

    @Override
    protected List<MobEffectInstance> effects()
    {
        return List.of();
    }

    @Override
    protected ParticleOptions getCloudParticles()
    {
        return new DustParticleOptions(0x522D52, 1f); // Chorus fruit purple
    }

    @Override
    protected float initialRadius()
    {
        return 3f;
    }

    @Override
    protected float impactDamageRadius()
    {
        return this.initialRadius() * 2.5f;
    }

    @Override
    protected int durationTicks()
    {
        return 700;
    }

    @Override
    protected BiConsumer<Entity, Integer> onVictimInAreaEffectCloudTick()
    {
        TeleportRandomlyConsumeEffect tpEffect = new TeleportRandomlyConsumeEffect();
        return (entity, integer) ->
        {
            if (!(entity instanceof LivingEntity livingEntity))
            {
                return;
            }
            tpEffect.apply(livingEntity.level(), livingEntity.getActiveItem(), livingEntity);
        };
    }

    @Override
    protected DamageSource impactDamageSource(ServerLevel serverLevel, LivingEntity hitEntity, Entity owner)
    {
        if (owner instanceof LivingEntity livingOwner)
        {
            return TMCDamageTypes.getDamageSource(TMCDamageTypes.ENDER_DRAGON_MAGIC, serverLevel, hitEntity, livingOwner);
        }
        return null;
    }

    @Override
    protected float impactDamage(Entity owner)
    {
        if (owner instanceof EnderDragon enderDragon)
        {
            return 2.5f * TMCEnderDragonManager.getMagicScalar(enderDragon);
        }
        return 2.5f;
    }

    @Override
    protected ItemStack getDefaultItem()
    {
        return ItemUtils.createCustomHead(TEXTURE_HASH);
    }
}

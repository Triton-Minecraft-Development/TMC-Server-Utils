package tmc.entity.fireball;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import tmc.damagetypes.TMCDamageTypes;
import tmc.entity.enderdragon.TMCEnderDragonManager;
import tmc.entity.enderdragon.TMCEnderDragonRenderer;
import tmc.util.ItemUtils;

import java.util.List;

public class BlindnessFireball extends AbstractAreaEffectCloudFireball {

    private static final String TEXTURE_HASH = "fba85836b7005c3bdec9bb28644217969d3154dd006c4bdb325468f2e9ad172a";

    public static BlindnessFireball launch(ServerLevel serverLevel, EnderDragon shooter, Vec3 dir)
    {
        Vec3 spawn = TMCEnderDragonRenderer.getWarningHeadPos(shooter, 0);

        BlindnessFireball fb = new BlindnessFireball(serverLevel, shooter, dir.normalize());
        fb.snapTo(spawn, 0.0f, 0.0f);
        serverLevel.addFreshEntity(fb);
        if (!shooter.isSilent())
        {
            serverLevel.levelEvent(null, LevelEvent.SOUND_DRAGON_FIREBALL, shooter.blockPosition(), 0);
        }
        return fb;
    }

    public BlindnessFireball(Level level, LivingEntity mob, Vec3 direction)
    {
        super(level, mob, direction, 0);
    }

    @Override
    protected List<MobEffectInstance> effects()
    {
        return List.of(new MobEffectInstance(MobEffects.BLINDNESS, 140));
    }

    @Override
    protected ParticleOptions getCloudParticles()
    {
        return ParticleTypes.LARGE_SMOKE;
    }

    @Override
    protected float initialRadius()
    {
        return 4f;
    }

    @Override
    protected float impactDamageRadius()
    {
        return this.initialRadius() * 2f;
    }

    @Override
    protected int durationTicks()
    {
        return 750;
    }

    @Override
    protected @Nullable ParticleOptions getTrailParticle()
    {
        return ParticleTypes.LARGE_SMOKE;
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

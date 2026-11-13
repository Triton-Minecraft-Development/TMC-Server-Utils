package tmc.entity.fireball;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import tmc.TMCServerUtils;
import tmc.entity.areaeffectcloud.TMCAreaEffectCloud;

import java.util.List;
import java.util.function.BiConsumer;

public abstract class AbstractAreaEffectCloudFireball extends LargeFireball {

    public AbstractAreaEffectCloudFireball(Level level, LivingEntity mob, Vec3 direction, int explosionPower)
    {
        super(level, mob, direction, explosionPower);
    }

    /**
     * @return {@linkplain MobEffectInstance Effects} to apply to entities within the area effect cloud created on impact
     */
    protected abstract List<MobEffectInstance> effects();

    /**
     * @return Particle to display in the area effect cloud
     */
    protected abstract ParticleOptions getCloudParticles();

    /**
     * @return The initial radius of the area effect cloud
     */
    protected abstract float initialRadius();

    /**
     * @return Duration of the area effect cloud in ticks
     */
    protected abstract int durationTicks();

    protected BiConsumer<Entity, Integer> onVictimInAreaEffectCloudTick()
    {
        return ((entity, integer) -> {});
    }

    protected DamageSource impactDamageSource(ServerLevel serverLevel, LivingEntity hitEntity, Entity owner)
    {
        return null;
    }

    protected float impactDamage(Entity owner)
    {
        return 0f;
    }

    protected float impactDamageRadius()
    {
        return this.initialRadius();
    }

    @Override
    protected void onHit(HitResult hitResult)
    {
        if (this.level().isClientSide())
        {
            return;
        }
        super.onHit(hitResult);
        if (this.impactDamageRadius() > 0)
        {
            for (LivingEntity livingEntity : TMCServerUtils.getEntitiesInRadius(this, LivingEntity.class, this.impactDamageRadius()))
            {
                if (this.owner == null || this.owner.matches(livingEntity))
                {
                    continue;
                }
                if (!(this.level() instanceof ServerLevel serverLevel))
                {
                    return;
                }
                DamageSource damageSource = this.impactDamageSource(serverLevel, livingEntity, this.getOwner());
                if (damageSource == null)
                {
                    return;
                }
                livingEntity.hurtServer(serverLevel, damageSource, this.impactDamage(this.getOwner()));
                EnchantmentHelper.doPostAttackEffects(serverLevel, livingEntity, damageSource);
            }
        }
        if (hitResult.getType() == HitResult.Type.ENTITY && this.ownedBy(((EntityHitResult)hitResult).getEntity()))
        {
            return;
        }

        List<LivingEntity> entitiesOfClass = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0, 2.0, 4.0));
        TMCAreaEffectCloud cloud = new TMCAreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ(), this.onVictimInAreaEffectCloudTick());
        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity)
        {
            LivingEntity livingEntity = (LivingEntity)owner;
            cloud.setOwner(livingEntity);
        }
        cloud.setCustomParticle(this.getCloudParticles());
        cloud.setRadius(this.initialRadius());
        cloud.setDuration(this.durationTicks());
        cloud.setRadiusPerTick((7.0f - cloud.getRadius()) / (float) cloud.getDuration());
        cloud.setPotionDurationScale(0.25F);
        this.effects().forEach(cloud::addEffect);
        if (!entitiesOfClass.isEmpty())
        {
            for (LivingEntity entity : entitiesOfClass)
            {
                double dist = this.distanceToSqr(entity);
                if (!(dist < 16.0))
                {
                    continue;
                }
                cloud.setPos(entity.getX(), entity.getY(), entity.getZ());
                break;
            }
        }
        this.level().levelEvent(2006, this.blockPosition(), this.isSilent() ? -1 : 1);
        this.level().addFreshEntity(cloud);
        this.discard();
    }

//    @Override
//    protected void onHitEntity(EntityHitResult hitResult)
//    {
//        Level level = this.level();
//        if (!(level instanceof ServerLevel serverLevel))
//        {
//            return;
//        }
//        Entity entity = hitResult.getEntity();
//        Entity owner = this.getOwner();
//        DamageSource damageSource = this.impactDamageSource(serverLevel, entity, owner);
//        if (damageSource == null)
//        {
//            return;
//        }
//        entity.hurtServer(serverLevel, damageSource, this.impactDamage(owner));
//        EnchantmentHelper.doPostAttackEffects(serverLevel, entity, damageSource);
//    }

    @Override
    protected boolean shouldBurn()
    {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage)
    {
        return false;
    }

    @Override
    public boolean deflect(ProjectileDeflection deflection, @Nullable Entity deflectingEntity, @Nullable EntityReference<Entity> newOwner, boolean byAttack, Vec3 power)
    {
        if (this.level().isClientSide())
        {
            return super.deflect(deflection, deflectingEntity, newOwner, byAttack, power);
        }
        return false;
    }

    @Override
    public boolean deflect(ProjectileDeflection deflection, @Nullable Entity deflectingEntity, @Nullable EntityReference<Entity> newOwner, boolean byAttack, double power)
    {
        if (this.level().isClientSide())
        {
            return super.deflect(deflection, deflectingEntity, newOwner, byAttack, power);
        }
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion)
    {
        return true;
    }

    @Override
    public boolean shouldBeSaved()
    {
        return true;
    }
}

package tmc.entity.fireball;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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

    /**
     * Re-implementation of {@linkplain net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball#onHit(HitResult) Dragon fireball's onHit method}
     * <p>
     *     Calls this class's version of {@linkplain Projectile#onHit(HitResult) Projectile's onHit} to make this projectile non-deflectable
     *     and uses the dragon fireball's area effect cloud logic to create an area effect cloud at the area of impact.
     * </p>
     * @param hitResult
     */
    @Override
    protected void onHit(HitResult hitResult)
    {
        if (this.level().isClientSide())
        {
            return;
        }
        this.onHitProjectile(hitResult);
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

    @Override
    protected boolean shouldBurn()
    {
        return false;
    }

    /**
     * Re-implementation of {@linkplain net.minecraft.world.entity.projectile.Projectile#onHit(HitResult) Projectile's onHit method}.
     * This is to allow the projectile to be non-deflectable (similar to the dragon fireball) since the original function checks if
     * the projectile has the {@linkplain net.minecraft.tags.EntityTypeTags#REDIRECTABLE_PROJECTILE redirectable projectile tag}
     * @param hitResult
     */
    private void onHitProjectile(HitResult hitResult)
    {
        HitResult.Type type = hitResult.getType();
        switch (type)
        {
            case ENTITY -> {
                EntityHitResult entityHitResult = (EntityHitResult) hitResult;
                this.onHitEntity(entityHitResult);
                this.level().gameEvent(GameEvent.PROJECTILE_LAND, hitResult.getLocation(), GameEvent.Context.of(this, null));
            }
            case BLOCK -> {
                BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                this.onHitBlock(blockHitResult);
                BlockPos target = blockHitResult.getBlockPos();
                this.level().gameEvent(GameEvent.PROJECTILE_LAND, target, GameEvent.Context.of(this, this.level().getBlockState(target)));
            }
        }
    }
}

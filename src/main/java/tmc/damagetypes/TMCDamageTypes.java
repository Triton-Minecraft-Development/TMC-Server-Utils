package tmc.damagetypes;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import tmc.TMCServerUtils;

public class TMCDamageTypes {

    public static final ResourceKey<DamageType> ENDER_DRAGON_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, TMCServerUtils.of("ender_dragon_magic"));


    public static DamageSource getDamageSource(ResourceKey<DamageType> damageTypeResourceKey, ServerLevel serverLevel)
    {
        return new DamageSource(
                serverLevel.registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .get(damageTypeResourceKey.identifier()).orElseThrow()
        );
    }

    public static DamageSource getDamageSource(ResourceKey<DamageType> damageTypeResourceKey, ServerLevel serverLevel, LivingEntity victim, LivingEntity attacker)
    {
        return new DamageSource(
                serverLevel.registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .get(damageTypeResourceKey.identifier()).orElseThrow(),
                victim,
                attacker
        );
    }

    public static void initialize() {}

}

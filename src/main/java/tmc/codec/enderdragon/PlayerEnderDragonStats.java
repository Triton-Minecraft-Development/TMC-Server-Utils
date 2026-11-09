package tmc.codec.enderdragon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class PlayerEnderDragonStats {

    public static final Codec<PlayerEnderDragonStats> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.fieldOf("dragons_slayed")
                            .forGetter(PlayerEnderDragonStats::getKills),
                    Codec.FLOAT.fieldOf("best_dragon_damage")
                            .forGetter(PlayerEnderDragonStats::getBestDamage)
            ).apply(instance, PlayerEnderDragonStats::new)
    );

    private int kills = 0;
    private float bestDamage = 0;

    public PlayerEnderDragonStats() {}

    public PlayerEnderDragonStats(int kills, float bestDamage)
    {
        this.kills = kills;
        this.bestDamage = bestDamage;
    }

    public PlayerEnderDragonStats updateKills()
    {
        this.kills++;
        return this;
    }

    public int getKills()
    {
        return this.kills;
    }

    public PlayerEnderDragonStats updateBestDamage(float damage)
    {
        this.bestDamage = Math.max(this.bestDamage, damage);
        return this;
    }

    public float getBestDamage()
    {
        return this.bestDamage;
    }
}

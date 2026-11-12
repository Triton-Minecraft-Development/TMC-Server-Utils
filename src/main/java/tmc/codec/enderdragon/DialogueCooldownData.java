package tmc.codec.enderdragon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class DialogueCooldownData {

    public static final Codec<DialogueCooldownData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.fieldOf("take_damage").forGetter(DialogueCooldownData::getTakeDamageCooldown),
                    Codec.INT.fieldOf("kill_player").forGetter(DialogueCooldownData::getKillPlayerCooldown),
                    Codec.INT.fieldOf("taunt").forGetter(DialogueCooldownData::getTauntCooldown),
                    Codec.INT.fieldOf("attack").forGetter(DialogueCooldownData::getAttackCooldown)
            ).apply(instance, DialogueCooldownData::new)
    );

    public static final int TAKE_DAMAGE_COOLDOWN_TICKS = 300;
    public static final int KILL_PLAYER_COOLDOWN_TICKS = 200;
    public static final int TAUNT_COOLDOWN_TICKS = 600;
    public static final int ATTACK_COOLDOWN_TICKS = 400;

    private int takeDamageCooldown;
    private int killPlayerCooldown;
    private int tauntCooldown;
    private int attackCooldown;

    public DialogueCooldownData()
    {
        this(0, 0, 0, 0);
    }

    public DialogueCooldownData(int takeDamageCooldown, int killPlayerCooldown, int tauntCooldown, int attackCooldown)
    {
        this.takeDamageCooldown = takeDamageCooldown;
        this.killPlayerCooldown = killPlayerCooldown;
        this.tauntCooldown = tauntCooldown;
        this.attackCooldown = attackCooldown;
    }

    public int getTakeDamageCooldown()
    {
        return takeDamageCooldown;
    }

    public DialogueCooldownData setTakeDamageCooldown(int takeDamageCooldown)
    {
        this.takeDamageCooldown = takeDamageCooldown;
        return this;
    }

    public boolean isTakeDamageReady(int ticks)
    {
        return ticks > this.takeDamageCooldown;
    }

    public int getKillPlayerCooldown()
    {
        return killPlayerCooldown;
    }

    public DialogueCooldownData setKillPlayerCooldown(int killPlayerCooldown)
    {
        this.killPlayerCooldown = killPlayerCooldown;
        return this;
    }

    public boolean isKillPlayerReady(int ticks)
    {
        return ticks > this.killPlayerCooldown;
    }

    public int getTauntCooldown()
    {
        return tauntCooldown;
    }

    public DialogueCooldownData setTauntCooldown(int attackCooldown)
    {
        this.tauntCooldown = attackCooldown;
        return this;
    }

    public boolean isTauntReady(int ticks)
    {
        return ticks > this.tauntCooldown;
    }

    public int getAttackCooldown()
    {
        return attackCooldown;
    }

    public DialogueCooldownData setAttackCooldown(int ticks)
    {
        this.attackCooldown = ticks;
        return this;
    }

    public boolean isAttackReady(int ticks)
    {
        return ticks > this.attackCooldown;
    }
}

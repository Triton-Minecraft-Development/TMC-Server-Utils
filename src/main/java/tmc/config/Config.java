package tmc.config;

import tmc.GameRules;

import java.time.LocalDate;
import java.util.EnumMap;

public class Config
{
    public EnumMap<GameRules, Boolean> gameRules = new EnumMap<>(GameRules.class);

    public LocalDate serverStartDate = LocalDate.of(2026, 10, 9);

    public Config()
    {
        gameRules.put(GameRules.DISABLE_EXPLOSION_DAMAGE, false);
        gameRules.put(GameRules.PVP_EXPLOSIONS_DONT_DELETE_DROPS, true);
        gameRules.put(GameRules.HOPPER_MINECARTS_RETAIN_DEFAULT_SPEED, true);
    }
}

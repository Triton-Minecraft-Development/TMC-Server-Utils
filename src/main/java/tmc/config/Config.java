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
    }
}

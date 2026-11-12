package tmc.entity.enderdragon;

public enum EnderDragonStage {

    /**
     * Vanilla stage of the Ender Dragon fight
     */
    VANILLA,

    /**
     * Stage 2 of the Ender Dragon fight.
     * Happens as soon as a killing blow is landed on
     * the {@linkplain EnderDragonStage#VANILLA vanilla stage}
     */
    STAGE_2,

    /**
     * The death stage of the fight. Happens as soon as
     * a killing blow is landed on the {@linkplain EnderDragonStage#STAGE_2 second stage}
     */
    DEATH
}

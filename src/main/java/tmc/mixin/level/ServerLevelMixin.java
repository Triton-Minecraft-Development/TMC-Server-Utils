package tmc.mixin.level;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tmc.attachments.TMCDataAttachments;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initEnderDragonLeaderboard(MinecraftServer server, Executor executor, LevelStorageSource.LevelStorageAccess levelStorage, ServerLevelData levelData, ResourceKey dimension, LevelStem levelStem, boolean isDebug, long biomeZoomSeed, List customSpawners, boolean tickTime, CallbackInfo ci)
    {
        ServerLevel serverLevel = (ServerLevel) (Object) this;

        // Attached to all levels, but only query the Overworld to update
        serverLevel.getAttachedOrCreate(TMCDataAttachments.SERVER_ENDER_DRAGON_LEADERBOARD);

        // Attached to all levels, but only query the End to update
        serverLevel.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_KEEP_INVENTORY);
    }
}

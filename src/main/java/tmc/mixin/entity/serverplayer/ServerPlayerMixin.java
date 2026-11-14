package tmc.mixin.entity.serverplayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tmc.TMCServerUtils;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.InvolvedPlayersData;

import java.util.Optional;
import java.util.UUID;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Unique
    private static final double DISTANCE = 500;

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initTmcData(MinecraftServer server, ServerLevel level, GameProfile gameProfile, ClientInformation clientInformation, CallbackInfo ci)
    {
        ServerPlayer player = (ServerPlayer) (Object) this;

        player.getAttachedOrCreate(TMCDataAttachments.PLAYER_WARNING_TITLES_DATA);
    }

    /**
     * Keep Inventory when fighting the Ender Dragon
     * <p>
     *     Players who enter combat near/with the Ender Dragon will have
     *     keep inventory automatically enabled for them.
     * </p>
     * <p>
     *     Keep Inventory will turn off for players who exit the {@linkplain ServerPlayerMixin#DISTANCE specified distance}
     *     from the Ender Dragon, or origin of the End Dimension if no dragon is present yet a Dragon Fight is active.
     * </p>
     * @param oldPlayer
     * @param restoreAll
     * @param ci
     */
    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void restoreFrom(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci)
    {
        ServerPlayer current = (ServerPlayer) (Object) this;
        ServerLevel end = this.server.getLevel(ServerLevel.END);
        if (end == null)
        {
            return;
        }
        InvolvedPlayersData data = end.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_KEEP_INVENTORY);
        if (!data.isPlayerInvolved(current))
        {
            if (oldPlayer.getLastAttacker() != null && oldPlayer.getLastAttacker() instanceof EnderDragon dragon)
            {
                current.transferInventoryXpAndScore(oldPlayer);
                end.modifyAttached(TMCDataAttachments.ENDER_DRAGON_KEEP_INVENTORY, d -> d.removePlayer(current));
                return;
            }
        }
        EnderDragonFight fight = end.getDragonFight();
        UUID dragonUUID = fight.dragonUUID(); // EnderDragonFight is NEVER null for the End server level
        if (dragonUUID == null)
        {
            // Dragon is missing
            // Tie range to origin of the world
            return;
        }
        // Tie range to the current dragon

        current.transferInventoryXpAndScore(oldPlayer);
        end.modifyAttached(TMCDataAttachments.ENDER_DRAGON_KEEP_INVENTORY, d -> d.removePlayer(current));
    }

    @Unique
    private boolean isInRange(ServerPlayer oldPlayer, Vec3 targetPos)
    {
        double xzDistance = Math.sqrt(Math.pow(oldPlayer.getX() - targetPos.x, 2) + Math.pow(oldPlayer.getZ() - targetPos.z, 2));
        return xzDistance <= DISTANCE;
    }
}

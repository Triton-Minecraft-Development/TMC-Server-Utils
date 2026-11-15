package tmc.mixin.entity.player;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tmc.attachments.TMCDataAttachments;
import tmc.codec.InvolvedPlayersData;
import tmc.entity.enderdragon.TMCEnderDragonManager;

import java.util.UUID;

import static tmc.TMCServerUtils.isInRange;


@Mixin(Player.class)
public class PlayerMixin {

    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    private void dropEquipment(ServerLevel level, CallbackInfo ci)
    {
        Player player = (Player) (Object) this;
        if (!(player instanceof ServerPlayer serverPlayer))
        {
            return;
        }
        ServerLevel overworld = level.getServer().overworld();
        InvolvedPlayersData data = overworld.getAttachedOrCreate(TMCDataAttachments.ENDER_DRAGON_KEEP_INVENTORY);
        if (data.isPlayerInvolved(serverPlayer))
        {
            ci.cancel();
            return;
        }


        if (isInRange(serverPlayer, Vec3.ZERO, TMCEnderDragonManager.KEEP_INVENTORY_DISTANCE))
        {
            ci.cancel();
        }
    }
}

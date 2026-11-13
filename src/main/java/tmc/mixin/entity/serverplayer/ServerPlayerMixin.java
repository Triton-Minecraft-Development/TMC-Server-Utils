package tmc.mixin.entity.serverplayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tmc.attachments.TMCDataAttachments;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initTmcData(MinecraftServer server, ServerLevel level, GameProfile gameProfile, ClientInformation clientInformation, CallbackInfo ci)
    {
        ServerPlayer player = (ServerPlayer) (Object) this;

        player.getAttachedOrCreate(TMCDataAttachments.PLAYER_WARNING_TITLES_DATA);
    }
}

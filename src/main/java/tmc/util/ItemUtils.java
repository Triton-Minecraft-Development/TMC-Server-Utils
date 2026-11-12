package tmc.util;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class ItemUtils {

    public static ItemStack createCustomHead(String textureHash)
    {
        JsonObject skinJson = createSkinJson("https://textures.minecraft.net/texture/" + textureHash);
        String base64Texture = Base64.getEncoder().encodeToString(skinJson.toString().getBytes(StandardCharsets.UTF_8));
        PropertyMap propertyMap = new PropertyMap(
                ImmutableMultimap.of("textures", new Property("textures", base64Texture))
        );

        UUID uuid = UUID.nameUUIDFromBytes(base64Texture.getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(uuid, "", propertyMap);

        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
        return head;
    }

    private static JsonObject createSkinJson(String url)
    {
        JsonObject root = new JsonObject();
        JsonObject skin = new JsonObject();
        JsonObject urlJson = new JsonObject();

        urlJson.addProperty("url", url);
        skin.add("SKIN", urlJson);
        root.add("textures", skin);
        return root;
    }
}

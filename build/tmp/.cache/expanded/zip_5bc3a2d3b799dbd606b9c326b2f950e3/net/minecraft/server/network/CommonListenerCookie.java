package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ClientInformation;

public record CommonListenerCookie(GameProfile gameProfile, int latency, ClientInformation clientInformation, boolean transferred) {
    public static CommonListenerCookie createInitial(GameProfile pGameProfile, boolean pTransferred) {
        return new CommonListenerCookie(pGameProfile, 0, ClientInformation.createDefault(), pTransferred);
    }
}
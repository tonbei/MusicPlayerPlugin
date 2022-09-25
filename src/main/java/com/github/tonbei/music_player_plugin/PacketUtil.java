package com.github.tonbei.music_player_plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PacketUtil {

    public static final Gson GSON = new GsonBuilder().enableComplexMapKeySerialization().disableHtmlEscaping().create();
    public static final MinecraftKey CHANNEL = new MinecraftKey("musicplayer", "multi");

    public static void sendFirstTrackAudioData(MusicPlayerManager manager, Collection<? extends Player> receivers) {
        Map<String, String> sendData = new HashMap<>();
        sendData.put("playStatus", "PLAY");
        sendData.put("URL", manager.getPlayingTrackUrl());
        if (manager.isLoop()) sendData.put("LOOP", "loop");
        sendPacket(receivers, sendData);
    }

    public static void sendStopPlayerData(Collection<? extends Player> receivers) {
        Map<String, String> sendData = new HashMap<>();
        sendData.put("playStatus", "STOP");
        sendPacket(receivers, sendData);
    }

    public static void sendPacket(Collection<? extends Player> receivers, Map<String, String> sendData) {
        byte[] byteData = GSON.toJson(sendData).getBytes(StandardCharsets.UTF_8);
        sendPacket(receivers, byteData);
    }

    private static void sendPacket(Collection<? extends Player> receivers, byte[] sendData) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.CUSTOM_PAYLOAD);
        packet.getMinecraftKeys().write(0, CHANNEL);

        Object serializer = MinecraftReflection.getPacketDataSerializer(Unpooled.copiedBuffer(sendData));
        packet.getModifier().withType(ByteBuf.class).write(0, serializer);

        receivers.forEach(receiver -> {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, packet);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        });
    }
}

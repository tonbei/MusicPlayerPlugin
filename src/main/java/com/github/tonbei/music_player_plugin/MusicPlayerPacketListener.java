package com.github.tonbei.music_player_plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.google.gson.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MusicPlayerPacketListener extends PacketAdapter {

    private MusicPlayerPlugin plugin;

    public MusicPlayerPacketListener(MusicPlayerPlugin plugin) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.CUSTOM_PAYLOAD);
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceiving(PacketEvent e) {
        PacketContainer packet = e.getPacket();
        MinecraftKey channel = packet.getMinecraftKeys().read(0);

        if (MusicPlayerPlugin.CHANNEL.getFullKey().equalsIgnoreCase(channel.getFullKey())) {
            ByteBuf buf = (ByteBuf) packet.getModifier().withType(ByteBuf.class).read(0);
            Type mapTokenType = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> packetData = MusicPlayerPlugin.GSON.fromJson(new String(ByteBufUtil.getBytes(buf), StandardCharsets.UTF_8), mapTokenType);

            plugin.getLogger().warning(packetData.toString());

            if (!MusicPlayerPlugin.playList.isEmpty()){
                Player sender = e.getPlayer();
                Map<String, String> sendData = new HashMap<>();
                sendData.put("playStatus", "PLAY");

                if (StringUtils.isNotBlank(packetData.get("endTrack"))) {
                    if (MusicPlayerPlugin.playList.getFirst().getDate().equalsIgnoreCase(packetData.get("endTrack"))) {
                        MusicPlayerPlugin.playList.removeFirst();
                        if (MusicPlayerPlugin.playList.isEmpty()) return;
                        Track firstTrack = MusicPlayerPlugin.playList.getFirst();
                        sendData.put("URL", firstTrack.getUrl());
                        if (firstTrack.isLoop()) packetData.put("LOOP", "loop");
                        sendData.put("timeStamp", firstTrack.getDate());
                    } else {
                        Track firstTrack = MusicPlayerPlugin.playList.getFirst();
                        sendData.put("URL", firstTrack.getUrl());
                        if (firstTrack.isLoop()) packetData.put("LOOP", "loop");
                        sendData.put("timeStamp", firstTrack.getDate());
                    }
                } else {
                    Track firstTrack = MusicPlayerPlugin.playList.getFirst();
                    sendData.put("URL", firstTrack.getUrl());
                    if (firstTrack.isLoop()) packetData.put("LOOP", "loop");
                    sendData.put("timeStamp", firstTrack.getDate());
                }

                MusicPlayerPlugin.sendPacket(sender, sendData);
            }
        }
    }
}

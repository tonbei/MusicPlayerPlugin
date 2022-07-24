package com.github.tonbei.music_player_plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public final class MusicPlayerPlugin extends JavaPlugin implements Listener {

    public static final Gson GSON = new GsonBuilder().enableComplexMapKeySerialization().disableHtmlEscaping().create();

    public static final MinecraftKey CHANNEL = new MinecraftKey("musicplayer", "multi");

    //public static String previousTrack = "";
    public static final LinkedList<Track> playList = new LinkedList<>();

    private ProtocolManager manager;

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(new MusicPlayerPacketListener(this));
    }

    @Override
    public void onDisable() {
        manager.removePacketListeners(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //if (!(sender instanceof Player)) return false;
        if (args.length <= 0) return false;

        Map<String, String> packetData = new HashMap<>();

        if (args[0].equalsIgnoreCase("play")) {
            packetData.put("playStatus", "PLAY");
            if (args.length > 1) packetData.put("URL", args[1]);
            else return false;

            if (args.length > 2 && args[2].equalsIgnoreCase("loop"))
                packetData.put("LOOP", args[2]);
        } else if (args[0].equalsIgnoreCase("stop")) {
            packetData.put("playStatus", "STOP");
        } else if (args[0].equalsIgnoreCase("sync")) {
            if (playList.isEmpty()) {
                packetData.put("playStatus", "STOP");
            } else {
                packetData.put("playStatus", "PLAY");
                Track firstTrack = MusicPlayerPlugin.playList.getFirst();
                packetData.put("URL", firstTrack.getUrl());
                if (firstTrack.isLoop()) packetData.put("LOOP", "loop");
                packetData.put("timeStamp", firstTrack.getDate());
            }
            sendPacket(sender instanceof Player ? Collections.singletonList((Player) sender) : Bukkit.getOnlinePlayers(), packetData);
            return true;
        } else if (args[0].equalsIgnoreCase("next")) {
            if (playList.isEmpty()) {
                packetData.put("playStatus", "STOP");
            } else {
                MusicPlayerPlugin.playList.removeFirst();
                if (MusicPlayerPlugin.playList.isEmpty()) {
                    packetData.put("playStatus", "STOP");
                } else {
                    packetData.put("playStatus", "PLAY");
                    Track firstTrack = MusicPlayerPlugin.playList.getFirst();
                    packetData.put("URL", firstTrack.getUrl());
                    if (firstTrack.isLoop()) packetData.put("LOOP", "loop");
                    packetData.put("timeStamp", firstTrack.getDate());
                }
            }
            sendPacket(Bukkit.getOnlinePlayers(), packetData);
            return true;
        } else if (args[0].equalsIgnoreCase("list")) {
            StringBuilder builder = new StringBuilder();
            builder.append("MusicPlayer PlayList:\n");
            playList.forEach(track -> builder.append("URL: ").append(track.getUrl()).append(" / ").append("Loop: ").append(track.isLoop()).append("\n"));
            sender.sendMessage(builder.toString());
            return true;
        } else {
            return false;
        }

        //byte[] byteData = GSON.toJson(packetData).getBytes(StandardCharsets.UTF_8);
        //sendPacket(Bukkit.getOnlinePlayers(), byteData);
        if (packetData.containsKey("URL")) {
            String date = OffsetDateTime.now(ZoneOffset.UTC).toString();
            packetData.put("timeStamp", date);

            if (playList.isEmpty()) {
                sendPacket(Bukkit.getOnlinePlayers(), packetData);
            }
            playList.add(new Track(date, packetData.get("URL"), packetData.containsKey("LOOP")));
        } else if (args[0].equalsIgnoreCase("stop")) {
            sendPacket(Bukkit.getOnlinePlayers(), packetData);
        }

        getLogger().warning(packetData.toString());

        return true;
    }

    public static void sendPacket(Player receiver, Object sendData) {
        byte[] byteData = GSON.toJson(sendData).getBytes(StandardCharsets.UTF_8);
        sendPacket(receiver, byteData);
    }

    public static void sendPacket(Collection<? extends Player> receivers, Object sendData) {
        byte[] byteData = GSON.toJson(sendData).getBytes(StandardCharsets.UTF_8);
        sendPacket(receivers, byteData);
    }

    public static void sendPacket(Player receiver, byte[] sendData) {
        sendPacket(Collections.singletonList(receiver), sendData);
    }

    public static void sendPacket(Collection<? extends Player> receivers, byte[] sendData) {
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Map<String, String> packetData = new HashMap<>();

        if (playList.isEmpty()) {
            packetData.put("playStatus", "STOP");
        } else {
            packetData.put("playStatus", "PLAY");
            Track firstTrack = MusicPlayerPlugin.playList.getFirst();
            packetData.put("URL", firstTrack.getUrl());
            if (firstTrack.isLoop()) packetData.put("LOOP", "loop");
            packetData.put("timeStamp", firstTrack.getDate());
        }
        sendPacket(e.getPlayer(), packetData);
    }
}

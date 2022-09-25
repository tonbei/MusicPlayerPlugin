package com.github.tonbei.music_player_plugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Collections;

public final class MusicPlayerPlugin extends JavaPlugin implements Listener {

    private ProtocolManager manager;
    private MusicPlayerManager musicPlayerManager;

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(new MusicPlayerPacketListener(this));
        musicPlayerManager = new MusicPlayerManager(this);
    }

    @Override
    public void onDisable() {
        manager.removePacketListeners(this);
        musicPlayerManager.shutdown();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length <= 0) return false;

        if (args[0].equalsIgnoreCase("play")) {
            String url;
            boolean loop = false;

            if (args.length > 1) url = args[1];
            else return false;

            if (args.length > 2 && args[2].equalsIgnoreCase("loop")) {
                loop = true;
            }
            musicPlayerManager.playTrack(url, loop);
        } else if (args[0].equalsIgnoreCase("stop")) {
            if (musicPlayerManager.isPlaying()) musicPlayerManager.stop();
//            PacketUtil.sendStopPlayerData(Bukkit.getOnlinePlayers());
        } else if (args[0].equalsIgnoreCase("sync")) {
            Collection<? extends Player> receivers = sender instanceof Player ? Collections.singletonList((Player) sender) : Bukkit.getOnlinePlayers();

            if (!musicPlayerManager.isPlaying()) {
                PacketUtil.sendStopPlayerData(receivers);
            } else {
                PacketUtil.sendFirstTrackAudioData(musicPlayerManager, receivers);
            }
        } else if (args[0].equalsIgnoreCase("next")) {
            musicPlayerManager.playNextTrack();
//            if (musicPlayerManager.playNextTrack() == null) {
//                PacketUtil.sendStopPlayerData(Bukkit.getOnlinePlayers());
//            } else {
//                PacketUtil.sendFirstTrackAudioData(musicPlayerManager, Bukkit.getOnlinePlayers());
//            }
        } else if (args[0].equalsIgnoreCase("list")) {
            StringBuilder builder = new StringBuilder();
            builder.append("MusicPlayer PlayList:\n");
            musicPlayerManager.getTrackList().forEach(track -> builder.append("URL: ").append(track.getUrl()).append(" / ").append("Loop: ").append(track.isLoop()).append("\n"));
            sender.sendMessage(builder.toString());
        } else {
            return false;
        }

        return true;
    }

    public MusicPlayerManager getMusicPlayerManager() {
        return musicPlayerManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (!musicPlayerManager.isPlaying()) {
            PacketUtil.sendStopPlayerData(Collections.singletonList(e.getPlayer()));
        } else {
            PacketUtil.sendFirstTrackAudioData(musicPlayerManager, Collections.singletonList(e.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e) {
        PacketUtil.sendStopPlayerData(Collections.singletonList(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent e) {
        PacketUtil.sendStopPlayerData(Collections.singletonList(e.getPlayer()));
    }
}

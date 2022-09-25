package com.github.tonbei.music_player_plugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
            musicPlayerManager.playTrack(url, loop, result -> {
                switch (result.getFirst()) {
                    case START:
                        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage("Start track: " + result.getSecond()));
                        break;
                    case QUEUE:
                        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage("Queue track: " + result.getSecond()));
                        break;
                    case NO_MATCH:
                        sender.sendMessage("Nothing found by " + result.getSecond());
                        break;
                    case FAILED:
                        sender.sendMessage("Could not play: " + result.getSecond());
                        break;
                }
            });
        } else if (args[0].equalsIgnoreCase("stop")) {
            if (musicPlayerManager.isPlaying()) {
                musicPlayerManager.stop();
                Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage("Stop track"));
            }
        } else if (args[0].equalsIgnoreCase("sync")) {
            Collection<? extends Player> receivers = sender instanceof Player ? Collections.singletonList((Player) sender) : Bukkit.getOnlinePlayers();
            if (!musicPlayerManager.isPlaying()) {
                PacketUtil.sendStopPlayerData(receivers);
            } else {
                PacketUtil.sendFirstTrackAudioData(musicPlayerManager, receivers);
            }
            receivers.forEach(player -> player.sendMessage("Sync track data"));
        } else if (args[0].equalsIgnoreCase("next")) {
            String playUrl = musicPlayerManager.playNextTrack();
            if (playUrl == null) {
                Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage("Stop track"));
            } else {
                Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage("Start next track: " + playUrl));
            }
        } else if (args[0].equalsIgnoreCase("list")) {
            int index = 0;
            ComponentBuilder message = new ComponentBuilder("MusicPlayer PlayList:");
            for (Track track : musicPlayerManager.getTrackList()) {
                String trackTitle = track.getTrack().getInfo().title;
                message.append("\n" + ++index + ". ")
                        .append(truncateBytes(trackTitle, Charset.forName("Shift_JIS"), 45))
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(trackTitle)))
                        .event(new ClickEvent(ClickEvent.Action.OPEN_URL, track.getUrl()))
                        .append(" / Loop: " + track.isLoop()).event((HoverEvent) null).event((ClickEvent) null);
            }
            sender.spigot().sendMessage(message.create());
        } else {
            return false;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("play", "stop", "list", "next", "sync"), new ArrayList<>());
        }

        if (args[0].equalsIgnoreCase("play")) {
            if (args.length == 2){
                return Collections.singletonList("<URL>");
            } else if (args.length == 3) {
                return StringUtil.copyPartialMatches(args[2], Collections.singletonList("loop"), new ArrayList<>());
            }
        }

        return super.onTabComplete(sender, command, alias, args);
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
            e.getPlayer().sendMessage("Start track: " + musicPlayerManager.getPlayingTrackUrl());
        }
    }

    private String truncateBytes(String s, Charset charset, int maxBytes) {
        ByteBuffer bb = ByteBuffer.allocate(maxBytes);
        CharBuffer cb = CharBuffer.wrap(s);
        CharsetEncoder encoder = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .reset();
        CoderResult cr = encoder.encode(cb, bb, true);
        if (!cr.isOverflow()) {
            return s;
        }
        encoder.flush(bb);
        return cb.flip().toString() + "...";
    }
}

package com.github.tonbei.music_player_plugin;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.Collections;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackManager {

    private final MusicPlayerPlugin musicPlayerPlugin;
    private final BlockingQueue<Track> queue;

    private final Timer timer;
    private final AudioTimerTask timerTask;

    public TrackManager(MusicPlayerPlugin plugin) {
        musicPlayerPlugin = plugin;
        queue = new LinkedBlockingQueue<>();
        timer = new Timer(true);
        timerTask = new AudioTimerTask(this);
        timer.scheduleAtFixedRate(timerTask, 0L, 1000L);
    }

    public void shutdown() {
        timer.cancel();
    }

    public void stop() {
        timerTask.stop();
        musicPlayerPlugin.getLogger().warning("Stop track");
        PacketUtil.sendStopPlayerData(Bukkit.getOnlinePlayers());
    }

    public boolean setTrack(AudioTrack audioTrack, boolean loop) {
        Track track = new Track(audioTrack, loop);

        if (!timerTask.startTrack(track, true)) {
            musicPlayerPlugin.getLogger().warning("Queue track: " + audioTrack.getInfo().uri);
            queue.offer(track);
            return false;
        } else {
            musicPlayerPlugin.getLogger().warning("Start track: " + audioTrack.getInfo().uri);
            PacketUtil.sendFirstTrackAudioData(musicPlayerPlugin.getMusicPlayerManager(), Bukkit.getOnlinePlayers());
            return true;
        }
    }

    public String nextTrack() {
        if (hasNextTrack()) {
            Track track = queue.poll();
            timerTask.startTrack(track, false);
            musicPlayerPlugin.getLogger().warning("Start next track: " + track.getUrl());
            PacketUtil.sendFirstTrackAudioData(musicPlayerPlugin.getMusicPlayerManager(), Bukkit.getOnlinePlayers());
            return track.getUrl();
        }
        stop();
        return null;
    }

    public boolean hasNextTrack() {
        return !queue.isEmpty();
    }

    public Track getPlayingTrack() {
        return timerTask.getPlayingTrack();
    }

    public Collection<Track> getQueue() {
        return Collections.unmodifiableCollection(queue);
    }
}

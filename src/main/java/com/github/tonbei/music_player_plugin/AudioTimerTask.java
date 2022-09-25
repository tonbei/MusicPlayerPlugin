package com.github.tonbei.music_player_plugin;

import org.bukkit.Bukkit;

import java.util.TimerTask;

public class AudioTimerTask extends TimerTask {

    private final TrackManager trackManager;
    private long time;
    private Track playingTrack;

    public AudioTimerTask(TrackManager manager) {
        trackManager = manager;
    }

    @Override
    public void run() {
        if (playingTrack != null && !playingTrack.isLoop() && !playingTrack.getTrack().getInfo().isStream) {
            if (time > 0) {
                time--;
                //Bukkit.getLogger().severe("time: " + time);
            } else if (time == 0) {
                playingTrack = null;
                trackManager.nextTrack();
            }
        }
    }

    public boolean startTrack(Track track, boolean noInterrupt) {
        if (!noInterrupt || playingTrack == null) {
            time = track.getTrack().getDuration() / 1000;
            playingTrack = track;
            return true;
        }
        return false;
    }

    public void stop() {
        playingTrack = null;
    }

    public Track getPlayingTrack() {
        return playingTrack;
    }
}

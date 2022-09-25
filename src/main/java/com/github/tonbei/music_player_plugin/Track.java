package com.github.tonbei.music_player_plugin;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class Track {

    private final AudioTrack track;
    private final String url;
    private final boolean loop;

    public Track(AudioTrack track) {
        this(track, false);
    }

    public Track(AudioTrack track, boolean loop) {
        this.track = track;
        this.url = track.getInfo().uri;
        this.loop = loop;
    }

    public AudioTrack getTrack() {
        return track;
    }

    public String getUrl() {
        return url;
    }

    public boolean isLoop() {
        return loop;
    }
}

package com.github.tonbei.music_player_plugin;

public class Track {

    private final String date;
    private final String url;
    private final boolean loop;

    public Track(String date, String url) {
        this(date, url, false);
    }

    public Track(String date, String url, boolean loop) {
        this.date = date;
        this.url = url;
        this.loop = loop;
    }

    public String getDate() {
        return date;
    }

    public String getUrl() {
        return url;
    }

    public boolean isLoop() {
        return loop;
    }
}

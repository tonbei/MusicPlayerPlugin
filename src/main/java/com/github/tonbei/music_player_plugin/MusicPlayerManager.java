package com.github.tonbei.music_player_plugin;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class MusicPlayerManager {

    private final MusicPlayerPlugin musicPlayerPlugin;
    private final Logger logger;
    private final AudioPlayerManager audioPlayerManager;
    private final TrackManager trackManager;

    public MusicPlayerManager(MusicPlayerPlugin plugin) {
        musicPlayerPlugin = plugin;
        logger = plugin.getLogger();
        audioPlayerManager = new DefaultAudioPlayerManager();
        trackManager = new TrackManager(plugin);
        registerSources();
    }

    private void registerSources() {
        YoutubeAudioSourceManager youtube = new YoutubeAudioSourceManager();
        youtube.setPlaylistPageCount(100);
        audioPlayerManager.registerSourceManager(youtube);
        audioPlayerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        audioPlayerManager.registerSourceManager(new BandcampAudioSourceManager());
        audioPlayerManager.registerSourceManager(new VimeoAudioSourceManager());
        audioPlayerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        audioPlayerManager.registerSourceManager(new BeamAudioSourceManager());
        audioPlayerManager.registerSourceManager(new HttpAudioSourceManager());
        audioPlayerManager.registerSourceManager(new LocalAudioSourceManager());
    }

    public void shutdown() {
        trackManager.shutdown();
        audioPlayerManager.shutdown();
    }

    public void stop() {
        trackManager.stop();
    }

    public boolean hasNextTrack() {
        return trackManager.hasNextTrack();
    }

    public boolean isPlaying() {
        return trackManager.getPlayingTrack() != null;
    }

    public String getPlayingTrackUrl() {
        if (isPlaying()) {
            return trackManager.getPlayingTrack().getUrl();
        }
        return null;
    }

    public boolean isLoop() {
        if (isPlaying()) {
            return trackManager.getPlayingTrack().isLoop();
        }
        return false;
    }

    public List<Track> getTrackList() {
        List<Track> list = new ArrayList<>();
        if (isPlaying()) list.add(trackManager.getPlayingTrack());
        if (hasNextTrack()) list.addAll(trackManager.getQueue());
        return Collections.unmodifiableList(list);
    }

    public String playNextTrack() {
        return trackManager.nextTrack();
    }

    public void playTrack(String uri, boolean loop) {
        audioPlayerManager.loadItem(uri, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                trackManager.setTrack(track, loop);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                for (AudioTrack track : playlist.getTracks()) {
                    trackManager.setTrack(track, false);
                }
            }

            @Override
            public void noMatches() {
                logger.warning("Nothing found by " + uri);
            }

            @Override
            public void loadFailed(FriendlyException ex) {
                logger.severe("Could not play: " + ex.getMessage());
            }
        });
    }
}

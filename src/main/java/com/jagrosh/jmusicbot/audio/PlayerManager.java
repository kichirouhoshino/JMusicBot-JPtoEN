/*
 * Copyright 2018-2020 Cosgy Dev
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.jagrosh.jmusicbot.audio;

import com.jagrosh.jmusicbot.Bot;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import dev.cosgy.jmusicbot.util.YtDlpManager;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.*;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerManager extends DefaultAudioPlayerManager {
    private final Bot bot;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // yt-dlp
    private Path ytDlpPath;

    public PlayerManager(Bot bot) {
        this.bot = bot;
    }

    public void init() {
        try {
            Path botDir = Paths.get("").toAbsolutePath();
            YtDlpManager y = new YtDlpManager(botDir);
            this.ytDlpPath = y.prepare();
            y.startAutoUpdate(Duration.ofHours(6));
            logger.info("yt-dlp ready at {}", ytDlpPath);
        } catch (Exception e) {
            logger.error("Failed to initialize yt-dlp. YouTube fallback will be disabled.", e);
            this.ytDlpPath = null;
        }

        // ==== Source Registration ====
        if (bot.getConfig().isNicoNicoEnabled()) {
            registerSourceManager(
                    new com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager(
                            bot.getConfig().getNicoNicoEmailAddress(),
                            bot.getConfig().getNicoNicoPassword())
            );
        }

        YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager(true);
        yt.setPlaylistPageCount(10);
        if (bot.getConfig().isYouTubeOauth2Enabled()) {
            String refreshToken = bot.getConfig().getYouTubeOauth2RefreshToken();
            yt.useOauth2(refreshToken == null || refreshToken.isBlank() ? null : refreshToken, false);
            logger.info("YouTube OAuth2 has been enabled.");
        }
        registerSourceManager(yt);

        AudioSourceManagers.registerRemoteSources(this);
        AudioSourceManagers.registerLocalSource(this);

        // Encode/Resampling Quality
        if (getConfiguration().getOpusEncodingQuality() != 10) {
            logger.debug("Set OpusEncodingQuality to 10 (previous: {})", getConfiguration().getOpusEncodingQuality());
            getConfiguration().setOpusEncodingQuality(10);
        }
        if (getConfiguration().getResamplingQuality() != AudioConfiguration.ResamplingQuality.HIGH) {
            logger.debug("Set ResamplingQuality to HIGH (previous: {})", getConfiguration().getResamplingQuality().name());
            getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
        }
    }

    public Bot getBot() { return bot; }

    public boolean hasHandler(Guild guild) {
        return guild.getAudioManager().getSendingHandler() != null;
    }

    public AudioHandler setUpHandler(Guild guild) {
        AudioHandler handler;
        if (guild.getAudioManager().getSendingHandler() == null) {
            AudioPlayer player = createPlayer();
            player.setVolume(bot.getSettingsManager().getSettings(guild).getVolume());
            handler = new AudioHandler(this, guild, player);
            player.addListener(handler);

            // Playback exception → Fallback
            player.addListener(new YtDlpExceptionListener(this, player, handler));

            guild.getAudioManager().setSendingHandler(handler);
        } else {
            handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        }
        return handler;
    }

    // =========================
    //  Load stage: Lavaplayer failure → Fallback to yt-dlp
    // =========================
    @Override
    public Future<Void> loadItemOrdered(Object orderingKey, String identifier, AudioLoadResultHandler handler) {
        return super.loadItemOrdered(orderingKey, identifier, new AudioLoadResultHandler() {
            @Override public void trackLoaded(AudioTrack track) { handler.trackLoaded(track); }
            @Override public void playlistLoaded(AudioPlaylist playlist) { handler.playlistLoaded(playlist); }

            @Override
            public void noMatches() {
                if (shouldFallbackToYtDlp(identifier)) {
                    tryFallbackDownload(orderingKey, identifier, handler, null);
                } else handler.noMatches();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                if (shouldFallbackToYtDlp(identifier)) {
                    tryFallbackDownload(orderingKey, identifier, handler, exception);
                } else handler.loadFailed(exception);
            }
        });
    }

    boolean shouldFallbackToYtDlp(String identifier) {
        if (ytDlpPath == null || identifier == null) return false;
        String id = identifier.toLowerCase(Locale.ROOT);
        if (id.startsWith("ytsearch:")) return false;
        if (id.startsWith("http://") || id.startsWith("https://")) {
            return id.contains("youtube.com/") || id.contains("youtu.be/");
        }
        return id.matches("^[a-zA-Z0-9_-]{10,}$"); // Raw ID
    }

    private void tryFallbackDownload(Object orderingKey,
                                     String identifier,
                                     AudioLoadResultHandler handler,
                                     FriendlyException cause) {
        logger.warn("Failed to load YouTube. Falling back to yt-dlp: {}", identifier);
        try {
            Path out = downloadViaYtDlp(identifier);
            if (out == null || !Files.isRegularFile(out))
                throw new IllegalStateException("yt-dlp output not found: " + out);

            // LocalSource expects an absolute path string instead of file://
            super.loadItemOrdered(orderingKey, out.toAbsolutePath().toString(), handler);
        } catch (Exception ex) {
            logger.error("yt-dlp fallback failed: {}", ex.toString());
            if (cause != null) {
                handler.loadFailed(new FriendlyException(
                    "YouTube load failed. yt-dlp fallback also failed: " + ex.getMessage(),
                    FriendlyException.Severity.SUSPICIOUS, cause));
            } else {
                handler.loadFailed(new FriendlyException(
                    "YouTube unmatched. yt-dlp fallback also failed: " + ex.getMessage(),
                    FriendlyException.Severity.SUSPICIOUS, ex));
            }
        }
    }

    Path downloadViaYtDlp(String input) throws Exception {
        Path botRoot = Paths.get("").toAbsolutePath().normalize();
        Path cacheDir = botRoot.resolve("cache");
        Files.createDirectories(cacheDir);

        String url = toYoutubeUrl(input);
        logger.info("Downloading with yt-dlp: {}", url);

        List<String> cmd = new ArrayList<>();
        cmd.add(ytDlpPath.toString());

        // Prioritize formats that Lavaplayer can read without conversion (webm/opus → m4a/aac → others)
        Collections.addAll(cmd,
                "--no-playlist",
                "--ignore-config",
                "--no-progress",
                "--newline",
                "--restrict-filenames",
                "--force-overwrites",
                "-f", "bestaudio[ext=webm][acodec=opus]/bestaudio[ext=m4a]/bestaudio",
                "--no-post-overwrites",
                "--output", cacheDir.resolve("%(id)s.%(ext)s").toString(),
                "--print", "after_move:filepath"
        );

        cmd.add(url);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(botRoot.toFile());
        pb.redirectErrorStream(true);
        // Japanese path support: Fix Python (yt-dlp) output to UTF-8
        pb.environment().put("PYTHONIOENCODING", "utf-8");

        Process proc = pb.start();

        String lastNonEmpty = null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.isBlank()) lastNonEmpty = line.trim();
                logger.debug("[yt-dlp] {}", line);
            }
        }

        if (!proc.waitFor(600, TimeUnit.SECONDS)) {
            proc.destroyForcibly();
            throw new RuntimeException("yt-dlp timeout (600s)");
        }
        if (proc.exitValue() != 0) throw new RuntimeException("yt-dlp exit code=" + proc.exitValue());

        if (lastNonEmpty == null) {
            String id = tryExtractYoutubeId(url);
            if (id == null) throw new IllegalStateException("Final path unknown (print is empty). ID extraction also failed");
            // Search for known extensions
            Path guessWebm = cacheDir.resolve(id + ".webm");
            if (Files.isRegularFile(guessWebm)) return guessWebm;
            Path guessM4a = cacheDir.resolve(id + ".m4a");
            if (Files.isRegularFile(guessM4a)) return guessM4a;
            throw new FileNotFoundException("Output unknown");
        }

        Path out = Paths.get(lastNonEmpty);
        if (!out.isAbsolute()) out = botRoot.resolve(out).normalize();
        if (!Files.isRegularFile(out)) throw new FileNotFoundException("No output exists: " + out);
        logger.info("yt-dlp completed: {}", out);
        return out;
    }

    private String toYoutubeUrl(String input) {
        String s = input == null ? "" : input.trim();
        if (s.startsWith("http://") || s.startsWith("https://")) return s;
        return "https://www.youtube.com/watch?v=" + s;
    }

    private String tryExtractYoutubeId(String url) {
        try {
            int vIndex = url.indexOf("v=");
            if (vIndex >= 0) {
                String v = url.substring(vIndex + 2);
                int amp = v.indexOf('&');
                return amp > 0 ? v.substring(0, amp) : v;
            }
            int idx = url.indexOf("youtu.be/");
            if (idx >= 0) {
                String v = url.substring(idx + "youtu.be/".length());
                int q = v.indexOf('?');
                return q > 0 ? v.substring(0, q) : v;
            }
            idx = url.indexOf("/shorts/");
            if (idx >= 0) {
                String v = url.substring(idx + "/shorts/".length());
                int q = v.indexOf('?');
                return q > 0 ? v.substring(0, q) : v;
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ======== For passing on meta during replacement ========
    static final class TrackContext {
        final AudioTrackInfo originalInfo; // Original YouTube metadata
        final Object userData;             // RequestMetadata and other existing user data
        TrackContext(AudioTrackInfo info, Object userData) {
            this.originalInfo = info;
            this.userData = userData;
        }
    }

    // Attach the original track's information to the new local track
    void applyReplacementContext(AudioTrack newTrack, AudioTrack oldTrack) {
        Object ud = oldTrack.getUserData(); // RequestMetadata, etc.
        newTrack.setUserData(new TrackContext(oldTrack.getInfo(), ud));
    }

    // ==========================================================
    // Playback exception → yt-dlp fallback
    // ==========================================================
    private static class YtDlpExceptionListener extends AudioEventAdapter {
        private final PlayerManager pm;
        private final AudioPlayer player;
        private final AudioHandler handler;
        private final AtomicBoolean fallingBack = new AtomicBoolean(false);
        private final Set<String> attempted = Collections.synchronizedSet(new HashSet<>());

        YtDlpExceptionListener(PlayerManager pm, AudioPlayer player, AudioHandler handler) {
            this.pm = pm;
            this.player = player;
            this.handler = handler;
        }

        @Override
        public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
            String id = track != null ? track.getIdentifier() : null;
            pm.logger.warn("Exception occurred during playback. id={} msg={}", id, exception.getMessage());

            if (track == null || !pm.shouldFallbackToYtDlp(id)) return;

            // Suppress the next onTrackEnd call once only (prevent exit)
            handler.suppressAutoLeaveOnce();

            if (!attempted.add(id)) {
                pm.logger.debug("This track has already attempted a fallback: {}", id);
                return;
            }
            if (!fallingBack.compareAndSet(false, true)) return;

            CompletableFuture.runAsync(() -> {
                try {
                    Path out = pm.downloadViaYtDlp(id);
                    if (out == null || !Files.isRegularFile(out))
                        throw new IllegalStateException("yt-dlp output not found: " + out);

                    pm.logger.info("yt-dlp fallback successful. Replaced with local file and playing: {}", out);

                    pm.loadItemOrdered(handler, out.toAbsolutePath().toString(), new AudioLoadResultHandler() {
                        @Override public void trackLoaded(AudioTrack newTrack) {
                            // Meta Transfer
                            pm.applyReplacementContext(newTrack, track);
                            // Do not call stopTrack(). Use replaced playback (REPLACED).
                            player.startTrack(newTrack, false);
                        }
                        @Override public void playlistLoaded(AudioPlaylist playlist) {
                            AudioTrack t = playlist.getTracks().isEmpty() ? null : playlist.getTracks().get(0);
                            if (t != null) {
                                pm.applyReplacementContext(t, track);
                                player.startTrack(t, false);
                            } else noMatches();
                        }
                        @Override public void noMatches() {
                            pm.logger.error("Failed to load local replacement (no matches): {}", out);
                        }
                        @Override public void loadFailed(FriendlyException e) {
                            pm.logger.error("Failed to load local replacement: {}", e.getMessage());
                        }
                    });
                } catch (Exception ex) {
                    pm.logger.error("yt-dlp fallback (during playback) failed: {}", ex.toString());
                } finally {
                    fallingBack.set(false);
                }
            });
        }

        @Override
        public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
            if (track == null) return;
            String id = track.getIdentifier();
            if (pm.shouldFallbackToYtDlp(id)) {
                pm.logger.warn("Track is stuck. Attempting fallback to yt-dlp: id={}, stuck={}ms", id, thresholdMs);
                onTrackException(player, track, new FriendlyException("stuck " + thresholdMs + "ms",
                        FriendlyException.Severity.SUSPICIOUS, null));
            }
        }
    }
}

/*
 *  Copyright 2023 Cosgy Dev (info@cosgy.dev).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.cosgy.jmusicbot.slashcommands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.cosgy.jmusicbot.slashcommands.MusicCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyCmd extends MusicCommand {

    Logger log = LoggerFactory.getLogger(this.name);
    private static final HttpClient httpClient = HttpClient.newBuilder().build();
    private static final String SPOTIFY_TRACK_URL_PREFIX = "https://open.spotify.com/track/";
    private static final String SPOTIFY_AUTH_URL = "https://accounts.spotify.com/api/token";

    private final static String LOAD = "\uD83D\uDCE5"; // 📥
    private final static String CANCEL = "\uD83D\uDEAB"; // 🚫

    private String accessToken = null;
    private long accessTokenExpirationTime;

    private final OrderedMenu.Builder builder;

    public SpotifyCmd(Bot bot) {
        super(bot);
        this.name = "spotify";
        this.arguments = "<title|URL|subcommand>";
        this.help = "Plays the specified track";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = false;

        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "tracklink", "URL of the Spotify track", true));
        this.options = options;

        builder = new OrderedMenu.Builder()
                .allowTextInput(true)
                .useNumbers()
                .useCancelButton(true)
                .setEventWaiter(bot.getWaiter())
                .setTimeout(1, TimeUnit.MINUTES);

        // Retrieve Spotify username and password
        String clientId = bot.getConfig().getSpotifyClientId();
        String clientSecret = bot.getConfig().getSpotifyClientSecret();

        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            return;
        }
        // Issue ACCESS_TOKEN
        accessToken = getAccessToken(clientId, clientSecret);
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        String trackUrl = event.getOption("tracklink").getAsString();

        if (accessToken == null) {
            event.reply("This command is unavailable. Configuration by the bot owner is required to enable this command.").queue();
            return;
        }

        // Renew the access token if it has expired
        if (System.currentTimeMillis() >= accessTokenExpirationTime) {
            String clientId = bot.getConfig().getSpotifyClientId();
            String clientSecret = bot.getConfig().getSpotifyClientSecret();
            accessToken = getAccessToken(clientId, clientSecret);
        }

        if (!isSpotifyTrackUrl(trackUrl)) {
            event.reply("Error: The specified URL is not a Spotify track URL").queue();
            return;
        }

        String trackId = extractTrackIdFromUrl(trackUrl);
        String endpoint = "https://api.spotify.com/v1/tracks/" + trackId;

        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept-Language", "en")
                .GET()
                .uri(URI.create(endpoint))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());
            String trackName = json.getString("name");
            String albumName = json.getJSONObject("album").getString("name");
            String artistName = json.getJSONArray("artists").getJSONObject(0).getString("name");
            String albumImageUrl = json.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url");

            // Use the Audio Features endpoint to retrieve track information
            endpoint = "https://api.spotify.com/v1/audio-features/" + trackId;
            request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .uri(URI.create(endpoint))
                    .build();

            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            json = new JSONObject(response.body());
            // Use a default value when valence key does not exist
            double trackColor = json.has("valence") ? json.getDouble("valence") : 0.5;

            int hue = (int) (trackColor * 360);
            Color color = Color.getHSBColor((float) hue / 360, 1.0f, 1.0f);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Track Information");
            embed.addField("Track Name", trackName, true);
            embed.addField("Album Name", albumName, true);
            embed.addField("Artist Name", artistName, true);
            embed.setImage(albumImageUrl);
            embed.setColor(color);

            event.getTextChannel().sendMessageEmbeds(embed.build()).queue();

            event.reply("Loading `[" + trackName + "]`...").queue(m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), "ytmsearch:" + trackName + " " + artistName, new SlashResultHandler(m, event)));
        } catch (IOException | InterruptedException e) {
            event.reply("Error: " + e.getMessage()).queue();
        }
    }

    @Override
    public void doCommand(CommandEvent event) {
        if (event.getArgs().isEmpty()) {
            event.reply(event.getClient().getError() + " Please include the playlist name.");
            return;
        }
        String trackUrl = event.getArgs();

        if (accessToken == null) {
            event.reply("This command is unavailable. Configuration by the bot owner is required to enable this command.");
            return;
        }

        // Renew the access token if it has expired
        if (System.currentTimeMillis() >= accessTokenExpirationTime) {
            String clientId = bot.getConfig().getSpotifyClientId();
            String clientSecret = bot.getConfig().getSpotifyClientSecret();
            accessToken = getAccessToken(clientId, clientSecret);
        }

        if (!isSpotifyTrackUrl(trackUrl)) {
            event.reply("Error: The specified URL is not a Spotify track URL");
            return;
        }

        String trackId = extractTrackIdFromUrl(trackUrl);
        String endpoint = "https://api.spotify.com/v1/tracks/" + trackId;

        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept-Language", "en")
                .GET()
                .uri(URI.create(endpoint))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());
            String trackName = json.getString("name");
            String albumName = json.getJSONObject("album").getString("name");
            String artistName = json.getJSONArray("artists").getJSONObject(0).getString("name");
            String albumImageUrl = json.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url");

            // Use the Audio Features endpoint to retrieve track information
            endpoint = "https://api.spotify.com/v1/audio-features/" + trackId;
            request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .uri(URI.create(endpoint))
                    .build();

            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            json = new JSONObject(response.body());
            // Use a default value when valence key does not exist
            double trackColor = json.has("valence") ? json.getDouble("valence") : 0.5;

            int hue = (int) (trackColor * 360);
            Color color = Color.getHSBColor((float) hue / 360, 1.0f, 1.0f);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Track Information");
            embed.addField("Track Name", trackName, true);
            embed.addField("Album Name", albumName, true);
            embed.addField("Artist Name", artistName, true);
            embed.setImage(albumImageUrl);
            embed.setColor(color);

            event.getTextChannel().sendMessageEmbeds(embed.build()).queue();

            event.reply("Loading `[" + trackName + "]`...", m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), "ytmsearch:" + trackName + " " + artistName, new ResultHandler(m, event)));
        } catch (IOException | InterruptedException e) {
            event.reply("Error: " + e.getMessage());
        }
    }

    public static String extractTrackIdFromUrl(String url) {
        String trackId = null;

        Pattern pattern = Pattern.compile("track/(\\w+)");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            trackId = matcher.group(1);
        }

        return trackId;
    }

    public boolean isSpotifyTrackUrl(String url) {
        Pattern pattern = Pattern.compile("https://open\\.spotify\\.com/(intl-ja/)?track/\\w+");
        Matcher matcher = pattern.matcher(url.split("\\?")[0]);

        return matcher.matches();
    }

    private String getAccessToken(String clientId, String clientSecret) {
        String encodedCredentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Basic " + encodedCredentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .uri(URI.create(SPOTIFY_AUTH_URL))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());
            accessTokenExpirationTime = System.currentTimeMillis() + json.getInt("expires_in") * 1000L;
            return json.getString("access_token");
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }


    private class SlashResultHandler implements AudioLoadResultHandler {
        private final InteractionHook m;
        private final SlashCommandEvent event;

        private SlashResultHandler(InteractionHook m, SlashCommandEvent event) {
            this.m = m;
            this.event = event;
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            if (bot.getConfig().isTooLong(track)) {
                event.getHook().sendMessage(FormatUtil.filter(event.getClient().getWarning() + "**" + track.getInfo().title + "**` exceeds the allowed maximum length. "
                        + FormatUtil.formatTime(track.getDuration()) + "` > `" + bot.getConfig().getMaxTime() + "`")).queue();
                return;
            }
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrack(new QueuedTrack(track, event.getUser())) + 1;
            event.getHook().sendMessage(FormatUtil.filter(event.getClient().getSuccess() + "**" + track.getInfo().title
                    + "**(`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? "has been added."
                    : " has been added to the queue at position " + pos + "."))).queue();
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            builder.setColor(event.getGuild().getSelfMember().getColor())
                    .setText(FormatUtil.filter(event.getClient().getSuccess() + "Search results:"))
                    .setChoices()
                    .setSelection((msg, i) ->
                    {
                        AudioTrack track = playlist.getTracks().get(i - 1);
                        if (bot.getConfig().isTooLong(track)) {
                            event.getHook().sendMessage(event.getClient().getWarning() + "**" + track.getInfo().title + "**` exceeds the allowed maximum length. "
                                    + FormatUtil.formatTime(track.getDuration()) + "` > `" + bot.getConfig().getMaxTime() + "`").queue();
                            return;
                        }
                        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                        int pos = handler.addTrack(new QueuedTrack(track, event.getUser())) + 1;
                        event.getHook().sendMessage(event.getClient().getSuccess() + "**" + track.getInfo().title
                                + "**(`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? "has been added."
                                : " has been added to the queue at position " + pos + ".")).queue();
                    })
                    .setCancel((msg) -> {
                    })
                    .setUsers(event.getUser())
            ;
            for (int i = 0; i < 4 && i < playlist.getTracks().size(); i++) {
                AudioTrack track = playlist.getTracks().get(i);
                builder.addChoices("`[" + FormatUtil.formatTime(track.getDuration()) + "]` [**" + track.getInfo().title + "**](" + track.getInfo().uri + ")");
            }
            builder.build().display(event.getChannel());
        }

        @Override
        public void noMatches() {
            event.getHook().sendMessage(FormatUtil.filter(event.getClient().getWarning() + " 曲を検索しましたが見つかりませんでした。 `")).queue();
        }

        @Override
        public void loadFailed(FriendlyException throwable) {

            if (throwable.severity == FriendlyException.Severity.COMMON)
                event.getHook().sendMessage(event.getClient().getError() + " An error occurred while loading: " + throwable.getMessage()).queue();
            else
                event.getHook().sendMessage(event.getClient().getError() + " An error occurred while loading").queue();
        }
    }

    private class ResultHandler implements AudioLoadResultHandler {
        private final Message m;
        private final CommandEvent event;

        private ResultHandler(Message m, CommandEvent event) {
            this.m = m;
            this.event = event;
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            if (bot.getConfig().isTooLong(track)) {
                m.editMessage(FormatUtil.filter(event.getClient().getWarning() + "**" + track.getInfo().title + "**` exceeds the allowed maximum length. "
                        + FormatUtil.formatTime(track.getDuration()) + "` > `" + bot.getConfig().getMaxTime() + "`")).queue();
                return;
            }
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrack(new QueuedTrack(track, event.getAuthor())) + 1;
            m.editMessage(FormatUtil.filter(event.getClient().getSuccess() + "**" + track.getInfo().title
                    + "**(`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? "has been added."
                    : " has been added to the queue at position " + pos + "."))).queue();
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            builder.setColor(event.getSelfMember().getColor())
                    .setText(FormatUtil.filter(event.getClient().getSuccess() + "Search results:"))
                    .setChoices()
                    .setSelection((msg, i) -> {
                        AudioTrack track = playlist.getTracks().get(i - 1);
                        if (bot.getConfig().isTooLong(track)) {
                            event.replyWarning("This track (**" + track.getInfo().title + "**) exceeds the allowed maximum length: `"
                                    + FormatUtil.formatTime(track.getDuration()) + "` > `" + bot.getConfig().getMaxTime() + "`");
                            return;
                        }
                        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                        int pos = handler.addTrack(new QueuedTrack(track, event.getAuthor())) + 1;
                        event.replySuccess("**" + FormatUtil.filter(track.getInfo().title)
                                + "** (`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? "playback will start."
                                : " has been added to the queue at position " + pos + "."));
                    })
                    .setCancel((msg) -> {
                    })
                    .setUsers(event.getAuthor());
            for (int i = 0; i < 4 && i < playlist.getTracks().size(); i++) {
                AudioTrack track = playlist.getTracks().get(i);
                builder.addChoices("`[" + FormatUtil.formatTime(track.getDuration()) + "]` [**" + track.getInfo().title + "**](" + track.getInfo().uri + ")");
            }
            builder.build().display(m);
        }

        @Override
        public void noMatches() {
            m.editMessage(FormatUtil.filter(event.getClient().getWarning() + " No matches found for the searched track.")).queue();
        }

        @Override
        public void loadFailed(FriendlyException throwable) {

            if (throwable.severity == FriendlyException.Severity.COMMON)
                m.editMessage(event.getClient().getError() + " An error occurred while loading: " + throwable.getMessage()).queue();
            else
                m.editMessage(event.getClient().getError() + " An error occurred while loading").queue();
        }
    }
}


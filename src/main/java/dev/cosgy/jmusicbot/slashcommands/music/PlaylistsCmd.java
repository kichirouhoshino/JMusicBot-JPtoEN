/*
 * Copyright 2018 John Grosh (jagrosh).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.cosgy.jmusicbot.slashcommands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.playlist.PlaylistLoader;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import dev.cosgy.jmusicbot.slashcommands.MusicCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class PlaylistsCmd extends MusicCommand {
    public PlaylistsCmd(Bot bot) {
        super(bot);
        this.name = "playlists";
        this.help = "Displays available playlists";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = true;
        this.beListening = false;
        this.children = new MusicCommand[] { new PlayCmd(bot) };
    }

    @Override
    public void doCommand(CommandEvent event) {
        String guildID = event.getGuild().getId();
        if (!bot.getPlaylistLoader().folderExists())
            bot.getPlaylistLoader().createFolder();
        if (!bot.getPlaylistLoader().folderGuildExists(guildID))
            bot.getPlaylistLoader().createGuildFolder(guildID);
        if (!bot.getPlaylistLoader().folderExists()) {
            event.reply(event.getClient().getWarning() + " Could not create playlist folder because it doesn't exist.");
            return;
        }
        if (!bot.getPlaylistLoader().folderGuildExists(guildID)) {
            event.reply(event.getClient().getWarning() + " Could not create folder for this server's playlists because it doesn't exist.");
            return;
        }
        List<String> list = bot.getPlaylistLoader().getPlaylistNames(guildID);
        if (list == null)
            event.reply(event.getClient().getError() + " Could not load available playlists.");
        else if (list.isEmpty())
            event.reply(event.getClient().getWarning() + " There are no playlists in the playlist folder.");
        else {
            StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + " Available playlists:\n");
            list.forEach(str -> builder.append("`").append(str).append("` "));
            builder.append("\nYou can play a playlist by typing `")
                    .append(event.getClient().getTextualPrefix())
                    .append("play playlist <name>`.");
            event.reply(builder.toString());
        }
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        String guildID = event.getGuild().getId();
        if (!bot.getPlaylistLoader().folderExists())
            bot.getPlaylistLoader().createFolder();
        if (!bot.getPlaylistLoader().folderGuildExists(guildID))
            bot.getPlaylistLoader().createGuildFolder(guildID);
        if (!bot.getPlaylistLoader().folderExists()) {
            event.reply(event.getClient().getWarning() + " Could not create playlist folder because it doesn't exist.").queue();
            return;
        }
        if (!bot.getPlaylistLoader().folderGuildExists(guildID)) {
            event.reply(event.getClient().getWarning() + " Could not create folder for this server's playlists because it doesn't exist.").queue();
            return;
        }
        List<String> list = bot.getPlaylistLoader().getPlaylistNames(guildID);
        if (list == null)
            event.reply(event.getClient().getError() + " Could not load available playlists.").queue();
        else if (list.isEmpty())
            event.reply(event.getClient().getWarning() + " There are no playlists in the playlist folder.").queue();
        else {
            StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + " Available playlists:\n");
            list.forEach(str -> builder.append("`").append(str).append("` "));
            builder.append("\nYou can play a playlist by typing `")
                    .append(event.getClient().getTextualPrefix())
                    .append("play playlist <name>`.");
            event.reply(builder.toString()).queue();
        }
    }

    public class PlayCmd extends MusicCommand {
        public PlayCmd(Bot bot) {
            super(bot);
            this.name = "playlist";
            this.aliases = new String[]{"pl"};
            this.arguments = "<name>";
            this.help = "Plays the provided playlist";
            this.beListening = true;
            this.bePlaying = false;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", "Playlist name", true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {
            String guildId = event.getGuild().getId();
            if (event.getArgs().isEmpty()) {
                event.reply(event.getClient().getError() + "Please include the playlist name.");
                return;
            }
            PlaylistLoader.Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildId, event.getArgs());
            if (playlist == null) {
                event.replyError("Could not find `" + event.getArgs() + ".txt`");
                return;
            }
            event.getChannel().sendMessage(":calling: Loading playlist **" + event.getArgs() + "**... (" + playlist.getItems().size() + " songs)").queue(m -> {
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                playlist.loadTracks(bot.getPlayerManager(), (at) -> handler.addTrack(new QueuedTrack(at, event.getAuthor())), () -> {
                    StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                            ? event.getClient().getWarning() + " No songs were loaded."
                            : event.getClient().getSuccess() + "Loaded **" + playlist.getTracks().size() + "** songs.");
                    if (!playlist.getErrors().isEmpty())
                        builder.append("\nThe following songs could not be loaded:");
                    playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1).append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                    String str = builder.toString();
                    if (str.length() > 2000)
                        str = str.substring(0, 1994) + " (truncated)";
                    m.editMessage(FormatUtil.filter(str)).queue();
                });
            });
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            String guildId = event.getGuild().getId();
            if (event.getOption("name") == null) {
                event.reply(event.getClient().getError() + "Please include the playlist name.").queue();
                return;
            }
            PlaylistLoader.Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildId, event.getOption("name").getAsString());
            if (playlist == null) {
                event.reply("Could not find `" + event.getOption("name").getAsString() + ".txt`");
                return;
            }
            event.getChannel().sendMessage(":calling: Loading playlist **" + event.getOption("name").getAsString() + "**... (" + playlist.getItems().size() + " songs)").queue(m -> {
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                playlist.loadTracks(bot.getPlayerManager(), (at) -> handler.addTrack(new QueuedTrack(at, event.getUser())), () -> {
                    StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                            ? event.getClient().getWarning() + " No songs were loaded."
                            : event.getClient().getSuccess() + "Loaded **" + playlist.getTracks().size() + "** songs.");
                    if (!playlist.getErrors().isEmpty())
                        builder.append("\nThe following songs could not be loaded:");
                    playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1).append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                    String str = builder.toString();
                    if (str.length() > 2000)
                        str = str.substring(0, 1994) + " (truncated)";
                    m.editMessage(FormatUtil.filter(str)).queue();
                });
            });
        }
    }
}

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
        this.help = "利用可能な再生リストを表示します";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = true;
        this.beListening = false;
        this.children = new MusicCommand[]{new PlayCmd(bot)};
    }

    @Override
    public void doCommand(CommandEvent event) {
        handlePlaylistsCommand(event.getGuild().getId(), event, null);
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        handlePlaylistsCommand(event.getGuild().getId(), null, event);
    }

    private void handlePlaylistsCommand(String guildID, CommandEvent cmdEvent, SlashCommandEvent slashEvent) {
        ensureFoldersExist(guildID);

        List<String> playlists = bot.getPlaylistLoader().getPlaylistNames(guildID);

        String prefix = (cmdEvent != null ? cmdEvent.getClient().getTextualPrefix() : slashEvent.getClient().getTextualPrefix());
        String message;
        if (playlists == null) {
            message = (cmdEvent != null ? cmdEvent.getClient().getError() : slashEvent.getClient().getError()) + " 利用可能な再生リストを読み込めませんでした。";
        } else if (playlists.isEmpty()) {
            message = (cmdEvent != null ? cmdEvent.getClient().getWarning() : slashEvent.getClient().getWarning()) + " 再生リストフォルダにプレイリストがありません。";
        } else {
            StringBuilder builder = new StringBuilder((cmdEvent != null ? cmdEvent.getClient().getSuccess() : slashEvent.getClient().getSuccess()) + " 利用可能な再生リスト:\n");
            playlists.forEach(name -> builder.append("`").append(name).append("` "));
            builder.append("\n`").append(prefix).append("play playlist <name>` と入力することで再生リストを再生できます。");
            message = builder.toString();
        }

        reply(cmdEvent, slashEvent, message);
    }

    private void ensureFoldersExist(String guildID) {
        if (!bot.getPlaylistLoader().folderExists()) {
            bot.getPlaylistLoader().createFolder();
        }
        if (!bot.getPlaylistLoader().folderGuildExists(guildID)) {
            bot.getPlaylistLoader().createGuildFolder(guildID);
        }
    }

    private void reply(CommandEvent cmdEvent, SlashCommandEvent slashEvent, String message) {
        if (cmdEvent != null) {
            cmdEvent.reply(message); // CommandEventにはqueue()を付けない
        } else if (slashEvent != null) {
            slashEvent.reply(message).queue(); // SlashCommandEventにはqueue()を付ける
        }
    }

    public class PlayCmd extends MusicCommand {
        public PlayCmd(Bot bot) {
            super(bot);
            this.name = "playlist";
            this.aliases = new String[]{"pl"};
            this.arguments = "<name>";
            this.help = "指定された再生リストを再生します";
            this.beListening = true;
            this.bePlaying = false;
        }

        @Override
        public void doCommand(CommandEvent event) {
            playPlaylist(event.getGuild().getId(), event.getArgs(), event, null);
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            String playlistName = event.getOption("name").getAsString();
            playPlaylist(event.getGuild().getId(), playlistName, null, event);
        }

        private void playPlaylist(String guildID, String playlistName, CommandEvent cmdEvent, SlashCommandEvent slashEvent) {
            if (playlistName == null || playlistName.isEmpty()) {
                reply(cmdEvent, slashEvent, (cmdEvent != null ? cmdEvent.getClient().getError() : slashEvent.getClient().getError()) + " 再生リスト名を指定してください。");
                return;
            }

            PlaylistLoader.Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildID, playlistName);
            if (playlist == null) {
                reply(cmdEvent, slashEvent, (cmdEvent != null ? cmdEvent.getClient().getError() : slashEvent.getClient().getError()) + " 再生リスト `" + playlistName + "` が見つかりませんでした。");
                return;
            }

            reply(cmdEvent, slashEvent, (cmdEvent != null ? cmdEvent.getClient().getSuccess() : slashEvent.getClient().getSuccess()) + ":calling: 再生リスト **" + playlistName + "** を読み込んでいます... (" + playlist.getItems().size() + " 曲)");

            AudioHandler handler = (AudioHandler) (cmdEvent != null
                    ? cmdEvent.getGuild().getAudioManager().getSendingHandler()
                    : slashEvent.getGuild().getAudioManager().getSendingHandler());

            playlist.loadTracks(bot.getPlayerManager(), track -> handler.addTrack(new QueuedTrack(track, cmdEvent != null ? cmdEvent.getAuthor() : slashEvent.getUser())), () -> {
                StringBuilder builder = new StringBuilder();
                if (playlist.getTracks().isEmpty()) {
                    builder.append((cmdEvent != null ? cmdEvent.getClient().getWarning() : slashEvent.getClient().getWarning())).append(" 楽曲がロードされていません。");
                } else {
                    builder.append((cmdEvent != null ? cmdEvent.getClient().getSuccess() : slashEvent.getClient().getSuccess())).append(" **").append(playlist.getTracks().size()).append("** 曲をロードしました。");
                }
                if (!playlist.getErrors().isEmpty()) {
                    builder.append("\n以下の楽曲をロードできませんでした:");
                    playlist.getErrors().forEach(error -> builder.append("\n`[").append(error.getIndex() + 1).append("]` **").append(error.getItem()).append("**: ").append(error.getReason()));
                }

                String result = FormatUtil.filter(builder.toString());
                if (result.length() > 2000) {
                    result = result.substring(0, 1994) + " (以下略)";
                }

                reply(cmdEvent, slashEvent, result);
            });
        }
    }
}
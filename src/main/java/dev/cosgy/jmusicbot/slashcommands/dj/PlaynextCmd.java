/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
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
package dev.cosgy.jmusicbot.slashcommands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.cosgy.jmusicbot.slashcommands.DJCommand;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PlaynextCmd extends DJCommand {
    private final String loadingEmoji;
    Logger log = LoggerFactory.getLogger("Playnext");

    public PlaynextCmd(Bot bot) {
        super(bot);
        this.loadingEmoji = bot.getConfig().getLoading();
        this.name = "playnext";
        this.arguments = "<title|URL>";
        this.help = "Specify the next song to play";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = false;
        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "title", "Song title or URL", true));
        this.options = options;
    }

    @Override
    public void doCommand(CommandEvent event) {
        if (event.getArgs().isEmpty() && event.getMessage().getAttachments().isEmpty()) {
            event.replyWarning("Please provide the song title or URL.");
            return;
        }
        String args = event.getArgs().startsWith("<") && event.getArgs().endsWith(">")
                ? event.getArgs().substring(1, event.getArgs().length() - 1)
                : event.getArgs().isEmpty() ? event.getMessage().getAttachments().get(0).getUrl() : event.getArgs();
        log.info("Loading [" + args + "] on " + event.getGuild().getName());
        event.reply(loadingEmoji + "`[" + args + "]` is being loaded...", m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), args, new ResultHandler(m, event, false)));
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        if (!checkDJPermission(event.getClient(), event)) {
            event.reply(event.getClient().getWarning() + "You don't have permission to execute this command.").queue();
            return;
        }
        String args = event.getOption("title").getAsString();
        log.info("Loading [" + args + "] on " + event.getGuild().getName());
        event.reply(loadingEmoji + "`[" + args + "]` is being loaded...").queue(m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), args, new SlashResultHandler(m, event, false)));
    }

    private class SlashResultHandler implements AudioLoadResultHandler {
        private final InteractionHook m;
        private final SlashCommandEvent event;
        private final boolean ytsearch;

        private SlashResultHandler(InteractionHook m, SlashCommandEvent event, boolean ytsearch) {
            this.m = m;
            this.event = event;
            this.ytsearch = ytsearch;
        }

        private void loadSingle(AudioTrack track) {
            if (bot.getConfig().isTooLong(track)) {
                m.editOriginal(FormatUtil.filter(event.getClient().getWarning() + "(**" + (track.getInfo().uri.contains("https://stream.gensokyoradio.net/") ? "Gensokyo Radio" : track.getInfo().title) + "**) This track is too long: `"
                        + FormatUtil.formatTime(track.getDuration()) + "` > `" + FormatUtil.formatTime(bot.getConfig().getMaxSeconds() * 1000) + "`")).queue();
                return;
            }
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrackToFront(new QueuedTrack(track, event.getUser())) + 1;
            String addMsg = FormatUtil.filter(event.getClient().getSuccess() + "**" + (track.getInfo().uri.contains("https://stream.gensokyoradio.net/") ? "Gensokyo Radio" : track.getInfo().title)
                    + "** (`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? "was added to the queue." : "was added to position " + pos + " in the queue."));
            m.editOriginal(addMsg).queue();

            // log.info(event.getGuild().getName() + track.getInfo().title
            // + "(" + FormatUtil.formatTime(track.getDuration()) + ") " + (pos == 0 ? "added to the playback queue." : "added to the playback queue at position " + pos + "."));

        }

        @Override
        public void trackLoaded(AudioTrack track) {
            loadSingle(track);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            AudioTrack single;
            if (playlist.getTracks().size() == 1 || playlist.isSearchResult())
                single = playlist.getSelectedTrack() == null ? playlist.getTracks().get(0) : playlist.getSelectedTrack();
            else if (playlist.getSelectedTrack() != null)
                single = playlist.getSelectedTrack();
            else
                single = playlist.getTracks().get(0);
            loadSingle(single);
        }

        @Override
        public void noMatches() {
            if (ytsearch)
                m.editOriginal(FormatUtil.filter(event.getClient().getWarning() + " No search results found for `" + event.getUser() + "`.")).queue();
            else
                bot.getPlayerManager().loadItemOrdered(event.getGuild(), "ytsearch:" + event.getUser(), new SlashResultHandler(m, event, true));
        }

        @Override
        public void loadFailed(FriendlyException throwable) {
            if (throwable.severity == FriendlyException.Severity.COMMON)
                m.editOriginal(event.getClient().getError() + " Load error: " + throwable.getMessage()).queue();
            else
                m.editOriginal(event.getClient().getError() + " An error occurred while loading the song.").queue();
            log.info("Load error occurred on " + event.getGuild().getName());
        }
    }


    private class ResultHandler implements AudioLoadResultHandler {
        private final Message m;
        private final CommandEvent event;
        private final boolean ytsearch;

        private ResultHandler(Message m, CommandEvent event, boolean ytsearch) {
            this.m = m;
            this.event = event;
            this.ytsearch = ytsearch;
        }

        private void loadSingle(AudioTrack track) {
            if (bot.getConfig().isTooLong(track)) {
                m.editMessage(FormatUtil.filter(event.getClient().getWarning() + "(**" + (track.getInfo().uri.contains("https://stream.gensokyoradio.net/") ? "Gensokyo Radio" : track.getInfo().title) + "**) This track is too long: `"
                        + FormatUtil.formatTime(track.getDuration()) + "` > `" + FormatUtil.formatTime(bot.getConfig().getMaxSeconds() * 1000) + "`")).queue();
                return;
            }
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrackToFront(new QueuedTrack(track, event.getAuthor())) + 1;
            String addMsg = FormatUtil.filter(event.getClient().getSuccess() + "**" + (track.getInfo().uri.contains("https://stream.gensokyoradio.net/") ? "Gensokyo Radio" : track.getInfo().title)
                    + "** (`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? "was added to the queue." : "was added to position " + pos + " in the queue."));
            m.editMessage(addMsg).queue();

            // log.info(event.getGuild().getName() + track.getInfo().title
            // + "(" + FormatUtil.formatTime(track.getDuration()) + ") " + (pos == 0 ? "Added to the playback queue." : "Added to the playback queue at position " + pos + "."));

        }

        @Override
        public void trackLoaded(AudioTrack track) {
            loadSingle(track);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            AudioTrack single;
            if (playlist.getTracks().size() == 1 || playlist.isSearchResult())
                single = playlist.getSelectedTrack() == null ? playlist.getTracks().get(0) : playlist.getSelectedTrack();
            else if (playlist.getSelectedTrack() != null)
                single = playlist.getSelectedTrack();
            else
                single = playlist.getTracks().get(0);
            loadSingle(single);
        }

        @Override
        public void noMatches() {
            if (ytsearch)
                m.editMessage(FormatUtil.filter(event.getClient().getWarning() + " No search results found for `" + event.getArgs() + "`.")).queue();
            else
                bot.getPlayerManager().loadItemOrdered(event.getGuild(), "ytsearch:" + event.getArgs(), new ResultHandler(m, event, true));
        }

        @Override
        public void loadFailed(FriendlyException throwable) {
            if (throwable.severity == FriendlyException.Severity.COMMON)
                m.editMessage(event.getClient().getError() + " Load error: " + throwable.getMessage()).queue();
            else
                m.editMessage(event.getClient().getError() + " An error occurred while loading the song.").queue();
            log.info("Load error occurred on " + event.getGuild().getName());
        }
    }
}

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
import com.jagrosh.jlyrics.LyricsClient;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import dev.cosgy.jmusicbot.slashcommands.MusicCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class LyricsCmd extends MusicCommand {
    private final LyricsClient lClient = new LyricsClient();

    public LyricsCmd(Bot bot) {
        super(bot);
        this.name = "lyrics";
        this.arguments = "[song name]";
        this.help = "Displays the lyrics of a song";
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.aliases = bot.getConfig().getAliases(this.name);
        this.bePlaying = true;

        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "name", "Song name", false));
        this.options = options;
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        event.getChannel().sendTyping().queue();
        String title;
        if (event.getOption("name").getAsString().isEmpty()) {
            AudioHandler sendingHandler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            if (sendingHandler.isMusicPlaying(event.getJDA()))
                title = sendingHandler.getPlayer().getPlayingTrack().getInfo().title;
            else {
                event.reply(event.getClient().getError() + "The command can't be used as no song is currently playing.").queue();
                return;
            }
        } else
            title = event.getOption("name").getAsString();
        lClient.getLyrics(title).thenAccept(lyrics ->
        {
            if (lyrics == null) {
                event.reply(event.getClient().getError() + "No lyrics found for `" + title + "`." + (event.getOption("name").getAsString().isEmpty() ? " Try manually entering the song name (`lyrics [song name]`)." : "")).queue();
                return;
            }

            EmbedBuilder eb = new EmbedBuilder()
                    .setAuthor(lyrics.getAuthor())
                    .setColor(event.getMember().getColor())
                    .setTitle(lyrics.getTitle(), lyrics.getURL());
            if (lyrics.getContent().length() > 15000) {
                event.reply(event.getClient().getWarning() + " Lyrics found for `" + title + "` but they might be incorrect: " + lyrics.getURL()).queue();
            } else if (lyrics.getContent().length() > 2000) {
                String content = lyrics.getContent().trim();
                while (content.length() > 2000) {
                    int index = content.lastIndexOf("\n\n", 2000);
                    if (index == -1)
                        index = content.lastIndexOf("\n", 2000);
                    if (index == -1)
                        index = content.lastIndexOf(" ", 2000);
                    if (index == -1)
                        index = 2000;
                    event.replyEmbeds(eb.setDescription(content.substring(0, index).trim()).build()).queue();
                    content = content.substring(index).trim();
                    eb.setAuthor(null).setTitle(null, null);
                }
                event.replyEmbeds(eb.setDescription(content).build()).queue();
            } else
                event.replyEmbeds(eb.setDescription(lyrics.getContent()).build()).queue();
        });
    }

    @Override
    public void doCommand(CommandEvent event) {
        event.getChannel().sendTyping().queue();
        String title;
        if (event.getArgs().isEmpty()) {
            AudioHandler sendingHandler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            if (sendingHandler.isMusicPlaying(event.getJDA()))
                title = sendingHandler.getPlayer().getPlayingTrack().getInfo().title;
            else {
                event.replyError("The command can't be used as no song is currently playing.");
                return;
            }
        } else
            title = event.getArgs();
        lClient.getLyrics(title).thenAccept(lyrics ->
        {
            if (lyrics == null) {
                event.replyError("No lyrics found for `" + title + "`." + (event.getArgs().isEmpty() ? " Try manually entering the song name (`lyrics [song name]`)." : ""));
                return;
            }

            EmbedBuilder eb = new EmbedBuilder()
                    .setAuthor(lyrics.getAuthor())
                    .setColor(event.getSelfMember().getColor())
                    .setTitle(lyrics.getTitle(), lyrics.getURL());
            if (lyrics.getContent().length() > 15000) {
                event.replyWarning(" Lyrics found for `" + title + "` but they might be incorrect: " + lyrics.getURL());
            } else if (lyrics.getContent().length() > 2000) {
                String content = lyrics.getContent().trim();
                while (content.length() > 2000) {
                    int index = content.lastIndexOf("\n\n", 2000);
                    if (index == -1)
                        index = content.lastIndexOf("\n", 2000);
                    if (index == -1)
                        index = content.lastIndexOf(" ", 2000);
                    if (index == -1)
                        index = 2000;
                    event.reply(eb.setDescription(content.substring(0, index).trim()).build());
                    content = content.substring(index).trim();
                    eb.setAuthor(null).setTitle(null, null);
                }
                event.reply(eb.setDescription(content).build());
            } else
                event.reply(eb.setDescription(lyrics.getContent()).build());
        });
    }
}


package dev.cosgy.jmusicbot.slashcommands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.settings.Settings;
import dev.cosgy.jmusicbot.settings.RepeatMode;
import dev.cosgy.jmusicbot.slashcommands.MusicCommand;
import dev.cosgy.jmusicbot.util.QueuePaginatorManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.List;

/**
 * Command to display page information for currently playing or queued songs.
 */
public class QueueCmd extends MusicCommand {
    private static final String REPEAT_ALL = "\uD83D\uDD01";   // 🔁
    private static final String REPEAT_SINGLE = "\uD83D\uDD02"; // 🔂

    public QueueCmd(Bot bot) {
        super(bot);
        this.name = "queue";
        this.help = "Displays the list of songs in the queue";
        this.arguments = "[page]";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.bePlaying = true;
        // Required permissions for the bot (Message embedding permission is required. Reactions are not required)
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    public void doCommand(CommandEvent event) {
        // Retrieve the page number from the argument (default to page 1 if not specified)
        int page = 1;
        try {
            page = Integer.parseInt(event.getArgs().trim());
        } catch (NumberFormatException ignored) {
        }

        AudioHandler ah = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        List<QueuedTrack> queue = ah.getQueue().getList();
        if (queue.isEmpty()) {
            // If there are no songs in the queue, display the currently playing song information and exit.
            MessageCreateData nowPlayingData;
            try {
                nowPlayingData = ah.getNowPlaying(event.getJDA());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            MessageCreateData noMusicData = ah.getNoMusicPlaying(event.getJDA());
            MessageCreateData response = new MessageCreateBuilder()
                    .setContent(event.getClient().getWarning() + " There are no songs waiting to be played.")
                    .setEmbeds((nowPlayingData == null ? noMusicData : nowPlayingData).getEmbeds().get(0))
                    .build();
            event.reply(response, m -> {
                // Registered as the final playback message in NowPlayingHandler (for automatic updating of the current track)
                if (nowPlayingData != null) {
                    bot.getNowplayingHandler().setLastNPMessage(m);
                }
            });
            return;
        }

        // Calculate the total number of songs and total playback time
        long totalDuration = 0;
        for (QueuedTrack qt : queue) {
            totalDuration += qt.getTrack().getDuration();
        }
        int totalPages = (int) Math.ceil(queue.size() / 10.0);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        // Generate an Embed corresponding to the page number
        Settings settings = event.getClient().getSettingsFor(event.getGuild());
        RepeatMode repeatMode = settings.getRepeatMode();
        // Use QueuePaginatorManager to retrieve the Embed for the current page
        // (Displays the currently playing track information, number of entries, total duration, and repeat mode on the QueuePaginatorManager side)
        net.dv8tion.jda.api.entities.MessageEmbed queueEmbed = QueuePaginatorManager.createQueuePageEmbed(
                ah, queue, event.getClient().getSuccess(), repeatMode, page, totalPages);

        // Creating Page Navigation Buttons (Assigning User-Specific Custom IDs)
        String userId = event.getAuthor().getId();
        Button btnPrev = Button.secondary("queue:prev:" + userId, "⏮ Previous");
        Button btnNext = Button.secondary("queue:next:" + userId, "⏭ Next");
        Button btnClose = Button.danger("queue:close:" + userId, "❌ Close");

        // Embed and send button to channel
        event.getChannel().sendMessageEmbeds(queueEmbed)
                .setActionRow(btnPrev, btnNext, btnClose)
                .queue();
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        // For slash commands, first delay the response and display a "Thinking..." message.
        event.deferReply().queue();

        int page = 1;
        AudioHandler ah = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        List<QueuedTrack> queue = ah.getQueue().getList();
        if (queue.isEmpty()) {
            // If there are no songs in the queue, display the currently playing song information and exit.
            MessageCreateData nowPlayingData;
            try {
                nowPlayingData = ah.getNowPlaying(event.getJDA());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            MessageCreateData noMusicData = ah.getNoMusicPlaying(event.getJDA());
            MessageEditData response = new MessageEditBuilder()
                    .setContent(event.getClient().getWarning() + " There are no songs waiting to be played.")
                    .setEmbeds((nowPlayingData == null ? noMusicData : nowPlayingData).getEmbeds().get(0))
                    .build();
            event.getHook().editOriginal(response).queue();
            return;
        }

        // Calculate the total number of songs and total playback time
        long totalDuration = 0;
        for (QueuedTrack qt : queue) {
            totalDuration += qt.getTrack().getDuration();
        }
        int totalPages = (int) Math.ceil(queue.size() / 10.0);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        Settings settings = event.getClient().getSettingsFor(event.getGuild());
        RepeatMode repeatMode = settings.getRepeatMode();
        net.dv8tion.jda.api.entities.MessageEmbed queueEmbed = QueuePaginatorManager.createQueuePageEmbed(
                ah, queue, event.getClient().getSuccess(), repeatMode, page, totalPages);

        // Page navigation button (includes user ID in custom ID)
        String userId = event.getUser().getId();
        Button btnPrev = Button.secondary("queue:prev:" + userId, "⏮ Previous");
        Button btnNext = Button.secondary("queue:next:" + userId, "⏭ Next");
        Button btnClose = Button.danger("queue:close:" + userId, "❌ Close");

        // Set Embed and Button as initial response
        event.getHook().editOriginalEmbeds(queueEmbed)
                .setActionRow(btnPrev, btnNext, btnClose)
                .queue();
    }
}

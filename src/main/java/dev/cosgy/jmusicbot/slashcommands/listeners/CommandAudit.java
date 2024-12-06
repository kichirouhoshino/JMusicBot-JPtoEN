package dev.cosgy.jmusicbot.slashcommands.listeners;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.CommandListener;
import com.jagrosh.jmusicbot.JMusicBot;
import dev.cosgy.jmusicbot.util.LastSendTextChannel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandAudit implements CommandListener {
    /**
     * Called when a {@link Command Command} is triggered
     * by a {@link CommandEvent CommandEvent}.
     *
     * @param event   The CommandEvent that triggered the Command
     * @param command 実行されたコマンドオブジェクト
     */
    @Override
    public void onCommand(CommandEvent event, Command command) {
        if (JMusicBot.COMMAND_AUDIT_ENABLED) {
            Logger logger = LoggerFactory.getLogger("CommandAudit");
            String textFormat = event.isFromType(ChannelType.PRIVATE) ? "%s%s executed command %s#%s (%s) in %s" : "%s executed command %s#%s (%s) in #%s on %s";

            logger.info(String.format(textFormat,
                    event.isFromType(ChannelType.PRIVATE) ? "DM" : event.getGuild().getName(),
                    event.isFromType(ChannelType.PRIVATE) ? "" : event.getTextChannel().getName(),
                    event.getAuthor().getName(), event.getAuthor().getDiscriminator(), event.getAuthor().getId(),
                    event.getMessage().getContentDisplay()));
        }

        LastSendTextChannel.SetLastTextId(event);
    }
}

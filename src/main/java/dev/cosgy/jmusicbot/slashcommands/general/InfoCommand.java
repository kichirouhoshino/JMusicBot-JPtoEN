package dev.cosgy.jmusicbot.slashcommands.general;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import dev.cosgy.jmusicbot.util.MaintenanceInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Kosugi_kun
 */
public class InfoCommand extends SlashCommand {

    public InfoCommand(Bot bot) {
        this.name = "info";
        this.help = "Provides maintenance information.";
        this.guildOnly = false;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date nowTime = now.getTime();
        event.reply("Receiving announcement...").queue(m -> {
            try {
                if (MaintenanceInfo.Verification()) {
                    MaintenanceInfo infoResult = MaintenanceInfo.GetInfo();

                    MessageCreateBuilder builder = new MessageCreateBuilder().addContent("**").addContent(infoResult.Title).addContent("**");
                    EmbedBuilder ebuilder = new EmbedBuilder()
                            .setColor(Color.orange)
                            .setDescription(infoResult.Content);
                    if (!infoResult.StartTime.equals("")) {
                        ebuilder.addField("Start Time:", infoResult.StartTime, false);
                    }
                    if (!infoResult.EndTime.equals("")) {
                        ebuilder.addField("End Time:", infoResult.EndTime, false);
                    }
                    ebuilder.addField("Last Updated:", infoResult.LastUpdate, false)
                            .addField("Current Time", sdf.format(nowTime), false)
                            .setFooter("※Maintenance periods may change without notice.", null);
                    m.editOriginalEmbeds(ebuilder.build()).queue();
                } else {
                    m.editOriginal("No announcement available.").queue();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    @Override
    protected void execute(CommandEvent event) {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date nowTime = now.getTime();
        Message m = event.getChannel().sendMessage("Receiving announcement...").complete();
        try {
            if (MaintenanceInfo.Verification()) {
                MaintenanceInfo infoResult = MaintenanceInfo.GetInfo();

                MessageCreateBuilder builder = new MessageCreateBuilder().addContent("**").addContent(infoResult.Title).addContent("**");
                EmbedBuilder ebuilder = new EmbedBuilder()
                        .setColor(Color.orange)
                        .setDescription(infoResult.Content);
                if (!infoResult.StartTime.equals("")) {
                    ebuilder.addField("Start Time:", infoResult.StartTime, false);
                }
                if (!infoResult.EndTime.equals("")) {
                    ebuilder.addField("End Time:", infoResult.EndTime, false);
                }
                ebuilder.addField("Last Updated:", infoResult.LastUpdate, false)
                        .addField("Current Time", sdf.format(nowTime), false)
                        .setFooter("※Maintenance periods may change without notice.", null);
                m.editMessageEmbeds(ebuilder.build()).queue();

            } else {
                m.editMessage("No announcement available.").queue();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

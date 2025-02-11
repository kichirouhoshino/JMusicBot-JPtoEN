package dev.cosgy.jmusicbot.slashcommands.general;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ServerInfo extends SlashCommand {
    public ServerInfo(Bot bot) {
        this.name = "serverinfo";
        this.help = "Displays information about the server";
        this.guildOnly = true;
        this.category = new Category("General");
        this.aliases = bot.getConfig().getAliases(this.name);
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        String GuildName = event.getGuild().getName();
        String GuildIconURL = event.getGuild().getIconUrl();
        String GuildId = event.getGuild().getId();
        String GuildOwner = Objects.requireNonNull(event.getGuild().getOwner()).getUser().getName() + "#" + event.getGuild().getOwner().getUser().getDiscriminator();
        String GuildCreatedDate = event.getGuild().getTimeCreated().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));

        String GuildRolesCount = String.valueOf(event.getGuild().getRoles().size());
        String GuildMember = String.valueOf(event.getGuild().getMembers().size());
        String GuildCategoryCount = String.valueOf(event.getGuild().getCategories().size());
        String GuildTextChannelCount = String.valueOf(event.getGuild().getTextChannels().size());
        String GuildVoiceChannelCount = String.valueOf(event.getGuild().getVoiceChannels().size());
        String GuildStageChannelCount = String.valueOf(event.getGuild().getStageChannels().size());
        String GuildForumChannelCount = String.valueOf(event.getGuild().getForumChannels().size());
        String GuildLocation = event.getGuild().getLocale().getNativeName();
                /*
                .replace("japan", ":flag_jp: Japan")
                .replace("singapore", ":flag_sg: Singapore")
                .replace("hongkong", ":flag_hk: Hong Kong")
                .replace("Brazil", ":flag_br: Brazil")
                .replace("us-central", ":flag_us: Central America")
                .replace("us-west", ":flag_us: Western America")
                .replace("us-east", ":flag_us: Eastern America")
                .replace("us-south", ":flag_us: Southern America")
                .replace("sydney", ":flag_au: Sydney")
                .replace("eu-west", ":flag_eu: Western Europe")
                .replace("eu-central", ":flag_eu: Central Europe")
                .replace("russia", ":flag_ru: Russia");
                 */

        EmbedBuilder eb = new EmbedBuilder();

        eb.setAuthor("Information about server " + GuildName, null, GuildIconURL);

        eb.addField("Server ID", GuildId, true);
        eb.addField("Primary Language of the Server", GuildLocation, true);
        eb.addField("Server Owner", GuildOwner, true);
        eb.addField("Member Count", GuildMember, true);
        eb.addField("Role Count", GuildRolesCount, true);
        eb.addField("Category Count", GuildCategoryCount, true);
        eb.addField("Text Channel Count", GuildTextChannelCount, true);
        eb.addField("Voice Channel Count", GuildVoiceChannelCount, true);
        eb.addField("Stage Channel Count", GuildStageChannelCount, true);
        eb.addField("Forum Channel Count", GuildForumChannelCount, true);

        eb.setFooter("Server Creation Date: " + GuildCreatedDate, null);

        event.replyEmbeds(eb.build()).queue();
    }

    @Override
    public void execute(CommandEvent event) {
        String GuildName = event.getGuild().getName();
        String GuildIconURL = event.getGuild().getIconUrl();
        String GuildId = event.getGuild().getId();
        String GuildOwner = Objects.requireNonNull(event.getGuild().getOwner()).getUser().getName() + "#" + event.getGuild().getOwner().getUser().getDiscriminator();
        String GuildCreatedDate = event.getGuild().getTimeCreated().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));

        String GuildRolesCount = String.valueOf(event.getGuild().getRoles().size());
        String GuildMember = String.valueOf(event.getGuild().getMembers().size());
        String GuildCategoryCount = String.valueOf(event.getGuild().getCategories().size());
        String GuildTextChannelCount = String.valueOf(event.getGuild().getTextChannels().size());
        String GuildVoiceChannelCount = String.valueOf(event.getGuild().getVoiceChannels().size());
        String GuildStageChannelCount = String.valueOf(event.getGuild().getStageChannels().size());
        String GuildForumChannelCount = String.valueOf(event.getGuild().getForumChannels().size());
        String GuildLocation = event.getGuild().getLocale().getNativeName();
                /*.replace("japan", ":flag_jp: Japan")
                .replace("singapore", ":flag_sg: Singapore")
                .replace("hongkong", ":flag_hk: Hong Kong")
                .replace("Brazil", ":flag_br: Brazil")
                .replace("us-central", ":flag_us: Central America")
                .replace("us-west", ":flag_us: Western America")
                .replace("us-east", ":flag_us: Eastern America")
                .replace("us-south", ":flag_us: Southern America")
                .replace("sydney", ":flag_au: Sydney")
                .replace("eu-west", ":flag_eu: Western Europe")
                .replace("eu-central", ":flag_eu: Central Europe")
                .replace("russia", ":flag_ru: Russia");*/


        EmbedBuilder eb = new EmbedBuilder();

        eb.setAuthor("Information about server " + GuildName, null, GuildIconURL);

        eb.addField("Server ID", GuildId, true);
        eb.addField("Primary Language of the Server", GuildLocation, true);
        eb.addField("Server Owner", GuildOwner, true);
        eb.addField("Member Count", GuildMember, true);
        eb.addField("Role Count", GuildRolesCount, true);
        eb.addField("Category Count", GuildCategoryCount, true);
        eb.addField("Text Channel Count", GuildTextChannelCount, true);
        eb.addField("Voice Channel Count", GuildVoiceChannelCount, true);
        eb.addField("Stage Channel Count", GuildStageChannelCount, true);
        eb.addField("Forum Channel Count", GuildForumChannelCount, true);

        eb.setFooter("Server Creation Date: " + GuildCreatedDate, null);

        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }
}

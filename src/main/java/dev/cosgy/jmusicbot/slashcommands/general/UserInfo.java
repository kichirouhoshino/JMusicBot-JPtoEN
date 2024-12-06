package dev.cosgy.jmusicbot.slashcommands.general;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UserInfo extends SlashCommand {
    Logger log = LoggerFactory.getLogger("UserInfo");

    public UserInfo() {
        this.name = "userinfo";
        this.help = "Displays information about the specified user";
        this.arguments = "<user>";
        this.guildOnly = true;
        this.category = new Category("General");

        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.USER, "user", "User", true));
        this.options = options;

    }

    @Override
    protected void execute(SlashCommandEvent event) {
        Member memb = event.getOption("user").getAsMember();

        EmbedBuilder eb = new EmbedBuilder().setColor(memb.getColor());
        String NAME = memb.getEffectiveName();
        String TAG = "#" + memb.getUser().getDiscriminator();
        String GUILD_JOIN_DATE = memb.getTimeJoined().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        String DISCORD_JOINED_DATE = memb.getUser().getTimeCreated().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        String ID = memb.getUser().getId();
        String STATUS = memb.getOnlineStatus().getKey().replace("offline", ":x: Offline").replace("dnd", ":red_circle: Do not disturb").replace("idle", "Idle").replace("online", ":white_check_mark: Online");
        String ROLES;
        String GAME;
        String AVATAR = memb.getUser().getAvatarUrl();

        log.debug("\nUsername: " + memb.getEffectiveName() + "\n" +
                "Tag: " + memb.getUser().getDiscriminator() + "\n" +
                "Guild join date: "
                + memb.getUser().getTimeCreated()
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")) + "\n" +
                "User ID: " + memb.getUser().getId() + "\n" +
                "Online status: " + memb.getOnlineStatus());

        try {
            GAME = memb.getActivities().toString();
        } catch (Exception e) {
            GAME = "-/-";
        }

        StringBuilder ROLESBuilder = new StringBuilder();
        for (Role r : memb.getRoles()) {
            ROLESBuilder.append(r.getName()).append(", ");
        }
        ROLES = ROLESBuilder.toString();
        if (ROLES.length() > 0)
            ROLES = ROLES.substring(0, ROLES.length() - 2);
        else
            ROLES = "No roles in this server";

        if (AVATAR == null) {
            AVATAR = "No avatar";
        }

        eb.setAuthor(memb.getUser().getName() + TAG + "'s user info", null, null)
                .addField(":pencil2: Name/Nickname", "**" + NAME + "**", true)
                .addField(":link: DiscordTag", "**" + TAG + "**", true)
                .addField(":1234: ID", "**" + ID + "**", true)
                .addBlankField(false)
                .addField(":signal_strength: Current status", "**" + STATUS + "**", true)
                .addField(":video_game: Playing", "**" + GAME + "**", true)
                .addField(":tools: Roles", "**" + ROLES + "**", true)
                .addBlankField(false)
                .addField(":inbox_tray: Server join date", "**" + GUILD_JOIN_DATE + "**", true)
                .addField(":beginner: Account created on", "**" + DISCORD_JOINED_DATE + "**", true)
                .addBlankField(false)
                .addField(":frame_photo: Avatar URL", AVATAR, false);

        if (!AVATAR.equals("No avatar")) {
            eb.setAuthor(memb.getUser().getName() + TAG + "'s user info", null, AVATAR);
        }

        event.replyEmbeds(eb.build()).queue();
    }

    @Override
    public void execute(CommandEvent event) {
        Member memb;

        if (event.getArgs().length() > 0) {
            try {

                if (event.getMessage().getReferencedMessage().getMentions().getMembers().size() != 0) {
                    memb = event.getMessage().getReferencedMessage().getMentions().getMembers().get(0);
                } else {
                    memb = FinderUtil.findMembers(event.getArgs(), event.getGuild()).get(0);
                }
            } catch (Exception e) {
                event.reply("User \"" + event.getArgs() + "\" not found.");
                return;
            }
        } else {
            memb = event.getMember();
        }

        EmbedBuilder eb = new EmbedBuilder().setColor(memb.getColor());
        String NAME = memb.getEffectiveName();
        String TAG = "#" + memb.getUser().getDiscriminator();
        String GUILD_JOIN_DATE = memb.getTimeJoined().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        String DISCORD_JOINED_DATE = memb.getUser().getTimeCreated().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        String ID = memb.getUser().getId();
        String STATUS = memb.getOnlineStatus().getKey().replace("offline", ":x: Offline").replace("dnd", ":red_circle: Do not disturb").replace("idle", "Idle").replace("online", ":white_check_mark: Online");
        String ROLES;
        String GAME;
        String AVATAR = memb.getUser().getAvatarUrl();

        log.debug("\nUsername: " + memb.getEffectiveName() + "\n" +
                "Tag: " + memb.getUser().getDiscriminator() + "\n" +
                "Guild join date: "
                + memb.getUser().getTimeCreated()
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")) + "\n" +
                "User ID: " + memb.getUser().getId() + "\n" +
                "Online status: " + memb.getOnlineStatus());

        try {
            GAME = memb.getActivities().toString();
        } catch (Exception e) {
            GAME = "-/-";
        }

        StringBuilder ROLESBuilder = new StringBuilder();
        for (Role r : memb.getRoles()) {
            ROLESBuilder.append(r.getName()).append(", ");
        }
        ROLES = ROLESBuilder.toString();
        if (ROLES.length() > 0)
            ROLES = ROLES.substring(0, ROLES.length() - 2);
        else
            ROLES = "No roles in this server";

        if (AVATAR == null) {
            AVATAR = "No avatar";
        }

        eb.setAuthor(memb.getUser().getName() + TAG + "'s user info", null, null)
                .addField(":pencil2: Name/Nickname", "**" + NAME + "**", true)
                .addField(":link: DiscordTag", "**" + TAG + "**", true)
                .addField(":1234: ID", "**" + ID + "**", true)
                .addBlankField(false)
                .addField(":signal_strength: Current status", "**" + STATUS + "**", true)
                .addField(":video_game: Playing", "**" + GAME + "**", true)
                .addField(":tools: Roles", "**" + ROLES + "**", true)
                .addBlankField(false)
                .addField(":inbox_tray: Server join date", "**" + GUILD_JOIN_DATE + "**", true)
                .addField(":beginner: Account created on", "**" + DISCORD_JOINED_DATE + "**", true)
                .addBlankField(false)
                .addField(":frame_photo: Avatar URL", AVATAR, false);

        if (!AVATAR.equals("No avatar")) {
            eb.setAuthor(memb.getUser().getName() + TAG + "'s user info", null, AVATAR);
        }

        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }
}

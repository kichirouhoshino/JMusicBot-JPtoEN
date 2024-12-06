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

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.playlist.PlaylistLoader.Playlist;
import dev.cosgy.jmusicbot.slashcommands.DJCommand;
import dev.cosgy.jmusicbot.util.StackTraceUtil;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class PlaylistCmd extends DJCommand {

    public PlaylistCmd(Bot bot) {
        super(bot);
        this.guildOnly = true;
        this.name = "playlist";
        this.arguments = "<append|delete|make>";
        this.help = "Manage playlists";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.children = new DJCommand[]{
                new ListCmd(bot),
                new AppendlistCmd(bot),
                new DeletelistCmd(bot),
                new MakelistCmd(bot)
        };
    }

    @Override
    public void doCommand(CommandEvent event) {
        StringBuilder builder = new StringBuilder(event.getClient().getWarning() + " Playlist management commands:\n");
        for (Command cmd : this.children)
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName())
                    .append(" ").append(cmd.getArguments() == null ? "" : cmd.getArguments()).append("` - ").append(cmd.getHelp());
        event.reply(builder.toString());
    }

    @Override
    public void doCommand(SlashCommandEvent slashCommandEvent) {
        // This method will not be executed.
    }

    public class MakelistCmd extends DJCommand {
        public MakelistCmd(Bot bot) {
            super(bot);
            this.name = "make";
            this.aliases = new String[]{"create"};
            this.help = "Create a new playlist";
            this.arguments = "<name>";
            this.guildOnly = true;
            this.ownerCommand = false;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", "Playlist name", true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {

            String pName = event.getArgs().replaceAll("\\s+", "_");
            String guildId = event.getGuild().getId();

            if (pName == null || pName.isEmpty()) {
                event.replyError("Please enter a playlist name.");
            } else if (bot.getPlaylistLoader().getPlaylist(guildId, pName) == null) {
                try {
                    bot.getPlaylistLoader().createPlaylist(guildId, pName);
                    event.reply(event.getClient().getSuccess() + "Playlist `" + pName + "` has been created.");
                } catch (IOException e) {
                    if (event.isOwner() || event.getMember().isOwner()) {
                        event.replyError("An error occurred while loading songs.\n" +
                                "**Error details: " + e.getLocalizedMessage() + "**");
                        StackTraceUtil.sendStackTrace(event.getTextChannel(), e);
                        return;
                    }

                    event.reply(event.getClient().getError() + " Could not create the playlist: " + e.getLocalizedMessage());
                }
            } else {
                event.reply(event.getClient().getError() + " Playlist `" + pName + "` already exists.");
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            if (!checkDJPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + "You don't have permission to execute this command.").queue();
                return;
            }
            String pname = event.getOption("name").getAsString();
            String guildId = event.getGuild().getId();
            if (pname == null || pname.isEmpty()) {
                event.reply(event.getClient().getError() + "Please enter a playlist name.").queue();
            } else if (bot.getPlaylistLoader().getPlaylist(guildId, pname) == null) {
                try {
                    bot.getPlaylistLoader().createPlaylist(guildId, pname);
                    event.reply(event.getClient().getSuccess() + "Playlist `" + pname + "` has been created.").queue();
                } catch (IOException e) {
                    if (event.getClient().getOwnerId() == event.getMember().getId() || event.getMember().isOwner()) {
                        event.reply(event.getClient().getError() + "An error occurred while loading songs.\n" +
                                "**Error details: " + e.getLocalizedMessage() + "**").queue();
                        StackTraceUtil.sendStackTrace(event.getTextChannel(), e);
                        return;
                    }

                    event.reply(event.getClient().getError() + " Could not create the playlist: " + e.getLocalizedMessage()).queue();
                }
            } else {
                event.reply(event.getClient().getError() + " Playlist `" + pname + "` already exists.").queue();
            }
        }
    }

    public class DeletelistCmd extends DJCommand {
        public DeletelistCmd(Bot bot) {
            super(bot);
            this.name = "delete";
            this.aliases = new String[]{"remove"};
            this.help = "Delete an existing playlist";
            this.arguments = "<name>";
            this.guildOnly = true;
            this.ownerCommand = false;
            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", "Playlist name", true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {
            String pname = event.getArgs().replaceAll("\\s+", "_");
            String guildid = event.getGuild().getId();
            if (!pname.equals("")) {
                if (bot.getPlaylistLoader().getPlaylist(guildid, pname) == null)
                    event.reply(event.getClient().getError() + " Playlist does not exist: `" + pname + "`");
                else {
                    try {
                        bot.getPlaylistLoader().deletePlaylist(guildid, pname);
                        event.reply(event.getClient().getSuccess() + " Playlist deleted: `" + pname + "`");
                    } catch (IOException e) {
                        event.reply(event.getClient().getError() + " Could not delete playlist: " + e.getLocalizedMessage());
                    }
                }
            } else {
                event.reply(event.getClient().getError() + "Please include the playlist name.");
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            if (!checkDJPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + "You don't have permission to execute this command.").queue();
                return;
            }
            String pname = event.getOption("name").getAsString();
            String guildid = event.getGuild().getId();
            if (bot.getPlaylistLoader().getPlaylist(guildid, pname) == null)
                event.reply(event.getClient().getError() + " Playlist does not exist: `" + pname + "`").queue();
            else {
                try {
                    bot.getPlaylistLoader().deletePlaylist(guildid, pname);
                    event.reply(event.getClient().getSuccess() + " Playlist deleted: `" + pname + "`").queue();
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + " Could not delete playlist: " + e.getLocalizedMessage()).queue();
                }
            }
        }
    }

    public class AppendlistCmd extends DJCommand {
        public AppendlistCmd(Bot bot) {
            super(bot);
            this.name = "append";
            this.aliases = new String[]{"add"};
            this.help = "Add tracks to an existing playlist";
            this.arguments = "<name> <URL>| <URL> | ...";
            this.guildOnly = true;
            this.ownerCommand = false;
            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", "Playlist name", true));
            options.add(new OptionData(OptionType.STRING, "url", "URL", true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {

            String[] parts = event.getArgs().split("\\s+", 2);
            String guildid = event.getGuild().getId();
            if (parts.length < 2) {
                event.reply(event.getClient().getError() + " Please include the playlist name and URLs.");
                return;
            }
            String pname = parts[0];
            Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildid, pname);
            if (playlist == null)
                event.reply(event.getClient().getError() + " Playlist does not exist: `" + pname + "`");
            else {
                StringBuilder builder = new StringBuilder();
                playlist.getItems().forEach(item -> builder.append("\r\n").append(item));
                String[] urls = parts[1].split("\\|");
                for (String url : urls) {
                    String u = url.trim();
                    if (u.startsWith("<") && u.endsWith(">"))
                        u = u.substring(1, u.length() - 1);
                    builder.append("\r\n").append(u);
                }
                try {
                    bot.getPlaylistLoader().writePlaylist(guildid, pname, builder.toString());
                    event.reply(event.getClient().getSuccess() + urls.length + " items added to the playlist: `" + pname + "`");
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + " Could not add to playlist: " + e.getLocalizedMessage());
                }
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            if (!checkDJPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + "You don't have permission to execute this command.").queue();
                return;
            }

            String guildid = event.getGuild().getId();
            String pname = event.getOption("name").getAsString();
            Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildid, pname);
            if (playlist == null)
                event.reply(event.getClient().getError() + " Playlist does not exist: `" + pname + "`").queue();
            else {
                StringBuilder builder = new StringBuilder();
                playlist.getItems().forEach(item -> builder.append("\r\n").append(item));
                String[] urls = event.getOption("url").getAsString().split("\\|");
                for (String url : urls) {
                    String u = url.trim();
                    if (u.startsWith("<") && u.endsWith(">"))
                        u = u.substring(1, u.length() - 1);
                    builder.append("\r\n").append(u);
                }
                try {
                    bot.getPlaylistLoader().writePlaylist(guildid, pname, builder.toString());
                    event.reply(event.getClient().getSuccess() + urls.length + " items added to the playlist: `" + pname + "`").queue();
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + " Could not add to playlist: " + e.getLocalizedMessage()).queue();
                }
            }
        }
    }

    public class ListCmd extends DJCommand {
        public ListCmd(Bot bot) {
            super(bot);
            this.name = "all";
            this.aliases = new String[]{"available", "list"};
            this.help = "Display all available playlists";
            this.guildOnly = true;
            this.ownerCommand = false;
        }

        @Override
        public void doCommand(CommandEvent event) {
            String guildId = event.getGuild().getId();

            if (!bot.getPlaylistLoader().folderGuildExists(guildId))
                bot.getPlaylistLoader().createGuildFolder(guildId);
            if (!bot.getPlaylistLoader().folderGuildExists(guildId)) {
                event.reply(event.getClient().getWarning() + " Could not create the playlist folder.");
                return;
            }
            List<String> list = bot.getPlaylistLoader().getPlaylistNames(guildId);
            if (list == null)
                event.reply(event.getClient().getError() + " Could not load available playlists.");
            else if (list.isEmpty())
                event.reply(event.getClient().getWarning() + " There are no playlists in the folder.");
            else {
                StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + " Available playlists:\n");
                list.forEach(str -> builder.append("`").append(str).append("` "));
                event.reply(builder.toString());
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            if (!checkDJPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + "You don't have permission to execute this command.").queue();
                return;
            }
            String guildId = event.getGuild().getId();
            if (!bot.getPlaylistLoader().folderGuildExists(guildId))
                bot.getPlaylistLoader().createGuildFolder(guildId);
            if (!bot.getPlaylistLoader().folderGuildExists(guildId)) {
                event.reply(event.getClient().getWarning() + " Could not create the playlist folder.").queue();
                return;
            }
            List<String> list = bot.getPlaylistLoader().getPlaylistNames(guildId);
            if (list == null)
                event.reply(event.getClient().getError() + " Could not load available playlists.").queue();
            else if (list.isEmpty())
                event.reply(event.getClient().getWarning() + " There are no playlists in the folder.").queue();
            else {
                StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + " Available playlists:\n");
                list.forEach(str -> builder.append("`").append(str).append("` "));
                event.reply(builder.toString()).queue();
            }
        }
    }
}
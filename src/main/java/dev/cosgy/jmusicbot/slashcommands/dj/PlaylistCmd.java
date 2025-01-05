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
import com.jagrosh.jmusicbot.playlist.PlaylistLoader;
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
        this.arguments = "<append|delete|make|show>";
        this.help = "再生リスト管理";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.children = new DJCommand[]{
                new ListCmd(bot),
                new AppendlistCmd(bot),
                new DeletelistCmd(bot),
                new MakelistCmd(bot),
                new ShowTracksCmd(bot)
        };
    }

    @Override
    public void doCommand(CommandEvent event) {

        StringBuilder builder = new StringBuilder(event.getClient().getWarning() + " 再生リスト管理コマンド:\n");
        for (Command cmd : this.children)
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName())
                    .append(" ").append(cmd.getArguments() == null ? "" : cmd.getArguments()).append("` - ").append(cmd.getHelp());
        event.reply(builder.toString());
    }

    @Override
    public void doCommand(SlashCommandEvent slashCommandEvent) {
        // ここは、実行されません。
    }

    /**
     * 新しいサブコマンド: ShowTracksCmd
     * 指定された再生リスト内の曲を一覧表示します。
     */
    public class ShowTracksCmd extends DJCommand {
        public ShowTracksCmd(Bot bot) {
            super(bot);
            this.name = "show";
            this.help = "指定した再生リスト内の曲を表示";
            this.arguments = "<name>";
            this.guildOnly = true;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", "プレイリスト名", true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {
            String guildId = event.getGuild().getId();
            String playlistName = event.getArgs().trim();

            if (playlistName.isEmpty()) {
                event.reply(event.getClient().getError() + " プレイリスト名を指定してください。");
                return;
            }

            PlaylistLoader.Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildId, playlistName);
            if (playlist == null) {
                event.reply(event.getClient().getError() + " 再生リスト `" + playlistName + "` が見つかりませんでした。");
                return;
            }

            if (playlist.getItems().isEmpty()) {
                event.reply(event.getClient().getWarning() + " 再生リスト `" + playlistName + "` に曲がありません。");
                return;
            }

            StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + " 再生リスト `" + playlistName + "` 内の曲:\n");
            for (int i = 0; i < playlist.getItems().size(); i++) {
                builder.append(i + 1).append(". ").append(playlist.getItems().get(i)).append("\n");
            }

            if (builder.length() > 2000) {
                builder.setLength(1997); // Discordのメッセージ制限に対応
                builder.append("...");
            }

            event.reply(builder.toString());
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            String guildId = event.getGuild().getId();
            String playlistName = event.getOption("name").getAsString();

            PlaylistLoader.Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildId, playlistName);
            if (playlist == null) {
                event.reply(event.getClient().getError() + " 再生リスト `" + playlistName + "` が見つかりませんでした。").queue();
                return;
            }

            if (playlist.getItems().isEmpty()) {
                event.reply(event.getClient().getWarning() + " 再生リスト `" + playlistName + "` に曲がありません。").queue();
                return;
            }

            StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + " 再生リスト `" + playlistName + "` 内の曲:\n");
            for (int i = 0; i < playlist.getItems().size(); i++) {
                builder.append(i + 1).append(". ").append(playlist.getItems().get(i)).append("\n");
            }

            if (builder.length() > 2000) {
                builder.setLength(1997); // Discordのメッセージ制限に対応
                builder.append("...");
            }

            event.reply(builder.toString()).queue();
        }
    }

    public class MakelistCmd extends DJCommand {
        public MakelistCmd(Bot bot) {
            super(bot);
            this.name = "make";
            this.aliases = new String[]{"create"};
            this.help = "再生リストを新規作成";
            this.arguments = "<name>";
            this.guildOnly = true;
            this.ownerCommand = false;

            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", "プレイリスト名", true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {

            String pName = event.getArgs().replaceAll("\\s+", "_");
            String guildId = event.getGuild().getId();

            if (pName == null || pName.isEmpty()) {
                event.replyError("プレイリストの名前を入力してください。");
            } else if (bot.getPlaylistLoader().getPlaylist(guildId, pName) == null) {
                try {
                    bot.getPlaylistLoader().createPlaylist(guildId, pName);
                    event.reply(event.getClient().getSuccess() + "再生リスト `" + pName + "` を作成しました");
                } catch (IOException e) {
                    if (event.isOwner() || event.getMember().isOwner()) {
                        event.replyError("曲の読み込み中にエラーが発生しました。\n" +
                                "**エラーの内容: " + e.getLocalizedMessage() + "**");
                        StackTraceUtil.sendStackTrace(event.getTextChannel(), e);
                        return;
                    }

                    event.reply(event.getClient().getError() + " 再生リストを作成できませんでした。:" + e.getLocalizedMessage());
                }
            } else {
                event.reply(event.getClient().getError() + " 再生リスト `" + pName + "` は既に存在します");
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            if (!checkDJPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + "権限がないため実行できません。").queue();
                return;
            }
            String pname = event.getOption("name").getAsString();
            String guildId = event.getGuild().getId();
            if (pname == null || pname.isEmpty()) {
                event.reply(event.getClient().getError() + "プレイリストの名前を入力してください。").queue();
            } else if (bot.getPlaylistLoader().getPlaylist(guildId, pname) == null) {
                try {
                    bot.getPlaylistLoader().createPlaylist(guildId, pname);
                    event.reply(event.getClient().getSuccess() + "再生リスト `" + pname + "` を作成しました").queue();
                } catch (IOException e) {
                    if (event.getClient().getOwnerId() == event.getMember().getId() || event.getMember().isOwner()) {
                        event.reply(event.getClient().getError() + "曲の読み込み中にエラーが発生しました。\n" +
                                "**エラーの内容: " + e.getLocalizedMessage() + "**").queue();
                        StackTraceUtil.sendStackTrace(event.getTextChannel(), e);
                        return;
                    }

                    event.reply(event.getClient().getError() + " 再生リストを作成できませんでした。:" + e.getLocalizedMessage()).queue();
                }
            } else {
                event.reply(event.getClient().getError() + " 再生リスト `" + pname + "` は既に存在します").queue();
            }
        }
    }

    public class DeletelistCmd extends DJCommand {
        public DeletelistCmd(Bot bot) {
            super(bot);
            this.name = "delete";
            this.aliases = new String[]{"remove"};
            this.help = "既存の再生リストを削除";
            this.arguments = "<name>";
            this.guildOnly = true;
            this.ownerCommand = false;
            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", "プレイリスト名", true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {

            String pname = event.getArgs().replaceAll("\\s+", "_");
            String guildid = event.getGuild().getId();
            if (!pname.equals("")) {
                if (bot.getPlaylistLoader().getPlaylist(guildid, pname) == null)
                    event.reply(event.getClient().getError() + " 再生リストは存在しません:`" + pname + "`");
                else {
                    try {
                        bot.getPlaylistLoader().deletePlaylist(guildid, pname);
                        event.reply(event.getClient().getSuccess() + " 再生リストを削除しました:`" + pname + "`");
                    } catch (IOException e) {
                        event.reply(event.getClient().getError() + " 再生リストを削除できませんでした: " + e.getLocalizedMessage());
                    }
                }
            } else {
                event.reply(event.getClient().getError() + "再生リストの名前を含めてください");
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            if (!checkDJPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + "権限がないため実行できません。").queue();
                return;
            }
            String pname = event.getOption("name").getAsString();
            String guildid = event.getGuild().getId();
            if (bot.getPlaylistLoader().getPlaylist(guildid, pname) == null)
                event.reply(event.getClient().getError() + " 再生リストは存在しません:`" + pname + "`").queue();
            else {
                try {
                    bot.getPlaylistLoader().deletePlaylist(guildid, pname);
                    event.reply(event.getClient().getSuccess() + " 再生リストを削除しました:`" + pname + "`").queue();
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + " 再生リストを削除できませんでした: " + e.getLocalizedMessage()).queue();
                }
            }
        }
    }

    public class AppendlistCmd extends DJCommand {
        public AppendlistCmd(Bot bot) {
            super(bot);
            this.name = "append";
            this.aliases = new String[]{"add"};
            this.help = "既存の再生リストに曲を追加";
            this.arguments = "<name> <URL>| <URL> | ...";
            this.guildOnly = true;
            this.ownerCommand = false;
            List<OptionData> options = new ArrayList<>();
            options.add(new OptionData(OptionType.STRING, "name", "プレイリスト名", true));
            options.add(new OptionData(OptionType.STRING, "url", "URL", true));
            this.options = options;
        }

        @Override
        public void doCommand(CommandEvent event) {

            String[] parts = event.getArgs().split("\\s+", 2);
            String guildid = event.getGuild().getId();
            if (parts.length < 2) {
                event.reply(event.getClient().getError() + " 追加先の再生リスト名とURLを含めてください。");
                return;
            }
            String pname = parts[0];
            Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildid, pname);
            if (playlist == null)
                event.reply(event.getClient().getError() + " 再生リストは存在しません:`" + pname + "`");
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
                    event.reply(event.getClient().getSuccess() + urls.length + " 項目を再生リストに追加しました:`" + pname + "`");
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + " 再生リストに追加できませんでした: " + e.getLocalizedMessage());
                }
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            if (!checkDJPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + "権限がないため実行できません。").queue();
                return;
            }

            String guildid = event.getGuild().getId();
            String pname = event.getOption("name").getAsString();
            Playlist playlist = bot.getPlaylistLoader().getPlaylist(guildid, pname);
            if (playlist == null)
                event.reply(event.getClient().getError() + " 再生リストは存在しません:`" + pname + "`").queue();
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
                    event.reply(event.getClient().getSuccess() + urls.length + " 項目を再生リストに追加しました:`" + pname + "`").queue();
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + " 再生リストに追加できませんでした: " + e.getLocalizedMessage()).queue();
                }
            }
        }
    }

    public class ListCmd extends DJCommand {
        public ListCmd(Bot bot) {
            super(bot);
            this.name = "all";
            this.aliases = new String[]{"available", "list"};
            this.help = "利用可能なすべての再生リストを表示";
            this.guildOnly = true;
            this.ownerCommand = false;
        }

        @Override
        public void doCommand(CommandEvent event) {
            String guildId = event.getGuild().getId();

            if (!bot.getPlaylistLoader().folderGuildExists(guildId))
                bot.getPlaylistLoader().createGuildFolder(guildId);
            if (!bot.getPlaylistLoader().folderGuildExists(guildId)) {
                event.reply(event.getClient().getWarning() + " 再生リストフォルダが存在しないため作成できませんでした。");
                return;
            }
            List<String> list = bot.getPlaylistLoader().getPlaylistNames(guildId);
            if (list == null)
                event.reply(event.getClient().getError() + " 利用可能な再生リストを読み込めませんでした。");
            else if (list.isEmpty())
                event.reply(event.getClient().getWarning() + " 再生リストフォルダに再生リストがありません。");
            else {
                StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + " 利用可能な再生リスト:\n");
                list.forEach(str -> builder.append("`").append(str).append("` "));
                event.reply(builder.toString());
            }
        }

        @Override
        public void doCommand(SlashCommandEvent event) {
            if (!checkDJPermission(event.getClient(), event)) {
                event.reply(event.getClient().getWarning() + "権限がないため実行できません。").queue();
                return;
            }
            String guildId = event.getGuild().getId();
            if (!bot.getPlaylistLoader().folderGuildExists(guildId))
                bot.getPlaylistLoader().createGuildFolder(guildId);
            if (!bot.getPlaylistLoader().folderGuildExists(guildId)) {
                event.reply(event.getClient().getWarning() + " 再生リストフォルダが存在しないため作成できませんでした。").queue();
                return;
            }
            List<String> list = bot.getPlaylistLoader().getPlaylistNames(guildId);
            if (list == null)
                event.reply(event.getClient().getError() + " 利用可能な再生リストを読み込めませんでした。").queue();
            else if (list.isEmpty())
                event.reply(event.getClient().getWarning() + " 再生リストフォルダに再生リストがありません。").queue();
            else {
                StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + " 利用可能な再生リスト:\n");
                list.forEach(str -> builder.append("`").append(str).append("` "));
                event.reply(builder.toString()).queue();
            }
        }
    }
}
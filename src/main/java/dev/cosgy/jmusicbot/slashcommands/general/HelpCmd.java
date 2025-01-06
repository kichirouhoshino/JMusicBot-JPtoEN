/*
 *  Copyright 2021 Cosgy Dev (info@cosgy.dev).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.cosgy.jmusicbot.slashcommands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import net.dv8tion.jda.api.entities.channel.ChannelType;

import java.util.List;
import java.util.Objects;

public class HelpCmd extends SlashCommand {
    public Bot bot;

    public HelpCmd(Bot bot) {
        this.bot = bot;
        this.name = "help";
        this.help = "Displays the list of available commands.";
        this.aliases = bot.getConfig().getAliases(this.name);
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        StringBuilder builder = new StringBuilder("**" + event.getJDA().getSelfUser().getName() + "** Command List:\n");
        Category category = null;
        List<Command> commands = event.getClient().getCommands();
        for (Command command : commands) {
            if (!command.isHidden() && (!command.isOwnerCommand() || event.getMember().isOwner())) {
                if (!Objects.equals(category, command.getCategory())) {
                    category = command.getCategory();
                    builder.append("\n\n  __").append(category == null ? "No Category" : category.getName()).append("__:\n");
                }
                builder.append("\n`").append(event.getClient().getTextualPrefix()).append(event.getClient().getPrefix() == null ? " " : "").append(command.getName())
                        .append(command.getArguments() == null ? "`" : " " + command.getArguments() + "`")
                        .append(" - ").append(command.getHelp());
            }
        }
        if (event.getClient().getServerInvite() != null)
            builder.append("\n\nIf you need further help, you can join the official server: ").append(event.getClient().getServerInvite());

        event.reply(builder.toString()).queue();
    }

    public void execute(CommandEvent event) {
        StringBuilder builder = new StringBuilder("**" + event.getJDA().getSelfUser().getName() + "** Command List:\n");
        Category category = null;
        List<Command> commands = event.getClient().getCommands();
        for (Command command : commands) {
            if (!command.isHidden() && (!command.isOwnerCommand() || event.isOwner())) {
                if (!Objects.equals(category, command.getCategory())) {
                    category = command.getCategory();
                    builder.append("\n\n  __").append(category == null ? "No Category" : category.getName()).append("__:\n");
                }
                builder.append("\n`").append(event.getClient().getTextualPrefix()).append(event.getClient().getPrefix() == null ? " " : "").append(command.getName())
                        .append(command.getArguments() == null ? "`" : " " + command.getArguments() + "`")
                        .append(" - ").append(command.getHelp());
            }
        }
        if (event.getClient().getServerInvite() != null)
            builder.append("\n\nIf you need further help, you can join the official server: ").append(event.getClient().getServerInvite());

        if (bot.getConfig().getHelpToDm()) {
            event.replyInDm(builder.toString(), unused ->
            {
                if (event.isFromType(ChannelType.TEXT))
                    event.reactSuccess();
            }, t -> event.replyWarning("Unable to send help due to blocked direct messages."));
        } else {
            event.reply(builder.toString());
        }
    }
}

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
package dev.cosgy.jmusicbot.slashcommands.owner;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import dev.cosgy.jmusicbot.slashcommands.OwnerCommand;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SetstatusCmd extends OwnerCommand {
    public SetstatusCmd(Bot bot) {
        this.name = "setstatus";
        this.help = "Sets the status that the bot displays";
        this.arguments = "<status>";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "status", "One of the following statuses: ONLINE, IDLE, DND, INVISIBLE", true));
        this.options = options;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        try {
            OnlineStatus status = OnlineStatus.fromKey(event.getOption("status").getAsString());
            if (status == OnlineStatus.UNKNOWN) {
                event.reply(event.getClient().getError() + "Please include one of the following statuses: `ONLINE`, `IDLE`, `DND`, `INVISIBLE`.").queue();
            } else {
                event.getJDA().getPresence().setStatus(status);
                event.reply(event.getClient().getSuccess() + "Status has been set to `" + status.getKey().toUpperCase() + "`.").queue();
            }
        } catch (Exception e) {
            event.reply(event.getClient().getError() + " Could not set the status.").queue();
        }
    }

    @Override
    protected void execute(CommandEvent event) {
        try {
            OnlineStatus status = OnlineStatus.fromKey(event.getArgs());
            if (status == OnlineStatus.UNKNOWN) {
                event.replyError("Please include one of the following statuses: `ONLINE`, `IDLE`, `DND`, `INVISIBLE`.");
            } else {
                event.getJDA().getPresence().setStatus(status);
                event.replySuccess("Status has been set to `" + status.getKey().toUpperCase() + "`.");
            }
        } catch (Exception e) {
            event.reply(event.getClient().getError() + " Could not set the status.");
        }
    }
}
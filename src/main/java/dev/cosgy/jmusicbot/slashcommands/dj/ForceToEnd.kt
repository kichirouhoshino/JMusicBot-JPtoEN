/*
 *  Copyright 2024 Cosgy Dev (info@cosgy.dev).
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

package dev.cosgy.jmusicbot.slashcommands.dj

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.command.SlashCommandEvent
import com.jagrosh.jmusicbot.Bot
import dev.cosgy.jmusicbot.slashcommands.DJCommand
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

class ForceToEnd(bot: Bot) : DJCommand(bot) {
    init {
        this.name = "forcetoend"
        this.help = "Toggles between fair and normal song addition modes. `TRUE` enables normal mode."
        this.aliases = bot.config.getAliases(this.name)
        this.options = listOf(OptionData(OptionType.BOOLEAN, "value", "Whether to use normal addition mode", true))
    }

    override fun doCommand(event: CommandEvent) {

        val currentSetting = bot.settingsManager?.getSettings(event.guild)?.isForceToEndQue
        var newSetting = false

        if (event.args.isEmpty()) {
            newSetting = !currentSetting!!
        } else if (event.args.equals("true", ignoreCase = true) || event.args.equals("on", ignoreCase = true) || event.args.equals("enabled", ignoreCase = true)) {
            newSetting = true
        } else if (event.args.equals("false", ignoreCase = true) || event.args.equals("off", ignoreCase = true) || event.args.equals("disabled", ignoreCase = true)) {
            newSetting = false
        }

        bot.settingsManager.getSettings(event.guild)?.isForceToEndQue = newSetting

        var msg = "Changed the way songs are added to the queue.\nSetting:"
        if (newSetting == true) {
            msg += "Normal addition mode\nSongs will be added to the end of the queue."
        } else if (newSetting == false) {
            msg += "Fair addition mode\nSongs will be added to the queue in a fair order."
        }

        event.replySuccess(msg)
    }

    override fun doCommand(event: SlashCommandEvent) {
        val currentSetting = bot.settingsManager?.getSettings(event.guild)?.isForceToEndQue
        var newSetting = false

        newSetting = event.getOption("value")?.asBoolean!!

        bot.settingsManager.getSettings(event.guild)?.isForceToEndQue = newSetting

        var msg = "Changed the way songs are added to the queue.\nSetting:"
        if (newSetting == true) {
            msg += "Normal addition mode\nSongs will be added to the end of the queue."
        } else if (newSetting == false) {
            msg += "Fair addition mode\nSongs will be added to the queue in a fair order."
        }

        event.reply(msg).queue()
    }
}

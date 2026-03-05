/*
 *  Copyright 2024 Cosgy Dev (info@cosgy.dev).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 */
package dev.cosgy.jmusicbot.slashcommands.dj;

import dev.cosgy.jmusicbot.framework.jdautilities.command.CommandEvent;
import dev.cosgy.jmusicbot.framework.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.settings.Settings;
import dev.cosgy.jmusicbot.slashcommands.DJCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class ForceToEnd extends DJCommand {
    public ForceToEnd(Bot bot) {
        super(bot);
        this.name = "forcetoend";
        this.help = "Toggle between fair queue and normal queue mode. TRUE enables normal queue.";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.options = List.of(new OptionData(OptionType.BOOLEAN, "value", "Use normal queue mode", true));
    }

    @Override
    public void doCommand(CommandEvent event) {
        Settings settings = bot.getSettingsManager().getSettings(event.getGuild());
        boolean nowSetting = settings.isForceToEndQue();
        boolean newSetting;

        if (event.getArgs().isEmpty()) {
            newSetting = !nowSetting;
        } else if (event.getArgs().equalsIgnoreCase("true")
                || event.getArgs().equalsIgnoreCase("on")
                || event.getArgs().equalsIgnoreCase("enabled")) {
            newSetting = true;
        } else if (event.getArgs().equalsIgnoreCase("false")
                || event.getArgs().equalsIgnoreCase("off")
                || event.getArgs().equalsIgnoreCase("disabled")) {
            newSetting = false;
        } else {
            newSetting = nowSetting;
        }

        settings.setForceToEndQue(newSetting);
        event.replySuccess(buildMessage(newSetting));
    }

    @Override
    public void doCommand(SlashCommandEvent event) {
        Settings settings = bot.getSettingsManager().getSettings(event.getGuild());
        boolean newSetting = event.getOption("value") != null && event.getOption("value").getAsBoolean();
        settings.setForceToEndQue(newSetting);
        event.reply(buildMessage(newSetting)).queue();
    }

    private String buildMessage(boolean enabled) {
        if (enabled) {
            return "Changed queue addition method.\nMode: Normal Queue Mode\nRequested songs are added to the end of the queue.";
        }
        return "Changed queue addition method.\nMode: Fair Queue Mode\nRequested songs are added to the queue in fair order.";
    }
}

/*
 * Copyright 2018-2020 Cosgy Dev
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.jagrosh.jmusicbot.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.commands.DJCommand;
import net.dv8tion.jda.core.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class ForceskipCmd extends DJCommand {
    public ForceskipCmd(Bot bot) {
        super(bot);
        this.name = "forceskip";
        this.help = "現在の曲をスキップします";
        this.aliases = new String[]{"modskip"};
        this.bePlaying = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        Logger log = LoggerFactory.getLogger("Forceskip");
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        User u = event.getJDA().getUserById(handler.getRequester());
        log.info(event.getGuild().getName() + "で" + handler.getPlayer().getPlayingTrack().getInfo().title + "をスキップしました" +
               " (" + (u == null ? "誰かがリクエストしました。" : u.getName() + "さんがリクエストしました。")+")");
        event.reply(event.getClient().getSuccess() + "**" + handler.getPlayer().getPlayingTrack().getInfo().title
                + "** をスキップしました\n(" + (u == null ? "誰かがリクエストしました。" : "**" + u.getName() + "さんがリクエストしました。**") + ")");
        handler.getPlayer().stopTrack();
    }
}

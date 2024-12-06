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
package com.jagrosh.jmusicbot.utils;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class FormatUtil {
    public static String getStacktraceByString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    public static String formatTime(long duration) {
        if (duration == Long.MAX_VALUE)
            return "LIVE";
        long seconds = Math.round(duration / 1000.0);
        long hours = seconds / (60 * 60);
        seconds %= 60 * 60;
        long minutes = seconds / 60;
        seconds %= 60;
        return (hours > 0 ? hours + ":" : "") + (minutes < 10 ? "0" + minutes : minutes) + ":" + (seconds < 10 ? "0" + seconds : seconds);
    }

    public static String formatTime(int duration) {
        if (duration == Integer.MAX_VALUE)
            return "LIVE";
        int seconds = duration;
        int hours = seconds / (60 * 60);
        seconds %= 60 * 60;
        int minutes = seconds / 60;
        seconds %= 60;
        return (hours > 0 ? hours + ":" : "") + (minutes < 10 ? "0" + minutes : minutes) + ":" + (seconds < 10 ? "0" + seconds : seconds);
    }

    public static String progressBar(double percent) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < 12; i++)
            if (i == (int) (percent * 12))
                str.append("\uD83D\uDD18"); // 🔘
            else
                str.append("▬");
        return str.toString();
    }

    public static String volumeIcon(int volume) {
        if (volume == 0)
            return "\uD83D\uDD07"; // 🔇
        if (volume < 30)
            return "\uD83D\uDD08"; // 🔈
        if (volume < 70)
            return "\uD83D\uDD09"; // 🔉
        return "\uD83D\uDD0A";     // 🔊
    }

    public static String listOfTChannels(List<TextChannel> list, String query) {
        StringBuilder out = new StringBuilder(" The query \"" + query + "\" matched in multiple text channels:");
        for (int i = 0; i < 6 && i < list.size(); i++)
            out.append("\n - ").append(list.get(i).getName()).append(" (<#").append(list.get(i).getId()).append(">)");
        if (list.size() > 6)
            out.append("\n**and " + (list.size() - 6) + " more...**");
        return out.toString();
    }

    public static String listOfVChannels(List<VoiceChannel> list, String query) {
        StringBuilder outBuilder = new StringBuilder(" The query \"" + query + "\" matched in multiple voice channels:");
        for (int i = 0; i < 6 && i < list.size(); i++)
            outBuilder.append("\n - ").append(list.get(i).getAsMention()).append(" (ID:").append(list.get(i).getId()).append(")");
        String out = outBuilder.toString();
        if (list.size() > 6)
            out += "\n**and " + (list.size() - 6) + " more...**";
        return out;
    }

    public static String listOfRoles(List<Role> list, String query) {
        StringBuilder outBuilder = new StringBuilder(" The query \"" + query + "\" matched in multiple text channels:");
        for (int i = 0; i < 6 && i < list.size(); i++)
            outBuilder.append("\n - ").append(list.get(i).getName()).append(" (ID:").append(list.get(i).getId()).append(")");
        String out = outBuilder.toString();
        if (list.size() > 6)
            out += "\n**and " + (list.size() - 6) + " more...**";
        return out;
    }

    public static String filter(String input) {
        return input.replace("@everyone", "@\u0435veryone").replace("@here", "@h\u0435re").trim(); // cyrillic letter e
    }
}

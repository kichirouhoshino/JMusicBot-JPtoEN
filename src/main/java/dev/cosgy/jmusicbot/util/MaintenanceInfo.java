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
package dev.cosgy.jmusicbot.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.settings.Settings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author kosugikun
 */
public class MaintenanceInfo {
    private static JsonNode root;
    public String Title;
    public String Content;
    public String StartTime;
    public String EndTime;
    public String LastUpdate;

    public static boolean Verification() throws IOException {
        // Confirm whether to make an announcement
        ObjectMapper mapper = new ObjectMapper();
        root = mapper.readTree(new URL("https://cosgy.dev/botinfo/info.json"));
        return root.get("setting").get(0).get("Announce").asBoolean();
    }

    public static MaintenanceInfo GetInfo() throws IOException {
        Logger log = LoggerFactory.getLogger("GetInfo");

        ObjectMapper mapper = new ObjectMapper();
        root = mapper.readTree(new URL("https://cosgy.dev/botinfo/info.json"));

        MaintenanceInfo Info = new MaintenanceInfo();

        // Check if there is temporary maintenance
        if (root.get("setting").get(0).get("emergency").asBoolean()) {
            Info.Title = root.get("emergencyInfo").get(0).get("Title").asText();
            Info.Content = root.get("emergencyInfo").get(0).get("Content").asText();
            Info.StartTime = root.get("emergencyInfo").get(0).get("StartTime").asText();
            Info.EndTime = root.get("emergencyInfo").get(0).get("StartTime").asText();
        } else {
            int InfoID = root.get("setting").get(0).get("InfoID").asInt();
            Info.Title = root.get("Normal").get(InfoID).get("Title").asText();
            Info.Content = root.get("Normal").get(InfoID).get("Content").asText();
            Info.StartTime = root.get("Normal").get(InfoID).get("StartTime").asText();
            Info.EndTime = root.get("Normal").get(InfoID).get("EndTime").asText();
        }
        Info.LastUpdate = root.get("setting").get(0).get("LastUpdate").asText();
        return Info;

    }

    public static void CommandInfo(CommandEvent event) throws IOException, ParseException {
        Logger log = LoggerFactory.getLogger("AutoInfo");
        ObjectMapper mapper = new ObjectMapper();
        root = mapper.readTree(new URL("https://cosgy.dev/botinfo/info.json"));
        String Start1 = root.get("setting").get(0).get("AutoAnnounceStart").asText();
        String End1 = root.get("setting").get(0).get("AutoAnnounceEnd").asText();
        boolean Announce = root.get("setting").get(0).get("AutoAnnounce").asBoolean();
        int AnnounceID = root.get("setting").get(0).get("AutoAnnounceID").asInt();

        Calendar Now = Calendar.getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        Date Start = sdf.parse(Start1);
        Date End = sdf.parse(End1);
        Date NowTime = Now.getTime();

        boolean StartBoolean = Start.before(NowTime);
        boolean EndBoolean = End.after(NowTime);
        log.info("Confirm start time: " + StartBoolean);
        log.info("Confirm end time: " + EndBoolean);
        log.info("StartTime: " + sdf.format(Start));
        log.info("EndTime: " + sdf.format(End));
        log.info("NowTime: " + sdf.format(NowTime));

        Settings s = event.getClient().getSettingsFor(event.getGuild());
        log.info("Saved AutoAnnounceID: " + s.getAnnounce());
        log.info("Announce Server ID: " + AnnounceID);
        if (Announce && AnnounceID > s.getAnnounce() && StartBoolean && EndBoolean) {
            MaintenanceInfo InfoResult = MaintenanceInfo.GetInfo();


            MessageCreateBuilder builder = new MessageCreateBuilder().addContent("**").addContent(InfoResult.Title).addContent("**");
            EmbedBuilder ebuilder = new EmbedBuilder()
                    .setColor(Color.orange)
                    .setDescription(InfoResult.Content);
            if (!InfoResult.StartTime.equals("")) {
                ebuilder.addField("Start Time:", InfoResult.StartTime, false);
            }
            if (!InfoResult.EndTime.equals("")) {
                ebuilder.addField("End Time:", InfoResult.EndTime, false);
            }
            ebuilder.addField("Last Update:", InfoResult.LastUpdate, false)
                    .addField("Current Time", sdf.format(NowTime), false)
                    .setFooter("※Maintenance periods may change without notice.", null);
            event.getChannel().sendMessage(builder.addEmbeds(ebuilder.build()).build()).complete();
            s.setAnnounce(AnnounceID);
        }
    }

    public static void CommandInfo(SlashCommandEvent event, CommandClient client) throws IOException, ParseException {
        Logger log = LoggerFactory.getLogger("AutoInfo");
        ObjectMapper mapper = new ObjectMapper();
        root = mapper.readTree(new URL("https://cosgy.dev/botinfo/info.json"));
        String Start1 = root.get("setting").get(0).get("AutoAnnounceStart").asText();
        String End1 = root.get("setting").get(0).get("AutoAnnounceEnd").asText();
        boolean Announce = root.get("setting").get(0).get("AutoAnnounce").asBoolean();
        int AnnounceID = root.get("setting").get(0).get("AutoAnnounceID").asInt();

        Calendar Now = Calendar.getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        Date Start = sdf.parse(Start1);
        Date End = sdf.parse(End1);
        Date NowTime = Now.getTime();

        boolean StartBoolean = Start.before(NowTime);
        boolean EndBoolean = End.after(NowTime);
        log.info("Confirm start time: " + StartBoolean);
        log.info("Confirm end time: " + EndBoolean);
        log.info("StartTime: " + sdf.format(Start));
        log.info("EndTime: " + sdf.format(End));
        log.info("NowTime: " + sdf.format(NowTime));

        Settings s = client.getSettingsFor(event.getGuild());
        log.info("Saved AutoAnnounceID: " + s.getAnnounce());
        log.info("Announce Server ID: " + AnnounceID);
        if (Announce && AnnounceID > s.getAnnounce() && StartBoolean && EndBoolean) {
            MaintenanceInfo InfoResult = MaintenanceInfo.GetInfo();

            MessageCreateBuilder builder = new MessageCreateBuilder().addContent("**").addContent(InfoResult.Title).addContent("**");
            EmbedBuilder ebuilder = new EmbedBuilder()
                    .setColor(Color.orange)
                    .setDescription(InfoResult.Content);
            if (!InfoResult.StartTime.equals("")) {
                ebuilder.addField("Start Time:", InfoResult.StartTime, false);
            }
            if (!InfoResult.EndTime.equals("")) {
                ebuilder.addField("End Time:", InfoResult.EndTime, false);
            }
            ebuilder.addField("Last Update:", InfoResult.LastUpdate, false)
                    .addField("Current Time", sdf.format(NowTime), false)
                    .setFooter("※Maintenance periods may change without notice.", null);
            event.getChannel().sendMessage(builder.addEmbeds(ebuilder.build()).build()).complete();
            s.setAnnounce(AnnounceID);
        }
    }
}

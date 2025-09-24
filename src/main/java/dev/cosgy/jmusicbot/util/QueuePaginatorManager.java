/*
 *  Copyright 2025 Cosgy Dev (info@cosgy.dev).
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

import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import dev.cosgy.jmusicbot.settings.RepeatMode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

/**
 * A utility class that generates a music cue page as an embed.
 */
public class QueuePaginatorManager {

    /**
     * Generates an Embed corresponding to the specified page.
     *
     * @param ah          AudioHandler (containing current playback information)
     * @param queue       Queue list for playback
     * @param successIcon Icon displayed upon success (e.g., ✅)
     * @param repeatMode  Repeat Mode (ALL/SINGLE/OFF)
     * @param page        Current page number (starting from 1)
     * @param totalPages  Total number of pages
     * @return MessageEmbed (Embeddable message for Discord)
     */
    public static MessageEmbed createQueuePageEmbed(AudioHandler ah,
                                                    List<QueuedTrack> queue,
                                                    String successIcon,
                                                    RepeatMode repeatMode,
                                                    int page,
                                                    int totalPages) {

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, queue.size());

        StringBuilder description = new StringBuilder();
        long totalDuration = 0;
        for (int i = start; i < end; i++) {
            QueuedTrack track = queue.get(i);
            description.append("`").append(i + 1).append(".` ").append(track.toString()).append("\n");
            totalDuration += track.getTrack().getDuration();
        }

        String repeatEmoji;
        if (repeatMode == RepeatMode.ALL) {
            repeatEmoji = " 🔁";
        } else if (repeatMode == RepeatMode.SINGLE) {
            repeatEmoji = " 🔂";
        } else {
            repeatEmoji = "";
        }

        EmbedBuilder eb = getEmbedBuilder(ah, description);

        eb.setFooter("Page " + page + "/" + totalPages + repeatEmoji
                + " | Total time: " + FormatUtil.formatTime(totalDuration), null);

        return eb.build();
    }

    private static @NotNull EmbedBuilder getEmbedBuilder(AudioHandler ah, StringBuilder description) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.CYAN);
        eb.setDescription(description.toString());

        // Currently playing track
        if (ah.getPlayer().getPlayingTrack() != null) {
            eb.setTitle(
                    (ah.getPlayer().isPaused() ? "⏸" : "▶️") + " Now playing: " +
                            (ah.getPlayer().getPlayingTrack().getInfo().uri.contains("gensokyoradio.net")
                                    ? "Gensokyo Radio"
                                    : ah.getPlayer().getPlayingTrack().getInfo().title)
            );
        } else {
            eb.setTitle("No track is currently playing.");
        }
        return eb;
    }
}


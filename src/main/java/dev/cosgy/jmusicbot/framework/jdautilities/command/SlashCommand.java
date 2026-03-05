/*
 * Copyright 2016-2018 John Grosh (jagrosh) & Kaidan Gustave (TheMonitorLizard)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.cosgy.jmusicbot.framework.jdautilities.command;

import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Base class for slash command execution.
 * Handles permission checks, cooldowns, and command data generation.
 */
public abstract class SlashCommand extends Command
{
    
    protected Map<DiscordLocale, String> nameLocalization = new HashMap<>();

    
    protected Map<DiscordLocale, String> descriptionLocalization = new HashMap<>();

    
    @Deprecated
    protected String requiredRole = null;

    
    protected SlashCommand[] children = new SlashCommand[0];

    
    protected SubcommandGroupData subcommandGroup = null;

    
    protected List<OptionData> options = new ArrayList<>();

    
    protected CommandClient client;

    
    protected abstract void execute(SlashCommandEvent event);

    
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {}

    
    @Override
    protected void execute(CommandEvent event) {}

    
    public final void run(SlashCommandEvent event)
    {
        // Set client reference
        this.client = event.getClient();

        // Check owner permissions
        if(ownerCommand && !(isOwner(event, client)))
        {
            terminate(event, "This command can only be executed by the bot owner.", client);
            return;
        }

        // Check channel availability
        try {
            if(!isAllowed(event.getTextChannel()))
            {
                terminate(event, "This command cannot be used in this channel.", client);
                return;
            }
        } catch (Exception e) {
            // Non-text channels are not checked
        }

        // Check required role
        if(requiredRole!=null)
            if(!(event.getChannelType() == ChannelType.TEXT) || event.getMember().getRoles().stream().noneMatch(r -> r.getName().equalsIgnoreCase(requiredRole)))
            {
                terminate(event, client.getError()+" You need the `"+requiredRole+"` role to use this command.", client);
                return;
            }

        // Check execution conditions
        if(event.getChannelType() != ChannelType.PRIVATE)
        {
            // Check user permissions
            for(Permission p: userPermissions)
            {
                // This is usually not null when executed on the server
                if(event.getMember() == null)
                    continue;

                if(p.isChannel())
                {
                    if(!event.getMember().hasPermission(event.getGuildChannel(), p))
                    {
                        terminate(event, String.format(userMissingPermMessage, client.getError(), p.getName(), "channel"), client);
                        return;
                    }
                }
                else
                {
                    if(!event.getMember().hasPermission(p))
                    {
                        terminate(event, String.format(userMissingPermMessage, client.getError(), p.getName(), "server"), client);
                        return;
                    }
                }
            }

            // Check bot permissions
            for (Permission p : botPermissions) {
                // Permissions not required for operation are excluded from checks
                if (p == Permission.VIEW_CHANNEL || p == Permission.MESSAGE_EMBED_LINKS) {
                    continue;
                }

                // Get guild and bot member info
                Member selfMember = event.getGuild() != null ? event.getGuild().getSelfMember() : null;

                if (p.isChannel()) {
                    // Check channel-level permissions
                    GuildVoiceState voiceState = event.getMember().getVoiceState();
                    AudioChannelUnion channel = voiceState != null ? voiceState.getChannel() : null;

                    if (channel == null || !channel.getType().isAudio()) {
                        terminate(event, client.getError() + " You must join a voice channel to use this command.", client);
                        return;
                    }

                    // Check bot permissions in voice channel
                    if (!selfMember.hasPermission(channel, p)) {
                        terminate(event, String.format(botMissingPermMessage, client.getError(), p.getName(), "voice channel"), client);
                        return;
                    }
                } else {
                    // Check guild-wide permissions
                    if (!selfMember.hasPermission(p)) {
                        terminate(event, String.format(botMissingPermMessage, client.getError(), p.getName(), "サーバー"), client);
                        return;
                    }
                }
            }

            // Check NSFW
            if (nsfwOnly && event.getChannelType() == ChannelType.TEXT && !event.getTextChannel().isNSFW())
            {
                terminate(event, "This command can only be used in NSFW text channels.", client);
                return;
            }
        }
        else if(guildOnly)
        {
            terminate(event, client.getError()+" This command cannot be used in direct messages.", client);
            return;
        }

        // Check cooldown (owner excluded)
        if(cooldown>0 && !(isOwner(event, client)))
        {
            String key = getCooldownKey(event);
            int remaining = client.getRemainingCooldown(key);
            if(remaining>0)
            {
                terminate(event, getCooldownError(event, remaining, client), client);
                return;
            }
            else client.applyCooldown(key, cooldown);
        }

        // Execute main process
        try {
            execute(event);
        } catch(Throwable t) {
            if(client.getListener() != null)
            {
                client.getListener().onSlashCommandException(event, this, t);
                return;
            }
            // Re-throw exception if listener not set
            throw t;
        }

        if(client.getListener() != null)
            client.getListener().onCompletedSlashCommand(event, this);
    }

    
    public boolean isOwner(SlashCommandEvent event, CommandClient client)
    {
        if(event.getUser().getId().equals(client.getOwnerId()))
            return true;
        if(client.getCoOwnerIds()==null)
            return false;
        for(String id : client.getCoOwnerIds())
            if(id.equals(event.getUser().getId()))
                return true;
        return false;
    }

    
    @Deprecated
    @ForRemoval(deadline = "2.0.0")
    public CommandClient getClient()
    {
        return client;
    }

    
    public SubcommandGroupData getSubcommandGroup()
    {
        return subcommandGroup;
    }

    
    public List<OptionData> getOptions()
    {
        return options;
    }

    
    public CommandData buildCommandData()
    {
        // Build command definition
        SlashCommandData data = Commands.slash(getName(), getHelp());
        if (!getOptions().isEmpty())
        {
            data.addOptions(getOptions());
        }

        // Apply name localization
        if (!getNameLocalization().isEmpty())
        {
            // Set localization
            data.setNameLocalizations(getNameLocalization());
        }
        // Apply description localization
        if (!getDescriptionLocalization().isEmpty())
        {
            // Set localization
            data.setDescriptionLocalizations(getDescriptionLocalization());
        }

        // Apply subcommands
        if (children.length != 0)
        {
            // Map to aggregate subcommand groups
            Map<String, SubcommandGroupData> groupData = new HashMap<>();
            for (SlashCommand child : children)
            {
                // Create subcommand definition
                SubcommandData subcommandData = new SubcommandData(child.getName(), child.getHelp());
                // Apply options
                if (!child.getOptions().isEmpty())
                {
                    subcommandData.addOptions(child.getOptions());
                }

                // Apply child command name localization
                if (!child.getNameLocalization().isEmpty())
                {
                    // Set localization
                    subcommandData.setNameLocalizations(child.getNameLocalization());
                }
                // Apply child command description localization
                if (!child.getDescriptionLocalization().isEmpty())
                {
                    // Set localization
                    subcommandData.setDescriptionLocalizations(child.getDescriptionLocalization());
                }

                // If subcommand group exists
                if (child.getSubcommandGroup() != null)
                {
                    SubcommandGroupData group = child.getSubcommandGroup();

                    SubcommandGroupData newData = groupData.getOrDefault(group.getName(), group)
                            .addSubcommands(subcommandData);

                    groupData.put(group.getName(), newData);
                }
                // If group not specified, add directly
                else
                {
                    data.addSubcommands(subcommandData);
                }
            }
            if (!groupData.isEmpty())
                data.addSubcommandGroups(groupData.values());
        }

        if (this.getUserPermissions() == null)
            data.setDefaultPermissions(DefaultMemberPermissions.DISABLED);
        else
            data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(this.getUserPermissions()));

        //data.setGuildOnly(this.guildOnly);

        return data;
    }

    
    public SlashCommand[] getChildren()
    {
        return children;
    }

    private void terminate(SlashCommandEvent event, String message, CommandClient client)
    {
        if(message!=null)
            event.reply(message).setEphemeral(true).queue();
        if(client.getListener()!=null)
            client.getListener().onTerminatedSlashCommand(event, this);
    }

    
    public String getCooldownKey(SlashCommandEvent event)
    {
        switch (cooldownScope)
        {
            case USER:         return cooldownScope.genKey(name,event.getUser().getIdLong());
            case USER_GUILD:   return event.getGuild()!=null ? cooldownScope.genKey(name,event.getUser().getIdLong(),event.getGuild().getIdLong()) :
                    CooldownScope.USER_CHANNEL.genKey(name,event.getUser().getIdLong(), event.getChannel().getIdLong());
            case USER_CHANNEL: return cooldownScope.genKey(name,event.getUser().getIdLong(),event.getChannel().getIdLong());
            case GUILD:        return event.getGuild()!=null ? cooldownScope.genKey(name,event.getGuild().getIdLong()) :
                    CooldownScope.CHANNEL.genKey(name,event.getChannel().getIdLong());
            case CHANNEL:      return cooldownScope.genKey(name,event.getChannel().getIdLong());
            case SHARD:
                event.getJDA().getShardInfo();
                return cooldownScope.genKey(name, event.getJDA().getShardInfo().getShardId());
            case USER_SHARD:
                event.getJDA().getShardInfo();
                return cooldownScope.genKey(name,event.getUser().getIdLong(),event.getJDA().getShardInfo().getShardId());
            case GLOBAL:       return cooldownScope.genKey(name, 0);
            default:           return "";
        }
    }

    
    public String getCooldownError(SlashCommandEvent event, int remaining, CommandClient client)
    {
        if(remaining<=0)
            return null;
        String front = client.getWarning()+" This command is on cooldown for "+remaining+" more seconds";
        if(cooldownScope.equals(CooldownScope.USER))
            return front+"!";
        else if(cooldownScope.equals(CooldownScope.USER_GUILD) && event.getGuild()==null)
            return front+" "+ CooldownScope.USER_CHANNEL.errorSpecification+"!";
        else if(cooldownScope.equals(CooldownScope.GUILD) && event.getGuild()==null)
            return front+" "+ CooldownScope.CHANNEL.errorSpecification+"!";
        else
            return front+" "+cooldownScope.errorSpecification+"!";
    }

    
    public Map<DiscordLocale, String> getNameLocalization() {
        return nameLocalization;
    }

    
    public Map<DiscordLocale, String> getDescriptionLocalization() {
        return descriptionLocalization;
    }
}

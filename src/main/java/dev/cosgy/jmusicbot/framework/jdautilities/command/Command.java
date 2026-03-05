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

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;



/**
 * Base class for command execution.
 * Handles permission checks, cooldowns, and child command resolution.
 */
public abstract class Command extends Interaction
{
    
    protected String name = "null";

    
    protected String help = "Help is not set";

    
    protected Category category = null;

    
    protected String arguments = null;

    
    protected boolean nsfwOnly = false;

    
    protected String requiredRole = null;

    
    protected String[] aliases = new String[0];

    
    protected Command[] children = new Command[0];

    
    protected BiConsumer<CommandEvent, Command> helpBiConsumer = null;

    
    protected boolean usesTopicTags = true;

    
    protected boolean hidden = false;

    
    protected abstract void execute(CommandEvent event);

    
    public final void run(CommandEvent event)
    {
        // Check for subcommands
        if(!event.getArgs().isEmpty())
        {
            String[] parts = Arrays.copyOf(event.getArgs().split("\\s+",2), 2);
            if(helpBiConsumer!=null && parts[0].equalsIgnoreCase(event.getClient().getHelpWord()))
            {
                helpBiConsumer.accept(event, this);
                return;
            }
            for(Command cmd: getChildren())
            {
                if(cmd.isCommandFor(parts[0]))
                {
                    event.setArgs(parts[1]==null ? "" : parts[1]);
                    cmd.run(event);
                    return;
                }
            }
        }

        // Check owner permissions
        if(ownerCommand && !(event.isOwner()))
        {
            terminate(event,null);
            return;
        }

        // Check category conditions
        if(category!=null && !category.test(event))
        {
            terminate(event, category.getFailureResponse());
            return;
        }

        // Check channel availability
        if(event.isFromType(ChannelType.TEXT) && !isAllowed(event.getTextChannel()))
        {
            terminate(event, "This command cannot be used in this channel.");
            return;
        }

        // Check required role
        if(requiredRole!=null)
            if(!event.isFromType(ChannelType.TEXT) || event.getMember().getRoles().stream().noneMatch(r -> r.getName().equalsIgnoreCase(requiredRole)))
            {
                terminate(event, event.getClient().getError()+" This command requires the `"+requiredRole+"` role to use.");
                return;
            }

        // Check execution conditions
        if(!event.isFromType(ChannelType.PRIVATE))
        {
            // Check user permissions
            for(Permission p: userPermissions)
            {
                if(p.isChannel())
                {
                    if(!event.getMember().hasPermission(event.getGuildChannel(), p))
                    {
                        terminate(event, String.format(userMissingPermMessage, event.getClient().getError(), p.getName(), "channel"));
                        return;
                    }
                }
                else
                {
                    if(!event.getMember().hasPermission(p))
                    {
                        terminate(event, String.format(userMissingPermMessage, event.getClient().getError(), p.getName(), "server"));
                        return;
                    }
                }
            }

            // Check bot permissions
            for (Permission p : botPermissions) {
                // Exclude non-essential permissions from checks
                if (p == Permission.VIEW_CHANNEL || p == Permission.MESSAGE_EMBED_LINKS) {
                    continue;
                }

                // Get guild and bot member information
                Member selfMember = event.getGuild() != null ? event.getGuild().getSelfMember() : null;

                if (p.isChannel()) {
                    // Check channel-level permissions
                    GuildVoiceState voiceState = event.getMember().getVoiceState();
                    AudioChannelUnion channel = voiceState != null ? voiceState.getChannel() : null;

                    if (channel == null || !channel.getType().isAudio()) {
                        terminate(event, event.getClient().getError() + " You must be in a voice channel to use this command.");
                        return;
                    }

                    // Check bot permissions in voice channel
                    if (!selfMember.hasPermission(channel, p)) {
                        terminate(event, String.format(botMissingPermMessage, event.getClient().getError(), p.getName(), "voice channel"));
                        return;
                    }
                } else {
                    // Check guild-wide permissions
                    if (!selfMember.hasPermission(p)) {
                        terminate(event, String.format(botMissingPermMessage, event.getClient().getError(), p.getName(), "server"));
                        return;
                    }
                }
            }

            // Check NSFW
            if (nsfwOnly && event.isFromType(ChannelType.TEXT) && !event.getTextChannel().isNSFW())
            {
                terminate(event, "This command can only be used in NSFW text channels.");
                return;
            }
        }
        else if(guildOnly)
        {
            terminate(event, event.getClient().getError()+" This command cannot be used in direct messages.");
            return;
        }

        // Check cooldown (owner is excluded)
        if(cooldown>0 && !(event.isOwner()))
        {
            String key = getCooldownKey(event);
            int remaining = event.getClient().getRemainingCooldown(key);
            if(remaining>0)
            {
                terminate(event, getCooldownError(event, remaining));
                return;
            }
            else event.getClient().applyCooldown(key, cooldown);
        }

        // Execute main process
        try {
            execute(event);
        } catch(Throwable t) {
            if(event.getClient().getListener() != null)
            {
                event.getClient().getListener().onCommandException(event, this, t);
                return;
            }
            // Re-throw exception if no listener is set
            throw t;
        }

        if(event.getClient().getListener() != null)
            event.getClient().getListener().onCompletedCommand(event, this);
    }

    
    public boolean isCommandFor(String input)
    {
        if(name.equalsIgnoreCase(input))
            return true;
        for(String alias: aliases)
            if(alias.equalsIgnoreCase(input))
                return true;
        return false;
    }

    
    public boolean isAllowed(TextChannel channel)
    {
        if(!usesTopicTags)
            return true;
        if(channel==null)
            return true;
        String topic = channel.getTopic();
        if(topic==null || topic.isEmpty())
            return true;
        topic = topic.toLowerCase(Locale.ROOT);
        String lowerName = name.toLowerCase(Locale.ROOT);
        if(topic.contains("{"+lowerName+"}"))
            return true;
        if(topic.contains("{-"+lowerName+"}"))
            return false;
        String lowerCat = category==null ? null : category.getName().toLowerCase(Locale.ROOT);
        if(lowerCat!=null)
        {
            if(topic.contains("{"+lowerCat+"}"))
                return true;
            if(topic.contains("{-"+lowerCat+"}"))
                return false;
        }
        return !topic.contains("{-all}");
    }

    
    public String getName()
    {
        return name;
    }

    
    public String getHelp()
    {
        return help;
    }

    
    public Category getCategory()
    {
        return category;
    }

    
    public String getArguments()
    {
        return arguments;
    }

    
    public boolean isGuildOnly()
    {
        return guildOnly;
    }

    
    public String getRequiredRole()
    {
        return requiredRole;
    }

    
    public String[] getAliases()
    {
        return aliases;
    }

    
    public Command[] getChildren()
    {
        return children;
    }

    
    public boolean isHidden()
    {
        return hidden;
    }

    private void terminate(CommandEvent event, String message)
    {
        if(message!=null)
            event.reply(message);
        if(event.getClient().getListener()!=null)
            event.getClient().getListener().onTerminatedCommand(event, this);
    }

    
    public String getCooldownKey(CommandEvent event)
    {
        switch (cooldownScope)
        {
            case USER:         return cooldownScope.genKey(name,event.getAuthor().getIdLong());
            case USER_GUILD:   return event.getGuild()!=null ? cooldownScope.genKey(name,event.getAuthor().getIdLong(),event.getGuild().getIdLong()) :
                    CooldownScope.USER_CHANNEL.genKey(name,event.getAuthor().getIdLong(), event.getChannel().getIdLong());
            case USER_CHANNEL: return cooldownScope.genKey(name,event.getAuthor().getIdLong(),event.getChannel().getIdLong());
            case GUILD:        return event.getGuild()!=null ? cooldownScope.genKey(name,event.getGuild().getIdLong()) :
                    CooldownScope.CHANNEL.genKey(name,event.getChannel().getIdLong());
            case CHANNEL:      return cooldownScope.genKey(name,event.getChannel().getIdLong());
            case SHARD:        return event.getJDA().getShardInfo()!= JDA.ShardInfo.SINGLE ? cooldownScope.genKey(name, event.getJDA().getShardInfo().getShardId()) :
                    CooldownScope.GLOBAL.genKey(name, 0);
            case USER_SHARD:   return event.getJDA().getShardInfo()!= JDA.ShardInfo.SINGLE ? cooldownScope.genKey(name,event.getAuthor().getIdLong(),event.getJDA().getShardInfo().getShardId()) :
                    CooldownScope.USER.genKey(name, event.getAuthor().getIdLong());
            case GLOBAL:       return cooldownScope.genKey(name, 0);
            default:           return "";
        }
    }

    
    public String getCooldownError(CommandEvent event, int remaining)
    {
        if(remaining<=0)
            return null;
        String front = event.getClient().getWarning()+" This command will be on cooldown for "+remaining+" more seconds";
        if(cooldownScope.equals(CooldownScope.USER))
            return front+"!";
        else if(cooldownScope.equals(CooldownScope.USER_GUILD) && event.getGuild()==null)
            return front+" "+CooldownScope.USER_CHANNEL.errorSpecification+"!";
        else if(cooldownScope.equals(CooldownScope.GUILD) && event.getGuild()==null)
            return front+" "+CooldownScope.CHANNEL.errorSpecification+"!";
        else
            return front+" "+cooldownScope.errorSpecification+"!";
    }

    
    public static class Category
    {
        private final String name;
        private final String failResponse;
        private final Predicate<CommandEvent> predicate;

        
        public Category(String name)
        {
            this.name = name;
            this.failResponse = null;
            this.predicate = null;
        }

        
        public Category(String name, Predicate<CommandEvent> predicate)
        {
            this.name = name;
            this.failResponse = null;
            this.predicate = predicate;
        }

        
        public Category(String name, String failResponse, Predicate<CommandEvent> predicate)
        {
            this.name = name;
            this.failResponse = failResponse;
            this.predicate = predicate;
        }

        
        public String getName()
        {
            return name;
        }

        
        public String getFailureResponse()
        {
            return failResponse;
        }

        
        public boolean test(CommandEvent event)
        {
            return predicate==null || predicate.test(event);
        }

        @Override
        public boolean equals(Object obj)
        {
            if(!(obj instanceof Category))
                return false;
            Category other = (Category)obj;
            return Objects.equals(name, other.name) && Objects.equals(predicate, other.predicate) && Objects.equals(failResponse, other.failResponse);
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 17 * hash + Objects.hashCode(this.name);
            hash = 17 * hash + Objects.hashCode(this.failResponse);
            hash = 17 * hash + Objects.hashCode(this.predicate);
            return hash;
        }
    }
}

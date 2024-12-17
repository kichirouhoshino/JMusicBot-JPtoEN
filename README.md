<img align="right" src="https://i.imgur.com/zrE80HY.png" height="200" width="200" alt="ロゴ">

[![Downloads](https://img.shields.io/github/downloads/Cosgy-Dev/MusicBot-JP-java/total.svg)](https://github.com/Cosgy-Dev/MusicBot-JP-java/releases/latest)
[![Stars](https://img.shields.io/github/stars/Cosgy-Dev/MusicBot-JP-java.svg)](https://github.com/Cosgy-Dev/MusicBot-JP-java/stargazers)
[![Release](https://img.shields.io/github/release/Cosgy-Dev/MusicBot-JP-java.svg)](https://github.com/Cosgy-Dev/MusicBot-JP-java/releases/latest)
[![License](https://img.shields.io/github/license/Cosgy-Dev/MusicBot-JP-java.svg)](https://github.com/Cosgy-Dev/MusicBot-JP-java/blob/master/LICENSE)
[![Discord](https://discordapp.com/api/guilds/497317844191805450/widget.png)](https://discord.gg/RBpkHxf)
![CircleCI](https://img.shields.io/circleci/build/github/Cosgy-Dev/JMusicBot-JP/develop?token=c2ceb77e45cfce45bc8e15161f91d355c54f48b1)
[![CodeFactor](https://www.codefactor.io/repository/github/cosgy-dev/jmusicbot-jp/badge)](https://www.codefactor.io/repository/github/cosgy-dev/jmusicbot-jp)

# JMusicBotJP

MusicBot uses a simple and user-friendly UI. Both setup and launch are easy.
<br><br>This is an English translation of JMusicBotJP.
<br>This fork only aims to translate strings while keeping most of the code intact.
<br>If some fixes are needed, they are very small fixes and are mentioned below.
### Changes
* Translated all strings (and some comments), reference.conf and this README from Japanese to English
* Modified the code for reading config.txt to accept settings from both JP and this fork
* Fix spotify command not working when "valence" value is empty.
* Lower JDA version due to .isVoice() being deprecated.
### ToDo
* Fix weird strings due to translation
### What I can't fix
* The help command is broken due to Discord's 1000-character limit. This requires rewriting the whole HelpCmd file,
which obviously I won't do. For now, running the command tells the user to look at slash commands instead.

[![Setup](http://i.imgur.com/VvXYp5j.png)](https://www.cosgy.dev/2019/09/06/jmusicbot-setup/)

# Bot Features

* Easy setup
* Fast music loading
* Setup with only a Discord Bot token
* Smooth playback with minimal lag
* Unique permission system with DJs
* Simple and easy-to-use UI
* Playback bar displayed in channel topics
* Supports many sites including NicoNico Douga, YouTube, and Soundcloud
* Supports numerous online radios/streams
* Playback of local files
* Playlist support
* Create server and personal playlists

# Setting up

This bot requires Java version 11 or higher.
If Java is not installed, download it from [here](https://www.oracle.com/jp/java/technologies/downloads/).
To start this bot yourself, refer to the [Cosgy Dev Official Page](https://www.cosgy.dev/2019/09/06/jmusicbot-setup/).

# Setup Using Docker

You can start this bot yourself using Docker without having to install Java and other dependencies.
If using Docker, refer to [here](https://hub.docker.com/r/cyberrex/jmusicbot-jp).

# Note

This bot cannot be used as a public bot.
It is recommended for personal or small server use.

# Questions/Suggestions/Bug Reports

**Please read the list of recommended/planned features before suggesting any.**<br>
If you would like to suggest changes to the bot's functionality, recommend customization options, or report bugs, please open an Issue in this repository or join the [Discord server](https://discord.gg/RBpkHxf).
(Note: We do not accept feature requests that require additional API keys or non-music-related features).
<br>If you like this bot, we would appreciate it if you could star this repository.
Additionally, please consider starring the essential dependent libraries for this bot's development: [JDA](https://github.com/DV8FromTheWorld/JDA) and [lavaplayer](https://github.com/lavalink-devs/lavaplayer).

# Example of Commands

![Example](https://i.imgur.com/tevrtKt.png)

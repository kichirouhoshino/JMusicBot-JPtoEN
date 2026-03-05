#　JMusicBot JP Docker container configuration file
#  Maintained by CyberRex (CyberRex0)
#  Edited by kichirouhoshino for JMusicBot-JPtoEN

FROM eclipse-temurin:25-jdk

# DO NOT EDIT UNDER THIS LINE
RUN mkdir -p /opt/jmusicbot

WORKDIR /opt/jmusicbot

RUN \
    echo "JMusicBot-JP Docker Container Builder v1.1\nMaintained by CyberRex (CyberRex0)"; \
    echo "Preconfiguring apt..." && apt-get update > /dev/null; \
    echo "Installing packages..." && apt-get install -y ffmpeg wget curl jq > /dev/null; \
    echo "Downloading latest version of JMusicBot-JP..."; \
    ARCH=$(uname -m); \
    if [ "$ARCH" = "x86_64" ]; then PLATFORM="linux-x86_64"; elif [ "$ARCH" = "aarch64" ]; then PLATFORM="linux-aarch64"; else echo "Unsupported architecture: $ARCH" && exit 1; fi; \
    DOWNLOAD_URL=$(curl -s https://api.github.com/repos/kichirouhoshino/JMusicBot-JPtoEN/releases/latest | jq -r ".assets[] | select(.name | contains(\"-$PLATFORM.jar\")) | .browser_download_url"); \
    if [ -z "$DOWNLOAD_URL" ]; then echo "Failed to find release for $PLATFORM" && exit 1; fi; \
    wget $DOWNLOAD_URL -O /opt/jmusicbot/jmusicbot.jar; \
    echo "cd /opt/jmusicbot && java --enable-native-access=ALL-UNNAMED -Dnogui=true -jar jmusicbot.jar" > /opt/jmusicbot/execute.bash; \
    echo "Build Completed."

CMD ["bash", "/opt/jmusicbot/execute.bash"]

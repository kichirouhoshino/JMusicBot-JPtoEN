package dev.cosgy.jmusicbot.framework.jdautilities.command;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum CooldownScope {
    USER("Per User"),
    USER_GUILD("Per User per Guild"),
    USER_CHANNEL("Per User per Channel"),
    GUILD("Per Guild"),
    CHANNEL("Per Channel"),
    SHARD("Per Shard"),
    USER_SHARD("Per User per Shard"),
    GLOBAL("Global");

    public final String errorSpecification;

    CooldownScope(String errorSpecification) {
        this.errorSpecification = errorSpecification;
    }

    public String genKey(String name, long... ids) {
        String suffix = Arrays.stream(ids)
                .mapToObj(Long::toString)
                .collect(Collectors.joining("_"));
        return name + ':' + suffix;
    }
}

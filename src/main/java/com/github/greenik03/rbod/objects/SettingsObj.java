package com.github.greenik03.rbod.objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class SettingsObj {
    List<String> names;
    boolean reactOnName, reactOnReply, ephemeralUpdates;

    public SettingsObj() {
        reactOnName = false;
        reactOnReply = false;
        ephemeralUpdates = false;
        this.names = new ArrayList<>(List.of(
                "react bot",
                "reactbot",
                "rbod"
        ));
    }

    public List<String> getNames() { return names; }

    public void addName(String name) { names.add(name); }

    public void removeName(String name) { names.remove(name); }

    public boolean containsName(String name) { return names.contains(name); }

    public boolean isReactOnName() { return reactOnName; }

    public void setReactOnName(boolean reactOnName) { this.reactOnName = reactOnName; }

    public boolean isReactOnReply() { return reactOnReply; }

    public void setReactOnReply(boolean reactOnReply) { this.reactOnReply = reactOnReply; }

    @JsonProperty("ephemeralUpdates")
    public boolean areUpdatesEphemeral() { return ephemeralUpdates; }

    public void setEphemeralUpdates(boolean ephemeralUpdates) { this.ephemeralUpdates = ephemeralUpdates; }

    @Override
    public String toString() {
        String RN = reactOnName? "Enabled" : "Disabled";
        String RR = reactOnReply? "Enabled" : "Disabled";
        String EU = ephemeralUpdates? "Enabled" : "Disabled";

        return String.format("""
                    - Name reactions: %s
                    - Reply reactions: %s
                    - Ephemeral update messages: %s
                """, RN, RR, EU);
    }
}

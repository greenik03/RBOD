package com.github.greenik03.rbod.objects;

public class ServerClass {
    String serverId;
    SettingsObj settings;

    public ServerClass(String serverId, SettingsObj settings) {
        this.serverId = serverId;
        this.settings = settings;
    }

    @Override
    public String toString() {
        return String.format("%s:\n%s\n%s", serverId, settings.getNames(),  settings);
    }
}

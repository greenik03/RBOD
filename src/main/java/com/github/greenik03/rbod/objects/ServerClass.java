package com.github.greenik03.rbod.objects;

// Used to be Serializable, only used to show server info in the CLI (for now)
public class ServerClass {
    String serverId;
    SettingsObj settings;

    public ServerClass(String serverId, SettingsObj settings) {
        this.serverId = serverId;
        this.settings = settings;
    }
    //TODO: Change string format
    @Override
    public String toString() {
        return "Server {" +
                "serverId='" + serverId + '\'' +
                ", settings=" + settings +
                '}';
    }
}

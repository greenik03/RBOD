package g03.discord.rbod;

// Used to be Serializable, only used to show server info in the CLI (for now)
public class ServerClass {
    String serverId;
    SettingsObj settings;

    public ServerClass(String serverId, SettingsObj settings) {
        this.serverId = serverId;
        this.settings = settings;
    }

    public String getServerId() {
        return serverId;
    }

    public SettingsObj getSettings() {
        return settings;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public void setSettings(SettingsObj settings) {
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

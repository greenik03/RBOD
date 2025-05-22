import java.io.Serializable;

// Serializable probably does nothing here since this class is only used to show server info in the CLI
public class ServerClass implements Serializable {
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

    @Override
    public String toString() {
        return "ServerClass{" +
                "serverId='" + serverId + '\'' +
                ", settings=" + settings +
                '}';
    }
}

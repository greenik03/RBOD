import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ServerDatabase {
    private static final File databaseFile = new File("databases/guilds.json");
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void init() {
        if (!databaseFile.exists()) {
            try {
                if (databaseFile.createNewFile()) {
                    if (databaseFile.setReadable(true) && databaseFile.setWritable(true))
                        System.out.println("Created new database file.");
                    else
                        throw new RuntimeException("Cannot set read/write permissions on database file.");
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else if (!databaseFile.canRead() || !databaseFile.canWrite()) {
            throw new RuntimeException("Cannot read/write to database file.");
        }
        else {
            System.out.println("Database file already exists.");
        }
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false);
    }

    public static List<ServerClass> getServers() {
        try {
            if (!databaseFile.exists() || databaseFile.length() == 0) {
                return new ArrayList<>();
            }
            JsonNode node = mapper.readTree(databaseFile);
            List<ServerClass> servers = new ArrayList<>();
            node.fields().forEachRemaining(entry -> {
                try {
                    SettingsObj settings = mapper.treeToValue(entry.getValue(), SettingsObj.class);
                    ServerClass server = new ServerClass(entry.getKey(), settings);
                    servers.add(server);
                }
                catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
            return servers;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addServer(String serverId) throws IOException {
        // If server exists in database, ignore
        JsonNode node = (databaseFile.length() == 0)? mapper.createObjectNode() : mapper.readTree(databaseFile);
        if (node.has(serverId)) {
            throw new IOException("Server already exists in database.");
        }
        ((ObjectNode) node).set(serverId, mapper.valueToTree(new SettingsObj()));
        mapper.writeValue(databaseFile, node);
    }

    public static void removeServer(String serverId) throws IOException {
        JsonNode node = mapper.readTree(databaseFile);
        if (!node.has(serverId)) {
            return;
        }
        mapper.writeValue(databaseFile, ((ObjectNode) node).remove(serverId));
    }

    public static SettingsObj getSettings(String serverId) throws IOException {
        if (!databaseFile.exists() || databaseFile.length() == 0) {
            return null;
        }
        JsonNode node = mapper.readTree(databaseFile);
        return !node.has(serverId)? null : mapper.treeToValue(node.get(serverId), SettingsObj.class);
    }

    public static void setSettings(String serverId, SettingsObj settings) throws IOException {
        JsonNode node = (databaseFile.length() == 0)? mapper.createObjectNode() : mapper.readTree(databaseFile);
        ((ObjectNode) node).set(serverId, mapper.valueToTree(settings));
        mapper.writeValue(databaseFile, node);
    }
}

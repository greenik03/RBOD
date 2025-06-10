package g03.discord.rbod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static g03.discord.rbod.RBODMeta.systemMessagePrefix;

public class ServerDatabase {
    private static final File databaseFile = new File("databases/guilds.json"),
        customPhrasesFile = new File("databases/custom_phrases.json");
    private static final ObjectMapper mapper = new ObjectMapper();

    private static void createDatabaseFile(File file) {
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    if (file.setReadable(true) && file.setWritable(true))
                        System.out.println(systemMessagePrefix + "Created new database file: " + file.getName());
                    else
                        throw new RuntimeException("Cannot set read/write permissions on database file: " + file.getName());
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else if (!file.canRead() || !file.canWrite()) {
            throw new RuntimeException("Cannot read/write to database file: " + file.getName());
        }
        else {
            System.out.println(systemMessagePrefix + "Database file " + file.getName() + " already exists.");
        }
    }

    // Create database file if none exists, then configure ObjectMapper
    public static void init() {
        createDatabaseFile(databaseFile);
        createDatabaseFile(customPhrasesFile);
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

    // IOException thrown from if statement is handled in main code
    public static void addServer(String serverId) throws IOException {
        JsonNode node = (databaseFile.length() == 0)? mapper.createObjectNode() : mapper.readTree(databaseFile);
        if (node.has(serverId)) {
            throw new IOException("Server already exists in database.");
        }
        ((ObjectNode) node).set(serverId, mapper.valueToTree(new SettingsObj()));
        mapper.writeValue(databaseFile, node);
    }
    public static void addServerToCustomPhrases(String serverId) throws IOException {
        JsonNode node = (customPhrasesFile.length() == 0)? mapper.createObjectNode() : mapper.readTree(customPhrasesFile);
        if (node.has(serverId)) {
            throw new IOException("Server already exists in database.");
        }
        ((ObjectNode) node).set(serverId, mapper.valueToTree(new ArrayList<String>()));
        mapper.writeValue(customPhrasesFile, node);
    }

    // if the server was never in the database (somehow), then do nothing
    public static void removeServer(String serverId) throws IOException {
        JsonNode node = mapper.readTree(databaseFile);
        if (!node.has(serverId)) {
            return;
        }
        ((ObjectNode) node).remove(serverId);
        mapper.writeValue(databaseFile, node);
    }
    public static void removeServerFromCustomPhrases(String serverId) throws IOException {
        JsonNode node = mapper.readTree(customPhrasesFile);
        if (!node.has(serverId)) {
            return;
        }
        ((ObjectNode) node).remove(serverId);
        mapper.writeValue(customPhrasesFile, node);
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

    public static List<String> getCustomPhrases(String serverId) throws IOException {
        if (!customPhrasesFile.exists() || customPhrasesFile.length() == 0) {
            return null;
        }
        JsonNode node = mapper.readTree(customPhrasesFile);
        return !node.has(serverId)? null : mapper.treeToValue(node.get(serverId), mapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    public static void setCustomPhrases(String serverId, List<String> phrases) throws IOException {
        JsonNode node = (customPhrasesFile.length() == 0)? mapper.createObjectNode() : mapper.readTree(customPhrasesFile);
        ((ObjectNode) node).set(serverId, mapper.valueToTree(phrases));
        mapper.writeValue(customPhrasesFile, node);
    }
}

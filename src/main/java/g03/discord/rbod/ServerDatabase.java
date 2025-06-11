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
    private static JsonNode settingsNode, customPhrasesNode;

    private static void createDatabaseFile(File file) {
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdir();
        }
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
        try {
            settingsNode = mapper.readTree(databaseFile);
            customPhrasesNode = mapper.readTree(customPhrasesFile);
            // Handle empty files by creating ObjectNodes
            if (settingsNode == null || settingsNode.isMissingNode()) {
                settingsNode = mapper.createObjectNode();
            }
            if (customPhrasesNode == null || customPhrasesNode.isMissingNode()) {
                customPhrasesNode = mapper.createObjectNode();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<ServerClass> getServers() {

        if (!databaseFile.exists() || databaseFile.length() == 0) {
            return new ArrayList<>();
        }
        List<ServerClass> servers = new ArrayList<>();
        settingsNode.fields().forEachRemaining(entry -> {
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

    }

    // IOException thrown from if statement is handled in main code
    public static void addServer(String serverId) throws IOException {
        if (settingsNode.has(serverId)) {
            throw new IOException("Server already exists in database.");
        }
        ObjectNode node = ((ObjectNode) settingsNode).set(serverId, mapper.valueToTree(new SettingsObj()));
        mapper.writeValue(databaseFile, node);
    }

    public static void addServerToCustomPhrases(String serverId) throws IOException {
        if (customPhrasesNode.has(serverId)) {
            throw new IOException("Server already exists in database.");
        }
        ObjectNode node = ((ObjectNode) customPhrasesNode).set(serverId, mapper.valueToTree(new ArrayList<String>()));
        mapper.writeValue(customPhrasesFile, node);
    }

    // if the server was never in the database (somehow), then do nothing
    public static void removeServer(String serverId) throws IOException {
        if (!settingsNode.has(serverId)) {
            return;
        }
        ((ObjectNode) settingsNode).remove(serverId);
        mapper.writeValue(databaseFile, settingsNode);
    }
    public static void removeServerFromCustomPhrases(String serverId) throws IOException {
        if (!customPhrasesNode.has(serverId)) {
            return;
        }
        ((ObjectNode) customPhrasesNode).remove(serverId);
        mapper.writeValue(customPhrasesFile, customPhrasesNode);
    }

    public static SettingsObj getSettings(String serverId) throws IOException {
        if (!databaseFile.exists() || databaseFile.length() == 0) {
            return null;
        }
        return !settingsNode.has(serverId)? null : mapper.treeToValue(settingsNode.get(serverId), SettingsObj.class);
    }

    public static void setSettings(String serverId, SettingsObj settings) throws IOException {
        ObjectNode node = ((ObjectNode) settingsNode).set(serverId, mapper.valueToTree(settings));
        mapper.writeValue(databaseFile, node);
    }

    public static List<String> getCustomPhrases(String serverId) throws IOException {
        if (!customPhrasesFile.exists() || customPhrasesFile.length() == 0) {
            return null;
        }
        return !customPhrasesNode.has(serverId)? null : mapper.treeToValue(customPhrasesNode.get(serverId), mapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    public static void setCustomPhrases(String serverId, List<String> phrases) throws IOException {
        ObjectNode node = ((ObjectNode) customPhrasesNode).set(serverId, mapper.valueToTree(phrases));
        mapper.writeValue(customPhrasesFile, node);
    }
}

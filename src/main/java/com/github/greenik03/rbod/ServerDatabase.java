package com.github.greenik03.rbod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.greenik03.rbod.objects.ServerClass;
import com.github.greenik03.rbod.objects.SettingsObj;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.greenik03.rbod.RBODMeta.systemMessagePrefix;

public class ServerDatabase {
    private static final File databaseFile = new File("databases/guilds.json"),
        customPhrasesFile = new File("databases/custom_phrases.json");
    private static final ObjectMapper mapper = new ObjectMapper();
    private static JsonNode settingsNode, customPhrasesNode;
    private static final ReadWriteLock databaseLock = new ReentrantReadWriteLock(),
        customPhrasesLock = new ReentrantReadWriteLock();

    private static void createDatabaseFile(File file) {
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
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
        databaseLock.writeLock().lock();
        databaseLock.readLock().lock();
        customPhrasesLock.writeLock().lock();
        customPhrasesLock.readLock().lock();
        try {
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
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        finally {
            databaseLock.writeLock().unlock();
            customPhrasesLock.writeLock().unlock();
            databaseLock.readLock().unlock();
            customPhrasesLock.readLock().unlock();
        }
    }

    // Remove servers the bot is no longer in from the databases
    public static void cleanupDatabaseFiles(List<Guild> guilds) {
        List<String> actualServers = guilds.stream()
                .map(ISnowflake::getId)
                .toList();
        List<String> databaseServers = getServers().stream()
                .map(ServerClass::getServerId)
                .toList();

        databaseServers.forEach(id -> {
            if (!actualServers.contains(id)) {
                try {
                    removeServer(id);
                    removeServerFromCustomPhrases(id);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static List<ServerClass> getServers() {
        databaseLock.readLock().lock();
        try {
            if (!databaseFile.exists() || databaseFile.length() == 0) {
                return new ArrayList<>();
            }
            List<ServerClass> servers = new ArrayList<>();
            settingsNode.fields().forEachRemaining(entry -> {
                try {
                    SettingsObj settings = mapper.treeToValue(entry.getValue(), SettingsObj.class);
                    ServerClass server = new ServerClass(entry.getKey(), settings);
                    servers.add(server);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
            return servers;
        }
        finally {
            databaseLock.readLock().unlock();
        }
    }

    // IOException thrown from if condition is handled in main code
    public static void addServer(String serverId) throws IOException {
        databaseLock.writeLock().lock();
        try {
            if (settingsNode.has(serverId)) {
                throw new IOException("Server already exists in database.");
            }
            SettingsObj settings = new SettingsObj();
            ObjectNode node = ((ObjectNode) settingsNode).set(serverId, mapper.valueToTree(settings));
            mapper.writeValue(databaseFile, node);
        }
        finally {
            databaseLock.writeLock().unlock();
        }
    }

    public static void addServerToCustomPhrases(String serverId) throws IOException {
        customPhrasesLock.writeLock().lock();
        try {
            if (customPhrasesNode.has(serverId)) {
                throw new IOException("Server already exists in database.");
            }
            List<String> newPhrases = new ArrayList<>();
            ObjectNode node = ((ObjectNode) customPhrasesNode).set(serverId, mapper.valueToTree(newPhrases));
            mapper.writeValue(customPhrasesFile, node);
        }
        finally {
            customPhrasesLock.writeLock().unlock();
        }
    }

    // if the server was never in the database (somehow), then do nothing
    public static void removeServer(String serverId) throws IOException {
        databaseLock.writeLock().lock();
        try {
            if (!settingsNode.has(serverId)) {
                return;
            }
            ((ObjectNode) settingsNode).remove(serverId);
            mapper.writeValue(databaseFile, settingsNode);
        }
        finally {
            databaseLock.writeLock().unlock();
        }
    }
    public static void removeServerFromCustomPhrases(String serverId) throws IOException {
        customPhrasesLock.writeLock().lock();
        try {
            if (!customPhrasesNode.has(serverId)) {
                return;
            }
            ((ObjectNode) customPhrasesNode).remove(serverId);
            mapper.writeValue(customPhrasesFile, customPhrasesNode);
        }
        finally {
            customPhrasesLock.writeLock().unlock();
        }
    }

    public static SettingsObj getSettings(String serverId) throws IOException {
        databaseLock.readLock().lock();
        try {
            if (!databaseFile.exists() || databaseFile.length() == 0) {
                return null;
            }
            return !settingsNode.has(serverId) ? null : mapper.treeToValue(settingsNode.get(serverId), SettingsObj.class);
        }
        finally {
            databaseLock.readLock().unlock();
        }
    }

    public static void setSettings(String serverId, SettingsObj settings) throws IOException {
        databaseLock.writeLock().lock();
        try {
            ObjectNode node = ((ObjectNode) settingsNode).set(serverId, mapper.valueToTree(settings));
            mapper.writeValue(databaseFile, node);
        }
        finally {
            databaseLock.writeLock().unlock();
        }
    }

    public static List<String> getCustomPhrases(String serverId) throws IOException {
        customPhrasesLock.readLock().lock();
        try {
            if (!customPhrasesFile.exists() || customPhrasesFile.length() == 0) {
                return null;
            }
            return !customPhrasesNode.has(serverId) ? null : mapper.treeToValue(customPhrasesNode.get(serverId), mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        }
        finally {
            customPhrasesLock.readLock().unlock();
        }
    }

    public static void setCustomPhrases(String serverId, List<String> phrases) throws IOException {
        customPhrasesLock.writeLock().lock();
        try {
            ObjectNode node = ((ObjectNode) customPhrasesNode).set(serverId, mapper.valueToTree(phrases));
            mapper.writeValue(customPhrasesFile, node);
        }
        finally {
            customPhrasesLock.writeLock().unlock();
        }
    }
}

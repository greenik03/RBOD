package com.github.greenik03.rbod;

import com.github.greenik03.rbod.objects.ServerClass;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class RBODMeta {
    static File phrases = new File("assets/phrases.txt"),
            discordToken = new File("assets/token.txt");
    static EnumSet<GatewayIntent> intents = EnumSet.of(
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.DIRECT_MESSAGES,
            GatewayIntent.MESSAGE_CONTENT
    );
    public static String systemMessagePrefix = "[RBOD]: ";

    // Start of main method
    public static void main(String[] args) {
        // Prevent the bot from starting until it has all the data it needs
        String token = readTokenFromFile();
        if (token == null) {
            System.out.println(systemMessagePrefix + "No token found in "+ discordToken.getPath() +". Please add a token and restart the bot.");
            return;
        }
        if (readPhrasesFromFile().isEmpty()) {
            System.out.println(systemMessagePrefix + phrases.getPath() + " has just been created with placeholder text. Restart the bot after adding phrases to it.");
            return;
        }

        JDA jda = JDABuilder.createLight(token, intents)
                .addEventListeners(new RBOD())
                .setActivity(Activity.customStatus("It's reacting time!"))
                .build();

        CommandListUpdateAction commands = jda.updateCommands();
        //Slash commands are added here. Should be server-only.
        commands.addCommands(
                Commands.slash("toggle", "Toggles the bot's response activations.")
                        .addSubcommands(
                                new SubcommandData("on-name-react", "Toggles the bot's reaction on name mentions.")
                                        .addOption(OptionType.BOOLEAN, "option", "Whether to turn the reaction on or off.", true),
                                new SubcommandData("on-reply-react", "Toggles the bot's reaction on replies.")
                                        .addOption(OptionType.BOOLEAN, "option", "Whether to turn the reaction on or off.", true)
                        )
                        .setIntegrationTypes(IntegrationType.GUILD_INSTALL)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)),
                Commands.slash("names", "Change the names the bot reacts to. (Requires name reactions to be on!)")
                        .addSubcommands(
                                new SubcommandData("add", "Adds a name to the list of names the bot reacts to.")
                                        .addOption(OptionType.STRING, "name", "The name the bot will react to.", true),
                                new SubcommandData("remove", "Removes a name from the list of names the bot reacts to.")
                                        .addOption(OptionType.STRING, "name", "The name to remove from the list.", true),
                                new SubcommandData("list", "Lists all the names the bot reacts to.")
                                        .addOption(OptionType.INTEGER, "page", "The page to show first. (Autocomplete maxes out at 25 due to Discord's limitations)", false, true)
                        )
                        .setIntegrationTypes(IntegrationType.GUILD_INSTALL)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)),
                Commands.slash("help", "Lists all the commands.")
                        .setIntegrationTypes(IntegrationType.GUILD_INSTALL)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)),
                Commands.slash("reset", "Resets the bot's settings and/or custom phrases for this server to default.")
                        .addOption(OptionType.STRING, "data", "The data to reset.", false, true)
                        .setIntegrationTypes(IntegrationType.GUILD_INSTALL)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)),
                Commands.slash("phrases", "Add/Remove server-specific phrases.")
                        .addSubcommands(
                                new SubcommandData("add", "Adds a phrase to the custom phrases list.")
                                        .addOption(OptionType.STRING, "phrase", "The phrase to add for this server.", true),
                                new SubcommandData("remove", "Removes a phrase from the custom phrases list.")
                                        .addOption(OptionType.INTEGER, "index", "The index of the phrase to remove. (Use the list subcommand for reference!)", true),
                                new SubcommandData("list", "Lists all the phrases for this server, with indexes, divided into pages if necessary.")
                                        .addOption(OptionType.INTEGER, "page", "The page to show first. (Autocomplete maxes out at 25 due to Discord's limitations)", false, true)
                        )
                        .setIntegrationTypes(IntegrationType.GUILD_INSTALL)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
        ).queue();
        //TODO: Add context menu command

        ServerDatabase.init();
        try {
            jda.awaitReady();
        }
        catch (Exception e) {
            System.err.println(systemMessagePrefix + "Something went wrong while starting the bot.");
            jda.shutdown();
            throw new RuntimeException(e);
        }
        int settingsAdded = 0, phrasesAdded = 0;
        List<Guild> guilds = jda.getGuilds();
        for (Guild guild : guilds) {
            try {
                ServerDatabase.addServer(guild.getId());
                settingsAdded++;
            }
            catch (IOException e) {
                if (!e.getMessage().equals("Server already exists in database.")) {
                    throw new RuntimeException(e);
                }
            }
            try {
                ServerDatabase.addServerToCustomPhrases(guild.getId());
                phrasesAdded++;
            }
            catch (IOException e) {
                if (!e.getMessage().equals("Server already exists in database.")) {
                    throw new RuntimeException(e);
                }
            }
        }
        System.out.printf("%sBot is now active in %d servers/guilds.\n", systemMessagePrefix, guilds.size());
        System.out.printf("%sSettings database: %d added, %d unchanged\n", systemMessagePrefix, settingsAdded, guilds.size() - settingsAdded);
        System.out.printf("%sCustom phrases database: %d added, %d unchanged\n", systemMessagePrefix, phrasesAdded, guilds.size() - phrasesAdded);

        RBOD.cacheInit(guilds);

        // Control the bot from a CLI
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String[] input = scanner.nextLine()
                    .toLowerCase()
                    .trim()
                    .split("\\s+");
            String baseCommand = input[0];
            if (baseCommand.equals("stop") || baseCommand.equals("exit") || baseCommand.equals("quit")) {
                break;
            }
            switch (baseCommand) {
                case "cache":
                    if (input.length < 2) {
                        System.out.println(systemMessagePrefix + "Available subcommands:");
                        System.out.println("\treset - Resets the cache.");
                        break;
                    }
                    if (input[1].equals("reset")) {
                        RBOD.cacheInit(jda.getGuildCache().asList());
                        break;
                    }
                    break;
                case "help":
                    printCLIUsage();
                    break;
                case "listservers":
                    System.out.println(systemMessagePrefix + "Servers in database:");
                    List<ServerClass> servers = ServerDatabase.getServers();
                    for (ServerClass server : servers) {
                        System.out.println(server.toString());
                    }
                    System.out.println(systemMessagePrefix + "There are " + servers.size() + " servers in the database.");
                    System.out.println(systemMessagePrefix + "Servers in cache:");
                    jda.getGuildCache().forEach(System.out::println);
                    System.out.println(systemMessagePrefix + "There are " + jda.getGuildCache().size() + " servers in the cache.");
                    break;
                case "ping":
                    System.out.println(systemMessagePrefix + "Gateway: " + jda.getGatewayPing() + "ms");
                    jda.getRestPing().queue(time -> System.out.println(systemMessagePrefix + "REST: " + time + "ms"));
                    break;
                default:
                    System.out.println(systemMessagePrefix + "Unknown command. Type 'help' to list all commands.");
            }
        }
        scanner.close();
        jda.shutdown();

    }
    // End of main method

    static void printCLIUsage() {
        System.out.println(systemMessagePrefix + "Here are the commands you can use:");
        System.out.println("\tcache - Interact with the cache using the following subcommands:");
        System.out.println("\t\treset - Resets the cache.");
        System.out.println("\tlistservers - Lists all the servers in the database and cache.");
        System.out.println("\tping - Prints the bot's ping to Discord.");
        System.out.println("\tstop, exit, quit - Stops the bot.");
    }

    // Read text in assets/phrases.txt
    public static List<String> readPhrasesFromFile() {
        if (phrases.exists() && phrases.canRead()) {
            try {
                return Files.readAllLines(phrases.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            createAssetsFile(phrases);
        }
        return new ArrayList<>();
    }

    // Read token from assets/token.txt
    public static String readTokenFromFile() {
        if (discordToken.exists() && discordToken.canRead()) {
            try {
                return Files.readString(discordToken.toPath(), StandardCharsets.UTF_8)
                        .trim();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            createAssetsFile(discordToken);
        }
        return null;
    }

    public static void createAssetsFile(File file) {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try {
            boolean success =
                file.createNewFile() &&
                file.setReadable(true) &&
                file.setWritable(true);
            if (success || file.length() == 0) {
                FileWriter fw = new FileWriter(file);
                fw.append("Placeholder text");
                fw.close();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(systemMessagePrefix + "Created new file: " + file.getPath());
    }

    public static boolean messageContainsExactString(String message, String string) {
        // the \b escape sequence indicates non-alphanumeric characters
        String exactName = "\\b" + Pattern.quote(string) + "\\b";
        Pattern pattern = Pattern.compile(exactName, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(message).find();
    }
}

package g03.discord.rbod;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RBOD extends ListenerAdapter {
    // Initialize variables
    static EnumSet<GatewayIntent> intents = EnumSet.of(
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.MESSAGE_CONTENT
    );
    static File discordToken = new File("assets/token.txt"),
        phrases = new File("assets/phrases.txt");
    static Random rng = new Random();
    static String systemMessagePrefix = "[RBOD]: ";

    // Start of main method
    public static void main(String[] args) {
        String token = readSingleLineFile(discordToken);
        JDA jda = JDABuilder.createLight(token, intents)
                .addEventListeners(new RBOD())
                .setActivity(Activity.customStatus("It's reacting time!"))
                .build();

        CommandListUpdateAction commands = jda.updateCommands();
        //Slash commands are added here. Should be server-only.
        commands.addCommands(Commands.slash("toggle", "Toggles the bot's response activations.")
                .addSubcommands(
                        new SubcommandData("on-name-react", "Toggles the bot's reaction on name mentions.")
                                .addOption(OptionType.BOOLEAN, "option", "Whether to turn the reaction on or off.", true),
                        new SubcommandData("on-reply-react", "Toggles the bot's reaction on replies.")
                                .addOption(OptionType.BOOLEAN, "option", "Whether to turn the reaction on or off.", true)
                ).setIntegrationTypes(IntegrationType.GUILD_INSTALL)
                 .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)),
                Commands.slash("names", "Change the names the bot reacts to. (Requires name reactions to be on!)")
                        .addSubcommands(
                                new SubcommandData("add", "Adds a name to the list of names the bot reacts to.")
                                        .addOption(OptionType.STRING, "name", "The name the bot will react to.", true),
                                new SubcommandData("remove", "Removes a name from the list of names the bot reacts to.")
                                        .addOption(OptionType.STRING, "name", "The name to remove from the list.", true),
                                new SubcommandData("list", "Lists all the names the bot reacts to.")
                        ).setIntegrationTypes(IntegrationType.GUILD_INSTALL)
                         .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)),
                Commands.slash("help", "Lists all the commands.")
                        .setIntegrationTypes(IntegrationType.GUILD_INSTALL)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)),
                Commands.slash("reset", "Resets the bot's settings for this server to default.")
                        .setIntegrationTypes(IntegrationType.GUILD_INSTALL)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
        ).queue();

        ServerDatabase.init();
        try {
            jda.awaitReady();
        }
        catch (Exception e) {
            System.err.println(systemMessagePrefix + "Something went wrong while starting the bot.");
            jda.shutdown();
            throw new RuntimeException(e);
        }
        jda.getGuilds()
                .forEach(guild -> {
                    try {
                        ServerDatabase.addServer(guild.getId());
                    }
                    catch (IOException e) {
                        if (e.getMessage().equals("Server already exists in database.")) {
                            System.out.println(systemMessagePrefix + "Server already exists in database. Skipping " + guild.getId() + " ...");
                        }
                        else {
                            throw new RuntimeException(e);
                        }
                    }
                });

        // Control the bot from a CLI
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String[] input = scanner.nextLine().split("\\s+");
            String baseCommand = input[0];
            if (baseCommand.equalsIgnoreCase("stop") || baseCommand.equalsIgnoreCase("exit") || baseCommand.equalsIgnoreCase("quit")) {
                break;
            }
            switch (baseCommand) {
                case "help":
                    printCLIUsage();
                    break;
                case "listservers":
                    List<ServerClass> servers = ServerDatabase.getServers();
                    for (ServerClass server : servers) {
                        System.out.println(server.toString());
                    }
                    break;
                default:
                    System.out.println(systemMessagePrefix + "Unknown command. Type 'help' to list all commands.");
            }
        }
        scanner.close();
        jda.shutdown();

    }
    // End of main method

    // Read text in assets/phrases.txt, used as variable in main
    static List<String> readPhrasesFromFile() {
        if (phrases.exists() && phrases.canRead()) {
            try {
                return Files.readAllLines(phrases.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            throw new RuntimeException("phrases.txt does not exist or cannot be read. Read assets/guide.md for more info.");
        }
    }

    // Read token from assets/token.txt
    static String readSingleLineFile(File file) {
        if (file.exists() && file.canRead()) {
            try {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8)
                        .trim();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            throw new RuntimeException(file.getAbsolutePath() + " does not exist or cannot be read. Read assets/guide.md for more info.");
        }
    }

    static void printCLIUsage() {
        System.out.println(systemMessagePrefix + "Here are the commands you can use:");
        System.out.println("\tlistservers - Lists all the servers in the database.");
        System.out.println("\tstop, exit, quit - Stops the bot.");
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        System.out.println(systemMessagePrefix + "Bot is online! Type 'help' for a list of commands.");
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.reply("`Commands are only available in servers.`").queue();
            return;
        }
        String ID = Objects.requireNonNull(event.getGuild()).getId();
        SettingsObj settings;
        try {
            settings = ServerDatabase.getSettings(ID);
        } catch (IOException e) {
            settings = null;
        }
        if (settings == null) {
            event.reply("`Settings not found. Creating new settings for server. Run the command again.`").queue();
            try {
                ServerDatabase.addServer(ID);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        String[] command = event.getFullCommandName()
                .toLowerCase()
                .trim()
                .split("\\s+");
        if (command.length >= 2) {
            if (command[0].equalsIgnoreCase("toggle")) {
                try {
                    if (command[1].equalsIgnoreCase("on-name-react")) {
                        boolean reactOnName = Objects.requireNonNull(event.getOption("option")).getAsBoolean();
                        settings.setReactOnName(reactOnName);
                        ServerDatabase.setSettings(ID, settings);
                        event.reply("`Name reactions are " + (reactOnName ? "on" : "off") + "!`").queue();
                    } else if (command[1].equalsIgnoreCase("on-reply-react")) {
                        boolean reactOnReply = Objects.requireNonNull(event.getOption("option")).getAsBoolean();
                        settings.setReactOnReply(reactOnReply);
                        ServerDatabase.setSettings(ID, settings);
                        event.reply("`Reply reactions are " + (reactOnReply ? "on" : "off") + "!`").queue();
                    }
                }
                catch (IOException e) {
                    event.reply("Something went wrong while setting the bot's response activations. \n" + e.getMessage())
                            .setEphemeral(true)
                            .queue();
                }
            }
            else if (command[0].equalsIgnoreCase("names")) {
                try {
                    if (command[1].equalsIgnoreCase("add")) {
                        String name = Objects.requireNonNull(event.getOption("name"))
                                .getAsString()
                                .trim()
                                .toLowerCase();
                        if (name.isBlank()) {
                            event.reply("That's a nothing burger... `(Name cannot be blank)`").queue();
                            return;
                        }
                        if (settings.containsName(name)) {
                            event.reply("`That name is already in the list.`").queue();
                        } else {
                            settings.addName(name);
                            ServerDatabase.setSettings(ID, settings);
                            event.reply("`Added '" + name + "' to the name list. Wowza!`").queue();
                        }
                    } else if (command[1].equalsIgnoreCase("remove")) {
                        String name = Objects.requireNonNull(event.getOption("name"))
                                .getAsString()
                                .trim()
                                .toLowerCase();
                        if (name.isBlank()) {
                            event.reply("That's a nothing burger... `(Name cannot be blank)`").queue();
                            return;
                        }
                        if (!settings.containsName(name)) {
                            event.reply("`That name is not in the list.`").queue();
                        } else {
                            settings.removeName(name);
                            ServerDatabase.setSettings(ID, settings);
                            event.reply("`Removed '" + name + "' from the name list. Fiddlesticks...`").queue();
                        }
                    } else if (command[1].equalsIgnoreCase("list")) {
                        StringBuilder builder = new StringBuilder("```\nThe current name list is:\n");
                        for (String name : settings.getNames()) {
                            builder.append(name).append("\n");
                        }
                        event.reply(builder.append("\n```").toString()).queue();
                    }
                }
                catch (IOException e) {
                    event.reply("Something went wrong while setting the bot's response activations. \n" + e.getMessage())
                            .setEphemeral(true)
                            .queue();
                }
            }
        }
        else {
            if (command[0].equalsIgnoreCase("help")) {
                event.reply("""
                    ```
                    Here are the commands you can use:
                    
                    /toggle [on-name-react | on-reply-react] [true | false] - Toggles the bot's reaction triggers. (Default: false for both)
                    /names [add | remove] name - Adds/Removes a name to the list of names the bot reacts to. (case-insensitive)
                    /names list - Lists all the names the bot reacts to. (Default names: 'react bot', 'reactbot', 'rbod')
                    /reset - Resets the bot's settings for this server to default.
                    /help - This one.
                    ```
                    """).queue();
            }
            else if (command[0].equalsIgnoreCase("reset")) {
                try {
                    ServerDatabase.removeServer(ID);
                    ServerDatabase.addServer(ID);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                event.reply("`Settings reset for this server.`").queue();
            }
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        SelfUser self = event.getJDA().getSelfUser();
        // Do not reply to self
        if (event.getAuthor().equals(self))
            return;

        List<String> phrasesList = readPhrasesFromFile();
        int phraseIndex = rng.nextInt(0, phrasesList.size());
        String reply = phrasesList.get(phraseIndex).trim();

        // Check for blank phrases or escape sequences
        while (reply.isBlank()) {
            phraseIndex = rng.nextInt(0, phrasesList.size());
            reply = phrasesList.get(phraseIndex).trim();
        }
        if (reply.contains("\\n")) {
            reply = reply.replace("\\n", "\n");
        }

        // Respond to direct messages
        if (!event.isFromGuild()) {
            event.getChannel()
                    .sendMessage(reply)
                    .queue();
            return;
        }

        String message = event.getMessage().getContentRaw();
        String mention = String.format("<@%s>", self.getApplicationId());

        // Respond to @ mentions
        if (message.contains(mention)) {
            event.getMessage()
                    .reply(reply)
                    .queue();
            return;
        }

        SettingsObj settings;
        String ID = event.getGuild().getId();
        try {
            settings = ServerDatabase.getSettings(ID);
        } catch (IOException e) {
            settings = null;
        }
        if (settings == null) {
            event.getMessage()
                    .reply("`Settings not found. Creating new settings for server. Use /toggle and try interacting with me again.`")
                    .queue();
            try {
                ServerDatabase.addServer(ID);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        // Respond to name call
        if (settings.isReactOnName()) {
            for (String name : settings.getNames()) {
                if (message.toLowerCase().contains(name)) {
                    event.getMessage()
                            .reply(reply)
                            .queue();
                    break;
                }
            }
            return;
        }
        // Respond to reply
        if (settings.isReactOnReply()) {
            Message referencedMessage = event.getMessage().getReferencedMessage();
            if (referencedMessage == null) {
                return;
            }
            if (event.getMessage().getType().equals(MessageType.INLINE_REPLY) && referencedMessage.getAuthor().equals(self)) {
                event.getMessage()
                        .reply(reply)
                        .queue();
            }
        }
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        System.out.println(systemMessagePrefix + "Bot is shutting down...");
    }

    // Message when bot joins a guild/server
    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        String welcomeMessage = "Hi! I'm ReactBot! @ me to react to your message! Use `/help` to get started.";
        TextChannel channel = event.getGuild().getSystemChannel();
        if (channel == null || !channel.canTalk()) {
            for (TextChannel textChannel : event.getGuild().getTextChannels()) {
                if (textChannel.canTalk()) {
                    channel = textChannel;
                    break;
                }
            }
        }
        if (channel != null) {
            channel.sendMessage(welcomeMessage)
                    .queue();
        }
        try {
            ServerDatabase.addServer(event.getGuild().getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        try {
            ServerDatabase.removeServer(event.getGuild().getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

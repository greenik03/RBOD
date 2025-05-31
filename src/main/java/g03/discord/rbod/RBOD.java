package g03.discord.rbod;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class RBOD extends ListenerAdapter {
    // Initialize variables
    static EnumSet<GatewayIntent> intents = EnumSet.of(
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.MESSAGE_CONTENT
    );
    static File discordToken = new File("assets/token.txt");
    static Random rng = new Random();
    static String systemMessagePrefix = "[RBOD]: ";

    // Start of main method
    public static void main(String[] args) {
        String token = RBODUtils.readTokenFromFile(discordToken);
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
                                        .addOption(OptionType.INTEGER, "index", "The index of the phrase to remove. (Use the list subcommand for reference)", true),
                                new SubcommandData("list", "Lists all the phrases for this server, with indexes.")
                        )
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
                        ServerDatabase.addServerToCustomPhrases(guild.getId());
                    }
                    catch (IOException e) {
                        if (e.getMessage().equals("Server already exists in database.")) {
                            System.out.println(systemMessagePrefix + "Server already exists in settings database. Skipping " + guild.getId() + "...");
                        }
                        else {
                            throw new RuntimeException(e);
                        }
                    }
                    try {
                        ServerDatabase.addServerToCustomPhrases(guild.getId());
                    }
                    catch (IOException e) {
                        if (e.getMessage().equals("Server already exists in database.")) {
                            System.out.println(systemMessagePrefix + "Server already exists in phrases database. Skipping " + guild.getId() + "...");
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
        System.out.println("\tlistservers - Lists all the servers in the database.");
        System.out.println("\tping - Prints the bot's ping to Discord.");
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
        // var command = [command, subcommand, ...]
        switch (command[0]) {
            case "toggle":
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
                break;

            case "names":
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
            break;

            case "phrases":
                try {
                    if (command[1].equalsIgnoreCase("add")) {
                        String phrase = Objects.requireNonNull(event.getOption("phrase")).getAsString();
                        List<String> phrases = ServerDatabase.getCustomPhrases(ID);
                        if (phrases == null || phrases.isEmpty()) {
                            phrases = new ArrayList<>();
                        }
                        if (phrases.contains(phrase)) {
                            event.reply("`That phrase is already in the list.`").queue();
                            return;
                        }
                        phrases.add(phrase);
                        ServerDatabase.setCustomPhrases(ID, phrases);
                        event.reply("`Added the following phrase to the list: '" + phrase + "'`").queue();
                    }
                    else if (command[1].equalsIgnoreCase("remove")) {
                        int index = Objects.requireNonNull(event.getOption("index")).getAsInt() - 1;
                        List<String> phrases = ServerDatabase.getCustomPhrases(ID);
                        if (phrases == null || phrases.isEmpty()) {
                            event.reply("`There are no custom phrases for this server.`").queue();
                            return;
                        }
                        if (index < 0 || index >= phrases.size()) {
                            event.reply("`That index is out of bounds.`").queue();
                        }
                        else {
                            phrases.remove(index);
                            ServerDatabase.setCustomPhrases(ID, phrases);
                            event.reply("`Removed custom phrase " + (index + 1) + " from the list.`").queue();
                        }
                    }
                    else if (command[1].equalsIgnoreCase("list")) {
                        List<String> phrases = ServerDatabase.getCustomPhrases(ID);
                        if (phrases == null || phrases.isEmpty()) {
                            event.reply("`There are no custom phrases for this server.`").queue();
                            return;
                        }
                        StringBuilder builder = new StringBuilder("```\nThe current custom phrases list is:\n");
                        for (int i = 0; i < phrases.size(); i++) {
                            builder.append(i + 1).append(". ").append(phrases.get(i)).append("\n");
                        }
                        builder.append("\n```");
                        event.reply(builder.toString()).queue();
                    }
                }
                catch (IOException e) {
                    event.reply("Something went wrong while setting the server's custom phrases. \n" + e.getMessage())
                            .setEphemeral(true)
                            .queue();
                }
            break;

            case "reset":
                String option = event.getOption("data") == null? "" : Objects.requireNonNull(event.getOption("data")).getAsString();
                if (option.isBlank() || option.equalsIgnoreCase("all")) {
                    try {
                        ServerDatabase.removeServer(ID);
                        ServerDatabase.addServer(ID);
                        ServerDatabase.removeServerFromCustomPhrases(ID);
                        ServerDatabase.addServerToCustomPhrases(ID);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    event.reply("`Settings and phrases reset for this server.`").queue();
                }
                else if (option.equalsIgnoreCase("settings")) {
                    try {
                        ServerDatabase.removeServer(ID);
                        ServerDatabase.addServer(ID);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    event.reply("`Settings reset for this server.`").queue();
                }
                else if (option.equalsIgnoreCase("phrases")) {
                    try {
                        ServerDatabase.removeServerFromCustomPhrases(ID);
                        ServerDatabase.addServerToCustomPhrases(ID);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    event.reply("`Phrases reset for this server.`").queue();
                }
                else {
                    event.reply("`Invalid option. Use 'all' (or just '/reset') for settings and phrases, 'settings', or 'phrases'.`").queue();
                }
                break;

            case "help":
                event.reply("""
                    ```
                    Here are the commands you can use:
                    
                    /toggle [on-name-react | on-reply-react] [true | false] - Toggles the bot's reaction triggers.
                    (Default: false for both)
                    
                    /names [add | remove] [name] - Adds/Removes [name] to/from the list of names the bot reacts to. (case-insensitive)
                    
                    /names list - Lists all the names the bot reacts to.
                    (Default names: 'react bot', 'reactbot', 'rbod')
                    
                    /phrases add [phrase] - Adds [phrase] to the list of custom phrases for this server. (case-sensitive)
                    (Markdown and emoji supported, add '\\n' for a new line)
                    
                    /phrases remove [index] - Removes the custom phrase at the specified index from the list of custom phrases for this server.
                    (You can find the index by typing '/phrases list')
                    
                    /phrases list - Lists all the custom phrases for this server, with indexes.
                    (Default: empty)
                    
                    /reset [data] - Resets the bot's settings and/or custom phrases for this server to default.
                    (Options: 'all' (or just '/reset') for all, 'settings' for only settings, 'phrases' for only custom phrases)
                    
                    /help - This one.
                    ```
                    """).queue();
            break;

            // This will never appear when using the bot, added in just in case
            default:
                event.reply("`Unknown command. Type '/help' for a list of commands.`").queue();
            break;
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equalsIgnoreCase("reset") && event.getFocusedOption().getName().equalsIgnoreCase("data")) {
            String[] options = new String[] {"all", "settings", "phrases"};
            List<Command.Choice> choices = Stream.of(options)
                    .filter(word -> word.startsWith(event.getFocusedOption().getValue()))
                    .map(word -> new Command.Choice(word, word))
                    .toList();
            event.replyChoices(choices).queue();
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        SelfUser self = event.getJDA().getSelfUser();
        // Do not reply to self
        if (event.getAuthor().equals(self)) {
            if (RBODUtils.messageContainsExactName(event.getMessage().getContentRaw(), "edit:")) {
               // this works for some reason
                String message = event.getMessage().getContentRaw();
                event.getMessage().editMessage(message).queue();
            }
            return;
        }

        String ID = event.getGuild().getId();
        List<String> phrasesList = RBODUtils.readPhrasesFromFile();
        List<String> customPhrasesList;
        try {
            customPhrasesList = ServerDatabase.getCustomPhrases(ID);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (customPhrasesList != null && !customPhrasesList.isEmpty()) {
            phrasesList.addAll(customPhrasesList);
        }

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
                if (RBODUtils.messageContainsExactName(message, name)) {
                    event.getMessage()
                            .reply(reply)
                            .queue();
                    return;
                }
            }
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
                // return;
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
            ServerDatabase.addServerToCustomPhrases(event.getGuild().getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        try {
            ServerDatabase.removeServer(event.getGuild().getId());
            ServerDatabase.removeServerFromCustomPhrases(event.getGuild().getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

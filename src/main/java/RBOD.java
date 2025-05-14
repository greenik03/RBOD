import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

//TODO: Implement database for server-specific settings

public class RBOD extends ListenerAdapter {
    // Initialize variables
    static EnumSet<GatewayIntent> intents = EnumSet.of(
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.MESSAGE_CONTENT,
        GatewayIntent.GUILD_MESSAGE_REACTIONS,
        GatewayIntent.DIRECT_MESSAGE_REACTIONS
    );
    static File discordToken = new File("assets/token.txt"),
    phrases = new File("assets/phrases.txt"),
    serverDatabase = new File("assets/servers.db"),
    nameDatabase = new File("assets/names.db");
    static Random rng = new Random();
    static boolean reactOnName = false,
            reactOnReply = false,
            slashNameUsed = false;
    static List<String> names = List.of(
            "react bot",
            "reactbot",
            "rbod"
    );

    // Start of main method
    public static void main(String[] args) {
        String token = readSingleLineFile(discordToken);
        JDA jda = JDABuilder.createLight(token, intents)
                .addEventListeners(new RBOD())
                .setActivity(Activity.customStatus("It's reacting time!"))
                .build();

        CommandListUpdateAction commands = jda.updateCommands();
        //Options for response activations on name mention and on reply through slash commands. Direct mentions and DMs are always on.
        commands.addCommands(Commands.slash("toggle", "Toggles the bot's response activations.")
                .addSubcommands(
                        new SubcommandData("on-name-react", "Toggles the bot's reaction on name mentions.")
                                .addOption(OptionType.BOOLEAN, "option", "Whether to turn the reaction on or off.", true),
                        new SubcommandData("on-reply-react", "Toggles the bot's reaction on replies.")
                                .addOption(OptionType.BOOLEAN, "option", "Whether to turn the reaction on or off.", true)
                ),
                Commands.slash("names", "Change the names the bot reacts to. (Requires name reactions to be on!)")
                        .addSubcommands(
                                new SubcommandData("add", "Adds a name to the list of names the bot reacts to.")
                                        .addOption(OptionType.STRING, "name", "The name the bot will react to.", true),
                                new SubcommandData("remove", "Removes a name from the list of names the bot reacts to.")
                                        .addOption(OptionType.STRING, "name", "The name to remove from the list.", true),
                                new SubcommandData("list", "Lists all the names the bot reacts to.")
                        )
        ).queue();

        try {
                jda.awaitReady();
        }
        catch (Exception e) {
            System.err.println("Something went wrong while starting the bot.");
            jda.shutdown();
            throw new RuntimeException(e);
        }

//        if (!serverDatabase.exists() || !nameDatabase.exists()) {
//            ServerDatabase.init();
//            jda.getGuilds()
//                    .forEach(guild -> ServerDatabase.updateServerTable(guild.getId(), "false", "false", true));
//        }

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
                default:
                    System.out.println("Unknown command. Type 'help' to list all commands.");
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
        System.out.println("Here are the commands you can use:");
        System.out.println("stop, exit, quit - Stops the bot.");
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        System.out.println("Bot is online! Type 'help' for a list of commands.");
        super.onReady(event);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equalsIgnoreCase("toggle")) {
            if (event.getSubcommandName().equalsIgnoreCase("on-name-react")) {
                reactOnName = event.getOption("option").getAsBoolean();
                slashNameUsed = false;
                event.reply("Name reactions are " + (reactOnName ? "on!" : "off!")).queue();
            }
            else if (event.getSubcommandName().equalsIgnoreCase("on-reply-react")) {
                reactOnReply = event.getOption("option").getAsBoolean();
                event.reply("Reply reactions are " + (reactOnReply ? "on!" : "off!")).queue();
            }
        }
        else if (event.getName().equalsIgnoreCase("names")) {
            if (event.getSubcommandName().equalsIgnoreCase("add")) {
                String name = event.getOption("name").getAsString();
                if (names.contains(name)) {
                    event.reply("That name is already in the list.").queue();
                }
                else {
                    names.add(name);
                    slashNameUsed = true;
                    event.reply("Added " + name + " to the name list. Wowza!").queue();
                }
            }
            else if (event.getSubcommandName().equalsIgnoreCase("remove")) {
                String name = event.getOption("name").getAsString();
                if (!names.contains(name)) {
                    event.reply("That name is not in the list.").queue();
                }
                else {
                    names.remove(name);
                    event.reply("Removed " + name + " from the name list. Fiddlesticks...").queue();
                }
            }
            else if (event.getSubcommandName().equalsIgnoreCase("list")) {
                slashNameUsed = true;
                StringBuilder builder = new StringBuilder("The current name list is:\n");
                for (String name : names) {
                    builder.append(name).append("\n");
                }
                event.reply(builder.toString()).queue();
            }
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        List<String> phrasesList = readPhrasesFromFile();
        int phraseIndex = rng.nextInt(0, phrasesList.size());
        String message = event.getMessage().getContentRaw();
        SelfUser self = event.getJDA().getSelfUser();
        String mention = String.format("<@%s>", self.getApplicationId());
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
            if (event.getMessage().getAuthor().equals(self)) return;
            event.getMessage()
                    .reply(reply)
                    .queue();
            return;
        }
        // Respond to @ mentions
        if (message.contains(mention)) {
            event.getMessage()
                    .reply(reply)
                    .queue();
            return;
        }
        // Respond to name call
        if (reactOnName) {
            // React Bot may react to itself if the phrase contains its name
            if (slashNameUsed) {
                slashNameUsed = false;
                return;
            }
            for (String name : names) {
                if (message.toLowerCase().contains(name.toLowerCase())) {
                    event.getMessage()
                            .reply(reply)
                            .queue();
                    break;
                }
            }
            return;
        }
        // Respond to reply
        if (reactOnReply) {
            if (event.getMessage().getType().equals(MessageType.INLINE_REPLY) &&
                    event.getMessage().getReferencedMessage().getAuthor().equals(self)) {
                event.getMessage()
                        .reply(reply)
                        .queue();
            }
        }
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        System.out.println("Bot is shutting down...");
        super.onShutdown(event);
    }

    // Message when bot joins a guild/server
    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        String welcomeMessage = "Hi! I'm ReactBot! @ me to react to your message! Use `/toggle` for additional ways to interact with me.";
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
    }
}

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
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
    appID = new File("assets/applicationID.txt");
    static Random rng = new Random();
    static boolean reactOnName = false,
            reactOnReply = false;
    static List<String> names = List.of(
            "react bot",
            "reactbot",
            "stinky"
    );

    // Start of main method
    public static void main(String[] args) {
        String token = readSingleLineFile(discordToken);
        JDA jda = JDABuilder.createLight(token, intents)
                .addEventListeners(new RBOD())
                .setActivity(Activity.customStatus("It's reacting time!"))
                .build();
//        System.out.printf("%d: %s", phraseIndex, phrasesList.get(phraseIndex));

        CommandListUpdateAction commands = jda.updateCommands();
        //Options for response activations on name mention and on reply through slash commands. Direct mentions are always on.
        commands.addCommands(Commands.slash("toggle", "Toggles the bot's response activations.")
                .addSubcommands(
                        new SubcommandData("reactname", "Toggles the bot's reaction on name mentions.")
                                .addOption(OptionType.BOOLEAN, "on", "Whether to turn the reaction on or off.", true),
                        new SubcommandData("reactreply", "Toggles the bot's reaction on replies.")
                                .addOption(OptionType.BOOLEAN, "on", "Whether to turn the reaction on or off.", true)
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
        // Control the bot from a CLI
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String[] input = scanner.nextLine().split("\\s+");
            String baseCommand = input[0];
            if (baseCommand.equalsIgnoreCase("stop") || baseCommand.equalsIgnoreCase("exit") || baseCommand.equalsIgnoreCase("quit")) {
                break;
            }
            switch (baseCommand) {
                case "toggle":
                    String argument = input[1];
                    if (argument.equalsIgnoreCase("reactOnName")) {
                        reactOnName = !reactOnName;
                        System.out.println("reactOnName is now " + (reactOnName ? "on" : "off"));
                    }
                    else if (argument.equalsIgnoreCase("reactOnReply")) {
                        reactOnReply = !reactOnReply;
                        System.out.println("reactOnReply is now " + (reactOnReply ? "on" : "off"));
                    }
                    else if (argument.equalsIgnoreCase("all")) {
                        reactOnName = !reactOnName;
                        reactOnReply = !reactOnReply;
                        System.out.println("reactOnName is now " + (reactOnName ? "on" : "off"));
                        System.out.println("reactOnReply is now " + (reactOnReply ? "on" : "off"));
                    }
                    break;
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

    // Read token from assets/token.txt or bot's ID from assets/applicationID.txt
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
        System.out.println("toggle all - Toggles both reactOnName and reactOnReply.");
        System.out.println("toggle reactOnName - Toggles reactOnName.");
        System.out.println("toggle reactOnReply - Toggles reactOnReply.");
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        System.out.println("Bot is online! Type 'help' for a list of commands.");
        super.onReady(event);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equalsIgnoreCase("toggle")) {
            if (event.getSubcommandName().equalsIgnoreCase("reactname")) {
                reactOnName = event.getOption("on").getAsBoolean();
                event.reply("Name reactions are " + (reactOnName ? "on!" : "off!")).queue();
            }
            else if (event.getSubcommandName().equalsIgnoreCase("reactreply")) {
                reactOnReply = event.getOption("on").getAsBoolean();
                event.reply("Reply reactions are " + (reactOnReply ? "on!" : "off!")).queue();
            }
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        List<String> phrasesList = readPhrasesFromFile();
        int phraseIndex = rng.nextInt(0, phrasesList.size());
        String message = event.getMessage().getContentRaw();
        String mention = String.format("<@%s>", readSingleLineFile(appID));
        User self = event.getGuild().getSelfMember().getUser();

        if (message.contains(mention)) {
            event.getMessage()
                    .reply(phrasesList.get(phraseIndex))
                    .queue();
            return;
        }
        if (reactOnName) {
            // React Bot may react to itself if phrase contains its name
            for (String name : names) {
                if (message.toLowerCase().contains(name.toLowerCase())) {
                    event.getMessage()
                            .reply(phrasesList.get(phraseIndex))
                            .queue();
                    break;
                }
            }
            return;
        }
        if (reactOnReply) {
            if (event.getMessage().getType().equals(MessageType.INLINE_REPLY) &&
                    event.getMessage().getReferencedMessage().getAuthor().equals(self)) {
                event.getMessage()
                        .reply(phrasesList.get(phraseIndex))
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
        String welcomeMessage = "Hi! I'm ReactBot! @ me for a cool reaction! Use `/toggle` for additional ways to interact with me.";
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

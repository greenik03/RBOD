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
    phrases = new File("assets/phrases.txt");
    static Random rng = new Random();
    static boolean reactOnName = false,
            reactOnReply = false;
    static List<String> names = List.of(
            "react bot",
            "reactbot"
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
                event.reply("Name reactions are " + (reactOnName ? "on!" : "off!")).queue();
            }
            else if (event.getSubcommandName().equalsIgnoreCase("on-reply-react")) {
                reactOnReply = event.getOption("option").getAsBoolean();
                event.reply("Reply reactions are " + (reactOnReply ? "on!" : "off!")).queue();
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

        // Respond to direct messages
        if (!event.isFromGuild()) {
            if (event.getMessage().getAuthor().equals(self)) return;
            event.getMessage()
                    .reply(phrasesList.get(phraseIndex))
                    .queue();
            return;
        }
        // Respond to @ mentions
        if (message.contains(mention)) {
            event.getMessage()
                    .reply(phrasesList.get(phraseIndex))
                    .queue();
            return;
        }
        // Respond to name call
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
        // Respond to reply
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

package g03.discord.rbod;

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
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static g03.discord.rbod.RBODMeta.systemMessagePrefix;

public class RBOD extends ListenerAdapter {
    // Initialize variables
    static Random rng = new Random();
    static HashMap<String, List<String>> phrasesCache = new HashMap<>();
    static HashMap<String, SettingsObj> settingsCache = new HashMap<>();

    public static void cacheInit(List<Guild> guilds) {
        // regular boolean data type doesn't work inside the lambda expression of forEach
        AtomicBoolean exceptionThrown = new AtomicBoolean(false);
        System.out.println(systemMessagePrefix + "Building caches...");
        phrasesCache.put("global", RBODMeta.readPhrasesFromFile());
        guilds.forEach(guild -> {
            try {
                phrasesCache.put(guild.getId(), ServerDatabase.getCustomPhrases(guild.getId()));
                SettingsObj settings = ServerDatabase.getSettings(guild.getId());
                if (settings != null) {
                    settingsCache.put(guild.getId(), settings);
                }
            } catch (IOException e) {
                System.err.println(systemMessagePrefix + "Error while reading databases for guild " + guild.getName() + " (" + guild.getId() + ")");
                System.err.println(e.getMessage());
                exceptionThrown.set(true);
            }
        });
        System.out.println(systemMessagePrefix + (!exceptionThrown.get()? "Caches: done!" : "Caches: error!"));
    }

    public SettingsObj getSettingsFromCache(String ID) {
        SettingsObj settings = settingsCache.get(ID);
        if (settings == null) {
            try {
                settings = ServerDatabase.getSettings(ID);
                if (settings != null) {
                    settingsCache.put(ID, settings);
                }
                else {
                    System.out.println(systemMessagePrefix + "Settings not found. Creating new settings for server with ID " + ID);
                    ServerDatabase.addServer(ID);
                    settings = new SettingsObj();
                    settingsCache.put(ID, settings);
                }
            }
            catch (IOException e) {
                System.err.println(systemMessagePrefix + "Error while reading databases for guild with ID " + ID);
                System.err.println(e.getMessage());
                return null;
            }
        }
        return settings;
    }

    public String getPhraseFromCache(String ID) {
        List<String> phrases = new ArrayList<>(phrasesCache.get("global"));
        if (ID != null && phrasesCache.get(ID) != null && !phrasesCache.get(ID).isEmpty()) {
            List<String> customPhrases = new ArrayList<>(phrasesCache.get(ID));
            if (customPhrases.isEmpty()) {
                try {
                    customPhrases = ServerDatabase.getCustomPhrases(ID);
                }
                catch (IOException e) {
                    System.err.println(systemMessagePrefix + "Error while reading phrases database for guild with ID " + ID);
                    System.err.println(e.getMessage());
                    return null;
                }
                phrasesCache.put(ID, customPhrases);
            }
            if (customPhrases != null && !customPhrases.isEmpty()) {
                phrases.addAll(customPhrases);
            }
        }
        int phraseIndex = rng.nextInt(0, phrases.size());
        String reply = phrases.get(phraseIndex).trim();
        // Check for blank phrases and escape sequences
        while (reply.isBlank()) {
            phraseIndex = rng.nextInt(0, phrases.size());
            reply = phrases.get(phraseIndex).trim();
        }
        if (reply.contains("\\n")) {
            reply = reply.replace("\\n", "\n");
        }
        return reply;
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
            //TODO: Handle this differently, as method in 'try' can return null
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
            //TODO: Add cache methods
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

                //TODO: Add cache methods
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

                //TODO: Add cache methods
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

                //TODO: Add cache methods
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
                    \t- Markdown and emojis work
                    \t- Add '\\n' anywhere to add a line break
                    \t- Add 'edit:' anywhere to make message appear edited
                    
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
            if (RBODMeta.messageContainsExactString(event.getMessage().getContentRaw(), "edit:")/*TODO: && message is not of type Context Command*/) {
               // this works for some reason
                String message = event.getMessage().getContentRaw();
                event.getMessage().editMessage(message).queue();
            }
            return;
        }

        // Respond to direct messages
        if (!event.isFromGuild()) {
            // null is for global phrases only
            String reply = getPhraseFromCache(null);
            event.getChannel()
                    .sendMessage(reply)
                    .queue();
            return;
        }

        if (!event.getChannel().asGuildMessageChannel().canTalk()) {
            return;
        }

        String message = event.getMessage().getContentRaw();
        String mention = String.format("<@%s>", self.getApplicationId());
        String ID = event.getGuild().getId();

        // Respond to @ mentions
        if (message.contains(mention)) {
            String reply = getPhraseFromCache(ID);
            event.getMessage()
                    .reply(reply)
                    .queue();
            return;
        }

        SettingsObj settings = getSettingsFromCache(ID);

        // Respond to name call
        if (settings != null && settings.isReactOnName()) {
            for (String name : settings.getNames()) {
                if (RBODMeta.messageContainsExactString(message, name)) {
                    String reply = getPhraseFromCache(ID);
                    event.getMessage()
                            .reply(reply)
                            .queue();
                    return;
                }
            }
        }
        // Respond to reply
        if (settings != null && settings.isReactOnReply()) {
            Message referencedMessage = event.getMessage().getReferencedMessage();
            if (referencedMessage == null) {
                return;
            }
            if (event.getMessage().getType().equals(MessageType.INLINE_REPLY) && referencedMessage.getAuthor().equals(self)) {
                String reply = getPhraseFromCache(ID);
                event.getMessage()
                        .reply(reply)
                        .queue();
                 return;
            }
        }

        if (settings == null) {
            event.getChannel()
                    .sendMessage("`Settings not found or corrupted. Creating new settings for server. New settings may need to be adjusted.`")
                    .queue();
            try {
                ServerDatabase.addServer(ID);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            settingsCache.put(ID, new SettingsObj());
//            return;
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
        Guild guild = event.getGuild();
        TextChannel channel = guild.getSystemChannel();
        if (channel == null || !channel.canTalk()) {
            for (TextChannel textChannel : guild.getTextChannels()) {
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
            ServerDatabase.addServer(guild.getId());
            ServerDatabase.addServerToCustomPhrases(guild.getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        settingsCache.put(guild.getId(), new SettingsObj());
        System.out.println(systemMessagePrefix + "Joined guild " + guild.getName() + " (" + guild.getId() + ").");
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        Guild guild = event.getGuild();
        try {
            ServerDatabase.removeServer(guild.getId());
            ServerDatabase.removeServerFromCustomPhrases(guild.getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        phrasesCache.remove(guild.getId());
        settingsCache.remove(guild.getId());
        System.out.println(systemMessagePrefix + "Left guild " + guild.getName() + " (" + guild.getId() + ").");
    }
}

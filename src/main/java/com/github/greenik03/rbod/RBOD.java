package com.github.greenik03.rbod;

import com.github.greenik03.rbod.objects.SettingsObj;
import com.github.greenik03.rbod.paginator.NamesPaginatorManager;
import com.github.greenik03.rbod.paginator.PhrasesPaginatorManager;
import com.github.greenik03.rbod.paginator.PaginatorSession;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.github.greenik03.rbod.RBODMeta.systemMessagePrefix;

public class RBOD extends ListenerAdapter {
    // Initialize variables
    static Random rng = new Random();
    static ConcurrentHashMap<String, List<String>> phrasesCache = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, SettingsObj> settingsCache = new ConcurrentHashMap<>();
    PhrasesPaginatorManager phrasesPM = new PhrasesPaginatorManager();
    NamesPaginatorManager namesPM = new NamesPaginatorManager();

    public static void cacheInit(List<Guild> guilds) {
        // regular boolean data type doesn't work inside the lambda expression of forEach
        AtomicBoolean exceptionThrown = new AtomicBoolean(false);
        System.out.println(systemMessagePrefix + "Building caches...");
        phrasesCache.put("global", RBODMeta.readPhrasesFromFile());
        guilds.forEach(guild -> {
            try {
                List<String> phrases = ServerDatabase.getCustomPhrases(guild.getId());
                if (phrases != null && !phrases.isEmpty()) {
                    phrasesCache.put(guild.getId(), phrases);
                }
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
                    return null;
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
            }
            if (customPhrases != null && !customPhrases.isEmpty()) {
                phrasesCache.put(ID, customPhrases);
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
        SettingsObj settings = getSettingsFromCache(ID);
        if (settings == null) {
            event.reply("`Settings not found or are corrupted. New settings have been made. Run the command again.`").queue();
            return;
        }
        boolean ephemeral = settings.areUpdatesEphemeral();
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
                        settingsCache.put(ID, settings);
                        event.reply("`Name reactions are " + (reactOnName ? "on" : "off") + "!`")
                                .setEphemeral(ephemeral)
                                .queue();
                    } else if (command[1].equalsIgnoreCase("on-reply-react")) {
                        boolean reactOnReply = Objects.requireNonNull(event.getOption("option")).getAsBoolean();
                        settings.setReactOnReply(reactOnReply);
                        ServerDatabase.setSettings(ID, settings);
                        settingsCache.put(ID, settings);
                        event.reply("`Reply reactions are " + (reactOnReply ? "on" : "off") + "!`")
                                .setEphemeral(ephemeral)
                                .queue();
                    } else if (command[1].equalsIgnoreCase("ephemeral-updates")) {
                        boolean isEphemeral = Objects.requireNonNull(event.getOption("option")).getAsBoolean();
                        settings.setEphemeralUpdates(isEphemeral);
                        ServerDatabase.setSettings(ID, settings);
                        settingsCache.put(ID, settings);
                        event.reply("`Ephemeral updates are " + (isEphemeral ? "on" : "off") + "!`")
                                .setEphemeral(isEphemeral)
                                .queue();
                    }
                }
                catch (IOException e) {
                    event.reply("Something went wrong while setting the bot's response activations. \n" + e.getMessage())
                        .setEphemeral(ephemeral)
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
                            event.reply("That's a nothing burger... `(Name cannot be blank)`")
                                .setEphemeral(ephemeral)
                                .queue();
                            return;
                        }
                        if (settings.containsName(name)) {
                            event.reply("`That name is already in the list.`")
                                .setEphemeral(ephemeral)
                                .queue();
                        } else {
                            settings.addName(name);
                            ServerDatabase.setSettings(ID, settings);
                            settingsCache.put(ID, settings);
                            if (namesPM.getSession(ID) != null) {
                                namesPM.updateSession(ID, settings.getNames());
                            }
                            event.reply("`Added '" + name + "' to the name list. Wowza!`")
                                .setEphemeral(ephemeral)
                                .queue();
                        }
                    } else if (command[1].equalsIgnoreCase("remove")) {
                        String name = Objects.requireNonNull(event.getOption("name"))
                                .getAsString()
                                .trim()
                                .toLowerCase();
                        if (name.isBlank()) {
                            event.reply("That's a nothing burger... `(Name cannot be blank)`")
                                .setEphemeral(ephemeral)
                                .queue();
                            return;
                        }
                        if (!settings.containsName(name)) {
                            event.reply("`That name is not in the list.`")
                                .setEphemeral(ephemeral)
                                .queue();
                        } else {
                            settings.removeName(name);
                            ServerDatabase.setSettings(ID, settings);
                            settingsCache.put(ID, settings);
                            if (namesPM.getSession(ID) != null) {
                                namesPM.updateSession(ID, settings.getNames());
                            }
                            event.reply("`Removed '" + name + "' from the name list. Fiddlesticks...`")
                                .setEphemeral(ephemeral)
                                .queue();
                        }
                    } else if (command[1].equalsIgnoreCase("list")) {
                        List<String> names = settings.getNames();
                        if (names == null || names.isEmpty()) {
                            event.reply("`There are no names for the bot in this server.`")
                                .setEphemeral(ephemeral)
                                .queue();
                            return;
                        }
                        PaginatorSession session = namesPM.getSession(ID);
                        if (session == null) {
                            session = namesPM.createSession(ID, names);
                        }
                        if (session.getPageCount() == 1) {
                            event.reply(session.getCurrentPage())
                                .setEphemeral(ephemeral)
                                .queue();
                        }
                        else {
                            if (event.getOption("page") != null) {
                                int page = Objects.requireNonNull(event.getOption("page")).getAsInt();
                                if (page <= 0 || page > session.getPageCount()) {
                                    page = 1;
                                }
                                session.setPageNumber(page);
                            }
                            String buttonID = String.format("%s-%s", ID, event.getChannelId());
                            Button btnNext = Button.secondary(buttonID + "-npm-next", "->")
                                    .withDisabled(session.getCurrentPageNumber() == session.getPageCount());
                            Button btnPrev = Button.secondary(buttonID + "-npm-prev", "<-")
                                    .withDisabled(session.getCurrentPageNumber() == 1);

                            event.reply(session.getCurrentPage())
                                .addActionRow(btnPrev, btnNext)
                                .setEphemeral(ephemeral)
                                .queue();
                        }
                    }
                }
                catch (IOException e) {
                    event.reply("Something went wrong while setting the bot's response activations. \n" + e.getMessage())
                        .setEphemeral(ephemeral)
                        .queue();
                }
            break;

            case "phrases":
                try {
                    List<String> phrases = phrasesCache.get(ID);
                    if (phrases == null || phrases.isEmpty()) {
                        phrases = ServerDatabase.getCustomPhrases(ID);
                    }
                    if (command[1].equalsIgnoreCase("add")) {
                        String phrase = Objects.requireNonNull(event.getOption("phrase")).getAsString();
                        if (phrases == null || phrases.isEmpty()) {
                            phrases = new ArrayList<>();
                        }
                        if (phrases.contains(phrase)) {
                            event.reply("`That phrase is already in the list.`")
                                .setEphemeral(ephemeral)
                                .queue();
                            return;
                        }
                        if (phrase.length() > 1900) {
                            event.reply("`That phrase is too long. Max length is 1900 characters.`")
                                .setEphemeral(ephemeral)
                                .queue();
                            return;
                        }
                        phrases.add(phrase);
                        ServerDatabase.setCustomPhrases(ID, phrases);
                        phrasesCache.put(ID, phrases);
                        if (phrasesPM.getSession(ID) != null) {
                            phrasesPM.updateSession(ID, phrases);
                        }
                        event.reply("`Added the phrase to the list with index: " + phrases.size() + "`")
                            .setEphemeral(ephemeral)
                            .queue();
                    }
                    else if (command[1].equalsIgnoreCase("remove")) {
                        int index = Objects.requireNonNull(event.getOption("index")).getAsInt() - 1;
                        if (phrases == null || phrases.isEmpty()) {
                            event.reply("`There are no custom phrases for this server.`")
                                .setEphemeral(ephemeral)
                                .queue();
                            return;
                        }
                        if (index < 0 || index >= phrases.size()) {
                            event.reply("`That index is out of bounds.`")
                                .setEphemeral(ephemeral)
                                .queue();
                        }
                        else {
                            phrases.remove(index);
                            ServerDatabase.setCustomPhrases(ID, phrases);
                            phrasesCache.put(ID, phrases);
                            if (phrasesPM.getSession(ID) != null) {
                                phrasesPM.updateSession(ID, phrases);
                            }
                            event.reply("`Removed custom phrase " + (index + 1) + " from the list.`")
                                .setEphemeral(ephemeral)
                                .queue();
                        }
                    }
                    else if (command[1].equalsIgnoreCase("list")) {
                        if (phrases == null || phrases.isEmpty()) {
                            event.reply("`There are no custom phrases for this server.`")
                                .setEphemeral(ephemeral)
                                .queue();
                            return;
                        }
                        // Initialization of the session variable is done like this in the event a user tries using the page number option
                        // (Autocomplete needs a session to provide options for the user, so it can create one if it doesn't exist)
                        PaginatorSession session = phrasesPM.getSession(ID);
                        if (session == null) {
                            session = phrasesPM.createSession(ID, phrases);
                        }
                        if (session.getPageCount() == 1) {
                            event.reply(session.getCurrentPage())
                                .setEphemeral(ephemeral)
                                .queue();
                        }
                        else {
                            if (event.getOption("page") != null) {
                                int page = Objects.requireNonNull(event.getOption("page")).getAsInt();
                                if (page <= 0 || page > session.getPageCount()) {
                                    page = 1;
                                }
                                session.setPageNumber(page);
                            }
                            String buttonID = String.format("%s-%s", ID, event.getChannelId());
                            Button btnNext = Button.secondary(buttonID + "-ppm-next", "->")
                                    .withDisabled(session.getCurrentPageNumber() == session.getPageCount());
                            Button btnPrev = Button.secondary(buttonID + "-ppm-prev", "<-")
                                    .withDisabled(session.getCurrentPageNumber() == 1);

                            event.reply(session.getCurrentPage())
                                .addActionRow(btnPrev, btnNext)
                                .setEphemeral(ephemeral)
                                .queue();
                        }
                    }
                }
                catch (IOException e) {
                    event.reply("Something went wrong while setting the server's custom phrases. \n" + e.getMessage())
                        .setEphemeral(ephemeral)
                        .queue();
                }
            break;

            case "reset":
                String option = event.getOption("data") == null? "" : Objects.requireNonNull(event.getOption("data")).getAsString();
                if (option.isBlank() || option.equalsIgnoreCase("all")) {
                    try {
                        ServerDatabase.removeServer(ID);
                        ServerDatabase.addServer(ID);
                        settingsCache.put(ID, new SettingsObj());
                        ServerDatabase.removeServerFromCustomPhrases(ID);
                        ServerDatabase.addServerToCustomPhrases(ID);
                        phrasesCache.put(ID, new ArrayList<>());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    event.reply("`Settings and phrases reset for this server.`")
                        .setEphemeral(ephemeral)
                        .queue();
                }
                else if (option.equalsIgnoreCase("settings")) {
                    try {
                        ServerDatabase.removeServer(ID);
                        ServerDatabase.addServer(ID);
                        settingsCache.put(ID, new SettingsObj());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    event.reply("`Settings reset for this server.`")
                        .setEphemeral(ephemeral)
                        .queue();
                }
                else if (option.equalsIgnoreCase("phrases")) {
                    try {
                        ServerDatabase.removeServerFromCustomPhrases(ID);
                        ServerDatabase.addServerToCustomPhrases(ID);
                        phrasesCache.put(ID, new ArrayList<>());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    event.reply("`Phrases reset for this server.`")
                        .setEphemeral(ephemeral)
                        .queue();
                }
                else {
                    event.reply("`Invalid option. Use 'all' (or just '/reset') for settings and phrases, 'settings', or 'phrases'.`")
                        .setEphemeral(ephemeral)
                        .queue();
                }
                break;

            case "help":
                event.reply("""
                    ```
                    Here are the commands you can use:
                    
                    /toggle [on-name-react | on-reply-react] [true | false] - Toggles the bot's reaction triggers.
                    (Default: false for both)
                    
                    /toggle ephemeral-updates [true | false] - Toggle update messages to be ephemeral.
                    (Default: false)
                    
                    /names [add | remove] [name] - Adds/Removes [name] to/from the list of names the bot reacts to. (case-insensitive)
                    
                    /names list [page] - Lists all the names the bot reacts to, sorted alphabetically.
                    The message that is sent will be divided into pages, each page containing 20 names.
                    [page] will default to 1 if not specified or doesn't exist. Autocomplete will max out at 25.
                    (Default names: 'react bot', 'reactbot', 'rbod')
                    
                    /phrases add [phrase] - Adds [phrase] to the list of custom phrases for this server. (case-sensitive)
                    \t- Markdown and emojis work
                    \t- Add '\\n' anywhere to add a line break
                    \t- Add 'edit:' anywhere to make message appear edited
                    
                    /phrases remove [index] - Removes the custom phrase at the specified index from the list of custom phrases for this server.
                    (You can find the index by typing '/phrases list')
                    
                    /phrases list [page] - Lists all the custom phrases for this server, with indexes.
                    The message that is sent will be divided into pages if it surpasses the character limit.
                    [page] will default to 1 if not specified or doesn't exist. Autocomplete will max out at 25.
                    (Default: empty)
                    
                    /view-settings - View the bot's current settings for this server.
                    
                    /reset [data] - Resets the bot's settings and/or custom phrases for this server to default.
                    (Options: 'all' (or just '/reset') for all, 'settings' for only settings, 'phrases' for only custom phrases)
                    
                    /help - This one.
                    ```
                    """)
                        .setEphemeral(ephemeral)
                        .queue();
            break;

            case "view-settings":
                event.reply("```\nHere are this server's settings:\n"+ settings +"```")
                    .setEphemeral(ephemeral)
                    .queue();
            break;

            // This will never appear when using the bot, added in just in case
            default:
                event.reply("`Unknown command. Type '/help' for a list of commands.`")
                    .setEphemeral(ephemeral)
                    .queue();
            break;
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.replyChoices().queue();
            return;
        }
        String ID = Objects.requireNonNull(event.getGuild()).getId();
        if (event.getName().equalsIgnoreCase("reset") && event.getFocusedOption().getName().equalsIgnoreCase("data")) {
            String[] options = new String[]{"all", "settings", "phrases"};
            List<Command.Choice> choices = Stream.of(options)
                    .filter(word -> word.startsWith(event.getFocusedOption().getValue()))
                    .map(word -> new Command.Choice(word, word))
                    .toList();
            event.replyChoices(choices).queue();
            return;
        }
        if (event.getName().equalsIgnoreCase("phrases") && event.getFocusedOption().getName().equalsIgnoreCase("page")) {
            if (phrasesCache.get(ID) != null && !phrasesCache.get(ID).isEmpty()) {
                PaginatorSession session = phrasesPM.getSession(ID);
                if (session == null) {
                    session = phrasesPM.createSession(ID, phrasesCache.get(ID));
                }
                List<Command.Choice> choices = Stream.iterate(1, i -> i + 1)
                        .limit(Math.min(session.getPageCount(), 25))
                        .filter(number -> String.valueOf(number).startsWith(event.getFocusedOption().getValue()))
                        .map(i -> new Command.Choice(String.valueOf(i), i))
                        .toList();
                event.replyChoices(choices).queue();
            }
            return;
        }
        if (event.getName().equalsIgnoreCase("names")) {
            String subcommand = event.getSubcommandName();
            List<String> names = getSettingsFromCache(ID).getNames();
            if (subcommand != null && subcommand.equalsIgnoreCase("remove") && event.getFocusedOption().getName().equalsIgnoreCase("name")) {
                if (names != null && !names.isEmpty()) {
                    List<Command.Choice> choices = names.stream()
                            .filter(name -> name.startsWith(event.getFocusedOption().getValue()))
                            .limit(Math.min(25, names.size()))
                            .map(name -> new Command.Choice(name, name))
                            .toList();
                    event.replyChoices(choices).queue();
                }
                return;
            }
            if (event.getFocusedOption().getName().equalsIgnoreCase("page")) {
                if (names != null && !names.isEmpty()) {
                    PaginatorSession session = namesPM.getSession(ID);
                    if (session == null) {
                        session = namesPM.createSession(ID, names);
                    }
                    List<Command.Choice> choices = Stream.iterate(1, i -> i + 1)
                            .limit(Math.min(session.getPageCount(), 25))
                            .filter(number -> String.valueOf(number).startsWith(event.getFocusedOption().getValue()))
                            .map(i -> new Command.Choice(String.valueOf(i), i))
                            .toList();
                    event.replyChoices(choices).queue();
                }
//                return;
            }
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String serverID = Objects.requireNonNull(event.getGuild()).getId(),
            channelID = event.getChannelId();
        String buttonID = String.format("%s-%s", serverID, channelID);
        if (event.getComponentId().contains("ppm")) {
            PaginatorSession session = phrasesPM.getSession(serverID);
            // sessions are null if not accessed after 20 minutes and cleanup deleted them
            if (session == null) {
                event.editMessage("`Session expired! Use '/phrases list' for a new one.`")
                        .setComponents() // leave no arguments for no buttons
                        .queue();
                return;
            }
            if (event.getComponentId().equals(buttonID + "-ppm-next")) {
                if (session.nextPage()) {
                    Button btnNext = (session.getCurrentPageNumber() == session.getPageCount())?
                            event.getButton().asDisabled() : event.getButton().asEnabled();
                    Button btnPrev = Button.of(ButtonStyle.SECONDARY, buttonID + "-ppm-prev", "<-");
                    btnPrev = (session.getCurrentPageNumber() == 1)? btnPrev.asDisabled() : btnPrev.asEnabled();
                    event.editMessage(session.getCurrentPage()).setActionRow(btnPrev, btnNext).queue();
                }
            }
            else if (event.getComponentId().equals(buttonID + "-ppm-prev")) {
                if (session.previousPage()) {
                    Button btnPrev = (session.getCurrentPageNumber() == 1)?
                            event.getButton().asDisabled() : event.getButton().asEnabled();
                    Button btnNext = Button.of(ButtonStyle.SECONDARY, buttonID + "-ppm-next", "->");
                    btnNext = (session.getCurrentPageNumber() == session.getPageCount())? btnNext.asDisabled() : btnNext.asEnabled();
                    event.editMessage(session.getCurrentPage()).setActionRow(btnPrev, btnNext).queue();
                }
            }
            else {
                System.err.println(systemMessagePrefix + "Unknown button interaction: " + event.getComponentId());
            }
            return;
        }
        if (event.getComponentId().contains("npm")) {
            PaginatorSession session = namesPM.getSession(serverID);
            // sessions are null if not accessed after 20 minutes and cleanup deleted them
            if (session == null) {
                event.editMessage("`Session expired! Use '/names list' for a new one.`")
                        .setComponents() // leave no arguments for no buttons
                        .queue();
                return;
            }
            if (event.getComponentId().equals(buttonID + "-npm-next")) {
                if (session.nextPage()) {
                    Button btnNext = (session.getCurrentPageNumber() == session.getPageCount())?
                            event.getButton().asDisabled() : event.getButton().asEnabled();
                    Button btnPrev = Button.of(ButtonStyle.SECONDARY, buttonID + "-npm-prev", "<-");
                    btnPrev = (session.getCurrentPageNumber() == 1)? btnPrev.asDisabled() : btnPrev.asEnabled();
                    event.editMessage(session.getCurrentPage()).setActionRow(btnPrev, btnNext).queue();
                }
            }
            else if (event.getComponentId().equals(buttonID + "-npm-prev")) {
                if (session.previousPage()) {
                    Button btnPrev = (session.getCurrentPageNumber() == 1)?
                            event.getButton().asDisabled() : event.getButton().asEnabled();
                    Button btnNext = Button.of(ButtonStyle.SECONDARY, buttonID + "-npm-next", "->");
                    btnNext = (session.getCurrentPageNumber() == session.getPageCount())? btnNext.asDisabled() : btnNext.asEnabled();
                    event.editMessage(session.getCurrentPage()).setActionRow(btnPrev, btnNext).queue();
                }
            }
            else {
                System.err.println(systemMessagePrefix + "Unknown button interaction: " + event.getComponentId());
            }
        }
        else {
            System.err.println(systemMessagePrefix + "Unknown button interaction: " + event.getComponentId());
        }
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
        if (event.getName().equalsIgnoreCase("react to this")) {
            String reply = event.isFromGuild()? getPhraseFromCache(Objects.requireNonNull(event.getGuild()).getId()) : getPhraseFromCache(null);
            boolean ephemeral = !event.isFromGuild() || getSettingsFromCache(Objects.requireNonNull(event.getGuild()).getId()).areUpdatesEphemeral();
            event.reply("Ooh, something to react to!")
                    .setEphemeral(ephemeral)
                    .queue();
            event.getTarget()
                    .reply(reply)
                    .queue();
//            return;
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        SelfUser self = event.getJDA().getSelfUser();
        // Do not reply to self
        if (event.getAuthor().equals(self)) {
            if (RBODMeta.messageContainsExactString(event.getMessage().getContentRaw(), "edit:") && !event.getMessage().getType().equals(MessageType.CONTEXT_COMMAND)) {
               // this works for some reason
                String message = event.getMessage().getContentRaw();
                event.getMessage().editMessage(message).queue();
            }
            return;
        }

        // Respond to direct messages
        if (!event.isFromGuild() && event.getChannel().asPrivateChannel().canTalk()) {
            // null is for global phrases only
            String reply = getPhraseFromCache(null);
            event.getChannel()
                    .sendMessage(reply)
                    .queue();
            return;
        }

        // if the bot can't send messages in a channel, stop here
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
        phrasesPM.shutdown();
        namesPM.shutdown();
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
        phrasesPM.removeSession(guild.getId());
        namesPM.removeSession(guild.getId());
        System.out.println(systemMessagePrefix + "Left guild " + guild.getName() + " (" + guild.getId() + ").");
    }
}

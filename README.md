# RBOD (ReactBot on Discord)
RBOD is an implementation of jacksfilms' ReactBot as a Discord bot written in Java.
If you're unfamiliar with jacksfilms or ReactBot, consider watching [this video](https://www.youtube.com/watch?v=f5Ob7U231ns) to learn more, but the idea for the bot is simple: \
It's an RNG-based chatbot where its responses are randomly chosen from a text document.

## How does it work?
RBOD is to be used in servers, mainly. When a user @mentions the bot, it'll reply to the message with one of the phrases included in phrases.txt, located in the "assets" folder.
The user can also directly message the bot to interact with it, but they cannot use the bot's commands this way. \
Interaction by @mention is always enabled, but there are optional triggers that can be enabled for a server:
- `/toggle on-reply-react [true/false]` - toggles the option for the bot to reply to messages from users where they reply to the bot's messages. **Default: `false`**
- `/toggle on-name-react [true/false]` - toggles the option for the bot to reply to messages from users where they refer to the bot by name, without a @mention. While there are names the bot responds to by default when joining a server, they can be changed with the `/names` commands. **Default: `false`**
- `/names list` - Lists the names the bot can currently respond to. By default, i.e. when the bot first joins a server, this list contains `react bot, reactbot, rbod`.
- `/names add [name]` - Adds the name the user inputs to the list of names. (case-insensitive)
- `/names remove [name]` - Removes the name the user inputs from the list of names. (case-insensitive)
- `/reset` - Resets the bot's settings for the server the command was executed from back to default.
- `/help` - Brings up the list of commands.

The slash commands, by default, are enabled for all members of a server. They can be limited to specific users or roles by going into `Server Settings -> Integrations`.

## Setup
This part assumes you have a spare computer you can use as the server for the bot or an actual server you can use to host the bot.
You can also use services like PebbleHost which allow Discord bot hosting, but the steps required to setting up a bot with them may not be 1:1 with the steps below.
1. Clone the repository and open the project in your IDE
   - if you get an error about Gradle not being set up properly, then set the JDK version to 20 (that's the version the bot was developed in) or higher, and make sure the following is included in build.gradle:
   ```gradle
    repositories {
        mavenCentral()
        maven { url = 'https://jitpack.io' }
    }
   dependencies {
        implementation("net.dv8tion:JDA:5.5.1") {
            exclude module: 'opus-java' // required for encoding audio into opus, not needed if audio is already provided in opus encoding
            exclude module: 'tink' // required for encrypting and decrypting audio
        }
        implementation("ch.qos.logback:logback-classic:1.5.18")
        implementation('com.fasterxml.jackson.core:jackson-databind:2.18.3')
    }
    ```
2. Create the necessary files in the "assets" folder ([check guide.md for more info](assets/guide.md))
3. Run RBOD.java

## Contribution
If you find any problems with the program, open an issue and let me know about it. \
If there are any changes you want to make to the main repository, consider opening a pull request. \
You can also fork the repository and make the bot your own, all in compliance with [the MIT License](LICENSE).
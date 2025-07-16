# RBOD (ReactBot on Discord)
RBOD is an implementation of jacksfilms' ReactBot as a Discord bot written in Java.
If you're unfamiliar with jacksfilms or ReactBot, consider watching [this video](https://www.youtube.com/watch?v=f5Ob7U231ns) to learn more, but the idea for the bot is simple: \
It's an RNG-based chatbot where its responses are randomly chosen from a text document.

### Attribution
Original ReactBot program developed by [Cuyoya](https://beacons.ai/cuyoya), El_Mander, and Astrapboy

## How does it work?
RBOD is to be used in servers, mainly. When a user @mentions the bot, it'll reply to the message with one of the phrases included in phrases.txt, located in the "assets" folder.
The user can also directly message the bot to interact with it, but they cannot use the bot's commands this way. \
Interaction by @mention is always enabled, but there are optional triggers that can be enabled for a server:
- `/toggle on-reply-react [true/false]` - toggles the option for the bot to reply to messages from users where they reply to the bot's messages. **Default: `false`**
- `/toggle on-name-react [true/false]` - toggles the option for the bot to reply to messages from users where they refer to the bot by name, without a @mention. While there are names the bot responds to by default when joining a server, they can be changed with the `/names` commands. **Default: `false`**
- `/toggle ephemeral-updates [true/false]` - toggles the option for the bot to make update messages ephemeral, i.e. only make update messages visible to the user who ran the command. **Default: `false`**
- `/names list [page]` - Lists the names the bot can currently respond to. [page] will be set to 1 if not provided, or the given page number doesn't exist. By default, i.e. when the bot first joins a server, this list contains `react bot, reactbot, rbod`.
- `/names add [name]` - Adds [name] the user inputs to the list of names. (case-insensitive)
- `/names remove [name]` - Removes [name] the user inputs from the list of names. (case-insensitive)
- `/phrases add [phrase]` - Adds [phrase] to the list of server-only (custom) phrases for the bot to use. (case-sensitive; [read "Phrase Formatting" in guide.md for more](assets/guide.md))
- `/phrases list [page]` - Lists all custom phrases for the server, with indexes. [page] will be set to 1 if not provided, or the given page number doesn't exist. **Default: empty**
- `/phrases remove [index]` - Removes the phrase at the given index from the list of custom phrases.
- `/view-settings` - View the bot's current settings for this server.
- `/reset [data]` - Resets the bot's settings and/or custom phrases for the server the command was executed from back to default.
- `/help` - Brings up the list of commands.

The slash commands, by default, are enabled for members with `Manage Messages` permission, though they can be overridden for use by specific users or roles by going into `Server Settings -> Integrations`.

## Self-hosting

### Making the bot
1. Open the [Discord Developer Portal](https://discord.com/developers/applications).
2. Create a new application or use a pre-existing one for the bot.
3. In the Installation section, select only `Guild Install` as the installation context. Then, in Default Install Settings, include the scopes `applications.commands` and `bot`, with the permissions being `Embed Links, Read Message History, Send Messages, Send Messages in Threads, View Channels`.
4. In the Bot section, copy the token and keep it somewhere safe (you will need it).
   - If there's no option to copy the token, reset it, and it should give you a new one you can then copy.
5. In the same section, turn on `Message Content Intent`.

### Building the JAR file
1. Clone the repository and open the project in your IDE.
   - if you get an error about Gradle not being set up properly, then set the JDK version to 20 or higher and make sure the following is included in build.gradle:
   ```gradle
   plugins {
      id 'java'
      id 'com.gradleup.shadow' version '8.3.6'
   }
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
   shadowJar {
      // replace with just 'minimize()' if you don't care about the logger
      minimize {
        exclude(dependency("ch.qos.logback:.*:.*"))
      }
      archiveClassifier.set('')
      manifest {
         attributes 'Main-Class': 'RBOD' // this may need to change if the main class is in a package
      }
   }
   jar.enabled = false
   assemble.dependsOn shadowJar
    ```
2. Run the build task in Gradle to create a .jar file. 
3. Make sure your OS has JRE, at least version 20. If you're not sure what version you have, open a terminal/CMD and run `java -version`. If you don't have version 20 or newer, [download it from here](https://adoptium.net/temurin/releases/).
   - If you're running the .jar off of the IDE, then skip this step.
4. Locate the newly created .jar in build/libs. It should be named as 'rbod-*[version]*.jar'.
   - If you're not running the .jar using the IDE, you can take the .jar file and place it in a dedicated folder wherever you like.
5. Create an "assets" folder if there isn't one and create the necessary files in the folder. ([check guide.md for more info](assets/guide.md))
6. Create a folder called "databases" and leave it empty.

Once that's all done, the bot is ready to go! Open a terminal/CMD in the same directory as the .jar and run `java -jar rbod-[version].jar`.

## Contribution
If you find any problems with the program, open an issue and let me know about it. \
If there are any changes you want to make to the main repository, consider opening a pull request. \
You can also fork the repository and make the bot your own, all in compliance with [the MIT License](LICENSE).
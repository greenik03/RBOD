# What assets need to be stored here?
### This folder is empty by default to allow people to personalize and use the code for their own bots.
### However, the bot will not work without these files. Create the following files (case-insensitive, with read permissions and included in your .gitignore if they aren't already) and include the appropriate content:

## token.secret
The token for the bot. A string of random characters that should be the only line in the file. \
**WARNING: Make sure to never expose your token to anyone else in any way!
Doing so allows anyone to control the bot with their own code. 
Should something like this happen, reset the token in your Discord Developer Portal!**

## phrases.txt
A list of phrases the bot will pull from at random. (One non-blank string minimum.) \
Phrase formatting is provided below. \
Blank phrases will be ignored but exclude them for best practice. \
**The content of this file must not contain anything that breaks [Discord's Community Guidelines](https://discord.com/guidelines).**

# Phrase Formatting

- Markdown and emojis work.
    - For Markdown, be mindful of formatting or the message may appear incorrect.
    - Global emojis can be added either by their surrogate pairs or by typing them out like you would on Discord. (ex. `\uD83D\uDC80` or `:skull:`)
    - Custom emojis should be added in the Discord Developer Portal to ensure they work everywhere. Custom phrases can have emojis from the server they're in, as well as global emojis.
- Add `\n` anywhere you want your message to break into a new line. (ex. `Line 1\nLine 2`)
- Add `edit:` anywhere in the message (properly spaced) to make it appear as if it were edited on Discord.
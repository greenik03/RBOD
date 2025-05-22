# What assets need to be stored here?
### This folder is empty by default to allow people to personalize and use the code for their own bots.
### However, the bot will not work without these files. Create the following files (case-insensitive, with read permissions and included in your .gitignore if they aren't already) and include the appropriate content:

## token.txt
___
The token for the bot. A string of random characters that should be the only line of the
text document. \
**WARNING: Make sure to never expose your token to anyone else in any way!
Doing so allows anyone to control the bot with their own code. 
Should something like this happen, reset the token in your Discord Developer Portal!**

## phrases.txt
___
A list of phrases the bot will pull from at random. (One non-blank string minimum.) \
Each phrase must be its own line. Include line breaks in a phrase with '\n'. \
Blank phrases will be ignored but exclude them for best practice. \
**The content of this file must not contain anything that breaks [Discord's Community Guidelines](https://discord.com/guidelines).**
# mc-filter

A highly customizable chat filter for Minecraft.

## Features

Will detect when someone attempts to use a word on the filter list using RegEx to detect attempts to get around filter.  Provides three different modes of censorship: a classic censor which replaces banned words with "****", replacing the attempted message with a predetermined alternative, and preventing the message from being posted at all.  When a filtered word is detected, the server will automatically run any commands chosen by the admins and a message will be sent to all admins online.  All settings customizable both with the yaml file and in game using commands.  There exists two tiers of filtered words each individually customizable in case if the admins believe words of differing severity should be handled differently.

## Command Examples

/filter on/off

/filter staff on/off

/filter mode censor/replace/clear


/filter swears/slurs on/off

/filter swears/slurs mode censor/replace/clear

/filter swears/slurs commands list

/filter swears/slurs commands add [command to be added using "[senderName]" for the player]

/filter swears/slurs commands remove [number of command in list]

/filter swears/slurs replace current

/filter swears/slurs replace edit [what replacement message will be changed to]

/filter swears/slurs staff current

/filter swears/slurs staff edit [what staff message will be changed to]

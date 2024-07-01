# mc-filter

A highly customizable chat filter for Minecraft.

## Features

Will detect when someone attempts to use a word on the filter list using RegEx to detect attempts to get around the filter.  Users are able to create as many custom filter groups as they want using in game commands or directly using the yaml file.  Each group can be completely customized with various options and their own list of words to filter.  Each group has it's own tier of severity so if a message includes words from multiple tiers, only the most severe filter will trigger.

Provides three different modes of censorship: a classic censor which replaces banned words with "****", replacing the attempted message with one from a list of predetermined alternatives, and preventing the message from being posted at all.

When a filtered word is detected, the server will automatically run any commands chosen by the admins.  There are also options to send a message to all admins online and to record the message for later evaluation.  Admins may also write their own notes for a user for future reference.  Using the automatic commands feature, notes can easily be automated as well.

A strike system is an optional feature allowing for automatic punishment if a player exceeds the maximum amount of offenses.  Admins can view a player's accumalated strikes in addition to the recorded messages and admin notes in order to evaluate the user's transgressions.

Customizable chat speed options so that players can only type once every selected amount of seconds if admins do not want players to clutter up the chat.

Spam detection option to prevent players from typing x amount of messages in a selected amount of time.  If triggered, the player's message will not be posted and told to stop spamming.

Bot detection option will detect and prevent anyone (or bot) from send messages faster than humanly possible.

Timeout a player from chatting for a selected amount of time.

## Command Examples


- /filter

    - on/off
    - mode
        - censor/replace/clear
    - admin
        - on/off
        - current
        - edit
    - history
        - on/off     
    - strikes
        - max
    - notes
        - list
        - add
        - remove
            - all
            - history
            - [number of note]
        - strikes
            - clear
              - [nothing]
              - [group]
            - set
            - [nothing]
    - spam
        - on/off
    - bot
        - on/off
    - speed
        - normal/chill/slow/frozen
    - create
    - remove
    - groups
    - timeout
    - partial
        - on/off
    - [filter group] 
    
        - on/off
        - mode
          - censor/replace/clear
          
        - commands
          - list
          - add
          - remove
          
        - replace
          - current
          - edit
          
        - staff
          - current
          - edit
          


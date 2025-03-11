package mctest.chatfiltermc.commands;

import mctest.chatfiltermc.ChatFilterMC;
import mctest.chatfiltermc.util.ClearHistoryPrompt;
import mctest.chatfiltermc.util.ConfigUtil;
import mctest.chatfiltermc.util.ConfirmPrompt;
import mctest.chatfiltermc.util.ConvPrompt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public class Filter implements Listener, CommandExecutor {
    private final ChatFilterMC plugin;
    private ConfigUtil filterConfig;
    private ConfigUtil historyConfig;
    private final ConfigUtil wordListConfig;

    public Filter(ChatFilterMC plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.filterConfig = plugin.getFilterConfig();
        this.historyConfig = new ConfigUtil(plugin, "History.yml");
        this.wordListConfig = plugin.getWordList();
    }

    //TODO
    // new:
    // Fix default messages so they dont show for other options
    // Clean up switch, make commands consistent
    // add strikes on (unless theres a reason why there is none)
    // add more confirmation messages
    // old:
    // Decide if there should be separate files for regex and config stuff
    // Figure out if possible to change final groups command to switch
    // figure out best order of command... currently /filter notes strike [player] clear swears, etc...

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("filter") || !sender.isOp()) return true;

        this.filterConfig = plugin.getFilterConfig();

        try {
            // All group section
            switch (args[0].toLowerCase()) {
                case "on":
                case "off":
                    this.toggleFilter(sender, "all", args[0].toLowerCase());
                    break;
                case "reload":
                    plugin.reloadConfigs();
                    sender.sendMessage(ChatColor.GREEN + "Plugin reloaded!");
                    break;
                case "mode":
                    this.changeMode(sender, "all", args[1].toLowerCase());
                    break;
                case "staff":
                    switch (args[1].toLowerCase()) {
                        case "on":
                        case "off":
                            this.toggleStaffMessage(sender, "all", args[1].toLowerCase());
                            break;
                        case "current":
                            this.currentStaffMessage(sender, "all");
                            break;
                        case "edit":
                            this.editStaffMessage(sender, this.createMessage(args, "all"), "all");
                            break;
                        default:
                            sender.sendMessage(ChatColor.AQUA + "The staff commands are: on/off and edit.");
                            break;
                    }
                    break;
                case "history":
                    this.toggleHistory(sender, "all", args[1].toLowerCase());
                    break;
                case "strikes":
                    if (args.length < 2) return true;
                    switch (args[1].toLowerCase()) {
                        case "max":
                            this.setMaxStrikes(sender, "all", args[2]);
                            break;
                        case "off":
                            this.toggleStrikes(sender, "all");
                            break;
                        default:
                            sender.sendMessage(ChatColor.AQUA + "The options for strikes are max.");
                            break;
                    }
                    break;
                case "notes":
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.AQUA + "Must select an option and use a valid username.");
                        return true;
                    }
                    UUID uuid = null;
                    try {
                        uuid = Objects.requireNonNull(Bukkit.getPlayer(args[2])).getUniqueId();
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.AQUA + "Please enter a valid username.");
                        return true;
                    }

                    this.historyConfig = plugin.getHistoryConfig();
                    if (!this.historyConfig.getConfig().contains(uuid + ".notes")) {
                        this.historyConfig.getConfig().set(uuid + ".notes", new ArrayList<String>());
                    }

                    switch (args[1].toLowerCase()) {
                        case "list":
                            this.listPlayerNotes(sender, args[2].toLowerCase(), uuid);
                            break;
                        case "add":
                            this.addPlayerNotes(sender, uuid, this.createMessage(args, args[1].toLowerCase()));
                            break;
                        case "remove":
                            if (args.length == 3) {
                                sender.sendMessage(ChatColor.AQUA + "Must enter a valid number, all, or history.");
                                return true;
                            }
                            // args[3] options: all, history, an integer
                            this.removePlayerNote(sender, uuid, args[3].toLowerCase());
                            break;
                        case "strikes":
                            // ex: /filter notes strikes [name] clear|set
                            if (args.length >= 4) {
                                switch (args[3].toLowerCase()) {
                                    case "clear":
                                        if (args.length == 4) {
                                            this.clearPlayerStrikes(sender, uuid, "all");
                                        } else {
                                            this.clearPlayerStrikes(sender, uuid, args[4].toLowerCase());
                                        }
                                        break;
                                    case "set":
                                        this.setPlayerStrikes(sender, uuid, args[4].toLowerCase(), args[5]);
                                        break;
                                    default:
                                        sender.sendMessage(ChatColor.AQUA + "The commands for strikes are clear and set.  To show strikes count simply enter the username.");
                                        break;
                                }
                            } else {
                                // if no action specified: print the player's strikes
                                this.listPlayerStrikes(sender, uuid);

                            }
                            break;
                        case "wipe":
                            if (!this.historyConfig.getConfig().contains(uuid.toString())) {
                                sender.sendMessage(ChatColor.AQUA + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + " does not have any history");
                                break;
                            }
                            this.promptHistoryWipe(sender, uuid);
                            break;
                        default:
                            sender.sendMessage(ChatColor.AQUA + "The notes commands are: list, add, remove, and strikes.");
                            break;
                    }
                    break;
                case "spam":
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.AQUA + "The options are: on and off.");
                        break;
                    }
                    this.toggleSpamDetection(sender, args[1].toLowerCase());
                    break;
                case "bot":
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.AQUA + "The options are: on and off.");
                        break;
                    }
                    this.toggleBotDetection(sender, args[1].toLowerCase());
                    break;
                case "speed":
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.AQUA + "The options are: normal, chill, slow, ice..");
                        break;
                    }
                    this.toggleChatSpeed(sender, args[1].toLowerCase());
                    break;
                case "create":
                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.AQUA + "Must enter a name for the new filter group.");
                        break;
                    } else if (args.length > 2) {
                        sender.sendMessage(ChatColor.AQUA + "Must only enter the name of the new filter group.");
                        break;
                    }
                    Set<String> cmds = new HashSet<>(Arrays.asList("on", "off", "mode", "staff", "history", "strikes", "notes", "spam", "bot", "speed", "create", "remove", "groups", "timeout"));
                    if (cmds.contains(args[1].toLowerCase())) {
                        sender.sendMessage(ChatColor.AQUA + "Please choose a name that is not also a filter command.");
                        break;
                    }
                    this.createGroup(sender, args[1].toLowerCase());
                    break;
                case "remove":
                    //TODO Possibly change name to prevent confusion with other remove commands
                    //this.removeGroup(args, sender);
                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.AQUA + "Must enter the name of filter group you want to remove.");
                        break;
                    } else if (args.length > 2) {
                        sender.sendMessage(ChatColor.AQUA + "Must only enter the name of the filter group you want to remove.");
                        break;
                    }
                    this.promptRemove(sender, args[1].toLowerCase());
                    break;
                case "groups":
                    if (args.length != 1) {
                        sender.sendMessage(ChatColor.AQUA + "Not a valid command.");
                        break;
                    }
                    this.listGroups(sender);
                    break;
                case "words":
                    this.listWords(sender, "all");
                    break;
                case "timeout":
                    if (args.length != 4) {
                        sender.sendMessage(ChatColor.AQUA + "In order to time someone out use the format: username [time] m/min/s/sec.");
                        break;
                    }
                    UUID touuid = null;
                    try {
                        touuid = Objects.requireNonNull(Bukkit.getPlayer(args[1])).getUniqueId();
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.AQUA + "Please enter a valid username.");
                        break;
                    }
                    this.callTimeout(sender, touuid, args[2], args[3]);
                    break;
                case "partial":
                    switch (args[1].toLowerCase()) {
                        case "on":
                        case "off":
                            this.togglePartial(sender, "all", args[1].toLowerCase());
                            break;
                        default:
                            sender.sendMessage(ChatColor.AQUA + "Must select on or off.");
                            break;
                    }
                    break;
                case "settings":
                    this.openSettingsGUI(sender, "main");
                    break;
                default:
                    if (plugin.getGroupList().contains(args[0].toLowerCase())) break;
//                    sender.sendMessage(ChatColor.AQUA + "The options for the filter command are: on/off, mode, staff, history, strikes, notes, spam, bot, speed, create, remove, groups, words, timeout, partial, or the name of a filter group.");
//                    sender.sendMessage(ChatColor.AQUA + "Plugin created by Mithraea and DeathsValentine");
                    break;
            }
            // Group specific section
            if (plugin.getGroupList().contains(args[0].toLowerCase())) {
                try {
                    //TODO Create variables out of args for clearer readability
                    switch (args[1].toLowerCase()) {
                        case "on":
                        case "off":
                            this.toggleFilter(sender, args[0].toLowerCase(), args[1].toLowerCase());
                            break;
                        case "mode":
                            this.changeMode(sender, args[0].toLowerCase(), args[2].toLowerCase());
                            break;
                        case "commands":
                            switch (args[2].toLowerCase()) {
                                case "list":
                                    this.listCommands(sender, args[0].toLowerCase());
                                    break;
                                case "add":
                                    this.addCommands(sender, args[0].toLowerCase(), this.createMessage(args, args[0].toLowerCase()));
                                    break;
                                case "remove":
                                    this.removeCommands(sender, args[0].toLowerCase(), args[3]);
                                    break;
                                default:
                                    sender.sendMessage(ChatColor.AQUA + "The options for commands are: list, add, and remove.");
                                    break;
                            }
                            break;
                        case "staff":
                            switch (args[2].toLowerCase()) {
                                case "on":
                                case "off":
                                    this.toggleStaffMessage(sender, args[0].toLowerCase(), args[2].toLowerCase());
                                    break;
                                case "current":
                                    this.currentStaffMessage(sender, args[0].toLowerCase());
                                    break;
                                case "edit":
                                    this.editStaffMessage(sender, this.createMessage(args, args[0].toLowerCase()), args[0].toLowerCase());
                                    break;
                                default:
                                    sender.sendMessage(ChatColor.AQUA + "The options for staff are: current, edit, and on/off.");
                                    break;
                            }
                            break;
                        case "replace":
                            switch (args[2].toLowerCase()) {
                                case "list":
                                    this.listReplacements(sender, args[0].toLowerCase());
                                    break;
                                case "add":
                                    this.addReplacements(sender, args[0].toLowerCase(), this.createMessage(args, args[0].toLowerCase()));
                                    break;
                                case "remove":
                                    this.removeReplacements(sender, args[0].toLowerCase(), args[3]);
                                    break;
                                default:
                                    sender.sendMessage(ChatColor.AQUA + "The options for replace are: list, add, and remove.");
                                    break;
                            }
                            break;
                        case "strikes":
                            switch (args[2].toLowerCase()) {
                                case "max":
                                    this.setMaxStrikes(sender, args[0].toLowerCase(), args[3]);
                                    break;
                                case "off":
                                    this.toggleStrikes(sender, args[0].toLowerCase());
                                    break;
                                case "commands":
                                    switch (args[3].toLowerCase()) {
                                        case "list":
                                            this.listStrikeCommands(sender, args[0].toLowerCase());
                                            break;
                                        case "add":
                                            this.addStrikeCommands(sender, args[0].toLowerCase(), this.createMessage(args, args[0].toLowerCase()));
                                            break;
                                        case "remove":
                                            this.removeStrikeCommands(sender, args[0].toLowerCase(), args[4]);
                                            break;
                                        default:
                                            sender.sendMessage(ChatColor.AQUA + "The options for strike commands are: list, add, and remove.");
                                            break;
                                    }
                                    break;
                                default:
                                    sender.sendMessage(ChatColor.AQUA + "The commands for strikes are max, off, and commands.");
                                    break;
                            }
                            break;
                        case "history":
                            this.toggleHistory(sender, args[0].toLowerCase(), args[2].toLowerCase());
                            break;
                        case "add":
                            if (args.length != 3) {
                                sender.sendMessage(ChatColor.AQUA + "Must enter just the word to add.");
                                break;
                            }
                            this.addWord(sender, args[0].toLowerCase(), args[2].toLowerCase());
                            break;
                        case "remove":
                            if (args.length != 3) {
                                sender.sendMessage(ChatColor.AQUA + "Must enter just the word to remove.");
                                break;
                            }
                            this.removeWord(sender, args[0].toLowerCase(), args[2].toLowerCase());
                            break;
                        case "words":
                            this.listWords(sender, args[0].toLowerCase());
                            break;
                        case "partial":
                            this.togglePartial(sender, args[0].toLowerCase(), args[2].toLowerCase());
                            break;
                        default:
                            sender.sendMessage(ChatColor.AQUA + "The possible options are: on/off, mode, replace, commands, staff, strikes, history, add, remove, words, and partial.");
                            break;

                    }
                } catch (Exception e) {
                    return true;
                }
            } else {
                UUID uuid = null;
                try {
                    uuid = Objects.requireNonNull(Bukkit.getPlayer(args[0])).getUniqueId();
                } catch (Exception e) {
                    return true;
                }
                this.openPlayerGUI(sender, uuid, "main");
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.AQUA + "Not a valid command.");
        }
        return true;
    }

    private void listPlayerNotes(CommandSender sender, String player, UUID uuid) {
        List<String> notes = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
        if (notes.isEmpty()) {
            sender.sendMessage(ChatColor.AQUA + "This player has no notes related to them!");
            return;
        }
        sender.sendMessage(ChatColor.AQUA + "Notes for " + player + ":");
        for (int i = 0; i < notes.size(); i++) {
            int num = i + 1;
            sender.sendMessage(num + ": " + notes.get(i));
        }
    }
    private String createMessage(String[] args, String group) {
        StringBuilder cmd = new StringBuilder();
        int i = (Objects.equals(group, "all")) ? 2 : 3;
        for (; i < args.length; i++) {
            cmd.append(args[i]);
            if (i != args.length - 1) cmd.append(" ");
        }
        return cmd.toString();
    }

    public void addPlayerNotes(CommandSender sender, UUID uuid, String note) {
        this.historyConfig.getConfig().set(uuid + ".username", Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName());
        List<String> notesToAdd = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));

        StringBuilder sb = new StringBuilder();
        String str = sender.getName().equals("CONSOLE") ? "> " : ": ";
        sb.append(sender.getName()).append(str).append(note);

        notesToAdd.add(this.setCommand(sb.toString()));
        this.historyConfig.getConfig().set(uuid + ".notes", notesToAdd);
        this.historyConfig.save();
        sender.sendMessage(ChatColor.GREEN + "Note added for " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + ".");

    }

    public void removePlayerNote(CommandSender sender, UUID uuid, String note) {
        String player = Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName();
        if (Objects.equals(note, "all")) {
            this.historyConfig.getConfig().set(uuid + ".notes", new ArrayList<String>());
            this.historyConfig.save();
            plugin.setHistoryConfig(this.historyConfig);
            sender.sendMessage(ChatColor.GREEN + "Removed all notes for " + player + ".");
        } else if (Objects.equals(note, "history")) {
            //TODO could remove based off key word
            List<String> notesHistory = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));

            notesHistory.removeIf(n -> n.contains("CONSOLE>"));

            this.historyConfig.getConfig().set(uuid + ".notes", notesHistory);
            this.historyConfig.save();
            plugin.setHistoryConfig(this.historyConfig);
            sender.sendMessage(ChatColor.GREEN + "Removed the chat history from notes of " + player + ".");
        } else {
            try {
                List<String> notesUpdated = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
                int num = Integer.parseInt(note) - 1;
                notesUpdated.remove(num);
                this.historyConfig.getConfig().set(uuid + ".notes", notesUpdated);
                this.historyConfig.save();
                plugin.setHistoryConfig(this.historyConfig);
                sender.sendMessage(ChatColor.GREEN + "Removed note " + note.toLowerCase() + " for " + player + ".");
            } catch (Exception e) {
                sender.sendMessage(ChatColor.AQUA + "Please enter a valid number of the note you want to remove.");
            }
        }
    }

    private void promptHistoryWipe(CommandSender sender, UUID uuid) {
        if (!this.historyConfig.getConfig().contains(uuid.toString())) {
            sender.sendMessage(ChatColor.RED + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + " does not have any history");
            return;
        }
        ConversationFactory cf = new ConversationFactory(this.plugin);
        Conversation conv = cf.withFirstPrompt(new ClearHistoryPrompt(this, sender, uuid)).withLocalEcho(true).buildConversation((Player) sender);
        conv.begin();
    }

    public void wipeHistory(CommandSender sender, UUID uuid) {
        this.historyConfig.getConfig().set(uuid.toString(), null);
        this.historyConfig.save();
        sender.sendMessage(ChatColor.GREEN + "Removed " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + "'s history!");
    }

    private void listPlayerStrikes(CommandSender sender, UUID uuid) {
        sender.sendMessage(ChatColor.AQUA + "Strikes: ");
        for (String tier : this.plugin.getGroupList()) {
            int cnt = (historyConfig.getConfig().getString(uuid + "." + tier + ".count") == null) ? 0 : historyConfig.getConfig().getInt(uuid + "." + tier + ".count");
            sender.sendMessage("  " + tier + ": " + cnt);
        }
    }

    public void clearPlayerStrikes(CommandSender sender, UUID uuid, String group) {
        String player = Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName();
        if (Objects.equals(group, "all")) {
            for (String tier : this.plugin.getGroupList()) {
                this.historyConfig.getConfig().set(uuid + "." + tier + ".count", 0);
            }
            sender.sendMessage(ChatColor.GREEN + "Removed all of the strikes for " + player + ".");
        } else if (this.plugin.getGroupList().contains(group)) {
            plugin.getHistoryConfig().getConfig().set(uuid + "." + group + ".count", 0);
            plugin.getHistoryConfig().save();
            sender.sendMessage(ChatColor.GREEN + "Strikes removed for " + player + ".");
        } else {
            sender.sendMessage(ChatColor.AQUA + "Clear can only be followed by one of the filter groups.");
        }
        this.historyConfig.save();
        plugin.setHistoryConfig(this.historyConfig);
    }

    public void setPlayerStrikes(CommandSender sender, UUID uuid, String group, String num) {
        String player = Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName();
        if (plugin.getGroupList().contains(group)) {
            try {
                int strikes = Integer.parseInt(num);
                plugin.getHistoryConfig().getConfig().set(uuid + "." + group + ".count", strikes);
                plugin.getHistoryConfig().save();
                sender.sendMessage(ChatColor.GREEN + "Strikes for " + player + " set to " + strikes + ".");
            } catch (Exception e) {
                sender.sendMessage(ChatColor.AQUA + "Must enter a number.");
            }
        } else {
            sender.sendMessage(ChatColor.AQUA + "Not a valid filter group.");
        }
    }

    private void toggleSpamDetection(CommandSender sender, String toggle) {
        if (Objects.equals(toggle, "on") || Objects.equals(toggle, "off")) {
            Boolean spamState = toggle.equals("on");
            this.filterConfig.getConfig().set("spam.enabled", spamState);
            this.filterConfig.save();
            plugin.setFilterConfig(filterConfig);
            sender.sendMessage(ChatColor.GREEN + "Spam detection turned " + toggle + ".");
        } else {
            sender.sendMessage(ChatColor.AQUA + "The options are: on and off.");
        }
    }

    private void toggleBotDetection(CommandSender sender, String toggle) {
        if (Objects.equals(toggle, "on") || Objects.equals(toggle, "off")) {
            Boolean botState = toggle.equals("on");
            this.filterConfig.getConfig().set("bot.enabled", botState);
            this.filterConfig.save();
            plugin.setFilterConfig(filterConfig);
            sender.sendMessage(ChatColor.GREEN + "Bot detection turned " + toggle + ".");
        } else {
            sender.sendMessage(ChatColor.AQUA + "The options are: on and off.");
        }
    }

    private void toggleChatSpeed(CommandSender sender, String setting) {
        if (Objects.equals(setting, "normal") || Objects.equals(setting, "chill") || Objects.equals(setting, "slow") || Objects.equals(setting, "ice")) {
            this.filterConfig.getConfig().set("chatSpeed.mode", setting);
            this.filterConfig.save();
            plugin.setFilterConfig(this.filterConfig);
            sender.sendMessage(ChatColor.GREEN + "Chat speed set to " + setting);
        } else {
            sender.sendMessage(ChatColor.AQUA + "The options are: normal, chill, slow, ice..");
        }
    }

    public void createGroup(CommandSender sender, String name) {

        String group = "groups." + name + ".";
        this.filterConfig.getConfig().set(group + "enabled", false);
        this.filterConfig.getConfig().set(group + "level", plugin.getGroupList().size() + 1);
        this.filterConfig.getConfig().set(group + "partialMatches", false);
        this.filterConfig.getConfig().set(group + "mode", "replace");
        this.filterConfig.getConfig().set(group + "replaceWith", new ArrayList<>());
        this.filterConfig.getConfig().set(group + "msgToStaff", "[senderName] triggered " + name + " filter.");
        this.filterConfig.getConfig().set(group + "msgToStaffEnabled", false);
        this.filterConfig.getConfig().set(group + "commands", new ArrayList<>());
        this.filterConfig.getConfig().set(group + "history", false);
        this.filterConfig.getConfig().set(group + "maxStrikes", -1);
        this.filterConfig.getConfig().set(group + "strikeActions", new ArrayList<>());
        this.filterConfig.getConfig().set(group + "regex", new ArrayList<>());

        this.filterConfig.save();
        plugin.setFilterConfig(this.filterConfig);

        this.wordListConfig.getConfig().set(name, new ArrayList<>());
        this.wordListConfig.save();
        plugin.setWordList(this.wordListConfig);

        plugin.initGroupList();
        plugin.reloadGroupList();
        sender.sendMessage(ChatColor.GREEN + "New filter group " + name + " created!  Now to edit the options.");

    }

    public void removeGroup(CommandSender sender, String group) {
        if (this.filterConfig.getConfig().contains("groups." + group)) {
            this.filterConfig.getConfig().set("groups." + group, null);
            this.filterConfig.save();
            plugin.setFilterConfig(this.filterConfig);

            plugin.initGroupList();
            sender.sendMessage(ChatColor.GREEN + "Filter group " + group + " removed.");
        }
        if (this.wordListConfig.getConfig().contains(group)) {
            this.wordListConfig.getConfig().set(group, null);
            this.wordListConfig.save();
            plugin.setWordList(this.wordListConfig);
        }
    }

    private void promptRemove(CommandSender sender, String group) {
        if (!this.filterConfig.getConfig().contains("groups." + group)) {
            sender.sendMessage(ChatColor.AQUA + "Not a valid filter group!");
            return;
        }
        ConversationFactory cf = new ConversationFactory(this.plugin);
        Conversation conv = cf.withFirstPrompt(new ConfirmPrompt(this, sender, group)).withLocalEcho(true).buildConversation((Player) sender);
        conv.begin();
    }

    private void listGroups(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "Enabled filter groups ordered by level: ");
        for (String g : plugin.getGroupList()) {
            sender.sendMessage(" -" + g);
        }
    }

    private void callTimeout(CommandSender sender, UUID uuid, String length, String unit) {

        long time;
        try {
            time = Long.parseLong(length);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.AQUA + "Must enter a time in numbers.");
            return;
        }

        if (!Objects.equals(unit.toLowerCase(), "m") && !Objects.equals(unit.toLowerCase(), "min") && !Objects.equals(unit.toLowerCase(), "s") && !Objects.equals(unit.toLowerCase(), "sec")) {
            sender.sendMessage(ChatColor.AQUA + "For minutes use m or min.  For seconds use s or sec.");
            return;
        }
        if (plugin.getTimeoutMap().containsKey(uuid) && length.equals("0")) {
            Objects.requireNonNull(Bukkit.getPlayer(uuid)).sendMessage(ChatColor.GREEN + "Your timeout was removed!");
        } else if (!length.equals("0")) {
            Objects.requireNonNull(Bukkit.getPlayer(uuid)).sendMessage(ChatColor.RED + "You have been timed out for " + time + " " + unit);
        }
        this.timeout(uuid, time, unit);

    }

    private void toggleFilter(CommandSender sender, String group, String toggle) {
        Boolean filterState = toggle.equalsIgnoreCase("on");
        if (Objects.equals(group, "all")) {
            for (String tier : this.plugin.getGroupList()) {
                this.filterConfig.getConfig().set("groups." + tier + ".enabled", filterState);
            }
            sender.sendMessage(ChatColor.GREEN + "Filter turned " + toggle + ".");

        } else {
            this.filterConfig.getConfig().set("groups." + group + ".enabled", filterState);
            sender.sendMessage(ChatColor.GREEN + "Filter for " + group + " turned " + toggle + ".");
        }
        plugin.setFilterConfig(filterConfig);
    }

    private void changeMode(CommandSender sender, String group, String mode) {
        if (mode.equalsIgnoreCase("replace") || mode.equalsIgnoreCase("censor") || mode.equalsIgnoreCase("clear")) {
            if (Objects.equals(group, "all")) {
                for (String tier : this.plugin.getGroupList()) {
                    this.filterConfig.getConfig().set("groups." + tier + ".mode", mode);
                }
                sender.sendMessage(ChatColor.GREEN + "Filter mode set to " + mode + ".");
            } else {
                this.filterConfig.getConfig().set("groups." + group + ".mode", mode);
                sender.sendMessage(ChatColor.GREEN + "Filter mode set to " + mode + " for " + group + ".");
            }
            plugin.setFilterConfig(filterConfig);
        } else {
            sender.sendMessage(ChatColor.AQUA + "The three selectable modes are: Replace, Censor, Clear.");
        }

    }

    private void listCommands(CommandSender sender, String group) {
        List<String> commandList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + group + ".commands"));
        sender.sendMessage(ChatColor.AQUA + "Filter commands for " + group + ":");
        for (int i = 0; i < commandList.size(); i++) {
            int num = i + 1;
            sender.sendMessage(num + ": " + commandList.get(i));
        }
    }

    public void addCommands(CommandSender sender, String group, String message) {
        List<String> commandAddList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + group + ".commands"));
        commandAddList.add(message);
        this.filterConfig.getConfig().set("groups." + group + ".commands", commandAddList);
        plugin.setFilterConfig(this.filterConfig);
        sender.sendMessage(ChatColor.GREEN + "Added filter command for " + group);
    }

    public void removeCommands(CommandSender sender, String group, String number) {
        try {
            List<String> commandRemoveList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + group + ".commands"));
            int num = Integer.parseInt(number) - 1;
            commandRemoveList.remove(num);
            this.filterConfig.getConfig().set("groups." + group + ".commands", commandRemoveList);
            plugin.setFilterConfig(this.filterConfig);
            sender.sendMessage(ChatColor.GREEN + "Removed filter command for " + group + ".");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.AQUA + "Please enter the number of the command you want to remove.");
        }
    }

    private void toggleStaffMessage(CommandSender sender, String group, String toggle) {
        Boolean staffBool = toggle.equals("on");
        if (Objects.equals(group, "all")) {
            for (String tier : this.plugin.getGroupList()) {
                this.filterConfig.getConfig().set("groups." + tier + ".msgToStaffEnabled", staffBool);
            }
            sender.sendMessage(ChatColor.GREEN + "Staff messages turned " + toggle + ".");
        } else {
            this.filterConfig.getConfig().set("groups." + group + ".msgToStaffEnabled", staffBool);
            sender.sendMessage(ChatColor.GREEN + "Staff message for " + group + " turned " + toggle + ".");
        }
        plugin.setFilterConfig(filterConfig);
    }

    private void currentStaffMessage(CommandSender sender, String group) {
        if (Objects.equals(group, "all")) {
            for (String tier : this.plugin.getGroupList()) {
                sender.sendMessage(ChatColor.AQUA + "Current " + tier + " staff message: " + ChatColor.WHITE + this.filterConfig.getConfig().getString("groups." + tier + ".msgToStaff"));
            }
        } else {
            sender.sendMessage(ChatColor.AQUA + "Current staff message for " + group + ":");
            sender.sendMessage(Objects.requireNonNull(this.filterConfig.getConfig().getString("groups." + group + ".msgToStaff")));
        }
    }

    public void editStaffMessage(CommandSender sender, String group, String message) {

        if (Objects.equals(group, "all")) {
            for (String tier : this.plugin.getGroupList()) {
                this.filterConfig.getConfig().set("groups." + tier + ".msgToStaff", message);
            }
            sender.sendMessage(ChatColor.GREEN + "Changed staff message to:" + ChatColor.WHITE + "\"" + message + ".\"");

        } else {
            this.filterConfig.getConfig().set("groups." + group + ".msgToStaff", message);
            sender.sendMessage(ChatColor.GREEN + "Staff message for " + group + " changed.");
        }
        plugin.setFilterConfig(this.filterConfig);
    }

    private void listReplacements(CommandSender sender, String group) {
        sender.sendMessage(ChatColor.AQUA + "The current replacement messages for " + group + " are: ");
        List<String> replaceList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + group + ".replaceWith"));
        if (replaceList.isEmpty()) {
            sender.sendMessage(ChatColor.AQUA + "No replacement messages currently set.");
        }
        for (int i = 0; i < replaceList.size(); i++) {
            int num = i + 1;
            sender.sendMessage(num + ": " + replaceList.get(i));
        }
    }

    public void addReplacements(CommandSender sender, String group, String message) {
        List<String> addList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + group + ".replaceWith"));
        addList.add(message);
        this.filterConfig.getConfig().set("groups." + group + ".replaceWith", addList);
        plugin.setFilterConfig(this.filterConfig);
        sender.sendMessage(ChatColor.GREEN + "Added replacement message for " + group + ".");
    }

    public void removeReplacements(CommandSender sender, String group, String number) {
        try {
            List<String> removeList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + group + ".replaceWith"));
            int num = Integer.parseInt(number) - 1;
            removeList.remove(num);
            this.filterConfig.getConfig().set("groups." + group + ".replaceWith", removeList);
            plugin.setFilterConfig(this.filterConfig);
            sender.sendMessage(ChatColor.GREEN + "Removed replacement message for " + group + ".");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.AQUA + "Please enter the number of the replacement message you want to remove.");
        }
    }

    public void setMaxStrikes(CommandSender sender, String group, String strike) {
        int max;
        try {
            max = Integer.parseInt(strike);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.AQUA + "Must enter a number.");
            return;
        }
        if (Objects.equals(group, "all")) {
            for (String tier : this.plugin.getGroupList()) {
                this.filterConfig.getConfig().set("groups." + tier + ".maxStrikes", max);
            }
            sender.sendMessage(ChatColor.GREEN + "Max strikes for all groups set to " + max);
        } else {
            if (max <= 0) {
                sender.sendMessage(ChatColor.AQUA + "Max strikes cannot be 0 or below.  To turn strikes off enter 'off.'");
                return;
            }
            this.filterConfig.getConfig().set("groups." + group + ".maxStrikes", max);
            sender.sendMessage(ChatColor.GREEN + "Max strikes for " + group + " set to " + strike + ".");
        }
        this.filterConfig.save();
        plugin.setFilterConfig(this.filterConfig);
    }

    private void toggleStrikes(CommandSender sender, String group) {
        if (Objects.equals(group, "all")) {
            for (String tier : this.plugin.getGroupList()) {
                this.filterConfig.getConfig().set("groups." + tier + ".maxStrikes", -1);
            }
        } else {
            this.filterConfig.getConfig().set("groups." + group + ".maxStrikes", -1);
        }
        this.filterConfig.save();
        plugin.setFilterConfig(this.filterConfig);
        sender.sendMessage(ChatColor.GREEN + "Max strikes turned off!");
    }

    private void listStrikeCommands(CommandSender sender, String group) {
        List<String> commandList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + group + ".strikeActions"));
        sender.sendMessage(ChatColor.AQUA + "Max strike commands for " + group + ":");
        for (int i = 0; i < commandList.size(); i++) {
            int num = i + 1;
            sender.sendMessage(num + ": " + commandList.get(i));
        }
    }

    public void addStrikeCommands(CommandSender sender, String group, String command) {
        List<String> commandAddList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + group + ".strikeActions"));
        commandAddList.add(command);
        this.filterConfig.getConfig().set("groups." + group + ".strikeActions", commandAddList);
        this.filterConfig.save();
        plugin.setFilterConfig(this.filterConfig);
        sender.sendMessage(ChatColor.GREEN + "Added strike command for " + group);
    }

    public void removeStrikeCommands(CommandSender sender, String group, String number) {
        try {
            List<String> commandRemoveList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + group + ".strikeActions"));
            int num = Integer.parseInt(number) - 1;
            commandRemoveList.remove(num);
            this.filterConfig.getConfig().set("groups." + group + ".strikeActions", commandRemoveList);
            this.filterConfig.save();
            plugin.setFilterConfig(this.filterConfig);
            sender.sendMessage(ChatColor.GREEN + "Removed strike command for " + group + ".");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.AQUA + "Please enter the number of the strike command you want to remove.");
        }
    }

    private void toggleHistory(CommandSender sender, String group, String toggle) {
        switch (toggle) {
            case "on":
            case "off":
                if (Objects.equals(group, "all")) {
                    Boolean histBool = toggle.equals("on");
                    for (String tier : this.plugin.getGroupList()) {
                        this.filterConfig.getConfig().set("groups." + tier + ".history", histBool);
                    }
                    plugin.setFilterConfig(filterConfig);
                    sender.sendMessage(ChatColor.GREEN + "History turned " + toggle + ".");

                } else {
                    Boolean state = toggle.equalsIgnoreCase("on");
                    this.filterConfig.getConfig().set("groups." + group + ".history", state);
                    plugin.setFilterConfig(filterConfig);

                }
                sender.sendMessage(ChatColor.GREEN + "History for " + group + " turned " + toggle + ".");
                break;
            default:
                sender.sendMessage(ChatColor.AQUA + "The commands for history are on and off.");
                break;
        }
    }

    public void addWord(CommandSender sender, String group, String word) {
        List<String> wordList = this.wordListConfig.getConfig().getStringList(group);
        wordList.add(word);
        this.wordListConfig.getConfig().set(group, wordList);
        this.wordListConfig.save();
        plugin.setWordList(this.wordListConfig);
        plugin.getRegexBuilder().buildRegex(group);
        sender.sendMessage(ChatColor.GREEN + "Added " + word + " to the " + group + " filter list.");
    }

    public void removeWord(CommandSender sender, String group, String word) {
        List<String> wList = this.wordListConfig.getConfig().getStringList(group);
        if (!wList.contains(word)) {
            sender.sendMessage(ChatColor.AQUA + word + " is not in the " + group + " filter list.");
            return;
        }

        wList.remove(word);
        this.wordListConfig.getConfig().set(group, wList);
        this.wordListConfig.save();
        plugin.setWordList(this.wordListConfig);
        plugin.getRegexBuilder().buildRegex(group);
        sender.sendMessage(ChatColor.GREEN + "Removed " + word + " from the " + group + " filter list.");
    }

    private void listWords(CommandSender sender, String group) {
        List<String> gList = new ArrayList<>();
        if (Objects.equals(group, "all")) {
            gList.addAll(0, this.wordListConfig.getConfig().getKeys(false));
        } else {
            gList.add(group);
        }
        for (String g : gList) {
            List<String> wList = this.wordListConfig.getConfig().getStringList(g);
            int numGroups = this.filterConfig.getConfig().getKeys(false).size();
            int groupLevel = this.filterConfig.getConfig().getInt("groups." + g + ".level");
            float sev = (float) groupLevel / numGroups;
            if (sev <= ((float) 1 / 3)) {
                sender.sendMessage(ChatColor.RED + g);
            } else if (sev > ((float) 1 / 3) && sev <= ((float) 2 / 3)) {
                sender.sendMessage(ChatColor.YELLOW + g);
            } else if (sev > ((float) 2 / 3)) {
                sender.sendMessage(ChatColor.GREEN + g);
            }
            for (String word : wList) {
                sender.sendMessage(" " + word);
            }
        }
    }

    private void togglePartial(CommandSender sender, String group, String toggle) {
        switch (toggle) {
            case "on":
            case "off":
                if (Objects.equals(group, "all")) {
                    Boolean partBool = toggle.equals("on");
                    for (String tier : this.plugin.getGroupList()) {
                        this.filterConfig.getConfig().set("groups." + tier + ".partialMatches", partBool);
                        plugin.getRegexBuilder().buildRegex(tier);
                    }
                    plugin.setFilterConfig(filterConfig);
                    sender.sendMessage(ChatColor.GREEN + "Partial matches turned " + toggle + ".");

                } else {
                    Boolean state = toggle.equalsIgnoreCase("on");
                    this.filterConfig.getConfig().set("groups." + group + ".partialMatches", state);
                    plugin.setFilterConfig(filterConfig);
                    plugin.getRegexBuilder().buildRegex(group);
                    sender.sendMessage(ChatColor.GREEN + "Partial matches for " + group + " turned " + toggle + ".");

                }
                break;
            default:
                sender.sendMessage(ChatColor.AQUA + "The commands for partial history are on and off.");
                break;
        }
    }

    @EventHandler
    private void onInventoryClickEvent(InventoryClickEvent event) {
        String menu = event.getView().getTitle();
        if (!menu.contains("CFMC Moderation Tools")) return;
        event.setCancelled(true);
        UUID uuid = UUID.fromString(event.getView().getItem(4).getItemMeta().getLore().get(0));
        int slot = event.getSlot();

        switch (menu) {
            case "CFMC Moderation Tools - Main":
                switch (slot) {
                    case 12:
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "notes");
                        break;
                    case 13:
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "strikes");
                        break;
                    case 14:
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "clear");
                        break;
                    case 15:
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "timeout");
                        break;
                    default:
                        break;
                }
                break;
            case "CFMC Moderation Tools - Notes":
                switch (slot) {
                    case 0:
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "main");
                        break;
                    case 12:
                        this.listPlayerNotes(event.getWhoClicked(), Bukkit.getPlayer(uuid).getName(), uuid);
                        break;
                    case 13:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cf = new ConversationFactory(this.plugin);
                        Conversation conv = cf.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), uuid, "addNote")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        conv.begin();
                        break;
                    case 14:
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "notesRemove");
                        break;
                    default:
                        break;
                }
                break;
            case "CFMC Moderation Tools - Notes - Remove":
                switch (slot) {
                    case 0:
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "notes");
                        break;
                    case 12:
                        this.removePlayerNote(event.getWhoClicked(), uuid, "all");
                        break;
                    case 13:
                        this.removePlayerNote(event.getWhoClicked(), uuid, "history");
                        break;
                    case 14:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cf = new ConversationFactory(this.plugin);
                        Conversation conv = cf.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), uuid, "removeNote")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        conv.begin();
                        break;
                    default:
                        break;
                }
                break;
            case "CFMC Moderation Tools - Strikes":
                switch (slot) {
                    case 0:
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "main");
                        break;
                    case 12:
                        this.listPlayerStrikes(event.getWhoClicked(), uuid);
                        break;
                    case 13:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cf = new ConversationFactory(this.plugin);
                        Conversation conv = cf.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), uuid, "clearStrikes")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        conv.begin();
                        break;
                    case 14:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cf2 = new ConversationFactory(this.plugin);
                        Conversation conv2 = cf2.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), uuid, "setStrikes")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        conv2.begin();
                        break;
                    default:
                        break;
                }
                break;
            case "CFMC Moderation Tools - Clear History":
                switch (slot) {
                    case 0:
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "main");
                        break;
                    case 13:
                        this.wipeHistory(event.getWhoClicked(), uuid);
                        break;
                    default:
                        break;
                }
                break;
            case "CFMC Moderation Tools - Timeout":
                switch (slot) {
                    case 0:
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "main");
                        break;
                    case 4:
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "timeout");
                        break;
                    case 9:
                        this.callTimeout(event.getWhoClicked(), uuid, "0", "s");
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "timeout");
                        break;
                    case 10:
                        this.callTimeout(event.getWhoClicked(), uuid, "30", "s");
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "timeout");
                        break;
                    case 11:
                        this.callTimeout(event.getWhoClicked(), uuid, "1", "m");
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "timeout");
                        break;
                    case 12:
                        this.callTimeout(event.getWhoClicked(), uuid, "5", "m");
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "timeout");
                        break;
                    case 13:
                        this.callTimeout(event.getWhoClicked(), uuid, "10", "m");
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "timeout");
                        break;
                    case 14:
                        this.callTimeout(event.getWhoClicked(), uuid, "30", "m");
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "timeout");
                        break;
                    case 15:
                        this.callTimeout(event.getWhoClicked(), uuid, "60", "m");
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "timeout");
                        break;
                    case 16:
                        this.callTimeout(event.getWhoClicked(), uuid, "1440", "m");
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "timeout");
                        break;
                    case 17:
                        this.callTimeout(event.getWhoClicked(), uuid, "1000000", "m");
                        this.openPlayerGUI(event.getWhoClicked(), uuid, "timeout");
                        break;
                }
                break;
            default:
                break;
        }
    }

    private void openPlayerGUI(CommandSender sender, UUID uuid, String menu) {
        Player player = (Player) sender;
        Inventory inv;
        switch (menu) {
            case "main":
                inv = Bukkit.createInventory(player, 9 * 2, "CFMC Moderation Tools - Main");

                inv.setItem(4, getItem(new ItemStack(Material.CARVED_PUMPKIN), Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName(), uuid + ""));
                inv.setItem(12, getItem(new ItemStack(Material.BOOK), "Notes", "List, add, or remove notes for " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName()));
                inv.setItem(13, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Strikes", "List, set, or clear strikes for " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName()));
                inv.setItem(14, getItem(new ItemStack(Material.CYAN_STAINED_GLASS_PANE), "Clear History", "Completely delete the history for " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName()));
                inv.setItem(15, getItem(new ItemStack(Material.CLOCK), "Timeout", "Timeout " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName()));

                player.openInventory(inv);
                break;
            case "notes":
                inv = Bukkit.createInventory(player, 9 * 2, "CFMC Moderation Tools - Notes");

                inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
                inv.setItem(4, getItem(new ItemStack(Material.CARVED_PUMPKIN), Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName(), uuid + ""));
                inv.setItem(12, getItem(new ItemStack(Material.BOOK), "List Notes", "List notes for " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName()));
                inv.setItem(13, getItem(new ItemStack(Material.PAPER), "Add Note", "Add a note for " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName()));
                inv.setItem(14, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Remove Note", "Remove a note for " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName()));

                player.openInventory(inv);
                break;
            case "notesRemove":
                inv = Bukkit.createInventory(player, 9 * 2, "CFMC Moderation Tools - Notes - Remove");

                inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
                inv.setItem(4, getItem(new ItemStack(Material.CARVED_PUMPKIN), Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName(), uuid + ""));
                inv.setItem(12, getItem(new ItemStack(Material.BOOK), "Remove All Notes", "Remove all notes for " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName()));
                inv.setItem(13, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Remove History", "Remove history for " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName()));
                inv.setItem(14, getItem(new ItemStack(Material.PAPER), "Remove Note", "Remove a specific note for " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName()));

                player.openInventory(inv);
                break;
            case "strikes":
                inv = Bukkit.createInventory(player, 9 * 2, "CFMC Moderation Tools - Strikes");

                inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
                inv.setItem(4, getItem(new ItemStack(Material.CARVED_PUMPKIN), Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName(), uuid + ""));
                inv.setItem(12, getItem(new ItemStack(Material.BOOK), "List Strikes", "List strikes for " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName()));
                inv.setItem(13, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Clear Strikes", "Clear strikes for " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName()));
                inv.setItem(14, getItem(new ItemStack(Material.PAPER), "Set Strikes", "Set the strikes for " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName()));

                player.openInventory(inv);
                break;
            case "clear":
                inv = Bukkit.createInventory(player, 9 * 2, "CFMC Moderation Tools - Clear History");

                inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
                inv.setItem(4, getItem(new ItemStack(Material.CARVED_PUMPKIN), Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName(), uuid + ""));
                inv.setItem(13, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Remove From History", "Completely remove all history regarding " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName()));

                player.openInventory(inv);
                break;
            case "timeout":
                String rem;
                if (!plugin.getTimeoutMap().containsKey(uuid)) {
                    rem = "";
                } else {
                    Date date = new Date();
                    long now = date.getTime();
                    long dif = (plugin.getTimeoutMap().get(uuid) - now) / 1000;
                    long min = dif / 60;
                    long sec = dif % 60;
                    String clock = min + ":" + (sec < 10 ? "0" : "") + sec;
                    rem = ChatColor.RED + "Timeout remaining: " + ChatColor.WHITE + clock;
                }
                inv = Bukkit.createInventory(player, 9 * 2, "CFMC Moderation Tools - Timeout");

                inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
                inv.setItem(4, getItem(new ItemStack(Material.CARVED_PUMPKIN), Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName(), uuid + "", rem));
                inv.setItem(9, getItem(new ItemStack(Material.CLOCK), "Un-Timeout", "Take them off timeout"));
                inv.setItem(10, getItem(new ItemStack(Material.CLOCK), "30 seconds", ""));
                inv.setItem(11, getItem(new ItemStack(Material.CLOCK), "1 minutes", ""));
                inv.setItem(12, getItem(new ItemStack(Material.CLOCK), "5 minutes", ""));
                inv.setItem(13, getItem(new ItemStack(Material.CLOCK), "10 minutes", ""));
                inv.setItem(14, getItem(new ItemStack(Material.CLOCK), "30 minutes", ""));
                inv.setItem(15, getItem(new ItemStack(Material.CLOCK), "1 hour", ""));
                inv.setItem(16, getItem(new ItemStack(Material.CLOCK), "24 hours", ""));
                inv.setItem(17, getItem(new ItemStack(Material.CLOCK), "Until server restart", ""));

                player.openInventory(inv);
                break;
            default:
                break;
        }
    }

    @EventHandler
    private void onSettingsClickEvent(InventoryClickEvent event) {
        String menu = event.getView().getTitle();
        if (!menu.contains("CFMC Settings")) return;
        event.setCancelled(true);
        int slot = event.getSlot();

        switch (menu) {
            case "CFMC Settings - Main":
                switch (slot) {
                    case 1:
                        this.toggleFilter(event.getWhoClicked(), "all", this.getStatus("enabled").equals("on") ? "off" : "on");
                        this.openSettingsGUI(event.getWhoClicked(), "main");
                        break;
                    case 2:
                        String setting = this.filterConfig.getConfig().getString("chatSpeed.mode");
                        String set = (Objects.equals(setting, "normal")) ? "chill" : (Objects.equals(setting, "chill")) ? "slow" : (Objects.equals(setting, "slow")) ? "ice" : "normal";
                        this.toggleChatSpeed(event.getWhoClicked(), set);
                        this.openSettingsGUI(event.getWhoClicked(), "main");
                        break;
                    case 3:
                        String toggleBot = this.filterConfig.getConfig().getBoolean("bot.enabled") ? "off" : "on";
                        this.toggleBotDetection(event.getWhoClicked(), toggleBot);
                        this.openSettingsGUI(event.getWhoClicked(), "main");
                        break;
                    case 4:
                        String toggleSpam = this.filterConfig.getConfig().getBoolean("spam.enabled") ? "off" : "on";
                        this.toggleSpamDetection(event.getWhoClicked(), toggleSpam);
                        this.openSettingsGUI(event.getWhoClicked(), "main");
                        break;
                    case 5:
                        this.togglePartial(event.getWhoClicked(), "all", this.getStatus("partialMatches").equals("on") ? "off" : "on");
                        this.openSettingsGUI(event.getWhoClicked(), "main");
                        break;
                    case 6:
                        this.listGroups(event.getWhoClicked());
                        break;
                    case 7:
                        this.listWords(event.getWhoClicked(), "all");
                        break;
                    case 9:
                        this.openSettingsGUI(event.getWhoClicked(), "groups");
                        break;
                    case 10:
                        this.openSettingsGUI(event.getWhoClicked(), "staff");
                        break;
                    case 11:
                        this.openSettingsGUI(event.getWhoClicked(), "mode");
                        break;
                    case 12:
                        this.toggleHistory(event.getWhoClicked(), "all", this.getStatus("history").equals("on") ? "off" : "on");
                        this.openSettingsGUI(event.getWhoClicked(), "main");
                        break;
                    case 13:
                        this.openSettingsGUI(event.getWhoClicked(), "strikes");
                        break;
                    case 15:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cf = new ConversationFactory(this.plugin);
                        Conversation conv = cf.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), "createGroup")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        conv.begin();
                        break;
                    default:
                        break;
                }
                break;
            case "CFMC Settings - Staff Messages":
                switch (slot) {
                    case 0:
                        this.openSettingsGUI(event.getWhoClicked(), "main");
                        break;
                    case 2:
                        this.toggleStaffMessage(event.getWhoClicked(), "all", this.getStatus("msgToStaffEnabled").equals("on") ? "off" : "on");
                        this.openSettingsGUI(event.getWhoClicked(), "staff");
                        break;
                    case 3:
                        this.currentStaffMessage(event.getWhoClicked(), "all");
                        break;
                    case 4:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cf = new ConversationFactory(this.plugin);
                        Conversation conv = cf.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), "editStaffMsg")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        conv.begin();
                        break;
                }
                break;
            case "CFMC Settings - Strikes":
                switch (slot) {
                    case 0:
                        this.openSettingsGUI(event.getWhoClicked(), "main");
                        break;
                    case 2:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cf = new ConversationFactory(this.plugin);
                        Conversation conv = cf.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), "maxStrikes")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        conv.begin();
                        break;
                    case 3:
                        this.toggleStrikes(event.getWhoClicked(), "all");
                        break;
                    default:
                        break;
                }
                break;
            case "CFMC Settings - Groups":
                if (slot > plugin.getGroupList().size()) break;
                if (slot == 0) {
                    this.openSettingsGUI(event.getWhoClicked(), "main");
                    break;
                }

                String group = event.getView().getItem(slot).getItemMeta().getDisplayName();
                this.openSettingsGUI(event.getWhoClicked(), group);
                break;
            default:
                break;
        }
    }
    @EventHandler
    private void onFilterSettingsClickEvent(InventoryClickEvent event) {
        String menu = event.getView().getTitle();
        if (!menu.contains("CFMC Filter")) return;
        event.setCancelled(true);
        int slot = event.getSlot();

        String[] split = menu.split(" ");
        String group = split[3];
        String page = split[5];

        switch (page) {
            case "Main":
                switch (slot) {
                    case 0:
                        this.openSettingsGUI(event.getWhoClicked(), "groups");
                        break;
                    case 10:
                        String tog = this.filterConfig.getConfig().getBoolean("groups." + group + ".enabled") ? "off" : "on";
                        this.toggleFilter(event.getWhoClicked(), group, tog);
                        this.openSettingsGUI(event.getWhoClicked(), group);
                        break;
                    case 11:
                        this.listWords(event.getWhoClicked(), group);
                        break;
                    case 12:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cfa = new ConversationFactory(this.plugin);
                        Conversation conva = cfa.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), group, "addWord")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        conva.begin();
                        break;
                    case 13:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cfr = new ConversationFactory(this.plugin);
                        Conversation convr = cfr.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), group, "removeWord")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        convr.begin();
                        break;
                    case 14:
                        String setting = this.filterConfig.getConfig().getString("groups." + group + ".mode");
                        String set = (Objects.equals(setting, "replace")) ? "censor" : (Objects.equals(setting, "censor")) ? "clear" :  "replace";
                        this.changeMode(event.getWhoClicked(), group, set);
                        this.openSettingsGUI(event.getWhoClicked(), group);
                        break;
                    case 15:
                        String toggle = this.filterConfig.getConfig().getBoolean("groups." + group + ".history") ? "off" : "on";
                        this.toggleHistory(event.getWhoClicked(), group, toggle);
                        this.openSettingsGUI(event.getWhoClicked(), group);
                        break;
                    case 16:
                        String togglePartial = this.filterConfig.getConfig().getBoolean("groups." + group + ".partialMatches") ? "off" : "on";
                        this.togglePartial(event.getWhoClicked(), group, togglePartial);
                        this.openSettingsGUI(event.getWhoClicked(), group);
                        break;
                    case 20:
                        this.openSettingsGUI(event.getWhoClicked(), group + " - Strikes");
                        break;
                    case 21:
                        this.openSettingsGUI(event.getWhoClicked(), group + " - Commands");
                        break;
                    case 22:
                        this.openSettingsGUI(event.getWhoClicked(), group + " - Staff");
                        break;
                    case 23:
                        this.openSettingsGUI(event.getWhoClicked(), group + " - Replacement");
                        break;
                    case 26:
                        event.getWhoClicked().closeInventory();
                        this.promptRemove(event.getWhoClicked(), group);
                        break;

                    default:
                        break;
                }
                break;
            case "Strikes":
                switch (slot) {
                    case 0:
                        this.openSettingsGUI(event.getWhoClicked(), group);
                        break;
                    case 2:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cfm = new ConversationFactory(this.plugin);
                        Conversation convm = cfm.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), group, "setMaxStrikes")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        convm.begin();
                        break;
                    case 3:
                        this.toggleStrikes(event.getWhoClicked(), group);
                        break;
                    case 4:
                        this.listStrikeCommands(event.getWhoClicked(), group);
                        break;
                    case 5:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cf1 = new ConversationFactory(this.plugin);
                        Conversation conv1 = cf1.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), group, "addStrikeCommand")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        conv1.begin();
                        break;
                    case 6:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cf2 = new ConversationFactory(this.plugin);
                        Conversation conv2 = cf2.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), group, "removeStrikeCommand")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        conv2.begin();
                        break;
                }
                break;
            case "Commands":
                switch (slot) {
                    case 0:
                        this.openSettingsGUI(event.getWhoClicked(), group);
                        break;
                    case 2:
                        this.listCommands(event.getWhoClicked(), group);
                        break;
                    case 3:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cfA = new ConversationFactory(this.plugin);
                        Conversation convA = cfA.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), group, "addCommand")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        convA.begin();
                        break;
                    case 4:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cfR = new ConversationFactory(this.plugin);
                        Conversation convR = cfR.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), group, "removeCommand")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        convR.begin();
                        break;
                }
                break;
            case "Staff":
                switch (slot) {
                    case 0:
                        this.openSettingsGUI(event.getWhoClicked(), group);
                        break;
                    case 2:
                        String tog = this.filterConfig.getConfig().getBoolean("groups." + group + ".msgToStaffEnabled") ? "off" : "on";
                        this.toggleStaffMessage(event.getWhoClicked(), group, tog);
                        break;
                    case 3:
                        this.currentStaffMessage(event.getWhoClicked(), group);
                        break;
                    case 4:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cfS = new ConversationFactory(this.plugin);
                        Conversation convS = cfS.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), group, "editStaffMsg")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        convS.begin();
                        break;
                }
                break;
            case "Replacement":
                switch (slot) {
                    case 0:
                        this.openSettingsGUI(event.getWhoClicked(), group);
                        break;
                    case 2:
                        this.listReplacements(event.getWhoClicked(), group);
                        break;
                    case 3:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cfA = new ConversationFactory(this.plugin);
                        Conversation convA = cfA.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), group, "addReplacement")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        convA.begin();
                        break;
                    case 4:
                        event.getWhoClicked().closeInventory();
                        ConversationFactory cfR = new ConversationFactory(this.plugin);
                        Conversation convR = cfR.withFirstPrompt(new ConvPrompt(this, event.getWhoClicked(), group, "removeReplacement")).withLocalEcho(true).buildConversation((Player) event.getWhoClicked());
                        convR.begin();
                        break;
                }
                break;
        }
    }

    private void openSettingsGUI(CommandSender sender, String menu) {
        Player player = (Player) sender;
        Inventory inv;
        String[] split = menu.split(" ");
        if (Objects.equals(menu, "main")) {
            String speed = this.filterConfig.getConfig().getString("chatSpeed.mode");
            String interval = Objects.equals(speed, "normal") ? "0" : this.filterConfig.getConfig().getString("chatSpeed." + speed + ".time");
            String bot = this.filterConfig.getConfig().getBoolean("bot.enabled") ? "on" : "off";
            String spam = this.filterConfig.getConfig().getBoolean("spam.enabled") ? "on" : "off";

            inv = Bukkit.createInventory(player, 9 * 2, "CFMC Settings - Main");
            inv.setItem(1, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "Toggle Filter On or Off", "Currently: " + this.getStatus("enabled")));
            inv.setItem(2, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Chat Speed", "Speed: " + speed, "Time interval: " + interval));
            inv.setItem(3, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "Bot Detection", "Bot Detection: " + bot));
            inv.setItem(4, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Spam Detection", "Spam Detection: " + spam));
            inv.setItem(5, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "Partial Detection", "Toggle Partial Detection for all groups."));
            inv.setItem(6, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "List Groups", ""));
            inv.setItem(7, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "List Words", ""));
            inv.setItem(9, getItem(new ItemStack(Material.BOOKSHELF), "Edit Groups", "Edit filter group specific settings"));
            inv.setItem(10, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Staff Message Settings", ""));
            inv.setItem(11, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "Filter Modes", "Change filter mode"));
            inv.setItem(12, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "history", ""));
            inv.setItem(13, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "Strikes", "Set strikes for all: max or off"));
            inv.setItem(15, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Create New Group", ""));
            player.openInventory(inv);
        } else if (Objects.equals(menu, "staff")) {
            inv = Bukkit.createInventory(player, 9 * 2, "CFMC Settings - Staff Messages");
            inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
            inv.setItem(2, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Toggle Staff Messages", "Currently: " + this.getStatus("msgToStaffEnabled")));
            inv.setItem(3, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "List Staff Messages", "List all staff messages"));
            inv.setItem(4, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Edit Staff Messages", "Edit all staff messages.  Use [senderName] for a players name in the message."));
            player.openInventory(inv);
        } else if (Objects.equals(menu, "strikes")) {
            inv = Bukkit.createInventory(player, 9 * 2, "CFMC Settings - Strikes");
            inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
            inv.setItem(2, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Set Max Strikes", ""));
            inv.setItem(3, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "Turn Off Max Strikes", ""));
            inv.setItem(4, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Max Strike Commands", "Edit settings for the max strike commands"));
            player.openInventory(inv);
        } else if (Objects.equals(menu, "strikeCommands")) {
            inv = Bukkit.createInventory(player, 9 * 2, "CFMC Settings - Strike Commands");
            inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
            inv.setItem(2, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "List Strike Commands", ""));
            inv.setItem(3, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "Add Strike Commands", ""));
            inv.setItem(4, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Remove Strike Command", "Remove command by its number"));
            player.openInventory(inv);
        } else if (Objects.equals(menu, "groups")) {
            int s = plugin.getGroupList().size() / 9 + 2;
            inv = Bukkit.createInventory(player, 9 * s, "CFMC Settings - Groups");
            inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
            for (int i = 0; i < plugin.getGroupList().size(); i++) {
                inv.setItem(i + 1, getItem(new ItemStack(Material.BOOK), plugin.getGroupList().get(i), ""));
            }
            player.openInventory(inv);
        } else if (plugin.getGroupList().contains(split[0].toLowerCase())) {
            if (split.length == 1) {
                String status = this.filterConfig.getConfig().getBoolean("groups." + menu + ".enabled") ? "on" : "off";
                String mode = this.filterConfig.getConfig().getString("groups." + menu + ".mode");
                String historyToggle = this.filterConfig.getConfig().getBoolean("groups." + menu + ".history") ? "on" : "off";
                String partialToggle = this.filterConfig.getConfig().getBoolean("groups." + menu + ".partialMatches") ? "on" : "off";

                inv = Bukkit.createInventory(player, 9 * 3, "CFMC Filter - " + menu + " - Main");
                inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
                inv.setItem(10, getItem(new ItemStack(Material.BOOK), "Toggle Filter", "Currently " + status));
                inv.setItem(11, getItem(new ItemStack(Material.BOOK), "List Words", ""));
                inv.setItem(12, getItem(new ItemStack(Material.BOOK), "Add Word", ""));
                inv.setItem(13, getItem(new ItemStack(Material.BOOK), "Remove Word", ""));
                inv.setItem(14, getItem(new ItemStack(Material.BOOK), "Change Filter Mode", "Currently: " + mode));
                inv.setItem(15, getItem(new ItemStack(Material.BOOK), "Toggle History", "Currently: " + historyToggle));
                inv.setItem(16, getItem(new ItemStack(Material.BOOK), "Toggle Partial Detection", "Currently: " + partialToggle));
                inv.setItem(20, getItem(new ItemStack(Material.BOOK), "Strike Settings", ""));
                inv.setItem(21, getItem(new ItemStack(Material.BOOK), "Command Settings", ""));
                inv.setItem(22, getItem(new ItemStack(Material.BOOK), "Staff Settings", ""));
                inv.setItem(23, getItem(new ItemStack(Material.BOOK), "Replacement Settings", ""));
                inv.setItem(26, getItem(new ItemStack(Material.SKELETON_SKULL), "Delete Group", "Permanently delete this group."));
                player.openInventory(inv);
            } else {
                String page = split[2];
                String group = split[0];
                switch (page) {
                    case "Strikes":
                        inv = Bukkit.createInventory(player, 9 * 2, "CFMC Filter - " + group + " - Strikes");
                        inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
                        inv.setItem(2, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Set Max Strikes", "Also turns max strikes on if set off."));
                        inv.setItem(3, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "Turn Off Max Strikes", ""));
                        inv.setItem(4, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "List Max Strike Commands", ""));
                        inv.setItem(5, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "Add Max Strike Command", ""));
                        inv.setItem(6, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Remove Max Strike Command", ""));
                        break;
                    case "Commands":
                        inv = Bukkit.createInventory(player, 9 * 2, "CFMC Filter - " + group + " - Commands");
                        inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
                        inv.setItem(2, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "List Commands", ""));
                        inv.setItem(3, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "Add Commands", ""));
                        inv.setItem(4, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Remove Command", ""));
                        break;
                    case "Staff":
                        inv = Bukkit.createInventory(player, 9 * 2, "CFMC Filter - " + group + " - Staff");
                        String status = this.filterConfig.getConfig().getBoolean("groups." + menu + ".msgToStaffEnabled") ? "on" : "off";
                        inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
                        inv.setItem(2, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Toggle Staff Messages", "Currently: " + status));
                        inv.setItem(3, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "List Current Staff Message", ""));
                        inv.setItem(4, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Edit Staff Message", "Use [senderName] for a players name in the message."));
                        break;
                    case "Replacement":
                        inv = Bukkit.createInventory(player, 9 * 2, "CFMC Filter - " + group + " - Replacement");
                        inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
                        inv.setItem(2, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "List Replacement Messages", ""));
                        inv.setItem(3, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "Add Replacement Message", ""));
                        inv.setItem(4, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Remove Replacement Message", ""));
                        break;
                    default:
                        inv = Bukkit.createInventory(player, 9 * 2, "CFMC Filter - " + menu + " - Main");
                        Bukkit.getLogger().info("Error in menu");
                        break;
                }
                player.openInventory(inv);
            }


            //TODO set item 4 to list all current settings?
            // list words, remove, on/off, mode, commands (list, add, remove), staff (on/off, current, edit), replace (list, add, remove),
            // strikes (max, off, commands(list, add, remove)), history, add word, remove word, partial
        }
    }

    private String getStatus(String setting) {
        boolean on = false;
        for (String group : plugin.getGroupList()) {
            if (this.filterConfig.getConfig().getBoolean("groups." + group + "." + setting)) {
                on = true;
                break;
            }
        }
        return on ? "on" : "off";
    }

    private ItemStack getItem(ItemStack item, String name, String... lore) {
        ItemMeta meta = item.getItemMeta();

        Objects.requireNonNull(meta).setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

        List<String> lores = new ArrayList<>();
        for (String s : lore) {
            lores.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        meta.setLore(lores);
        item.setItemMeta(meta);

        return item;
    }

    private String setCommand(String msg) {
        if (msg.contains("[date]")) {
            Date now = new Date();
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            msg = msg.replace("[date]", "[" + format.format(now) + "]");
        }

        return msg;
    }

    private void timeout(UUID uuid, Long time, String var) {
        Date now = new Date();
        Long cnt = ((Objects.equals(var, "m") || Objects.equals(var, "min")) ? time * 60 : time) * 1000;
        Long until = now.getTime() + cnt;
        if (time == 0) {
            plugin.getTimeoutMap().remove(uuid);
            return;
        }
        plugin.getTimeoutMap().put(uuid, until);
    }
}
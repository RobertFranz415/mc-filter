package mctest.chatfiltermc.commands;

import com.sun.tools.javac.util.StringUtils;
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
                            this.editStaffMessage(args, sender, "all");
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
                            this.setMaxStrikes(sender, args[2], "all");
                            break;
                        case "off":
                            this.toggleStrikes("all");
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
                            this.addPlayerNotes(sender, uuid, this.buildNote(args));
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
                                    this.addCommands(args, sender);
                                    break;
                                case "remove":
                                    this.removeCommands(args, sender);
                                    break;
                                default:
                                    sender.sendMessage(ChatColor.AQUA + "The options for commands are: list, add, and remove..");
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
                                    this.editStaffMessage(args, sender, args[0].toLowerCase());
                                    break;
                                default:
                                    sender.sendMessage(ChatColor.AQUA + "The options for staff are: current, edit, and on/off.");
                                    break;
                            }
                            break;
                        case "replace":
                            switch (args[2].toLowerCase()) {
                                case "list":
                                    this.listReplacements(args, sender);
                                    break;
                                case "add":
                                    this.addReplacements(args, sender);
                                    break;
                                case "remove":
                                    this.removeReplacements(args, sender);
                                    break;
                                default:
                                    sender.sendMessage(ChatColor.AQUA + "The options for replace are: list, add, and remove.");
                                    break;
                            }
                            break;
                        case "strikes":
                            switch (args[2].toLowerCase()) {
                                case "max":
                                    this.setMaxStrikes(sender, args[3], args[0].toLowerCase());
                                    break;
                                case "off":
                                    this.toggleStrikes(args[0].toLowerCase());
                                    break;
                                case "commands":
                                    switch (args[3].toLowerCase()) {
                                        case "list":
                                            this.listStrikeCommands(args, sender);
                                            break;
                                        case "add":
                                            this.addStrikeCommands(args, sender);
                                            break;
                                        case "remove":
                                            this.removeStrikeCommands(args, sender);
                                            break;
                                        default:
                                            sender.sendMessage(ChatColor.AQUA + "The options for strike commands are: list, add, and remove..");
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
            }
            else {
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
    private String buildNote(String[] args) {
        StringBuilder note = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            note.append(args[i]);
            if (i != args.length - 1) note.append(" ");
        }
        return note.toString();
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
        //TODO Implement conversation
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
    private void addCommands(String[] args, CommandSender sender) {
        List<String> commandAddList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + args[0].toLowerCase() + ".commands"));
        StringBuilder cmd = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            cmd.append(args[i]);
            if (i != args.length - 1) cmd.append(" ");
        }
        commandAddList.add(cmd.toString());
        this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".commands", commandAddList);
        plugin.setFilterConfig(this.filterConfig);
        sender.sendMessage(ChatColor.GREEN + "Added filter command for " + args[0].toLowerCase());
    }
    private void removeCommands(String[] args, CommandSender sender) {
        try {
            List<String> commandRemoveList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + args[0].toLowerCase() + ".commands"));
            int num = Integer.parseInt(args[3]) - 1;
            commandRemoveList.remove(num);
            this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".commands", commandRemoveList);
            plugin.setFilterConfig(this.filterConfig);
            sender.sendMessage(ChatColor.GREEN + "Removed filter command for " + args[0].toLowerCase() + ".");
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
    private void editStaffMessage(String[] args, CommandSender sender, String group) {
        StringBuilder cmd = new StringBuilder();
        int i = (Objects.equals(group, "all")) ? 2 : 3;
        for (; i < args.length; i++) {
            cmd.append(args[i]);
            if (i != args.length - 1) cmd.append(" ");
        }
        if (Objects.equals(group, "all")) {
            for (String tier : this.plugin.getGroupList()) {
                this.filterConfig.getConfig().set("groups." + tier + ".msgToStaff", cmd.toString());
            }
            sender.sendMessage(ChatColor.GREEN + "Changed staff message to:" + ChatColor.WHITE + "\"" + cmd + ".\"");

        } else {
            this.filterConfig.getConfig().set("groups." + group + ".msgToStaff", cmd.toString());
            sender.sendMessage(ChatColor.GREEN + "Staff message for " + group + " changed.");
        }
        plugin.setFilterConfig(this.filterConfig);
    }

    private void listReplacements(String[] args, CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "The current replacement messages for " + args[0].toLowerCase() + " are: ");
        List<String> replaceList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + args[0].toLowerCase() + ".replaceWith"));
        if (replaceList.isEmpty()) {
            sender.sendMessage(ChatColor.AQUA + "No replacement messages currently set.");
        }
        for (int i = 0; i < replaceList.size(); i++) {
            int num = i + 1;
            sender.sendMessage(num + ": " + replaceList.get(i));
        }
    }
    private void addReplacements(String[] args, CommandSender sender) {
        List<String> addList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + args[0].toLowerCase() + ".replaceWith"));
        StringBuilder msg = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            msg.append(args[i]);
            if (i != args.length - 1) msg.append(" ");
        }
        addList.add(msg.toString());
        this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".replaceWith", addList);
        plugin.setFilterConfig(this.filterConfig);
        sender.sendMessage(ChatColor.GREEN + "Added replacement message for " + args[0].toLowerCase() + ".");
    }
    private void removeReplacements(String[] args, CommandSender sender) {
        try {
            List<String> removeList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + args[0].toLowerCase() + ".replaceWith"));
            int num = Integer.parseInt(args[3]) - 1;
            removeList.remove(num);
            this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".replaceWith", removeList);
            plugin.setFilterConfig(this.filterConfig);
            sender.sendMessage(ChatColor.GREEN + "Removed replacement message for " + args[0].toLowerCase() + ".");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.AQUA + "Please enter the number of the replacement message you want to remove.");
            return;
        }
    }

    private void setMaxStrikes(CommandSender sender, String strike, String group) {
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
            sender.sendMessage(ChatColor.GREEN + "Max strikes for " + group + " set to " + strike +".");
        }
        this.filterConfig.save();
        plugin.setFilterConfig(this.filterConfig);
    }

    private void toggleStrikes(String group) {
        if (Objects.equals(group, "all")) {
            for (String tier : this.plugin.getGroupList()) {
                this.filterConfig.getConfig().set("groups." + tier + ".maxStrikes", -1);
            }
        } else {
            this.filterConfig.getConfig().set("groups." + group + ".maxStrikes", -1);
        }
        this.filterConfig.save();
        plugin.setFilterConfig(this.filterConfig);
    }

    private void listStrikeCommands(String[] args, CommandSender sender) {
        List<String> commandList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + args[0].toLowerCase() + ".strikeActions"));
        sender.sendMessage(ChatColor.AQUA + "Filter commands for " + args[0].toLowerCase() + ":");
        for (int i = 0; i < commandList.size(); i++) {
            int num = i + 1;
            sender.sendMessage(num + ": " + commandList.get(i));
        }
    }

    private void addStrikeCommands(String[] args, CommandSender sender) {
        List<String> commandAddList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + args[0].toLowerCase() + ".strikeActions"));
        StringBuilder cmd = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            cmd.append(args[i]);
            if (i != args.length - 1) cmd.append(" ");
        }
        commandAddList.add(cmd.toString());
        this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".strikeActions", commandAddList);
        this.filterConfig.save();
        plugin.setFilterConfig(this.filterConfig);
        sender.sendMessage(ChatColor.GREEN + "Added strike command for " + args[0].toLowerCase());
    }

    private void removeStrikeCommands(String[] args, CommandSender sender) {
        try {
            List<String> commandRemoveList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + args[0].toLowerCase() + ".strikeActions"));
            int num = Integer.parseInt(args[4]) - 1;
            commandRemoveList.remove(num);
            this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".strikeActions", commandRemoveList);
            this.filterConfig.save();
            plugin.setFilterConfig(this.filterConfig);
            sender.sendMessage(ChatColor.GREEN + "Removed strike command for " + args[0].toLowerCase() + ".");
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

    private void addWord(CommandSender sender, String group, String word) {
        List<String> wordList = this.wordListConfig.getConfig().getStringList(group);
        wordList.add(word);
        this.wordListConfig.getConfig().set(group, wordList);
        this.wordListConfig.save();
        plugin.setWordList(this.wordListConfig);
        plugin.getRegexBuilder().buildRegex(group);
        sender.sendMessage(ChatColor.GREEN + "Added " + word + " to the " + group + " filter list.");
    }

    private void removeWord(CommandSender sender, String group, String word) {
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
            if (sev <= ((float) 1 /3)) {
                sender.sendMessage(ChatColor.RED + g);
            } else if (sev > ((float) 1 /3) && sev <= ((float) 2/3)) {
                sender.sendMessage(ChatColor.YELLOW + g);
            } else if (sev > ((float) 2 /3)) {
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
                        // delete
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
                    case 9:
                        this.callTimeout(event.getWhoClicked(), uuid, "0", "s");
                        break;
                    case 10:
                        this.callTimeout(event.getWhoClicked(), uuid, "30", "s");
                        break;
                    case 11:
                        this.callTimeout(event.getWhoClicked(), uuid, "1", "m");
                        break;
                    case 12:
                        this.callTimeout(event.getWhoClicked(), uuid, "5", "m");
                        break;
                    case 13:
                        this.callTimeout(event.getWhoClicked(), uuid, "10", "m");
                        break;
                    case 14:
                        this.callTimeout(event.getWhoClicked(), uuid, "30", "m");
                        break;
                    case 15:
                        this.callTimeout(event.getWhoClicked(), uuid, "60", "m");
                        break;
                    case 16:
                        this.callTimeout(event.getWhoClicked(), uuid, "1440", "m");
                        break;
                    case 17:
                        this.callTimeout(event.getWhoClicked(), uuid, "1000000", "m");
                        break;

                }
                this.openPlayerGUI(event.getWhoClicked(), uuid, "timeout");
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
                    rem = "Currently timed out for " + min + " minutes and " + sec + " seconds.";
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
                        boolean on = false;
                        for (String group : plugin.getGroupList()) {
                            if (this.filterConfig.getConfig().getBoolean("groups." + group + ".enabled")) {
                                on = true;
                                break;
                            }
                        }
                        String togFilter = on ? "off" : "on";
                        this.toggleFilter(event.getWhoClicked(), "all", togFilter);
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
                        //TODO Partials?
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
        if (menu.contains("CFMC Settings - Filter: ")) {
            String[] split = menu.split(" ");
            String group = split[split.length-1];
            switch (slot) {
                case 0:
                    this.openSettingsGUI(event.getWhoClicked(), "groups");
                    break;
                case 2:
                    this.listWords(event.getWhoClicked(), group);
                    break;
                case 8:
                    event.getWhoClicked().closeInventory();
                    this.promptRemove(event.getWhoClicked(), group);
                    break;
            }
        }
    }
    //TODO Add spam detection, bot detection, chat speed menu
    private void openSettingsGUI(CommandSender sender, String menu) {
        Player player = (Player) sender;
        Inventory inv;
        if (Objects.equals(menu, "main")) {
            String speed = this.filterConfig.getConfig().getString("chatSpeed.mode");
            String interval = Objects.equals(speed, "normal") ? "0" : this.filterConfig.getConfig().getString("chatSpeed." + speed + ".time");
            String bot = this.filterConfig.getConfig().getBoolean("bot.enabled") ? "on" : "off";
            String spam = this.filterConfig.getConfig().getBoolean("spam.enabled") ? "on" : "off";
            boolean on = false;
            for (String group : plugin.getGroupList()) {
                if (this.filterConfig.getConfig().getBoolean( "groups." + group + ".enabled")) {
                    on = true;
                    break;
                }
            }
            String toggleFilter = on ? "on" : "off";

            inv = Bukkit.createInventory(player, 9 * 2, "CFMC Settings - Main");
            inv.setItem(1, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "Toggle Filter On or Off", "Currently: " + toggleFilter));
            inv.setItem(2, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Chat Speed", "Speed: " + speed, "Time interval: " + interval));
            inv.setItem(3, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "Bot Detection", "Bot Detection: " + bot));
            inv.setItem(4, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Spam Detection", "Spam Detection: " + spam));
            inv.setItem(5, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "Partial Detection", "Toggle Partial Detection for all groups."));
            inv.setItem(6, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "List Groups", ""));
            inv.setItem(7, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "List Words", ""));
            inv.setItem(9, getItem(new ItemStack(Material.BOOKSHELF), "Edit Groups", "Edit filter group specific settings"));
            inv.setItem(10, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Staff Message Settings", "")); //on/off, current, edit
            inv.setItem(11, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "mode", ""));
            inv.setItem(12, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "history", ""));
            inv.setItem(13, getItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), "Strikes", "Set strikes for all: max or off"));
            inv.setItem(15, getItem(new ItemStack(Material.RED_STAINED_GLASS_PANE), "Create New Group", ""));
            player.openInventory(inv);
        } else if (Objects.equals(menu, "groups")) {
            int s = plugin.getGroupList().size() / 9 + 2;
            inv = Bukkit.createInventory(player, 9 * s, "CFMC Settings - Groups");
            inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
            for (int i = 0; i < plugin.getGroupList().size(); i++) {
                inv.setItem(i+1, getItem(new ItemStack(Material.BOOK), plugin.getGroupList().get(i), ""));
            }
            player.openInventory(inv);
        } else if (plugin.getGroupList().contains(menu.toLowerCase())) {

            inv = Bukkit.createInventory(player, 9 * 2, "CFMC Settings - Filter: " + menu);
            inv.setItem(0, getItem(new ItemStack(Material.ARROW), "Go Back", ""));
            inv.setItem(2, getItem(new ItemStack(Material.BOOK), "List words", ""));
            inv.setItem(8, getItem(new ItemStack(Material.SKELETON_SKULL), "Delete Group", "Permanently delete this group."));

            player.openInventory(inv);
            //TODO set item 4 to list all current settings?
            // list words, remove, on/off, mode, commands (list, add, remove), staff (on/off, current, edit), replace (list, add, remove),
            // strikes (max, off, commands(list, add, remove)), history, add word, remove word, partial
        }

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
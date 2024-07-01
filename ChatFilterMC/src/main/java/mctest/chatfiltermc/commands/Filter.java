package mctest.chatfiltermc.commands;

import com.sun.tools.javac.util.StringUtils;
import mctest.chatfiltermc.ChatFilterMC;
import mctest.chatfiltermc.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;

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
    // Decide if there should be separate files for regex and config stuff
    // Figure out if possible to change final groups command to switch
    // figure out best order of command... currently /filter notes strike [player] clear swears, etc...

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("filter") || !sender.isOp()) return true;

        this.filterConfig = plugin.getFilterConfig();

        try {
            switch (args[0].toLowerCase()) {
                case "on":
                case "off":
                    Boolean state = args[0].equals("on");
                    for (String tier : this.plugin.getGroupList()) {
                        this.filterConfig.getConfig().set("groups." + tier + ".enabled", state);
                    }
                    plugin.setFilterConfig(filterConfig);
                    sender.sendMessage(ChatColor.GREEN + "Filter turned " + args[0].toLowerCase() + ".");
                    break;
                case "mode":
                    if (args[1].equalsIgnoreCase("replace") || args[1].equalsIgnoreCase("censor") || args[1].equalsIgnoreCase("clear")) {
                        for (String tier : this.plugin.getGroupList()) {
                            this.filterConfig.getConfig().set("groups." + tier + ".mode", args[1].toLowerCase());
                        }
                        plugin.setFilterConfig(filterConfig);
                        sender.sendMessage(ChatColor.GREEN + "Filter mode set to " + args[1].toLowerCase() + ".");
                    } else {
                        sender.sendMessage(ChatColor.AQUA + "The three selectable modes are: Replace, Censor, Clear.");
                    }
                    break;
                case "admin":
                    switch (args[1].toLowerCase()) {
                        case "on":
                        case "off":
                            Boolean adminBool = args[1].equals("on");
                            for (String tier : this.plugin.getGroupList()) {
                                this.filterConfig.getConfig().set("groups." + tier + ".msgToAdminEnabled", adminBool);
                            }
                            plugin.setFilterConfig(filterConfig);
                            sender.sendMessage(ChatColor.GREEN + "Admin messages turned " + args[1].toLowerCase() + ".");
                            break;
                        case "current":
                            for (String tier : this.plugin.getGroupList()) {
                                sender.sendMessage(ChatColor.AQUA + "Current " + tier + " admin message: " + ChatColor.WHITE + this.filterConfig.getConfig().getString("groups." + tier + ".msgToAdmin"));
                            }
                            break;
                        case "edit":
                            StringBuilder cmd = new StringBuilder();
                            for (int i = 2; i < args.length; i++) {
                                cmd.append(args[i]);
                                if (i != args.length - 1) cmd.append(" ");
                            }
                            for (String tier : this.plugin.getGroupList()) {
                                this.filterConfig.getConfig().set("groups." + tier + ".msgToAdmin", cmd.toString());
                            }
                            plugin.setFilterConfig(this.filterConfig);
                            sender.sendMessage(ChatColor.GREEN + "Changed admin message to:" + ChatColor.WHITE + "\"" + cmd + ".\"");
                            break;
                        default:
                            sender.sendMessage(ChatColor.AQUA + "The admin commands are: on/off and edit.");
                            break;
                    }
                    break;
                case "history":
                    switch (args[1].toLowerCase()) {
                        case "on":
                        case "off":
                            Boolean histBool = args[1].equals("on");
                            for (String tier : this.plugin.getGroupList()) {
                                this.filterConfig.getConfig().set("groups." + tier + ".history", histBool);
                            }
                            plugin.setFilterConfig(filterConfig);
                            sender.sendMessage(ChatColor.GREEN + "History turned " + args[1].toLowerCase() + ".");
                            break;
                        default:
                            sender.sendMessage(ChatColor.AQUA + "Must select on or off.");
                            break;
                    }
                    break;
                case "strikes":
                    if (args.length < 2) return true;
                    switch (args[1].toLowerCase()) {
                        case "max":
                            int max;
                            try {
                                max = Integer.parseInt(args[2]);
                            } catch (Exception e) {
                                sender.sendMessage(ChatColor.AQUA + "Must enter a number.");
                                return true;
                            }
                            for (String tier : this.plugin.getGroupList()) {
                                this.filterConfig.getConfig().set("groups." + tier + ".maxStrikes", max);
                            }
                            this.filterConfig.save();
                            plugin.setFilterConfig(filterConfig);
                            sender.sendMessage(ChatColor.GREEN + "Max strikes for all groups set to " + max);
                            break;
                        default:
                            sender.sendMessage(ChatColor.AQUA + "The options for strikes are max and options.");
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
                            List<String> notes = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
                            if (notes.isEmpty()) {
                                sender.sendMessage(ChatColor.AQUA + "This player has no notes related to them!");
                                return true;
                            }
                            sender.sendMessage(ChatColor.AQUA + "Notes for " + args[2].toLowerCase() + ":");
                            for (int i = 0; i < notes.size(); i++) {
                                int num = i + 1;
                                sender.sendMessage(num + ": " + notes.get(i));
                            }
                            break;
                        case "add":
                            List<String> notesToAdd = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
                            StringBuilder note = new StringBuilder();
                            String str = sender.getName().equals("CONSOLE") ? "> " : ": ";
                            note.append(sender.getName()).append(str);
                            for (int i = 3; i < args.length; i++) {
                                note.append(args[i]);
                                if (i != args.length - 1) note.append(" ");
                            }
                            notesToAdd.add(this.setCommand(note.toString()));
                            this.historyConfig.getConfig().set(uuid + ".notes", notesToAdd);
                            this.historyConfig.save();
                            sender.sendMessage(ChatColor.GREEN + "Note added for " + args[2].toLowerCase() + ".");
                            break;
                        case "remove":
                            if (args.length == 3) {
                                sender.sendMessage(ChatColor.AQUA + "Must enter a valid number, all, or history.");
                                return true;
                            }
                            if (Objects.equals(args[3], "all")) {
                                this.historyConfig.getConfig().set(uuid + ".notes", new ArrayList<String>());
                                this.historyConfig.save();
                                plugin.setHistoryConfig(this.historyConfig);
                                sender.sendMessage(ChatColor.GREEN + "Removed all notes for " + args[2].toLowerCase() + ".");
                            } else if (Objects.equals(args[3], "history")) {
                                //TODO could remove based off key word
                                List<String> notesHistory = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));

                                notesHistory.removeIf(n -> n.contains("CONSOLE>"));

                                this.historyConfig.getConfig().set(uuid + ".notes", notesHistory);
                                this.historyConfig.save();
                                plugin.setHistoryConfig(this.historyConfig);
                                sender.sendMessage(ChatColor.GREEN + "Removed the chat history from notes of " + args[2].toLowerCase() + ".");
                            } else {
                                try {
                                    List<String> notesUpdated = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
                                    int num = Integer.parseInt(args[3]) - 1;
                                    notesUpdated.remove(num);
                                    this.historyConfig.getConfig().set(uuid + ".notes", notesUpdated);
                                    this.historyConfig.save();
                                    plugin.setHistoryConfig(this.historyConfig);
                                    sender.sendMessage(ChatColor.GREEN + "Removed note " + args[3].toLowerCase() + " for " + args[2].toLowerCase() + ".");
                                } catch (Exception e) {
                                    sender.sendMessage(ChatColor.AQUA + "Please enter a valid number of the note you want to remove.");
                                }
                            }
                            break;
                        case "strikes":
                            if (args.length >= 4) {
                                switch (args[3].toLowerCase()) {
                                    case "clear":
                                        if (args.length == 4) {
                                            for (String tier : this.plugin.getGroupList()) {
                                                this.historyConfig.getConfig().set(uuid + "." + tier + ".count", 0);
                                            }
                                            sender.sendMessage(ChatColor.GREEN + "Removed all of the strikes for " + args[2].toLowerCase() + ".");
                                        } else if (this.plugin.getGroupList().contains(args[4].toLowerCase())) {
                                            plugin.getHistoryConfig().getConfig().set(uuid + "." + args[4].toLowerCase() + ".count", 0);
                                            plugin.getHistoryConfig().save();
                                            sender.sendMessage(ChatColor.GREEN + "Strikes removed for " + args[2].toLowerCase() + ".");
                                        } else {
                                            sender.sendMessage(ChatColor.AQUA + "Clear can only be followed by one of the filter groups.");
                                        }
                                        this.historyConfig.save();
                                        plugin.setHistoryConfig(this.historyConfig);
                                        break;
                                    case "set":
                                        if (plugin.getGroupList().contains(args[4].toLowerCase())) {
                                            try {
                                                int strikes = Integer.parseInt(args[5]);
                                                plugin.getHistoryConfig().getConfig().set(uuid + "." + args[4].toLowerCase() + ".count", strikes);
                                                plugin.getHistoryConfig().save();
                                                sender.sendMessage(ChatColor.GREEN + "Strikes for " + args[2].toLowerCase() + " set to " + strikes + ".");
                                            } catch (Exception e) {
                                                sender.sendMessage(ChatColor.AQUA + "Must enter a number.");
                                            }
                                        } else {
                                            sender.sendMessage(ChatColor.AQUA + "Not a valid filter group.");
                                        }

                                        break;
                                    default:
                                        sender.sendMessage(ChatColor.AQUA + "The commands for strikes are clear and max.  To show strikes count simply enter the username.");
                                        break;
                                }
                            } else {
                                sender.sendMessage(ChatColor.AQUA + "Strikes: ");
                                for (String tier : this.plugin.getGroupList()) {
                                    int cnt = (historyConfig.getConfig().getString(uuid + "." + tier + ".count") == null) ? 0 : historyConfig.getConfig().getInt(uuid + "." + tier + ".count");
                                    sender.sendMessage("  " + tier + ": " + cnt);
                                }
                            }
                            break;
                        default:
                            sender.sendMessage(ChatColor.AQUA + "The notes commands are: list, add, remove, and strikes.");
                            break;
                    }
                    break;
                case "spam":
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.AQUA + "The options are: on and off.");
                        return true;
                    }
                    if (Objects.equals(args[1].toLowerCase(), "on") || Objects.equals(args[1].toLowerCase(), "off")) {
                        Boolean spamState = args[1].equals("on");
                        this.filterConfig.getConfig().set("spam.enabled", spamState);
                        this.filterConfig.save();
                        plugin.setFilterConfig(filterConfig);
                        sender.sendMessage(ChatColor.GREEN + "Spam detection turned " + args[1].toLowerCase() + ".");
                    } else {
                        sender.sendMessage(ChatColor.AQUA + "The options are: on and off.");
                    }
                    break;
                case "bot":
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.AQUA + "The options are: on and off.");
                        return true;
                    }
                    if (Objects.equals(args[1].toLowerCase(), "on") || Objects.equals(args[1].toLowerCase(), "off")) {
                        Boolean botState = args[1].equals("on");
                        this.filterConfig.getConfig().set("bot.enabled", botState);
                        this.filterConfig.save();
                        plugin.setFilterConfig(filterConfig);
                        sender.sendMessage(ChatColor.GREEN + "Bot detection turned " + args[1].toLowerCase() + ".");
                    } else {
                        sender.sendMessage(ChatColor.AQUA + "The options are: on and off.");
                    }
                    break;
                case "speed":
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.AQUA + "The options are: normal, chill, slow, ice..");
                        return true;
                    }
                    if (Objects.equals(args[1].toLowerCase(), "normal") || Objects.equals(args[1].toLowerCase(), "chill") || Objects.equals(args[1].toLowerCase(), "slow") || Objects.equals(args[1].toLowerCase(), "ice")) {
                        this.filterConfig.getConfig().set("chatSpeed.mode", args[1].toLowerCase());
                        this.filterConfig.save();
                        plugin.setFilterConfig(this.filterConfig);
                        sender.sendMessage(ChatColor.GREEN + "Chat speed set to " + args[1].toLowerCase());
                    } else {
                        sender.sendMessage(ChatColor.AQUA + "The options are: normal, chill, slow, ice..");
                    }
                    break;
                case "create":
                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.AQUA + "Must enter a name for the new filter group.");
                        return true;
                    } else if (args.length > 2) {
                        sender.sendMessage(ChatColor.AQUA + "Must only enter the name of the new filter group.");
                        return true;
                    }
                    Set<String> cmds = new HashSet<>(Arrays.asList("on", "off", "mode", "admin", "history", "strikes", "notes", "spam", "bot", "speed", "create", "remove", "groups", "timeout"));
                    if (cmds.contains(args[1].toLowerCase())) {
                        sender.sendMessage(ChatColor.AQUA + "Please choose a name that is not also a filter command.");
                        return true;
                    }

                    String group = "groups." + args[1].toLowerCase() + ".";
                    this.filterConfig.getConfig().set(group + "enabled", false);
                    this.filterConfig.getConfig().set(group + "level", plugin.getGroupList().size() + 1);
                    this.filterConfig.getConfig().set(group + "partialMatches", false);
                    this.filterConfig.getConfig().set(group + "mode", "replace");
                    this.filterConfig.getConfig().set(group + "replaceWith", new ArrayList<>());
                    this.filterConfig.getConfig().set(group + "msgToAdmin", "[senderName] triggered " + args[1].toLowerCase() + " filter.");
                    this.filterConfig.getConfig().set(group + "msgToAdminEnabled", false);
                    this.filterConfig.getConfig().set(group + "commands", new ArrayList<>());
                    this.filterConfig.getConfig().set(group + "history", false);
                    this.filterConfig.getConfig().set(group + "maxStrikes", -1);
                    this.filterConfig.getConfig().set(group + "strikeActions", new ArrayList<>());
                    this.filterConfig.getConfig().set(group + "regex", new ArrayList<>());

                    this.filterConfig.save();
                    plugin.setFilterConfig(this.filterConfig);

                    this.wordListConfig.getConfig().set(args[1].toLowerCase(), new ArrayList<>());
                    this.wordListConfig.save();
                    plugin.setWordList(this.wordListConfig);

                    plugin.initGroupList();
                    sender.sendMessage(ChatColor.GREEN + "New filter group " + args[1].toLowerCase() + " created!  Now to edit the options.");

                    break;
                case "remove":
                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.AQUA + "Must enter the name of filter group you want to remove.");
                        return true;
                    } else if (args.length > 2) {
                        sender.sendMessage(ChatColor.AQUA + "Must only enter the name of the new filter group.");
                        return true;
                    }
                    if (this.filterConfig.getConfig().contains("groups." + args[1].toLowerCase())) {
                        this.filterConfig.getConfig().set("groups." + args[1].toLowerCase(), null);
                        this.filterConfig.save();
                        plugin.setFilterConfig(this.filterConfig);

                        plugin.initGroupList();
                        sender.sendMessage(ChatColor.GREEN + "Filter group " + args[1].toLowerCase() + " removed.");
                    } else {
                        sender.sendMessage(ChatColor.AQUA + "Not a valid filter group.");
                    }
                    if (this.wordListConfig.getConfig().contains(args[1].toLowerCase())) {
                        this.wordListConfig.getConfig().set(args[1].toLowerCase(), null);
                        this.wordListConfig.save();
                        plugin.setWordList(this.wordListConfig);
                    }
                    break;
                case "groups":
                    if (args.length != 1) {
                        sender.sendMessage(ChatColor.AQUA + "Not a valid command.");
                        return true;
                    }
                    sender.sendMessage(ChatColor.AQUA + "Enabled filter groups ordered by level: ");
                    for (String g : plugin.getGroupList()) {
                        sender.sendMessage(" -" + g);
                    }
                    break;
                case "timeout":
                    if (args.length != 4) {
                        sender.sendMessage(ChatColor.AQUA + "In order to time someone out use the format: username [time] m/min/s/sec.");
                        return true;
                    }
                    UUID touuid = null;
                    try {
                        touuid = Objects.requireNonNull(Bukkit.getPlayer(args[1])).getUniqueId();
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.AQUA + "Please enter a valid username.");
                        return true;
                    }
                    long time;
                    try {
                        time = Long.parseLong(args[2]);
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.AQUA + "Must enter a time in numbers.");
                        return true;
                    }
                    String var = args[3];
                    if (!Objects.equals(var, "m") && !Objects.equals(var, "min") && !Objects.equals(var, "s") && !Objects.equals(var, "sec")) {
                        sender.sendMessage(ChatColor.AQUA + "For minutes use m or min.  For seconds use s or sec.");
                        return true;
                    }

                    this.timeout(touuid, time, var);
                    Bukkit.getPlayer(touuid).sendMessage(ChatColor.RED + "You have been timed out for " + time + " " + var);
                    break;
                case "partial":
                    switch (args[1].toLowerCase()) {
                        case "on":
                        case "off":
                            Boolean partBool = args[1].equals("on");
                            for (String tier : this.plugin.getGroupList()) {
                                this.filterConfig.getConfig().set("groups." + tier + ".partialMatches", partBool);
                                plugin.getRegexBuilder().buildRegex(tier);
                            }
                            plugin.setFilterConfig(filterConfig);
                            sender.sendMessage(ChatColor.GREEN + "Partial matches turned " + args[1].toLowerCase() + ".");
                            break;
                        default:
                            sender.sendMessage(ChatColor.AQUA + "Must select on or off.");
                            break;
                    }
                    break;
                default:
                    if (plugin.getGroupList().contains(args[0].toLowerCase())) break;
                    sender.sendMessage(ChatColor.AQUA + "The options for the filter command are: on/off, mode, admin, notes, spam, bot, speed, swears/slurs.");
                    sender.sendMessage(ChatColor.AQUA + "Plugin created by Mithraea and DeathsValentine");
                    break;
            }
            if (plugin.getGroupList().contains(args[0].toLowerCase())) {
                try {
                    switch (args[1].toLowerCase()) {
                        case "on":
                        case "off":
                            Boolean filterState = args[1].equalsIgnoreCase("on");
                            this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".enabled", filterState);
                            plugin.setFilterConfig(filterConfig);
                            sender.sendMessage(ChatColor.GREEN + "Filter for " + args[0].toLowerCase() + " turned " + args[1].toLowerCase() + ".");
                            break;
                        case "mode":
                            if (args[2].equalsIgnoreCase("replace") || args[2].equalsIgnoreCase("censor") || args[2].equalsIgnoreCase("clear")) {
                                this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".mode", args[2].toLowerCase());
                                plugin.setFilterConfig(filterConfig);
                                sender.sendMessage(ChatColor.GREEN + "Filter mode set to " + args[2].toLowerCase() + " for " + args[0].toLowerCase() + ".");
                            } else {
                                sender.sendMessage(ChatColor.AQUA + "The three selectable modes are: replace, censor, clear.");
                            }
                            break;
                        case "commands":
                            switch (args[2].toLowerCase()) {
                                case "list":
                                    List<String> commandList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + args[0].toLowerCase() + ".commands"));
                                    sender.sendMessage(ChatColor.AQUA + "Filter commands for " + args[0].toLowerCase() + ":");
                                    for (int i = 0; i < commandList.size(); i++) {
                                        int num = i + 1;
                                        sender.sendMessage(num + ": " + commandList.get(i));
                                    }
                                    break;
                                case "add":
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
                                    break;
                                case "remove":
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
                                    break;
                                default:
                                    sender.sendMessage(ChatColor.AQUA + "The options for commands are: list, add, and remove..");
                                    break;
                            }
                            break;
                        case "admin":
                            switch (args[2].toLowerCase()) {
                                case "on":
                                case "off":
                                    Boolean msgState = args[2].equals("on");
                                    this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".msgToAdminEnabled", msgState);
                                    plugin.setFilterConfig(filterConfig);
                                    sender.sendMessage(ChatColor.GREEN + "Admin message for " + args[0].toLowerCase() + " turned " + args[2].toLowerCase() + ".");
                                    break;
                                case "current":
                                    sender.sendMessage(ChatColor.AQUA + "Current admin message for " + args[0].toLowerCase() + ":");
                                    sender.sendMessage(Objects.requireNonNull(this.filterConfig.getConfig().getString("groups." + args[0].toLowerCase() + ".msgToAdmin")));
                                    break;
                                case "edit":
                                    StringBuilder cmd = new StringBuilder();
                                    for (int i = 3; i < args.length; i++) {
                                        cmd.append(args[i]);
                                        if (i != args.length - 1) cmd.append(" ");
                                    }
                                    this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".msgToAdmin", cmd.toString());
                                    plugin.setFilterConfig(this.filterConfig);
                                    sender.sendMessage(ChatColor.GREEN + "Admin message for " + args[0].toLowerCase() + " changed.");
                                    break;
                                default:
                                    sender.sendMessage(ChatColor.AQUA + "The options for admin are: current, edit, and on/off.");
                                    break;
                            }
                            break;
                        case "replace":
                            switch (args[2].toLowerCase()) {
                                case "list":
                                    sender.sendMessage(ChatColor.AQUA + "The current replacement messages for " + args[0].toLowerCase() + " are: ");
                                    List<String> replaceList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + args[0].toLowerCase() + ".replaceWith"));
                                    if (replaceList.isEmpty()) {
                                        sender.sendMessage(ChatColor.AQUA + "No replacement messages currently set.");
                                    }
                                    for (int i = 0; i < replaceList.size(); i++) {
                                        int num = i + 1;
                                        sender.sendMessage(num + ": " + replaceList.get(i));
                                    }
                                    break;
                                case "add":
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
                                    break;
                                case "remove":
                                    try {
                                        List<String> removeList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + args[0].toLowerCase() + ".replaceWith"));
                                        int num = Integer.parseInt(args[3]) - 1;
                                        removeList.remove(num);
                                        this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".replaceWith", removeList);
                                        plugin.setFilterConfig(this.filterConfig);
                                        sender.sendMessage(ChatColor.GREEN + "Removed replacement message for " + args[0].toLowerCase() + ".");
                                    } catch (Exception e) {
                                        sender.sendMessage(ChatColor.AQUA + "Please enter the number of the replacement message you want to remove.");
                                        return true;
                                    }
                                    break;
                                default:
                                    sender.sendMessage(ChatColor.AQUA + "The possible replace commands are: list, add, and remove.");
                                    break;
                            }
                            break;
                        case "strikes":
                            switch (args[2].toLowerCase()) {
                                case "max":
                                    int strikes = Integer.parseInt(args[3]);
                                    if (strikes <= 0) {
                                        sender.sendMessage(ChatColor.AQUA + "Max strikes cannot be 0 or below.  To turn strikes off enter 'off.'");
                                        return true;
                                    }
                                    this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".maxStrikes", strikes);
                                    this.filterConfig.save();
                                    plugin.setFilterConfig(this.filterConfig);
                                    break;
                                case "off":
                                    this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".maxStrikes", -1);
                                    this.filterConfig.save();
                                    plugin.setFilterConfig(this.filterConfig);
                                    break;
                                case "commands":
                                    switch (args[3].toLowerCase()) {
                                        case "list":
                                            List<String> commandList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + args[0].toLowerCase() + ".strikeActions"));
                                            sender.sendMessage(ChatColor.AQUA + "Filter commands for " + args[0].toLowerCase() + ":");
                                            for (int i = 0; i < commandList.size(); i++) {
                                                int num = i + 1;
                                                sender.sendMessage(num + ": " + commandList.get(i));
                                            }
                                            break;
                                        case "add":
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
                                            break;
                                        case "remove":
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
                            switch (args[2]) {
                                case "on":
                                case "off":
                                    Boolean state = args[2].equals("on");
                                    this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".history", state);
                                    plugin.setFilterConfig(filterConfig);
                                    sender.sendMessage(ChatColor.GREEN + "History for " + args[0].toLowerCase() + " turned " + args[0].toLowerCase() + ".");
                                    break;
                                default:
                                    sender.sendMessage(ChatColor.AQUA + "The commands for history are on and off.");
                                    break;
                            }
                            break;
                        case "add":
                            if (args.length != 3) {
                                sender.sendMessage(ChatColor.AQUA + "Must enter just the word to add.");
                                return true;
                            }

                            List<String> wordList = this.wordListConfig.getConfig().getStringList(args[0].toLowerCase());
                            wordList.add(args[2].toLowerCase());
                            this.wordListConfig.getConfig().set(args[0].toLowerCase(), wordList);
                            this.wordListConfig.save();
                            plugin.setWordList(this.wordListConfig);
                            plugin.getRegexBuilder().buildRegex(args[0].toLowerCase());
                            sender.sendMessage(ChatColor.GREEN + "Added " + args[2].toLowerCase() + " to " + args[0].toLowerCase() + " filter list.");
                            break;
                        case "remove":
                            if (args.length != 3) {
                                sender.sendMessage(ChatColor.AQUA + "Must enter just the word to remove.");
                                return true;
                            }
                            List<String> wList = this.wordListConfig.getConfig().getStringList(args[0].toLowerCase());
                            if (!wList.contains(args[2].toLowerCase())) {
                                sender.sendMessage(ChatColor.AQUA + "Entered word not in " + args[0].toLowerCase() + " filter list.");
                                return true;
                            }

                            wList.remove(args[2].toLowerCase());

                            this.wordListConfig.getConfig().set(args[0].toLowerCase(), wList);
                            this.wordListConfig.save();
                            plugin.setWordList(this.wordListConfig);
                            plugin.getRegexBuilder().buildRegex(args[0].toLowerCase());
                            sender.sendMessage(ChatColor.GREEN + "Removed " + args[2].toLowerCase() + " from " + args[0].toLowerCase() + " filter list.");
                            break;
                        case "partial":
                            switch (args[2]) {
                                case "on":
                                case "off":
                                    Boolean state = args[2].equals("on");
                                    this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".partialMatches", state);
                                    plugin.setFilterConfig(filterConfig);
                                    plugin.getRegexBuilder().buildRegex(args[0].toLowerCase());
                                    sender.sendMessage(ChatColor.GREEN + "Partial matches for " + args[0].toLowerCase() + " turned " + args[1].toLowerCase() + ".");
                                    break;
                                default:
                                    sender.sendMessage(ChatColor.AQUA + "The commands for partial history are on and off.");
                                    break;
                            }
                            break;
                        default:
                            sender.sendMessage(ChatColor.AQUA + "The possible options are: on/off, mode, replace, commands, and admin.");
                            break;

                    }
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.AQUA + "Must enter a full command");
                    return true;
                }
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.AQUA + "Not a valid command.");
        }
        return true;
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
        plugin.getTimeoutMap().put(uuid, until);
    }
}
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
    private final ConfigUtil filterConfig;
    private ConfigUtil historyConfig;
    private final List<String> groups;

    public Filter(ChatFilterMC plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.filterConfig = plugin.getFilterConfig();
        this.historyConfig = new ConfigUtil(plugin, "History.yml");
        this.groups = plugin.getGroupList();
    }

    //TODO
    // Decide if there should be separate files for regex and config stuff
    // Add command to create whole new tier
    // Timeout individual player
    // Figure out if possible to change final groups command to switch
    // Add catches for if they don't put in enough args because it just throws an error
    // Change staff to admin (?)

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //TODO Probs dont need this
        if (!sender.hasPermission("filter") || !sender.isOp()) return true;

        switch (args[0].toLowerCase()) {
            case "on":
            case "off":
                Boolean state = args[0].equals("on");
                for (String tier : this.groups) {
                    this.filterConfig.getConfig().set("groups." + tier + ".enabled", state);
                }
                plugin.setFilterConfig(filterConfig);
                sender.sendMessage(ChatColor.AQUA + "Filter turned " + args[0].toLowerCase() + ".");
                break;
            case "mode":
                if (args[1].equalsIgnoreCase("replace") || args[1].equalsIgnoreCase("censor") || args[1].equalsIgnoreCase("clear")) {
                    for (String tier : this.groups) {
                        this.filterConfig.getConfig().set("groups." + tier + ".mode", args[1].toLowerCase());
                    }
                    plugin.setFilterConfig(filterConfig);
                    sender.sendMessage(ChatColor.AQUA + "Filter mode set to " + args[1].toLowerCase() + ".");
                } else {
                    sender.sendMessage(ChatColor.AQUA + "The three selectable modes are: Replace, Censor, Clear.");
                }
                break;
            case "staff":
                switch (args[1].toLowerCase()) {
                    case "on":
                    case "off":
                        Boolean staffBool = args[1].equals("on");
                        for (String tier : this.groups) {
                            this.filterConfig.getConfig().set("groups." + tier + ".msgToStaffEnabled", staffBool);
                        }
                        plugin.setFilterConfig(filterConfig);
                        sender.sendMessage(ChatColor.AQUA + "Admin messages turned " + args[1].toLowerCase() + ".");
                        break;
                    case "current":
                        for (String tier : this.groups) {
                            sender.sendMessage(ChatColor.AQUA + "Current " + tier + " staff message: " + ChatColor.WHITE + this.filterConfig.getConfig().getString("groups." + tier + ".msgToStaff"));
                        }
                        break;
                    case "edit":
                        StringBuilder cmd = new StringBuilder();
                        for (int i = 2; i < args.length; i++) {
                            cmd.append(args[i]);
                            if (i != args.length - 1) cmd.append(" ");
                        }
                        for (String tier : this.groups) {
                            this.filterConfig.getConfig().set("groups." + tier + ".msgToStaff", cmd.toString());
                        }
                        plugin.setFilterConfig(this.filterConfig);
                        sender.sendMessage(ChatColor.AQUA + "Changed admin message to: \"" + cmd + ".\"");
                        break;
                    default:
                        sender.sendMessage(ChatColor.AQUA + "The staff commands are: on/off and edit.");
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
                //TODO check if this is needed
                // or if i also have to do this for strikes, etc
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
                        sender.sendMessage(ChatColor.AQUA + "Notes for " + args[2] + ":");
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
                        sender.sendMessage(ChatColor.AQUA + "Note added for " + args[2] + ".");
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
                            sender.sendMessage(ChatColor.AQUA + "Removed all notes for " + args[2] + ".");
                        } else if (Objects.equals(args[3], "history")) {
                            //TODO could remove based off key word
                            List<String> notesHistory = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));

                            notesHistory.removeIf(n -> n.contains("CONSOLE>"));

                            this.historyConfig.getConfig().set(uuid + ".notes", notesHistory);
                            this.historyConfig.save();
                            plugin.setHistoryConfig(this.historyConfig);
                            sender.sendMessage(ChatColor.AQUA + "Removed the chat history from notes of " + args[2] + ".");
                        } else {
                            //TODO could use regex to detect if int
                            try {
                                List<String> notesUpdated = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
                                int num = Integer.parseInt(args[3]) - 1;
                                notesUpdated.remove(num);
                                this.historyConfig.getConfig().set(uuid + ".notes", notesUpdated);
                                this.historyConfig.save();
                                plugin.setHistoryConfig(this.historyConfig);
                                sender.sendMessage(ChatColor.AQUA + "Removed note " + args[3] + " for " + args[2] + ".");
                            } catch (Exception e) {
                                sender.sendMessage(ChatColor.AQUA + "Please enter a valid number of the note you want to remove.");
                            }
                        }
                        break;
                    case "strikes":
                        //TODO figure out best order of command... currently /filter notes strike [player] clear swears, etc...
                        // possibly add set command to manually set number of strikes
                        // Make sure args[2] is a username
                        if (args.length >= 4) {
                            if (args[3].equalsIgnoreCase("clear")) {
                                if (args.length == 4) {
                                    for (String tier : this.groups) {
                                        this.historyConfig.getConfig().set(uuid + "." + tier + ".count", 0);
                                    }
                                    sender.sendMessage(ChatColor.AQUA + "Removed all of the strikes for " + args[2] + ".");
                                } else if (this.groups.contains(args[4].toLowerCase())) {
                                    this.historyConfig.getConfig().set(uuid + "." + args[4].toLowerCase() + ".count", 0);
                                    sender.sendMessage(ChatColor.AQUA + "Strikes removed for " + args[2] + ".");
                                } else {
                                    sender.sendMessage(ChatColor.AQUA + "Clear can only be followed by one of the filter groups.");
                                }
                                this.historyConfig.save();
                                plugin.setHistoryConfig(this.historyConfig);
                            } else {
                                sender.sendMessage(ChatColor.AQUA + "To display strike counts simply enter the username of the person.  To clear strikes add clear after.");
                            }
                        } else {
                            sender.sendMessage(ChatColor.AQUA + "Strikes: ");
                            for (String tier : this.groups) {
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
                    sender.sendMessage(ChatColor.AQUA + "Spam detection turned " + args[1].toLowerCase() + ".");
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
                    sender.sendMessage(ChatColor.AQUA + "Bot detection set " + args[1].toLowerCase() + ".");
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
                    plugin.setFilterConfig(filterConfig);
                    sender.sendMessage(ChatColor.AQUA + "Chat speed set to " + args[1].toLowerCase());
                } else {
                    sender.sendMessage(ChatColor.AQUA + "The options are: normal, chill, slow, ice..");
                }
                break;

//            case "swears": case "slurs":
//                //TODO implement this with groups instead of just swears/slurs... if cant figure out switch: if (groups.contains(args[0])
//                try {
//                    switch (args[1].toLowerCase()) {
//                        case "on": case "off":
//                            Boolean filterState = args[1].equalsIgnoreCase("on");
//                            //TODO fix this switch... its clunky... can just do what i did in mode below instead of switch
//                            switch (args[0].toLowerCase()) {
//                                case "swears":
//                                    this.filterConfig.getConfig().set("groups.swears.enabled", filterState);
//                                    plugin.setFilterConfig(filterConfig);
//                                    sender.sendMessage(ChatColor.AQUA + "Swear filter turned " + args[1].toLowerCase() + ".");
//                                    break;
//                                case "slurs":
//                                    this.filterConfig.getConfig().set("groups.slurs.enabled", filterState);
//                                    plugin.setFilterConfig(filterConfig);
//                                    sender.sendMessage(ChatColor.AQUA + "Slur filter turned " + args[1].toLowerCase() + ".");
//                                    break;
//                                default:
//                                    sender.sendMessage(ChatColor.AQUA + "The options are: on and off.");
//                                    break;
//                            }
//                            break;
//                        case "mode":
//                            if (args[2].equalsIgnoreCase("replace") || args[2].equalsIgnoreCase("censor") || args[2].equalsIgnoreCase("clear")) {
//                                this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".mode", args[2].toLowerCase());
//                                plugin.setFilterConfig(filterConfig);
//                                sender.sendMessage(ChatColor.AQUA + "Filter mode set to " + args[2].toLowerCase() + " for " + args[0].toLowerCase() + ".");
//                            } else {
//                                sender.sendMessage(ChatColor.AQUA + "The three selectable modes are: replace, censor, clear.");
//                            }
//                            break;
//                        case "commands":
//                            switch (args[2].toLowerCase()) {
//                                case "list":
//                                    List<String> commandList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".commands"));
//                                    sender.sendMessage(ChatColor.AQUA + "Filter commands for " + args[0].toLowerCase() + ":");
//                                    for (int i = 0; i < commandList.size(); i++) {
//                                        int num = i + 1;
//                                        sender.sendMessage(num + ": " + commandList.get(i));
//                                    }
//                                    break;
//                                case "add":
//                                    List<String> commandAddList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".commands"));
//                                    StringBuilder cmd = new StringBuilder();
//                                    for (int i = 3; i < args.length; i++) {
//                                        cmd.append(args[i]);
//                                        if (i != args.length - 1) cmd.append(" ");
//                                    }
//                                    commandAddList.add(cmd.toString());
//                                    this.filterConfig.getConfig().set(args[0].toLowerCase() + ".commands", commandAddList);
//                                    plugin.setFilterConfig(this.filterConfig);
//                                    sender.sendMessage(ChatColor.AQUA + "Added filter command for " + args[0].toLowerCase());
//                                    break;
//                                case "remove":
//                                    try {
//                                        List<String> commandRemoveList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".commands"));
//                                        int num = Integer.parseInt(args[3]) - 1;
//                                        commandRemoveList.remove(num);
//                                        this.filterConfig.getConfig().set(args[0].toLowerCase() + ".commands", commandRemoveList);
//                                        plugin.setFilterConfig(this.filterConfig);
//                                        sender.sendMessage(ChatColor.AQUA + "Removed filter command for " + args[0] + ".");
//                                    } catch (Exception e) {
//                                        sender.sendMessage(ChatColor.AQUA + "Please enter the number of the command you want to remove.");
//                                    }
//                                    break;
//                                default:
//                                    sender.sendMessage(ChatColor.AQUA + "The options for commands are: list, add, and remove..");
//                                    break;
//                            }
//                            break;
//                        case "staff":
//                            switch (args[2].toLowerCase()) {
//                                case "on":
//                                case "off":
//                                    Boolean msgState = args[2].equals("on");
//                                    this.filterConfig.getConfig().set("swears.msgToStaffEnabled", msgState);
//                                    plugin.setFilterConfig(filterConfig);
//                                    sender.sendMessage(ChatColor.AQUA + "Staff message for " + args[0].toLowerCase() + " turned " + args[2].toLowerCase() + ".");
//                                    break;
//                                case "current":
//                                    sender.sendMessage(ChatColor.AQUA + "Current staff message for " + args[0].toLowerCase() + ":");
//                                    sender.sendMessage(Objects.requireNonNull(this.filterConfig.getConfig().getString(args[0].toLowerCase() + ".msgToStaff")));
//                                    break;
//                                case "edit":
//                                    StringBuilder cmd = new StringBuilder();
//                                    for (int i = 3; i < args.length; i++) {
//                                        cmd.append(args[i]);
//                                        if (i != args.length - 1) cmd.append(" ");
//                                    }
//                                    this.filterConfig.getConfig().set(args[0].toLowerCase() + ".msgToStaff", cmd.toString());
//                                    plugin.setFilterConfig(this.filterConfig);
//                                    sender.sendMessage(ChatColor.AQUA + "Staff message for " + args[0].toLowerCase() + " changed.");
//                                    break;
//                                default:
//                                    sender.sendMessage(ChatColor.AQUA + "The options for staff are: current, edit, and on/off.");
//                                    break;
//                            }
//                            break;
//                        case "replace":
//                            switch (args[2].toLowerCase()) {
//                                case "list":
//                                    sender.sendMessage(ChatColor.AQUA + "The current replacement messages for " + args[0].toLowerCase() + " are: ");
//                                    List<String> replaceList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".replaceWith"));
//                                    if (replaceList.isEmpty()) {
//                                        sender.sendMessage(ChatColor.AQUA + "No replacement messages currently set.");
//                                    }
//                                    for (int i = 0; i < replaceList.size(); i++) {
//                                        int num = i + 1;
//                                        sender.sendMessage(num + ": " + replaceList.get(i));
//                                    }
//                                    break;
//                                case "add":
//                                    List<String> addList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".replaceWith"));
//                                    StringBuilder msg = new StringBuilder();
//                                    for (int i = 3; i < args.length; i++) {
//                                        msg.append(args[i]);
//                                        if (i != args.length - 1) msg.append(" ");
//                                    }
//                                    addList.add(msg.toString());
//                                    this.filterConfig.getConfig().set(args[0].toLowerCase() + ".replaceWith", addList);
//                                    plugin.setFilterConfig(this.filterConfig);
//                                    sender.sendMessage(ChatColor.AQUA + "Added replacement message for " + args[0] + ".");
//                                    break;
//                                case "remove":
//                                    try {
//                                        List<String> removeList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".replaceWith"));
//                                        int num = Integer.parseInt(args[3]) - 1;
//                                        removeList.remove(num);
//                                        this.filterConfig.getConfig().set(args[0].toLowerCase() + ".replaceWith", removeList);
//                                        plugin.setFilterConfig(this.filterConfig);
//                                        sender.sendMessage(ChatColor.AQUA + "Removed replacement message for " + args[0] + ".");
//                                    } catch (Exception e) {
//                                        sender.sendMessage(ChatColor.AQUA + "Please enter the number of the replacement message you want to remove.");
//                                        return true;
//                                    }
//                                    break;
//                                default:
//                                    sender.sendMessage(ChatColor.AQUA + "The possible replace commands are: list, add, and remove.");
//                                    break;
//                            }
//                            break;
//                        default:
//                            sender.sendMessage(ChatColor.AQUA + "The possible options are: on/off, mode, replace, commands, and staff.");
//                            break;
//
//                    }
//                    break;
//                } catch (Exception e) {
//                    sender.sendMessage(ChatColor.AQUA + "Must enter a full command");
//                    return true;
//                }
            default:
                if (groups.contains(args[0].toLowerCase())) break;
                sender.sendMessage(ChatColor.AQUA + "The options for the filter command are: on/off, mode, staff, notes, spam, bot, speed, swears/slurs.");
                sender.sendMessage(ChatColor.AQUA + "Plugin created by Mithraea and DeathsValentine");
                break;
        }
        if (groups.contains(args[0].toLowerCase())) {
            //TODO add history on/off command
            // add strikes count command
            // add strikes actions
            try {
                switch (args[1].toLowerCase()) {
                    case "on": case "off":
                        Boolean filterState = args[1].equalsIgnoreCase("on");
                        this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".enabled", filterState);
                        plugin.setFilterConfig(filterConfig);
                        sender.sendMessage(ChatColor.AQUA + "Filter for " + args[0].toLowerCase() + " turned " + args[1].toLowerCase() + ".");
                        break;
                    case "mode":
                        if (args[2].equalsIgnoreCase("replace") || args[2].equalsIgnoreCase("censor") || args[2].equalsIgnoreCase("clear")) {
                            this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".mode", args[2].toLowerCase());
                            plugin.setFilterConfig(filterConfig);
                            sender.sendMessage(ChatColor.AQUA + "Filter mode set to " + args[2].toLowerCase() + " for " + args[0].toLowerCase() + ".");
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
                                sender.sendMessage(ChatColor.AQUA + "Added filter command for " + args[0].toLowerCase());
                                break;
                            case "remove":
                                try {
                                    List<String> commandRemoveList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + args[0].toLowerCase() + ".commands"));
                                    int num = Integer.parseInt(args[3]) - 1;
                                    commandRemoveList.remove(num);
                                    this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".commands", commandRemoveList);
                                    plugin.setFilterConfig(this.filterConfig);
                                    sender.sendMessage(ChatColor.AQUA + "Removed filter command for " + args[0] + ".");
                                } catch (Exception e) {
                                    sender.sendMessage(ChatColor.AQUA + "Please enter the number of the command you want to remove.");
                                }
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
                                Boolean msgState = args[2].equals("on");
                                this.filterConfig.getConfig().set("groups." + args[0] + ".msgToStaffEnabled", msgState);
                                plugin.setFilterConfig(filterConfig);
                                sender.sendMessage(ChatColor.AQUA + "Staff message for " + args[0].toLowerCase() + " turned " + args[2].toLowerCase() + ".");
                                break;
                            case "current":
                                sender.sendMessage(ChatColor.AQUA + "Current staff message for " + args[0].toLowerCase() + ":");
                                sender.sendMessage(Objects.requireNonNull(this.filterConfig.getConfig().getString("groups." + args[0].toLowerCase() + ".msgToStaff")));
                                break;
                            case "edit":
                                StringBuilder cmd = new StringBuilder();
                                for (int i = 3; i < args.length; i++) {
                                    cmd.append(args[i]);
                                    if (i != args.length - 1) cmd.append(" ");
                                }
                                this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".msgToStaff", cmd.toString());
                                plugin.setFilterConfig(this.filterConfig);
                                sender.sendMessage(ChatColor.AQUA + "Staff message for " + args[0].toLowerCase() + " changed.");
                                break;
                            default:
                                sender.sendMessage(ChatColor.AQUA + "The options for staff are: current, edit, and on/off.");
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
                                sender.sendMessage(ChatColor.AQUA + "Added replacement message for " + args[0] + ".");
                                break;
                            case "remove":
                                try {
                                    List<String> removeList = new ArrayList<>(this.filterConfig.getConfig().getStringList("groups." + args[0].toLowerCase() + ".replaceWith"));
                                    int num = Integer.parseInt(args[3]) - 1;
                                    removeList.remove(num);
                                    this.filterConfig.getConfig().set("groups." + args[0].toLowerCase() + ".replaceWith", removeList);
                                    plugin.setFilterConfig(this.filterConfig);
                                    sender.sendMessage(ChatColor.AQUA + "Removed replacement message for " + args[0] + ".");
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
                    default:
                        sender.sendMessage(ChatColor.AQUA + "The possible options are: on/off, mode, replace, commands, and staff.");
                        break;

                }
            } catch (Exception e) {
                sender.sendMessage(ChatColor.AQUA + "Must enter a full command");
                return true;
            }
        }

//        if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off")) {
//            Boolean state = args[0].equals("on");
//            this.filterConfig.getConfig().set("swears.enabled", state);
//            this.filterConfig.getConfig().set("slurs.enabled", state);
//            plugin.setFilterConfig(filterConfig);
//        } else if (args[0].equalsIgnoreCase("mode")) {
//            if (args[1].equalsIgnoreCase("replace") || args[1].equalsIgnoreCase("censor") || args[1].equalsIgnoreCase("clear")) {
//                this.filterConfig.getConfig().set("swears.mode", args[1]);
//                this.filterConfig.getConfig().set("slurs.mode", args[1]);
//                plugin.setFilterConfig(filterConfig);
//            } else {
//                sender.sendMessage(ChatColor.AQUA + "The three selectable modes are: Replace, Censor, Clear");
//            }
//        } else if (args[0].equalsIgnoreCase("staff")) {
//            if (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off")) {
//                this.filterConfig.getConfig().set("swears.msgToStaffEnabled", args[1]);
//                this.filterConfig.getConfig().set("slurs.msgToStaffEnabled", args[1]);
//                plugin.setFilterConfig(filterConfig);
//            } else if (args[1].equalsIgnoreCase("edit")) {
//                StringBuilder cmd = new StringBuilder();
//                for (int i = 3; i < args.length; i++) {
//                    cmd.append(args[i]);
//                    if (i != args.length - 1) cmd.append(" ");
//                }
//                this.filterConfig.getConfig().set("swears.msgToStaff", cmd.toString());
//                this.filterConfig.getConfig().set("slurs.msgToStaff", cmd.toString());
//                plugin.setFilterConfig(this.filterConfig);
//            } else {
//                sender.sendMessage(ChatColor.AQUA + "The staff commands are: on/off and edit.");
//            }
//        } else if (args[0].equalsIgnoreCase("notes")) {
//            if (args.length < 3) {
//                sender.sendMessage(ChatColor.AQUA + "Must select an option and use a valid username.");
//                return true;
//            }
//            UUID uuid = null;
//            try {
//                uuid = Objects.requireNonNull(Bukkit.getPlayer(args[2])).getUniqueId();
//            } catch (Exception e) {
//                sender.sendMessage(ChatColor.AQUA + "Please enter a valid username.");
//            }
//
//            if (uuid == null) {
//                return true;
//            }
//            this.historyConfig = plugin.getHistoryConfig();
//            //TODO check if this is needed
//            // or if i also have to do this for strikes, etc
//            if (!this.historyConfig.getConfig().contains(uuid + ".notes")) {
//                this.historyConfig.getConfig().set(uuid + ".notes", new ArrayList<String>());
//            }
//
//            if (args[1].equalsIgnoreCase("list")) {
//                List<String> notes = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
//                if (notes.isEmpty()) {
//                    sender.sendMessage(ChatColor.AQUA + "This player has no notes related to them!");
//                    return true;
//                }
//                for (int i = 0; i < notes.size(); i++) {
//                    int num = i + 1;
//                    sender.sendMessage(num + ": " + notes.get(i));
//                }
//            } else if (args[1].equalsIgnoreCase("add")) {
//                List<String> notes = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
//                StringBuilder note = new StringBuilder();
//                note.append(sender.getName()).append(": ");
//                for (int i = 3; i < args.length; i++) {
//                    note.append(args[i]);
//                    if (i != args.length - 1) note.append(" ");
//                }
//                notes.add(this.setCommand(uuid, note.toString()));
//                this.historyConfig.getConfig().set(uuid + ".notes", notes);
//                this.historyConfig.save();
//            } else if (args[1].equalsIgnoreCase("remove")) {
//                if (args.length == 3) {
//                    sender.sendMessage(ChatColor.AQUA + "Must enter a valid number, all, or history.");
//                    return true;
//                }
//                if (Objects.equals(args[3], "all")) {
//                    this.historyConfig.getConfig().set(uuid + ".notes", new ArrayList<String>());
//                    this.historyConfig.save();
//                    plugin.setHistoryConfig(this.historyConfig);
//                } else if (Objects.equals(args[3], "history")) {
//                    //TODO could remove based off key word
//                    List<String> notes = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
//
//                    notes.removeIf(note -> note.contains("CONSOLE:"));
//
//                    this.historyConfig.getConfig().set(uuid + ".notes", notes);
//                    this.historyConfig.save();
//                    plugin.setHistoryConfig(this.historyConfig);
//                } else {
//                    //TODO could use regex to detect if int
//                    try {
//                        List<String> notes = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
//                        int num = Integer.parseInt(args[3]) - 1;
//                        notes.remove(num);
//                        this.historyConfig.getConfig().set(uuid + ".notes", notes);
//                        this.historyConfig.save();
//                        plugin.setHistoryConfig(this.historyConfig);
//                    } catch (Exception e) {
//                        sender.sendMessage(ChatColor.AQUA + "Please enter a valid number of the note you want to remove.");
//                    }
//                }
//            } else if (args[1].equalsIgnoreCase("strikes")) {
//                //TODO figure out best order of command... currently /filter notes strike [player] clear swears, etc...
//                // possibly add set command to manually set number of strikes
//                if (args.length >= 4) {
//                    if (args[3].equalsIgnoreCase("clear")) {
//                        Bukkit.getLogger().info("Args length: " + args.length);
//                        if (args.length == 4) {
//                            Bukkit.getLogger().info("Args length is 4!!!");
//                            this.historyConfig.getConfig().set(uuid + ".slurs.count", 0);
//                            this.historyConfig.getConfig().set(uuid + ".swears.count", 0);
//                        } else if (args[4].equalsIgnoreCase("swears")) {
//                            Bukkit.getLogger().info("CLEAR SWEARS!!!");
//                            this.historyConfig.getConfig().set(uuid + ".swears.count", 0);
//                        } else if (args[4].equalsIgnoreCase("slurs")) {
//                            this.historyConfig.getConfig().set(uuid + ".slurs.count", 0);
//                        } else {
//                            sender.sendMessage(ChatColor.AQUA + "Clear can only be followed by 'slurs' or 'swears'.");
//                        }
//                        this.historyConfig.save();
//                        plugin.setHistoryConfig(this.historyConfig);
//                    } else {
//                        sender.sendMessage(ChatColor.AQUA + "To display strike counts simply enter the username of the person.  To clear strikes add clear after.");
//                    }
//                } else {
//                    int slurCnt = (historyConfig.getConfig().getString(uuid + ".slurs.count") == null) ? 0 : historyConfig.getConfig().getInt(uuid + ".slurs.count");
//                    int swearCnt = (historyConfig.getConfig().getString(uuid + ".swears.count") == null) ? 0 : historyConfig.getConfig().getInt(uuid + ".swears.count");
//                    sender.sendMessage("Slur strikes: " + slurCnt);
//                    sender.sendMessage("Swear strikes: " + swearCnt);
//                }
//            } else {
//                sender.sendMessage(ChatColor.AQUA + "The notes commands are: list, add, remove, and strikes.");
//            }

//        } else if (args[0].equalsIgnoreCase("spam")) {
//            if (Objects.equals(args[1], "on") || Objects.equals(args[1], "off")) {
//                Boolean state = args[1].equals("on");
//                this.filterConfig.getConfig().set("spam.enabled", state);
//                this.filterConfig.save();
//                plugin.setFilterConfig(filterConfig);
//            } else {
//                sender.sendMessage(ChatColor.AQUA + "The options are: on and off.");
//            }
//        } else if (args[0].equalsIgnoreCase("bot")) {
//            if (Objects.equals(args[1], "on") || Objects.equals(args[1], "off")) {
//                Boolean state = args[1].equals("on");
//                this.filterConfig.getConfig().set("bot.enabled", state);
//                this.filterConfig.save();
//                plugin.setFilterConfig(filterConfig);
//            } else {
//                sender.sendMessage(ChatColor.AQUA + "The options are: on and off.");
//            }
//        } else if (args[0].equalsIgnoreCase("speed")) {
//            if (Objects.equals(args[1], "normal") || Objects.equals(args[1], "chill") || Objects.equals(args[1], "slow") || Objects.equals(args[1], "ice")) {
//                this.filterConfig.getConfig().set("chatSpeed.mode", args[1]);
//                this.filterConfig.save();
//                plugin.setFilterConfig(filterConfig);
//            } else {
//                sender.sendMessage(ChatColor.AQUA + "The options are: normal, chill, slow, ice..");
//            }
//        } else if (args[0].equalsIgnoreCase("swears") || args[0].equalsIgnoreCase("slurs")) {
//            if (Objects.equals(args[1], "on") || Objects.equals(args[1], "off")) {
//                Boolean state = args[1].equals("on");
//                switch (args[0]) {
//                    case "swears":
//                        this.filterConfig.getConfig().set("swears.enabled", state);
//                        plugin.setFilterConfig(filterConfig);
//                        break;
//                    case "slurs":
//                        this.filterConfig.getConfig().set("slurs.enabled", state);
//                        plugin.setFilterConfig(filterConfig);
//                        break;
//                    default:
//                        sender.sendMessage(ChatColor.AQUA + "The options are: on and off.");
//                        break;
//                }
//            } else {
//                switch (args[1].toLowerCase()) {
//                    case "mode":
//                        if (args[2].equalsIgnoreCase("replace") || args[2].equalsIgnoreCase("censor") || args[2].equalsIgnoreCase("clear")) {
//                            this.filterConfig.getConfig().set(args[0].toLowerCase() + ".mode", args[2]);
//                            plugin.setFilterConfig(filterConfig);
//                        } else {
//                            sender.sendMessage(ChatColor.AQUA + "The three selectable modes are: replace, censor, clear.");
//                        }
//                        break;
//                    case "commands":
//                        if (args[2].equalsIgnoreCase("list")) {
//                            List<String> commandList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".commands"));
//                            for (int i = 0; i < commandList.size(); i++) {
//                                int num = i + 1;
//                                sender.sendMessage(num + ": " + commandList.get(i));
//                            }
//                        } else if (args[2].equalsIgnoreCase("add")) {
//                            List<String> commandList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".commands"));
//                            StringBuilder cmd = new StringBuilder();
//                            for (int i = 3; i < args.length; i++) {
//                                cmd.append(args[i]);
//                                if (i != args.length - 1) cmd.append(" ");
//                            }
//                            commandList.add(cmd.toString());
//                            this.filterConfig.getConfig().set(args[0].toLowerCase() + ".commands", commandList);
//                            plugin.setFilterConfig(this.filterConfig);
//                        } else if (args[2].equalsIgnoreCase("remove")) {
//                            try {
//                                List<String> commandList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".commands"));
//                                int num = Integer.parseInt(args[3]) - 1;
//                                commandList.remove(num);
//                                this.filterConfig.getConfig().set(args[0].toLowerCase() + ".commands", commandList);
//                                plugin.setFilterConfig(this.filterConfig);
//                            } catch (Exception e) {
//                                sender.sendMessage(ChatColor.AQUA + "Please enter the number of the command you want to remove.");
//                            }
//                        } else {
//                            sender.sendMessage(ChatColor.AQUA + "The options for commands are: list, add, and remove..");
//                        }
//                        break;
//                    case "replace":
//                        if (args[2].equalsIgnoreCase("current")) {
//                            sender.sendMessage(Objects.requireNonNull(this.filterConfig.getConfig().getString(args[0].toLowerCase() + ".replaceWith")));
//                        } else if (args[2].equalsIgnoreCase("edit")) {
//                            StringBuilder cmd = new StringBuilder();
//                            for (int i = 3; i < args.length; i++) {
//                                cmd.append(args[i]);
//                                if (i != args.length - 1) cmd.append(" ");
//                            }
//                            this.filterConfig.getConfig().set(args[0].toLowerCase() + ".replaceWith", cmd.toString());
//                            plugin.setFilterConfig(this.filterConfig);
//                        } else {
//                            sender.sendMessage(ChatColor.AQUA + "The possible replace commands are: current and edit.");
//                        }
//                        break;
//                    case "staff":
//                        if (args[2].equalsIgnoreCase("current")) {
//                            sender.sendMessage(Objects.requireNonNull(this.filterConfig.getConfig().getString(args[0].toLowerCase() + ".msgToStaff")));
//                        } else if (args[2].equalsIgnoreCase("edit")) {
//                            StringBuilder cmd = new StringBuilder();
//                            for (int i = 3; i < args.length; i++) {
//                                cmd.append(args[i]);
//                                if (i != args.length - 1) cmd.append(" ");
//                            }
//                            this.filterConfig.getConfig().set(args[0].toLowerCase() + ".msgToStaff", cmd.toString());
//                            plugin.setFilterConfig(this.filterConfig);
//                        } else if (args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("off")) {
//                            Boolean state = args[2].equals("on");
//                            this.filterConfig.getConfig().set("swears.msgToStaffEnabled", state);
//                            plugin.setFilterConfig(filterConfig);
//                        } else {
//                            sender.sendMessage(ChatColor.AQUA + "The options for staff are: current, edit, and on/off.");
//                        }
//                        break;
//                    default:
//                        sender.sendMessage(ChatColor.AQUA + "The possible options are: on/off, mode, replace, commands, and staff.");
//                        break;
//                }
//            }
//        } else {
//            sender.sendMessage(ChatColor.AQUA + "The options for the filter command are: on/off, mode, staff, notes, spam, bot, speed, swears/slurs.");
//        }
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
}
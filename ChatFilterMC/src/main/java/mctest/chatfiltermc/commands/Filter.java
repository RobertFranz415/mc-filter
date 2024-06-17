package mctest.chatfiltermc.commands;

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

    public Filter(ChatFilterMC plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.filterConfig = plugin.getFilterConfig();
        this.historyConfig = new ConfigUtil(plugin, "History.yml");
    }

    //TODO
    // Move regex to end of yaml file sections to clean up when more regex added... or make separate files for regex and config stuff
    // Add multiple possible replace messages to be randomly selected
    // Three strike system
    // User history/ past offences saved
    // Clear history
    // Exceptions list
    // Timeout individual player
    // Change to switch statement for performance
    // Add messages when someone does something to confirm it worked
    // Add catches for if they don't put in enough args because it just throws an error

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //TODO Probs dont need this
        if (!sender.hasPermission("filter") || !sender.isOp()) return true;

        switch (args[0].toLowerCase()) {
            case "on":
            case "off":
                Boolean state = args[0].equals("on");
                this.filterConfig.getConfig().set("swears.enabled", state);
                this.filterConfig.getConfig().set("slurs.enabled", state);
                plugin.setFilterConfig(filterConfig);
                break;
            case "mode":
                if (args[1].equalsIgnoreCase("replace") || args[1].equalsIgnoreCase("censor") || args[1].equalsIgnoreCase("clear")) {
                    this.filterConfig.getConfig().set("swears.mode", args[1]);
                    this.filterConfig.getConfig().set("slurs.mode", args[1]);
                    plugin.setFilterConfig(filterConfig);
                } else {
                    sender.sendMessage(ChatColor.AQUA + "The three selectable modes are: Replace, Censor, Clear");
                }
                break;
            case "staff":
                if (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off")) {
                    this.filterConfig.getConfig().set("swears.msgToStaffEnabled", args[1]);
                    this.filterConfig.getConfig().set("slurs.msgToStaffEnabled", args[1]);
                    plugin.setFilterConfig(filterConfig);
                } else if (args[1].equalsIgnoreCase("edit")) {
                    StringBuilder cmd = new StringBuilder();
                    for (int i = 3; i < args.length; i++) {
                        cmd.append(args[i]);
                        if (i != args.length - 1) cmd.append(" ");
                    }
                    this.filterConfig.getConfig().set("swears.msgToStaff", cmd.toString());
                    this.filterConfig.getConfig().set("slurs.msgToStaff", cmd.toString());
                    plugin.setFilterConfig(this.filterConfig);
                } else {
                    sender.sendMessage(ChatColor.AQUA + "The staff commands are: on/off and edit.");
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
                }

                if (uuid == null) {
                    return true;
                }
                this.historyConfig = plugin.getHistoryConfig();
                //TODO check if this is needed
                // or if i also have to do this for strikes, etc
                if (!this.historyConfig.getConfig().contains(uuid + ".notes")) {
                    this.historyConfig.getConfig().set(uuid + ".notes", new ArrayList<String>());
                }

                if (args[1].equalsIgnoreCase("list")) {
                    List<String> notes = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
                    if (notes.isEmpty()) {
                        sender.sendMessage(ChatColor.AQUA + "This player has no notes related to them!");
                        return true;
                    }
                    for (int i = 0; i < notes.size(); i++) {
                        int num = i + 1;
                        sender.sendMessage(num + ": " + notes.get(i));
                    }
                } else if (args[1].equalsIgnoreCase("add")) {
                    List<String> notes = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
                    StringBuilder note = new StringBuilder();
                    note.append(sender.getName()).append(": ");
                    for (int i = 3; i < args.length; i++) {
                        note.append(args[i]);
                        if (i != args.length - 1) note.append(" ");
                    }
                    notes.add(this.setCommand(uuid, note.toString()));
                    this.historyConfig.getConfig().set(uuid + ".notes", notes);
                    this.historyConfig.save();
                } else if (args[1].equalsIgnoreCase("remove")) {
                    if (args.length == 3) {
                        sender.sendMessage(ChatColor.AQUA + "Must enter a valid number, all, or history.");
                        return true;
                    }
                    if (Objects.equals(args[3], "all")) {
                        this.historyConfig.getConfig().set(uuid + ".notes", new ArrayList<String>());
                        this.historyConfig.save();
                        plugin.setHistoryConfig(this.historyConfig);
                    } else if (Objects.equals(args[3], "history")) {
                        //TODO could remove based off key word
                        List<String> notes = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));

                        notes.removeIf(note -> note.contains("CONSOLE:"));

                        this.historyConfig.getConfig().set(uuid + ".notes", notes);
                        this.historyConfig.save();
                        plugin.setHistoryConfig(this.historyConfig);
                    } else {
                        //TODO could use regex to detect if int
                        try {
                            List<String> notes = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
                            int num = Integer.parseInt(args[3]) - 1;
                            notes.remove(num);
                            this.historyConfig.getConfig().set(uuid + ".notes", notes);
                            this.historyConfig.save();
                            plugin.setHistoryConfig(this.historyConfig);
                        } catch (Exception e) {
                            sender.sendMessage(ChatColor.AQUA + "Please enter a valid number of the note you want to remove.");
                        }
                    }
                } else if (args[1].equalsIgnoreCase("strikes")) {
                    //TODO figure out best order of command... currently /filter notes strike [player] clear swears, etc...
                    // possibly add set command to manually set number of strikes
                    if (args.length >= 4) {
                        if (args[3].equalsIgnoreCase("clear")) {
                            Bukkit.getLogger().info("Args length: " + args.length);
                            if (args.length == 4) {
                                Bukkit.getLogger().info("Args length is 4!!!");
                                this.historyConfig.getConfig().set(uuid + ".slurs.count", 0);
                                this.historyConfig.getConfig().set(uuid + ".swears.count", 0);
                            } else if (args[4].equalsIgnoreCase("swears")) {
                                Bukkit.getLogger().info("CLEAR SWEARS!!!");
                                this.historyConfig.getConfig().set(uuid + ".swears.count", 0);
                            } else if (args[4].equalsIgnoreCase("slurs")) {
                                this.historyConfig.getConfig().set(uuid + ".slurs.count", 0);
                            } else {
                                sender.sendMessage(ChatColor.AQUA + "Clear can only be followed by 'slurs' or 'swears'.");
                            }
                            this.historyConfig.save();
                            plugin.setHistoryConfig(this.historyConfig);
                        } else {
                            sender.sendMessage(ChatColor.AQUA + "To display strike counts simply enter the username of the person.  To clear strikes add clear after.");
                        }
                    } else {
                        int slurCnt = (historyConfig.getConfig().getString(uuid + ".slurs.count") == null) ? 0 : historyConfig.getConfig().getInt(uuid + ".slurs.count");
                        int swearCnt = (historyConfig.getConfig().getString(uuid + ".swears.count") == null) ? 0 : historyConfig.getConfig().getInt(uuid + ".swears.count");
                        sender.sendMessage("Slur strikes: " + slurCnt);
                        sender.sendMessage("Swear strikes: " + swearCnt);
                    }
                } else {
                    sender.sendMessage(ChatColor.AQUA + "The notes commands are: list, add, remove, and strikes.");
                }
                break;
            case "spam":
                if (Objects.equals(args[1], "on") || Objects.equals(args[1], "off")) {
                    Boolean spamState = args[1].equals("on");
                    this.filterConfig.getConfig().set("spam.enabled", spamState);
                    this.filterConfig.save();
                    plugin.setFilterConfig(filterConfig);
                } else {
                    sender.sendMessage(ChatColor.AQUA + "The options are: on and off.");
                }
                break;
            case "bot":
                if (Objects.equals(args[1], "on") || Objects.equals(args[1], "off")) {
                    Boolean botState = args[1].equals("on");
                    this.filterConfig.getConfig().set("bot.enabled", botState);
                    this.filterConfig.save();
                    plugin.setFilterConfig(filterConfig);
                } else {
                    sender.sendMessage(ChatColor.AQUA + "The options are: on and off.");
                }
                break;
            case "speed":
                if (Objects.equals(args[1], "normal") || Objects.equals(args[1], "chill") || Objects.equals(args[1], "slow") || Objects.equals(args[1], "ice")) {
                    this.filterConfig.getConfig().set("chatSpeed.mode", args[1]);
                    this.filterConfig.save();
                    plugin.setFilterConfig(filterConfig);
                } else {
                    sender.sendMessage(ChatColor.AQUA + "The options are: normal, chill, slow, ice..");
                }
                break;
            case "swears":
            case "slurs":
                if (Objects.equals(args[1], "on") || Objects.equals(args[1], "off")) {
                    Boolean filterState = args[1].equals("on");
                    switch (args[0]) {
                        case "swears":
                            this.filterConfig.getConfig().set("swears.enabled", filterState);
                            plugin.setFilterConfig(filterConfig);
                            break;
                        case "slurs":
                            this.filterConfig.getConfig().set("slurs.enabled", filterState);
                            plugin.setFilterConfig(filterConfig);
                            break;
                        default:
                            sender.sendMessage(ChatColor.AQUA + "The options are: on and off.");
                            break;
                    }
                } else {
                    switch (args[1].toLowerCase()) {
                        case "mode":
                            if (args[2].equalsIgnoreCase("replace") || args[2].equalsIgnoreCase("censor") || args[2].equalsIgnoreCase("clear")) {
                                this.filterConfig.getConfig().set(args[0].toLowerCase() + ".mode", args[2]);
                                plugin.setFilterConfig(filterConfig);
                            } else {
                                sender.sendMessage(ChatColor.AQUA + "The three selectable modes are: replace, censor, clear.");
                            }
                            break;
                        case "commands":
                            if (args[2].equalsIgnoreCase("list")) {
                                List<String> commandList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".commands"));
                                for (int i = 0; i < commandList.size(); i++) {
                                    int num = i + 1;
                                    sender.sendMessage(num + ": " + commandList.get(i));
                                }
                            } else if (args[2].equalsIgnoreCase("add")) {
                                List<String> commandList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".commands"));
                                StringBuilder cmd = new StringBuilder();
                                for (int i = 3; i < args.length; i++) {
                                    cmd.append(args[i]);
                                    if (i != args.length - 1) cmd.append(" ");
                                }
                                commandList.add(cmd.toString());
                                this.filterConfig.getConfig().set(args[0].toLowerCase() + ".commands", commandList);
                                plugin.setFilterConfig(this.filterConfig);
                            } else if (args[2].equalsIgnoreCase("remove")) {
                                try {
                                    List<String> commandList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".commands"));
                                    int num = Integer.parseInt(args[3]) - 1;
                                    commandList.remove(num);
                                    this.filterConfig.getConfig().set(args[0].toLowerCase() + ".commands", commandList);
                                    plugin.setFilterConfig(this.filterConfig);
                                } catch (Exception e) {
                                    sender.sendMessage(ChatColor.AQUA + "Please enter the number of the command you want to remove.");
                                }
                            } else {
                                sender.sendMessage(ChatColor.AQUA + "The options for commands are: list, add, and remove..");
                            }
                            break;
                        case "staff":
                            if (args[2].equalsIgnoreCase("current")) {
                                sender.sendMessage(Objects.requireNonNull(this.filterConfig.getConfig().getString(args[0].toLowerCase() + ".msgToStaff")));
                            } else if (args[2].equalsIgnoreCase("edit")) {
                                StringBuilder cmd = new StringBuilder();
                                for (int i = 3; i < args.length; i++) {
                                    cmd.append(args[i]);
                                    if (i != args.length - 1) cmd.append(" ");
                                }
                                this.filterConfig.getConfig().set(args[0].toLowerCase() + ".msgToStaff", cmd.toString());
                                plugin.setFilterConfig(this.filterConfig);
                            } else if (args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("off")) {
                                Boolean msgState = args[2].equals("on");
                                this.filterConfig.getConfig().set("swears.msgToStaffEnabled", msgState);
                                plugin.setFilterConfig(filterConfig);
                            } else {
                                sender.sendMessage(ChatColor.AQUA + "The options for staff are: current, edit, and on/off.");
                            }
                            break;
                        case "replace":
                            if (args[2].equalsIgnoreCase("list")) {
                                List<String> replaceList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".replaceWith"));
                                if (replaceList.isEmpty()) {
                                    sender.sendMessage(ChatColor.AQUA + "No replacement messages currently set.");
                                }
                                for (int i = 0; i < replaceList.size(); i++) {
                                    int num = i + 1;
                                    sender.sendMessage(num + ": " + replaceList.get(i));
                                }
                            } else if (args[2].equalsIgnoreCase("add")) {
                                List<String> replaceList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".replaceWith"));
                                StringBuilder msg = new StringBuilder();
                                for (int i = 3; i < args.length; i++) {
                                    msg.append(args[i]);
                                    if (i != args.length - 1) msg.append(" ");
                                }
                                replaceList.add(msg.toString());
                                this.filterConfig.getConfig().set(args[0].toLowerCase() + ".replaceWith", replaceList);
                                plugin.setFilterConfig(this.filterConfig);
                            } else if (args[2].equalsIgnoreCase("remove")) {
                                try {
                                    List<String> replaceList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".replaceWith"));
                                    int num = Integer.parseInt(args[3]) - 1;
                                    replaceList.remove(num);
                                    this.filterConfig.getConfig().set(args[0].toLowerCase() + ".replaceWith", replaceList);
                                    plugin.setFilterConfig(this.filterConfig);
                                } catch (Exception e) {
                                    sender.sendMessage(ChatColor.AQUA + "Please enter the number of the replacement message you want to remove.");
                                }
                            } else {
                                sender.sendMessage(ChatColor.AQUA + "The possible replace commands are: list, add, and remove.");
                            }
                            break;
                        default:
                            sender.sendMessage(ChatColor.AQUA + "The possible options are: on/off, mode, replace, commands, and staff.");
                            break;

                    }
                }
                break;
            default:
                sender.sendMessage(ChatColor.AQUA + "Plugin created by Mithraea and DeathsValentine");
                sender.sendMessage(ChatColor.AQUA + "The options for the filter command are: on/off, mode, staff, notes, spam, bot, speed, swears/slurs.");
                break;
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

    private String setCommand(UUID uuid, String msg) {
        if (msg.contains("[date]")) {
            Date now = new Date();
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            msg = msg.replace("[date]", "[" + format.format(now) + "]");
        }

        return msg;
    }
}
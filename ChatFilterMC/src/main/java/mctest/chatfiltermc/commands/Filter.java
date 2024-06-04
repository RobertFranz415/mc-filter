package mctest.chatfiltermc.commands;

import mctest.chatfiltermc.ChatFilterMC;
import mctest.chatfiltermc.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Filter implements Listener, CommandExecutor {
    private final ChatFilterMC plugin;
    private final ConfigUtil filterConfig;
    private final ConfigUtil historyConfig;

    public Filter(ChatFilterMC plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.filterConfig = plugin.getFilterConfig();
        this.historyConfig = new ConfigUtil(plugin, "History.yml");
    }

    //TODO
    // Move regex to end of yaml file sections to clean up when more regex added
    // Add multiple possible replace messages to be randomly selected
    // Three strike system
    // User history/ past offences saved
    // Clear history

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //TODO Probs dont need this
        if (!sender.hasPermission("filter") || !sender.isOp()) return true;

        if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off")) {
            Boolean state = args[0].equals("on");
            this.filterConfig.getConfig().set("swears.enabled", state);
            this.filterConfig.getConfig().set("slurs.enabled", state);
            plugin.setFilterConfig(filterConfig);
        } else if (args[0].equalsIgnoreCase("mode")) {
            if (args[1].equalsIgnoreCase("replace") || args[1].equalsIgnoreCase("censor") || args[1].equalsIgnoreCase("clear")) {
                this.filterConfig.getConfig().set("swears.mode", args[1]);
                this.filterConfig.getConfig().set("slurs.mode", args[1]);
                plugin.setFilterConfig(filterConfig);
            } else {
                sender.sendMessage(ChatColor.AQUA + "The three selectable modes are: Replace, Censor, Clear");
            }
        } else if (args[0].equalsIgnoreCase("staff")) {
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
        } else if (args[0].equalsIgnoreCase("notes")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.AQUA + "Must select an option and use a valid username.");
                return true;
            }
            //TODO Check that notes exist first
            UUID uuid = null;
            try {
                uuid = Objects.requireNonNull(Bukkit.getPlayer(args[2])).getUniqueId();
            } catch (Exception e) {
                sender.sendMessage(ChatColor.AQUA + "Please enter a valid username.");
            }

            if (uuid == null) {
                return true;
            }
            if (args[1].equalsIgnoreCase("show")) {
                List<String> notes = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
                for (int i = 0; i < notes.size(); i++) {
                    int num = i + 1;
                    sender.sendMessage(num + ": " + notes.get(i));
                }
            } else if (args[1].equalsIgnoreCase("add")) {
                List<String> notes = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
                StringBuilder note = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    note.append(args[i]);
                    if (i != args.length - 1) note.append(" ");
                }
                notes.add(note.toString());
                this.historyConfig.getConfig().set(uuid + ".notes", notes);
                this.historyConfig.save();
            } else if (args[1].equalsIgnoreCase("remove")) {
                try {
                    List<String> notes = new ArrayList<>(this.historyConfig.getConfig().getStringList(uuid + ".notes"));
                    int num = Integer.parseInt(args[3]) - 1;
                    notes.remove(num);
                    this.historyConfig.getConfig().set(uuid + ".notes", notes);
                    this.historyConfig.save();
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.AQUA + "Please enter the number of the note you want to remove.");
                }
            } else if (args[1].equalsIgnoreCase("strikes")) {
                int slurCnt = (historyConfig.getConfig().getString(uuid + ".slurs.count") == null) ? 0 : historyConfig.getConfig().getInt(uuid + ".slurs.count");
                int swearCnt = (historyConfig.getConfig().getString(uuid + ".swears.count") == null) ? 0 : historyConfig.getConfig().getInt(uuid + ".swears.count");
                sender.sendMessage("Slur strikes: " + slurCnt);
                sender.sendMessage("Swear strikes: " + swearCnt);
            } else {
                sender.sendMessage(ChatColor.AQUA + "The notes commands are: show, add, remove, and strikes.");
            }

        } else if (args[0].equalsIgnoreCase("swears") || args[0].equalsIgnoreCase("slurs")) {
            if (Objects.equals(args[1], "on") || Objects.equals(args[1], "off")) {
                Boolean state = args[1].equals("on");
                switch (args[0]) {
                    case "swears":
                        this.filterConfig.getConfig().set("swears.enabled", state);
                        plugin.setFilterConfig(filterConfig);
                        break;
                    case "slurs":
                        this.filterConfig.getConfig().set("slurs.enabled", state);
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
                    case "replace":
                        if (args[2].equalsIgnoreCase("current")) {
                            sender.sendMessage(Objects.requireNonNull(this.filterConfig.getConfig().getString(args[0].toLowerCase() + ".replaceWith")));
                        } else if (args[2].equalsIgnoreCase("edit")) {
                            StringBuilder cmd = new StringBuilder();
                            for (int i = 3; i < args.length; i++) {
                                cmd.append(args[i]);
                                if (i != args.length - 1) cmd.append(" ");
                            }
                            this.filterConfig.getConfig().set(args[0].toLowerCase() + ".replaceWith", cmd.toString());
                            plugin.setFilterConfig(this.filterConfig);
                        } else {
                            sender.sendMessage(ChatColor.AQUA + "The possible replace commands are: current and edit.");
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
                            Boolean state = args[2].equals("on");
                            this.filterConfig.getConfig().set("swears.msgToStaffEnabled", state);
                            plugin.setFilterConfig(filterConfig);
                        } else {
                            sender.sendMessage(ChatColor.AQUA + "The options for staff are: current, edit, and on/off.");
                        }
                        break;
                    default:
                        sender.sendMessage(ChatColor.AQUA + "The possible options are on/off, mode, replace, commands, and staff.");
                        break;
                }
            }
        } else {
            sender.sendMessage(ChatColor.AQUA + "The options for the filter command are: on/off, mode, staff, notes, swears/slurs.");
        }
        return true;
    }
}
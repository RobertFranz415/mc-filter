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

public class Filter implements Listener, CommandExecutor {
    private final ChatFilterMC plugin;
    private final ConfigUtil filterConfig;

    public Filter(ChatFilterMC plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.filterConfig = plugin.getFilterConfig();
    }

    //TODO
    // Move regex to end of yaml file sections to clean up when more regex added

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
                sender.sendMessage(ChatColor.BLUE + "The three selectable modes are: Replace, Censor, Clear");
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
                sender.sendMessage(ChatColor.BLUE + "The staff commands are: on/off and edit.");
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
                        sender.sendMessage(ChatColor.RED + "Not a valid command");
                        break;
                }
            } else {
                switch (args[1].toLowerCase()) {
                    case "mode":
                        if (args[2].equalsIgnoreCase("replace") || args[2].equalsIgnoreCase("censor") || args[2].equalsIgnoreCase("clear")) {
                            this.filterConfig.getConfig().set(args[0].toLowerCase() + ".mode", args[2]);
                            plugin.setFilterConfig(filterConfig);
                        } else {
                            sender.sendMessage(ChatColor.BLUE + "The three selectable modes are: replace, censor, clear.");
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
                                sender.sendMessage(ChatColor.RED + "Please enter the number of the command you want to remove.");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "Not a valid command.");
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
                            sender.sendMessage(ChatColor.RED + "Not a valid command.");
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
                            sender.sendMessage(ChatColor.RED + "Not a valid command.");
                        }
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Not a valid command");
                        break;
                }
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Not a valid command");
        }
        return true;
    }
}
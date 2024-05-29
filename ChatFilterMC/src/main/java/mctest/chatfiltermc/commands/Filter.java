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

    //TODO Make switch depend on first arg not length
    // Determine what should be added and what is fine just being added to yaml manually
    // Make config all lowercase or figure out how to better implement
    // Message to staff on/off, edit
    // ReplaceWith edit
    // Move regex to end of yaml file sections to clean up when more regex added
    // Implement switch instead of if-else

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //TODO Add more universal commands
        if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off")) {
            Boolean state = args[0].equals("on");
            this.filterConfig.getConfig().set("swears.Enabled", state);
            this.filterConfig.getConfig().set("slurs.Enabled", state);
            plugin.setFilterConfig(filterConfig);
        } else if (args[0].equalsIgnoreCase("mode")) {
            if (Objects.equals(args[1], "Replace") || Objects.equals(args[1], "Censor") || Objects.equals(args[1], "Clear")) {
                this.filterConfig.getConfig().set("swears.Mode", args[1]);
                this.filterConfig.getConfig().set("slurs.Mode", args[1]);
                plugin.setFilterConfig(filterConfig);
            } else {
                sender.sendMessage(ChatColor.BLUE + "The three selectable modes are: Replace, Censor, Clear");
            }
        } else if (args[0].equalsIgnoreCase("swears") || args[0].equalsIgnoreCase("slurs")) {
            if (Objects.equals(args[1], "on") || Objects.equals(args[1], "off")) {
                Boolean state = args[1].equals("on");
                switch (args[0]) {
                    case "swears":
                        this.filterConfig.getConfig().set("swears.Enabled", state);
                        plugin.setFilterConfig(filterConfig);
                        break;
                    case "slurs":
                        this.filterConfig.getConfig().set("slurs.Enabled", state);
                        plugin.setFilterConfig(filterConfig);
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Not a valid command");
                        break;
                }
            }

            if (args[1].equalsIgnoreCase("mode")) {
                if (Objects.equals(args[2], "Replace") || Objects.equals(args[2], "Censor") || Objects.equals(args[2], "Clear")) {
                    this.filterConfig.getConfig().set(args[0].toLowerCase() + ".Mode", args[2]);
                    plugin.setFilterConfig(filterConfig);
                } else {
                    sender.sendMessage(ChatColor.BLUE + "The three selectable modes are: Replace, Censor, Clear");
                }
            }

            if (args[1].equalsIgnoreCase("commands")) {
                if (args[2].equalsIgnoreCase("list")) {
                    List<String> commandList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".Commands"));
                    for (int i = 0; i < commandList.size(); i++) {
                        int num = i + 1;
                        sender.sendMessage(num + ": " + commandList.get(i));
                    }
                } else if (args[2].equalsIgnoreCase("add")) {
                    List<String> commandList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".Commands"));
                    StringBuilder cmd = new StringBuilder();
                    for (int i = 3; i < args.length; i++) {
                        cmd.append(args[i]);
                        if (i != args.length - 1) cmd.append(" ");
                    }
                    commandList.add(cmd.toString());
                    this.filterConfig.getConfig().set(args[0].toLowerCase() + ".Commands", commandList);
                    plugin.setFilterConfig(this.filterConfig);
                } else if (args[2].equalsIgnoreCase("remove")) {
                    try {
                        List<String> commandList = new ArrayList<>(this.filterConfig.getConfig().getStringList(args[0].toLowerCase() + ".Commands"));
                        int num = Integer.parseInt(args[3]) - 1;
                        commandList.remove(num);
                        this.filterConfig.getConfig().set(args[0].toLowerCase() + ".Commands", commandList);
                        plugin.setFilterConfig(this.filterConfig);
                    } catch (Exception e) {
                        sender.sendMessage("Not a valid command or number.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Not a valid command");
                }
            }
            if (args[1].equalsIgnoreCase("replace")) {
                if (args[2].equalsIgnoreCase("current")) {
                    sender.sendMessage(this.filterConfig.getConfig().getString(args[0].toLowerCase() + ".ReplaceWith"));
                } else if (args[2].equalsIgnoreCase("edit")) {
                    StringBuilder cmd = new StringBuilder();
                    for (int i = 3; i < args.length; i++) {
                        cmd.append(args[i]);
                        if (i != args.length - 1) cmd.append(" ");
                    }
                    this.filterConfig.getConfig().set(args[0].toLowerCase() + ".ReplaceWith", cmd.toString());
                    plugin.setFilterConfig(this.filterConfig);
                } else {
                    sender.sendMessage(ChatColor.RED + "Not a valid command");
                }
            }
            if (args[1].equalsIgnoreCase("staff")) {
                if (args[2].equalsIgnoreCase("current")) {
                    sender.sendMessage(this.filterConfig.getConfig().getString(args[0].toLowerCase() + ".msgToStaff"));
                } else if (args[2].equalsIgnoreCase("edit")) {
                    StringBuilder cmd = new StringBuilder();
                    for (int i = 3; i < args.length; i++) {
                        cmd.append(args[i]);
                        if (i != args.length - 1) cmd.append(" ");
                    }
                    this.filterConfig.getConfig().set(args[0].toLowerCase() + ".msgToStaff", cmd.toString());
                    plugin.setFilterConfig(this.filterConfig);
                } else {
                    sender.sendMessage(ChatColor.RED + "Not a valid command");
                }
            }

        } else {
            sender.sendMessage(ChatColor.RED + "Not a valid command");
        }
        return true;
    }
}
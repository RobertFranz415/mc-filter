package mctest.chatfiltermc.commands;

import mctest.chatfiltermc.ChatFilterMC;
import mctest.chatfiltermc.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;

import java.util.Objects;

public class Filter implements Listener, CommandExecutor {
    private final ChatFilterMC plugin;
    private final ConfigUtil filterConfig;

    public Filter(ChatFilterMC plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.filterConfig = plugin.getFilterConfig();
    }

    //TODO Determine what should be added and what is fine just being added to yaml manually
    // list commands (numbered)
    // remove command by number
    // add command
    // Make config all lowercase or figure out how to better implement
    // Message to staff on/off
    // Move regex to end of yaml file sections to clean up when more regex added
    // Implement switch instead of if-else

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
//        Bukkit.getLogger().info(args.length + "");
//        for (String arg : args) {
//            Bukkit.getLogger().info(arg);
//        }
        if (args.length == 2) {
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
            } else {
                sender.sendMessage(ChatColor.RED + "Not a valid command");
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("swears")) {
                if (args[1].equalsIgnoreCase("mode")) {
                    if (Objects.equals(args[2], "Replace") || Objects.equals(args[2], "Censor") || Objects.equals(args[2], "Clear")) {
                        this.filterConfig.getConfig().set("swears.Mode", args[2]);
                        plugin.setFilterConfig(filterConfig);
                    }
                }
            } else if (args[0].equalsIgnoreCase("slurs")) {
                if (args[1].equalsIgnoreCase("mode")) {
                    if (Objects.equals(args[2], "Replace") || Objects.equals(args[2], "Censor") || Objects.equals(args[2], "Clear")) {
                        this.filterConfig.getConfig().set("slurs.Mode", args[2]);
                        plugin.setFilterConfig(filterConfig);
                    }
                }
            }
        }
        return true;
    }
}

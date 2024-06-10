package mctest.chatfiltermc;

import mctest.chatfiltermc.util.ConfigUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.*;

public class ChatFilter implements Listener {
    private final ChatFilterMC plugin;
    private List<String> swearList = new ArrayList<>();
    private List<String> slurList = new ArrayList<>();
    private final ConfigUtil filterConfig;
    private ConfigUtil historyConfig;

    public ChatFilter(ChatFilterMC plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.filterConfig = plugin.getFilterConfig();
        this.historyConfig = plugin.getHistoryConfig();

        this.swearList = filterConfig.getConfig().getStringList("swears.regex");
        this.slurList = filterConfig.getConfig().getStringList("slurs.regex");

    }

    //TODO Make it easy to use a commmand which will store message in notes
    // Possibly detect option in command with [msg] and replace with message by passing through original maessage to handlers
    // Clean up/modularize code
    // The notes are saving with '' now for some reason since creating method for replacing message
    public void handleSlurs(UUID uuid, String msg) {
        this.historyConfig = plugin.getHistoryConfig();
        List<String> slurCommands = filterConfig.getConfig().getStringList("slurs.commands");
        for (String command : slurCommands) {
            command = this.setCommand(command, uuid, msg);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }

        if (this.filterConfig.getConfig().getBoolean("slurs.msgToStaffEnabled")) {
            String msgToStaff = this.filterConfig.getConfig().getString("slurs.msgToStaff");
            if (msgToStaff.contains("[senderName]")) {
                msgToStaff = msgToStaff.replace("[senderName]", Objects.requireNonNull(Bukkit.getPlayer(uuid)).getDisplayName());
            }
            Bukkit.broadcast(ChatColor.RED + msgToStaff, "filter");
        }

        //TODO Possibly add: message, dates, last date, etc
        // might need to do plain ol if-statement instead of ternary if going to init more than just count
        // spam detection
        // messages separate from notes?
        if (this.filterConfig.getConfig().getBoolean("swears.history")) {
            //TODO maybe use .contains instead of get == null
            int cnt = (historyConfig.getConfig().get(uuid + ".slurs.count") == null) ? 1 : historyConfig.getConfig().getInt(uuid + ".slurs.count")+1;
            this.historyConfig.getConfig().set(uuid + ".slurs.count", cnt);
            plugin.setHistoryConfig(this.historyConfig);

            int maxStrikes = this.filterConfig.getConfig().getInt("slurs.maxStrikes");
            if (cnt >= maxStrikes && maxStrikes != -1) {
                List<String> actions = filterConfig.getConfig().getStringList("slurs.strikeActions");
                for (String command : actions) {
                    if (command.contains("[senderName]")) {
                        command = command.replace("[senderName]", Objects.requireNonNull(Bukkit.getPlayer(uuid)).getDisplayName());
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        }
        //So that the config will update if any commands updater the history
        plugin.setHistoryConfig(this.historyConfig);
    }

    public void handleSwears(UUID uuid, String msg) {
        this.historyConfig = plugin.getHistoryConfig();
        List<String> swearCommands = filterConfig.getConfig().getStringList("swears.commands");
        for (String command : swearCommands) {
            command = this.setCommand(command, uuid, msg);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }

        if (this.filterConfig.getConfig().getBoolean("swears.msgToStaffEnabled")) {
            String msgToStaff = this.filterConfig.getConfig().getString("swears.msgToStaff");
            if (msgToStaff.contains("[senderName]")) {
                msgToStaff = msgToStaff.replace("[senderName]", Objects.requireNonNull(Bukkit.getPlayer(uuid)).getDisplayName());
            }

            Bukkit.broadcast(ChatColor.RED + msgToStaff, "filter");
        }

        if (this.filterConfig.getConfig().getBoolean("swears.history")) {
            int cnt = (historyConfig.getConfig().getString(uuid + ".swears.count") == null) ? 1 : historyConfig.getConfig().getInt(uuid + ".swears.count")+1;
            this.historyConfig.getConfig().set(uuid + ".swears.count", cnt);
            plugin.setHistoryConfig(this.historyConfig);

            int maxStrikes = this.filterConfig.getConfig().getInt("swears.maxStrikes");
            if (cnt >= maxStrikes && maxStrikes != -1) {
                List<String> actions = filterConfig.getConfig().getStringList("swears.strikeActions");
                for (String command : actions) {
                    if (command.contains("[senderName]")) {
                        command = command.replace("[senderName]", Objects.requireNonNull(Bukkit.getPlayer(uuid)).getDisplayName());
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        }
        //So that the config will update if any commands updater the history
        plugin.setHistoryConfig(this.historyConfig);
    }

    private String setCommand(String command, UUID uuid, String msg) {
        if (command.contains("[senderName]")) {
            command = command.replace("[senderName]", Objects.requireNonNull(Bukkit.getPlayer(uuid)).getDisplayName());
        }
        if (command.contains("[date]")) {
            Date now = new Date();
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            command = command.replace("[date]", "[" + format.format(now) + "]");
        }
        if (command.contains("[msg]")) {
            command = command.replace("[msg]", ": \"" + msg + "\"");
        }

        return command;
    }

    @EventHandler
    private void onPlayerChatEvent(AsyncPlayerChatEvent event) {
        if (!this.filterConfig.getConfig().getBoolean("slurs.enabled") && !this.filterConfig.getConfig().getBoolean("swears.enabled")) {
            return;
        }
        String message = event.getMessage();
        String[] msg = message.split(" ");

        // Using these booleans instead of implementing actions when found so that
        // actions do not trigger multiple times and so that slurs will have a higher priority
        // IE: "blah swear blah slur, slur" will only trigger the slur handler and only once
        boolean slurFound = false;
        boolean swearFound = false;

        for (int i = 0; i < msg.length; i++) {
            if (this.filterConfig.getConfig().getBoolean("slurs.enabled")) {
                for (String rex : this.slurList) {
                    if (msg[i].matches(rex)) {
                        slurFound = true;
                        //TODO if() statement not necessary, not sure how/if effects efficiency
                        if (Objects.equals(this.filterConfig.getConfig().getString("slurs.mode"), "censor")) {
                            msg[i] = "****";
                        }
                    }
                }
            }
            if (this.filterConfig.getConfig().getBoolean("swears.enabled")) {
                for (String rex : this.swearList) {
                    if (msg[i].matches(rex)) {
                        swearFound = true;

                        if (Objects.equals(this.filterConfig.getConfig().getString("swears.mode"), "censor")) {
                            msg[i] = "****";
                        }
                    }
                }
            }
        }

        if (slurFound) {
            // Need BukkitRunnable to run handlers since this event is Async
            new BukkitRunnable() {
                @Override
                public void run() {
                    handleSlurs(event.getPlayer().getUniqueId(), message);
                }
            }.runTask(this.plugin);

            switch (Objects.requireNonNull(this.filterConfig.getConfig().getString("slurs.mode"))) {
                case "censor":
                    String clean = String.join(" ", msg);;
                    event.setMessage(clean);
                    break;
                case "replace":
                    event.setMessage(Objects.requireNonNull(filterConfig.getConfig().getString("slurs.replaceWith")));
                    break;
                case "clear":
                    event.setCancelled(true);
                    break;
                default:
                    break;
            }
        } else if (swearFound) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    handleSwears(event.getPlayer().getUniqueId(), message);
                }
            }.runTask(this.plugin);

            switch (Objects.requireNonNull(this.filterConfig.getConfig().getString("swears.mode"))) {
                case "censor":
                    String clean = String.join(" ", msg);;
                    event.setMessage(clean);
                    break;
                case "replace":
                    event.setMessage(Objects.requireNonNull(filterConfig.getConfig().getString("swears.replaceWith")));
                    break;
                case "clear":
                    event.setCancelled(true);
                    break;
                default:
                    break;
            }
        }
    }
}
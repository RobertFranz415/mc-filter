package mctest.chatfiltermc;

import jdk.tools.jlink.plugin.Plugin;
import mctest.chatfiltermc.util.ConfigUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import javax.print.attribute.standard.Severity;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatFilter implements Listener {
    private final ChatFilterMC plugin;
    private ConfigUtil filterConfig;
    private ConfigUtil historyConfig;
    private List<String> swearList = new ArrayList<>();
    private List<String> slurList = new ArrayList<>();
    private final HashMap<UUID, Integer> spamMap = new HashMap<>();
    private final HashMap<UUID, Date> lastMsgMap = new HashMap<>();

    public ChatFilter(ChatFilterMC plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.setConfigs();

    }

    //TODO
    // Add exceptions or decide to just use permission/op
    // Possibly detect option in command with [msg] and replace with message by passing through original maessage to handlers
    // Clean up/modularize code
    // If new entry: make strikes come first in yaml file
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
        // messages separate from notes?
        if (this.filterConfig.getConfig().getBoolean("swears.history")) {
            int cnt = !historyConfig.getConfig().contains(uuid + ".slurs.count") ? 1 : historyConfig.getConfig().getInt(uuid + ".slurs.count")+1;
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
        //So that the config will update if any commands update the history
        plugin.setHistoryConfig(this.historyConfig);
    }

    public void handleSwears(UUID uuid, String msg) {
        this.historyConfig = plugin.getHistoryConfig();

        // Commands
        List<String> swearCommands = filterConfig.getConfig().getStringList("swears.commands");
        for (String command : swearCommands) {
            command = this.setCommand(command, uuid, msg);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }

        // Message to staff
        if (this.filterConfig.getConfig().getBoolean("swears.msgToStaffEnabled")) {
            String msgToStaff = this.filterConfig.getConfig().getString("swears.msgToStaff");
            if (msgToStaff.contains("[senderName]")) {
                msgToStaff = msgToStaff.replace("[senderName]", Objects.requireNonNull(Bukkit.getPlayer(uuid)).getDisplayName());
            }

            Bukkit.broadcast(ChatColor.RED + msgToStaff, "filter");
        }

        // History
        if (this.filterConfig.getConfig().getBoolean("swears.history")) {
            int cnt = !historyConfig.getConfig().contains(uuid + ".swears.count") ? 1 : historyConfig.getConfig().getInt(uuid + ".swears.count")+1;
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
        //So that the config will update if any commands update the history
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
    private void onChatSpamEvent(AsyncPlayerChatEvent event) {
        //TODO Uncomment this
//        if (event.getPlayer().isOp() || event.getPlayer().hasPermission("filter.exception")) return;
        UUID uuid = event.getPlayer().getUniqueId();

        //TODO
        // take length of messages into account (?)
        if (this.filterConfig.getConfig().getBoolean("spam.enabled")) {

            if (this.filterConfig.getConfig().getBoolean("bot.enabled") || !Objects.equals(this.filterConfig.getConfig().getString("chatSpeed.mode"), "normal")) {
                if (!this.lastMsgMap.containsKey(uuid)) {
                    this.lastMsgMap.put(uuid, new Date());
                } else {
                    Date now = new Date();
                    long dif = now.getTime() - this.lastMsgMap.get(uuid).getTime();

                    if (this.filterConfig.getConfig().getBoolean("bot.enabled")) {
                        // Time is in milliseconds. 400 seemed too fast for a human in my testing.  I only got below 500 once or twice
                        if (dif < this.filterConfig.getConfig().getLong("bot.time")) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    //Bukkit.getLogger().warning(ChatColor.RED + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + " is likely botting.");
                                    Bukkit.broadcast(ChatColor.RED + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + " is typing suspiciously fast...", "filter");
                                    event.setCancelled(true);
                                }
                            }.runTask(this.plugin);
                            //TODO Punish: ban, mute, etc...
                            return;
                        }
                    }

                    if (!Objects.equals(this.filterConfig.getConfig().getString("chatSpeed.mode"), "normal")) {
                        String mode = this.filterConfig.getConfig().getString("chatSpeed.mode");
                        long limit = this.filterConfig.getConfig().getLong("chatSpeed." + mode + ".time") * 1000;
                        if (dif < limit) {
                            long left = (limit - dif) / 1000;
                            Bukkit.getPlayer(uuid).sendMessage(ChatColor.RED + "Must wait " + left + " more seconds to comment again.");
                            event.setCancelled(true);
                            return;
                        }
                    }

                    if (!this.spamMap.containsKey(uuid)) {
                        this.spamMap.put(uuid, this.spamMap.getOrDefault(uuid, 0) + 1);
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                            this.spamMap.remove(uuid);
                        }, this.filterConfig.getConfig().getInt("spam.timeAllotted") * 20L);
                    } else {
                        this.spamMap.put(uuid, this.spamMap.getOrDefault(uuid, 0) + 1);
                        if (this.spamMap.get(uuid) > this.filterConfig.getConfig().getInt("spam.numberToTrigger")) {
                            //Bukkit.getLogger().info(event.getPlayer().getName() + " is spamming messages!");
                            Bukkit.broadcast(ChatColor.RED + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + " is spamming messages.", "filter");
                            //TODO punish spammer: mute, ban, make note etc...
                        }
                    }

                    this.lastMsgMap.put(uuid, now);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerChatEvent(AsyncPlayerChatEvent event) {
        //TODO Uncomment this
//        if (event.getPlayer().isOp() || event.getPlayer().hasPermission("filter.exception")) return;
        if (!this.filterConfig.getConfig().getBoolean("slurs.enabled") && !this.filterConfig.getConfig().getBoolean("swears.enabled")) {
            return;
        }
        UUID uuid = event.getPlayer().getUniqueId();

        String message = event.getMessage().toLowerCase();
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
                    handleSlurs(uuid, message);
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
                    handleSwears(uuid, message);
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

    public void setConfigs(){
        this.filterConfig = this.plugin.getFilterConfig();
        this.historyConfig = this.plugin.getHistoryConfig();

        this.swearList = this.filterConfig.getConfig().getStringList("swears.regex");
        this.slurList = this.filterConfig.getConfig().getStringList("slurs.regex");
    }
}
package mctest.chatfiltermc;

import mctest.chatfiltermc.util.ConfigUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.*;

public class ChatFilter implements Listener {
    private final ChatFilterMC plugin;
    private ConfigUtil filterConfig;
    private ConfigUtil historyConfig;
    private final HashMap<UUID, Integer> spamMap = new HashMap<>();
    private final HashMap<UUID, Date> lastMsgMap = new HashMap<>();
    private final List<String> groups;

    public ChatFilter(ChatFilterMC plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.setConfigs();
        this.groups = plugin.getGroupList();
    }

    //TODO
    // messages separate from notes (?)
    private void handleActions(UUID uuid, String tier, String msg) {
        this.historyConfig = plugin.getHistoryConfig();

        // Commands
        List<String> swearCommands = filterConfig.getConfig().getStringList("groups." + tier + ".commands");
        for (String command : swearCommands) {
            command = this.setCommand(command, uuid, msg);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }

        // Message to staff
        if (this.filterConfig.getConfig().getBoolean("groups." + tier + ".msgToStaffEnabled")) {
            String msgToStaff = this.filterConfig.getConfig().getString("groups." + tier + ".msgToStaff");
            if (msgToStaff.contains("[senderName]")) {
                msgToStaff = msgToStaff.replace("[senderName]", Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName());
            }

            Bukkit.broadcast(ChatColor.RED + msgToStaff, "filter");
        }

        // History
        if (this.filterConfig.getConfig().getBoolean("groups." + tier + ".history")) {
            this.setHistory(uuid, msg, tier);
        }
        //So that the config will update if any commands update the history
        plugin.setHistoryConfig(this.historyConfig);
    }

    //TODO Possibly pass through foundMap to be able to increment all counts instead of just highest tier
    private void setHistory(UUID uuid, String msg, String tier) {
        int cnt = !historyConfig.getConfig().contains(uuid + "." + tier + ".count") ? 1 : historyConfig.getConfig().getInt(uuid + "." + tier + ".count") + 1;
        this.historyConfig.getConfig().set(uuid + "." + tier + ".count", cnt);
        plugin.setHistoryConfig(this.historyConfig);

        int maxStrikes = this.filterConfig.getConfig().getInt("groups." + tier + ".maxStrikes");
        if (cnt >= maxStrikes && maxStrikes != -1) {
            List<String> actions = filterConfig.getConfig().getStringList("groups." + tier + ".strikeActions");
            for (String command : actions) {
                this.setCommand(command, uuid, msg);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }

    private String setCommand(String command, UUID uuid, String msg) {
        if (command.contains("[senderName] [msg]")) {
            command = command.replace("[senderName] [msg]", Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + ": \"" + msg + "\"");
        }
        if (command.contains("[senderName]")) {
            command = command.replace("[senderName]", Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName());
        }
        if (command.contains("[msg]")) {
            command = command.replace("[msg]", ": \"" + msg + "\"");
        }
        if (command.contains("[date]")) {
            Date now = new Date();
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            command = command.replace("[date]", "[" + format.format(now) + "]");
        }
        return command;
    }

    @EventHandler
    private void timeoutCheck(AsyncPlayerChatEvent event) {
        if (!plugin.getTimeoutMap().containsKey(event.getPlayer().getUniqueId())) return;

        Date date = new Date();
        long now = date.getTime();
        long dif = (plugin.getTimeoutMap().get(event.getPlayer().getUniqueId()) - now) / 1000;

        if (dif > 0) {
            if (dif > 60) {
                long min = dif / 60;
                long sec = dif % 60;
                event.getPlayer().sendMessage(ChatColor.RED + "Still timed out for " + min + " minutes and " + sec + " seconds.");
            }
            event.getPlayer().sendMessage(ChatColor.RED + "Still timed out for " + dif + " seconds.");
            event.setCancelled(true);
        } else {
            plugin.getTimeoutMap().remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    private void onChatSpamEvent(AsyncPlayerChatEvent event) {
        //TODO Uncomment this
//        if (event.getPlayer().isOp() || event.getPlayer().hasPermission("filter.exception")) return;
        UUID uuid = event.getPlayer().getUniqueId();
        String msg = event.getMessage();

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
                                    Bukkit.broadcast(ChatColor.RED + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + " is typing suspiciously fast...", "filter");
                                    event.setCancelled(true);

                                }
                            }.runTask(this.plugin);
                            List<String> swearCommands = filterConfig.getConfig().getStringList("bot.commands");
                            for (String command : swearCommands) {
                                command = this.setCommand(command, uuid, msg);
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                            }
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
                            Bukkit.broadcast(ChatColor.RED + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + " is spamming messages.", "filter");
                            List<String> swearCommands = filterConfig.getConfig().getStringList("spam.commands");
                            for (String command : swearCommands) {
                                command = this.setCommand(command, uuid, msg);
                                final String com = command;
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), com);
                                    }
                                }.runTask(this.plugin);
                            }
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

        List<String> enabled = new ArrayList<>();
        for (String tier : this.groups) {
            if (this.filterConfig.getConfig().getBoolean("groups." + tier + ".enabled")) {
                enabled.add(tier);
            }
        }
        if (enabled.isEmpty()) {
            return;
        }

        UUID uuid = event.getPlayer().getUniqueId();

        String message = event.getMessage().toLowerCase();
        String[] msg = message.split(" ");

        // Using these booleans instead of implementing actions when found so that
        // actions do not trigger multiple times and so that different groups will have different priority
        // IE: "blah swear blah slur, slur" will only trigger the slur handler and only once
        HashMap<String, Boolean> foundMap = new HashMap<>();
        for (String tier : enabled) {
            foundMap.put(tier, false);
        }

        for (String tier : enabled) {
            for (String rex : this.filterConfig.getConfig().getStringList("groups." + tier + ".regex")) {
                for (int i = 0; i < msg.length; i++) {
                    if (msg[i].toLowerCase().matches(rex)) {
                        foundMap.put(tier, true);
                        msg[i] = "****";
                    }
                }
                if (message.toLowerCase().matches(rex)) {
                    //Bukkit.getLogger().info("****FOUND****");
                }
            }
        }

        for (String tier : enabled) {
            if (foundMap.get(tier)) {
                // Need BukkitRunnable to run handlers since this event is Async
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        handleActions(uuid, tier, message);
                    }
                }.runTask(this.plugin);
                String mode = Objects.requireNonNull(this.filterConfig.getConfig().getString("groups." + tier + ".mode"));
                switch (mode) {
                    case "censor":
                        String clean = String.join(" ", msg);
                        event.setMessage(clean);
                        break;
                    case "replace":
                        String rep = "groups." + tier + ".replaceWith";
                        List<String> replaceList = new ArrayList<>(this.filterConfig.getConfig().getStringList(rep));
                        if (replaceList.isEmpty()) {
                            event.setCancelled(true);
                        }
                        try {
                            Random random = new Random();
                            int num = random.nextInt(replaceList.size());
                            event.setMessage(Objects.requireNonNull(replaceList.get(num)));
                        } catch (Exception e) {
                            Bukkit.getLogger().info("Replace list empty!");
                        }
                        break;
                    case "clear":
                        event.setCancelled(true);
                        break;
                    default:
                        break;
                }
                return;
            }
        }
    }

    public void setConfigs(){
        this.filterConfig = this.plugin.getFilterConfig();
        this.historyConfig = this.plugin.getHistoryConfig();
    }
}
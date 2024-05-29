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

import java.util.*;

public class ChatFilter implements Listener {
    private final ChatFilterMC plugin;
    private List<String> swearList = new ArrayList<>();
    private List<String> slurList = new ArrayList<>();
    private final ConfigUtil filterConfig;
    private List<String> slurCommands = new ArrayList<>();
    private List<String> swearCommands = new ArrayList<>();
    public ChatFilter(ChatFilterMC plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.filterConfig = plugin.getFilterConfig();

        this.swearList = filterConfig.getConfig().getStringList("swears.Regex");
        this.slurList = filterConfig.getConfig().getStringList("slurs.Regex");

    }

    public void handleSlurs(UUID uuid) {
        this.slurCommands = filterConfig.getConfig().getStringList("slurs.Commands");
        for (int i = 0; i < this.slurCommands.size(); i++) {
            String command = this.slurCommands.get(i);
            if (command.contains("[senderName]")) {
                command = command.replace("[senderName]", Objects.requireNonNull(Bukkit.getPlayer(uuid)).getDisplayName());
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        }
        //TODO this might be a command ^
        Bukkit.broadcastMessage(ChatColor.RED + Bukkit.getPlayer(uuid).getDisplayName() + " is a loser");
    }

    //TODO Swears might not need separate function... TBD
    public void handleSwears(UUID uuid) {
        this.swearCommands = filterConfig.getConfig().getStringList("swears.Commands");
        for (int i = 0; i < this.swearCommands.size(); i++) {
            String command = this.swearCommands.get(i);
            if (command.contains("[senderName]")) {
                command = command.replace("[senderName]", Objects.requireNonNull(Bukkit.getPlayer(uuid)).getDisplayName());
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        }
    }

    @EventHandler
    private void onPlayerChatEvent(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        String[] msg = message.split(" ");

        // Using these booleans instead of implementing actions when found so that
        // actions do not trigger multiple times and so that slurs will have a higher priority
        // IE: "blah swear blah slur, slur" will only trigger the slur handler and only once
        boolean slurFound = false;
        boolean swearFound = false;

        for (int i = 0; i < msg.length; i++) {
            if (this.filterConfig.getConfig().getBoolean("slurs.Enabled")) {
                for (String rex : this.slurList) {
                    if (msg[i].matches(rex)) {
                        slurFound = true;
                        //TODO if statement not necessary, not sure how/if effects efficiency
                        if (Objects.equals(this.filterConfig.getConfig().getString("slurs.Mode"), "Censor")) {
                            msg[i] = "****";
                        }
                    }
                }
            }
            if (this.filterConfig.getConfig().getBoolean("swears.Enabled")) {
                for (String rex : this.swearList) {
                    if (msg[i].matches(rex)) {
                        swearFound = true;

                        if (Objects.equals(this.filterConfig.getConfig().getString("swears.Mode"), "Censor")) {
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
                    handleSlurs(event.getPlayer().getUniqueId());
                }
            }.runTask(this.plugin);

            switch (Objects.requireNonNull(this.filterConfig.getConfig().getString("slurs.Mode"))) {
                case "Censor":
                    String clean = String.join(" ", msg);;
                    event.setMessage(clean);
                    break;
                case "Replace":
                    event.setMessage(Objects.requireNonNull(filterConfig.getConfig().getString("slurs.ReplaceWith")));
                    break;
                case "Clear":
                    event.setCancelled(true);
                    break;
                default:
                    break;
            }
        } else if (swearFound) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    handleSwears(event.getPlayer().getUniqueId());
                }
            }.runTask(this.plugin);

            switch (Objects.requireNonNull(this.filterConfig.getConfig().getString("swears.Mode"))) {
                case "Censor":
                    String clean = String.join(" ", msg);;
                    event.setMessage(clean);
                    break;
                case "Replace":
                    event.setMessage(Objects.requireNonNull(filterConfig.getConfig().getString("swears.ReplaceWith")));
                    break;
                case "Clear":
                    event.setCancelled(true);
                    break;
                default:
                    break;
            }
        }
    }
}
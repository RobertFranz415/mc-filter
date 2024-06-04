package mctest.chatfiltermc;

import mctest.chatfiltermc.commands.Filter;
import mctest.chatfiltermc.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ChatFilterMC extends JavaPlugin {
    private ConfigUtil filterConfig;
    private ConfigUtil historyConfig;
    @Override
    public void onEnable() {
        // Plugin startup logic
        this.saveResource("FilterList.yml", false);
        this.saveResource("History.yml", false);
        this.filterConfig = new ConfigUtil(this, "FilterList.yml");
        this.filterConfig.save();
        this.historyConfig = new ConfigUtil(this, "History.yml");
        this.historyConfig.save();

        Objects.requireNonNull(getCommand("filter")).setExecutor(new Filter(this));
        new ChatFilter(this);
    }

    public ConfigUtil getFilterConfig() {
        return this.filterConfig;
    }

    public void setFilterConfig(ConfigUtil filterConfig) {
        this.filterConfig = filterConfig;
        this.filterConfig.save();
    }

    public ConfigUtil getHistoryConfig() {
        return this.historyConfig;
    }

    public void setHistoryConfig(ConfigUtil historyConfig) {
        this.historyConfig = historyConfig;
        this.historyConfig.save();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

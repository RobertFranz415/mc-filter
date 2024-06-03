package mctest.chatfiltermc;

import mctest.chatfiltermc.commands.Filter;
import mctest.chatfiltermc.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ChatFilterMC extends JavaPlugin {
    private ConfigUtil filterConfig;
    @Override
    public void onEnable() {
        // Plugin startup logic
        this.saveResource("FilterList.yml", false);
        this.filterConfig = new ConfigUtil(this, "FilterList.yml");
        filterConfig.save();

        Objects.requireNonNull(getCommand("filter")).setExecutor(new Filter(this));
        new ChatFilter(this);
    }

    public ConfigUtil getFilterConfig() {
        return this.filterConfig;
    }

    public void setFilterConfig(ConfigUtil filterConfig) {
        this.filterConfig = filterConfig;
        filterConfig.save();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

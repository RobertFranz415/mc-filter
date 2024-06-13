package mctest.chatfiltermc;

import mctest.chatfiltermc.commands.Filter;
import mctest.chatfiltermc.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

public final class ChatFilterMC extends JavaPlugin {
    private ConfigUtil filterConfig;
    private ConfigUtil historyConfig;
    private ConfigUtil libraryConfig;
    private ConfigUtil wordList;
    private RegexBuilder regexBuilder;
    private ChatFilter chatFilter;
    @Override
    public void onEnable() {
        // Plugin startup logic
        this.saveResource("FilterList.yml", false);
        this.saveResource("History.yml", false);
        this.saveResource("Library.yml", false);
        this.saveResource("WordList.yml", false);

        this.filterConfig = new ConfigUtil(this, "FilterList.yml");
        this.filterConfig.save();
        this.historyConfig = new ConfigUtil(this, "History.yml");
        this.historyConfig.save();
        this.libraryConfig = new ConfigUtil(this, "Library.yml");
        this.libraryConfig.save();
        this.wordList = new ConfigUtil(this, "WordList.yml");
        this.wordList.save();

        Objects.requireNonNull(getCommand("filter")).setExecutor(new Filter(this));
        this.chatFilter = new ChatFilter(this);
        this.regexBuilder = new RegexBuilder(this);

        try{
            this.regexBuilder.buildSwearRegex();
            this.regexBuilder.buildSlurRegex();
        }catch (OutOfMemoryError e){
            Bukkit.getLogger().info("Out of memory. Attempting to try again once server is finished starting up.");
            Bukkit.getScheduler().runTaskLater(this, () -> {
                this.regexBuilder.buildSwearRegex();
                this.regexBuilder.buildSlurRegex();
            }, 1);
        }
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

    public ConfigUtil getLibraryConfig() { return this.libraryConfig; }
    public void setLibraryConfig(ConfigUtil libraryConfig){
        this.libraryConfig = libraryConfig;
        this.libraryConfig.save();
    }

    public ConfigUtil getWordList() { return this.wordList; }
    public void setWordList(ConfigUtil wordList){
        this.wordList = wordList;
        this.wordList.save();
    }

    private void reloadConfigs(){
        this.filterConfig = new ConfigUtil(this, "FilterList.yml");
        this.filterConfig.save();
        this.historyConfig = new ConfigUtil(this, "History.yml");
        this.historyConfig.save();
        this.libraryConfig = new ConfigUtil(this, "Library.yml");
        this.libraryConfig.save();
        this.wordList = new ConfigUtil(this, "WordList.yml");
        this.wordList.save();

        this.regexBuilder.setConfigs();
        this.chatFilter.setConfigs();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

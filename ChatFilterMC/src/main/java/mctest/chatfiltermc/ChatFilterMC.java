package mctest.chatfiltermc;

import mctest.chatfiltermc.commands.Filter;
import mctest.chatfiltermc.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class ChatFilterMC extends JavaPlugin {
    private ConfigUtil filterConfig;
    private ConfigUtil historyConfig;
    private ConfigUtil libraryConfig;
    private ConfigUtil wordList;
    private RegexBuilder regexBuilder;
    private ChatFilter chatFilter;
    private List<String> groupList;
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

        this.initGroupList();

        Objects.requireNonNull(getCommand("filter")).setExecutor(new Filter(this));
        this.chatFilter = new ChatFilter(this);
        this.regexBuilder = new RegexBuilder(this);

        try{
            for (String tier : this.getGroupList()) {
                this.regexBuilder.buildRegex(tier);
            }
        }catch (OutOfMemoryError e){
            Bukkit.getLogger().info("Out of memory. Attempting to try again once server is finished starting up.");
            Bukkit.getScheduler().runTaskLater(this, () -> {
                for (String tier : this.getGroupList()) {
                    this.regexBuilder.buildRegex(tier);
                }
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

    private void initGroupList() {
        try {
            List<String> unordered = new ArrayList<>(Objects.requireNonNull(this.getFilterConfig().getConfig().getConfigurationSection("groups")).getKeys(false));
            this.groupList = new ArrayList<>(Collections.nCopies(unordered.size(), null));
            for (String group : unordered) {
                int level = this.getFilterConfig().getConfig().getInt("groups." + group + ".level") - 1;
                this.groupList.set(level, group);
            }
        } catch (Exception e) {
            Bukkit.getLogger().info("Filter groups empty!");
        }
    }
    public void setGroupList(List<String> groupList) {
        this.groupList = groupList;
    }
    public List<String> getGroupList() {
        return this.groupList;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

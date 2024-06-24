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
    private HashMap<UUID, Long> timeoutMap = new HashMap<>();
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

    public void initGroupList() {
        try {
            List<String> unordered = new ArrayList<>(Objects.requireNonNull(this.getFilterConfig().getConfig().getConfigurationSection("groups")).getKeys(false));
            this.groupList = new ArrayList<>(Collections.nCopies(unordered.size(), null));
            // First insert the groups into the correct position.  If a group's level is already taken move on.
            for (int i = 0; i < unordered.size(); i++) {
                if (this.getFilterConfig().getConfig().getInt("groups." + unordered.get(i) + ".level") > unordered.size()) {
                    this.getFilterConfig().getConfig().set("groups." + unordered.get(i) + ".level", unordered.size());
                    this.filterConfig.save();
                }
                int level = this.getFilterConfig().getConfig().getInt("groups." + unordered.get(i) + ".level") - 1;
                if (this.groupList.get(level) == null) {
                    this.groupList.set(level, unordered.get(i));
                    unordered.set(i, null);
                }
            }
            // Insert any remaining groups into the empty positions
            for (int i = 0; i < unordered.size(); i++) {
                if (unordered.get(i) != null) {
                    int j = 0;
                    while (this.groupList.get(j) != null) {
                        if (++j >= unordered.size()) {
                            j = 0;
                        }
                    }
                    this.groupList.set(j, unordered.get(i));
                    unordered.set(i, null);
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().info("Error setting up filter groups list!");
        }
    }
    public void setGroupList(List<String> groupList) {
        this.groupList = groupList;
        this.initGroupList();
    }
    public List<String> getGroupList() {
        return this.groupList;
    }

    public void setTimeoutMap(HashMap<UUID, Long> map) {
        this.timeoutMap = map;
    }

    public HashMap<UUID, Long> getTimeoutMap() {
        return this.timeoutMap;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

package mctest.chatfiltermc;

import mctest.chatfiltermc.commands.Filter;
import mctest.chatfiltermc.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.*;

public final class ChatFilterMC extends JavaPlugin {
    private ConfigUtil filterConfig;
    private ConfigUtil historyConfig;
    private ConfigUtil libraryConfig;
    private ConfigUtil wordListConfig;
    private ConfigUtil wordList;
    private RegexBuilder regexBuilder;
    private ChatFilter chatFilter;
    private List<String> groupList;
    private HashMap<UUID, Long> timeoutMap = new HashMap<>();
    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getLogger().info("Starting ChatFilterMC...");

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

        if (filterConfig.getConfig().getBoolean("autoDelete.enabled")) {
            Bukkit.getLogger().info("AUTO DELETE ENABLED");
            this.checkExpiredHistory();
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

    public void reloadConfigs(){
        this.filterConfig = new ConfigUtil(this, "FilterList.yml");
        this.filterConfig.save();

        this.historyConfig = new ConfigUtil(this, "History.yml");
        this.historyConfig.save();

        this.libraryConfig = new ConfigUtil(this, "Library.yml");
        this.libraryConfig.save();

        this.wordList = new ConfigUtil(this, "WordList.yml");
        this.wordList.save();

        this.initGroupList();

        this.chatFilter.setConfigs();
        this.regexBuilder.setConfigs();
        this.chatFilter.setConfigs();

        // Rebuilding regex statements
        for (String tier : this.getGroupList()) {
            this.regexBuilder.buildRegex(tier);
        }

    }

    public void reloadGroupList() {
        this.initGroupList();
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
    public RegexBuilder getRegexBuilder() {
        return this.regexBuilder;
    }

    private void checkExpiredHistory() {
        int[] months = {0, 31, 59, 89, 120, 151, 181, 212, 243, 273, 304, 334};
        ArrayList<String> keys = new ArrayList<>(this.historyConfig.getConfig().getKeys(false));
        for (String key : keys) {
            if (!this.historyConfig.getConfig().contains(key + ".latest")) {
                continue;
            }

            String[] date = Objects.requireNonNull(this.historyConfig.getConfig().getString(key + ".latest")).split("/");
            int days = months[Integer.parseInt(date[0])] + Integer.parseInt(date[1]) + (Integer.parseInt(date[2]) - 2000) * 365;
            days += filterConfig.getConfig().getInt("autoDelete.time");

            Date now = new Date();
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            String[] curDate = format.format(now).split("/");
            int curDays = months[Integer.parseInt(curDate[0])] + Integer.parseInt(curDate[1]) + (Integer.parseInt(curDate[2]) - 2000) * 365;

            if (days - curDays <= 0) {
                this.historyConfig.getConfig().set(key, null);
            }
        }
        this.historyConfig.save();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

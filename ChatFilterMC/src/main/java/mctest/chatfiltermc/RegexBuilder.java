package mctest.chatfiltermc;

import mctest.chatfiltermc.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class RegexBuilder {
    private final ChatFilterMC plugin;
    private ConfigUtil filterConfig;
    private ConfigUtil wordList;
    private ConfigUtil library;

    public RegexBuilder(ChatFilterMC plugin){
        this.plugin = plugin;
        this.setConfigs();
    }

    public void buildRegex(String tier) {
        List<String> words = wordList.getConfig().getStringList(tier);
        List<String> newStatements = new ArrayList<>();

        List<Character> vowels = Arrays.asList('a', 'e', 'i', 'o', 'u', 'y');

        for (String s : words) {
            char[] word = s.toCharArray();
            StringBuilder statement = new StringBuilder();

            if (filterConfig.getConfig().getBoolean("groups." + tier + ".partialMatches")) {
                statement.append("(\\A|\\s)");
            }

            //TODO Add more regex stuff
            statement.append("^.");

            for (int i = 0; i < word.length; i++) {
                List<String> altLetters = library.getConfig().getStringList(word[i] + "");
                statement.append("*(");
                for (int j = 0; j < altLetters.size(); j++) {
                    if (j != 0) {
                        statement.append("|");
                        statement.append(altLetters.get(j));
                    } else {
                        statement.append(altLetters.get(j));
                    }
                }
                //maybe add this after all instead of just vowels
                if ((i < word.length - 1) && (vowels.contains(word[i]))) {
                    statement.append(")?+\\s");
                }
                else if (i < word.length - 1) {
                    statement.append(")+\\s");
                }

            }
            statement.append(")+.*$");

            if (filterConfig.getConfig().getBoolean("groups." + tier + ".partialMatches")) {
                statement.append("(\\z|\\s)");
            }

            newStatements.add(statement.toString());
        }

        filterConfig.getConfig().set("groups." + tier + ".regex", newStatements);
        filterConfig.save();
    }

    public void setConfigs(){
        this.filterConfig = plugin.getFilterConfig();
        this.wordList = plugin.getWordList();
        this.library = plugin.getLibraryConfig();
    }
}

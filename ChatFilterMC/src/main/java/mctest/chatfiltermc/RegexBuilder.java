package mctest.chatfiltermc;

import mctest.chatfiltermc.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RegexBuilder {
    private final ChatFilterMC plugin;
    private ConfigUtil filterConfig;
    private ConfigUtil wordList;
    private ConfigUtil library;

    public RegexBuilder(ChatFilterMC plugin){
        this.plugin = plugin;
        this.setConfigs();
    }

//    public void buildSlurRegex(){
//        List<String> words = wordList.getConfig().getStringList("slurs");
//        List<String> newStatements = new ArrayList<>();
//
//        for (int i = 0; i < words.size(); i++) {
//            int first = 0;
//            char[] word = words.get(i).toCharArray();
//            Bukkit.getLogger().severe("CHAT FILTER MC WORD: " + Arrays.toString(word));
//            StringBuilder statement = new StringBuilder();
//
//            if(!filterConfig.getConfig().getBoolean("slurs.partialMatches")){
//                statement.append("(\\A|\\s) ");
//            }
//
//            statement.append("^.*(");
//
//            for(char c : word){
//                List<String> altLetters = library.getConfig().getStringList(c+"");
//                Bukkit.getLogger().severe("CHAT FILTER MC c: " + c);
//                for(String l : altLetters){
//                    if(first != 0){
//                        statement.append("|");
//                        statement.append(l);
//                    }else{
//                        statement.append(l);
//                        first ++;
//                    }
//                }
//            }
//            statement.append(")+.*$");
//
//            if(!filterConfig.getConfig().getBoolean("slurs.partialMatches")){
//                statement.append(" (\\z|\\s)");
//            }
//
//            newStatements.add(statement.toString());
//        }
//
//        filterConfig.getConfig().set("slurs.regex", newStatements);
//        filterConfig.save();
//    }
//
//    public void buildSwearRegex(){
//        List<String> words = wordList.getConfig().getStringList("swears");
//        List<String> newStatements = new ArrayList<>();
//
//        for (int i = 0; i < words.size(); i++) {
//            int first = 0;
//            char[] word = words.get(i).toCharArray();
//            StringBuilder statement = new StringBuilder();
//
//            if(filterConfig.getConfig().getBoolean("swears.partialMatches")){
//                statement.append("(\\A|\\s)");
//            }
//
//            statement.append("(");
//
//            for(char c : word){
//                List<String> altLetters = library.getConfig().getStringList(c+"");
//                Bukkit.getLogger().severe("CHAT FILTER MC c: " + c);
//                for(String l : altLetters){
//                    if(first != 0){
//                        statement.append("|");
//                        statement.append(l);
//                    }else{
//                        statement.append(l);
//                        first ++;
//                    }
//                }
//            }
//            statement.append(")+");
//
//            if(filterConfig.getConfig().getBoolean("swears.partialMatches")){
//                statement.append("(\\z|\\s)");
//            }
//
//            newStatements.add(statement.toString());
//        }
//
//        filterConfig.getConfig().set("swears.regex", newStatements);
//        filterConfig.save();
//    }

    public void buildRegex(String tier) {
        List<String> words = wordList.getConfig().getStringList(tier);
        List<String> newStatements = new ArrayList<>();

        for (String s : words) {
            int first = 0;
            char[] word = s.toCharArray();
            StringBuilder statement = new StringBuilder();

            if (filterConfig.getConfig().getBoolean(tier + ".partialMatches")) {
                statement.append("(\\A|\\s)");
            }

            statement.append("^.*(");

            for (char c : word) {
                List<String> altLetters = library.getConfig().getStringList(c + "");
                Bukkit.getLogger().severe("CHAT FILTER MC c: " + c);
                for (String l : altLetters) {
                    if (first != 0) {
                        statement.append("|");
                        statement.append(l);
                    } else {
                        statement.append(l);
                        first++;
                    }
                }
            }
            statement.append(")+.*$");

            if (filterConfig.getConfig().getBoolean(tier + ".partialMatches")) {
                statement.append("(\\z|\\s)");
            }

            newStatements.add(statement.toString());
        }

        filterConfig.getConfig().set(tier + ".regex", newStatements);
        filterConfig.save();
    }

    public void setConfigs(){
        this.filterConfig = plugin.getFilterConfig();
        this.wordList = plugin.getWordList();
        this.library = plugin.getLibraryConfig();
    }
}

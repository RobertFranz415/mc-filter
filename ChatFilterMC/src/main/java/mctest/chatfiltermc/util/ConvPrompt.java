package mctest.chatfiltermc.util;

import mctest.chatfiltermc.commands.Filter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;

import java.util.UUID;

public class ConvPrompt extends StringPrompt {

    private final Filter filter;
    private final String action;
    private final String group;
    private final CommandSender sender;
    private final UUID uuid;
    public ConvPrompt(Filter filter, CommandSender sender, UUID uuid, String action) {
        this.filter = filter;
        this.action = action;
        this.sender = sender;
        this.uuid = uuid;
        this.group = null;
    }
    public ConvPrompt(Filter filter, CommandSender sender, String action) {
        this.filter = filter;
        this.action = action;
        this.sender = sender;
        this.uuid = null;
        this.group = null;
    }
    public ConvPrompt(Filter filter, CommandSender sender, String group, String action) {
        this.filter = filter;
        this.action = action;
        this.sender = sender;
        this.group = group;
        this.uuid = null;
    }
    @Override
    public String getPromptText(ConversationContext conversationContext) {
        String prompt;

        switch (action) {
            case "addNote":
                prompt = "Enter note:";
                break;
            case "removeNote":
                prompt = "Enter number of note to remove:";
                break;
            case "clearStrikes":
                prompt = "Enter group of strikes to clear:";
                break;
            case "setStrikes":
                prompt = "Enter the filter group and number of strikes:";
                break;
            case "createGroup":
                prompt = "Enter name of new filter group:";
                break;
            case "editStaffMsg":
                prompt = "Enter new staff message:";
                break;
            case "setMaxStrikes":
                prompt = "Enter max strikes:";
                break;
            case "addStrikeCommand":
                prompt = "Enter new max strike command:";
                break;
            case "removeStrikeCommand":
                prompt = "Enter the number of the command you want to remove:";
                break;
            case "addWord":
                prompt = "Enter word to add:";
                break;
            case "removeWord":
                prompt = "Enter word to remove:";
                break;
            case "addCommand":
                prompt = "Enter new command to add:";
                break;
            case "removeCommand":
                prompt = "Enter the number of the command to remove:";
                break;
            case "addReplacement":
                prompt = "Enter new replacement message:";
                break;
            case "removeReplacement":
                prompt = "Enter the number of the replacement message you want to remove:";
                break;
            default:
                prompt = "Enter input:";
                break;
        }
        return ChatColor.AQUA + prompt;
    }

    @Override
    public Prompt acceptInput(ConversationContext conversationContext, String s) {
        switch (action) {
            case "addNote":
                filter.addPlayerNotes(sender, uuid, s);
                break;
            case "removeNote":
                filter.removePlayerNote(sender, uuid, s);
                break;
            case "clearStrikes":
                filter.clearPlayerStrikes(sender, uuid, s);
                break;
            case "setStrikes":
                filter.setPlayerStrikes(sender, uuid, s.split(" ")[0], s.split(" ")[1]);
                break;
            case "createGroup":
                filter.createGroup(sender, s);
                sender.sendMessage(ChatColor.GREEN + "Group created!");
                break;
            case "editStaffMsg":
                filter.editStaffMessage(sender, "all", s);
                break;
            case "setMaxStrikes":
                filter.setMaxStrikes(sender, group, s);
                break;
            case "addStrikeCommand":
                filter.addStrikeCommands(sender, group, s);
                break;
            case "removeStrikeCommand":
                filter.removeStrikeCommands(sender, group, s);
                break;
            case "addWord":
                filter.addWord(sender, group, s);
                break;
            case "removeWord":
                filter.removeWord(sender, group, s);
                break;
            case "addCommand":
                filter.addCommands(sender, group, s);
                break;
            case "removeCommand":
                filter.removeCommands(sender, group, s);
                break;
            case "addReplacement":
                filter.addReplacements(sender, group, s);
                break;
            case "removeReplacement":
                filter.removeReplacements(sender, group, s);
                break;
            default:
                break;
        }

        return null;
    }
}

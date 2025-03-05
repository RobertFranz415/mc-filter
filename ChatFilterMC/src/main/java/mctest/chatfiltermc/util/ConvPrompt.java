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
    private final CommandSender sender;
    private final UUID uuid;
    public ConvPrompt(Filter filter, CommandSender sender, UUID uuid, String action) {
        this.filter = filter;
        this.action = action;
        this.sender = sender;
        this.uuid = uuid;
    }
    public ConvPrompt(Filter filter, CommandSender sender, String action) {
        this.filter = filter;
        this.action = action;
        this.sender = sender;
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
                prompt = "Enter the filter group and number of strikes";
                break;
            case "createGroup":
                prompt = "Enter name of new filter group:";
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
            default:
                break;
        }

        return null;
    }
}

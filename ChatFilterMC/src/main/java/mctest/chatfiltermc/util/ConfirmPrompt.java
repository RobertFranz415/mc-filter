package mctest.chatfiltermc.util;

import mctest.chatfiltermc.ChatFilterMC;
import mctest.chatfiltermc.commands.Filter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;

public class ConfirmPrompt extends ValidatingPrompt {
    private final Filter filter;
    private final String group;
    private final CommandSender sender;
    public ConfirmPrompt(Filter filter, CommandSender sender, String group) {
        this.filter = filter;
        this.group = group;
        this.sender = sender;
    }

    @Override
    protected boolean isInputValid(ConversationContext conversationContext, String s) {
        return (s.equalsIgnoreCase("y") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("n") || s.equalsIgnoreCase("no"));
    }

    @Override
    protected Prompt acceptValidatedInput(ConversationContext conversationContext, String s) {
        if (s.equalsIgnoreCase("y") || s.equalsIgnoreCase("yes")) {
            filter.removeGroup(sender, group);
            sender.sendMessage(ChatColor.GREEN + "Filter group " + group + " removed.");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Did not remove " + this.group + "!");
        }

        return null;
    }

    @Override
    public String getPromptText(ConversationContext conversationContext) {
        return "Are you sure you want to delete the " + this.group + " group? (y/n)";
    }
}

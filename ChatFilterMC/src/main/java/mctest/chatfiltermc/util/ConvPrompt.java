package mctest.chatfiltermc.util;

import mctest.chatfiltermc.commands.Filter;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;

public class ConvPrompt extends StringPrompt {

    private final Filter filter;
    private final String group;
    private final String[] args;
    private final CommandSender sender;
    public ConvPrompt(Filter filter, CommandSender sender, String[] args, String group) {
        this.filter = filter;
        this.group = group;
        this.args = args;
        this.sender = sender;
    }
    @Override
    public String getPromptText(ConversationContext conversationContext) {
        return "Are you sure you want to delete the " + group + " group?";
    }

    @Override
    public Prompt acceptInput(ConversationContext conversationContext, String s) {
        if (s.equalsIgnoreCase("y") || s.equalsIgnoreCase("n")) {
            filter.removeGroup(args, sender);
        }
        return null;
    }
}

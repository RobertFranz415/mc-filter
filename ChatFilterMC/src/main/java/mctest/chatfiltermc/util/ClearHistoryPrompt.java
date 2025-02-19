package mctest.chatfiltermc.util;

import mctest.chatfiltermc.commands.Filter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;

import java.util.Objects;
import java.util.UUID;

public class ClearHistoryPrompt extends StringPrompt {

    private final Filter filter;
    private final UUID uuid;
    private final CommandSender sender;
    public ClearHistoryPrompt(Filter filter, CommandSender sender, UUID uuid) {
        this.filter = filter;
        this.uuid = uuid;
        this.sender = sender;
    }
    @Override
    public String getPromptText(ConversationContext conversationContext) {
        return ChatColor.AQUA + "Are you sure you want to wipe the history for " + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + "?";
    }

    @Override
    public Prompt acceptInput(ConversationContext conversationContext, String s) {
        if (s.equalsIgnoreCase("y") || s.equalsIgnoreCase("n")) {
            filter.wipeHistory(this.sender, this.uuid);
        }
        return null;
    }
}

package me.dankofuk.discord.commands.botRequiredCommands;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import me.dankofuk.discord.DiscordBot;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import me.dankofuk.utils.ColorUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.event.Listener;

public class SuggestionCommand extends ListenerAdapter implements CommandExecutor, Listener {
    private DiscordBot discordBot;
    private String channelId;
    private String threadId;
    private String suggestionMessage;
    private String noPermissionMessage;
    private String suggestionUsageMessage;
    private String responseMessage;
    private long cooldown;
    private Map<UUID, Long> cooldowns = new HashMap<>();
    private String title;
    private String description;
    private String footer;
    private Color color;
    private String thumbnailUrl;
    public FileConfiguration config;


    public SuggestionCommand(DiscordBot discordBot, String channelId, String threadId, String suggestionMessage,
                                     String noPermissionMessage, String suggestionUsageMessage, String responseMessage, long cooldown, String title, String description, String footer, Color color, String thumbnailUrl, FileConfiguration config) {
        this.discordBot = discordBot;
        this.channelId = channelId;
        this.threadId = threadId;
        this.suggestionMessage = suggestionMessage;
        this.noPermissionMessage = noPermissionMessage;
        this.suggestionUsageMessage = suggestionUsageMessage;
        this.responseMessage = responseMessage;
        this.cooldown = cooldown;
        this.title = title;
        this.description = description;
        this.footer = footer;
        this.color = color;
        this.thumbnailUrl = thumbnailUrl;
        this.config = config;

        if (!config.getBoolean("bot.enabled")) {
            Bukkit.getLogger().warning("[SuggestionCommandListener] Bot is not enabled. Suggestion command will not function.");
        }
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        if (!config.getBoolean("bot.enabled")) {
            sender.sendMessage("The bot is not enabled. Suggestion command will not function.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("commandlogger.suggest.use")) {
            player.sendMessage(ColorUtils.translateColorCodes(noPermissionMessage));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ColorUtils.translateColorCodes(suggestionUsageMessage));
            return true;
        }

        String suggest = String.join(" ", args);
        long currentTime = System.currentTimeMillis();
        long lastSuggestTime = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long timeRemaining = (lastSuggestTime + (cooldown * 1000L) - currentTime) / 1000L;

        if (timeRemaining > 0) {
            String cooldownMessage = "&cPlease wait " + timeRemaining + " seconds before submitting another suggestion.";
            player.sendMessage(ColorUtils.translateColorCodes(cooldownMessage));
            return true;
        }

        sendSuggestion(player, suggest);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // Send a separate message to the player
        player.sendMessage(ColorUtils.translateColorCodes(responseMessage.replace("%suggestion%", suggest)));

        return true;
    }

    private void sendSuggestion(Player player, String suggestion) {
        CompletableFuture.runAsync(() -> {
            try {
                TextChannel channel = discordBot.getJda().getTextChannelById(channelId);
                if (channel == null) {
                    Bukkit.getLogger().warning("[SuggestionCommandListener] Invalid channel ID specified: " + channelId);
                    return;
                }

                String playerName = player.getName();
                String playerUUID = player.getUniqueId().toString();

                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle(title.replace("%suggestion%", suggestion).replace("%player%", playerName).replace("%player_uuid%", playerUUID));
                embed.setDescription(description.replace("%suggestion%", suggestion).replace("%player%", playerName).replace("%player_uuid%", playerUUID));
                embed.setColor(color);
                embed.setFooter(footer.replace("%suggestion%", suggestion).replace("%player%", playerName).replace("%player_uuid%", playerUUID), null);
                embed.setThumbnail(thumbnailUrl);

                channel.sendMessageEmbeds(embed.build()).queue(sentMessage -> {
                    sentMessage.addReaction(Emoji.fromUnicode("U+1F44D")).queue(); // thumbs up reaction
                    sentMessage.addReaction(Emoji.fromUnicode("U+1F44E")).queue(); // thumbs down reaction
                });
            } catch (Exception e) {
                Bukkit.getLogger().warning("[SuggestionCommandListener] Error sending suggestion: " + e.getMessage());
            }
        });
    }




    public void reloadConfigOptions(DiscordBot discordBot, String channelId, String threadId, String suggestionMessage, String noPermissionMessage, String suggestionUsageMessage, String responseMessage, long cooldown, String title, String description, String footer, Color color, String thumbnailUrl) {
        this.discordBot = discordBot;
        this.channelId = channelId;
        this.threadId = threadId;
        this.suggestionMessage = suggestionMessage;
        this.noPermissionMessage = noPermissionMessage;
        this.suggestionUsageMessage = suggestionUsageMessage;
        this.responseMessage = responseMessage;
        this.cooldown = cooldown;
        this.title = title;
        this.description = description;
        this.footer = footer;
        this.color = color;
        this.thumbnailUrl = thumbnailUrl;
    }
}

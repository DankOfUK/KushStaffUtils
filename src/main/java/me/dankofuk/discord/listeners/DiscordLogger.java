package me.dankofuk.discord.listeners;

import me.dankofuk.discord.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DiscordLogger extends ListenerAdapter {
    private List<String> messageFormats;

    private String serverName;

    private List<String> embedTitleFormats;

    public boolean logAsEmbed;

    public String logChannelId;

    public DiscordBot discordBot;

    private static DiscordLogger instance;

    public DiscordLogger(DiscordBot discordBot, List<String> messageFormat, List<String> embedTitleFormat, String serverName, boolean logAsEmbed, String logChannelId) {
            this.discordBot = discordBot;
            this.messageFormats = messageFormat;
            this.serverName = serverName;
            this.embedTitleFormats = embedTitleFormat;
            this.logAsEmbed = logAsEmbed;
            this.logChannelId = logChannelId;
            instance = this;
    }
    public static DiscordLogger getInstance() {
        return instance;
    }

    public void reloadMessageFormats(List<String> messageFormats) {
        this.messageFormats = messageFormats;
    }

    public void reloadEmbedTitleFormats(List<String> embedTitleFormats) {
        this.embedTitleFormats = embedTitleFormats;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void reloadLogChannelID(String logChannelId) {
        this.logChannelId = logChannelId;
    }

    public void logCommand(String command, String playerName) {
        CompletableFuture.runAsync(() -> {
            List<String> messages = new ArrayList<>();
            List<String> embedTitles = new ArrayList<>();
            long time = System.currentTimeMillis() / 1000L;
            for (String messageFormat : this.messageFormats) {
                String message = messageFormat.replace("%player%", playerName).replace("%time%", "<t:" + time + ":R>").replace("%server%", serverName).replace("%command%", command);
                messages.add(message);
            }
            for (String embedTitleFormat : this.embedTitleFormats) {
                String embedTitle = embedTitleFormat.replace("%player%", playerName).replace("%time%", (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date())).replace("%server%", serverName).replace("%command%", command);
                embedTitles.add(embedTitle);
            }
            String playerHeadUrl = getPlayerHeadUrl(playerName);
            sendToDiscord(messages, embedTitles, playerHeadUrl, discordBot, logChannelId);
        });
    }

    public String getPlayerHeadUrl(String playerName) {
        String playerHeadUrl = "";
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "KushStaffLogger");
            connection.setDoOutput(true);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject)parser.parse(new InputStreamReader(connection.getInputStream()));
            String playerUuid = json.get("id").toString();
            playerHeadUrl = "https://crafatar.com/avatars/" + playerUuid + "?overlay=head";
        } catch (IOException|org.json.simple.parser.ParseException e) {
            e.printStackTrace();
        }
        return playerHeadUrl;
    }

    private void sendToDiscord(List<String> messages, List<String> embedTitles, String playerHeadUrl, DiscordBot jda, String logChannelId) {
        CompletableFuture.runAsync(() -> {
            if (logChannelId == null || logChannelId.isEmpty()) {
                Bukkit.getLogger().warning("[DiscordLogger] No log channel specified.");
                return;
            }
            try {
                TextChannel channel = discordBot.getJda().getTextChannelById(logChannelId);
                if (channel == null) {
                    Bukkit.getLogger().warning("[DiscordLogger] Invalid log channel ID specified: " + logChannelId);
                    return;
                }
                for (int i = 0; i < messages.size(); i++) {
                    String message = messages.get(i);
                    if (logAsEmbed) {
                        String embedTitle = embedTitles.get(i);
                        EmbedBuilder embedBuilder = new EmbedBuilder();
                        embedBuilder.setTitle(embedTitle);
                        embedBuilder.setDescription(message);
                        embedBuilder.setThumbnail(playerHeadUrl);
                        channel.sendMessageEmbeds(embedBuilder.build()).queue();
                    } else {
                        channel.sendMessage(message).queue();
                    }
                }
            } catch (NumberFormatException e) {
                Bukkit.getLogger().warning("[DiscordLogger] Invalid log channel ID specified: " + logChannelId);
                e.printStackTrace();
            } catch (Exception e) {
                Bukkit.getLogger().warning("[DiscordLogger] Error sending message to Discord.");
                e.printStackTrace();
            }
        });
    }



    public void reloadLogAsEmbed(boolean logAsEmbed) {
    }

    public void DLoggerInstance() {
        instance = this;
    }
}

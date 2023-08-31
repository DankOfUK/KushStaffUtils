package me.dankofuk.discord.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.json.simple.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class ChatWebhook implements Listener {

    private String chatWebhookUrl;
    private String chatUsername;
    private String chatAvatarUrl;
    private String chatMessageFormat;
    private boolean enabled;

    public ChatWebhook(String chatWebhookUrl, String chatUsername, String chatAvatarUrl, String chatMessageFormat, boolean enabled, FileConfiguration config) {
        this.chatWebhookUrl = chatWebhookUrl;
        this.chatUsername = chatUsername;
        this.chatAvatarUrl = chatAvatarUrl;
        this.chatMessageFormat = chatMessageFormat;
        this.enabled = enabled;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled) {
            return;
        }

        String playerName = event.getPlayer().getName();
        String message = event.getMessage();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            Player player = event.getPlayer();
            playerName = PlaceholderAPI.setPlaceholders(player, "%player%");
            message = PlaceholderAPI.setPlaceholders(player, message);
        }

        String webhookMessage = PlaceholderAPI.setPlaceholders(event.getPlayer(), chatMessageFormat);
        webhookMessage = webhookMessage
                .replace("%player%", playerName)
                .replace("%message%", message);

        sendWebhook(webhookMessage);
    }


    private void sendWebhook(String webhookMessage) {
        try {
            // Remove color codes from webhook message
            webhookMessage = ChatColor.stripColor(webhookMessage);

            // Create URL object with your Discord webhook URL
            URL url = new URL(chatWebhookUrl);

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "ChatLoggerWebhook");
            JSONObject payload = new JSONObject();
            payload.put("content", webhookMessage);
            payload.put("username", chatUsername);
            payload.put("avatar_url", chatAvatarUrl);

            String payloadString = payload.toString();

            // Write payload string to connection output stream
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(payloadString.getBytes());
                outputStream.flush();
            }

            // Get response code and message from connection
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();

            // Disconnect the connection
            connection.disconnect();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[ChatLogger] Error sending message to Discord webhook:", e);
        }
    }
}

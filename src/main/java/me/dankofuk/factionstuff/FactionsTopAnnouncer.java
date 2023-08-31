package me.dankofuk.factionstuff; // Update the package name

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class FactionsTopAnnouncer implements Listener {

        private String webhookUrl;
        private String footer;
        private String title;
        private String username;
        private String thumbnailUrl;
        private String avatarUrl;
        private List<String> messages;
        private long announcementInterval; // in seconds
        private boolean isEnabled;
        private boolean debuggerEnabled;

        public FactionsTopAnnouncer(String webhookUrl, List<String> messages, long announcementInterval, boolean isEnabled, String title, String username, String thumbnailUrl, String avatarUrl, String footer, boolean debuggerEnabled) {
                this.title = title;
                this.footer = footer;
                this.username = username;
                this.thumbnailUrl = thumbnailUrl;
                this.avatarUrl = avatarUrl;
                this.webhookUrl = webhookUrl;
                this.messages = messages;
                this.announcementInterval = announcementInterval * 20;
                this.isEnabled = isEnabled;
                this.debuggerEnabled = debuggerEnabled;

                if (isEnabled) {
                        scheduleAnnouncements();
                }
        }

        private void scheduleAnnouncements() {
                new BukkitRunnable() {
                        @Override
                        public void run() {
                                if (isEnabled) {
                                        sendAnnouncement();
                                }
                        }
                }.runTaskTimer(Bukkit.getPluginManager().getPlugin("KushStaffUtils"), announcementInterval, announcementInterval);
        }

        private void sendAnnouncement() {
                if (webhookUrl == null || webhookUrl.isEmpty()) {
                        return; // No webhook URL, nothing to send
                }

                try {
                        URL url = new URL(webhookUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setRequestProperty("User-Agent", "FactionsTopAnnouncer");

                        connection.setDoOutput(true);

                        String message = messages.stream()
                                .map(line -> PlaceholderAPI.setPlaceholders(null, line)) // Replace 'player' with the appropriate Player instance
                                .collect(Collectors.joining("\n"));

                        JsonObject json = new JsonObject();
                        json.addProperty("username", username);
                        json.addProperty("avatar_url", avatarUrl);

                        JsonObject embed = new JsonObject();
                        embed.addProperty("description", message);
                        embed.addProperty("color", getColorCode("#00FF00")); // Change color code as needed
                        embed.addProperty("title", title);

                        JsonObject footerObj = new JsonObject();
                        footerObj.addProperty("text", footer);

                        embed.add("footer", footerObj);

                        JsonObject thumbnail = new JsonObject();
                        thumbnail.addProperty("url", thumbnailUrl);
                        embed.add("thumbnail", thumbnail);

                        JsonArray embeds = new JsonArray();
                        embeds.add(embed);
                        json.add("embeds", embeds);

                        String payload = json.toString();

                        try (OutputStream os = connection.getOutputStream()) {
                                os.write(payload.getBytes());
                        }

                        int responseCode = connection.getResponseCode();
                        String responseMessage = connection.getResponseMessage();

                        if (debuggerEnabled) {
                                System.out.println("Response Code: " + responseCode);
                                System.out.println("Response Message: " + responseMessage);
                        }

                } catch (MalformedURLException e) {
                        Bukkit.getLogger().warning("[FactionsTopAnnouncerWebhook] Invalid webhook URL specified: " + this.webhookUrl);
                        e.printStackTrace();
                } catch (ProtocolException e) {
                        Bukkit.getLogger().warning("[FactionsTopAnnouncerWebhook] Invalid protocol specified in webhook URL: " + this.webhookUrl);
                        e.printStackTrace();
                } catch (IOException e) {
                        Bukkit.getLogger().warning("[FactionsTopAnnouncerWebhook] Error sending message to Discord webhook.");
                        e.printStackTrace();
                }
        }

        private int getColorCode(String color) {
                color = color.replace("#", "");
                return Integer.parseInt(color, 16);
        }

        public void reloadAnnouncer(boolean isEnabled) {
                this.isEnabled = isEnabled;
                if (isEnabled) {
                        scheduleAnnouncements();
                }
        }
}

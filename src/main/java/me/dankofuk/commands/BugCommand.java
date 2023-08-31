package me.dankofuk.commands;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.dankofuk.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class BugCommand implements Listener, CommandExecutor {
    public String bugWebhookUrl;
    public String bugUsername;
    public String bugAvatarUrl;
    public boolean isBugEnabled;
    public String bugMessage;
    public String bugResponse;
    public String noBugPermissionMessage;
    public String bugUsageMessage;
    public FileConfiguration config;
    public String bugThumbnail;
    public final Map<UUID, Long> cooldowns = new HashMap<>();
    private long bugCooldown;

    public BugCommand(String bugWebhookUrl, String bugUsername, String bugAvatarUrl, boolean isBugEnabled, String bugMessage, String noBugPermissionMessage, String bugUsageMessage, String bugThumbnail, long bugCooldown, String bugResponse, FileConfiguration config) {
        this.bugWebhookUrl = bugWebhookUrl;
        this.bugThumbnail = bugThumbnail;
        this.bugCooldown = bugCooldown;
        this.bugUsername = bugUsername;
        this.bugAvatarUrl = bugAvatarUrl;
        this.isBugEnabled = isBugEnabled;
        this.bugMessage = bugMessage;
        this.noBugPermissionMessage = noBugPermissionMessage;
        this.bugUsageMessage = bugUsageMessage;
        this.bugResponse = bugResponse;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("commandlogger.bug.use")) {
            player.sendMessage(ColorUtils.translateColorCodes(noBugPermissionMessage));
            return true;
        }

        if (!isBugEnabled) {
            player.sendMessage(ColorUtils.translateColorCodes("&cReporting bugs is currently disabled."));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ColorUtils.translateColorCodes(bugUsageMessage));
            return true;
        }

        String bugReport = String.join(" ", args);
        long currentTime = System.currentTimeMillis();
        long lastReportTime = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long timeRemaining = (lastReportTime + (bugCooldown * 1000L) - currentTime) / 1000L;

        if (timeRemaining > 0) {
            String cooldownMessage = "&cPlease wait " + timeRemaining + " seconds before submitting another report.";
            player.sendMessage(ColorUtils.translateColorCodes(cooldownMessage));
            return true;
        }
        sendWebhook(player, bugReport);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // Send a separate message to the player
        player.sendMessage(ColorUtils.translateColorCodes(bugResponse.replace("%reason%", bugReport)));

        return true;
    }


    private void sendWebhook(Player player, String bugReport) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(this.bugWebhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "BugWebhook");
                connection.setDoOutput(true);

                String description = ColorUtils.translateColorCodes(bugMessage.replace("%player%", player.getName()).replace("%bug%", bugReport));
                JsonObject json = new JsonObject();
                json.addProperty("username", this.bugUsername);
                json.addProperty("avatar_url", this.bugAvatarUrl);
                JsonObject embed = new JsonObject();
                embed.addProperty("description", description);
                embed.addProperty("color", getColorCode("#FF0000"));
                embed.addProperty("title", "New Bug Report");
                JsonObject thumbnail = new JsonObject();
                thumbnail.addProperty("url", this.bugThumbnail);
                embed.add("thumbnail", thumbnail);
                JsonArray embeds = new JsonArray();
                embeds.add(embed);
                json.add("embeds", embeds);

                String message = new Gson().toJson(json);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(message.getBytes());
                }

                connection.connect();

                int responseCode = connection.getResponseCode();
                String responseMessage = connection.getResponseMessage();
                // Debugger
                // if (responseCode != HttpURLConnection.HTTP_OK) {
                //    Bukkit.getLogger().warning("[BugWebhook] Bug report sent to the Discord - Response code: " + responseCode + " Response message: " + responseMessage);
                //}
            } catch (MalformedURLException e) {
                Bukkit.getLogger().log(Level.WARNING,"[BugWebhook] Invalid webhook URL specified: " + this.bugWebhookUrl, e);
            } catch (ProtocolException e) {
                Bukkit.getLogger().log(Level.WARNING,"[BugWebhook] Invalid protocol specified in webhook URL: " + this.bugWebhookUrl, e);
            } catch (IOException e) {
                Bukkit.getLogger().log(Level.WARNING,"[BugWebhook] Error sending message to Discord webhook.", e);
            }
        });
    }

    private int getColorCode(String color) {
        color = color.replace("#", "");
        return Integer.parseInt(color, 16);
    }

    public void reloadConfigOptions(String bugWebhookUrl, String bugUsername, String bugAvatarUrl, boolean isBugEnabled, String bugMessage, String noBugPermissionMessage, String bugUsageMessage, String bugThumbnail, long bugCooldown, String bugResponse, FileConfiguration config) {
        this.bugWebhookUrl = bugWebhookUrl;
        this.bugThumbnail = bugThumbnail;
        this.bugCooldown = bugCooldown;
        this.bugUsername = bugUsername;
        this.bugAvatarUrl = bugAvatarUrl;
        this.isBugEnabled = isBugEnabled;
        this.bugMessage = bugMessage;
        this.bugResponse = bugResponse;
        this.noBugPermissionMessage = noBugPermissionMessage;
        this.bugUsageMessage = bugUsageMessage;
        this.config = config;
    }
}

package me.dankofuk.commands;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import me.dankofuk.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class ReportCommand implements Listener, CommandExecutor {
    public String ReportWebhookUrl;
    public String username;
    public String avatarUrl;
    public boolean isReportEnabled;
    public String reportMessage;
    public final Map<UUID, Long> cooldowns = new HashMap<>();
    public int reportCooldown;
    public String reportSentMessage;
    public String noPermissionMessage;
    public String usageMessage;
    public FileConfiguration config;

    public ReportCommand(String ReportWebhookUrl, String username, String avatarUrl, boolean isReportEnabled, String reportMessage, int cooldownSeconds, String reportSentMessage, String noPermissionMessage, String usageMessage, FileConfiguration config) {
        this.ReportWebhookUrl = ReportWebhookUrl;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.isReportEnabled = isReportEnabled;
        this.reportMessage = reportMessage;
        this.reportCooldown = cooldownSeconds;
        this.reportSentMessage = reportSentMessage;
        this.noPermissionMessage = noPermissionMessage;
        this.usageMessage = usageMessage;
        this.config = config;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player player = (Player)sender;
        if (!player.hasPermission("commandlogger.report.use")) {
            player.sendMessage(ColorUtils.translateColorCodes(this.noPermissionMessage));
              return true;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtils.translateColorCodes(this.usageMessage));
            return true;
        }
        String reportedPlayerName = args[0];
        String reportReason = String.join(" ", Arrays.<CharSequence>copyOfRange((CharSequence[])args, 1, args.length));
        long currentTime = System.currentTimeMillis();
        long lastReportTime = ((Long)this.cooldowns.getOrDefault(player.getUniqueId(), 0L)).longValue();
        long timeRemaining = (lastReportTime + this.reportCooldown * 1000L - currentTime) / 1000L;
        if (timeRemaining > 0L) {
            String cooldownMessage = "&cPlease wait " + timeRemaining + " seconds before submitting another report.";
            player.sendMessage(ColorUtils.translateColorCodes(cooldownMessage));
            return true;
        }
        sendWebhook(player, reportedPlayerName, reportReason);
        this.cooldowns.put(player.getUniqueId(), currentTime);
        player.sendMessage(ColorUtils.translateColorCodes(this.reportSentMessage));
        return true;
    }

    private void sendWebhook(Player player, String reportedPlayerName, String reportReason) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(this.ReportWebhookUrl);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "ReportWebhook");
                connection.setDoOutput(true);
                String message = this.reportMessage.replace("%player%", player.getName()).replace("%reported_player%", reportedPlayerName).replace("%reason%", reportReason).replace("\n", "\\n");
                message = "{\"username\":\"" + this.username + "\",\"avatar_url\":\"" + this.avatarUrl + "\",\"embeds\":[{\"description\":\"" + message + "\",\"color\":" + getColorCode("#FF0000") + "}]}";
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(message.getBytes());
                }
                connection.getResponseCode();
                connection.getResponseMessage();
            } catch (MalformedURLException e) {
                Bukkit.getLogger().warning("[ReportWebhook] Invalid webhook URL specified: " + this.ReportWebhookUrl);
            } catch (ProtocolException e) {
                Bukkit.getLogger().warning("[ReportWebhook] Invalid protocol specified in webhook URL: " + this.ReportWebhookUrl);
            } catch (IOException e) {
                Bukkit.getLogger().warning("[ReportWebhook] Error sending message to Discord webhook.");
            }
        });
    }

    public void reloadReportWebhook(String ReportWebhookUrl) {
        this.ReportWebhookUrl = ReportWebhookUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setEnabled(boolean isReportEnabled) {
        this.isReportEnabled = isReportEnabled;
    }

    private int getColorCode(String color) {
        color = color.replace("#", "");
        return Integer.parseInt(color, 16);
    }

    public void reloadCooldown(int cooldownSeconds) {
        this.reportCooldown = cooldownSeconds;
    }

    public void reloadSentMessage(String reportSentMessage) {
        this.reportSentMessage = reportSentMessage;
    }

    public void reloadNoPermMessage(String noPermissionMessage) {
        this.noPermissionMessage = noPermissionMessage;
    }

    public void reloadUsageMessage(String usageMessage) {
        this.usageMessage = usageMessage;
    }

    public void reloadReportMessage(String reportMessage) {
        this.reportMessage = reportMessage;
    }
}

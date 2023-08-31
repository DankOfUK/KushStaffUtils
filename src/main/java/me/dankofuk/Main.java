package me.dankofuk;

import me.clip.placeholderapi.metrics.bukkit.Metrics;
import me.dankofuk.commands.BugCommand;
import me.dankofuk.commands.CommandLogViewer;
import me.dankofuk.commands.ReportCommand;
import me.dankofuk.discord.DiscordBot;
import me.dankofuk.discord.commands.botRequiredCommands.SuggestionCommand;
import me.dankofuk.discord.listeners.ChatWebhook;
import me.dankofuk.discord.listeners.CommandLogger;
import me.dankofuk.discord.listeners.StartStopLogger;
import me.dankofuk.factionstuff.FactionStrike;
import me.dankofuk.factionstuff.FactionsTopAnnouncer;
import me.dankofuk.listeners.FileCommandLogger;
import me.dankofuk.listeners.JoinLeaveLogger;
import me.dankofuk.utils.ColorUtils;
import net.dv8tion.jda.api.JDA;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main extends JavaPlugin implements Listener {
    private CommandLogger commandLogger;
    private List<String> ignoredCommands;
    public String joinWebhookUrl;
    private ReportCommand reportCommand;
    public String leaveWebhookUrl;
    public ArrayList<String> joinMessage;
    public ArrayList<String> leaveMessage;
    public boolean useEmbed;
    public boolean isEnabled;
    public boolean isReportEnabled;
    public JoinLeaveLogger JoinLeaveLogger;
    public FileCommandLogger FileCommandLogger;
    public ReportCommand ReportCommand;
    public String reportMessage;
    public String ReportWebhookUrl;
    public String username;
    public String avatarUrl;
    public int cooldownSeconds;
    public String UsageMessage;
    public String reportSentMessage;
    public String usageMessage;
    private static Main instance;
    private BugCommand BugCommand;
    private SuggestionCommand suggestionCommand;
    private DiscordBot discordBot;
    private StartStopLogger serverStatus;
    private FactionStrike factionStrike;
    private FactionsTopAnnouncer factionsTopAnnouncer;
    public String logsFolder;
    private JDA jda;
    private Plugin plugin;

    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        // BStats
        int pluginId = 18185; // <-- Replace with the id of your plugin!
        Metrics metrics = new Metrics(this, pluginId);

        // Check if PlaceholderAPI is installed
        PluginManager pluginManager = Bukkit.getPluginManager();
        Plugin placeholderAPI = pluginManager.getPlugin("PlaceholderAPI");
        if (placeholderAPI == null || !placeholderAPI.isEnabled()) {
            getLogger().warning("PlaceholderAPI is not installed or enabled. Some placeholders may not work.");
        }

        // Check if Vault is installed
        Plugin vault = pluginManager.getPlugin("Vault");
        if (vault == null || !vault.isEnabled()) {
            getLogger().warning("Vault is not installed or enabled. Some functionality may be limited.");
        }

        // No Permission String
        String noPermissionMessage = config.getString("no-permission-message");
        // Files
        String logsFolder = (new File(getDataFolder(), "logs")).getPath();
        this.FileCommandLogger = new FileCommandLogger(logsFolder);
        boolean logCommands = getConfig().getBoolean("per-user-logging.enabled", true);
        this.FileCommandLogger.reloadLogCommands(logCommands);

        // Main Discord Bot
        String discordToken = getConfig().getString("bot.discord_token");
        boolean discordBotEnabled = getConfig().getBoolean("bot.enabled");
        Server minecraftServer = getServer();
        String adminRoleID = getConfig().getString("bot.adminRoleID");
        String discordActivity = getConfig().getString("bot.discord_activity");;
        String ServerStatusChannelID = getConfig().getString("serverstatus.channel_id");
        String noPlayersTitle = config.getString("bot.listplayers_no_players_online_title");
        String titleFormat = config.getString("bot.listplayers_title_format");
        String footerFormat = config.getString("bot.listplayers_footer_format");
        String listThumbnailUrl = config.getString("bot.listplayers_thumbnail_url");
        boolean requireAdminRole = config.getBoolean("bot.listplayers_requireAdminRole");
        boolean logsCommandRequiresAdminRole = config.getBoolean("bot.logsCommand_requireAdminRole");
        // Command Logger
        boolean logAsEmbed = config.getBoolean("commandlogger.logAsEmbed");
        String logChannelId = config.getString("commandlogger.channel_id");
        List<String> ignoredCommands = config.getStringList("ignored_commands");
        List<String> whitelistCommands = config.getStringList("whitelisted_commands");
        boolean whitelistMode = config.getBoolean("whitelist_mode_enabled");
        String serverName = config.getString("commandlogger.server_name");
        List<String> messageFormats = config.getStringList("commandlogger.message_formats");
        List<String> embedTitleFormats = config.getStringList("commandlogger.title_formats");
        commandLogger = new CommandLogger(discordBot, messageFormats, embedTitleFormats, serverName, logAsEmbed, logChannelId, ignoredCommands, whitelistMode, whitelistCommands);
        Bukkit.getServer().getPluginManager().registerEvents(commandLogger, this);
        if (config.getBoolean("bot.enabled")) {
            if ("false".equals(discordToken) || discordToken.isEmpty()) {
                Bukkit.getLogger().warning("[KushStaffUtils - Discord Bot] No bot token found. Bot initialization skipped.");
                return;
            }

            discordBot = new DiscordBot(discordToken, discordBotEnabled, minecraftServer, adminRoleID, discordActivity, this, config, ServerStatusChannelID, titleFormat, footerFormat, listThumbnailUrl, noPlayersTitle, requireAdminRole, logsCommandRequiresAdminRole, ignoredCommands, whitelistCommands, whitelistMode, serverName, messageFormats, embedTitleFormats, logChannelId, logAsEmbed, plugin);
            try {
                discordBot.start();
                Bukkit.getLogger().warning("[KushStaffUtils - Discord Bot] Starting Discord Bot...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            Bukkit.getLogger().warning("[KushStaffUtils - Discord Bot] Bot is disabled. Skipping initialization...");
        }
        // Command Log Viewer Command
        CommandLogViewer commandLogViewer = new CommandLogViewer(getDataFolder().getPath() + File.separator + "logs", 15);
        getCommand("viewlogs").setExecutor(commandLogViewer);
        if (logCommands)
            Bukkit.getServer().getPluginManager().registerEvents(this.FileCommandLogger, this);
        // Chat Webhook
        ChatWebhook chatWebhook = new ChatWebhook(config.getString("chatwebhook.url"), config.getString("chatwebhook.username"), config.getString("chatwebhook.avatarUrl"), config.getString("chatwebhook.message"), config.getBoolean("chatwebhook.enabled"), config);
        getServer().getPluginManager().registerEvents(chatWebhook, this);
        // Factions Top Announcer Listener
        this.factionsTopAnnouncer = new FactionsTopAnnouncer(config.getString("announcer.webhookUrl"), config.getStringList("announcer.messages"), config.getLong("announcer.sendEvery"), config.getBoolean("announcer.enabled"), config.getString("announcer.title"), config.getString("announcer.username"), config.getString("announcer.thumbnailUrl"), config.getString("announcer.avatarUrl"), config.getString("announcer.footer"), config.getBoolean("announcer.debuggerEnabled"));
        // Strike Command
        this.factionStrike = new FactionStrike(this, config.getString("strike.webhookUrl"), config.getString("strike.username"), config.getString("strike.avatarUrl"), config.getBoolean("strike.enabled"), config.getString("strike.message"), config.getString("strike.noPermissionMessage"), config.getString("strike.usageMessage"), config.getStringList("strike.sendCommand"), config.getString("strike.embedTitle"), config.getString("strike.thumbnail"), config);
        getCommand("strike").setExecutor(this.factionStrike);
        // Bug Command
        this.BugCommand = new BugCommand(config.getString("bug_webhook_url"), config.getString("bug_username"), config.getString("bug_avatar_url"), config.getBoolean("is_bug_enabled"), config.getString("bug_message"), config.getString("no_bug_permission_message"), config.getString("bug_usage_message"), config.getString("bug_thumbnail"), config.getLong("bug_cooldown"), config.getString("bug_sent_message"), config);
        getServer().getPluginManager().registerEvents(this.BugCommand, this);
        getCommand("bug").setExecutor(this.BugCommand);
        // Initialize the SuggestionCommand
        String suggestionWebhookUrl = config.getString("suggestion.webhook_url");
        String suggestionThumbnail = config.getString("suggestion.thumbnail");
        String channelId = config.getString("suggestion.channel_id");
        String threadId = config.getString("suggestion.thread_id");
        String suggestionMessage = config.getString("suggestion.message");
        String suggestionUsageMessage = config.getString("suggestion.usage_message");
        String responseMessage = config.getString("suggestion.sent_message");
        long cooldown = config.getLong("suggestion.cooldown");
        String title = config.getString("suggestion.title");
        String description = config.getString("suggestion.description");
        Color color = Color.decode(config.getString("suggestion.color"));
        String footer = config.getString("suggestion.footer");
        String thumbnailUrl = config.getString("suggestion.thumbnail_url");

        suggestionCommand = new SuggestionCommand(discordBot, channelId, threadId, suggestionMessage,
                noPermissionMessage, suggestionUsageMessage, responseMessage, cooldown, title, description, footer, color, listThumbnailUrl, config);
        getCommand("suggest").setExecutor(suggestionCommand);
        getCommand("suggestion").setExecutor(suggestionCommand);

        getServer().getPluginManager().registerEvents(this.suggestionCommand, this);
        // Report Command
        ReportCommand reportCommand = new ReportCommand(config.getString("report.webhookUrl"),
                config.getString("report.username"), config.getString("report.avatarUrl"),
                config.getBoolean("report.enabled"), config.getString("report.message"),
                config.getInt("report.cooldown"), config.getString("report.sent-message"),
                config.getString("report.no-permission-message"), config.getString("report.command-usage"), config);
        Bukkit.getPluginManager().registerEvents(reportCommand, this);
        getCommand("report").setExecutor(reportCommand);
        // JoinLeave - Command Logger Webhook
        this.joinWebhookUrl = getConfig().getString("joinWebhookUrl");
        this.leaveWebhookUrl = getConfig().getString("leaveWebhookUrl");
        this.joinMessage = (ArrayList<String>) getConfig().getStringList("joinMessage");
        this.leaveMessage = (ArrayList<String>) getConfig().getStringList("leaveMessage");
        this.useEmbed = getConfig().getBoolean("useEmbed", false);
        this.JoinLeaveLogger = new JoinLeaveLogger(this.joinWebhookUrl, this.leaveWebhookUrl, this.joinMessage, this.leaveMessage, this.useEmbed, this.isEnabled);
        new ThreadPoolExecutor(5, 10, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getServer().getPluginManager().registerEvents(this.JoinLeaveLogger, this);
        Bukkit.getConsoleSender().sendMessage("[KushStaffUtils] Plugin has been enabled");
    }

    public void onDisable() {
        FileConfiguration config = getConfig();
        boolean discordBotEnabled = config.getBoolean("bot.enabled");

        // Stop Discord bot if it is enabled
        if (discordBotEnabled) {
            discordBot.stop();
            Bukkit.getLogger().warning("[Discord Bot] Bot has been disabled!");
        } else {
            Bukkit.getLogger().warning("Discord Bot is disabled, won't stop.");
        }
        Bukkit.getConsoleSender().sendMessage("[KushStaffUtils] Plugin has been disabled!");
    }


    // reload command
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("stafflogger")) {
            if (!sender.hasPermission("commandlogger.reload")) {
                sender.sendMessage(ColorUtils.translateColorCodes(Objects.requireNonNull(getConfig().getString("no_permission"))));
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfigOptions();
                sender.sendMessage(ColorUtils.translateColorCodes(getConfig().getString("reload_message")));
                return true;
            }
            return false;
        }
        return false;
    }

    // Command Logger Methods
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!event.getPlayer().hasPermission("commandlogger.log") || event.getPlayer().hasPermission("commandlogger.bypass"))
            return;
        String[] args = event.getMessage().split(" ");
        String command = args[0];
        if (isIgnoredCommand(command))
            return;
        if (getConfig().getBoolean("whitelist_mode_enabled")) {
            List<String> whitelistedCommands = getConfig().getStringList("whitelisted_commands");
            if (!isWhitelistedCommand(command, whitelistedCommands))
                return;
        }
        String playerName = event.getPlayer().getName();
        this.commandLogger.logCommand(event.getMessage(), playerName);
    }

    private boolean isIgnoredCommand(String command) {
        for (String ignored : getConfig().getStringList("ignored_commands")) {
            if (ignored.equalsIgnoreCase(command))
                return true;
        }
        return false;
    }

    private boolean isWhitelistedCommand(String command, List<String> whitelistedCommands) {
        for (String whitelisted : whitelistedCommands) {
            if (whitelisted.trim().equalsIgnoreCase(command))
                return true;
        }
        return false;
    }


    public void reloadConfigOptions() {
        reloadConfig();
        FileConfiguration config = getConfig();
        String noPermissionMessage = config.getString("no-permission-message");
        ChatWebhook chatWebhook = new ChatWebhook(config.getString("chatwebhook.url"), config.getString("chatwebhook.username"), config.getString("chatwebhook.avatarUrl"), config.getString("chatwebhook.message"), config.getBoolean("chatwebhook.enabled"), config);
        String strikeWebhookUrl = config.getString("strike.webhookUrl");
        String strikeUsername = config.getString("strike.username");
        String strikeAvatarUrl = config.getString("strike.avatarUrl");
        boolean isStrikeEnabled = config.getBoolean("strike.enabled");
        String strikeMessage = config.getString("strike.message");
        String strikeNoPermissionMessage = config.getString("strike.noPermissionMessage");
        String strikeUsageMessage = config.getString("strike.usageMessage");
        List<String> strikeCommand = config.getStringList("strike.sendCommand");
        String strikeEmbedTitle = config.getString("strike.embedTitle");
        String strikeThumbnail = config.getString("strike.thumbnail");
        factionStrike.reloadConfigOptions(
                strikeWebhookUrl, strikeUsername, strikeAvatarUrl, isStrikeEnabled,
                strikeMessage, strikeNoPermissionMessage, strikeUsageMessage,
                strikeCommand, strikeEmbedTitle, strikeThumbnail, config
        );

        boolean logCommands = getConfig().getBoolean("log_commands");
        this.FileCommandLogger.reloadLogCommands(logCommands);
        String serverName = getConfig().getString("server_name");
        this.ignoredCommands = getConfig().getStringList("ignored_commands");
        List<String> messageFormats = getConfig().getStringList("message_formats");
        List<String> embedTitleFormats = getConfig().getStringList("embed_title_formats");
        boolean logAsEmbed = getConfig().getBoolean("logAsEmbed");
        String logChannelId = getConfig().getString("bot.command_log_channel_id");
        this.commandLogger.reloadLogAsEmbed(logAsEmbed);
        this.commandLogger.reloadMessageFormats(messageFormats);
        this.commandLogger.reloadEmbedTitleFormats(embedTitleFormats);
        this.commandLogger.setServerName(serverName);
        this.commandLogger.reloadLogChannelID(logChannelId);
        this.useEmbed = getConfig().getBoolean("useEmbed", false);
        this.isEnabled = getConfig().getBoolean("isEnabled", false);
        // Join Leave Logger
        this.JoinLeaveLogger.reloadJoinLeaveLogger(joinWebhookUrl, leaveWebhookUrl, joinMessage, leaveMessage, useEmbed, isEnabled);
        String suggestionWebhookUrl = config.getString("suggestion.webhook_url");
        String suggestionThumbnail = config.getString("suggestion.thumbnail");
        String channelId = config.getString("suggestion.channel_id");
        String threadId = config.getString("suggestion.thread_id");
        String suggestionMessage = config.getString("suggestion.message");
        String suggestionUsageMessage = config.getString("suggestion.usage_message");
        String responseMessage = config.getString("suggestion.sent_message");
        long cooldown = config.getLong("suggestion.cooldown");
        String title = config.getString("suggestion.title");
        String description = config.getString("suggestion.description");
        Color color = Color.decode(config.getString("suggestion.color"));
        String footer = config.getString("suggestion.footer");
        String thumbnailUrl = config.getString("suggestion.thumbnail_url");
        this.suggestionCommand.reloadConfigOptions(discordBot, channelId, threadId, suggestionMessage,
                noPermissionMessage, suggestionUsageMessage, responseMessage, cooldown, title, description, footer, color, thumbnailUrl);
        String bugWebhookUrl = config.getString("bug_webhook_url");
        String bugThumbnail = config.getString("bug_thumbnail");
        long bugCooldown = config.getLong("bug_cooldown");
        String bugUsername = config.getString("bug_username");
        String bugAvatarUrl = config.getString("bug_avatar_url");
        boolean isBugEnabled = config.getBoolean("is_bug_enabled");
        String bugMessage = config.getString("bug_message");
        String bugResponse = config.getString("bug_sent_message");
        String noBugPermissionMessage = config.getString("no_bug_permission_message");
        String bugUsageMessage = config.getString("bug_usage_message");
        this.BugCommand.reloadConfigOptions(bugWebhookUrl, bugUsername, bugAvatarUrl, isBugEnabled, bugMessage, noBugPermissionMessage, bugUsageMessage, bugThumbnail, bugCooldown, bugResponse, config);
        // load new configuration
        String discordToken = getConfig().getString("bot.discord_token");
        boolean discordBotEnabled = getConfig().getBoolean("bot.enabled");
        String adminRoleID = getConfig().getString("bot.adminRoleID");
        String discordActivity = getConfig().getString("bot.discord_activity");
        String ServerStatusChannelID = getConfig().getString("serverstatus.channel_id");
        reportCommand = new ReportCommand(config.getString("report.webhookUrl"),
                config.getString("report.username"), config.getString("report.avatarUrl"),
                config.getBoolean("report.enabled"), config.getString("report.message"),
                config.getInt("report.cooldown"), config.getString("report.sent-message"),
                config.getString("report.no-permission-message"), config.getString("report.command-usage"), config);
        // start new Discord bot session if enabled
        Bukkit.getConsoleSender().sendMessage("[KushStaffUtils] Config options have been reloaded!");

    }

    public String getCommandLoggerFolder() {
        return logsFolder;
    }
}

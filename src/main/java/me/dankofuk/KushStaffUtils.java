package me.dankofuk;

import com.golfing8.kore.FactionsKore;
import com.golfing8.kore.feature.PrinterFeature;
import me.dankofuk.commands.CommandLogViewer;
import me.dankofuk.commands.StaffUtilsCommand;
import me.dankofuk.discord.DiscordBot;
import me.dankofuk.discord.commands.botRequiredCommands.BugCommand;
import me.dankofuk.discord.commands.botRequiredCommands.ReportCommand;
import me.dankofuk.discord.commands.botRequiredCommands.SuggestionCommand;
import me.dankofuk.discord.listeners.ChatWebhook;
import me.dankofuk.discord.listeners.CommandLogger;
import me.dankofuk.discord.listeners.StartStopLogger;
import me.dankofuk.commands.SyncGameCommand;
import me.dankofuk.discord.syncing.SyncStorage;
import me.dankofuk.factions.FactionStrike;
import me.dankofuk.factions.FactionsTopAnnouncer;
import me.dankofuk.fkore.FKoreEnterPrinterLogger;
import me.dankofuk.fkore.FKoreLeavePrinterLogger;
import me.dankofuk.loggers.advancedbans.*;
import me.dankofuk.loggers.creative.CreativeDropLogger;
import me.dankofuk.loggers.creative.CreativeMiddleClickLogger;
import me.dankofuk.loggers.litebans.listeners.LBBanListener;
import me.dankofuk.loggers.litebans.listeners.LBKickListener;
import me.dankofuk.loggers.litebans.listeners.LBMuteListener;
import me.dankofuk.loggers.litebans.listeners.LBWarnListener;
import me.dankofuk.loggers.players.FileCommandLogger;
import me.dankofuk.loggers.players.JoinLeaveLogger;
import me.dankofuk.utils.Metrics;
import net.dv8tion.jda.api.JDA;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class KushStaffUtils extends JavaPlugin implements Listener {
    private static String logsFolder;
    private static KushStaffUtils instance;
    private static FactionsKore factionsKore;
    private JDA jda;
    private Plugin plugin;

    public FileConfiguration config;
    public FileConfiguration messagesConfig;
    public FileConfiguration syncingConfig;
    public FileConfiguration discordBotConfig;

    public StartStopLogger startStopLogger;
    public CommandLogger commandLogger;
    public JoinLeaveLogger joinLeaveLogger;
    public FileCommandLogger fileCommandLogger;
    public ReportCommand reportCommand;
    public String reportSentMessage;
    public BugCommand bugCommand;
    public SuggestionCommand suggestionCommand;
    public DiscordBot discordBot;
    public FactionStrike factionStrike;
    public FactionsTopAnnouncer factionsTopAnnouncer;
    public ChatWebhook chatWebhook;
    public CreativeMiddleClickLogger creativeLogger;
    public CreativeDropLogger creativeDropLogger;
    public CommandLogViewer commandLogViewer;
    public StaffUtilsCommand staffUtilsCommand;
    // LiteBans
    public LBBanListener bansListener;
    public LBMuteListener lbMuteListener;
    public LBWarnListener lbWarnListener;
    public LBKickListener lbKickListener;
    // AdvancedBans
    public ABanListener aBanListener;
    public ATempBanListener aTempBanListener;
    public AIPBanListener aIPBanListener;
    public AWarnListener aWarnListener;
    public AKickListener aKickListener;
    public AMuteListener aMuteListener;
    // Syncing
    public SyncStorage syncStorage;
    // FKore
    public FKoreLeavePrinterLogger printerLeaveLogger;
    public FKoreEnterPrinterLogger printerEnterLogger;
    public FactionsKore fkore;
    public PrinterFeature printerFeature;
    public void onEnable() {
        // Loading configuration
        FileConfiguration config = getConfig();
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        messagesConfig = loadMessagesConfig();
        syncingConfig = loadSyncingConfig();
        discordBotConfig = loadBotConfig();
        setDefaultMessages();
        setBotMessages();
        setSyncingConfig();

        // Instance
        instance = this;

        // PAPI/Vault Checker
        PluginManager pluginManager = Bukkit.getPluginManager();
        Plugin placeholderAPI = pluginManager.getPlugin("PlaceholderAPI");
        if (placeholderAPI == null)
            getLogger().warning("PlaceholderAPI is not installed or enabled. Some placeholders may not work.");
        Plugin vault = pluginManager.getPlugin("Vault");
        if (vault == null)
            getLogger().warning("Vault is not installed or enabled. Some functionality may be limited.");

        // bStats
        int pluginId = 18185;
        me.dankofuk.utils.Metrics metrics = new Metrics(this, pluginId);

        // Features
        if (config.getBoolean("bot.enabled")) {
            if ("false".equals(KushStaffUtils.getInstance().getConfig().getString("bot.discord_token")) || Objects.requireNonNull(KushStaffUtils.getInstance().getConfig().getString("bot.discord_token")).isEmpty()) {
                getLogger().warning("[Discord Bot] No bot token found. Bot initialization skipped.");
                return;
            }
            this.discordBot = new DiscordBot(this, config, syncStorage);
            try {
                this.discordBot.start();
                getLogger().warning("[Discord Bot] Starting Discord Bot...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            getLogger().warning("[Discord Bot] Bot is disabled. Skipping initialization...");
        }
        this.commandLogViewer = new CommandLogViewer(getDataFolder().getPath() + File.separator + "logs", 15);
        Objects.requireNonNull(getCommand("viewlogs")).setExecutor(commandLogViewer);


        // FileCommandLogger (Logging Folder)
        if (!config.getBoolean("per-user-logging.enabled")) {
            getLogger().warning("Per User Logging - [Not Enabled]");
        } else {
            String logsFolder = (new File(getDataFolder(), "logs")).getPath();
            this.fileCommandLogger = new FileCommandLogger(logsFolder);
            getServer().getPluginManager().registerEvents(fileCommandLogger, this);
            getLogger().warning("Per User Logging - [Enabled]");
        }
        // Chat Webhook (Webhook)
        if (!config.getBoolean("chatwebhook.enabled")) {
            getLogger().warning("Chat Logger - [Not Enabled]");
        } else {
            this.chatWebhook = new ChatWebhook(config);
            getServer().getPluginManager().registerEvents(chatWebhook, this);
            getLogger().warning("Chat Logger - [Enabled]");
        }
        // Start/Stop Logger (Discord Bot Feature)
        if (!config.getBoolean("bot.enabled")) {
            getLogger().warning("Start/Stop Logger - [Not Enabled] - (Requires Discord Bot enabled)");
            } else if (!config.getBoolean("serverstatus.enabled")) {
                getLogger().warning("Start/Stop Logger - [Not Enabled] - (Requires Discord Bot enabled)");
            } else {
            this.startStopLogger = new StartStopLogger(discordBot);
            startStopLogger.sendStatusUpdateMessage(true);
            getLogger().warning("Start/Stop Logger - [Enabled]");
        }
        // Factions/Skyblock Top Announcer (Webhook)
        if (!config.getBoolean("announcer.enabled")) {
            getLogger().warning("Factions Top Announcer - [Not Enabled]");
        } else {
            this.factionsTopAnnouncer = new FactionsTopAnnouncer(config, this);
            Bukkit.getPluginManager().registerEvents(factionsTopAnnouncer, this);
            getLogger().warning("Factions Top Announcer - [Enabled]");
        }
        // Player Report Command (Webhook + Command)
        if (!config.getBoolean("bot.enabled")) {
            getLogger().warning("Player Reporting Command - [Not Enabled] - (Requires Discord Bot enabled)");
        } else if (!config.getBoolean("report.enabled")) {
            getLogger().warning("Player Reporting Command - [Not Enabled] - (Requires Discord Bot enabled)");
        } else {
            this.reportCommand = new ReportCommand(this, discordBot);
            Objects.requireNonNull(getCommand("report")).setExecutor(this.reportCommand);
            Bukkit.getPluginManager().registerEvents(reportCommand, this);
            getLogger().warning("Player Reporting Command - [Enabled]");
        }
        // Strike Command (Webhook + Command)
        if (!config.getBoolean("strike.enabled")) {
            getLogger().warning("Strike Command - [Not Enabled]");
        } else {
            this.factionStrike = new FactionStrike(config, this);
            Objects.requireNonNull(getCommand("strike")).setExecutor(this.factionStrike);
            getLogger().warning("Strike Command - [Enabled]");
        }
        // Bug Report Command (Webhook + Command)
        if (!config.getBoolean("bot.enabled")) {
            getLogger().warning("Bug Report Command - [Not Enabled] - (Requires Discord Bot enabled)");
        } else if (!config.getBoolean("bug_report.enabled")) {
            getLogger().warning("Bug Report Command - [Not Enabled]");
        } else {
            this.bugCommand = new BugCommand(this, discordBot, config);
            getServer().getPluginManager().registerEvents(this.bugCommand, this);
            Objects.requireNonNull(getCommand("bug")).setExecutor(this.bugCommand);
            getLogger().warning("Bug Command - [Enabled]");
            }
        // Join Leave Logger (Webhooks)
        if (!config.getBoolean("player_leave_join_logger.enabled")) {
            getLogger().warning("Player Join Leave Logger - [Not Enabled]");
        } else {
            this.joinLeaveLogger = new JoinLeaveLogger(config);
            Bukkit.getServer().getPluginManager().registerEvents(this.joinLeaveLogger, this);
            getLogger().warning("Player Join Leave Logger - [Enabled]");
        }
        // Suggestion Command (Discord Bot + Command)
        if (!config.getBoolean("bot.enabled")) {
            getLogger().warning("Suggestion Command - [Not Enabled] - (Requires Discord Bot enabled)");
        } else if (!config.getBoolean("suggestion.enabled")) {
            getLogger().warning("Suggestion Command - [Not Enabled]");
        } else {
            this.suggestionCommand = new SuggestionCommand(this.discordBot, config);
            Objects.requireNonNull(getCommand("suggestion")).setExecutor(this.suggestionCommand);
            getServer().getPluginManager().registerEvents(this.suggestionCommand, this);
            getLogger().warning("Suggestion Command - [Enabled]");
        }
        // Command Logger (Discord Feature)
        if (!config.getBoolean("bot.enabled")) {
            getLogger().warning("Command Logger - [Not Enabled] - (Requires Discord Bot enabled)");
        } else {
            this.commandLogger = new CommandLogger(this.discordBot);
            getServer().getPluginManager().registerEvents(this.commandLogger, this);
            getLogger().warning("Command Logger - [Enabled]");
        }

        // Creative Logging (Webhooks)
        if (!config.getBoolean("creative-logging.enabled")) {
            getLogger().warning("Creative Logging - [Not Enabled]");
        } else {
            this.creativeLogger = new CreativeMiddleClickLogger(this);
            this.creativeDropLogger = new CreativeDropLogger(this);
            getServer().getPluginManager().registerEvents(this.creativeLogger, this);
            getServer().getPluginManager().registerEvents(this.creativeDropLogger, this);
            getLogger().warning("Creative Logging - [Enabled]");
        }

        // LiteBans Logging (Webhooks)
        if (!config.getBoolean("litebans.enabled")) {
            getLogger().warning("LiteBans Logging - [Not Enabled]");
        } else {
            this.lbKickListener = new LBKickListener(this);
            lbKickListener.registerEvents();
            this.lbWarnListener = new LBWarnListener(this);
            lbWarnListener.registerEvents();
            this.lbMuteListener = new LBMuteListener(this);
            lbMuteListener.registerEvents();
            this.bansListener = new LBBanListener(this);
            bansListener.registerEvents();
            getLogger().warning("LiteBans Logging - [Enabled]");
        }

        // AdvacnedBans Logging (Webhooks)
        if (!config.getBoolean("advancedbans.enabled")) {
            getLogger().warning("AdvancedBans Logging - [Not Enabled]");
        } else {
            this.aMuteListener = new AMuteListener(this);
            getServer().getPluginManager().registerEvents(aMuteListener, this);
            this.aIPBanListener = new AIPBanListener(this);
            getServer().getPluginManager().registerEvents(aIPBanListener, this);
            this.aKickListener = new AKickListener(this);
            getServer().getPluginManager().registerEvents(aKickListener, this);
            this.aWarnListener = new AWarnListener(this);
            getServer().getPluginManager().registerEvents(aWarnListener, this);
            this.aTempBanListener = new ATempBanListener(this);
            getServer().getPluginManager().registerEvents(aTempBanListener, this);
            this.aBanListener = new ABanListener(this);
            getServer().getPluginManager().registerEvents(aBanListener, this);
            getLogger().warning("AdvancedBans Logging - [Enabled]");
        }

        if (!syncingConfig.getBoolean("enabled")) {
            getLogger().warning("Discord 2 Game Syncing - [Not Enabled]");
        } else {
            syncStorage = new SyncStorage(syncingConfig.getString("MYSQL.URL"), syncingConfig.getString("MYSQL.USERNAME"), KushStaffUtils.getInstance().syncingConfig.getString("MYSQL.PASSWORD"));
            syncStorage.initDatabase();
            Objects.requireNonNull(getCommand("sync")).setExecutor(new SyncGameCommand(discordBot, KushStaffUtils.getInstance().syncingConfig.getString("MYSQL.URL"), KushStaffUtils.getInstance().syncingConfig.getString("MYSQL.USERNAME"), KushStaffUtils.getInstance().syncingConfig.getString("MYSQL.PASSWORD")));
            getLogger().warning("Discord 2 Game Syncing - [Enabled]");
        }
        if (!config.getBoolean("PRINTER-LOGGER.enabled")) {
            getLogger().info("FKore Leave Printer Logger - [Not Enabled]");
        } else {
            PrinterFeature printerKore = FactionsKore.get().getFeature(PrinterFeature.class);
            printerLeaveLogger = new FKoreLeavePrinterLogger(printerKore, this);
            getServer().getPluginManager().registerEvents(printerLeaveLogger, this);
            getLogger().info("FKore Leave Printer Logger - [Enabled]");
        }
        if (!config.getBoolean("PRINTER-LOGGER.enabled")) {
            getLogger().info("FKore Enter Printer Logger - [Not Enabled]");
        } else {
            PrinterFeature printerKore = FactionsKore.get().getFeature(PrinterFeature.class);
            printerEnterLogger = new FKoreEnterPrinterLogger(printerKore, this);
            getServer().getPluginManager().registerEvents(printerEnterLogger, this);
            getLogger().info("FKore Enter Printer Logger - [Enabled]");
        }
        this.staffUtilsCommand = new StaffUtilsCommand();
        Objects.requireNonNull(getCommand("stafflogger")).setExecutor(this.staffUtilsCommand);
        new ThreadPoolExecutor(5, 10, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
        Bukkit.getConsoleSender().sendMessage("[KushStaffUtils] Plugin has been enabled");
    }

    public void onDisable() {
        FileConfiguration config = getConfig();
        boolean discordBotEnabled = config.getBoolean("bot.enabled");
        syncStorage.closeConnection();
        if (discordBotEnabled) {
            this.discordBot.stop();
            getLogger().warning("[Discord Bot] Bot has been disabled!");
        } else {
            getLogger().warning("[Discord Bot] Bot is disabled, won't stop.");
        }
        boolean factionsTopAnnouncer = config.getBoolean("announcer.enabled");
        if (factionsTopAnnouncer) {
            this.factionsTopAnnouncer.cancelAnnouncements();
        }
        if (discordBotEnabled) {
            if (config.getBoolean("serverstatus.enabled")) {
                startStopLogger.sendStatusUpdateMessage(false);
            }
        } else
            getLogger().info("[Failed to start the start/stop logger]");
        Bukkit.getConsoleSender().sendMessage("[KushStaffUtils] Plugin has been disabled!");
    }


    public void reloadConfigOptions() {
        reloadConfig();
        HandlerList.unregisterAll((Listener) this);
        FileConfiguration config = getConfig();
        loadMessagesConfig();
        // Discord Bot Stuff
        if (KushStaffUtils.getInstance().getConfig().getBoolean("bot.enabled")) {
            discordBot.reloadBot();
        }
        // Instance Reloads
        instance = this;

        // FileCommandLogger (Logging Folder)
        if (!config.getBoolean("per-user-logging.enabled")) {
            getLogger().warning("Per User Logging - [Not Enabled]");
        } else {
            String logsFolder = (new File(getDataFolder(), "logs")).getPath();
            this.fileCommandLogger = new FileCommandLogger(logsFolder);
            getServer().getPluginManager().registerEvents(fileCommandLogger, this);
            getLogger().warning("Per User Logging - [Enabled]");
        }
        // Chat Webhook (Webhook)
        if (!config.getBoolean("chatwebhook.enabled")) {
            getLogger().warning("Chat Logger - [Not Enabled]");
        } else {
            this.chatWebhook = new ChatWebhook(config);
            getServer().getPluginManager().registerEvents(chatWebhook, this);
            getLogger().warning("Chat Logger - [Enabled]");
        }
        // Start/Stop Logger (Discord Bot Feature)
        if (!config.getBoolean("bot.enabled")) {
            getLogger().warning("Start/Stop Logger - [Not Enabled] - (Requires Discord Bot enabled)");
        } else if (!config.getBoolean("serverstatus.enabled")) {
            getLogger().warning("Start/Stop Logger - [Not Enabled] - (Requires Discord Bot enabled)");
        } else {
            this.startStopLogger = new StartStopLogger(discordBot);
            startStopLogger.sendStatusUpdateMessage(true);
            getLogger().warning("Start/Stop Logger - [Enabled]");
        }
        // Factions/Skyblock Top Announcer (Webhook)
        if (!config.getBoolean("announcer.enabled")) {
            getLogger().warning("Factions Top Announcer - [Not Enabled]");
        } else {
            this.factionsTopAnnouncer = new FactionsTopAnnouncer(config, this);
            Bukkit.getPluginManager().registerEvents(factionsTopAnnouncer, this);
            getLogger().warning("Factions Top Announcer - [Enabled]");
        }
        // Player Report Command (Webhook + Command)
        if (!config.getBoolean("bot.enabled")) {
            getLogger().warning("Player Reporting Command - [Not Enabled] - (Requires Discord Bot enabled)");
        } else if (!config.getBoolean("report.enabled")) {
            getLogger().warning("Player Reporting Command - [Not Enabled] - (Requires Discord Bot enabled)");
        } else {
            this.reportCommand = new ReportCommand(this, discordBot);
            Objects.requireNonNull(getCommand("report")).setExecutor(this.reportCommand);
            Bukkit.getPluginManager().registerEvents(reportCommand, this);
            getLogger().warning("Player Reporting Command - [Enabled]");
        }
        // Strike Command (Webhook + Command)
        if (!config.getBoolean("strike.enabled")) {
            getLogger().warning("Strike Command - [Not Enabled]");
        } else {
            this.factionStrike = new FactionStrike(config, this);
            Objects.requireNonNull(getCommand("strike")).setExecutor(this.factionStrike);
            getLogger().warning("Strike Command - [Enabled]");
        }
        // Bug Report Command (Webhook + Command)
        if (!config.getBoolean("bot.enabled")) {
            getLogger().warning("Bug Report Command - [Not Enabled] - (Requires Discord Bot enabled)");
        } else if (!config.getBoolean("bug_report.enabled")) {
            getLogger().warning("Bug Report Command - [Not Enabled]");
        } else {
            this.bugCommand = new BugCommand(this, discordBot, config);
            getServer().getPluginManager().registerEvents(this.bugCommand, this);
            Objects.requireNonNull(getCommand("bug")).setExecutor(this.bugCommand);
            getLogger().warning("Bug Command - [Enabled]");
        }
        // Join Leave Logger (Webhooks)
        if (!config.getBoolean("player_leave_join_logger.enabled")) {
            getLogger().warning("Player Join Leave Logger - [Not Enabled]");
        } else {
            this.joinLeaveLogger = new JoinLeaveLogger(config);
            Bukkit.getServer().getPluginManager().registerEvents(this.joinLeaveLogger, this);
            getLogger().warning("Player Join Leave Logger - [Enabled]");
        }
        // Suggestion Command (Discord Bot + Command)
        if (!config.getBoolean("bot.enabled")) {
            getLogger().warning("Suggestion Command - [Not Enabled] - (Requires Discord Bot enabled)");
        } else if (!config.getBoolean("suggestion.enabled")) {
            getLogger().warning("Suggestion Command - [Not Enabled]");
        } else {
            this.suggestionCommand = new SuggestionCommand(this.discordBot, config);
            Objects.requireNonNull(getCommand("suggestion")).setExecutor(this.suggestionCommand);
            getServer().getPluginManager().registerEvents(this.suggestionCommand, this);
            getLogger().warning("Suggestion Command - [Enabled]");
        }
        // Command Logger (Discord Feature)
        if (!config.getBoolean("bot.enabled")) {
            getLogger().warning("Command Logger - [Not Enabled] - (Requires Discord Bot enabled)");
        } else {
            this.commandLogger = new CommandLogger(this.discordBot);
            getServer().getPluginManager().registerEvents(this.commandLogger, this);
            getLogger().warning("Command Logger - [Enabled]");
        }

        // Creative Logging (Webhooks)
        if (!config.getBoolean("creative-logging.enabled")) {
            getLogger().warning("Creative Logging - [Not Enabled]");
        } else {
            this.creativeLogger = new CreativeMiddleClickLogger(this);
            this.creativeDropLogger = new CreativeDropLogger(this);
            getServer().getPluginManager().registerEvents(this.creativeLogger, this);
            getServer().getPluginManager().registerEvents(this.creativeDropLogger, this);
            getLogger().warning("Creative Logging - [Enabled]");
        }

        // LiteBans Logging (Webhooks)
        if (!config.getBoolean("litebans.enabled")) {
            getLogger().warning("LiteBans Logging - [Not Enabled]");
        } else {
            this.lbKickListener = new LBKickListener(this);
            lbKickListener.registerEvents();
            this.lbWarnListener = new LBWarnListener(this);
            lbWarnListener.registerEvents();
            this.lbMuteListener = new LBMuteListener(this);
            lbMuteListener.registerEvents();
            this.bansListener = new LBBanListener(this);
            bansListener.registerEvents();
            getLogger().warning("LiteBans Logging - [Enabled]");
        }

        // AdvacnedBans Logging (Webhooks)
        if (!config.getBoolean("advancedbans.enabled")) {
            getLogger().warning("AdvancedBans Logging - [Not Enabled]");
        } else {
            this.aMuteListener = new AMuteListener(this);
            getServer().getPluginManager().registerEvents(aMuteListener, this);
            this.aIPBanListener = new AIPBanListener(this);
            getServer().getPluginManager().registerEvents(aIPBanListener, this);
            this.aKickListener = new AKickListener(this);
            getServer().getPluginManager().registerEvents(aKickListener, this);
            this.aWarnListener = new AWarnListener(this);
            getServer().getPluginManager().registerEvents(aWarnListener, this);
            this.aTempBanListener = new ATempBanListener(this);
            getServer().getPluginManager().registerEvents(aTempBanListener, this);
            this.aBanListener = new ABanListener(this);
            getServer().getPluginManager().registerEvents(aBanListener, this);
            getLogger().warning("AdvancedBans Logging - [Enabled]");
        }
        Bukkit.getConsoleSender().sendMessage("[KushStaffUtils] Config options have been reloaded!");
    }

    private FileConfiguration loadSyncingConfig() {
        saveResource("syncing.yml", false);

        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), "syncing.yml"));
    }

    private void setSyncingConfig() {
        syncingConfig.options().copyDefaults(true);
        saveSyncingConfig();

    }

    private void saveSyncingConfig() {
        try {
            syncingConfig.save(new File(getDataFolder(), "syncing.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private FileConfiguration loadMessagesConfig() {
        saveResource("messages.yml", false);

        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
    }

    private void setDefaultMessages() {
        messagesConfig.addDefault("reloadMessage", "&c[&c&ᴋᴜѕʜѕᴛᴀꜰꜰᴜᴛɪʟѕ&c] &8» &dThe config files have been reloaded!");
        messagesConfig.addDefault("noPermissionMessage", "&c[&c&ᴋᴜѕʜѕᴛᴀꜰꜰᴜᴛɪʟѕ&c] &8» &cYou do not have permission to &d/stafflogger&f!");

        messagesConfig.options().copyDefaults(true);
        saveMessagesConfig();
    }

    private void saveMessagesConfig() {
        try {
            messagesConfig.save(new File(getDataFolder(), "messages.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setBotMessages() {
        discordBotConfig.addDefault("bot.factionTopCommandRoleID", "faction-top-command-role-id");

        discordBotConfig.options().copyDefaults(true);
        saveBotConfig();
    }

    public void saveBotConfig() {
        try {
            discordBotConfig.save(new File(getDataFolder(), "discord-bot.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration loadBotConfig() {
        saveResource("discord-bot.yml", false);

        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), "discord-bot.yml"));
    }



    public static String getCommandLoggerFolder() {
        return logsFolder;
    }


    public static KushStaffUtils getInstance() {
        return instance;
    }

    public static FactionsKore getFKoreInstance() {
        return factionsKore;
    }
}

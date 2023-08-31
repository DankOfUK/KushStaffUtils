package me.dankofuk.discord;

import me.dankofuk.discord.commands.*;
import me.dankofuk.discord.listeners.CommandLogger;
import me.dankofuk.discord.listeners.DiscordChat2Game;
import me.dankofuk.discord.listeners.StartStopLogger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DiscordBot extends ListenerAdapter {
    public String discordToken;
    public boolean discordBotEnabled;
    public Server minecraftServer;
    public String discordActivity;
    public JDA jda;
    public String adminRoleID;
    public Plugin botTask;
    public FileConfiguration config;
    public String ServerStatusChannelID;
    public String logChannelId;
    public CommandLogger commandLogger;
    public boolean logAsEmbed;
    public String titleFormat;
    public String footerFormat;
    public String listThumbnailUrl;
    public Plugin plugin;
    public StartStopLogger serverStatus;
    public String noPlayersTitle;
    public boolean requireAdminRole;
    public boolean logsCommandRequiresAdminRole;
    public List<String> ignoredCommands;
    private List<String> whitelistCommands;
    private boolean whitelistMode;
    private String serverName;
    private List<String> messageFormats;
    private List<String> embedTitleFormats;


    public DiscordBot(String discordToken, boolean discordBotEnabled, Server minecraftServer, String adminRoleID, String discordActivity, Plugin botTask, FileConfiguration config, String ServerStatusChannelID, String titleFormat, String footerFormat, String listThumbnailUrl, String noPlayersTitle, boolean requireAdminRole, boolean logsCommandRequiresAdminRole, List<String> ignoredCommands, List<String> whitelistCommands, boolean whitelistMode, String serverName, List<String> messageFormats, List<String> embedTitleFormats, String logChannelId, boolean logAsEmbed, Plugin plugin) {
        this.discordToken = discordToken;
        this.discordBotEnabled = discordBotEnabled;
        this.minecraftServer = minecraftServer;
        this.adminRoleID = adminRoleID;
        this.discordActivity = discordActivity;
        this.botTask = botTask;
        this.config = config;
        this.ServerStatusChannelID = ServerStatusChannelID;
        this.titleFormat = titleFormat;
        this.footerFormat = footerFormat;
        this.listThumbnailUrl = listThumbnailUrl;
        this.noPlayersTitle = noPlayersTitle;
        this.requireAdminRole = requireAdminRole;
        this.logsCommandRequiresAdminRole = logsCommandRequiresAdminRole;
        this.ignoredCommands = ignoredCommands;
        this.whitelistCommands = whitelistCommands;
        this.whitelistMode = whitelistMode;
        this.serverName = serverName;
        this.messageFormats = messageFormats;
        this.embedTitleFormats = embedTitleFormats;
        this.logChannelId = logChannelId;
        this.logAsEmbed = logAsEmbed;
        this.plugin = plugin;
    }


    public JDA getJda() {
        return jda;
    }

    public void start() throws InterruptedException {

        if (!discordBotEnabled) {
            return;
        }

        // Creates the Discord Bot
        jda = JDABuilder.createDefault(discordToken)
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(this)
                .setActivity(Activity.playing(discordActivity))
                .build()
                .awaitReady();

        // Configuration Options
        String noPlayersTitle = config.getString("bot.listplayers_no_players_online_title");
        String titleFormat = config.getString("bot.listplayers_title_format");
        String footerFormat = config.getString("bot.listplayers_footer_format");
        String listThumbnailUrl = config.getString("bot.listplayers_thumbnail_url");
        boolean requireAdminRole = config.getBoolean("bot.listplayers_requireAdminRole");
        boolean logsCommandRequiresAdminRole = config.getBoolean("bot.logsCommand_requireAdminRole");
        boolean enabled = config.getBoolean("bot.discord_to_game_enabled");
        boolean roleIdRequired = config.getBoolean("bot.discord_to_game_roleIdRequired");
        String channelId = config.getString("bot.discord_to_game_channel_id");
        String format = config.getString("bot.discord_to_game_format");
        String roleId = config.getString("bot.discord_to_game_roleId");
        boolean logCommands = config.getBoolean("per-user-logging.enabled");

        // Command Logger
        boolean logAsEmbed = config.getBoolean("commandlogger.logAsEmbed");
        String logChannelId = config.getString("commandlogger.channel_id");
        List<String> ignoredCommands = config.getStringList("ignored_commands");
        List<String> whitelistCommands = config.getStringList("whitelisted_commands");
        boolean whitelistMode = config.getBoolean("whitelist_mode_enabled");
        String serverName = config.getString("commandlogger.server_name");
        List<String> messageFormats = config.getStringList("commandlogger.message_formats");
        List<String> embedTitleFormats = config.getStringList("commandlogger.title_formats");

        // Event Listeners
        jda.addEventListener(new OnlinePlayersCommand(this, noPlayersTitle, titleFormat, footerFormat, listThumbnailUrl, requireAdminRole));
        jda.addEventListener(new StartStopLogger(this, ServerStatusChannelID));
        jda.addEventListener(new ConsoleCommand(this));
        jda.addEventListener(new HelpCommand(this));
        jda.addEventListener(new LogsCommand(this, logsCommandRequiresAdminRole, logCommands, config));
        jda.addEventListener(new CommandLogger(this, messageFormats, embedTitleFormats, serverName, logAsEmbed, logChannelId, ignoredCommands, whitelistMode, whitelistCommands));
        jda.addEventListener(new DiscordChat2Game(enabled, channelId, format, roleIdRequired, roleId));
        // Reload Discord Related Config Options
        jda.addEventListener(new ReloadCommand(this, config, logChannelId, logAsEmbed, titleFormat, footerFormat, listThumbnailUrl, noPlayersTitle, requireAdminRole, logsCommandRequiresAdminRole, ignoredCommands, whitelistCommands, whitelistMode, serverName, messageFormats, embedTitleFormats));
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        List<CommandData> commandsData =  new ArrayList<>();
        commandsData.add(Commands.slash("help", "Shows the list of all commands in this bot."));
        commandsData.add(Commands.slash("online", "Lists Online Players."));
        commandsData.add(Commands.slash("command", "Sends the command to the server.").addOption(OptionType.STRING, "command", "The command you want to send."));
        commandsData.add(Commands.slash("logs", "Gets the logs for the user you enter.").addOption(OptionType.STRING, "user", "The user you would like the logs for."));
        commandsData.add(Commands.slash("reload", "Reloads the bot configs. (only bot related)"));
        event.getJDA().updateCommands().addCommands(commandsData).queue();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        List<CommandData> commandsData =  new ArrayList<>();
        commandsData.add(Commands.slash("help", "Shows the list of all commands in this bot."));
        commandsData.add(Commands.slash("online", "Lists Online Players."));
        commandsData.add(Commands.slash("command", "Sends the command to the server.").addOption(OptionType.STRING, "command", "The command you want to send."));
        commandsData.add(Commands.slash("logs", "Gets the logs for the user you enter.").addOption(OptionType.STRING, "user", "The user you would like the logs for."));
        commandsData.add(Commands.slash("reload", "Reloads the bot configs. (only bot related)"));
        event.getJDA().updateCommands().addCommands(commandsData).queue();
    }


    // Method for stopping the Discord Bot
    public void stop() {
        if (jda != null) {
            jda.shutdown();
            // Cancel any tasks registered by the bot
            Bukkit.getScheduler().getPendingTasks().stream()
                    .filter(task -> task.getOwner() == botTask)
                    .forEach(BukkitTask::cancel);
        }
    }


    // Reload Discord Elements
    public void reloadDiscordConfig(String discordToken, boolean discordBotEnabled, Server minecraftServer, String adminRoleID, String discordActivity, Plugin botTask, FileConfiguration config, String ServerStatusChannelID, String titleFormat, String footerFormat, String listThumbnailUrl, String noPlayersTitle, boolean requireAdminRole, boolean logsCommandRequiresAdminRole, List<String> ignoredCommands, List<String> whitelistCommands, boolean whitelistMode, String serverName, List<String> messageFormats, List<String> embedTitleFormats, String logChannelId, boolean logAsEmbed) {
        this.discordToken = discordToken;
        this.discordBotEnabled = discordBotEnabled;
        this.minecraftServer = minecraftServer;
        this.adminRoleID = adminRoleID;
        this.discordActivity = discordActivity;
        this.botTask = botTask;
        this.config = config;
        this.ServerStatusChannelID = ServerStatusChannelID;
        this.logChannelId = logChannelId;
        this.logAsEmbed = logAsEmbed;
        this.titleFormat = titleFormat;
        this.footerFormat = footerFormat;
        this.listThumbnailUrl = listThumbnailUrl;
        this.noPlayersTitle = noPlayersTitle;
        this.requireAdminRole = requireAdminRole;
        this.logsCommandRequiresAdminRole = logsCommandRequiresAdminRole;
        this.ignoredCommands = ignoredCommands;
        this.whitelistCommands = whitelistCommands;
        this.whitelistMode = whitelistMode;
        this.serverName = serverName;
        this.messageFormats = messageFormats;
        this.embedTitleFormats = embedTitleFormats;
    }


    public Server getMinecraftServer() {
        return Bukkit.getServer();
    }

    public String getAdminRoleID() {
        return adminRoleID;
    }
}

package me.dankofuk.discord.commands;

import me.dankofuk.discord.DiscordBot;
import me.dankofuk.discord.listeners.CommandLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReloadCommand extends ListenerAdapter {

    private final DiscordBot discordBot;
    public FileConfiguration config;
    public Plugin botTask;
    public String discordToken;
    private boolean discordBotEnabled;
    private Server minecraftServer;
    private String discordActivity;
    private JDA jda;
    private String adminRoleID;
    private String ServerStatusChannelID;
    private CommandLogger commandLogger;
    public String logChannelId;
    public boolean logAsEmbed;
    private String titleFormat;
    private String footerFormat;
    private String listThumbnailUrl;
    private String noPlayersTitle;
    private boolean requireAdminRole;
    private boolean logsCommandRequiresAdminRole;
    private List<String> ignoredCommands;
    private List<String> whitelistCommands;
    private boolean whitelistMode;
    private String serverName;
    private List<String> messageFormats;
    private List<String> embedTitleFormats;

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public ReloadCommand(DiscordBot discordBot, FileConfiguration config, String logChannelId, boolean logAsEmbed, String titleFormat, String footerFormat, String listThumbnailUrl, String noPlayersTitle, boolean requireAdminRole, boolean logsCommandRequiresAdminRole, List<String> ignoredCommands, List<String> whitelistCommands, boolean whitelistMode, String serverName, List<String> messageFormats, List<String> embedTitleFormats) {
        this.discordBot = discordBot;
        this.config = config;
        this.botTask = discordBot.botTask;
        this.discordToken = discordBot.discordToken;
        this.discordBotEnabled = discordBot.discordBotEnabled;
        this.adminRoleID = discordBot.adminRoleID;
        this.discordActivity = discordBot.discordActivity;
        this.ServerStatusChannelID = discordBot.ServerStatusChannelID;
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

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("reload")) {
            if (event.getMember() != null && event.getMember().getRoles().stream()
                    .anyMatch(role -> role.getId().equals(discordBot.getAdminRoleID()))) {
                try {
                    config.load(new File("plugins/KushStaffUtils/config.yml"));
                } catch (IOException | InvalidConfigurationException e) {
                    e.printStackTrace();
                }

                boolean enabled = config.getBoolean("bot.discord_to_game_enabled");
                String channelId = config.getString("bot.discord_to_game_channel_id");
                String roleId = config.getString("bot.discord_to_game_roleId");
                boolean roleIdRequired = config.getBoolean("bot.discord_to_game_roleIdRequired");
                String format = config.getString("bot.discord_to_game_format");
                String discordToken = config.getString("bot.discord_token");
                boolean discordBotEnabled = config.getBoolean("bot.enabled");
                String adminRoleID = config.getString("bot.adminRoleID");
                String discordActivity = config.getString("bot.discord_activity");
                String ServerStatusChannelID = config.getString("serverstatus.channel_id");
                String titleFormat = config.getString("bot.listplayers_title_format");
                String footerFormat = config.getString("bot.listplayers_footer_format");
                String listThumbnailUrl = config.getString("bot.listplayers_thumbnail_url");
                String noPlayersTitle = config.getString("bot.listplayers_no_players_online_title");
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
                Server minecraftServer = Bukkit.getServer();
                Bukkit.getScheduler().getPendingTasks().stream()
                        .filter(task -> task.getOwner() == botTask)
                        .forEach(task -> task.cancel());
                discordBot.reloadDiscordConfig(discordToken, discordBotEnabled, minecraftServer, adminRoleID, discordActivity, botTask, config, ServerStatusChannelID, titleFormat, footerFormat, listThumbnailUrl, noPlayersTitle, requireAdminRole, logsCommandRequiresAdminRole, ignoredCommands, whitelistCommands, whitelistMode, serverName, messageFormats, embedTitleFormats, logChannelId, logAsEmbed);
                discordBot.stop();

                // Reload config strings
                this.discordToken = discordToken;
                this.discordBotEnabled = discordBotEnabled;
                this.adminRoleID = adminRoleID;
                this.discordActivity = discordActivity;
                this.ServerStatusChannelID = ServerStatusChannelID;
                this.logChannelId = logChannelId;
                this.logAsEmbed = logAsEmbed;
                this.titleFormat = titleFormat;
                this.footerFormat = footerFormat;
                this.listThumbnailUrl = listThumbnailUrl;
                this.noPlayersTitle = listThumbnailUrl;
                this.requireAdminRole = requireAdminRole;
                this.ignoredCommands = ignoredCommands;
                this.whitelistCommands = whitelistCommands;
                this.whitelistMode = whitelistMode;
                this.serverName = serverName;
                this.messageFormats = messageFormats;
                this.embedTitleFormats = embedTitleFormats;

                EmbedBuilder stoppedEmbed = new EmbedBuilder();
                stoppedEmbed.setColor(Color.RED);
                stoppedEmbed.setTitle("Bot stopped!");
                stoppedEmbed.setDescription(">  `Reloading configuration....`");
                stoppedEmbed.setFooter(OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
                event.replyEmbeds(stoppedEmbed.build()).queue();
                try {
                    discordBot.start();
                    System.out.println("[KushStaffUtils - Discord Bot] Reloading Discord Bot...");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                EmbedBuilder startedEmbed = new EmbedBuilder();
                startedEmbed.setColor(Color.GREEN);
                startedEmbed.setTitle("Bot started!");
                startedEmbed.setDescription(">  `Reload Complete!`");
                startedEmbed.setFooter(OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
                event.getChannel().sendMessageEmbeds(startedEmbed.build()).queue();
            } else {
                EmbedBuilder noPerms = new EmbedBuilder();
                noPerms.setColor(Color.RED);
                noPerms.setTitle("Error #NotDankEnough");
                noPerms.setDescription(">  `You lack the required permissions for this command!`");
                event.replyEmbeds(noPerms.build()).queue();

            }
        }
    }
}


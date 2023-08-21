package me.dankofuk.discord.commands;

import me.dankofuk.discord.DiscordBot;
import me.dankofuk.discord.managers.UUIDFetcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class LogsCommand extends ListenerAdapter {

    private DiscordBot discordBot;
    private boolean requireAdminRole;
    private FileConfiguration config;
    private boolean logCommands;

    public LogsCommand(DiscordBot discordBot, boolean requireAdminRole, boolean logCommands, FileConfiguration config) {
        this.discordBot = discordBot;
        this.requireAdminRole = requireAdminRole;
        this.config = config;
        this.logCommands = logCommands;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("logs")) {
                    String username = event.getOption("user").getAsString();

            // Check if user has permissions to execute the command
            boolean hasPermission = event.getMember().getRoles().stream()
                    .anyMatch(role -> role.getId().equals(discordBot.getAdminRoleID()));

            if (!hasPermission) {
                EmbedBuilder noPerms = new EmbedBuilder();
                noPerms.setColor(Color.RED);
                noPerms.setTitle("Error #NotDankEnough");
                noPerms.setDescription(">  `You lack the required permissions for this command!`");
                noPerms.setFooter(OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
                event.replyEmbeds(noPerms.build()).queue();
                return;
            }

            if (!config.getBoolean("per-user-logging.enabled")) {
                EmbedBuilder e = new EmbedBuilder();
                e.setColor(Color.RED);
                e.setTitle("Error 404");
                e.setDescription(">  `Per Player Logging is not enabled in the config.`");
                event.replyEmbeds(e.build()).queue();
                return;
            }
               UUID minecraftUUID = UUIDFetcher.getUUID(username);
               if (minecraftUUID != null) {
                   String fileName = minecraftUUID + ".txt";

                   FileUpload file = FileUpload.fromData(new File("plugins/KushStaffUtils/logs/" + fileName));
                   event.reply("Log file for " + username).addFiles(file).queue();
               } else {
                   event.reply("Could not find a log for " + username).queue();
            }
        }
    }
}

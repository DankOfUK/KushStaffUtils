package me.dankofuk.discord.commands;

import me.dankofuk.KushStaffUtils;
import me.dankofuk.discord.DiscordBot;
import me.dankofuk.discord.managers.UUIDFetcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public class LogsCommand extends ListenerAdapter {

    private final DiscordBot discordBot;
    private KushStaffUtils main;

    public LogsCommand(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("logs")) {
            String username = Objects.requireNonNull(event.getOption("user")).getAsString();
            boolean hasPermission = Objects.requireNonNull(event.getMember()).getRoles().stream()
                    .anyMatch(role -> role.getId().equals(discordBot.getAdminRoleID()));

            if (!hasPermission) {
                EmbedBuilder noPerms = new EmbedBuilder();
                noPerms.setColor(Color.RED);
                noPerms.setTitle("Error #NotDankEnough");
                noPerms.setDescription(">  `You lack the required permissions for this command!`");
                noPerms.setFooter(OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
                event.replyEmbeds(noPerms.build()).setEphemeral(true).queue();
                return;
            }

            if (!KushStaffUtils.getInstance().getConfig().getBoolean("per-user-logging.enabled")) {
                EmbedBuilder e = new EmbedBuilder();
                e.setColor(Color.RED);
                e.setTitle("Error 404");
                e.setDescription(">  `Per Player Logging is not enabled in the config.`");
                event.replyEmbeds(e.build()).setEphemeral(true).queue();
                return;
            }
            UUID minecraftUUID = UUIDFetcher.getUUID(username);
            if (minecraftUUID != null) {
                String fileName = minecraftUUID + ".txt";
                File logFile = new File("plugins/KushStaffUtils/logs/" + fileName);

                if (logFile.exists()) {
                    FileUpload file = FileUpload.fromData(logFile);
                    event.reply("Log file for " + username).addFiles(file).setEphemeral(true).queue();
                } else {
                    event.reply("Log file for " + username + " does not exist.").setEphemeral(true).queue();
                }
            }
        }
    }
}

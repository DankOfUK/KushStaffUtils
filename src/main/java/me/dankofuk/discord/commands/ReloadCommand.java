package me.dankofuk.discord.commands;

import me.dankofuk.KushStaffUtils;
import me.dankofuk.discord.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class ReloadCommand extends ListenerAdapter {
    private final DiscordBot discordBot;
    private Plugin botTask;

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public ReloadCommand (DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("reload"))
            if (event.getMember() != null && event.getMember().getRoles().stream()
                    .anyMatch(role -> role.getId().equals(discordBot.getAdminRoleID()))) {
                try {
                    FileConfiguration config = KushStaffUtils.getInstance().getConfig();
                    config.load(new File("plugins/KushStaffUtils/config.yml"));
                } catch (IOException|org.bukkit.configuration.InvalidConfigurationException e) {
                    e.printStackTrace();
                }
                KushStaffUtils.getInstance().loadBotConfig();

                Server minecraftServer = Bukkit.getServer();
                Bukkit.getScheduler().getPendingTasks().stream()
                        .filter(task -> (task.getOwner() == botTask))
                        .forEach(BukkitTask::cancel);
                Bukkit.getLogger().warning("[KushStaffUtils - Discord Bot] Stopping Discord Bot...");

                EmbedBuilder stoppedEmbed = new EmbedBuilder();
                stoppedEmbed.setColor(Color.RED);
                stoppedEmbed.setTitle("Bot stopped!");
                stoppedEmbed.setDescription(">  `Reloading configuration....`");
                stoppedEmbed.setFooter(OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
                event.replyEmbeds(stoppedEmbed.build(), new net.dv8tion.jda.api.entities.MessageEmbed[0]).queue();

                this.discordBot.reloadBot();

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
                event.getChannel().sendMessageEmbeds(noPerms.build()).queue();
            }
    }
}

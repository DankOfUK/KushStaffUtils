package me.dankofuk.discord.commands;

import me.clip.placeholderapi.PlaceholderAPI;
import me.dankofuk.discord.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Collectors;

public class FTopCommand extends ListenerAdapter {
    public DiscordBot discordBot;

    public FTopCommand(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("ftop")) {

            boolean hasPermission = Objects.requireNonNull(event.getMember()).getRoles().stream()
                    .anyMatch(role -> role.getId().equals(discordBot.getFactionTopCommandRoleID()));

            if (!hasPermission) {
                EmbedBuilder noPerms = new EmbedBuilder();
                noPerms.setColor(Color.RED);
                noPerms.setTitle("Error #NotDankEnough");
                noPerms.setDescription(">  `You lack the required permissions for this command!`");
                noPerms.setFooter(OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
                event.replyEmbeds(noPerms.build()).queue();
                return;
            }

            String message = discordBot.config.getStringList("announcer.messages").stream()
                    .map(line -> {
                        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                            return PlaceholderAPI.setPlaceholders(null, line);
                        } else {
                            return line;
                        }
                    })
                    .collect(Collectors.joining("\n"));
            long time = System.currentTimeMillis() / 1000L;

            EmbedBuilder helpEmbed = new EmbedBuilder();
            helpEmbed.setColor(Color.RED);
            helpEmbed.setTitle(discordBot.config.getString("announcer.title"));
            helpEmbed.setDescription(message.replace("%time%", "<t:" + time + ":R>"));
            helpEmbed.setFooter(discordBot.config.getString("announcer.footer"));
            event.replyEmbeds(helpEmbed.build()).setEphemeral(true).queue();
        }
    }
}
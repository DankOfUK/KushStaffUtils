package me.dankofuk.discord.commands;

import me.dankofuk.discord.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class OnlinePlayersCommand extends ListenerAdapter {
    private String noPlayersTitle;
    private String title;
    private String footer;
    private String thumbnailUrl;
    private Boolean requireAdminRole;
    private DiscordBot discordBot;

    public OnlinePlayersCommand(DiscordBot discordBot, String noPlayersTitle, String title, String footer, String thumbnailUrl, boolean requireAdminRole) {
        this.discordBot = discordBot;
        this.noPlayersTitle = noPlayersTitle;
        this.title = title;
        this.footer = footer;
        this.thumbnailUrl = thumbnailUrl;
        this.requireAdminRole = requireAdminRole;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("online")) {
            if (!requireAdminRole || (event.getMember() != null && event.getMember().getRoles().stream()
                    .anyMatch(role -> role.getId().equals(discordBot.getAdminRoleID())))) {

                List<String> playerNames = discordBot.getMinecraftServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());

                if (playerNames.isEmpty()) {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle(noPlayersTitle);
                    embed.setDescription(":negative_squared_cross_mark: `There are no players online currently!`");
                    embed.setThumbnail(thumbnailUrl);
                    event.replyEmbeds(embed.build()).queue();
                    return;
                }

                Instant currentTime = Instant.now();
                String unixTimestamp = String.valueOf(currentTime.getEpochSecond());

                String titleWithPlaceholders = title.replaceAll("%online%", String.valueOf(playerNames.size()))
                        .replaceAll("%time%", "<t:" + unixTimestamp + ":R>");

                String footerWithPlaceholders = footer.replaceAll("%online%", String.valueOf(playerNames.size()));

                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle(titleWithPlaceholders);
                embed.setDescription(String.join("\n", playerNames));
                embed.setFooter(footerWithPlaceholders);
                embed.setThumbnail(thumbnailUrl);
                event.replyEmbeds(embed.build()).queue();
            } else {
                event.reply(":no_entry_sign: You don't have permission to use this command.").queue();
            }
        }
    }
}

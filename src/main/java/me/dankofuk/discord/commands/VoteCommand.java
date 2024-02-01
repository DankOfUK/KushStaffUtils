package me.dankofuk.discord.commands;

import me.dankofuk.KushStaffUtils;
import me.dankofuk.discord.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class VoteCommand extends ListenerAdapter {
    public DiscordBot discordBot;

    public VoteCommand(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        boolean hasPermission = Objects.requireNonNull(event.getMember()).getRoles().stream()
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
        if (event.getName().equals("sendvotepanel")) {
            String channelId = Objects.requireNonNull(event.getOption("channel")).getAsString();
            MessageChannel channel = discordBot.jda.getChannelById(MessageChannel.class, channelId);
            if (channel == null) {
                event.reply("Invalid channel!").setEphemeral(true).queue();
                return;
            }
            FileConfiguration config = KushStaffUtils.getInstance().getConfig();
            String title = config.getString("VOTE-EMBED.TITLE");
            List<String> descriptionLines = config.getStringList("VOTE-EMBED.DESCRIPTION");
            String thumbnailUrl = config.getString("VOTE-EMBED.THUMBNAIL-URL");
            String description = String.join("\n", descriptionLines);
            Color color = Color.decode(Objects.requireNonNull(config.getString("VOTE-EMBED.COLOR")));

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle(title);
            embedBuilder.setDescription(description);
            embedBuilder.setColor(color);
            embedBuilder.setTimestamp(OffsetDateTime.now());
            embedBuilder.setThumbnail(thumbnailUrl);

            List<Button> buttons = new ArrayList<>();
            Objects.requireNonNull(config.getConfigurationSection("VOTE-BUTTONS")).getKeys(false).forEach(buttonKey -> {
                String id = buttonKey;
                String label = config.getString("VOTE-BUTTONS." + buttonKey + ".MESSAGE");
                buttons.add(Button.primary(id, label));
            });

            channel.sendMessageEmbeds(embedBuilder.build()).setActionRow(buttons).queue();
            event.reply("Vote embed with vote links sent to channel.").setEphemeral(true).queue();
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        User user = event.getUser();

        FileConfiguration config = KushStaffUtils.getInstance().getConfig();
        String voteLink = config.getString("VOTE-BUTTONS." + buttonId + ".VOTE-LINK");
        sendUserPrivateMessage(user, voteLink, event);

    }

    private void sendUserPrivateMessage(User user, String message, ButtonInteractionEvent event) {
        user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(message).queue(null, throwable -> event.getChannel().sendMessage(user.getAsMention() + " Please enable your direct messages to get the sync code!")
                .queue(sentMessage -> new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        sentMessage.delete().queue();
                    }
                }, 15000))));
    }

}

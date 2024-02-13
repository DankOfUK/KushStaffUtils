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
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VoteCommand extends ListenerAdapter {
    public DiscordBot discordBot;
    private final Map<String, Boolean> interactionHandled = new HashMap<>();

    public VoteCommand(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    private KushStaffUtils getPlugin() {
        return KushStaffUtils.getInstance();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent vEvent) {
        boolean hasPermission = Objects.requireNonNull(vEvent.getMember()).getRoles().stream()
                .anyMatch(role -> role.getId().equals(discordBot.getAdminRoleID()));

        if (!hasPermission) {
            EmbedBuilder noPerms = new EmbedBuilder();
            noPerms.setColor(Color.RED);
            noPerms.setTitle("Error #NotDankEnough");
            noPerms.setDescription(">  `You lack the required permissions for this command!`");
            noPerms.setFooter(OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
            vEvent.replyEmbeds(noPerms.build()).queue();
            return;
        }
        if (vEvent.getName().equals("sendvotepanel")) {
            String channelId = Objects.requireNonNull(vEvent.getOption("channel")).getAsString();
            MessageChannel channel = discordBot.jda.getChannelById(MessageChannel.class, channelId);


            FileConfiguration config = KushStaffUtils.getInstance().getConfig();
            String title = config.getString("VOTE-EMBED.TITLE");
            List<String> descriptionLines = config.getStringList("VOTE-EMBED.DESCRIPTION");
            String thumbnailUrl = config.getString("VOTE-EMBED.THUMBNAIL-URL");
            String description = String.join("\n", descriptionLines);
            Color color = Color.decode(Objects.requireNonNull(config.getString("VOTE-EMBED.COLOR")));

            EmbedBuilder embedVoteBuilder = new EmbedBuilder();
            embedVoteBuilder.setTitle(title);
            embedVoteBuilder.setDescription(description);
            embedVoteBuilder.setColor(color);
            embedVoteBuilder.setTimestamp(OffsetDateTime.now());
            embedVoteBuilder.setThumbnail(thumbnailUrl);

            List<Button> voteButtons = new ArrayList<>();
            Objects.requireNonNull(config.getConfigurationSection("VOTE-BUTTONS")).getKeys(false).forEach(voteButtonKey -> {
                String voteLabel = config.getString("VOTE-BUTTONS." + voteButtonKey + ".MESSAGE");
                voteButtons.add(Button.primary(voteButtonKey, Objects.requireNonNull(voteLabel)));
            });

            Objects.requireNonNull(channel).sendMessageEmbeds(embedVoteBuilder.build()).setActionRow(voteButtons).queue();
            vEvent.reply("Vote embed with vote links sent to channel.").setEphemeral(true).queue();
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent vEvent) {
        String voteButtonId = vEvent.getComponentId();
        User discordUser = vEvent.getUser();

        // Generate a unique interaction ID
        String interactionId = vEvent.getId() + voteButtonId;

        // Check if the interaction has already been handled
        if (interactionHandled.getOrDefault(interactionId, false)) {
            KushStaffUtils.getInstance().getLogger().info("Ignoring duplicate interaction: " + interactionId);
            return;
        }

        // Set the flag to true to mark that the interaction is being handled
        interactionHandled.put(interactionId, true);

        // Use CompletableFuture for asynchronous processing
        CompletableFuture.runAsync(() -> {
            FileConfiguration config = KushStaffUtils.getInstance().getConfig();
            String voteLink = config.getString("VOTE-BUTTONS." + voteButtonId + ".VOTE-LINK");
            if (voteLink == null) {
                KushStaffUtils.getInstance().getLogger().info("Vote Link not found: " + voteLink);
                return;
            }

            // Send the vote link to user's DM
            discordUser.openPrivateChannel().queue(privateChannel ->
                    privateChannel.sendMessage("The vote link has been sent to your DMs: " + voteLink).queue()
            );
        }).thenRun(() -> {
            // Reset the flag after a delay to allow for the next interaction
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
                interactionHandled.put(interactionId, false);
                KushStaffUtils.getInstance().getLogger().info("Interaction handling complete.");
            }, 20L);  // Delayed reset to ensure there's no interference with the next interaction
        });

        // Reply in the main thread to avoid the "already replied" issue
        vEvent.deferEdit().queue();
    }

    private void sendVoteUserPrivateMessage(User user, String message, ButtonInteractionEvent event) {
        user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(message).queue(null, throwable -> event.getChannel().sendMessage(user.getAsMention() + " Please enable your direct messages to get the sync code!")
                .queue(sentMessage -> new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        sentMessage.delete().queue();
                    }
                }, 15000))));
    }

}

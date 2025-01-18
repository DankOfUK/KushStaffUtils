package me.dankofuk.discord.commands;

import me.dankofuk.KushStaffUtils;
import me.dankofuk.discord.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class TicketSystem extends ListenerAdapter {

    private final DiscordBot discordBot;
    private final Map<String, String> ticketTypeToChannelIdMap;

    public TicketSystem(DiscordBot discordBot) {
        this.discordBot = discordBot;

        // Initialize the ticketTypeToChannelIdMap from config
        ticketTypeToChannelIdMap = Map.of(
                "support", Objects.requireNonNull(KushStaffUtils.getInstance().config.getString("TICKET_CHANNELS.SUPPORT")),
                "report", Objects.requireNonNull(KushStaffUtils.getInstance().config.getString("TICKET_CHANNELS.REPORT")),
                "feedback", Objects.requireNonNull(KushStaffUtils.getInstance().config.getString("TICKET_CHANNELS.FEEDBACK"))
        );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("createticketpanel")) return;

        // Check for admin permissions
        boolean hasPermission = Objects.requireNonNull(event.getMember()).getRoles().stream()
                .anyMatch(role -> role.getId().equals(discordBot.getAdminRoleID()));
        if (!hasPermission) {
            event.replyEmbeds(
                    new EmbedBuilder()
                            .setColor(Color.RED)
                            .setTitle("Error #NotDankEnough")
                            .setDescription("> `You lack the required permissions for this command!`")
                            .setFooter(OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME))
                            .build()
            ).setEphemeral(true).queue();
            return;
        }

        // Get the channel ID from the command option
        String channelId = Objects.requireNonNull(event.getOption("channel")).getAsString();
        MessageChannel channel = discordBot.jda.getChannelById(MessageChannel.class, channelId);

        if (!(channel instanceof TextChannel textChannel)) {
            event.reply("Please specify a valid text channel.").setEphemeral(true).queue();
            return;
        }

        // Create ticket panel embed
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Ticket Panel")
                .setDescription("Choose the type of ticket you want to create:\n" +
                        "💸 Support\n" +
                        "💩 Report\n" +
                        "📝 Feedback")
                .setColor(Color.GREEN);

        textChannel.sendMessageEmbeds(embedBuilder.build())
                .setActionRow(
                        Button.primary("ticket_support", "Support").withEmoji(Emoji.fromUnicode("💸")),
                        Button.danger("ticket_report", "Report").withEmoji(Emoji.fromUnicode("💩")),
                        Button.secondary("ticket_feedback", "Feedback").withEmoji(Emoji.fromUnicode("📝"))
                ).queue();

        event.reply("Ticket panel created in " + textChannel.getAsMention()).setEphemeral(true).queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String ticketType;
        switch (event.getComponentId()) {
            case "ticket_support" -> ticketType = "support";
            case "ticket_report" -> ticketType = "report";
            case "ticket_feedback" -> ticketType = "feedback";
            default -> {
                return;
            }
        }

        User user = event.getUser();
        String channelId = ticketTypeToChannelIdMap.get(ticketType);
        TextChannel ticketChannel = discordBot.jda.getTextChannelById(channelId);

        if (ticketChannel == null) {
            user.openPrivateChannel().queue(privateChannel ->
                    privateChannel.sendMessage("The destination channel for this ticket type is not accessible. Please contact an admin.").queue()
            );
            return;
        }

        // Create the ticket message embed
        EmbedBuilder ticketEmbed = new EmbedBuilder()
                .setTitle("New Ticket: " + ticketType.substring(0, 1).toUpperCase() + ticketType.substring(1))
                .setDescription("Ticket created by " + user.getAsTag())
                .setColor(Color.ORANGE);

        // Send the ticket to the appropriate channel
        ticketChannel.sendMessageEmbeds(ticketEmbed.build()).queue();

        // Notify user
        user.openPrivateChannel().queue(privateChannel ->
                privateChannel.sendMessage("Your " + ticketType + " ticket has been created in " + ticketChannel.getAsMention() + ".").queue()
        );

        // Acknowledge the button interaction
        event.reply("Your ticket has been created! Check " + ticketChannel.getAsMention()).setEphemeral(true).queue();
    }
}

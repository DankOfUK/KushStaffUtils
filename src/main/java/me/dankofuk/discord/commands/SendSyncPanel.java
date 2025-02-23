package me.dankofuk.discord.commands;

import me.dankofuk.KushStaffUtils;
import me.dankofuk.discord.DiscordBot;
import me.dankofuk.discord.syncing.SyncStorage;
import me.dankofuk.utils.CodeData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
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
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SendSyncPanel extends ListenerAdapter {

    public DiscordBot discordBot;
    private final Map<String, CodeData> codeDataMap = new ConcurrentHashMap<>();
    private final SyncStorage syncStorage;


    public SendSyncPanel(DiscordBot discordBot, SyncStorage syncStorage) {
        this.discordBot = discordBot;
        this.syncStorage = new SyncStorage(KushStaffUtils.getInstance().syncingConfig.getString("MYSQL.URL"), KushStaffUtils.getInstance().syncingConfig.getString("MYSQL.USERNAME"), KushStaffUtils.getInstance().syncingConfig.getString("MYSQL.PASSWORD"));
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
        if (event.getName().equals("sendsyncpanel")) {
            String channelId = Objects.requireNonNull(event.getOption("channel")).getAsString();
            MessageChannel channel = discordBot.jda.getChannelById(MessageChannel.class, channelId);

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Your Title Here");
            embedBuilder.setDescription(String.join("\n", KushStaffUtils.getInstance().syncingConfig.getStringList("SYNC-PANEL.EMBED-MESSAGE")));
            embedBuilder.setThumbnail(KushStaffUtils.getInstance().syncingConfig.getString("SYNC-PANEL.THUMBNAIL-URL"));
            embedBuilder.setColor(Color.BLUE);

            String buttonMessage = KushStaffUtils.getInstance().syncingConfig.getString("SYNC-PANEL.BUTTON-MESSAGE");

            Objects.requireNonNull(channel).sendMessageEmbeds(embedBuilder.build()).setActionRow(Button.primary("sync_button", Objects.requireNonNull(buttonMessage))).queue();
            channel.sendMessage("Panel sent to channel").queue();
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        if (event.getComponentId().equals("sync_button")) {
            User user = event.getUser();
            long userId = user.getIdLong();

            removeExpiredCodes();

            boolean hasActiveCode = codeDataMap.values().stream()
                    .anyMatch(codeData -> codeData.getUserId() == userId && System.currentTimeMillis() < codeData.getExpiryTime());

            if (syncStorage.isUserSynced(userId)) {
                sendUserPrivateMessage(user, "You are already synced! (If this is a mistake speak with your administrator)", event);
            } else if (hasActiveCode) {
                sendUserPrivateMessage(user, "You already have an active sync code. Please wait until it expires.", event);
            } else {
                String code = generateRandomCode();
                long expiryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);

                CodeData codeData = new CodeData(userId, code, expiryTime);
                codeDataMap.put(code, codeData);

                sendUserPrivateMessage(user, "Your sync code is: " + code + ". It will expire in 5 minutes.", event);
                event.reply("A sync code has been sent to your DMs.").setEphemeral(true).queue();
            }
        }
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


    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public void removeExpiredCodes() {
        long currentTime = System.currentTimeMillis();
        codeDataMap.entrySet().removeIf(entry -> currentTime > entry.getValue().getExpiryTime());
    }

    public boolean isCodeValid(String code) {
        CodeData codeData = codeDataMap.get(code);
        return codeData != null && System.currentTimeMillis() <= codeData.getExpiryTime();
    }

    public long getDiscordUserIdForCode(String code) {
        CodeData codeData = codeDataMap.get(code);
        return codeData != null ? codeData.getUserId() : -1;
    }
}
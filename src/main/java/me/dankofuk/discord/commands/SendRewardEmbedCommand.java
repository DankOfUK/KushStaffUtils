package me.dankofuk.discord.commands;

import me.dankofuk.KushStaffUtils;
import me.dankofuk.discord.DiscordBot;
import me.dankofuk.discord.syncing.SyncStorage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
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
import java.util.List;
import java.util.*;

public class SendRewardEmbedCommand extends ListenerAdapter {
    private final FileConfiguration config;
    public DiscordBot discordBot;
    public SyncStorage syncStorage;
    public Map<Long, Long> lastClaimedTimes = new HashMap<>();
    public String Url;
    public String Username;
    public String Password;

    public SendRewardEmbedCommand(DiscordBot discordBot, FileConfiguration config, String Url, String Username, String Password) {
        this.discordBot = discordBot;
        this.config = KushStaffUtils.getInstance().syncingConfig;
        this.Url = Url;
        this.Username = Username;
        this.Password = Password;
        this.syncStorage = new SyncStorage(Url, Username, Password);
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
        if (event.getName().equals("sendrewardpanel")) {
            String channelId = Objects.requireNonNull(event.getOption("channel")).getAsString();
            MessageChannel channel = discordBot.jda.getChannelById(MessageChannel.class, channelId);
            if (channel == null) {
                event.reply("Invalid channel!").setEphemeral(true).queue();
                return;
            }

            String title = config.getString("REWARD-EMBED.TITLE");
            List<String> descriptionLines = config.getStringList("REWARD-EMBED.DESCRIPTION");
            String description = String.join("\n", descriptionLines);
            String thumbnail = config.getString("REWARD-EMBED.THUMBNAIL-URL");
            Color color = Color.decode(Objects.requireNonNull(config.getString("REWARD-EMBED.COLOR")));

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle(title);
            embedBuilder.setDescription(description);
            embedBuilder.setColor(color);
            embedBuilder.setTimestamp(OffsetDateTime.now());
            embedBuilder.setThumbnail(thumbnail);

            List<Button> buttons = new ArrayList<>();
            Objects.requireNonNull(config.getConfigurationSection("BUTTONS")).getKeys(false).forEach(buttonKey -> {
                String label = config.getString("BUTTONS." + buttonKey + ".MESSAGE");
                buttons.add(Button.primary(buttonKey, Objects.requireNonNull(label)));
            });

            // Sending the message with buttons
            channel.sendMessageEmbeds(embedBuilder.build()).setActionRow(buttons).queue();
            event.reply("Reward embed with buttons sent to channel.").setEphemeral(true).queue();
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        long discordId = event.getUser().getIdLong();
        long currentTime = System.currentTimeMillis();

        String requiredRoleId = config.getString("BUTTONS." + buttonId + ".REQUIRED-ROLE-ID");
        long interval = config.getLong("BUTTONS." + buttonId + ".REWARD-INTERVAL") * 1000;

        if (canClaimReward(discordId, currentTime, interval) && hasRequiredRole(discordId, requiredRoleId)) {
            UUID minecraftUuid = syncStorage.getMinecraftUuid(discordId);
            if (minecraftUuid != null) {
                giveReward(minecraftUuid, buttonId);
                lastClaimedTimes.put(discordId, currentTime);
                event.reply("You have claimed your reward!").setEphemeral(true).queue();
            } else {
                event.reply("You need to sync with Minecraft to claim the reward.").setEphemeral(true).queue();
            }
        } else {
            long lastClaimedTime = lastClaimedTimes.getOrDefault(discordId, 0L);
            long timeUntilNextClaim = (lastClaimedTime + config.getLong("REWARD-INTERVAL") * 1000) - currentTime;
            event.reply("You can claim your next reward in " + timeUntilNextClaim / 1000 + " seconds.").setEphemeral(true).queue();
        }
    }

    private boolean canClaimReward(long discordId, long currentTime, long interval) {
        long lastClaimedTime = lastClaimedTimes.getOrDefault(discordId, 0L);
        return currentTime - lastClaimedTime >= interval;
    }

    private boolean hasRequiredRole(long discordId, String requiredRoleId) {
        Guild guild = discordBot.getJda().getGuilds().get(0);
        Member member = guild.getMemberById(discordId);
        return member != null && member.getRoles().stream().anyMatch(role -> role.getId().equals(requiredRoleId));
    }

    private void giveReward(UUID minecraftUuid, String buttonId) {
        Player player = Bukkit.getPlayer(minecraftUuid);
        if (player != null && player.isOnline()) {
            List<String> commands = config.getStringList("BUTTONS." + buttonId + ".REWARDS");
            for (String command : commands) {
                if (command != null && !command.isEmpty()) {
                    try {
                        String processedCommand = command.replace("%player%", player.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                    } catch (Exception e) {
                        // Log the error and continue with the next command
                        System.err.println("Error executing command: " + command);
                        e.printStackTrace();
                    }
                }
            }
            player.sendMessage(ChatColor.GREEN + "You have received your reward!");
        }
    }
}

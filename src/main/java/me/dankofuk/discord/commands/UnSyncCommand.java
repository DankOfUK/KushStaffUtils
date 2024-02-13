package me.dankofuk.discord.commands;

import me.dankofuk.KushStaffUtils;
import me.dankofuk.discord.DiscordBot;
import me.dankofuk.discord.syncing.SyncStorage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class UnSyncCommand extends ListenerAdapter {
    public DiscordBot discordBot;
    public final SyncStorage syncStorage;


    public UnSyncCommand(DiscordBot discordBot, SyncStorage syncStorage) {
        this.discordBot = discordBot;
        this.syncStorage = new SyncStorage(KushStaffUtils.getInstance().syncingConfig.getString("MYSQL.URL"), KushStaffUtils.getInstance().syncingConfig.getString("MYSQL.USERNAME"), KushStaffUtils.getInstance().syncingConfig.getString("MYSQL.PASSWORD"));
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!"unsync".equals(event.getName())) {
            return;
        }

        try {
            if (event.getMember() == null || event.getGuild() == null) {
                event.reply("This command must be used in a guild.").setEphemeral(true).queue();
                return;
            }

            boolean hasPermission = event.getMember().getRoles().stream()
                    .anyMatch(role -> role.getId().equals(discordBot.getAdminRoleID()));

            if (!hasPermission) {
                replyNoPermission(event);
                return;
            }

            if (event.getOption("user") == null) {
                event.reply("User not provided.").setEphemeral(true).queue();
                return;
            }

            long discordId = Objects.requireNonNull(event.getOption("user")).getAsUser().getIdLong();
            removeRoles(event.getGuild(), discordId);
            syncStorage.removeUserSync(discordId);

            event.reply("User has been unsynced successfully.").setEphemeral(true).queue();
        } catch (Exception e) {
            System.err.println("Error in UnSyncCommand: " + e.getMessage());
            e.printStackTrace();
            event.reply("An error occurred.").setEphemeral(true).queue();
        }
    }

    private void removeRoles(Guild guild, long discordId) {
        List<String> roleIds = KushStaffUtils.getInstance().syncingConfig.getStringList("SYNC-PANEL.ROLE-GIVEN-ON-SYNC");
        roleIds.forEach(roleId -> {
            Role role = guild.getRoleById(roleId);
            if (role != null) {
                guild.removeRoleFromMember(UserSnowflake.fromId(discordId), role).queue();
            }
        });
    }

    private void replyNoPermission(SlashCommandInteractionEvent event) {
        EmbedBuilder noPerms = new EmbedBuilder();
        noPerms.setColor(Color.RED);
        noPerms.setTitle("Error #NotDankEnough");
        noPerms.setDescription("> `You lack the required permissions for this command!`");
        noPerms.setFooter(OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
        event.replyEmbeds(noPerms.build()).queue();
    }
}

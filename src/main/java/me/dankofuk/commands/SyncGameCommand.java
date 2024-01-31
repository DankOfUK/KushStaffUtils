package me.dankofuk.commands;

import me.dankofuk.KushStaffUtils;
import me.dankofuk.discord.DiscordBot;
import me.dankofuk.discord.commands.SendSyncPanel;
import me.dankofuk.utils.ColorUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class SyncGameCommand implements CommandExecutor {

    private final SendSyncPanel sendPanel;
    private final DiscordBot discordBot;
    private final me.dankofuk.discord.syncing.SyncStorage syncStorage;
    public String Url;
    public String Username;
    public String Password;


    public SyncGameCommand(DiscordBot discordBot, String Url, String Username, String Password) {
        this.discordBot = discordBot;
        this.Url = Url;
        this.Username = Username;
        this.Password = Password;
        this.sendPanel = discordBot.getSendPanel();
        this.syncStorage = new me.dankofuk.discord.syncing.SyncStorage(Url, Username, Password);

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("commandlogger.sync.use")) {
            player.sendMessage(ColorUtils.translateColorCodes(Objects.requireNonNull(KushStaffUtils.getInstance().messagesConfig.getString("        try {\n"))));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ColorUtils.translateColorCodes(Objects.requireNonNull(KushStaffUtils.getInstance().syncingConfig.getString("MESSAGES.COMMAND-USAGE-MESSAGE"))));
            return true;
        }

        String code = args[0];
        if (!sendPanel.isCodeValid(code)) {
            player.sendMessage(ColorUtils.translateColorCodes(Objects.requireNonNull(KushStaffUtils.getInstance().syncingConfig.getString("MESSAGES.INVALID-CODE-MESSAGE"))));
            return true;
        }

        long discordUserId = sendPanel.getDiscordUserIdForCode(code);

        if (syncStorage.isUserSynced(discordUserId)) {
            player.sendMessage(ColorUtils.translateColorCodes(Objects.requireNonNull(KushStaffUtils.getInstance().syncingConfig.getString("MESSAGES.ALREADY-SYNCED-MESSAGE"))));
            return true;
        }
        assignRolesToDiscordUser(discordUserId, player);
        player.sendMessage(ColorUtils.translateColorCodes(Objects.requireNonNull(KushStaffUtils.getInstance().syncingConfig.getString("MESSAGES.SYNCED-SUCCESSFULLY-MESSAGE"))));
        syncStorage.saveSyncData(discordUserId, player.getUniqueId());
        return true;

    }

    private void assignRolesToDiscordUser(long discordUserId, Player player) {

        List<String> roleIds = KushStaffUtils.getInstance().syncingConfig.getStringList("SYNC-PANEL.ROLE-GIVEN-ON-SYNC");

        Guild guild = discordBot.getJda().getGuilds().isEmpty() ? null : discordBot.getJda().getGuilds().get(0);
        if (guild == null) {
            Bukkit.getLogger().warning("No guild found for the Discord bot.");
            return;
        }
        guild.retrieveMemberById(discordUserId).queue(member -> {
            boolean alreadyHasAllRoles = roleIds.stream()
                    .allMatch(roleId -> member.getRoles().stream().anyMatch(role -> role.getId().equals(roleId)));

            if (alreadyHasAllRoles) {
                player.sendMessage(Objects.requireNonNull(KushStaffUtils.getInstance().syncingConfig.getString(ColorUtils.translateColorCodes("MESSAGES.ALREADY-SYNCED-MESSAGE"))));
                //Bukkit.getLogger().info("User already has all the roles. Skipping role assignment.");
                return;
            }

            for (String roleId : roleIds) {
                Role role = guild.getRoleById(roleId);
                if (role == null) {
                    Bukkit.getLogger().warning("Role not found for ID: " + roleId);
                    continue;
                }

                guild.addRoleToMember(member, role).queue(
                        //success -> Bukkit.getLogger().info("Assigned role " + role.getName() + " to Discord user: " + discordUserId),
                        //failure -> Bukkit.getLogger().warning("Failed to assign role: " + failure.getMessage())
                );
            }
        }, failure -> Bukkit.getLogger().warning("Failed to retrieve member for Discord user: " + discordUserId));
    }

}
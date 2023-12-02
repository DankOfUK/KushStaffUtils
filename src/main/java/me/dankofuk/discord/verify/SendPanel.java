package me.dankofuk.discord.verify;

import me.dankofuk.KushStaffUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.configuration.Configuration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SendPanel extends ListenerAdapter {

    public String roleID;
    public KushStaffUtils instance;
    public JDA jda;

    public SendPanel(JDA jda, KushStaffUtils instance) {
        this.jda = jda;
        this.instance = instance;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        if (event.getName().equals("sendverifypanel")) {
            Configuration config = KushStaffUtils.getInstance().getConfig();
            String channelId = event.getOption("channel").getAsString();
            MessageChannel channel = jda.getChannelById(MessageChannel.class, channelId);
            String sentMessage = config.getString("verify_panel.sentMessage");
            String buttonMessage = config.getString("verify_panel.buttonMessage");
            List<String> embedMessageList = config.getStringList("verify_panel.embedMessage");
            String embedMessage = String.join("\n", embedMessageList);
            if (channel != null) {
                channel.sendMessage(embedMessage).setActionRow(
                        Button.primary("verify_button", buttonMessage)
                ).queue();
                event.reply(sentMessage).setEphemeral(true).queue();
            } else {
                event.reply("Invalid channel ID").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getComponentId().equals("verify_button")) {
            Guild guild = event.getGuild();
            if (guild != null) {
                Configuration config = KushStaffUtils.getInstance().getConfig();
                String roleId = config.getString("verify_panel.giveRoleOnClicks");
                Role role = guild.getRoleById(roleId);
                if (role != null) {
                    guild.addRoleToMember(UserSnowflake.fromId(event.getUser().getId()), role).queue();
                      event.reply(config.getString("verify_panel.roleGivenMessage")).setEphemeral(true).queue();
                } else {
                    event.reply("Role not found.").setEphemeral(true).queue();
                }
            }
        }
    }
}
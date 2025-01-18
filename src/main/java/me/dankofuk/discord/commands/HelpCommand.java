package me.dankofuk.discord.commands;

import me.dankofuk.discord.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class HelpCommand extends ListenerAdapter {
    public DiscordBot discordBot;

    public HelpCommand(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("help")) {

                EmbedBuilder helpEmbed = new EmbedBuilder();
                helpEmbed.setColor(Color.RED);
                helpEmbed.setTitle("__`Help Page 1/1`__");
                helpEmbed.setDescription("Command List");

                helpEmbed.addField("/help", "Shows this menu", false);
                helpEmbed.addField("/command [command]", "Sends a command to the server!", true);
                helpEmbed.addField("/online", "Shows the players online", false);
                helpEmbed.addField("/sendverifypanel [channel]", "Sends the verify panel the selected channel.", true);
                helpEmbed.addField("/logs [user]", "Shows the log file for the user selected", true);
                helpEmbed.addField("/avatar [user]", "Shows the avatar for the user selected", true);
                helpEmbed.addField("/unsync [user]", "Starts the syncing process!", true);
                helpEmbed.addField("/sendsyncpanel [channel]", "Sends the sync panel the selected channel.", true);
                helpEmbed.addField("/createticketpanel", "Sends the ticket panel the current channel.", true);
                helpEmbed.addField("/sendrewardpanel [channel]", "Sends the reward panel the selected channel.", true);
                helpEmbed.addField("/reload", "Reloads the configs for the bot related stuff.", false);

                helpEmbed.setFooter("Help Page 1/1 - Made by Exotic Development");
                event.replyEmbeds(helpEmbed.build()).setEphemeral(true).queue();
        }
    }
}

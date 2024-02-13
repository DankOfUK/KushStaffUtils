package me.dankofuk.fkore;

import com.golfing8.kore.FactionsKore;
import com.golfing8.kore.event.PlayerPrinterExitEvent;
import com.golfing8.kore.feature.PrinterFeature;
import me.dankofuk.KushStaffUtils;
import me.dankofuk.utils.WebhookUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.awt.*;
import java.io.IOException;

public class FKorePrinterLogger implements Listener {
    public PrinterFeature printerFeature;
    public FactionsKore fkore;

    public FKorePrinterLogger(PrinterFeature printerFeature, FactionsKore fkore) {
        this.printerFeature = printerFeature;
        this.fkore = fkore;

    }

    // Method to check if the player is in Printer mode
        public boolean isInPrinterMode(Player player) {
        return printerFeature.isInPrinter(player);
    }

    // Method to handle the exit from Printer mode
    public void handlePrinterExit(PlayerPrinterExitEvent event) {
        Player player = event.getPlayer();
        PlayerPrinterExitEvent.ExitReason reason = event.getExitReason();

        // You can customize the logging and webhook sending logic here
        if (isInPrinterMode(player)) {
            // Player was in Printer mode
            fkore.getLogger().info(player.getName() + " exited Printer mode with reason: " + reason.toString());

            // Send webhook to Discord (implement your logic)
            sendWebhookToDiscord(player.getName(), reason.toString());
        }
    }

    // Method to send a webhook to Discord (replace with your implementation)
    private void sendWebhookToDiscord(String playerName, String exitReason) {
        // Replace "your_webhook_url" with the actual webhook URL
        String webhookUrl = KushStaffUtils.getInstance().getConfig().getString("FKORE-PRINTER-LOGGER.your_webhook_url");

        WebhookUtils webhook = new WebhookUtils(webhookUrl);

        // Customize your webhook content, username, avatar, etc.
        webhook.setContent("Player " + playerName + " exited Printer mode");
        webhook.setUsername("PrinterLogger");
        webhook.setAvatarUrl("https://example.com/avatar.png");
        webhook.setTts(false);

        // Add an embed to the webhook for additional formatting
        WebhookUtils.EmbedObject embed = new WebhookUtils.EmbedObject()
                .setTitle("Printer Exit Log")
                .setDescription("Player " + playerName + " exited Printer mode with reason: " + exitReason)
                .setColor(Color.ORANGE);

        webhook.addEmbed(embed);

        try {
            webhook.execute();
        } catch (IOException e) {
            // Handle exception (e.g., log the error)
            e.printStackTrace();
        }
    }
}

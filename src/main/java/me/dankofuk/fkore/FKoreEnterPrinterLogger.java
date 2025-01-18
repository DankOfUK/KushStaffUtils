package me.dankofuk.fkore;

import com.golfing8.kore.event.PlayerPrinterEnterEvent;
import com.golfing8.kore.event.PlayerPrinterExitEvent;
import com.golfing8.kore.feature.PrinterFeature;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.dankofuk.KushStaffUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class FKoreEnterPrinterLogger implements Listener {
    public PrinterFeature printerFeature;
    public KushStaffUtils instance;

    public FKoreEnterPrinterLogger(PrinterFeature printerFeature, KushStaffUtils instance) {
        this.printerFeature = printerFeature;
        this.instance = instance;
    }

    @EventHandler
    public void handlePrinterEnter(PlayerPrinterEnterEvent event) {
        Player player = event.getPlayer();
            KushStaffUtils.getInstance().getLogger().info(player.getName() + " entered Printer mode!");
            try {
                sendWebhookToDiscord(player.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    private void sendWebhookToDiscord(String playerName) {
        KushStaffUtils.getInstance().getLogger().info("Sending enter webhook to Discord");
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(Objects.requireNonNull(KushStaffUtils.getInstance().getConfig().getString("PRINTER-LOGGER.webhookUrl")));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "PrinterLogger");
                connection.setDoOutput(true);

                JsonObject json = new JsonObject();
                json.addProperty("username", "Printer Logger");

                // Customize content based on the configuration
                JsonArray embeds = new JsonArray();
                JsonObject embed = new JsonObject();

                // Configurable embed properties
                embed.addProperty("description", KushStaffUtils.getInstance().getConfig().getString("PRINTER-LOGGER.description")
                        .replace("%player%", playerName));
                embed.addProperty("title", KushStaffUtils.getInstance().getConfig().getString("PRINTER-LOGGER.title"));
                embed.addProperty("color", KushStaffUtils.getInstance().getConfig().getInt("PRINTER-LOGGER.color"));

                embeds.add(embed);

                json.add("embeds", embeds);

                String message = (new Gson()).toJson(json);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(message.getBytes());
                }

                connection.connect();
                int responseCode = connection.getResponseCode();
                String str1 = connection.getResponseMessage();
            } catch (MalformedURLException e) {
                Bukkit.getLogger().warning("[PrinterLogger] Invalid webhook URL specified: " + KushStaffUtils.getInstance().getConfig().getString("PRINTER-LOGGER.webhookUrl"));
                //e.printStackTrace();
            } catch (ProtocolException e) {
                Bukkit.getLogger().warning("[PrinterLogger] Invalid protocol specified in webhook URL: " + KushStaffUtils.getInstance().getConfig().getString("PRINTER-LOGGER.webhookUrl"));
                //e.printStackTrace();
            } catch (IOException e) {
                Bukkit.getLogger().warning("[PrinterLogger] Error sending message to Discord webhook.");
               // e.printStackTrace();
            }
        });
    }
}

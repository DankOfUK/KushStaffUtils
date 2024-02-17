package me.dankofuk.fkore;

import com.golfing8.kore.event.PlayerPrinterExitEvent;
import com.golfing8.kore.feature.PrinterFeature;
import com.golfing8.kore.object.PlayerPrinterInfo;
import com.golfing8.kore.util.MaterialInfo;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.dankofuk.KushStaffUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class FKoreLeavePrinterLogger implements Listener {
    public PrinterFeature printerFeature;
    public KushStaffUtils instance;

    public FKoreLeavePrinterLogger(PrinterFeature printerFeature, KushStaffUtils instance) {
        this.printerFeature = printerFeature;
        this.instance = instance;

    }

    @EventHandler
    public void handlePrinterExit(PlayerPrinterExitEvent event) {
        Player player = event.getPlayer();
        PlayerPrinterExitEvent.ExitReason reason = event.getExitReason();
        BigDecimal moneySpent = event.getPrinterInfo().getMoneySpent();
        int xLocation = event.getPlayer().getLocation().getBlockX();
        int yLocation = event.getPlayer().getLocation().getBlockY();
        int zLocation = event.getPlayer().getLocation().getBlockZ();
        String worldName  = Objects.requireNonNull(event.getPlayer().getLocation().getWorld()).getName();
        Map<MaterialInfo, PlayerPrinterInfo.BlockRecord> blocksPlaced = event.getPrinterInfo().getPrintedBlocks();
        if (printerFeature.isInPrinter(player)) {
            KushStaffUtils.getInstance().getLogger().info(player.getName() + " exited Printer mode with reason: " + reason.toString());
            try {
                sendWebhookToDiscord(player.getName(), reason.toString(), moneySpent, blocksPlaced, xLocation, yLocation, zLocation, worldName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendWebhookToDiscord(String playerName, String exitReason, BigDecimal moneySpent, Map<MaterialInfo, PlayerPrinterInfo.BlockRecord> blocksPlaced, int xLocation, int yLocation, int zLocation, String worldName) {
        KushStaffUtils.getInstance().getLogger().info("Sending exit webhook to Discord");
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(Objects.requireNonNull(KushStaffUtils.getInstance().getConfig().getString("PRINTER-LOGGER.webhookUrl")));
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "PrinterLogger");
                connection.setDoOutput(true);
                JsonObject json = new JsonObject();
                json.addProperty("username", "Printer Logger");
                JsonObject embed = new JsonObject();
                embed.addProperty("description", playerName + " has exited printer mode!\n Reason: "
                        + exitReason + "\n Money Spent: " + moneySpent + "\n Blocks placed: "
                        + blocksPlaced + "\n Location: " + "X:" + xLocation + " Y:" + yLocation
                        + " Z:" + zLocation + " World: " + worldName);
                embed.addProperty("title", "Printer Logger");
                JsonArray embeds = new JsonArray();
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
                e.printStackTrace();
            } catch (ProtocolException e) {
                Bukkit.getLogger().warning("[PrinterLogger] Invalid protocol specified in webhook URL: " + KushStaffUtils.getInstance().getConfig().getString("PRINTER-LOGGER.webhookUrl"));
                e.printStackTrace();
            } catch (IOException e) {
                Bukkit.getLogger().warning("[PrinterLogger] Error sending message to Discord webhook.");
                e.printStackTrace();
            }
        });
    }
}

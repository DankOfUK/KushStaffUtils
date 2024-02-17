package me.dankofuk.fkore;

import com.golfing8.kore.FactionsKore;
import com.golfing8.kore.topexpansion.event.FTopKnockEvent;
import com.golfing8.kore.topexpansion.feature.FactionsTopFeature;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.dankofuk.KushStaffUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class FKoreFactionOvertakeLogger implements Listener {
    public FactionsTopFeature ftop;
    public KushStaffUtils instance;

    public FKoreFactionOvertakeLogger(FactionsTopFeature ftop, KushStaffUtils instance) {
        this.ftop = ftop;
        this.instance = instance;

    }

    @EventHandler
    public void overTakeFaction(FTopKnockEvent event) {
        String knockedId = event.getKnockedID();
        String overtakeId = event.getOvertakingID();
        String eventName = event.getEventName();
        int knockPosition = event.getKnockPosition();
        HandlerList handler = event.getHandlers();

        try {
            sendWebhookToDiscord(knockedId, overtakeId, eventName, knockPosition);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private void sendWebhookToDiscord(String knockedId, String overtakeId, String eventName, int knockPosition) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(Objects.requireNonNull(KushStaffUtils.getInstance().getConfig().getString("FKORE-OVERTAKE-LOGGER.webhookUrl")));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "FKoreFtopOvertakeLogger");
                connection.setDoOutput(true);

                JsonObject json = new JsonObject();
                json.addProperty("username", "FKoreFtopOvertakeLogger");
                JsonArray embeds = new JsonArray();
                JsonObject embed = new JsonObject();
                embed.addProperty("description", "Faction " + FactionsKore.getIntegration().getTagFromId(knockedId) + " was overtaken by " + FactionsKore.getIntegration().getTagFromId(overtakeId) + " at position " + knockPosition);
                embed.addProperty("title", "FKoreFtopOvertakeLogger");
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
                Bukkit.getLogger().warning("[FKoreFtopOvertakeLogger] Invalid webhook URL specified: " + KushStaffUtils.getInstance().getConfig().getString("FKORE-OVERTAKE-LOGGER.webhookUrl"));
                e.printStackTrace();
            } catch (ProtocolException e) {
                Bukkit.getLogger().warning("[FKoreFtopOvertakeLogger] Invalid protocol specified in webhook URL: " + KushStaffUtils.getInstance().getConfig().getString("FKORE-OVERTAKE-LOGGER.webhookUrl"));
                e.printStackTrace();
            } catch (IOException e) {
                Bukkit.getLogger().warning("[FKoreFtopOvertakeLogger] Error sending message to Discord webhook.");
                e.printStackTrace();
            }
        });
    }
}

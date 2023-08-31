package me.dankofuk.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FileCommandLogger implements Listener {
    private final String dataFolder;
    private final Map<UUID, BufferedWriter> writerMap;
    public boolean logCommands;

    public FileCommandLogger(String dataFolder) {
        this.dataFolder = dataFolder;
        File logsFolder = new File(dataFolder);
        boolean success = logsFolder.mkdirs();
        if (!success && !logsFolder.exists()) {
            // Handle the error here
            System.err.println("[KushStaffLogger] Player Logs: Failed to create the logs folder at path " + logsFolder.getAbsolutePath());
        } else {
            // TODO Change logger
            System.out.println("[KushStaffLogger] Player Logs: Logs folder already exists at path " + logsFolder.getAbsolutePath());
        }
        this.writerMap = new ConcurrentHashMap<>();
    }


    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!logCommands) {
            return;
        }

        Player player = event.getPlayer();
        String command = event.getMessage();
        log(player.getUniqueId(), "command", "Executed command: " + command);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        Player player = event.getPlayer();
        String message = event.getMessage();
        log(player.getUniqueId(), "chat", "Sent message: " + message);

    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // Check if the player moved to a different world
        if (from.getWorld() != to.getWorld()) {
            String location = String.format("%.2f, %.2f, %.2f", to.getX(), to.getY(), to.getZ());
            log(player.getUniqueId(), "move", "Changed world to " + to.getWorld().getName() + " at " + location);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        String itemName = droppedItem.getType().toString();
        String itemLore = "";

        if (droppedItem.hasItemMeta() && droppedItem.getItemMeta().hasLore()) {
            itemLore = String.join(", ", droppedItem.getItemMeta().getLore());
        }

        int itemAmount = droppedItem.getAmount();
        log(player.getUniqueId(), "drop", "Dropped item: " + "x" + itemAmount + " " + itemName + " with lore: " + itemLore);
    }


    @EventHandler
    public void onLeaveEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        log(player.getUniqueId(), "left", "Player Left The Server");
    }
    @EventHandler
    public void onJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        log(player.getUniqueId(), "join", "Player Joined The Server");
    }

    @EventHandler
    public void onDeathEvent(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String reason = event.getDeathMessage();
        log(player.getUniqueId(), "Died", "Player has died due to: " + reason);
    }


    public void log(UUID playerId, String action, String message) {
        String formattedMessage = formatMessage(message);
        String fileName = (playerId != null ? playerId.toString() : "server") + ".txt";
        BufferedWriter writer = getBufferedWriter(fileName);
        try {
            writer.write(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " | " + action + " | " + formattedMessage + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatMessage(String message) {
        return message.replace("|", "\\|");
    }

    private BufferedWriter getBufferedWriter(String fileName) {
        return writerMap.computeIfAbsent(UUID.fromString(fileName.substring(0, fileName.indexOf("."))), this::createBufferedWriter);
    }

    private BufferedWriter createBufferedWriter(UUID playerId) {
        File file = new File(dataFolder + File.separator + playerId + ".txt");
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw new IOException("[KushStaffLogger] Player Logs: Failed to create file: " + playerId + ".txt");
                }
                System.out.println("[KushStaffLogger] Player Logs: Created file: " + playerId + ".txt");
                return new BufferedWriter(new FileWriter(file, true));
            } else {
                System.out.println("[KushStaffLogger] Player Logs: File already exists: " + playerId + ".txt");
                return new BufferedWriter(new FileWriter(file, true));
            }
        } catch (IOException e) {
            System.err.println("[KushStaffLogger] Player Logs: Failed to create file: " + playerId + ".txt");
            e.printStackTrace();
        }
        return null;
    }


    public void reloadLogCommands(boolean logCommands) {
        this.logCommands = logCommands;
    }
}

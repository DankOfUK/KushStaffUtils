package me.dankofuk.utils;

import org.bukkit.ChatColor;

public class ColorUtils {
    public static final String WITH_DELIMITER = "((?<=%1$s)|(?=%1$s))";

    public static String translateColorCodes(String text) {
        String[] texts = text.split(String.format("((?<=%1$s)|(?=%1$s))", "&"));
        StringBuilder finalText = new StringBuilder();
        for (int i = 0; i < texts.length; i++) {
            if (texts[i].equalsIgnoreCase("&")) {
                i++;
                if (texts[i].charAt(0) == '#') {
                    finalText.append(ChatColor.valueOf(texts[i].substring(0, 7)) + texts[i].substring(7));
                } else {
                    finalText.append(ChatColor.translateAlternateColorCodes('&', "&" + texts[i]));
                }
            } else {
                finalText.append(texts[i]);
            }
        }
        return finalText.toString();
    }
}

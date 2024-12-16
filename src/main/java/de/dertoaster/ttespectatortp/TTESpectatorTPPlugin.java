package de.dertoaster.ttespectatortp;

import io.papermc.lib.PaperLib;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;


public class TTESpectatorTPPlugin extends JavaPlugin implements Listener {

    static class Permissions {

        static final String PREFIX = "ttespectatortp";

        public static final String CAN_SPECTATOR_TP = PREFIX + ".allowspectatortp";
        public static final String BYPASS = PREFIX + ".bypass";

    }

    static class Settings {

        public static boolean sendCommandEnabled = true;
        public static String commandToSend = "tpa %SELECTED_PLAYER%";

        static void load(FileConfiguration config) {
            sendCommandEnabled = config.getBoolean("send-command-enabled", true);
            commandToSend = config.getString("command-to-send", "tpa %SELECTED_PLAYER%");
        }

    }

    @Override
    public void onEnable() {
        PaperLib.suggestPaper(this);

        this.getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        Settings.load(this.getConfig());

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(
            priority = EventPriority.LOWEST,
            ignoreCancelled = true
    )
    final void onSpecatorTP(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) {
            return;
        }
        Player issuer = event.getPlayer();
        if (issuer.getGameMode() != GameMode.SPECTATOR) {
            return;
        }

        if (issuer.hasPermission(Permissions.BYPASS)) {
            return;
        }

        if (!issuer.hasPermission(Permissions.CAN_SPECTATOR_TP)) {
            event.setCancelled(true);
            return;
        }

        if (!Settings.sendCommandEnabled || Settings.commandToSend == null || Settings.commandToSend.isBlank()) {
            event.setCancelled(true);
        }

        Location target = event.getTo();

        List<Player> targetsOrdered = target.getWorld().getNearbyPlayers(target, 2).stream().sorted((a, b) -> {
            Location locA = a.getLocation();
            Location locB = b.getLocation();
            double distA = target.distance(locA);
            double distB = target.distance(locB);
            // ORder: Ascending
            if (distA == distB) {
                return 0;
            }
            else if (distA > distB) {
                return 1;
            } else {
                return -1;
            }
        }).collect(Collectors.toList());

        if (targetsOrdered.isEmpty()) {
            return;
        }

        Player targetPlayer = targetsOrdered.get(0);

        String commandToRun = Settings.commandToSend;
        commandToRun = commandToRun.replaceAll("%SELECTED_PLAYER%", targetPlayer.getName());
        commandToRun = commandToRun.replaceAll("%ISSUING_PLAYER%", issuer.getName());

        issuer.performCommand(commandToRun);
        event.setCancelled(true);
    }


}

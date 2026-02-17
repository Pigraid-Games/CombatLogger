package gg.pigraid.CombatLogger;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerCommandPreprocessEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.plugin.PluginBase;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CombatLogger - Prevents players from using certain commands while in combat
 * Ported to NukkitPetteriM1Edition
 */
public class CombatLogger extends PluginBase implements Listener {
    private static final Set<String> blockedCommands = new HashSet<>();
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final int COOLDOWN_TICKS = 500; // 25 seconds (500 ticks / 20 ticks per second)
    public static SimpleI18n i18n;

    @Override
    public void onLoad() {
        i18n = new SimpleI18n(this);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        // Load blocked commands from config
        List<String> configuredCommands = getConfig().getStringList("blocked-commands");
        configuredCommands.forEach(cmd -> registerCommand(cmd.toLowerCase()));
    }

    /**
     * Reload blocked commands from config
     */
    public void reloadBlockedCommands() {
        blockedCommands.clear();
        List<String> configuredCommands = getConfig().getStringList("blocked-commands");
        configuredCommands.forEach(cmd -> registerCommand(cmd.toLowerCase()));
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase().trim();
        String command = message.split(" ")[0].replace("/", "");

        if (!blockedCommands.contains(command)) {
            return;
        }

        Long lastHit = cooldowns.get(player.getUniqueId());
        if (lastHit != null) {
            long elapsedTicks = (System.currentTimeMillis() - lastHit) / 50;
            if (elapsedTicks < COOLDOWN_TICKS) {
                long remaining = (COOLDOWN_TICKS - elapsedTicks) / 20;
                player.sendMessage(i18n.tr(player.getLocale(), "combatLogger.cooldown.message", command, remaining));
                event.setCancelled(true);
            } else {
                cooldowns.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (!(entity instanceof Player)) {
            return;
        }
        Player damager = (Player) entity;

        entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player target = (Player) entity;

        // Don't track spectators
        if (damager.isSpectator() || target.isSpectator()) {
            return;
        }

        // Mark both players as in combat
        long now = System.currentTimeMillis();
        cooldowns.put(damager.getUniqueId(), now);
        cooldowns.put(target.getUniqueId(), now);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        cooldowns.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Only clean up combat state - item dropping on combat log is handled
        // by KitPvPEssentials' InventorySyncManager which also manages DB sync
        cooldowns.remove(event.getPlayer().getUniqueId());
    }

    private void registerCommand(String command) {
        blockedCommands.add(command.toLowerCase());
    }

    /**
     * Check if a player is currently in combat
     */
    public static boolean isInCombat(Player player) {
        Long last = cooldowns.get(player.getUniqueId());
        return last != null && ((System.currentTimeMillis() - last) / 50) < COOLDOWN_TICKS;
    }
}

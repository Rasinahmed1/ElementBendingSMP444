package com.elementbending;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

public class ElementBendingSMP extends JavaPlugin implements Listener {

    private final Map<UUID, String> bending = new HashMap<>();
    private final Map<UUID, Long> cooldown = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ElementBendingSMP Enabled!");
    }

    // ---------------- GUI ----------------
    private void openGUI(Player p) {

        Inventory gui = Bukkit.createInventory(null, 9, "§6Select Bending");

        gui.setItem(0, item(Material.BLAZE_POWDER, "§cFire"));
        gui.setItem(2, item(Material.WATER_BUCKET, "§9Water"));
        gui.setItem(4, item(Material.DIRT, "§2Earth"));
        gui.setItem(6, item(Material.FEATHER, "§fAir"));

        p.openInventory(gui);
    }

    private ItemStack item(Material mat, String name) {
        ItemStack i = new ItemStack(mat);
        ItemMeta meta = i.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            i.setItemMeta(meta);
        }
        return i;
    }

    // ---------------- OPEN GUI COMMAND (RIGHT CLICK ALSO) ----------------
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {

        if (e.getAction() == Action.RIGHT_CLICK_AIR ||
            e.getAction() == Action.RIGHT_CLICK_BLOCK) {

            openGUI(e.getPlayer());
            return;
        }

        if (e.getAction() != Action.LEFT_CLICK_AIR &&
            e.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player p = e.getPlayer();

        if (isCooldown(p)) return;

        String type = bending.getOrDefault(p.getUniqueId(), "AIR");

        switch (type) {

            case "FIRE" -> {
                p.launchProjectile(org.bukkit.entity.Fireball.class);
                setCooldown(p, 3000);
            }

            case "WATER" -> {
                p.getWorld().spawnParticle(Particle.WATER_SPLASH, p.getLocation(), 20);
                setCooldown(p, 2000);
            }

            case "EARTH" -> {
                p.getWorld().spawnFallingBlock(
                        p.getEyeLocation(),
                        Material.STONE.createBlockData()
                ).setVelocity(p.getLocation().getDirection().multiply(1.5));
                setCooldown(p, 2500);
            }

            case "AIR" -> {
                for (var ent : p.getNearbyEntities(4, 4, 4)) {
                    Vector v = ent.getLocation().toVector()
                            .subtract(p.getLocation().toVector())
                            .normalize()
                            .multiply(1.2);
                    ent.setVelocity(v);
                }
                setCooldown(p, 1500);
            }
        }
    }

    // ---------------- GUI CLICK ----------------
    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (e.getView() == null) return;
        if (!e.getView().getTitle().equals("§6Select Bending")) return;

        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getItemMeta() == null) return;

        Player p = (Player) e.getWhoClicked();

        String name = e.getCurrentItem().getItemMeta().getDisplayName();

        switch (name) {
            case "§cFire" -> bending.put(p.getUniqueId(), "FIRE");
            case "§9Water" -> bending.put(p.getUniqueId(), "WATER");
            case "§2Earth" -> bending.put(p.getUniqueId(), "EARTH");
            case "§fAir" -> bending.put(p.getUniqueId(), "AIR");
        }

        p.sendMessage("§aSelected: §e" + name);
        p.closeInventory();
    }

    // ---------------- COOLDOWN ----------------
    private boolean isCooldown(Player p) {
        return cooldown.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    private void setCooldown(Player p, long ms) {
        cooldown.put(p.getUniqueId(), System.currentTimeMillis() + ms);
    }
                    }

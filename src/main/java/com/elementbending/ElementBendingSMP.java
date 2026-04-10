package com.elementbending;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
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
        getLogger().info("ElementBendingSMP ENABLED SAFE MODE ✔");
    }

    // ---------------- GUI ----------------
    private void openGUI(Player p) {

        Inventory gui = Bukkit.createInventory(null, 9, "§6Select Bending");

        gui.setItem(0, createItem(Material.BLAZE_POWDER, "§cFire"));
        gui.setItem(2, createItem(Material.WATER_BUCKET, "§9Water"));
        gui.setItem(4, createItem(Material.DIRT, "§2Earth"));
        gui.setItem(6, createItem(Material.FEATHER, "§fAir"));

        p.openInventory(gui);
    }

    private ItemStack createItem(Material mat, String name) {

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }

        return item;
    }

    // ---------------- SAFE CLICK ----------------
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {

        Player p = e.getPlayer();

        // OPEN GUI (RIGHT CLICK ONLY)
        if (e.getAction() == Action.RIGHT_CLICK_AIR ||
            e.getAction() == Action.RIGHT_CLICK_BLOCK) {

            openGUI(p);
            return;
        }

        // LEFT CLICK = ABILITY
        if (e.getAction() != Action.LEFT_CLICK_AIR &&
            e.getAction() != Action.LEFT_CLICK_BLOCK) return;

        if (isCooldown(p)) return;

        String type = bending.getOrDefault(p.getUniqueId(), "AIR");

        switch (type) {

            case "FIRE" -> {
                if (p.isDead()) return;
                p.launchProjectile(Fireball.class);
                setCooldown(p, 3000);
            }

            case "WATER" -> {
                p.getWorld().spawnParticle(Particle.WATER_SPLASH, p.getLocation(), 15);
                setCooldown(p, 2000);
            }

            case "EARTH" -> {
                FallingBlock block = p.getWorld().spawnFallingBlock(
                        p.getEyeLocation(),
                        Material.STONE.createBlockData()
                );

                block.setVelocity(p.getLocation().getDirection().multiply(1.2));
                setCooldown(p, 2500);
            }

            case "AIR" -> {
                for (Entity ent : p.getNearbyEntities(4, 4, 4)) {

                    if (ent instanceof Player target && target.equals(p)) continue;

                    Vector v = ent.getLocation().toVector()
                            .subtract(p.getLocation().toVector())
                            .normalize()
                            .multiply(1.1);

                    ent.setVelocity(v);
                }

                setCooldown(p, 1500);
            }
        }
    }

    // ---------------- GUI CLICK SAFE ----------------
    @EventHandler
    public void onGUI(InventoryClickEvent e) {

        if (e.getView() == null) return;
        if (!e.getView().getTitle().equals("§6Select Bending")) return;

        e.setCancelled(true);

        if (e.getCurrentItem() == null) return;
        if (!e.getCurrentItem().hasItemMeta()) return;

        Player p = (Player) e.getWhoClicked();
        String name = e.getCurrentItem().getItemMeta().getDisplayName();

        if (name == null) return;

        switch (name) {
            case "§cFire" -> bending.put(p.getUniqueId(), "FIRE");
            case "§9Water" -> bending.put(p.getUniqueId(), "WATER");
            case "§2Earth" -> bending.put(p.getUniqueId(), "EARTH");
            case "§fAir" -> bending.put(p.getUniqueId(), "AIR");
            default -> {
                p.sendMessage("§cInvalid selection!");
                return;
            }
        }

        p.sendMessage("§aBending selected: §e" + name);
        p.closeInventory();
    }

    // ---------------- SAFE COMMANDS ----------------
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("bending")) {
            openGUI(p);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("mybending")) {
            String type = bending.getOrDefault(p.getUniqueId(), "NONE");
            p.sendMessage("§6Your bending: §e" + type);
            return true;
        }

        return false;
    }

    // ---------------- COOLDOWN SAFE ----------------
    private boolean isCooldown(Player p) {
        return cooldown.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    private void setCooldown(Player p, long ms) {
        cooldown.put(p.getUniqueId(), System.currentTimeMillis() + ms);
    }
    }

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

    // ================= DATA =================
    private final Map<UUID, String> ability = new HashMap<>();
    private final Map<UUID, Integer> level = new HashMap<>();
    private final Map<UUID, Integer> xp = new HashMap<>();
    private final Map<UUID, Boolean> avatar = new HashMap<>();
    private final Map<UUID, Long> cooldown = new HashMap<>();
    private final Map<UUID, Long> guiCD = new HashMap<>();

    // ================= ENABLE =================
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("🔥 ULTRA AVATAR BENDING LOADED");
    }

    // ================= GUI =================
    private void openGUI(Player p) {

        if (guiCD.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis()) return;

        guiCD.put(p.getUniqueId(), System.currentTimeMillis() + 1000);

        Inventory inv = Bukkit.createInventory(null, 9, "§6Avatar Bending");

        inv.setItem(0, item(Material.FIRE_CHARGE, "Fire Blast"));
        inv.setItem(1, item(Material.BLAZE_POWDER, "Fire Wave"));
        inv.setItem(3, item(Material.FEATHER, "Fly Mode"));
        inv.setItem(5, item(Material.WATER_BUCKET, "Water Shield"));
        inv.setItem(6, item(Material.DIRT, "Earth Wall"));
        inv.setItem(8, item(Material.NETHER_STAR, "Avatar State"));

        p.openInventory(inv);
    }

    private ItemStack item(Material m, String name) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f" + name);
            i.setItemMeta(meta);
        }
        return i;
    }

    // ================= COMMAND =================
    @EventHandler
    public void onCmd(PlayerCommandPreprocessEvent e) {
        if (e.getMessage().equalsIgnoreCase("/bending")) {
            e.setCancelled(true);
            openGUI(e.getPlayer());
        }
    }

    // ================= CLICK GUI =================
    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (e.getCurrentItem() == null || e.getCurrentItem().getItemMeta() == null) return;

        Player p = (Player) e.getWhoClicked();
        e.setCancelled(true);

        String name = e.getCurrentItem().getItemMeta().getDisplayName();

        switch (name) {

            case "§fFire Blast" -> ability.put(p.getUniqueId(), "Fire Blast");
            case "§fFire Wave" -> ability.put(p.getUniqueId(), "Fire Wave");
            case "§fWater Shield" -> ability.put(p.getUniqueId(), "Water Shield");
            case "§fEarth Wall" -> ability.put(p.getUniqueId(), "Earth Wall");
            case "§fFly Mode" -> ability.put(p.getUniqueId(), "Fly Mode");

            case "§fAvatar State" -> toggleAvatar(p);
        }

        p.sendMessage("§aSelected: §e" + name);
        p.closeInventory();
    }

    // ================= ABILITIES =================
    @EventHandler
    public void onUse(PlayerInteractEvent e) {

        Player p = e.getPlayer();

        if (e.getAction() != Action.RIGHT_CLICK_AIR &&
            e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (isCooldown(p)) return;

        String a = ability.getOrDefault(p.getUniqueId(), "NONE");
        boolean av = avatar.getOrDefault(p.getUniqueId(), false);

        switch (a) {

            // 🔥 FIRE
            case "Fire Blast" -> {
                Fireball fb = p.launchProjectile(Fireball.class);
                fb.setYield(av ? 3f : 1.5f);
                addXP(p, 10);
                setCooldown(p, 2500);
            }

            case "Fire Wave" -> {
                p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), av ? 80 : 30);
                addXP(p, 10);
                setCooldown(p, 2500);
            }

            // 💧 WATER
            case "Water Shield" -> {
                p.getWorld().spawnParticle(Particle.SPLASH, p.getLocation(), 25);
                p.setVelocity(new Vector(0, av ? 1 : 0.4, 0));
                addXP(p, 8);
                setCooldown(p, 2000);
            }

            // 🪨 EARTH
            case "Earth Wall" -> {
                p.getWorld().spawnParticle(Particle.BLOCK_CRACK, p.getLocation(), 30);
                addXP(p, 10);
                setCooldown(p, 3000);
            }

            // 💨 AIR
            case "Fly Mode" -> {

                if (av) {
                    p.setAllowFlight(true);
                    p.setFlying(true);
                    p.sendMessage("§bAvatar Flight ON");
                } else {
                    p.setAllowFlight(!p.getAllowFlight());
                    p.setFlying(p.getAllowFlight());
                }

                addXP(p, 5);
                setCooldown(p, 2000);
            }
        }

        // AIR PUSH always available if avatar
        if (a.equals("Air Push")) {
            for (Entity ent : p.getNearbyEntities(5, 5, 5)) {
                if (ent.equals(p)) continue;

                Vector v = ent.getLocation().toVector()
                        .subtract(p.getLocation().toVector())
                        .normalize()
                        .multiply(av ? 2 : 1.4);

                ent.setVelocity(v);
            }

            addXP(p, 10);
            setCooldown(p, 1500);
        }
    }

    // ================= AVATAR SYSTEM =================
    private void toggleAvatar(Player p) {

        UUID id = p.getUniqueId();

        if (level.getOrDefault(id, 1) < 10) {
            p.sendMessage("§cNeed level 10 to unlock Avatar State!");
            return;
        }

        boolean state = avatar.getOrDefault(id, false);
        avatar.put(id, !state);

        if (!state) {
            p.sendMessage("§bAVATAR STATE ON!");
            p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation(), 50);
        } else {
            p.sendMessage("§7Avatar OFF");
        }
    }

    // ================= XP SYSTEM =================
    private void addXP(Player p, int amount) {

        UUID id = p.getUniqueId();

        xp.put(id, xp.getOrDefault(id, 0) + amount);

        int lvl = level.getOrDefault(id, 1);

        if (xp.get(id) >= lvl * 100) {
            xp.put(id, 0);
            level.put(id, lvl + 1);

            p.sendMessage("§aLEVEL UP! §e" + (lvl + 1));

            if (lvl + 1 == 10) {
                p.sendMessage("§bYOU UNLOCKED AVATAR STATE!");
            }
        }
    }

    // ================= COOLDOWN =================
    private boolean isCooldown(Player p) {
        return cooldown.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    private void setCooldown(Player p, long ms) {
        cooldown.put(p.getUniqueId(), System.currentTimeMillis() + ms);
    }
                    }

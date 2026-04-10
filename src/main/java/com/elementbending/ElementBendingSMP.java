package com.elementbending;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

public class ElementBendingSMP extends JavaPlugin implements Listener {

    // ================= DATA =================
    private final Map<UUID, String> element = new HashMap<>();
    private final Map<UUID, String> ability = new HashMap<>();
    private final Map<UUID, Integer> level = new HashMap<>();
    private final Map<UUID, Integer> xp = new HashMap<>();
    private final Map<UUID, Boolean> avatar = new HashMap<>();
    private final Map<UUID, Long> cooldown = new HashMap<>();
    private final Map<UUID, Long> lastClick = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("🔥 GOD MODE BENDERS LOADED");
    }

    // ================= ITEM =================
    private ItemStack item(Material m, String name) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            i.setItemMeta(meta);
        }
        return i;
    }

    // ================= ELEMENT GUI =================
    private void openElementGUI(Player p) {

        Inventory inv = Bukkit.createInventory(null, 9, "§6Choose Element");

        inv.setItem(1, item(Material.BLAZE_POWDER, "§cFire"));
        inv.setItem(3, item(Material.WATER_BUCKET, "§9Water"));
        inv.setItem(5, item(Material.DIRT, "§2Earth"));
        inv.setItem(7, item(Material.FEATHER, "§fAir"));

        p.openInventory(inv);
    }

    // ================= ABILITY GUI =================
    private void openAbilityGUI(Player p) {

        Inventory inv = Bukkit.createInventory(null, 9, "§6Abilities");

        inv.setItem(0, item(Material.FIRE_CHARGE, "Fire Blast"));
        inv.setItem(1, item(Material.BLAZE_POWDER, "Fire Wave"));
        inv.setItem(3, item(Material.FEATHER, "Fly Mode"));
        inv.setItem(5, item(Material.WATER_BUCKET, "Water Shield"));
        inv.setItem(6, item(Material.DIRT, "Earth Wall"));
        inv.setItem(8, item(Material.NETHER_STAR, "Avatar State"));

        p.openInventory(inv);
    }

    // ================= COMMAND =================
    @EventHandler
    public void onCmd(PlayerCommandPreprocessEvent e) {

        Player p = e.getPlayer();

        if (e.getMessage().equalsIgnoreCase("/element")) {
            e.setCancelled(true);
            openElementGUI(p);
        }

        if (e.getMessage().equalsIgnoreCase("/bending")) {
            e.setCancelled(true);
            openAbilityGUI(p);
        }
    }

    // ================= GUI CLICK =================
    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;

        Player p = (Player) e.getWhoClicked();
        e.setCancelled(true);

        String title = e.getView().getTitle();
        String name = e.getCurrentItem().getItemMeta().getDisplayName();

        // ELEMENT
        if (title.equals("§6Choose Element")) {

            switch (e.getCurrentItem().getType()) {
                case BLAZE_POWDER -> element.put(p.getUniqueId(), "FIRE");
                case WATER_BUCKET -> element.put(p.getUniqueId(), "WATER");
                case DIRT -> element.put(p.getUniqueId(), "EARTH");
                case FEATHER -> element.put(p.getUniqueId(), "AIR");
            }

            p.sendMessage("§aElement Selected!");
            p.closeInventory();
            return;
        }

        // ABILITY
        switch (name) {
            case "Fire Blast", "Fire Wave", "Water Shield", "Earth Wall", "Fly Mode" ->
                    ability.put(p.getUniqueId(), name);

            case "Avatar State" -> toggleAvatar(p);
        }

        p.sendMessage("§aSelected: §e" + name);
        p.closeInventory();
    }

    // ================= USE ABILITY =================
    @EventHandler
    public void onUse(PlayerInteractEvent e) {

        Player p = e.getPlayer();

        if (e.getAction() != Action.RIGHT_CLICK_AIR &&
            e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (isCooldown(p)) return;

        // DOUBLE CLICK COMBO
        long now = System.currentTimeMillis();

        if (lastClick.containsKey(p.getUniqueId())) {
            if (now - lastClick.get(p.getUniqueId()) < 400) {

                if (element.getOrDefault(p.getUniqueId(), "").equals("FIRE")) {
                    p.getWorld().createExplosion(p.getLocation(), 2f);
                    p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 50);
                    setCooldown(p, 4000);
                    return;
                }
            }
        }

        lastClick.put(p.getUniqueId(), now);

        String a = ability.getOrDefault(p.getUniqueId(), "NONE");
        boolean av = avatar.getOrDefault(p.getUniqueId(), false);

        switch (a) {

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

            case "Water Shield" -> {
                p.getWorld().spawnParticle(Particle.WATER_SPLASH, p.getLocation(), 25);
                p.setVelocity(new Vector(0, av ? 1 : 0.4, 0));
                addXP(p, 8);
                setCooldown(p, 2000);
            }

            case "Earth Wall" -> {
                p.getWorld().spawnParticle(Particle.BLOCK_CRACK, p.getLocation(), 30);
                addXP(p, 10);
                setCooldown(p, 3000);
            }

            case "Fly Mode" -> {

                if (av) {
                    p.setAllowFlight(true);
                    p.setFlying(true);
                } else {
                    p.setAllowFlight(!p.getAllowFlight());
                    p.setFlying(p.getAllowFlight());
                }

                p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 15);

                addXP(p, 5);
                setCooldown(p, 2000);
            }
        }
    }

    // ================= SNEAK POWER =================
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {

        if (!e.isSneaking()) return;

        Player p = e.getPlayer();
        String el = element.getOrDefault(p.getUniqueId(), "NONE");

        switch (el) {

            case "AIR" -> {
                p.setVelocity(p.getLocation().getDirection().multiply(2));
                p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 20);
            }

            case "EARTH" -> {
                p.setVelocity(new Vector(0, 1, 0));
                p.getWorld().spawnParticle(Particle.BLOCK_CRACK, p.getLocation(), 20);
            }

            case "WATER" -> {
                p.setVelocity(new Vector(0, 0.5, 0));
                p.getWorld().spawnParticle(Particle.WATER_SPLASH, p.getLocation(), 20);
            }

            case "FIRE" -> {
                p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 30);
            }
        }
    }

    // ================= PASSIVE =================
    @EventHandler
    public void passive(PlayerMoveEvent e) {

        Player p = e.getPlayer();
        String el = element.getOrDefault(p.getUniqueId(), "NONE");

        switch (el) {
            case "AIR" -> p.setFallDistance(0);

            case "FIRE" -> p.setFireTicks(0);

            case "WATER" -> {
                if (p.getLocation().getBlock().getType() == Material.WATER) {
                    p.setVelocity(p.getVelocity().multiply(1.05));
                }
            }
        }
    }

    // ================= AVATAR =================
    private void toggleAvatar(Player p) {

        UUID id = p.getUniqueId();

        if (level.getOrDefault(id, 1) < 10) {
            p.sendMessage("§cNeed level 10!");
            return;
        }

        boolean state = avatar.getOrDefault(id, false);
        avatar.put(id, !state);

        if (!state) {
            p.sendMessage("§bAVATAR MODE ON!");
            p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation(), 50);
        } else {
            p.sendMessage("§7Avatar OFF");
        }
    }

    // ================= XP =================
    private void addXP(Player p, int amount) {

        UUID id = p.getUniqueId();

        xp.put(id, xp.getOrDefault(id, 0) + amount);

        int lvl = level.getOrDefault(id, 1);

        if (xp.get(id) >= lvl * 100) {
            xp.put(id, 0);
            level.put(id, lvl + 1);

            p.sendMessage("§aLEVEL UP! §e" + (lvl + 1));

            if (lvl + 1 == 10) {
                p.sendMessage("§bAvatar Unlocked!");
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

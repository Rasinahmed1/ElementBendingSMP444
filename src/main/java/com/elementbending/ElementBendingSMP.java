package com.elementbending;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;
import java.util.*;

public class ElementBendingSMP extends JavaPlugin implements Listener {

    private final Map<UUID, Long> cooldown = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    private boolean cooldown(Player p) {
        return cooldown.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    private void setCooldown(Player p, long ms) {
        cooldown.put(p.getUniqueId(), System.currentTimeMillis() + ms);
    }

    private String getElement(Player p) {
        int i = Math.abs(p.getUniqueId().hashCode() % 4);
        return switch (i) {
            case 0 -> "FIRE";
            case 1 -> "EARTH";
            case 2 -> "WATER";
            default -> "AIR";
        };
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {

        if (e.getAction() != Action.LEFT_CLICK_AIR &&
            e.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player p = e.getPlayer();

        if (cooldown(p)) return;

        switch (getElement(p)) {

            case "FIRE" -> {
                Fireball fb = p.launchProjectile(Fireball.class);
                fb.setYield(1.5f);
                setCooldown(p, 3000);
            }

            case "EARTH" -> {
                FallingBlock block = p.getWorld().spawnFallingBlock(
                        p.getEyeLocation(),
                        Material.STONE.createBlockData()
                );
                block.setVelocity(p.getLocation().getDirection().multiply(1.5));
                setCooldown(p, 2500);
            }

            case "WATER" -> {
                p.getWorld().spawnParticle(Particle.WATER_SPLASH, p.getLocation(), 30);
                setCooldown(p, 2000);
            }

            case "AIR" -> {
                for (Entity ent : p.getNearbyEntities(4, 4, 4)) {
                    Vector v = ent.getLocation().toVector()
                            .subtract(p.getLocation().toVector())
                            .normalize()
                            .multiply(1.5);
                    ent.setVelocity(v);
                }
                setCooldown(p, 1500);
            }
        }
    }
    }

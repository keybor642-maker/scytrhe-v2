package mc.mkay.scythe;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScytheListener implements Listener {

    private static final String OWNER = "mkaymc";

    private final ScythePlugin plugin;

    private final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long ABILITY_COOLDOWN_MS = 30000L;

    private final Map<UUID, Long> pullCooldowns = new HashMap<>();
    private static final long PULL_COOLDOWN_MS = 8000L;
    private static final double PULL_RANGE = 25.0;

    public ScytheListener(ScythePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.getName().equalsIgnoreCase(OWNER)) {
            return;
        }

        if (!ScytheItem.isScythe(player.getInventory().getItemInMainHand(), plugin)) {
            return;
        }

        event.setCancelled(true);

        if (player.isSneaking()) {
            handlePetalSnare(player);
        } else {
            handleBlossomBurst(player);
        }
    }

    private void handleBlossomBurst(Player player) {
        long now = System.currentTimeMillis();
        long last = abilityCooldowns.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < ABILITY_COOLDOWN_MS) {
            long remaining = (ABILITY_COOLDOWN_MS - (now - last)) / 1000L;
            player.sendActionBar(
                    Component.text("Blossom Burst recharging... " + remaining + "s", NamedTextColor.LIGHT_PURPLE)
                            .decoration(TextDecoration.ITALIC, false)
            );
            return;
        }

        abilityCooldowns.put(player.getUniqueId(), now);
        blossomBurst(player);
    }

    private void handlePetalSnare(Player player) {
        long now = System.currentTimeMillis();
        long last = pullCooldowns.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < PULL_COOLDOWN_MS) {
            long remaining = (PULL_COOLDOWN_MS - (now - last)) / 1000L;
            player.sendActionBar(
                    Component.text("Petal Snare recharging... " + remaining + "s", NamedTextColor.LIGHT_PURPLE)
                            .decoration(TextDecoration.ITALIC, false)
            );
            return;
        }

        Location eye = player.getEyeLocation();
        RayTraceResult result = player.getWorld().rayTraceEntities(
                eye,
                eye.getDirection(),
                PULL_RANGE,
                0.6,
                e -> e instanceof LivingEntity
                        && !e.equals(player)
                        && !(e instanceof Player p && p.getGameMode() == GameMode.CREATIVE)
        );

        if (result == null || result.getHitEntity() == null) {
            player.sendActionBar(
                    Component.text("No target in range.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            );
            return;
        }

        LivingEntity target = (LivingEntity) result.getHitEntity();

        pullCooldowns.put(player.getUniqueId(), now);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.2f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHERRY_LEAVES_PLACE, 1.0f, 1.4f);

        player.sendActionBar(
                Component.text("\u2766 Petal Snare!", NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.BOLD, true)
                        .decoration(TextDecoration.ITALIC, false)
        );

        new BukkitRunnable() {
            int tick = 0;
            final int duration = 12;

            @Override
            public void run() {
                if (tick >= duration || target.isDead() || !target.isValid()) {
                    cancel();
                    return;
                }

                World world = target.getWorld();
                Location from = target.getLocation().add(0, target.getHeight() / 2.0, 0);
                Location to = player.getEyeLocation();

                Vector path = to.toVector().subtract(from.toVector());
                double distance = path.length();

                if (distance > 0.6) {
                    Vector step = path.clone().normalize();

                    int points = (int) Math.max(1, distance * 2);
                    for (int i = 0; i <= points; i++) {
                        double frac = (double) i / points;
                        Location particleLoc = from.clone().add(step.clone().multiply(distance * frac));

                        world.spawnParticle(Particle.CHERRY_LEAVES, particleLoc, 1, 0.05, 0.05, 0.05, 0.0);

                        if (i % 2 == 0) {
                            world.spawnParticle(Particle.WITCH, particleLoc, 1, 0.02, 0.02, 0.02, 0.0);
                        }

                        world.spawnParticle(Particle.CRIT, particleLoc, 0, step.getX(), step.getY(), step.getZ(), 0.0);
                    }

                    Vector pull = step.multiply(Math.min(0.9, distance * 0.4 + 0.2));
                    pull.setY(Math.max(pull.getY(), 0.1));
                    target.setVelocity(pull);
                } else {
                    target.setVelocity(new Vector(0, 0.05, 0));
                    cancel();
                    return;
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void blossomBurst(Player player) {
        Location origin = player.getLocation();
        World world = player.getWorld();

        double radius = 5.0;
        double damage = 6.0;
        double knockbackStrength = 1.4;

        world.playSound(origin, Sound.BLOCK_CHERRY_LEAVES_BREAK, 1.5f, 0.8f);
        world.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
        world.playSound(origin, Sound.ITEM_TOTEM_USE, 0.6f, 1.8f);

        for (Entity entity : world.getNearbyEntities(origin, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (entity.equals(player)) {
                continue;
            }
            if (entity instanceof Player target && target.getGameMode() == GameMode.CREATIVE) {
                continue;
            }

            Vector knockback = entity.getLocation().toVector()
                    .subtract(origin.toVector())
                    .normalize()
                    .multiply(knockbackStrength);
            knockback.setY(0.35);

            living.damage(damage, player);
            entity.setVelocity(knockback);
            living.setFireTicks(60);

            world.spawnParticle(Particle.CHERRY_LEAVES, entity.getLocation().add(0, 1, 0), 25, 0.4, 0.4, 0.4, 0.05);
        }

        new BukkitRunnable() {
            int tick = 0;
            final double maxRadius = 5.5;

            @Override
            public void run() {
                if (tick > 15) {
                    cancel();
                    return;
                }

                double currentRadius = 0.36666666666666664 * tick;
                int points = 32;

                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI / points) * i;
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;

                    Location particleLoc = origin.clone().add(x, 0.2, z);
                    world.spawnParticle(Particle.CHERRY_LEAVES, particleLoc, 3, 0.1, 0.2, 0.1, 0.02);

                    if (tick % 2 == 0) {
                        world.spawnParticle(Particle.WITCH, particleLoc, 1, 0.05, 0.1, 0.05, 0.0);
                    }
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        player.sendActionBar(
                Component.text("\u2766 Blossom Burst!", NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.BOLD, true)
                        .decoration(TextDecoration.ITALIC, false)
        );

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                t++;
                if (t > 20) {
                    cancel();
                    return;
                }
                world.spawnParticle(Particle.CHERRY_LEAVES, player.getLocation().add(0, 1, 0), 8, 0.6, 0.8, 0.6, 0.04);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onScytheAttack(EntityDamageByEntityEvent event) {
        Player player;
        if (event.getDamager() instanceof Player p) {
            player = p;
        } else {
            return;
        }

        if (!ScytheItem.isScythe(player.getInventory().getItemInMainHand(), plugin)) {
            return;
        }

        if (event.getEntity() instanceof LivingEntity) {
            Location loc = event.getEntity().getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc, 12, 0.3, 0.3, 0.3, 0.05);
            player.getWorld().spawnParticle(Particle.WITCH, loc, 5, 0.2, 0.2, 0.2, 0.02);
        }
    }

    @EventHandler
    public void onNonOwnerHold(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equalsIgnoreCase(OWNER)) {
            return;
        }

        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (!ScytheItem.isScythe(item, plugin)) {
            return;
        }

        event.setCancelled(true);

        player.sendMessage(
                Component.text("\u2766", NamedTextColor.LIGHT_PURPLE)
                        .append(((TextComponent) Component.text("This relic is not yours to wield.", NamedTextColor.DARK_PURPLE))
                                .decoration(TextDecoration.ITALIC, true))
        );

        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.5f);
    }
}

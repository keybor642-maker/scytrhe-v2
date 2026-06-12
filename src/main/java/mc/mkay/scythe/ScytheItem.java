package mc.mkay.scythe;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ScytheItem {

    public static final int CUSTOM_MODEL_DATA = 100001;
    public static final String NBT_KEY = "mkay_scythe";

    public static ItemStack build(Plugin plugin) {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(
                ((TextComponent) Component.text("\u2766", NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.ITALIC, false))
                        .append(
                                ((TextComponent) Component.text("Bloom's Scythe", NamedTextColor.DARK_PURPLE)
                                        .decoration(TextDecoration.BOLD, true)
                                        .decoration(TextDecoration.ITALIC, false))
                                        .append(Component.text(" \u2766", NamedTextColor.LIGHT_PURPLE)
                                                .decoration(TextDecoration.ITALIC, false))
                        )
        );

        meta.lore(List.of(
                Component.empty(),
                ((TextComponent) Component.text("  The relic chose only one.", NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.ITALIC, true))
                        .append(Component.text("  Petals fall where others don't.", NamedTextColor.LIGHT_PURPLE)
                                .decoration(TextDecoration.ITALIC, true)),
                Component.empty(),
                ((TextComponent) Component.text(" \u2766", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false))
                        .append(((TextComponent) Component.text("Right-Click", NamedTextColor.LIGHT_PURPLE)
                                .decoration(TextDecoration.ITALIC, false)
                                .decoration(TextDecoration.BOLD, true))
                                .append(Component.text(" \u2766 Blossom Burst", NamedTextColor.DARK_PURPLE)
                                        .decoration(TextDecoration.ITALIC, false))),
                ((TextComponent) Component.text(" \u2766", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false))
                        .append(((TextComponent) Component.text("Sneak + Right-Click", NamedTextColor.LIGHT_PURPLE)
                                .decoration(TextDecoration.ITALIC, false)
                                .decoration(TextDecoration.BOLD, true))
                                .append(Component.text(" \u2766 Petal Snare", NamedTextColor.DARK_PURPLE)
                                        .decoration(TextDecoration.ITALIC, false))),
                Component.empty(),
                ((TextComponent) Component.text(" Bound to:", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false))
                        .append(Component.text("mkaymc", NamedTextColor.LIGHT_PURPLE)
                                .decoration(TextDecoration.ITALIC, false)
                                .decoration(TextDecoration.BOLD, true))
        ));

        meta.setCustomModelData(Integer.valueOf(CUSTOM_MODEL_DATA));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, NBT_KEY),
                PersistentDataType.BOOLEAN,
                Boolean.valueOf(true)
        );

        item.setItemMeta(meta);

        item.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
        item.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
        item.addUnsafeEnchantment(Enchantment.SWEEPING_EDGE, 3);
        item.addUnsafeEnchantment(Enchantment.LOOTING, 3);
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
        item.addUnsafeEnchantment(Enchantment.MENDING, 1);
        item.addUnsafeEnchantment(Enchantment.BREACH, 4);

        return item;
    }

    public static boolean isScythe(ItemStack item, Plugin plugin) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, NBT_KEY), PersistentDataType.BOOLEAN);
    }
}

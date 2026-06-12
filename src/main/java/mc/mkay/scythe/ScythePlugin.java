package mc.mkay.scythe;

import org.bukkit.plugin.java.JavaPlugin;

public class ScythePlugin extends JavaPlugin {

    private static ScythePlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(new ScytheListener(this), this);
        getCommand("givescythe").setExecutor(new GiveScytheCommand(this));
        getLogger().info("\u00a7d[Scythe] Cherry Blossom Relic awakened for mkaymc.");
    }

    @Override
    public void onDisable() {
        getLogger().info("\u00a7d[Scythe] Relic sealed.");
    }

    public static ScythePlugin getInstance() {
        return instance;
    }
}

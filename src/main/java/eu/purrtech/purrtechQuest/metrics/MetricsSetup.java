package eu.purrtech.purrtechQuest.metrics;

import eu.purrtech.purrtechQuest.config.PluginConfig;
import eu.purrtech.purrtechQuest.service.QuestService;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * bStats metrics. {@code PLUGIN_ID} is a placeholder — bStats ties collected metrics to a specific numeric
 * plugin id issued when you register a plugin at <a href="https://bstats.org">bstats.org</a>. PurrtechQuest
 * hasn't been published there, so this id doesn't correspond to a claimed dashboard. Server owners get an
 * opt-out for free either way (bStats' own shared {@code plugins/bStats/config.yml}), so nothing extra is
 * needed here for that — the only thing to fix before a real release is this id.
 */
public final class MetricsSetup {

    private static final int PLUGIN_ID = -1;

    private MetricsSetup() {
    }

    public static void init(JavaPlugin plugin, QuestService questService, PluginConfig config) {
        if (PLUGIN_ID <= 0) {
            plugin.getLogger().warning("bStats metrics not started: PLUGIN_ID is still a placeholder "
                    + "(register the plugin at https://bstats.org and update MetricsSetup.PLUGIN_ID).");
            return;
        }
        Metrics metrics = new Metrics(plugin, PLUGIN_ID);
        metrics.addCustomChart(new SimplePie("storage_type", () -> config.storageType().name()));
        metrics.addCustomChart(new SimplePie("tracking_display", () -> config.trackingDisplay().name()));
        metrics.addCustomChart(new SingleLineChart("quests_defined", () -> questService.allQuests().size()));
    }
}

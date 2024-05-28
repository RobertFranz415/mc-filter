package mctest.chatfiltermc.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class ConfigUtil {
    @SuppressWarnings("FieldMayBeFinal")
    private File file;
    @SuppressWarnings("FieldMayBeFinal")
    private FileConfiguration config;

    public ConfigUtil(Plugin plugin, String path) {this(plugin.getDataFolder().getAbsolutePath() + "/" + path);}

    public ConfigUtil(String path) {
        this.file = new File(path);
        this.config = YamlConfiguration.loadConfiguration(this.file);
    }

    public boolean save() {
        try {
            this.config.save(this.file);
            return true;
        } catch(Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            return false;
        }
    }

    public File getFile() {
        return this.file;
    }
    public FileConfiguration getConfig() {
        return this.config;
    }
}

package pl.jms.auth.bungee.util;

import org.yaml.snakeyaml.Yaml;
import pl.jms.auth.core.config.AuthConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

public final class BungeeConfigLoader {

    private BungeeConfigLoader() {
    }

    @SuppressWarnings("unchecked")
    public static AuthConfig load(File dataFolder, net.md_5.bungee.api.plugin.Plugin plugin) throws IOException {
        if (!dataFolder.exists()) {
            Files.createDirectories(dataFolder.toPath());
        }
        File file = new File(dataFolder, "config.yml");
        if (!file.exists()) {
            try (InputStream in = plugin.getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                } else {
                    throw new IOException("Missing default config.yml in jar");
                }
            }
        }
        Yaml yaml = new Yaml();
        try (FileInputStream in = new FileInputStream(file)) {
            Map<String, Object> root = yaml.load(in);
            if (root == null) {
                root = Map.of();
            }
            return AuthConfig.fromYamlRoot(root);
        }
    }
}

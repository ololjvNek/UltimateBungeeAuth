package pl.jms.auth.velocity;

import org.yaml.snakeyaml.Yaml;
import pl.jms.auth.core.config.AuthConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class VelocityConfigLoader {

    private VelocityConfigLoader() {
    }

    @SuppressWarnings("unchecked")
    public static AuthConfig load(Path dataDirectory, Class<?> clazz) throws IOException {
        Files.createDirectories(dataDirectory);
        Path file = dataDirectory.resolve("config.yml");
        if (!Files.exists(file)) {
            try (InputStream in = clazz.getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, file);
                } else {
                    throw new IOException("Default config missing in jar");
                }
            }
        }
        Yaml yaml = new Yaml();
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(file)) {
            root = yaml.load(in);
        }
        if (root == null) {
            root = Map.of();
        }
        return AuthConfig.fromYamlRoot(root);
    }
}

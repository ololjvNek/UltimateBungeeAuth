package pl.jms.auth.core.integrations;

import pl.jms.auth.core.config.AuthConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WebhookNotifier implements AutoCloseable {

    private final AuthConfig.Integrations integrations;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public WebhookNotifier(AuthConfig.Integrations integrations) {
        this.integrations = integrations;
    }

    public void firstJoin(String player, String serverLabel) {
        if (!integrations.firstJoinEnabled()) {
            return;
        }
        String body = toDiscordJson(expand(integrations.firstJoinFormat(), player, serverLabel));
        post(integrations.firstJoinWebhookUrl(), body);
    }

    public void loginSuccess(String player, String serverLabel) {
        if (!integrations.loginEnabled()) {
            return;
        }
        String body = toDiscordJson(expand(integrations.loginFormat(), player, serverLabel));
        post(integrations.loginWebhookUrl(), body);
    }

    private void post(String url, String jsonBody) {
        if (url == null || url.isBlank()) {
            return;
        }
        executor.execute(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(8))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                        .build();
                httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) {
            }
        });
    }

    private static String expand(String format, String player, String serverLabel) {
        String time = Instant.now().toString();
        return format.replace("{PLAYER}", player).replace("{TIME}", time).replace("{DATE}", time).replace("{SERVER}", serverLabel);
    }

    private static String toDiscordJson(String content) {
        String esc = content.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"content\":\"" + esc + "\"}";
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}

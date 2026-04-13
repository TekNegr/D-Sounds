package musicapp.integration.oauth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class LocalCallbackServer implements AutoCloseable {
    private final HttpServer server;
    private final ArrayBlockingQueue<Map<String, String>> queue = new ArrayBlockingQueue<>(1);

    public LocalCallbackServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        this.server.createContext("/callback", this::handleCallback);
        this.server.setExecutor(null);
    }

    public void start() {
        server.start();
    }

    public Map<String, String> awaitParams(long timeoutSeconds) throws OAuthException {
        try {
            Map<String, String> params = queue.poll(timeoutSeconds, TimeUnit.SECONDS);
            if (params == null) {
                throw new OAuthException("Le callback OAuth n'a pas été reçu à temps.");
            }
            return params;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OAuthException("Attente du callback interrompue.", e);
        }
    }

    private void handleCallback(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        Map<String, String> params = parseQuery(uri.getRawQuery());
        queue.offer(params);

        String html = """
                <html>
                  <head><meta charset="utf-8"><title>MusicApp OAuth</title></head>
                  <body style="font-family: Arial, sans-serif; padding: 24px;">
                    <h2>Connexion réussie</h2>
                    <p>Vous pouvez revenir à MusicApp.</p>
                  </body>
                </html>
                """;
        byte[] payload = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, payload.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(payload);
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) {
            return map;
        }
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1
                    ? java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    : "";
            map.put(key, value);
        }
        return map;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}

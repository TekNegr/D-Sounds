package integration.oauth;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

public final class GitHubOAuthService {
    private static final String AUTH_ENDPOINT = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_ENDPOINT = "https://github.com/login/oauth/access_token";
    private static final String USER_ENDPOINT = "https://api.github.com/user";
    private static final String USER_EMAILS_ENDPOINT = "https://api.github.com/user/emails";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final OAuthDesktopConfig config;

    public GitHubOAuthService(OAuthDesktopConfig config) {
        this.config = config;
    }

    public LinkedOAuthAccount authenticate() throws OAuthException {
        validateConfig();

        String state = PkceUtils.randomState();
        String verifier = PkceUtils.generateCodeVerifier();
        String challenge = PkceUtils.createCodeChallenge(verifier);

        String authUrl = AUTH_ENDPOINT
                + "?client_id=" + enc(config.getClientId())
                + "&redirect_uri=" + enc(config.getRedirectUri())
                + "&scope=" + enc("read:user user:email")
                + "&state=" + enc(state)
                + "&code_challenge=" + enc(challenge)
                + "&code_challenge_method=S256";

        try (LocalCallbackServer callbackServer = new LocalCallbackServer(config.getLocalPort())) {
            callbackServer.start();
            OAuthBrowserFlow.openSystemBrowser(authUrl);
            Map<String, String> params = callbackServer.awaitParams(180);

            if (params.containsKey("error")) {
                throw new OAuthException("GitHub a refusé la connexion : " + params.get("error"));
            }
            if (!state.equals(params.get("state"))) {
                throw new OAuthException("État OAuth invalide pour GitHub.");
            }

            String code = params.get("code");
            OAuthToken token = exchangeCodeForToken(code, verifier);
            OAuthProfile profile = fetchProfile(token);
            return new LinkedOAuthAccount(profile, token, Instant.now());
        } catch (OAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuthException("Erreur OAuth GitHub.", e);
        }
    }

    private OAuthToken exchangeCodeForToken(String code, String verifier) throws Exception {
        String form = "client_id=" + enc(config.getClientId())
                + "&client_secret=" + enc(config.getClientSecret())
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(config.getRedirectUri())
                + "&state="
                + "&code_verifier=" + enc(verifier);

        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new OAuthException("Échec de l'échange de token GitHub : " + response.body());
        }

        String json = response.body();
        return new OAuthToken(
                JsonUtils.extractString(json, "access_token"),
                "",
                JsonUtils.extractString(json, "token_type"),
                0L,
                JsonUtils.extractString(json, "scope")
        );
    }

    private OAuthProfile fetchProfile(OAuthToken token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(USER_ENDPOINT))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token.getAccessToken())
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new OAuthException("Impossible de récupérer le profil GitHub : " + response.body());
        }

        String email = fetchPrimaryEmail(token);
        String json = response.body();
        return new OAuthProfile(
                OAuthProvider.GITHUB,
                String.valueOf(JsonUtils.extractLong(json, "id")),
                JsonUtils.extractString(json, "name").isBlank() ? JsonUtils.extractString(json, "login") : JsonUtils.extractString(json, "name"),
                email,
                JsonUtils.extractString(json, "avatar_url"),
                JsonUtils.extractString(json, "html_url")
        );
    }

    private String fetchPrimaryEmail(OAuthToken token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(USER_EMAILS_ENDPOINT))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token.getAccessToken())
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            return "";
        }

        String json = response.body();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"email\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private void validateConfig() throws OAuthException {
        if (config.getClientId().isBlank() || config.getClientSecret().isBlank()) {
            throw new OAuthException("Configuration GitHub manquante : clientId/clientSecret.");
        }
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

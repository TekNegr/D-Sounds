package musicapp.integration.oauth;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

public final class GoogleOAuthService {
    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://openidconnect.googleapis.com/v1/userinfo";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final OAuthDesktopConfig config;

    public GoogleOAuthService(OAuthDesktopConfig config) {
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
                + "&response_type=code"
                + "&scope=" + enc("openid email profile")
                + "&code_challenge=" + enc(challenge)
                + "&code_challenge_method=S256"
                + "&state=" + enc(state)
                + "&access_type=offline"
                + "&prompt=consent";

        try (LocalCallbackServer callbackServer = new LocalCallbackServer(config.getLocalPort())) {
            callbackServer.start();
            OAuthBrowserFlow.openSystemBrowser(authUrl);
            Map<String, String> params = callbackServer.awaitParams(180);

            if (params.containsKey("error")) {
                throw new OAuthException("Google a refusé la connexion : " + params.get("error"));
            }
            if (!state.equals(params.get("state"))) {
                throw new OAuthException("État OAuth invalide pour Google.");
            }

            String code = params.get("code");
            OAuthToken token = exchangeCodeForToken(code, verifier);
            OAuthProfile profile = fetchProfile(token);
            return new LinkedOAuthAccount(profile, token, Instant.now());
        } catch (OAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuthException("Erreur OAuth Google.", e);
        }
    }

    private OAuthToken exchangeCodeForToken(String code, String verifier) throws Exception {
        String form = "client_id=" + enc(config.getClientId())
                + "&client_secret=" + enc(config.getClientSecret())
                + "&code=" + enc(code)
                + "&grant_type=authorization_code"
                + "&redirect_uri=" + enc(config.getRedirectUri())
                + "&code_verifier=" + enc(verifier);

        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new OAuthException("Échec de l'échange de token Google : " + response.body());
        }

        String json = response.body();
        return new OAuthToken(
                JsonUtils.extractString(json, "access_token"),
                JsonUtils.extractString(json, "refresh_token"),
                JsonUtils.extractString(json, "token_type"),
                JsonUtils.extractLong(json, "expires_in"),
                JsonUtils.extractString(json, "scope")
        );
    }

    private OAuthProfile fetchProfile(OAuthToken token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(USERINFO_ENDPOINT))
                .header("Authorization", "Bearer " + token.getAccessToken())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new OAuthException("Impossible de récupérer le profil Google : " + response.body());
        }

        String json = response.body();
        return new OAuthProfile(
                OAuthProvider.GOOGLE,
                JsonUtils.extractString(json, "sub"),
                JsonUtils.extractString(json, "name"),
                JsonUtils.extractString(json, "email"),
                JsonUtils.extractString(json, "picture"),
                ""
        );
    }

    private void validateConfig() throws OAuthException {
        if (config.getClientId().isBlank() || config.getClientSecret().isBlank()) {
            throw new OAuthException("Configuration Google manquante : clientId/clientSecret.");
        }
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

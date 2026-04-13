package integration.oauth;

public final class OAuthDesktopConfig {
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final int localPort;

    public OAuthDesktopConfig(String clientId, String clientSecret, String redirectUri, int localPort) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.localPort = localPort;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public int getLocalPort() {
        return localPort;
    }

    public static OAuthDesktopConfig fromSystemProperties(String prefix, int defaultPort) {
        String clientId = System.getProperty(prefix + ".clientId", "");
        String clientSecret = System.getProperty(prefix + ".clientSecret", "");
        String redirectUri = System.getProperty(prefix + ".redirectUri", "http://127.0.0.1:" + defaultPort + "/callback");
        return new OAuthDesktopConfig(clientId, clientSecret, redirectUri, defaultPort);
    }
}

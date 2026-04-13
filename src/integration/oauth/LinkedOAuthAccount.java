package integration.oauth;

import java.time.Instant;

public final class LinkedOAuthAccount {
    private final OAuthProfile profile;
    private final OAuthToken token;
    private final Instant linkedAt;

    public LinkedOAuthAccount(OAuthProfile profile, OAuthToken token, Instant linkedAt) {
        this.profile = profile;
        this.token = token;
        this.linkedAt = linkedAt;
    }

    public OAuthProfile getProfile() {
        return profile;
    }

    public OAuthToken getToken() {
        return token;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }
}

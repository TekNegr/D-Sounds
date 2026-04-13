package musicapp.integration.oauth;

public final class OAuthProfile {
    private final OAuthProvider provider;
    private final String providerUserId;
    private final String displayName;
    private final String email;
    private final String avatarUrl;
    private final String profileUrl;

    public OAuthProfile(OAuthProvider provider, String providerUserId, String displayName,
                        String email, String avatarUrl, String profileUrl) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.displayName = displayName;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.profileUrl = profileUrl;
    }

    public OAuthProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    @Override
    public String toString() {
        return provider.getDisplayName() + " / " + displayName + " / " + email;
    }
}

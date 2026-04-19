package dsounds.controllers;

/**
 * Placeholder controller for Google OAuth.
 *
 * Google and GitHub OAuth are intentionally disabled for now because the imported
 * implementation is currently unstable. Keep this class as the integration point
 * if you decide to wire OAuth later.
 */
public class GoogleAuthController {

    /**
     * TODO (future):
     * 1) Copy OAuth helper classes from src_laksman/integration/oauth into dsounds namespace.
     * 2) Add clientId/clientSecret/redirectUri/localPort configuration source.
     * 3) Instantiate GoogleOAuthService with desktop config and run browser flow.
     * 4) On success, call LocalAuthController.loginOrCreateFromOAuth(profileName, profileEmail).
     * 5) Add UI error handling for OAuth consent refusal and timeout.
     */
    public void loginWithGoogle() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Google OAuth is disabled in this build.");
    }
}

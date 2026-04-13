package musicapp.integration.oauth;

import java.awt.Desktop;
import java.net.URI;

public final class OAuthBrowserFlow {
    private OAuthBrowserFlow() {}

    public static void openSystemBrowser(String url) throws OAuthException {
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new OAuthException("Le bureau graphique n'est pas disponible pour ouvrir le navigateur.");
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            throw new OAuthException("Impossible d'ouvrir le navigateur pour OAuth.", e);
        }
    }
}

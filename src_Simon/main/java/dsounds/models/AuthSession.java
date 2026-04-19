package dsounds.models;

public class AuthSession {

    private User currentUser;

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void open(User user) {
        this.currentUser = user;
    }

    public void close() {
        this.currentUser = null;
    }

    public boolean hasRole(UserRole role) {
        return isAuthenticated() && currentUser.getRole() == role;
    }
}

module dsounds {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires transitive javafx.graphics;

    opens dsounds to javafx.fxml;
    opens dsounds.controllers to javafx.fxml;
    opens dsounds.models to javafx.fxml;
    exports dsounds;
    exports dsounds.controllers;
    exports dsounds.models;
    exports dsounds.repositories;
    exports dsounds.security;
}

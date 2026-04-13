module musicapp.auth {
    requires javafx.controls;
    requires java.desktop;
    requires java.net.http;
    requires jdk.httpserver;
    exports musicapp.app;
    exports musicapp.controller;
    exports musicapp.model;
    exports musicapp.persistence;
    exports musicapp.view;
    exports musicapp.view.javafx;
}

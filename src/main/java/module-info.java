module dsounds {
    requires javafx.controls; 
    requires javafx.fxml;

    opens dsounds to javafx.fxml;
    exports dsounds;
}

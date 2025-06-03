module com.pans.konrad.apka {
    requires javafx.controls;
    requires javafx.fxml;

    // Wymagane do HttpClient (java.net.http) i JSON parser (org.json)
    requires java.net.http;
    requires org.json;

    opens com.pans.konrad.apka.ui to javafx.fxml;
    opens com.pans.konrad.apka.model to javafx.base;

    exports com.pans.konrad.apka.ui;
    exports com.pans.konrad.apka.model;
    exports com.pans.konrad.apka.logic;
}

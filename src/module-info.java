module cmsc137.project {
	requires javafx.base;
	requires javafx.graphics;
	requires javafx.controls;
	requires javafx.media;
	
	requires java.desktop;
	
	// Duplicate this line for every package in the project
	opens controller to javafx.graphics;
    opens main to javafx.graphics;
    opens model to javafx.graphics;
    opens utils to javafx.graphics;
    opens view to javafx.graphics;
    opens client to javafx.graphics;
    opens server to javafx.graphics;
}

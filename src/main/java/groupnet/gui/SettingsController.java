package groupnet.gui;

import groupnet.util.Settings;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.shape.Rectangle;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.SimplePolygon2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
public class SettingsController extends Settings {

    public Map<Object, Object> globalMap = new HashMap<>();

    public static List<Point2D> debugPoints = new ArrayList<>();
    public static List<Node> debugNodes = new ArrayList<>();

    private static final int BBOX_SIZE = 50000;

    public static Polygon2D geomBBox = new SimplePolygon2D(new math.geom2d.Point2D(-BBOX_SIZE, -BBOX_SIZE),
            new math.geom2d.Point2D(BBOX_SIZE, -BBOX_SIZE),
            new math.geom2d.Point2D(BBOX_SIZE, BBOX_SIZE),
            new math.geom2d.Point2D(-BBOX_SIZE, BBOX_SIZE));

    public static Rectangle fxBBox = createBBox();

    private static Rectangle createBBox() {
        Rectangle r = new Rectangle(BBOX_SIZE * 2, BBOX_SIZE * 2);
        r.setTranslateX(-BBOX_SIZE);
        r.setTranslateY(-BBOX_SIZE);
        return r;
    }

    public static final double NODE_SIZE = 40.0;

    @FXML
    private CheckBox cbParallel;

    @Override
    public boolean isParallel() {
        return cbParallel.isSelected();
    }

    @FXML
    private CheckBox cbShowControls;

    public boolean showControls() {
        return cbShowControls.isSelected();
    }

    public BooleanProperty showControlsProperty() {
        return cbShowControls.selectedProperty();
    }

    @FXML
    private TextField fieldSmoothFactor;

    @Override
    public int smoothFactor() {
        return Integer.parseInt(fieldSmoothFactor.getText());
    }

    @FXML
    private TextField fieldStrokeSizeFactor;

    public double strokeSizeFactor() {
        return Double.parseDouble(fieldStrokeSizeFactor.getText());
    }

    @FXML
    private TextField fieldGraphSizeFactor;

    public double graphSizeFactor() {
        return Double.parseDouble(fieldGraphSizeFactor.getText());
    }

    @FXML
    private TextField fieldMEDSize;

    public double getMEDSize() {
        return Double.parseDouble(fieldMEDSize.getText());
    }

    @FXML
    private CheckBox cbShowMED;

    @Override
    public boolean showMED() {
        return cbShowMED.isSelected();
    }

    @FXML
    private TextField fieldNodeDistance;

    public double nodeDistance() {
        return Double.parseDouble(fieldNodeDistance.getText());
    }
}

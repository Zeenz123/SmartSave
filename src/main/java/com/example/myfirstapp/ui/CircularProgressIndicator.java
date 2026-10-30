package com.example.myfirstapp.ui;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Custom Circular Progress Indicator mirroring Android circular_progress_bar.xml.
 * Uses a Canvas to guarantee perfect concentric alignment, zero layout bounding-box displacement,
 * and standard clockwise progress starting from 12 o'clock (top).
 */
public class CircularProgressIndicator extends StackPane {
    private final Canvas canvas;
    private final double radius;
    private final double strokeWidth;
    private int percentage;
    private String displayText;

    public CircularProgressIndicator(double radius, int percentage, String displayText) {
        this.radius = radius;
        this.percentage = Math.max(0, Math.min(100, percentage));
        this.displayText = displayText;
        this.strokeWidth = radius * 0.22;

        double size = (radius + strokeWidth) * 2;
        this.canvas = new Canvas(size, size);
        setPrefSize(size, size);
        setMinSize(size, size);
        setMaxSize(size, size);

        getChildren().add(canvas);
        draw();
    }

    public void setProgress(int percentage, String displayText) {
        this.percentage = Math.max(0, Math.min(100, percentage));
        this.displayText = displayText;
        draw();
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);

        double cx = w / 2.0;
        double cy = h / 2.0;

        // 1. Background Track Ring (#E0E0E0)
        gc.setLineWidth(strokeWidth);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setStroke(Color.web("#E0E0E0"));
        gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // 2. Colored Progress Ring starting at 12 o'clock (90 deg in JavaFX) sweeping clockwise
        if (percentage > 0) {
            LinearGradient gradient = new LinearGradient(
                    0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#4CAF50")),
                    new Stop(1, Color.web("#00BCD4"))
            );
            gc.setStroke(gradient);
            gc.setLineWidth(strokeWidth);
            // In JavaFX GraphicsContext, 90 deg is 12 o'clock (straight up).
            // Negative extent sweeps clockwise from 12 o'clock through 3 o'clock, 6 o'clock, etc.
            double extent = -percentage * 3.6;
            gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, extent, ArcType.OPEN);
        }

        // 3. Center Text
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFill(Color.web("#00BCD4"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, radius * 0.44));
        String text = displayText != null ? displayText : (percentage + "%");
        gc.fillText(text, cx, cy);
    }
}

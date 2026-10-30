package com.example.myfirstapp.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * Renders exact vector drawables from the original Android application into JavaFX nodes.
 */
public class IconHelper {

    public static Node createSaveMoneyLogo(double scale) {
        Group group = new Group();

        // Foundation Steps
        SVGPath step1 = new SVGPath();
        step1.setContent("M 15,80 L 85,80 L 85,85 L 15,85 Z");
        step1.setFill(Color.web("#37474F"));

        SVGPath step2 = new SVGPath();
        step2.setContent("M 18,75 L 82,75 L 82,80 L 18,80 Z");
        step2.setFill(Color.web("#455A64"));

        // Pillars
        SVGPath col1 = new SVGPath();
        col1.setContent("M 23,45 L 29,45 L 29,75 L 23,75 Z");
        col1.setFill(Color.web("#00BCD4"));

        SVGPath col2 = new SVGPath();
        col2.setContent("M 39,45 L 45,45 L 45,75 L 39,75 Z");
        col2.setFill(Color.web("#00ACC1"));

        SVGPath col3 = new SVGPath();
        col3.setContent("M 55,45 L 61,45 L 61,75 L 55,75 Z");
        col3.setFill(Color.web("#00ACC1"));

        SVGPath col4 = new SVGPath();
        col4.setContent("M 71,45 L 77,45 L 77,75 L 71,75 Z");
        col4.setFill(Color.web("#00BCD4"));

        // Capitals & Bases
        SVGPath caps = new SVGPath();
        caps.setContent("M 21,43 L 31,43 L 31,45 L 21,45 Z M 37,43 L 47,43 L 47,45 L 37,45 Z M 53,43 L 63,43 L 63,45 L 53,45 Z M 69,43 L 79,43 L 79,45 L 69,45 Z");
        caps.setFill(Color.web("#26C6DA"));

        // Door
        SVGPath door = new SVGPath();
        door.setContent("M 45,58 L 55,58 L 55,75 L 45,75 Z");
        door.setFill(Color.web("#263238"));

        // Beam
        SVGPath beam = new SVGPath();
        beam.setContent("M 18,37 L 82,37 L 82,43 L 18,43 Z");
        beam.setFill(Color.web("#37474F"));

        // Roof
        SVGPath roof1 = new SVGPath();
        roof1.setContent("M 15,37 L 50,15 L 85,37 Z");
        roof1.setFill(Color.web("#0097A7"));

        SVGPath roof2 = new SVGPath();
        roof2.setContent("M 20,35 L 50,18 L 80,35 Z");
        roof2.setFill(Color.web("#00BCD4"));

        // Emblem
        SVGPath emblem = new SVGPath();
        emblem.setContent("M 50,22 A 6,6 0 1,0 50,34 A 6,6 0 1,0 50,22 Z");
        emblem.setFill(Color.web("#FFD700"));

        group.getChildren().addAll(step1, step2, col1, col2, col3, col4, caps, door, beam, roof1, roof2, emblem);
        group.setScaleX(scale);
        group.setScaleY(scale);
        return group;
    }

    public static SVGPath createIcon(String pathData, Color color, double scale) {
        SVGPath path = new SVGPath();
        path.setContent(pathData);
        path.setFill(color);
        path.setScaleX(scale);
        path.setScaleY(scale);
        return path;
    }

    // Exact paths from Android XML resources
    public static Node getAddCashIcon(Color color, double scale) {
        return createIcon("M21,18v1c0,1.1 -0.9,2 -2,2H5c-1.11,0 -2,-0.9 -2,-2V5c0,-1.1 0.89,-2 2,-2h14c1.1,0 2,0.9 2,2v1h-9c-1.11,0 -2,0.9 -2,2v8c0,1.1 0.89,2 2,2h9zm-9,-2h10V8H12v8zm4,-2.5c-0.83,0 -1.5,-0.67 -1.5,-1.5s0.67,-1.5 1.5,-1.5 1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5z M19,10h-2v2h-2v-2h-2V8h2V6h2v2h2v2z", color, scale);
    }

    public static Node getPayIcon(Color color, double scale) {
        return createIcon("M20,4H4C2.89,4 2,4.89 2,6v12c0,1.1 0.89,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.11 -0.9,-2 -2,-2zm0,14H4V12h16v6zm0,-10H4V6h16v2zm-12,7h4v2H8v-2z", color, scale);
    }

    public static Node getRequestPaymentIcon(Color color, double scale) {
        return createIcon("M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z", color, scale);
    }

    public static Node getAddGuardianIcon(Color color, double scale) {
        return createIcon("M12,1L3,5v6c0,5.55 3.84,10.74 9,12 5.16,-1.26 9,-6.45 9,-12V5l-9,-4zm0,4c1.66,0 3,1.34 3,3s-1.34,3 -3,3 -3,-1.34 -3,-3 1.34,-3 3,-3zm0,14.3c-2.5,0 -4.71,-1.28 -6,-3.22 0.03,-1.99 4,-3.08 6,-3.08 1.99,0 5.97,1.09 6,3.08 -1.29,1.94 -3.5,3.22 -6,3.22z", color, scale);
    }

    public static Node getWithdrawalIcon(Color color, double scale) {
        return createIcon("M4,10h16v2H4zm0,-4h16v2H4zm0,8h16v2H4zm-2,6h20v-2H2v2zm2,-16h16V2H4v2z", color, scale);
    }

    public static Node getConnectIcon(Color color, double scale) {
        return createIcon("M3.9,12c0,-1.71 1.39,-3.1 3.1,-3.1h4V7H7c-2.76,0 -5,2.24 -5,5s2.24,5 5,5h4v-1.9H7c-1.71,0 -3.1,-1.39 -3.1,-3.1zM8,13h8v-2H8v2zm9,-6h-4v1.9h4c1.71,0 3.1,1.39 3.1,3.1s-1.39,3.1 -3.1,3.1h-4V17h4c2.76,0 5,-2.24 5,-5s-2.24,-5 -5,-5z", color, scale);
    }

    public static Node getEarnReleaseIcon(Color color, double scale) {
        return createIcon("M16,6l2.29,2.29 -4.88,4.88 -4,-4L2,16.59 3.41,18l6,-6 4,4 6.3,-6.29L22,12V6h-6z", color, scale);
    }

    public static Node getWatchAdsIcon(Color color, double scale) {
        return createIcon("M21,3H3c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h5v2h8v-2h5c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zm0,14H3V5h18v12zM10,8v8l6,-4 -6,-4z", color, scale);
    }

    public static Node getReferIcon(Color color, double scale) {
        return createIcon("M18,16.08c-0.76,0 -1.44,0.3 -1.96,0.77L8.91,12.7c0.05,-0.23 0.09,-0.47 0.09,-0.7 0,-0.23 -0.04,-0.47 -0.09,-0.7l7.05,-4.11c0.54,0.5 1.25,0.81 2.04,0.81 1.66,0 3,-1.34 3,-3s-1.34,-3 -3,-3 -3,1.34 -3,3c0,0.23 0.04,0.47 0.09,0.7L8.04,9.81C7.5,9.31 6.79,9 6,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3c0.79,0 1.5,-0.31 2.04,-0.81l7.12,4.16c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.61 1.31,2.92 2.92,2.92 1.61,0 2.92,-1.31 2.92,-2.92s-1.31,-2.92 -2.92,-2.92z", color, scale);
    }

    public static Node getBackArrowIcon(Color color, double scale) {
        return createIcon("M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z", color, scale);
    }

    public static Node getAiSparkleIcon(Color color, double scale) {
        return createIcon("M19,9l1.25,-2.75L23,5l-2.75,-1.25L19,1l-1.25,2.75L15,5l2.75,1.25L19,9z M9,22l2.5,-5.5L17,14l-5.5,-2.5L9,6l-2.5,5.5L1,14l5.5,2.5L9,22z", color, scale);
    }

    public static Node getRefreshIcon(Color color, double scale) {
        return createIcon("M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -7.99,8s3.57,8 7.99,8c3.73,0 6.84,-2.55 7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z", color, scale);
    }
}

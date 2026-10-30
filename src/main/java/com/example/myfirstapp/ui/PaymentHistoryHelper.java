package com.example.myfirstapp.ui;

import com.example.myfirstapp.dao.TransactionDao;
import com.example.myfirstapp.dao.UserDao;
import com.example.myfirstapp.model.Transaction;
import com.example.myfirstapp.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reusable UI component and helper for rendering real-time payment and transfer history.
 * Used in both PayView and ConnectView.
 */
public final class PaymentHistoryHelper {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM, hh:mm a");
    private static final String CARD_STYLE = "-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-border-radius: 20px; " +
            "-fx-border-color: #B2EBF2; -fx-padding: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0.05, 0, 3);";

    private PaymentHistoryHelper() {
    }

    /**
     * Builds a self-contained Payment History card.
     */
    public static VBox buildHistoryCard(int currentUserId, Runnable onRefresh) {
        VBox card = new VBox(10);
        card.setStyle(CARD_STYLE);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconCircle = new StackPane(IconHelper.getPayIcon(Color.web("#00838F"), 0.8));
        iconCircle.setStyle("-fx-background-color: #E0F7FA; -fx-background-radius: 16px; " +
                "-fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px;");

        VBox titleBox = new VBox(1);
        Label title = new Label("Payment & Transfer History");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #00838F;");
        Label subtitle = new Label("Real-time log of sent and received P2P transfers");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #78909C;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(iconCircle, titleBox, spacer);
        if (onRefresh != null) {
            Button refreshBtn = new Button();
            refreshBtn.setGraphic(IconHelper.getRefreshIcon(Color.web("#00BCD4"), 0.8));
            refreshBtn.setStyle("-fx-background-color: #F5F7F8; -fx-background-radius: 14px; -fx-border-color: #CFD8DC; " +
                    "-fx-border-radius: 14px; -fx-cursor: hand; -fx-min-width: 28px; -fx-min-height: 28px; -fx-padding: 0;");
            refreshBtn.setOnAction(e -> onRefresh.run());
            header.getChildren().add(refreshBtn);
        }

        VBox itemsBox = new VBox(8);
        populateHistoryBox(itemsBox, currentUserId, 15);

        card.getChildren().addAll(header, itemsBox);
        return card;
    }

    /**
     * Populates the target container with formatted transaction rows.
     */
    public static void populateHistoryBox(VBox container, int currentUserId, int limit) {
        container.getChildren().clear();

        TransactionDao txDao = new TransactionDao();
        UserDao userDao = new UserDao();
        List<Transaction> all = txDao.findByUserId(currentUserId);

        if (all.isEmpty()) {
            VBox emptyBox = new VBox(6);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(16, 8, 16, 8));
            Label emptyIcon = new Label("💸");
            emptyIcon.setStyle("-fx-font-size: 22px;");
            Label emptyLabel = new Label("No payment transactions yet.");
            emptyLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #546E7A;");
            Label emptySub = new Label("Sent and received peer-to-peer transfers will appear here.");
            emptySub.setStyle("-fx-font-size: 11px; -fx-text-fill: #90A4AE; -fx-text-alignment: center;");
            emptyBox.getChildren().addAll(emptyIcon, emptyLabel, emptySub);
            container.getChildren().add(emptyBox);
            return;
        }

        Map<Integer, User> userCache = new HashMap<>();
        int count = 0;

        for (Transaction t : all) {
            if (count++ >= limit) break;
            container.getChildren().add(buildTransactionRow(t, currentUserId, userDao, userCache));
        }
    }

    private static HBox buildTransactionRow(Transaction t, int currentUserId, UserDao userDao, Map<Integer, User> userCache) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #F8FDFF; -fx-background-radius: 14px; -fx-border-color: #E0F2F1; " +
                "-fx-border-radius: 14px; -fx-padding: 9 12;");

        boolean isSender = (t.getSenderId() != null && t.getSenderId() == currentUserId);
        boolean isRecipient = (t.getRecipientId() != null && t.getRecipientId() == currentUserId);

        // Icon indicator
        StackPane iconBadge = new StackPane();
        Label iconLabel = new Label();
        iconLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        String primaryText;
        String amountText;
        String amountColor;

        if (t.getType() == Transaction.Type.TRANSFER) {
            if (isSender) {
                // Outgoing transfer
                iconBadge.setStyle("-fx-background-color: #FFEBEE; -fx-background-radius: 16px; " +
                        "-fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px;");
                iconLabel.setText("↗");
                iconLabel.setStyle("-fx-text-fill: #C62828; -fx-font-size: 14px; -fx-font-weight: bold;");

                User counterparty = resolveUser(t.getRecipientId(), userDao, userCache);
                String who = counterparty != null ? counterparty.getFullName() + " (@" + counterparty.getUsername() + ")"
                        : (t.getRecipientId() != null ? "User #" + t.getRecipientId() : "Recipient");
                primaryText = "To: " + who;
                amountText = String.format(Locale.getDefault(), "-₹%.2f", t.getAmount());
                amountColor = "#C62828";
            } else {
                // Incoming transfer
                iconBadge.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 16px; " +
                        "-fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px;");
                iconLabel.setText("↙");
                iconLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 14px; -fx-font-weight: bold;");

                User counterparty = resolveUser(t.getSenderId(), userDao, userCache);
                String who = counterparty != null ? counterparty.getFullName() + " (@" + counterparty.getUsername() + ")"
                        : (t.getSenderId() != null ? "User #" + t.getSenderId() : "Sender");
                primaryText = "From: " + who;
                amountText = String.format(Locale.getDefault(), "+₹%.2f", t.getAmount());
                amountColor = "#2E7D32";
            }
        } else if (t.getType() == Transaction.Type.DEPOSIT) {
            iconBadge.setStyle("-fx-background-color: #E0F7FA; -fx-background-radius: 16px; " +
                    "-fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px;");
            iconLabel.setText("💰");
            primaryText = "Cash Deposit";
            amountText = String.format(Locale.getDefault(), "+₹%.2f", t.getAmount());
            amountColor = "#00838F";
        } else if (t.getType() == Transaction.Type.WITHDRAWAL) {
            iconBadge.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 16px; " +
                    "-fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px;");
            iconLabel.setText("🏦");
            primaryText = "Withdrawal";
            amountText = String.format(Locale.getDefault(), "-₹%.2f", t.getAmount());
            amountColor = "#E65100";
        } else {
            iconBadge.setStyle("-fx-background-color: #ECEFF1; -fx-background-radius: 16px; " +
                    "-fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px;");
            iconLabel.setText("⚡");
            primaryText = t.getType().name().replace('_', ' ');
            amountText = String.format(Locale.getDefault(), "₹%.2f", t.getAmount());
            amountColor = "#455A64";
        }
        iconBadge.getChildren().add(iconLabel);

        // Center description and timestamp
        VBox details = new VBox(2);
        Label titleLabel = new Label(primaryText);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #263238;");
        titleLabel.setWrapText(true);

        String dateStr = t.getCreatedAt() != null ? t.getCreatedAt().format(DATE_FMT) : "";
        String desc = t.getDescription() != null && !t.getDescription().isBlank() ? t.getDescription() : "";
        String sub = dateStr + (!desc.isEmpty() ? " • " + desc : "");
        Label subLabel = new Label(sub);
        subLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #78909C;");
        subLabel.setWrapText(true);
        details.getChildren().addAll(titleLabel, subLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Right side: amount and status pill
        VBox rightBox = new VBox(2);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        Label amtLabel = new Label(amountText);
        amtLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + amountColor + ";");

        Label statusPill = new Label(t.getStatus() == Transaction.Status.SUCCESS ? "✓ Success" : t.getStatus().name());
        if (t.getStatus() == Transaction.Status.SUCCESS) {
            statusPill.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-font-size: 10px; " +
                    "-fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 8px;");
        } else {
            statusPill.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-font-size: 10px; " +
                    "-fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 8px;");
        }
        rightBox.getChildren().addAll(amtLabel, statusPill);

        row.getChildren().addAll(iconBadge, details, spacer, rightBox);
        return row;
    }

    private static User resolveUser(Integer id, UserDao userDao, Map<Integer, User> cache) {
        if (id == null) return null;
        if (cache.containsKey(id)) return cache.get(id);
        User u = userDao.findById(id).orElse(null);
        cache.put(id, u);
        return u;
    }
}

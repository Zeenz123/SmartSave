package com.example.myfirstapp.ui;

import com.example.myfirstapp.algo.Trie;
import com.example.myfirstapp.dao.DatabaseManager;
import com.example.myfirstapp.dao.PaymentRequestDao;
import com.example.myfirstapp.model.User;
import com.example.myfirstapp.service.AuthService;
import com.example.myfirstapp.service.ConnectService;
import com.example.myfirstapp.service.WalletService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;
import java.util.Locale;

/**
 * Connect page: shows your own SmartSave ID, finds other registered users (from any device sharing the
 * database), connects them as friend/guardian, and handles app-to-app payments and payment requests.
 * Incoming requests are polled every few seconds so a payment asked on one phone appears on the other.
 */
public class ConnectView extends VBox {
    private static final String CARD = "-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-border-radius: 20px; " +
            "-fx-border-color: #B2EBF2; -fx-padding: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0.05, 0, 3);";

    private final AuthService authService = AuthService.getInstance();
    private final ConnectService connectService = new ConnectService();

    private final TextField searchField = new TextField();
    private final TextField amountField = new TextField();
    private final ListView<User> usersListView = new ListView<>();
    private final VBox incomingBox = new VBox(8);
    private final VBox outgoingBox = new VBox(6);
    private final VBox historyBox = new VBox(8);
    private final Label incomingTitle = sectionTitle("Incoming Payment Requests");
    private final Label balanceLabel = new Label();
    private final Label statusLabel = new Label();
    private final HBox statusBanner = new HBox(statusLabel);
    private final Trie userTrie = new Trie();
    private Timeline poller;

    public ConnectView() {
        setSpacing(12);
        setPadding(new Insets(14, 16, 20, 16));
        setStyle("-fx-background-color: #E0F7FA;");

        User me = authService.getCurrentUser();

        // Top bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button();
        backBtn.setGraphic(IconHelper.getBackArrowIcon(Color.web("#263238"), 1.2));
        backBtn.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-padding: 8px; -fx-cursor: hand;");
        backBtn.setOnAction(e -> ScreenManager.getInstance().goBack());
        Label title = new Label("Connect & Pay");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #263238;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button refreshBtn = new Button();
        refreshBtn.setGraphic(IconHelper.getRefreshIcon(Color.web("#00BCD4"), 0.95));
        refreshBtn.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-min-width: 36px; -fx-min-height: 36px; " +
                "-fx-max-width: 36px; -fx-max-height: 36px; -fx-padding: 0; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> refreshAll());
        topBar.getChildren().addAll(backBtn, title, spacer, refreshBtn);

        // Status banner (top, hidden until needed)
        statusLabel.setWrapText(true);
        statusBanner.setPadding(new Insets(10, 16, 10, 16));
        statusBanner.setVisible(false);
        statusBanner.setManaged(false);

        VBox content = new VBox(14);
        content.getChildren().addAll(
                buildIdentityCard(me), buildIncomingCard(), buildFindCard(me), buildOutgoingCard(), buildHistoryCard());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-hbar-policy: never;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(topBar, statusBanner, scroll);

        if (me != null) {
            for (User u : connectService.searchUsers("", me.getId())) {
                userTrie.insert(u.getUsername());
                userTrie.insert(u.getFullName());
            }
            AutoComplete.attach(searchField, userTrie);
        }

        refreshAll();

        // Live polling so requests/payments from the other device show up automatically
        poller = new Timeline(new KeyFrame(Duration.seconds(4), e -> refreshLive()));
        poller.setCycleCount(Timeline.INDEFINITE);
        poller.play();
        sceneProperty().addListener((obs, old, now) -> {
            if (now == null && poller != null) poller.stop();
        });
    }

    // ---------- cards ----------

    private VBox buildIdentityCard(User me) {
        VBox card = new VBox(8);
        card.setStyle(CARD);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        String initial = me != null ? me.getFullName().substring(0, 1).toUpperCase(Locale.ROOT) : "?";
        Label avatar = new Label(initial);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px; " +
                "-fx-background-radius: 24px; -fx-min-width: 46px; -fx-min-height: 46px; -fx-max-width: 46px; -fx-max-height: 46px;");
        VBox names = new VBox(2);
        Label name = new Label(me != null ? me.getFullName() : "Not signed in");
        name.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #263238;");
        Label id = new Label(me != null ? "ID: @" + me.getUsername() +
                (me.getPhone() != null && !me.getPhone().isBlank() ? "   Phone: " + me.getPhone() : "") : "");
        id.setStyle("-fx-font-size: 12px; -fx-text-fill: #546E7A;");
        names.getChildren().addAll(name, id);
        row.getChildren().addAll(avatar, names);

        balanceLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #00838F;");

        DatabaseManager db = DatabaseManager.getInstance();
        Label net = new Label();
        net.setWrapText(true);
        if (db.isMySql()) {
            net.setText("● Online: shared MySQL database at " + hostOf(db.getJdbcUrl()) +
                    ". Other devices signed in to the same server can see and pay you.");
            net.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2E7D32; -fx-background-color: #E8F5E9; " +
                    "-fx-background-radius: 12px; -fx-padding: 6 10;");
        } else {
            net.setText("● Offline: local database only. Another phone cannot see this account until both apps use the same MySQL server.");
            net.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #E65100; -fx-background-color: #FFF3E0; " +
                    "-fx-background-radius: 12px; -fx-padding: 6 10;");
        }
        card.getChildren().addAll(row, balanceLabel, net);
        return card;
    }

    private VBox buildIncomingCard() {
        VBox card = new VBox(8);
        card.setStyle(CARD);
        card.getChildren().addAll(incomingTitle, incomingBox);
        return card;
    }

    private VBox buildFindCard(User me) {
        VBox card = new VBox(10);
        card.setStyle(CARD);

        Label find = sectionTitle("Find People & Pay");

        searchField.setPromptText("Search username, name or email...");
        styleInput(searchField);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        Button searchBtn = pill("Search", "#00BCD4", "white");
        searchBtn.setOnAction(e -> doSearch());
        searchField.setOnAction(e -> doSearch());
        HBox searchRow = new HBox(8, searchField, searchBtn);

        usersListView.setPrefHeight(180);
        usersListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getFullName() + "   (@" + u.getUsername() + ")");
                setStyle("-fx-text-fill: #263238; -fx-font-size: 13px; -fx-font-weight: bold;");
            }
        });
        usersListView.setPlaceholder(new Label("No other registered users found yet."));

        Button friendBtn = pill("Close Friend", "#00BCD4", "white");
        friendBtn.setOnAction(e -> handleConnect("Close Friend"));
        Button parentBtn = pill("Parent Guardian", "#4CAF50", "white");
        parentBtn.setOnAction(e -> handleConnect("Parent"));
        HBox relRow = new HBox(8, friendBtn, parentBtn);

        Label payLabel = new Label("Amount (₹) for selected person:");
        payLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #263238;");
        amountField.setPromptText("0.00");
        styleInput(amountField);
        Button sendBtn = pill("Send Money", "#00838F", "white");
        sendBtn.setOnAction(e -> handleSend());
        Button requestBtn = pill("Request Money", "#FF9800", "white");
        requestBtn.setOnAction(e -> handleRequest());
        HBox payRow = new HBox(8, sendBtn, requestBtn);

        card.getChildren().addAll(find, searchRow, usersListView, relRow, payLabel, amountField, payRow);
        return card;
    }

    private VBox buildOutgoingCard() {
        VBox card = new VBox(8);
        card.setStyle(CARD);
        card.getChildren().addAll(sectionTitle("Requests You Sent"), outgoingBox);
        return card;
    }

    private VBox buildHistoryCard() {
        VBox card = new VBox(8);
        card.setStyle(CARD);
        card.getChildren().addAll(sectionTitle("Payment & Transfer History"), historyBox);
        return card;
    }

    // ---------- data ----------

    private void refreshAll() {
        doSearch();
        refreshLive();
    }

    /** Cheap refresh of the parts that change when the other device acts. */
    private void refreshLive() {
        User me = authService.getCurrentUser();
        if (me == null) return;

        WalletService ws = new WalletService();
        double avail = ws.getWallet(me.getId()).getAvailableBalance();
        balanceLabel.setText(String.format(Locale.getDefault(), "Available balance: ₹%.2f", avail));

        incomingBox.getChildren().clear();
        List<ConnectService.IncomingRequest> incoming = connectService.getIncomingRequests(me.getId());
        incomingTitle.setText("Incoming Payment Requests (" + incoming.size() + ")");
        if (incoming.isEmpty()) {
            incomingBox.getChildren().add(hint("No one is asking you for money right now."));
        }
        for (ConnectService.IncomingRequest ir : incoming) {
            incomingBox.getChildren().add(incomingRow(ir, me));
        }

        outgoingBox.getChildren().clear();
        List<PaymentRequestDao.Record> sent = connectService.getOutgoingRequests(me.getId());
        if (sent.isEmpty()) outgoingBox.getChildren().add(hint("You have not requested money from anyone."));
        int shown = 0;
        for (PaymentRequestDao.Record r : sent) {
            if (shown++ >= 6) break;
            Label l = new Label(String.format(Locale.getDefault(), "₹%.2f from %s  -  %s", r.amount, r.targetContact, r.status));
            String color = "PAID".equals(r.status) ? "#2E7D32" : "DECLINED".equals(r.status) ? "#C62828" : "#E65100";
            l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
            outgoingBox.getChildren().add(l);
        }

        PaymentHistoryHelper.populateHistoryBox(historyBox, me.getId(), 10);
    }

    private HBox incomingRow(ConnectService.IncomingRequest ir, User me) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #F8FDFF; -fx-background-radius: 14px; -fx-border-color: #E0F2F1; -fx-border-radius: 14px; -fx-padding: 8 12;");
        Label who = new Label(String.format(Locale.getDefault(), "%s asks ₹%.2f", ir.requester.getFullName(), ir.record.amount));
        who.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #263238;");
        who.setWrapText(true);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button pay = pill("Pay", "#00BCD4", "white");
        pay.setOnAction(e -> {
            WalletService.TransferResult r = connectService.payRequest(ir.record.id, me);
            toast(r.isSuccess() ? String.format(Locale.getDefault(), "Paid ₹%.2f to %s", ir.record.amount, ir.requester.getFullName())
                    : r.getMessage(), r.isSuccess());
            refreshLive();
        });
        Button decline = pill("Decline", "#FFEBEE", "#C62828");
        decline.setOnAction(e -> {
            connectService.declineRequest(ir.record.id, me);
            toast("Request declined.", true);
            refreshLive();
        });
        row.getChildren().addAll(who, sp, pay, decline);
        return row;
    }

    private void doSearch() {
        User user = authService.getCurrentUser();
        int currentUserId = user != null ? user.getId() : 0;
        usersListView.getItems().setAll(connectService.searchUsers(searchField.getText(), currentUserId));
    }

    private void handleConnect(String relationship) {
        User selected = usersListView.getSelectionModel().getSelectedItem();
        User current = authService.getCurrentUser();
        if (selected == null || current == null) {
            toast("Select a person from the list first.", false);
            return;
        }
        boolean ok = connectService.connectUser(current.getId(), selected.getId(), relationship);
        toast(ok ? "Connected with " + selected.getFullName() + " as " + relationship + "!"
                : "Already connected with " + selected.getFullName(), ok);
    }

    private Double readAmount() {
        try {
            double v = Double.parseDouble(amountField.getText().trim());
            if (v > 0) return v;
        } catch (NumberFormatException ignored) {
        }
        toast("Enter a valid positive amount.", false);
        return null;
    }

    private void handleSend() {
        User selected = usersListView.getSelectionModel().getSelectedItem();
        User me = authService.getCurrentUser();
        if (selected == null || me == null) {
            toast("Select a person to pay first.", false);
            return;
        }
        Double amt = readAmount();
        if (amt == null) return;
        WalletService.TransferResult r = connectService.sendMoney(me, selected.getUsername(), amt);
        toast(r.isSuccess() ? String.format(Locale.getDefault(), "Sent ₹%.2f to %s", amt, selected.getFullName()) : r.getMessage(), r.isSuccess());
        if (r.isSuccess()) amountField.clear();
        refreshLive();
    }

    private void handleRequest() {
        User selected = usersListView.getSelectionModel().getSelectedItem();
        User me = authService.getCurrentUser();
        if (selected == null || me == null) {
            toast("Select a person to request from first.", false);
            return;
        }
        Double amt = readAmount();
        if (amt == null) return;
        boolean ok = connectService.sendPaymentRequest(me.getId(), selected.getUsername(), amt);
        toast(ok ? String.format(Locale.getDefault(), "Requested ₹%.2f from %s", amt, selected.getFullName()) : "Could not send request.", ok);
        if (ok) amountField.clear();
        refreshLive();
    }

    // ---------- helpers ----------

    private void toast(String message, boolean ok) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (ok ? "#2E7D32" : "#C62828") + ";");
        statusBanner.setStyle("-fx-background-radius: 16px; -fx-border-radius: 16px; -fx-background-color: " +
                (ok ? "#E8F5E9; -fx-border-color: #A5D6A7;" : "#FFEBEE; -fx-border-color: #FFCDD2;"));
        statusBanner.setVisible(true);
        statusBanner.setManaged(true);
    }

    private static String hostOf(String url) {
        String s = url.replaceFirst("^jdbc:mysql://", "");
        int slash = s.indexOf('/');
        return slash > 0 ? s.substring(0, slash) : s;
    }

    private static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #00838F;");
        return l;
    }

    private static Label hint(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #90A4AE; -fx-font-style: italic;");
        return l;
    }

    private static Button pill(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-font-weight: bold; " +
                "-fx-background-radius: 14px; -fx-cursor: hand; -fx-padding: 7 16; -fx-font-size: 12px;");
        return b;
    }

    private static void styleInput(TextField f) {
        f.setStyle("-fx-font-size: 14px; -fx-text-fill: #263238; -fx-background-color: #F5F7F8; -fx-border-color: #CFD8DC; " +
                "-fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 9px 14px;");
    }
}

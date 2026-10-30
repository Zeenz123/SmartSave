package com.example.myfirstapp.ui;

import com.example.myfirstapp.algo.Trie;
import com.example.myfirstapp.dao.GuardianDao;
import com.example.myfirstapp.dao.UserDao;
import com.example.myfirstapp.model.Guardian;
import com.example.myfirstapp.model.User;
import com.example.myfirstapp.service.AuthService;
import com.example.myfirstapp.service.ConnectService;
import com.example.myfirstapp.service.EarlyReleaseService;
import com.example.myfirstapp.service.WalletService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Guardian & Permissions Management View.
 * Matches the clean, rounded-card, modern aesthetic of HomeView.
 * Provides intuitive, one-tap boolean controls for supervising wards and managing permissions.
 */
public class GuardianView extends VBox {
    private final AuthService authService = AuthService.getInstance();
    private final ConnectService connectService = new ConnectService();
    private final EarlyReleaseService earlyReleaseService = new EarlyReleaseService();
    private final WalletService walletService = new WalletService();

    private final TextField guardianNameField;
    private final RadioButton radioParent;
    private final RadioButton radioFriend;
    private final RadioButton radioMentor;

    private final FlowPane guardiansChipsPane;
    private final Label noGuardiansLabel;

    private final CheckBox parentApprovalCheck;
    private final Label parentStatusBadge;
    private final Label parentHintLabel;

    private final CheckBox friendApprovalCheck;
    private final Label friendStatusBadge;
    private final Label friendHintLabel;

    private final CheckBox targetApprovalCheck;
    private final Label targetStatusBadge;
    private final Label targetHintLabel;

    private final VBox booleanSummaryCard;
    private final Label booleanFormulaLabel;
    private final Label booleanResultBadge;

    private final Label statusLabel;
    private final HBox statusBanner;
    private final VBox wardsContainer;
    private final Label wardsCountBadge;

    public GuardianView() {
        setSpacing(12);
        setPadding(new Insets(14, 16, 20, 16));
        setStyle("-fx-background-color: #E0F7FA;");

        // 1. Top Navigation Bar (Consistent with Home & Subviews)
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button();
        backBtn.setGraphic(IconHelper.getBackArrowIcon(Color.web("#263238"), 1.2));
        backBtn.getStyleClass().add("back-button");
        backBtn.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-padding: 8px; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);");
        backBtn.setOnAction(e -> ScreenManager.getInstance().goBack());

        VBox titleBox = new VBox(2);
        Label title = new Label("Guardian & Approvals");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #263238;");
        Label subtitle = new Label("Supervise wards & manage personal permissions");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #546E7A;");
        titleBox.getChildren().addAll(title, subtitle);

        topBar.getChildren().addAll(backBtn, titleBox);

        // 2. Status Toast Banner (Soft rounded pill)
        statusBanner = new HBox(8);
        statusBanner.setAlignment(Pos.CENTER_LEFT);
        statusBanner.setPadding(new Insets(10, 16, 10, 16));
        statusBanner.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 16px; -fx-border-color: #A5D6A7; -fx-border-radius: 16px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");
        statusBanner.setVisible(false);
        statusBanner.setManaged(false);

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        statusLabel.setWrapText(true);
        statusBanner.getChildren().add(statusLabel);

        // 3. Main Scrollable Layout
        VBox contentBox = new VBox(16);
        contentBox.setStyle("-fx-background-color: transparent;");

        // =====================================================================
        // CARD 1: Wards Depending On Your Permission (Supervising Others)
        // =====================================================================
        VBox wardsSection = new VBox(12);
        wardsSection.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-border-radius: 20px; " +
                "-fx-border-color: #B2EBF2; -fx-padding: 18px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0.05, 0, 3);");

        HBox wardsHeaderRow = new HBox(10);
        wardsHeaderRow.setAlignment(Pos.CENTER_LEFT);

        StackPane shieldCircle = new StackPane(IconHelper.getAddGuardianIcon(Color.web("#00838F"), 0.85));
        shieldCircle.setStyle("-fx-background-color: #E0F7FA; -fx-background-radius: 20px; -fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px;");

        Label wardsSectionTitle = new Label("Wards Depending On You");
        wardsSectionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00838F;");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        wardsCountBadge = new Label("0 Wards");
        wardsCountBadge.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-font-size: 11px; -fx-padding: 4 12; -fx-background-radius: 14px;");

        wardsHeaderRow.getChildren().addAll(shieldCircle, wardsSectionTitle, spacer1, wardsCountBadge);

        Label wardsDesc = new Label("Savers who listed you as their authority. Grant or revoke their permissions with one tap:");
        wardsDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #546E7A; -fx-wrap-text: true;");

        wardsContainer = new VBox(12);
        wardsContainer.setStyle("-fx-background-color: transparent;");

        wardsSection.getChildren().addAll(wardsHeaderRow, wardsDesc, wardsContainer);

        // =====================================================================
        // CARD 2: Guardians Supervising You & Your Personal Approval Statuses
        // =====================================================================
        VBox manageCard = new VBox(14);
        manageCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-border-radius: 20px; " +
                "-fx-border-color: #B2EBF2; -fx-padding: 18px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0.05, 0, 3);");

        HBox guardianHeaderRow = new HBox(10);
        guardianHeaderRow.setAlignment(Pos.CENTER_LEFT);

        StackPane userCircle = new StackPane(IconHelper.getConnectIcon(Color.web("#00838F"), 0.85));
        userCircle.setStyle("-fx-background-color: #E0F7FA; -fx-background-radius: 20px; -fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px;");

        Label registeredTitle = new Label("Guardians Supervising You");
        registeredTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00838F;");
        guardianHeaderRow.getChildren().addAll(userCircle, registeredTitle);

        Label guardianSub = new Label("Your trusted guardians whose authority is required for your early withdrawals and target changes:");
        guardianSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #546E7A; -fx-wrap-text: true;");

        // Guardian Chips FlowPane (Soft rounded pills)
        guardiansChipsPane = new FlowPane(8, 8);
        guardiansChipsPane.setPadding(new Insets(2, 0, 4, 0));

        noGuardiansLabel = new Label("No registered guardians yet. Add one in the section below.");
        noGuardiansLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #90A4AE; -fx-font-style: italic;");

        Separator sep1 = new Separator();

        // Boolean Permissions Header
        VBox booleanHeaderBox = new VBox(2);
        Label approvalHeader = new Label("Your Personal Approval Statuses");
        approvalHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #263238;");
        Label approvalSub = new Label("SmartSave boolean verification rules for early fund releases:");
        approvalSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #546E7A;");
        booleanHeaderBox.getChildren().addAll(approvalHeader, approvalSub);

        // Rule 1: Parent Emergency Approval Card
        VBox parentCard = new VBox(4);
        parentCard.setStyle("-fx-background-color: #F8FDFF; -fx-background-radius: 14px; -fx-padding: 10px 14px; " +
                "-fx-border-color: #E0F2F1; -fx-border-radius: 14px;");

        HBox parentRow = new HBox(8);
        parentRow.setAlignment(Pos.CENTER_LEFT);
        parentApprovalCheck = new CheckBox("Parent Emergency Approval");
        parentApprovalCheck.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #263238; -fx-cursor: hand;");
        parentApprovalCheck.setOnAction(e -> handleParentApprovalToggle());

        Region parentSpacer = new Region();
        HBox.setHgrow(parentSpacer, Priority.ALWAYS);

        parentStatusBadge = new Label("🔒 NOT GRANTED");
        parentStatusBadge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;");
        parentRow.getChildren().addAll(parentApprovalCheck, parentSpacer, parentStatusBadge);

        parentHintLabel = new Label("Requires at least one registered Parent guardian.");
        parentHintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #78909C; -fx-padding: 0 0 0 24;");
        parentCard.getChildren().addAll(parentRow, parentHintLabel);

        // Rule 2: Close Friend Approval Card
        VBox friendCard = new VBox(4);
        friendCard.setStyle("-fx-background-color: #F8FDFF; -fx-background-radius: 14px; -fx-padding: 10px 14px; " +
                "-fx-border-color: #E0F2F1; -fx-border-radius: 14px;");

        HBox friendRow = new HBox(8);
        friendRow.setAlignment(Pos.CENTER_LEFT);
        friendApprovalCheck = new CheckBox("Close Friend Early Approval");
        friendApprovalCheck.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #263238; -fx-cursor: hand;");
        friendApprovalCheck.setOnAction(e -> handleFriendApprovalToggle());

        Region friendSpacer = new Region();
        HBox.setHgrow(friendSpacer, Priority.ALWAYS);

        friendStatusBadge = new Label("🔒 NOT GRANTED");
        friendStatusBadge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;");
        friendRow.getChildren().addAll(friendApprovalCheck, friendSpacer, friendStatusBadge);

        friendHintLabel = new Label("Requires at least one registered Close Friend guardian.");
        friendHintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #78909C; -fx-padding: 0 0 0 24;");
        friendCard.getChildren().addAll(friendRow, friendHintLabel);

        // Rule 3: Target Modification Permission Card
        VBox targetCard = new VBox(4);
        targetCard.setStyle("-fx-background-color: #F8FDFF; -fx-background-radius: 14px; -fx-padding: 10px 14px; " +
                "-fx-border-color: #E0F2F1; -fx-border-radius: 14px;");

        HBox targetRow = new HBox(8);
        targetRow.setAlignment(Pos.CENTER_LEFT);
        targetApprovalCheck = new CheckBox("Target Modification Permission");
        targetApprovalCheck.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #00838F; -fx-cursor: hand;");
        targetApprovalCheck.setOnAction(e -> handleTargetApprovalToggle());

        Region targetSpacer = new Region();
        HBox.setHgrow(targetSpacer, Priority.ALWAYS);

        targetStatusBadge = new Label("🔒 BLOCKED");
        targetStatusBadge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;");
        targetRow.getChildren().addAll(targetApprovalCheck, targetSpacer, targetStatusBadge);

        targetHintLabel = new Label("Requires guardian authority to adjust your savings commitment.");
        targetHintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #78909C; -fx-padding: 0 0 0 24;");
        targetCard.getChildren().addAll(targetRow, targetHintLabel);

        // Computed Boolean Status Summary Pill Card
        booleanSummaryCard = new VBox(6);
        booleanSummaryCard.setPadding(new Insets(10, 14, 10, 14));
        booleanSummaryCard.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 14px; -fx-border-color: #A5D6A7; -fx-border-radius: 14px;");

        HBox summaryHeaderRow = new HBox(8);
        summaryHeaderRow.setAlignment(Pos.CENTER_LEFT);

        Label summaryTitle = new Label("Computed Early-Release Status:");
        summaryTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        Region summarySpacer = new Region();
        HBox.setHgrow(summarySpacer, Priority.ALWAYS);

        booleanResultBadge = new Label("🟢 UNLOCKED");
        booleanResultBadge.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;");
        summaryHeaderRow.getChildren().addAll(summaryTitle, summarySpacer, booleanResultBadge);

        booleanFormulaLabel = new Label("Logic: Target Met (false) OR Parent Approved (true) OR Friend Approved (false)");
        booleanFormulaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #37474F; -fx-wrap-text: true;");
        booleanSummaryCard.getChildren().addAll(summaryHeaderRow, booleanFormulaLabel);

        manageCard.getChildren().addAll(guardianHeaderRow, guardianSub, guardiansChipsPane, noGuardiansLabel, sep1,
                booleanHeaderBox, parentCard, friendCard, targetCard, booleanSummaryCard);

        // =====================================================================
        // CARD 3: Add New Guardian / Authority (Clean, Modern Pill Form)
        // =====================================================================
        VBox addCard = new VBox(14);
        addCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20px; -fx-border-radius: 20px; " +
                "-fx-border-color: #B2EBF2; -fx-padding: 18px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0.05, 0, 3);");

        HBox addHeaderRow = new HBox(10);
        addHeaderRow.setAlignment(Pos.CENTER_LEFT);

        StackPane addCircle = new StackPane(IconHelper.createIcon("M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z", Color.web("#00838F"), 0.9));
        addCircle.setStyle("-fx-background-color: #E0F7FA; -fx-background-radius: 20px; -fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px;");

        Label addTitle = new Label("Add New Guardian");
        addTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00838F;");
        addHeaderRow.getChildren().addAll(addCircle, addTitle);

        Label addDesc = new Label("Enter a trusted contact to supervise your financial discipline:");
        addDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #546E7A;");

        Label inputHeader = new Label("Guardian Full Name or Username:");
        inputHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        guardianNameField = new TextField();
        guardianNameField.setPromptText("e.g. Mom, David Miller, or @sarah");
        guardianNameField.getStyleClass().add("text-input-field");
        guardianNameField.setStyle("-fx-font-size: 14px; -fx-text-fill: #263238; -fx-background-color: #F5F7F8; -fx-border-color: #CFD8DC; -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 10px 14px;");

        User meUser = authService.getCurrentUser();
        if (meUser != null) {
            Trie nameTrie = new Trie();
            for (User u : new UserDao().findAllExcept(meUser.getId())) {
                nameTrie.insert(u.getFullName());
                nameTrie.insert(u.getUsername());
            }
            AutoComplete.attach(guardianNameField, nameTrie);
        }

        Label roleOptionsLabel = new Label("Select Guardian Authority Role:");
        roleOptionsLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        ToggleGroup roleGroup = new ToggleGroup();

        // Option 1: Parent
        VBox optionParentBox = new VBox(3);
        optionParentBox.setStyle("-fx-background-color: #F8FDFF; -fx-background-radius: 12px; -fx-padding: 8 12; -fx-border-color: #E0F2F1; -fx-border-radius: 12px;");
        radioParent = new RadioButton("Parent / Legal Guardian");
        radioParent.setToggleGroup(roleGroup);
        radioParent.setSelected(true);
        radioParent.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #263238; -fx-cursor: hand;");
        Label parentDesc = new Label("Can grant Parent Emergency Early-Release approval.");
        parentDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #78909C; -fx-padding: 0 0 0 24;");
        optionParentBox.getChildren().addAll(radioParent, parentDesc);

        // Option 2: Close Friend
        VBox optionFriendBox = new VBox(3);
        optionFriendBox.setStyle("-fx-background-color: #F8FDFF; -fx-background-radius: 12px; -fx-padding: 8 12; -fx-border-color: #E0F2F1; -fx-border-radius: 12px;");
        radioFriend = new RadioButton("Close Friend / Trusted Peer");
        radioFriend.setToggleGroup(roleGroup);
        radioFriend.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #263238; -fx-cursor: hand;");
        Label friendDesc = new Label("Can grant Close Friend Peer-Authorized early release.");
        friendDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #78909C; -fx-padding: 0 0 0 24;");
        optionFriendBox.getChildren().addAll(radioFriend, friendDesc);

        // Option 3: Academic Mentor
        VBox optionMentorBox = new VBox(3);
        optionMentorBox.setStyle("-fx-background-color: #F8FDFF; -fx-background-radius: 12px; -fx-padding: 8 12; -fx-border-color: #E0F2F1; -fx-border-radius: 12px;");
        radioMentor = new RadioButton("Academic Mentor / Advisor");
        radioMentor.setToggleGroup(roleGroup);
        radioMentor.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #263238; -fx-cursor: hand;");
        Label mentorDesc = new Label("Can supervise educational progress and target change approvals.");
        mentorDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #78909C; -fx-padding: 0 0 0 24;");
        optionMentorBox.getChildren().addAll(radioMentor, mentorDesc);

        VBox radioContainer = new VBox(8, optionParentBox, optionFriendBox, optionMentorBox);

        Button btnAddGuardian = new Button("Add Guardian");
        btnAddGuardian.getStyleClass().add("cyan-button");
        btnAddGuardian.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; " +
                "-fx-background-radius: 16px; -fx-padding: 12px 24px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0, 188, 212, 0.3), 8, 0.1, 0, 3);");
        btnAddGuardian.setPrefWidth(Double.MAX_VALUE);
        btnAddGuardian.setOnAction(e -> handleAddGuardian());

        addCard.getChildren().addAll(addHeaderRow, addDesc, inputHeader, guardianNameField, roleOptionsLabel, radioContainer, btnAddGuardian);

        contentBox.getChildren().addAll(statusBanner, wardsSection, manageCard, addCard);

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-hbar-policy: never;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(topBar, scrollPane);

        refreshData();
    }

    private void showToast(String message, boolean isSuccess) {
        statusLabel.setText(message);
        if (isSuccess) {
            statusBanner.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 16px; -fx-border-color: #A5D6A7; -fx-border-radius: 16px;");
            statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        } else {
            statusBanner.setStyle("-fx-background-color: #FFEBEE; -fx-background-radius: 16px; -fx-border-color: #FFCDD2; -fx-border-radius: 16px;");
            statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #C62828;");
        }
        statusBanner.setVisible(true);
        statusBanner.setManaged(true);
    }

    private void refreshData() {
        User user = authService.getCurrentUser();
        if (user == null) return;

        // 1. Populate Wards who added user as guardian
        List<GuardianDao.WardRecord> wards = connectService.getWardsWhoAddedUser(user.getId(), user.getFullName(), user.getUsername());
        wardsCountBadge.setText(wards.size() + (wards.size() == 1 ? " Ward" : " Wards"));
        wardsContainer.getChildren().clear();

        if (wards.isEmpty()) {
            VBox emptyCard = new VBox(10);
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(16));
            emptyCard.setStyle("-fx-background-color: #F8FDFF; -fx-background-radius: 16px; -fx-border-color: #CFD8DC; -fx-border-radius: 16px;");

            Label emptyLabel = new Label("No one has added you as their guardian yet.");
            emptyLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #37474F;");

            Label emptyTip = new Label("When friends or family enter your name or username as their Guardian, they will appear here so you can manage their permissions.");
            emptyTip.setStyle("-fx-font-size: 11px; -fx-text-fill: #78909C; -fx-wrap-text: true; -fx-text-alignment: center;");

            Button btnSimulateWard = new Button("+ Link Sarah Jenkins as Sample Ward");
            btnSimulateWard.setStyle("-fx-background-color: #E0F7FA; -fx-text-fill: #00838F; -fx-font-weight: bold; " +
                    "-fx-border-color: #00BCD4; -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-cursor: hand; -fx-padding: 8 16; -fx-font-size: 12px;");
            btnSimulateWard.setOnAction(e -> {
                connectService.linkWardSample(2, user.getId(), user.getFullName(), "Parent");
                showToast("Linked Sarah Jenkins as your ward for live permission management!", true);
                refreshData();
            });

            emptyCard.getChildren().addAll(emptyLabel, emptyTip, btnSimulateWard);
            wardsContainer.getChildren().add(emptyCard);
        } else {
            for (GuardianDao.WardRecord ward : wards) {
                wardsContainer.getChildren().add(createWardCard(ward, user));
            }
        }

        // 2. Populate Guardians supervising user as rounded chips
        List<Guardian> guardians = connectService.getGuardians(user.getId());
        guardiansChipsPane.getChildren().clear();

        if (guardians.isEmpty()) {
            noGuardiansLabel.setVisible(true);
            noGuardiansLabel.setManaged(true);
        } else {
            noGuardiansLabel.setVisible(false);
            noGuardiansLabel.setManaged(false);

            for (Guardian g : guardians) {
                HBox chip = new HBox(8);
                chip.setAlignment(Pos.CENTER_LEFT);
                chip.setStyle("-fx-background-color: #E0F7FA; -fx-background-radius: 16px; -fx-padding: 6 12; " +
                        "-fx-border-color: #80DEEA; -fx-border-radius: 16px;");

                Label avatarDot = new Label("👤");
                avatarDot.setStyle("-fx-font-size: 11px;");

                Label chipText = new Label(g.getName() + " (" + g.getRelationship() + ")");
                chipText.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #00838F;");

                Button btnRemove = new Button("✕");
                btnRemove.setStyle("-fx-background-color: transparent; -fx-text-fill: #78909C; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 4;");
                btnRemove.setOnMouseEntered(e -> btnRemove.setStyle("-fx-background-color: transparent; -fx-text-fill: #C62828; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 4;"));
                btnRemove.setOnMouseExited(e -> btnRemove.setStyle("-fx-background-color: transparent; -fx-text-fill: #78909C; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 4;"));
                btnRemove.setOnAction(e -> {
                    connectService.removeGuardian(g.getId());
                    showToast("Removed guardian: " + g.getName(), true);
                    refreshData();
                });

                chip.getChildren().addAll(avatarDot, chipText, btnRemove);
                guardiansChipsPane.getChildren().add(chip);
            }
        }

        // 3. Evaluate Boolean Logic for Personal Permissions
        boolean hasParent = guardians.stream().anyMatch(g -> "Parent".equalsIgnoreCase(g.getRelationship()));
        boolean hasFriend = guardians.stream().anyMatch(g -> "Close Friend".equalsIgnoreCase(g.getRelationship()) || "Mentor".equalsIgnoreCase(g.getRelationship()));
        boolean hasAnyGuardian = !guardians.isEmpty();

        boolean isParentApproved = earlyReleaseService.isParentApproved(user.getId());
        boolean isFriendApproved = earlyReleaseService.isFriendApproved(user.getId());
        boolean isTargetApproved = walletService.isGuardianTargetPermissionGranted(user.getId());

        // Update Parent Checkbox & Badge
        parentApprovalCheck.setDisable(!hasParent && !isParentApproved);
        parentApprovalCheck.setSelected(isParentApproved);
        if (isParentApproved) {
            parentStatusBadge.setText("✓ APPROVED");
            parentStatusBadge.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;");
            parentHintLabel.setText("Active parent approval authorized.");
            parentHintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-padding: 0 0 0 24;");
        } else {
            parentStatusBadge.setText("🔒 NOT GRANTED");
            parentStatusBadge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;");
            String parentNames = guardians.stream().filter(g -> "Parent".equalsIgnoreCase(g.getRelationship())).map(Guardian::getName).collect(Collectors.joining(", "));
            parentHintLabel.setText(hasParent ? "Available parent authorities: " + parentNames : "Disabled: No Parent guardian registered.");
            parentHintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #78909C; -fx-padding: 0 0 0 24;");
        }

        // Update Friend Checkbox & Badge
        friendApprovalCheck.setDisable(!hasFriend && !isFriendApproved);
        friendApprovalCheck.setSelected(isFriendApproved);
        if (isFriendApproved) {
            friendStatusBadge.setText("✓ APPROVED");
            friendStatusBadge.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;");
            friendHintLabel.setText("Active close friend approval authorized.");
            friendHintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-padding: 0 0 0 24;");
        } else {
            friendStatusBadge.setText("🔒 NOT GRANTED");
            friendStatusBadge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;");
            String friendNames = guardians.stream().filter(g -> "Close Friend".equalsIgnoreCase(g.getRelationship()) || "Mentor".equalsIgnoreCase(g.getRelationship())).map(Guardian::getName).collect(Collectors.joining(", "));
            friendHintLabel.setText(hasFriend ? "Available friend authorities: " + friendNames : "Disabled: No Close Friend registered.");
            friendHintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #78909C; -fx-padding: 0 0 0 24;");
        }

        // Update Target Checkbox & Badge
        targetApprovalCheck.setDisable(!hasAnyGuardian && !isTargetApproved);
        targetApprovalCheck.setSelected(isTargetApproved);
        if (isTargetApproved) {
            targetStatusBadge.setText("✓ PERMITTED");
            targetStatusBadge.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;");
            targetHintLabel.setText("Target modifications permitted by guardian authority.");
            targetHintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-padding: 0 0 0 24;");
        } else {
            targetStatusBadge.setText("🔒 LOCKED");
            targetStatusBadge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;");
            targetHintLabel.setText(hasAnyGuardian ? "Guardian authority required to adjust target." : "Disabled: Register a guardian first.");
            targetHintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #78909C; -fx-padding: 0 0 0 24;");
        }

        // 4. Update Overall Computed Boolean Summary
        boolean isAuthorized = earlyReleaseService.isEarlyWithdrawalAuthorized(user.getId(), 1.0);
        if (isAuthorized) {
            booleanSummaryCard.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 14px; -fx-border-color: #A5D6A7; -fx-border-radius: 14px;");
            booleanResultBadge.setText("🟢 UNLOCKED");
            booleanResultBadge.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;");
            booleanFormulaLabel.setText(String.format("Boolean: (Parent=%b OR Friend=%b) ➔ Early Withdrawal is AUTHORIZED", isParentApproved, isFriendApproved));
        } else {
            booleanSummaryCard.setStyle("-fx-background-color: #FFF8E1; -fx-background-radius: 14px; -fx-border-color: #FFE082; -fx-border-radius: 14px;");
            booleanResultBadge.setText("🔒 RESTRICTED");
            booleanResultBadge.setStyle("-fx-background-color: #F57C00; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;");
            booleanFormulaLabel.setText("Boolean: Target not yet reached & no guardian approvals active ➔ Early Withdrawal is RESTRICTED");
        }
    }

    private VBox createWardCard(GuardianDao.WardRecord ward, User currentUser) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #F8FDFF; -fx-background-radius: 16px; -fx-padding: 14px; " +
                "-fx-border-color: #B2EBF2; -fx-border-radius: 16px;");

        // Top Row: Avatar Circle + Name + Relationship Badge
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label(ward.getWardFullName().substring(0, 1).toUpperCase(Locale.getDefault()));
        avatar.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-font-size: 14px; -fx-alignment: center; -fx-background-radius: 20px; " +
                "-fx-min-width: 36px; -fx-min-height: 36px; -fx-max-width: 36px; -fx-max-height: 36px;");

        VBox nameBox = new VBox(1);
        Label nameLabel = new Label(ward.getWardFullName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #263238;");
        Label usernameLabel = new Label("@" + ward.getWardUsername());
        usernameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #78909C;");
        nameBox.getChildren().addAll(nameLabel, usernameLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label roleBadge = new Label("Role: " + ward.getRelationship());
        roleBadge.setStyle("-fx-background-color: #E0F2F1; -fx-text-fill: #00796B; -fx-font-weight: bold; " +
                "-fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 12px;");

        topRow.getChildren().addAll(avatar, nameBox, spacer, roleBadge);

        // Middle Row: Financial Summary Box (Clean rounded pill)
        HBox financialBox = new HBox(12);
        financialBox.setAlignment(Pos.CENTER_LEFT);
        financialBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-padding: 8 12; " +
                "-fx-border-color: #E0F2F1; -fx-border-radius: 12px;");

        Label targetInfo = new Label(String.format(Locale.getDefault(), "Target: ₹%.2f", ward.getTargetBalance()));
        targetInfo.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        Label availInfo = new Label(String.format(Locale.getDefault(), "Available: ₹%.2f", ward.getAvailableBalance()));
        availInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #00838F;");

        Label lockedInfo = new Label(String.format(Locale.getDefault(), "Locked: ₹%.2f", ward.getLockedSavings()));
        lockedInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #E65100;");

        financialBox.getChildren().addAll(targetInfo, availInfo, lockedInfo);

        Separator sep = new Separator();

        // Control 1: Early Emergency Withdrawal CheckBox + One-Tap Action Button
        boolean withdrawalApproved = earlyReleaseService.isWardWithdrawalApproved(ward.getWardUserId(), ward.getRelationship());
        HBox withdrawalRow = new HBox(8);
        withdrawalRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox chkWardWithdrawal = new CheckBox();
        chkWardWithdrawal.setSelected(withdrawalApproved);
        chkWardWithdrawal.setStyle("-fx-cursor: hand;");

        Label withdrawalLabel = new Label("Withdrawal:");
        withdrawalLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        Region rowSpacer1 = new Region();
        HBox.setHgrow(rowSpacer1, Priority.ALWAYS);

        Label withdrawalStatusBadge = new Label(withdrawalApproved ? "● APPROVED" : "🔒 LOCKED");
        withdrawalStatusBadge.setStyle(withdrawalApproved
                ? "-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;"
                : "-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;");

        Button btnWithdrawalAction = new Button(withdrawalApproved ? "✕ Revoke" : "✓ Grant");
        btnWithdrawalAction.setStyle(withdrawalApproved
                ? "-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: #FFCDD2; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-cursor: hand; -fx-padding: 4 12;"
                : "-fx-background-color: #00BCD4; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 12px; -fx-cursor: hand; -fx-padding: 4 12;");

        Runnable toggleWithdrawal = () -> {
            boolean nextState = !withdrawalApproved;
            earlyReleaseService.setWardWithdrawalPermission(ward.getWardUserId(), ward.getRelationship(), nextState, currentUser.getFullName());
            showToast((nextState ? "Granted" : "Revoked") + " Early Withdrawal permission for " + ward.getWardFullName() + "!", nextState);
            refreshData();
        };

        chkWardWithdrawal.setOnAction(e -> toggleWithdrawal.run());
        btnWithdrawalAction.setOnAction(e -> toggleWithdrawal.run());

        withdrawalRow.getChildren().addAll(chkWardWithdrawal, withdrawalLabel, withdrawalStatusBadge, rowSpacer1, btnWithdrawalAction);

        // Control 2: Target Modification Permission CheckBox + One-Tap Action Button
        boolean targetApproved = walletService.isGuardianTargetPermissionGranted(ward.getWardUserId());
        HBox targetRow = new HBox(8);
        targetRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox chkWardTarget = new CheckBox();
        chkWardTarget.setSelected(targetApproved);
        chkWardTarget.setStyle("-fx-cursor: hand;");

        Label targetLabel = new Label("Target:");
        targetLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #263238;");

        Region rowSpacer2 = new Region();
        HBox.setHgrow(rowSpacer2, Priority.ALWAYS);

        Label targetStatusBadge = new Label(targetApproved ? "● PERMITTED" : "🔒 BLOCKED");
        targetStatusBadge.setStyle(targetApproved
                ? "-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;"
                : "-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 12px;");

        Button btnTargetAction = new Button(targetApproved ? "✕ Revoke" : "✓ Grant");
        btnTargetAction.setStyle(targetApproved
                ? "-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: #FFCDD2; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-cursor: hand; -fx-padding: 4 12;"
                : "-fx-background-color: #00838F; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 12px; -fx-cursor: hand; -fx-padding: 4 12;");

        Runnable toggleTarget = () -> {
            boolean nextState = !targetApproved;
            walletService.setGuardianTargetPermission(ward.getWardUserId(), nextState, currentUser.getFullName());
            showToast((nextState ? "Granted" : "Revoked") + " Target Modification permission for " + ward.getWardFullName() + "!", nextState);
            refreshData();
        };

        chkWardTarget.setOnAction(e -> toggleTarget.run());
        btnTargetAction.setOnAction(e -> toggleTarget.run());

        targetRow.getChildren().addAll(chkWardTarget, targetLabel, targetStatusBadge, rowSpacer2, btnTargetAction);

        card.getChildren().addAll(topRow, financialBox, sep, withdrawalRow, targetRow);
        return card;
    }

    private void handleAddGuardian() {
        String name = guardianNameField.getText().trim();
        if (name.isEmpty()) {
            showToast("Please enter guardian name or username!", false);
            return;
        }

        String relationship;
        if (radioParent.isSelected()) {
            relationship = "Parent";
        } else if (radioFriend.isSelected()) {
            relationship = "Close Friend";
        } else {
            relationship = "Mentor";
        }

        User user = authService.getCurrentUser();
        if (user != null) {
            connectService.addGuardian(user.getId(), name, relationship);
            guardianNameField.clear();
            refreshData();
            showToast(name + " successfully added as your " + relationship + "!", true);
        }
    }

    private void handleParentApprovalToggle() {
        User user = authService.getCurrentUser();
        if (user == null) return;

        List<Guardian> guardians = connectService.getGuardians(user.getId());
        Guardian parentGuardian = guardians.stream()
                .filter(g -> "Parent".equalsIgnoreCase(g.getRelationship()))
                .findFirst()
                .orElse(null);

        if (parentGuardian == null) {
            parentApprovalCheck.setSelected(false);
            showToast("Cannot approve: You must first register a Parent guardian!", false);
            return;
        }

        boolean granted = parentApprovalCheck.isSelected();
        boolean processed = earlyReleaseService.processParentApproval(user.getId(), parentGuardian.getName(), "Parent", granted);

        if (processed) {
            showToast(granted ? "Parent approval granted by " + parentGuardian.getName() + "!" : "Parent approval revoked.", granted);
        } else {
            parentApprovalCheck.setSelected(!granted);
            showToast("Failed updating parent approval.", false);
        }
        refreshData();
    }

    private void handleFriendApprovalToggle() {
        User user = authService.getCurrentUser();
        if (user == null) return;

        List<Guardian> guardians = connectService.getGuardians(user.getId());
        Guardian friendGuardian = guardians.stream()
                .filter(g -> "Close Friend".equalsIgnoreCase(g.getRelationship()) || "Mentor".equalsIgnoreCase(g.getRelationship()))
                .findFirst()
                .orElse(null);

        if (friendGuardian == null) {
            friendApprovalCheck.setSelected(false);
            showToast("Cannot approve: You must first register a Close Friend!", false);
            return;
        }

        boolean granted = friendApprovalCheck.isSelected();
        boolean processed = earlyReleaseService.processFriendApproval(user.getId(), friendGuardian.getName(), granted);

        if (processed) {
            showToast(granted ? "Close Friend approval granted by " + friendGuardian.getName() + "!" : "Friend approval revoked.", granted);
        } else {
            friendApprovalCheck.setSelected(!granted);
            showToast("Failed updating friend approval.", false);
        }
        refreshData();
    }

    private void handleTargetApprovalToggle() {
        User user = authService.getCurrentUser();
        if (user == null) return;

        List<Guardian> list = connectService.getGuardians(user.getId());
        if (list.isEmpty()) {
            targetApprovalCheck.setSelected(false);
            showToast("Cannot authorize: You must first register a Guardian!", false);
            return;
        }

        boolean granted = targetApprovalCheck.isSelected();
        String guardianName = list.get(0).getName();
        walletService.setGuardianTargetPermission(user.getId(), granted, guardianName);
        showToast(granted ? "Guardian permission granted for target modifications!" : "Guardian target permission revoked.", granted);
        refreshData();
    }
}

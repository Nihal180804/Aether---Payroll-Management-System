package com.payroll.system;

import com.payroll.system.presenter.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * =============================================================================
 * CLASS: PayrollDashboardUI  (com.payroll.system)
 * =============================================================================
 * Modern SaaS-style payroll dashboard built with JavaFX.
 *
 * COUPLING DESIGN: This class imports NO class from model, service, pattern,
 * exception, or util packages. All backend calls go through PayrollPresenter.
 * Swap the UI entirely to a CLI or REST controller without touching any backend.
 *
 * ENTRY POINT:
 *   PayrollPresenterImpl is the default implementation (MockPayrollRepository).
 *   To switch to real DB: new PayrollPresenterImpl(new RealPayrollRepository(...), ...)
 * =============================================================================
 */
public class PayrollDashboardUI extends Application {

    // ── Presenter (only backend reference this class holds) ───────────────────
    private PayrollPresenter presenter;

    // ── State ─────────────────────────────────────────────────────────────────
    private String             currentPayPeriod = "2025-06";
    private String             currentBatchId   = "BATCH-2025-06";

    // ── Observable data lists (bound to tables) ───────────────────────────────
    private final ObservableList<EmployeeRowModel>   employeeList   = FXCollections.observableArrayList();
    private final ObservableList<PayrollRowModel>    payrollResults = FXCollections.observableArrayList();
    private final ObservableList<String>             auditLogEntries = FXCollections.observableArrayList();
    private final ObservableList<String>             employeeNamesForSearch = FXCollections.observableArrayList();

    // ── UI References ─────────────────────────────────────────────────────────
    private BorderPane     root;
    private VBox           sidebar;
    private StackPane      contentArea;
    private Label          pageTitle;
    private Label          statusBar;
    private ProgressBar    globalProgress;
    private Label          dbStatusLabel;
    private Label          empCountLabel;

    // ── Navigation tracking ───────────────────────────────────────────────────
    private Button         activeNavBtn;
    private String         currentView = "Dashboard";

    // ═══════════════════════════════════════════════════════════════════════════
    //  JavaFX Entry
    // ═══════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Wire presenter — swap this line to change backend
        presenter = new PayrollPresenterImpl();

        // Load employee data before building views
        loadEmployeesFromPresenter();

        // Build layout
        root        = new BorderPane();
        sidebar     = buildSidebar();
        contentArea = new StackPane();
        pageTitle   = new Label("Dashboard");

        HBox topBar = buildTopBar();
        root.setTop(topBar);
        root.setLeft(sidebar);
        root.setCenter(contentArea);

        applyRootStyle();

        // Load initial view
        showView("Dashboard");

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().clear();
        primaryStage.setScene(scene);
        primaryStage.setTitle("Aether — Payroll Management System");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(650);
        primaryStage.show();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Data Loading via Presenter (all backend calls are here)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Loads employees from presenter into the observable list. */
    private void loadEmployeesFromPresenter() {
        List<EmployeeViewModel> vms = presenter.loadAllEmployees(currentPayPeriod);
        employeeList.clear();
        employeeNamesForSearch.clear();
        for (EmployeeViewModel vm : vms) {
            employeeList.add(new EmployeeRowModel(
                    vm.empID, vm.name, vm.department, vm.grade,
                    vm.basicPay, vm.country, vm.taxRegime,
                    vm.state, vm.yearsService, vm.leaveWithoutPay,
                    vm.overtimeHours, vm.pendingClaims, vm.approvedReimbursement));
            employeeNamesForSearch.add(vm.name + " (" + vm.empID + ")");
        }
    }

    /** Syncs the audit log from presenter into the observable list. */
    private void syncAuditLog() {
        auditLogEntries.setAll(presenter.getAuditLog());
    }

    /** Runs a payroll batch in a background thread and updates the results table. */
    private void executePayrollBatch(String batchId, String payPeriod,
                                     ProgressBar progressBar, Label statusLabel,
                                     VBox hint, TableView<PayrollRowModel> table,
                                     Button runBtn) {
        runBtn.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        statusLabel.setText("Running batch " + batchId + " for period " + payPeriod + "…");

        Task<BatchResult> task = new Task<>() {
            @Override
            protected BatchResult call() {
                return presenter.runBatch(batchId, payPeriod);
            }
        };

        task.setOnSucceeded(e -> {
            BatchResult result = task.getValue();
            progressBar.setProgress(1.0);

            if (result.error != null) {
                progressBar.setStyle("-fx-accent: #ef4444;");
                statusLabel.setText("✗ Batch failed: " + result.error);
                showAlert(Alert.AlertType.ERROR, "Batch Failed",
                        result.error + "\n\nCheck Audit Log for details.");
                runBtn.setDisable(false);
                return;
            }

            // Populate results table
            payrollResults.clear();
            for (PayrollResultViewModel vm : result.results) {
                payrollResults.add(new PayrollRowModel(
                        vm.recordID, vm.empID, vm.name, vm.department,
                        vm.basicPay, vm.pf, vm.tds, vm.pt, vm.netPay, vm.status));
            }
            hint.setVisible(false);
            table.setVisible(true);

            // Sync audit log
            syncAuditLog();

            String summary = String.format(
                "✔ Batch %s complete — %d processed, %d skipped.",
                batchId, result.processedCount, result.skippedCount);
            statusLabel.setText(summary);
            progressBar.setStyle("-fx-accent: #22c55e;");
            updateStatusBar(summary);
        });

        task.setOnFailed(e -> {
            progressBar.setProgress(0);
            progressBar.setStyle("-fx-accent: #ef4444;");
            String msg = task.getException() != null
                    ? task.getException().getMessage() : "Unknown error";
            statusLabel.setText("✗ Error: " + msg);
            runBtn.setDisable(false);
        });

        new Thread(task, "payroll-batch-thread").start();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Navigation
    // ═══════════════════════════════════════════════════════════════════════════

    private void showView(String viewName) {
        currentView = viewName;
        pageTitle.setText(viewName);

        Node view = switch (viewName) {
            case "Dashboard"   -> createDashboardView();
            case "Employees"   -> createEmployeeView();
            case "Run Payroll" -> createRunPayrollView();
            case "Audit Log"   -> { syncAuditLog(); yield createAuditLogView(); }
            case "Reports"     -> createReportsView();
            case "Settings"    -> createSettingsView();
            default            -> createDashboardView();
        };

        contentArea.getChildren().clear();
        contentArea.getChildren().add(view);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Dashboard View
    // ═══════════════════════════════════════════════════════════════════════════

    private Node createDashboardView() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(28, 32, 28, 32));
        content.setStyle("-fx-background-color: #0f1117;");

        // Period header
        Label period = styledLabel(
            "Pay Period: " + currentPayPeriod + "  ·  " + presenter.getDbStatus(),
            13, "#94a3b8", false);

        // Stat cards row
        int empCount = presenter.getEmployeeCount();
        int auditCount = presenter.getAuditLog().size();

        HBox cards = new HBox(16,
            statCard("Total Employees",    String.valueOf(empCount),  "#6366f1", "👥"),
            statCard("Pay Period",         currentPayPeriod,          "#22c55e", "📅"),
            statCard("Processed",          payrollResults.isEmpty() ? "Not run yet"
                     : String.valueOf(payrollResults.filtered(r -> r.getStatus().startsWith("✔")).size()),
                     "#f59e0b", "✔"),
            statCard("Audit Entries",      String.valueOf(auditCount), "#ec4899", "📋")
        );
        cards.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cards.getChildren().get(0), Priority.ALWAYS);
        for (Node c : cards.getChildren()) HBox.setHgrow(c, Priority.ALWAYS);

        // Quick actions
        Label actTitle = styledLabel("Quick Actions", 16, "#e2e8f0", true);

        HBox actions = new HBox(12);
        actions.getChildren().addAll(
            actionBtn("▶  Run Payroll",    "#6366f1", () -> showView("Run Payroll")),
            actionBtn("👥  Employees",      "#0ea5e9", () -> showView("Employees")),
            actionBtn("📋  Audit Log",      "#8b5cf6", () -> showView("Audit Log")),
            actionBtn("📊  Reports",        "#10b981", () -> showView("Reports"))
        );

        // DB status card
        VBox dbCard = glassCard();
        dbCard.getChildren().addAll(
            styledLabel("Backend & Integrations", 13, "#94a3b8", false),
            styledLabel(presenter.getDbStatus(), 18, activeColor("#22c55e", "#6366f1",
                presenter.getDbStatus().contains("MockDB")), true),
            styledLabel("Payroll data is ready for batch processing and reporting.",
                12, "#94a3b8", false)
        );

        content.getChildren().addAll(period, cards, actTitle, actions, dbCard);
        scroll.setContent(content);
        return scroll;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Employee Directory View
    // ═══════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private Node createEmployeeView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(28, 32, 28, 32));
        root.setStyle("-fx-background-color: #0f1117;");

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label countLbl = styledLabel(employeeList.size() + " employees loaded  ·  " + presenter.getDbStatus(),
                13, "#94a3b8", false);
        Button refreshBtn = ghostBtn("⟳ Refresh");
        refreshBtn.setOnAction(e -> {
            loadEmployeesFromPresenter();
            countLbl.setText(employeeList.size() + " employees loaded  ·  " + presenter.getDbStatus());
        });
        topRow.getChildren().addAll(countLbl, new Spacer(), refreshBtn);

        TableView<EmployeeRowModel> table = new TableView<>(employeeList);
        table.setStyle(tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No employees loaded."));

        table.getColumns().addAll(
            col("EmpID",      "empID",      100),
            col("Name",       "name",       160),
            col("Department", "department", 140),
            col("Grade",      "grade",       70),
            col("Basic Pay",  "basicPay",   120),
            col("LOP Days",   "leaveWithoutPay", 80),
            col("OT Hours",   "overtimeHours", 90),
            col("Pending Claims", "pendingClaims", 130),
            col("Approved Reimb.", "approvedReimbursement", 140),
            col("Country",    "country",     70),
            col("Tax Regime", "taxRegime",  100),
            col("Work State", "state",      120),
            col("Years",      "yearsService", 75)
        );

        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(topRow, table);
        return root;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Run Payroll View
    // ═══════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private Node createRunPayrollView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(28, 32, 28, 32));
        root.setStyle("-fx-background-color: #0f1117;");

        // ── Config form ───────────────────────────────────────────────────────
        VBox configCard = glassCard();

        Label formTitle = styledLabel("Batch Configuration", 15, "#e2e8f0", true);

        HBox row1 = new HBox(16);
        TextField batchField  = formField("Batch ID",      currentBatchId);
        TextField periodField = formField("Pay Period",    currentPayPeriod);
        row1.getChildren().addAll(labeled("Batch ID", batchField), labeled("Pay Period", periodField));

        ProgressBar progress  = new ProgressBar(0);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setStyle("-fx-accent: #6366f1; -fx-pref-height: 6px;");
        progress.setVisible(false);

        Label statusLabel = styledLabel("Ready. Configure batch details above and click Run Payroll.",
                13, "#94a3b8", false);

        Button runBtn = primaryBtn("▶  Run Payroll Now");

        configCard.getChildren().addAll(formTitle, row1, runBtn, progress, statusLabel);

        // ── Results table ─────────────────────────────────────────────────────
        VBox hint = new VBox();
        hint.setAlignment(Pos.CENTER);
        hint.getChildren().add(styledLabel("Results will appear here after running a batch.",
                14, "#475569", false));

        TableView<PayrollRowModel> table = new TableView<>(payrollResults);
        table.setStyle(tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setVisible(!payrollResults.isEmpty());

        table.getColumns().addAll(
            col("Record ID",  "recordID",  150),
            col("ID",         "empID",      75),
            col("Name",       "name",      140),
            col("Department", "department",120),
            col("Basic Pay",  "basicPay",  110),
            col("PF",         "pf",         90),
            col("TDS",        "tds",        90),
            col("PT",         "pt",         75),
            col("Net Pay",    "netPay",    110),
            statusCol()
        );

        if (!payrollResults.isEmpty()) hint.setVisible(false);

        VBox.setVgrow(table, Priority.ALWAYS);

        // ── Wire button ───────────────────────────────────────────────────────
        runBtn.setOnAction(e -> {
            String bid = batchField.getText().trim();
            String pid = periodField.getText().trim();
            if (bid.isBlank() || pid.isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error",
                        "Batch ID and Pay Period cannot be empty.");
                return;
            }
            currentBatchId   = bid;
            currentPayPeriod = pid;
            executePayrollBatch(bid, pid, progress, statusLabel, hint, table, runBtn);
        });

        HBox verifyRow = new HBox(8);
        Button verifyBtn = ghostBtn("✔ Verify Last Batch");
        verifyBtn.setOnAction(e -> {
            boolean ok = presenter.verifyLastBatch();
            statusLabel.setText(ok ? "✔ Verification passed." : "✗ No batch to verify.");
        });
        verifyRow.getChildren().add(verifyBtn);

        root.getChildren().addAll(configCard, verifyRow, hint, table);
        return root;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Audit Log View
    // ═══════════════════════════════════════════════════════════════════════════

    private Node createAuditLogView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(28, 32, 28, 32));
        root.setStyle("-fx-background-color: #0f1117;");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label info = styledLabel(auditLogEntries.size() + " entries", 13, "#94a3b8", false);
        Button refreshBtn = ghostBtn("⟳ Refresh");
        Button clearBtn   = ghostBtn("🗑 Clear");
        refreshBtn.setOnAction(e -> {
            syncAuditLog();
            info.setText(auditLogEntries.size() + " entries");
        });
        clearBtn.setOnAction(e -> {
            presenter.clearAuditLog();
            auditLogEntries.clear();
            info.setText("0 entries");
        });
        topRow.getChildren().addAll(info, new Spacer(), refreshBtn, clearBtn);

        ListView<String> logList = new ListView<>(auditLogEntries);
        logList.setStyle(
            "-fx-background-color: #1e2435; -fx-border-radius: 8; " +
            "-fx-background-radius: 8; -fx-border-color: #2d3748;");
        logList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setFont(Font.font("Consolas", 12));
                    if (item.startsWith("[ERROR]"))
                        setTextFill(Color.web("#ef4444"));
                    else if (item.startsWith("[WARN]"))
                        setTextFill(Color.web("#f59e0b"));
                    else if (item.startsWith("[AUDIT]"))
                        setTextFill(Color.web("#22c55e"));
                    else
                        setTextFill(Color.web("#94a3b8"));
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        VBox.setVgrow(logList, Priority.ALWAYS);
        root.getChildren().addAll(topRow, logList);
        return root;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Reports View
    // ═══════════════════════════════════════════════════════════════════════════

    private Node createReportsView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(28, 32, 28, 32));
        root.setStyle("-fx-background-color: #0f1117;");
        root.setAlignment(Pos.TOP_LEFT);
        root.setFillWidth(true);

        VBox payslipCard = glassCard();
        payslipCard.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(payslipCard, Priority.ALWAYS);

        Label payslipTitle = styledLabel("Payslip Viewer", 16, "#e2e8f0", true);

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        ComboBox<String> employeeSearchBox = new ComboBox<>(employeeNamesForSearch);
        employeeSearchBox.setPromptText("Choose an employee...");
        employeeSearchBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(employeeSearchBox, Priority.ALWAYS);

        Button viewPayslipBtn = primaryBtn("Generate Payslip");
        Button exportAllPayslipsBtn = ghostBtn("Export All Payslips");
        searchRow.getChildren().addAll(new Label("Employee:"), employeeSearchBox, viewPayslipBtn, exportAllPayslipsBtn);

        TextArea payslipArea = new TextArea();
        payslipArea.setEditable(false);
        payslipArea.setFont(Font.font("Consolas", 12));
        payslipArea.setStyle("-fx-background-color: #0d0f14; -fx-text-fill: #94a3b8; -fx-background-insets: 0;");
        payslipArea.setMinHeight(360);
        payslipArea.setPrefHeight(520);
        payslipArea.setMaxHeight(Double.MAX_VALUE);
        payslipArea.setWrapText(false);
        payslipArea.setVisible(false);
        payslipArea.setManaged(false);
        VBox.setVgrow(payslipArea, Priority.ALWAYS);

        payslipCard.getChildren().addAll(payslipTitle, searchRow, payslipArea);

        viewPayslipBtn.setOnAction(e -> {
            String selected = employeeSearchBox.getSelectionModel().getSelectedItem();
            if (selected == null || !selected.contains("(")) {
                showAlert(Alert.AlertType.WARNING, "No Employee Selected", "Please select an employee from the list.");
                return;
            }
            String empId = selected.substring(selected.lastIndexOf('(') + 1, selected.lastIndexOf(')'));
            payslipArea.setPrefHeight(420);
            payslipArea.setText(presenter.getEmployeePayslip(empId, currentPayPeriod));
            payslipArea.setVisible(true);
            payslipArea.setManaged(true);
        });

        exportAllPayslipsBtn.setOnAction(e -> exportAllPayslips(payslipArea));

        root.getChildren().addAll(
            styledLabel("Reports & Payslips", 18, "#e2e8f0", true),
            payslipCard,
            styledLabel("Compliance Reports", 16, "#e2e8f0", true)
        );

        String[] reportTypes = {"Payroll Summary (PDF)", "TDS Report", "PF Report",
                                "Reimbursement Summary", "Audit Trail Export"};
        for (String r : reportTypes) {
            HBox card = new HBox(14);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(14, 20, 14, 20));
            card.setStyle("-fx-background-color: #1e2435; -fx-background-radius: 10; " +
                          "-fx-border-color: #2d3748; -fx-border-radius: 10;");
            Label lbl = styledLabel(r, 14, "#e2e8f0", false);
            Button dl  = ghostBtn("⬇ Download");
            dl.setDisable(payrollResults.isEmpty());
            dl.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, r,
                    payrollResults.isEmpty()
                        ? "No payroll has been run yet.\nRun a batch first."
                        : "Report generated: " + r + " — " + auditLogEntries.size() + " audit entries included."));
            card.getChildren().addAll(lbl, new Spacer(), dl);
            root.getChildren().add(card);
        }

        return root;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Settings View
    // ═══════════════════════════════════════════════════════════════════════════

    private Node createSettingsView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(28, 32, 28, 32));
        root.setStyle("-fx-background-color: #0f1117;");

        VBox card = glassCard();
        card.getChildren().addAll(
            styledLabel("System Settings", 16, "#e2e8f0", true),
            separator(),
            settingRow("Active Data Source",     presenter.getDbStatus()),
            settingRow("Active Pay Period",       currentPayPeriod),
            settingRow("Total Employees",         String.valueOf(presenter.getEmployeeCount())),
            settingRow("Audit Entries",           String.valueOf(presenter.getAuditLog().size())),
            separator(),
            styledLabel("To switch to real DB: replace MockPayrollRepository in PayrollPresenterImpl.",
                    12, "#475569", false)
        );

        root.getChildren().add(card);
        return root;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Layout Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private HBox buildTopBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 24, 14, 24));
        bar.setStyle("-fx-background-color: #151822; -fx-border-color: transparent transparent #1e2435 transparent;");

        Label logo = new Label("✦ Aether");
        logo.setFont(Font.font("Inter", FontWeight.BOLD, 18));
        logo.setTextFill(Color.web("#6366f1"));

        pageTitle = new Label("Dashboard");
        pageTitle.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 14));
        pageTitle.setTextFill(Color.web("#e2e8f0"));

        statusBar = new Label("System ready");
        statusBar.setFont(Font.font("Inter", 11));
        statusBar.setTextFill(Color.web("#475569"));

        dbStatusLabel = new Label(presenter.getDbStatus());
        dbStatusLabel.setFont(Font.font("Inter", 11));
        dbStatusLabel.setTextFill(Color.web("#22c55e"));

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        Label dateLbl = new Label(date);
        dateLbl.setFont(Font.font("Inter", 11));
        dateLbl.setTextFill(Color.web("#64748b"));

        Circle avatar = new Circle(14, Color.web("#6366f1"));
        Label avatarLbl = new Label("HR");
        avatarLbl.setFont(Font.font("Inter", FontWeight.BOLD, 10));
        avatarLbl.setTextFill(Color.WHITE);
        StackPane avatarStack = new StackPane(avatar, avatarLbl);

        bar.getChildren().addAll(logo, new Separator(javafx.geometry.Orientation.VERTICAL),
            pageTitle, new Spacer(), statusBar, dateLbl, dbStatusLabel, avatarStack);
        return bar;
    }

    private VBox buildSidebar() {
        VBox sb = new VBox(4);
        sb.setPrefWidth(200);
        sb.setPadding(new Insets(20, 12, 20, 12));
        sb.setStyle("-fx-background-color: #151822;");

        String[] items = {"Dashboard", "Employees", "Run Payroll", "Audit Log", "Reports", "Settings"};
        String[] icons = {"⊞", "👥", "▶", "📋", "📊", "⚙"};

        for (int i = 0; i < items.length; i++) {
            Button btn = navBtn(icons[i] + "  " + items[i]);
            final String viewName = items[i];
            btn.setOnAction(e -> {
                if (activeNavBtn != null) setNavBtnInactive(activeNavBtn);
                setNavBtnActive(btn);
                activeNavBtn = btn;
                showView(viewName);
            });
            if (i == 0) { setNavBtnActive(btn); activeNavBtn = btn; }
            sb.getChildren().add(btn);
        }

        return sb;
    }

    private void applyRootStyle() {
        root.setStyle("-fx-background-color: #0f1117;");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Widget Factories
    // ═══════════════════════════════════════════════════════════════════════════

    private VBox statCard(String title, String value, String color, String icon) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle("-fx-background-color: #1e2435; -fx-background-radius: 12; " +
                      "-fx-border-color: " + color + "40; -fx-border-radius: 12; -fx-border-width: 1;");
        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font(20));
        Label valLbl = styledLabel(value, 24, color, true);
        Label ttlLbl = styledLabel(title, 12, "#64748b", false);
        card.getChildren().addAll(iconLbl, valLbl, ttlLbl);
        return card;
    }

    private VBox glassCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setStyle("-fx-background-color: #1e2435; -fx-background-radius: 12; " +
                      "-fx-border-color: #2d3748; -fx-border-radius: 12; -fx-border-width: 1;");
        return card;
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10, 14, 10, 14));
        btn.setFont(Font.font("Inter", 13));
        setNavBtnInactive(btn);
        return btn;
    }

    private void setNavBtnActive(Button btn) {
        btn.setStyle("-fx-background-color: #6366f120; -fx-text-fill: #818cf8; " +
                     "-fx-background-radius: 8; -fx-border-color: transparent; -fx-cursor: hand;");
    }

    private void setNavBtnInactive(Button btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; " +
                     "-fx-background-radius: 8; -fx-border-color: transparent; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> {
            if (btn != activeNavBtn)
                btn.setStyle("-fx-background-color: #1e2435; -fx-text-fill: #94a3b8; " +
                             "-fx-background-radius: 8; -fx-border-color: transparent; -fx-cursor: hand;");
        });
        btn.setOnMouseExited(e -> {
            if (btn != activeNavBtn) setNavBtnInactive(btn);
        });
    }

    private Button primaryBtn(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 13));
        btn.setPadding(new Insets(10, 24, 10, 24));
        btn.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; " +
                     "-fx-background-radius: 8; -fx-cursor: hand;");
        btn.setOnMouseEntered(e ->
            btn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; " +
                         "-fx-background-radius: 8; -fx-cursor: hand;"));
        btn.setOnMouseExited(e ->
            btn.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; " +
                         "-fx-background-radius: 8; -fx-cursor: hand;"));
        return btn;
    }

    private Button ghostBtn(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Inter", 12));
        btn.setPadding(new Insets(7, 14, 7, 14));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; " +
                     "-fx-border-color: #2d3748; -fx-border-radius: 6; -fx-cursor: hand;");
        btn.setOnMouseEntered(e ->
            btn.setStyle("-fx-background-color: #1e2435; -fx-text-fill: #94a3b8; " +
                         "-fx-border-color: #4b5563; -fx-border-radius: 6; -fx-cursor: hand;"));
        btn.setOnMouseExited(e ->
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; " +
                         "-fx-border-color: #2d3748; -fx-border-radius: 6; -fx-cursor: hand;"));
        return btn;
    }

    private Button actionBtn(String text, String color, Runnable action) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 13));
        btn.setPadding(new Insets(12, 20, 12, 20));
        btn.setStyle("-fx-background-color: " + color + "20; -fx-text-fill: " + color + "; " +
                     "-fx-border-color: " + color + "60; -fx-border-radius: 8; " +
                     "-fx-background-radius: 8; -fx-cursor: hand;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private Label styledLabel(String text, int size, String color, boolean bold) {
        Label lbl = new Label(text);
        lbl.setFont(bold ? Font.font("Inter", FontWeight.BOLD, size) : Font.font("Inter", size));
        lbl.setTextFill(Color.web(color));
        return lbl;
    }

    private TextField formField(String prompt, String defaultValue) {
        TextField tf = new TextField(defaultValue);
        tf.setPromptText(prompt);
        tf.setFont(Font.font("Inter", 13));
        tf.setStyle("-fx-background-color: #252d3d; -fx-text-fill: #e2e8f0; " +
                    "-fx-border-color: #2d3748; -fx-border-radius: 6; -fx-background-radius: 6; " +
                    "-fx-pref-height: 36px;");
        return tf;
    }

    private VBox labeled(String label, TextField field) {
        VBox box = new VBox(6);
        box.getChildren().addAll(styledLabel(label, 12, "#64748b", false), field);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private <T> TableColumn<T, String> col(String title, String prop, double width) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(width);
        c.setStyle("-fx-alignment: CENTER-LEFT;");
        return c;
    }

    @SuppressWarnings("unchecked")
    private TableColumn<PayrollRowModel, String> statusCol() {
        TableColumn<PayrollRowModel, String> c = new TableColumn<>("Status");
        c.setCellValueFactory(new PropertyValueFactory<>("status"));
        c.setPrefWidth(180);
        c.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if (item.startsWith("✔"))
                    setTextFill(Color.web("#22c55e"));
                else if (item.startsWith("✗"))
                    setTextFill(Color.web("#ef4444"));
                else
                    setTextFill(Color.web("#f59e0b"));
                setStyle("-fx-background-color: transparent;");
            }
        });
        return c;
    }

    private HBox settingRow(String key, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
            styledLabel(key + ":", 13, "#94a3b8", false),
            new Spacer(),
            styledLabel(value, 13, "#e2e8f0", true)
        );
        return row;
    }

    private Separator separator() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2d3748;");
        return sep;
    }

    private String tableStyle() {
        return "-fx-background-color: #1e2435; -fx-border-color: #2d3748; " +
               "-fx-border-radius: 10; -fx-background-radius: 10; " +
               "-fx-table-cell-border-color: transparent;";
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void exportAllPayslips(TextArea payslipArea) {
        try {
            String text = presenter.getAllEmployeePayslips(currentPayPeriod);
            String csv = presenter.getAllEmployeePayslipsCsv(currentPayPeriod);

            Path exportDir = resolveExportDirectory();
            Files.createDirectories(exportDir);

            String suffix = currentPayPeriod.replaceAll("[^A-Za-z0-9_-]", "_");
            Path textPath = exportDir.resolve("all-payslips-" + suffix + ".txt");
            Path csvPath = exportDir.resolve("all-payslips-" + suffix + ".csv");

            Files.writeString(textPath, text, StandardCharsets.UTF_8);
            Files.writeString(csvPath, csv, StandardCharsets.UTF_8);

            payslipArea.setPrefHeight(640);
            payslipArea.setText(text);
            payslipArea.setVisible(true);
            payslipArea.setManaged(true);
            updateStatusBar("Exported all payslips to " + exportDir.toAbsolutePath());
            showAlert(Alert.AlertType.INFORMATION, "Payslips Exported",
                    "Text export:\n" + textPath.toAbsolutePath()
                            + "\n\nCSV export:\n" + csvPath.toAbsolutePath());
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Export Failed", ex.getMessage());
        }
    }

    private Path resolveExportDirectory() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path nestedProject = cwd.resolve("payroll-system");
        if (Files.exists(nestedProject.resolve("pom.xml"))) {
            return nestedProject.resolve("exports");
        }
        return cwd.resolve("exports");
    }

    private void updateStatusBar(String msg) {
        if (statusBar != null) Platform.runLater(() -> statusBar.setText(msg));
    }

    private String activeColor(String a, String b, boolean useA) {
        return useA ? a : b;
    }

    // ── Spacer utility ────────────────────────────────────────────────────────
    private static class Spacer extends Region {
        Spacer() { HBox.setHgrow(this, Priority.ALWAYS); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Inner Row Models (JavaFX TableView requires JavaBean-style properties)
    // ═══════════════════════════════════════════════════════════════════════════

    public static class EmployeeRowModel {
        private final SimpleStringProperty empID, name, department, grade,
                                           basicPay, country, taxRegime, state, yearsService,
                                           leaveWithoutPay, overtimeHours, pendingClaims, approvedReimbursement;

        public EmployeeRowModel(String empID, String name, String dept, String grade,
                String basicPay, String country, String taxRegime, String state, String years,
                String leaveWithoutPay, String overtimeHours, String pendingClaims,
                String approvedReimbursement) {
            this.empID       = new SimpleStringProperty(empID);
            this.name        = new SimpleStringProperty(name);
            this.department  = new SimpleStringProperty(dept);
            this.grade       = new SimpleStringProperty(grade);
            this.basicPay    = new SimpleStringProperty(basicPay);
            this.country     = new SimpleStringProperty(country);
            this.taxRegime   = new SimpleStringProperty(taxRegime);
            this.state       = new SimpleStringProperty(state);
            this.yearsService = new SimpleStringProperty(years);
            this.leaveWithoutPay = new SimpleStringProperty(leaveWithoutPay);
            this.overtimeHours = new SimpleStringProperty(overtimeHours);
            this.pendingClaims = new SimpleStringProperty(pendingClaims);
            this.approvedReimbursement = new SimpleStringProperty(approvedReimbursement);
        }

        public String getEmpID()        { return empID.get(); }
        public String getName()         { return name.get(); }
        public String getDepartment()   { return department.get(); }
        public String getGrade()        { return grade.get(); }
        public String getBasicPay()     { return basicPay.get(); }
        public String getCountry()      { return country.get(); }
        public String getTaxRegime()    { return taxRegime.get(); }
        public String getState()        { return state.get(); }
        public String getYearsService() { return yearsService.get(); }
        public String getLeaveWithoutPay() { return leaveWithoutPay.get(); }
        public String getOvertimeHours() { return overtimeHours.get(); }
        public String getPendingClaims() { return pendingClaims.get(); }
        public String getApprovedReimbursement() { return approvedReimbursement.get(); }
    }

    public static class PayrollRowModel {
        private final SimpleStringProperty recordID, empID, name, department,
                                           basicPay, pf, tds, pt, netPay, status;

        public PayrollRowModel(String recordID, String empID, String name, String dept,
                String basicPay, String pf, String tds, String pt, String netPay, String status) {
            this.recordID   = new SimpleStringProperty(recordID);
            this.empID      = new SimpleStringProperty(empID);
            this.name       = new SimpleStringProperty(name);
            this.department = new SimpleStringProperty(dept);
            this.basicPay   = new SimpleStringProperty(basicPay);
            this.pf         = new SimpleStringProperty(pf);
            this.tds        = new SimpleStringProperty(tds);
            this.pt         = new SimpleStringProperty(pt);
            this.netPay     = new SimpleStringProperty(netPay);
            this.status     = new SimpleStringProperty(status);
        }

        public String getRecordID()   { return recordID.get(); }
        public String getEmpID()      { return empID.get(); }
        public String getName()       { return name.get(); }
        public String getDepartment() { return department.get(); }
        public String getBasicPay()   { return basicPay.get(); }
        public String getPf()         { return pf.get(); }
        public String getTds()        { return tds.get(); }
        public String getPt()         { return pt.get(); }
        public String getNetPay()     { return netPay.get(); }
        public String getStatus()     { return status.get(); }
    }
}

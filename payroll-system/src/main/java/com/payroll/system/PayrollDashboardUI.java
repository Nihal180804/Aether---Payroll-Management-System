package com.payroll.system;

/**
 * =============================================================================
 * PayrollDashboardUI.java
 * =============================================================================
 * JavaFX frontend for the OCBC Payroll Management System.
 *
 * INTEGRATION POINTS (replace TODO comments when backend is ready):
 *   - MockPayrollRepository  → IPayrollRepository (swap one line in initRepository())
 *   - PayRunController       → called from handleRunPayroll()
 *   - PayrollFacade          → wired inside PayRunController, no direct UI call
 *   - AuditLogger            → getAllEntries() feeds the Audit Log tab
 *   - PayrollResultDTO       → populates the results TableView
 *
 * SCREENS:
 *   1. Dashboard   — stat cards + quick actions + system status
 *   2. Run Payroll — batch input form + live results table
 *   3. Employees   — searchable employee directory from mock DB
 *   4. Audit Log   — scrollable log from AuditLogger.getAllEntries()
 *
 * Team: Nehan Ahmad (PES1UG23AM184)
 * =============================================================================
 */

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PayrollDashboardUI extends Application {

    // ── Layout skeleton ────────────────────────────────────────────────────────
    private final BorderPane root = new BorderPane();
    private ScrollPane contentArea;
    private Button activeNavBtn = null;

    // ── Navigation view cache ─────────────────────────────────────────────────
    private final Map<String, Node> viewCache = new LinkedHashMap<>();

    // ── Shared state (populated by Run Payroll) ────────────────────────────────
    private final ObservableList<PayrollRowModel> payrollResults = FXCollections.observableArrayList();
    private final ObservableList<String> auditLogEntries = FXCollections.observableArrayList();
    private final ObservableList<EmployeeRowModel> employeeList = FXCollections.observableArrayList();

    // ── Status bar label (updated during batch run) ────────────────────────────
    private Label statusBarLabel;

    // ── Color palette (consistent across all views) ────────────────────────────
    private static final String C_BG = "#f0f2f8";
    private static final String C_SURFACE = "#ffffff";
    private static final String C_PRIMARY = "#4f46e5"; // Indigo
    private static final String C_PRIMARY_LT = "#ede9fe";
    private static final String C_SUCCESS = "#16a34a";
    private static final String C_WARNING = "#d97706";
    private static final String C_DANGER = "#dc2626";
    private static final String C_TEXT_HEAD = "#1e1b4b";
    private static final String C_TEXT_BODY = "#6b7280";
    private static final String C_BORDER = "#e5e7eb";
    private static final String C_SIDEBAR = "#1e1b4b"; // Deep navy

    // ==========================================================================
    // APPLICATION ENTRY
    // ==========================================================================

    @Override
    public void start(Stage stage) {
        loadMockEmployees(); // TODO: replace with repo.getAllActiveEmployeeIDs() + fetchEmployeeData()

        // Build all views upfront and cache them
        viewCache.put("Dashboard", createDashboardView());
        viewCache.put("Run Payroll", createRunPayrollView());
        viewCache.put("Employees", createEmployeesView());
        viewCache.put("Audit Log", createAuditLogView());

        // Main layout
        root.setStyle("-fx-background-color: " + C_BG + ";");
        root.setLeft(createSidebar());

        contentArea = new ScrollPane();
        contentArea.setFitToWidth(true);
        contentArea.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentArea.setStyle("-fx-background-color: transparent; -fx-background: " + C_BG + ";");
        root.setCenter(contentArea);

        root.setBottom(createStatusBar());

        // Default to Dashboard
        Button dashBtn = findNavButton("Dashboard");
        if (dashBtn != null)
            switchView(dashBtn, "Dashboard");

        Scene scene = new Scene(root, 1320, 820);
        stage.setScene(scene);
        stage.setTitle("OCBC Payroll Management System");
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();
    }

    // ==========================================================================
    // SIDEBAR NAVIGATION
    // ==========================================================================

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: " + C_SIDEBAR + ";");

        // ── Logo block ─────────────────────────────────────────────────────────
        VBox logoBlock = new VBox(4);
        logoBlock.setPadding(new Insets(28, 20, 24, 20));
        logoBlock.setStyle("-fx-border-color: transparent transparent #2d2a5e transparent;");

        Label logoMain = new Label("OCBC");
        logoMain.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label logoSub = new Label("Payroll Management");
        logoSub.setStyle("-fx-text-fill: #a5b4fc; -fx-font-size: 11px;");
        logoBlock.getChildren().addAll(logoMain, logoSub);

        // ── Navigation section label ────────────────────────────────────────────
        Label navLabel = new Label("MAIN MENU");
        navLabel.setStyle(
                "-fx-text-fill: #6366f1; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 20 20 8 20;");

        // ── Nav buttons ────────────────────────────────────────────────────────
        VBox navMenu = new VBox(4);
        navMenu.setPadding(new Insets(0, 12, 0, 12));

        String[][] navItems = {
                { "Dashboard", "⬜" },
                { "Run Payroll", "▶" },
                { "Employees", "👥" },
                { "Audit Log", "📋" }
        };

        for (String[] item : navItems) {
            navMenu.getChildren().add(createNavButton(item[0], item[1]));
        }

        // ── Spacer ─────────────────────────────────────────────────────────────
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // ── Footer info block ──────────────────────────────────────────────────
        VBox footer = new VBox(6);
        footer.setPadding(new Insets(16, 16, 24, 16));
        footer.setStyle("-fx-border-color: #2d2a5e transparent transparent transparent;");

        Label teamLabel = new Label("Team OCBC");
        teamLabel.setStyle("-fx-text-fill: #a5b4fc; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label sectionLabel = new Label("Section D  •  Payroll Subsystem");
        sectionLabel.setStyle("-fx-text-fill: #6366f1; -fx-font-size: 10px;");

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setStyle(
                "-fx-background-color: #2d2a5e; -fx-text-fill: #a5b4fc; " +
                        "-fx-background-radius: 8; -fx-padding: 8 0; -fx-font-size: 12px; -fx-cursor: hand;");
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle(
                "-fx-background-color: #3730a3; -fx-text-fill: white; " +
                        "-fx-background-radius: 8; -fx-padding: 8 0; -fx-font-size: 12px; -fx-cursor: hand;"));
        logoutBtn.setOnMouseExited(e -> logoutBtn.setStyle(
                "-fx-background-color: #2d2a5e; -fx-text-fill: #a5b4fc; " +
                        "-fx-background-radius: 8; -fx-padding: 8 0; -fx-font-size: 12px; -fx-cursor: hand;"));
        logoutBtn.setOnAction(e -> Platform.exit());

        footer.getChildren().addAll(teamLabel, sectionLabel, logoutBtn);
        sidebar.getChildren().addAll(logoBlock, navLabel, navMenu, spacer, footer);
        return sidebar;
    }

    /** Creates a styled sidebar navigation button. */
    private Button createNavButton(String label, String icon) {
        Button btn = new Button(icon + "  " + label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(11, 16, 11, 16));
        btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #a5b4fc; " +
                        "-fx-font-size: 13px; -fx-background-radius: 10; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> {
            if (btn != activeNavBtn)
                btn.setStyle("-fx-background-color: #2d2a5e; -fx-text-fill: white; " +
                        "-fx-font-size: 13px; -fx-background-radius: 10; -fx-cursor: hand;");
        });
        btn.setOnMouseExited(e -> {
            if (btn != activeNavBtn)
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a5b4fc; " +
                        "-fx-font-size: 13px; -fx-background-radius: 10; -fx-cursor: hand;");
        });
        btn.setOnAction(e -> switchView(btn, label));
        btn.setUserData(label); // store view key for lookup
        return btn;
    }

    /** Switches the main content area to the named view. */
    private void switchView(Button btn, String viewKey) {
        // Reset previously active button
        if (activeNavBtn != null) {
            activeNavBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #a5b4fc; " +
                            "-fx-font-size: 13px; -fx-background-radius: 10; -fx-cursor: hand;");
        }
        activeNavBtn = btn;
        activeNavBtn.setStyle(
                "-fx-background-color: " + C_PRIMARY + "; -fx-text-fill: white; " +
                        "-fx-font-size: 13px; -fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand;");

        // Refresh audit log content every time it's opened
        if ("Audit Log".equals(viewKey)) {
            viewCache.put("Audit Log", createAuditLogView());
        }

        contentArea.setContent(viewCache.getOrDefault(viewKey, viewCache.get("Dashboard")));
        updateStatus("Viewing: " + viewKey);
    }

    /** Finds a nav button by its stored view key. */
    private Button findNavButton(String viewKey) {
        VBox sidebar = (VBox) root.getLeft();
        for (Node child : sidebar.getChildren()) {
            if (child instanceof VBox navMenu) {
                for (Node item : navMenu.getChildren()) {
                    if (item instanceof Button b && viewKey.equals(b.getUserData()))
                        return b;
                }
            }
        }
        return null;
    }

    // ==========================================================================
    // STATUS BAR
    // ==========================================================================

    private HBox createStatusBar() {
        HBox bar = new HBox(20);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 20, 6, 20));
        bar.setStyle("-fx-background-color: " + C_SIDEBAR + ";");

        statusBarLabel = new Label("Ready");
        statusBarLabel.setStyle("-fx-text-fill: #a5b4fc; -fx-font-size: 11px;");

        Label version = new Label("OCBC Payroll v1.0  •  PES University Section D");
        version.setStyle("-fx-text-fill: #4338ca; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label time = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm")));
        time.setStyle("-fx-text-fill: #6366f1; -fx-font-size: 11px;");

        bar.getChildren().addAll(statusBarLabel, spacer, version, new Label("  "), time);
        return bar;
    }

    private void updateStatus(String msg) {
        if (statusBarLabel != null)
            statusBarLabel.setText(msg);
    }

    // ==========================================================================
    // VIEW 1 — DASHBOARD
    // ==========================================================================

    private Node createDashboardView() {
        VBox container = contentBase("Dashboard", "Welcome back, Nihal. Here's the system overview.");

        // ── Stat cards row ─────────────────────────────────────────────────────
        HBox stats = new HBox(16,
                statCard("Total Employees", String.valueOf(employeeList.size() > 0 ? employeeList.size() : 10),
                        "👥", C_PRIMARY),
                statCard("Last Batch", "2025-06", "📅", C_SUCCESS),
                statCard("Exceptions", "3 warnings", "⚠", C_WARNING),
                statCard("DB Status", "Mock Active", "🔌", C_TEXT_BODY));
        stats.setFillHeight(true);

        // ── Main grid ─────────────────────────────────────────────────────────
        GridPane grid = grid(3);

        // Quick actions card
        VBox actions = panel("Quick Actions");
        VBox actionList = new VBox(10);
        actionList.getChildren().addAll(
                actionButton("▶  Run Payroll Batch", C_PRIMARY, () -> navigateTo("Run Payroll")),
                actionButton("👥  View Employees", "#0891b2", () -> navigateTo("Employees")),
                actionButton("📋  Open Audit Log", "#059669", () -> navigateTo("Audit Log")));
        actions.getChildren().add(actionList);

        // System status card
        VBox sysStatus = panel("System Status");
        VBox statusList = new VBox(10);
        statusList.getChildren().addAll(
                statusRow("Mock DB", "Connected", true),
                statusRow("IPayrollRepository", "Bound", true),
                statusRow("PayRunController", "Ready", true),
                statusRow("AuditLogger", "Active", true),
                statusRow("Real DB", "Pending", false));
        sysStatus.getChildren().add(statusList);

        // Exception summary card
        VBox excSummary = panel("Exception Coverage");
        VBox excList = new VBox(8);
        String[][] exceptions = {
                { "MISSING_TAX_REGIME", "WARNING", C_WARNING },
                { "MISSING_WORK_STATE", "MAJOR", C_DANGER },
                { "EXCEEDS_CLAIM_LIMIT", "WARNING", C_WARNING },
                { "NEGATIVE_NET_PAY", "MAJOR", C_DANGER },
                { "MISSING_PERFORMANCE_RATING", "WARNING", C_WARNING },
                { "INVALID_PAY_PERIOD", "MAJOR", C_DANGER },
                { "PAYSLIP_GENERATION_FAILED", "WARNING", C_WARNING }
        };
        for (String[] ex : exceptions) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            Label name = new Label(ex[0]);
            name.setStyle("-fx-font-size: 11px; -fx-text-fill: " + C_TEXT_HEAD + ";");
            HBox.setHgrow(name, Priority.ALWAYS);
            Label badge = badge(ex[1], ex[2]);
            row.getChildren().addAll(name, badge);
            excList.getChildren().add(row);
        }
        excSummary.getChildren().add(excList);

        grid.add(actions, 0, 0);
        grid.add(sysStatus, 1, 0);
        grid.add(excSummary, 2, 0);

        // Recent activity panel (full width)
        VBox recentActivity = panel("Recent Activity");
        VBox activityList = new VBox(8);
        String[][] activities = {
                { "✔", "Mock repository loaded with 10 employees", C_SUCCESS },
                { "✔", "IPayrollRepository interface bound successfully", C_SUCCESS },
                { "⚠", "Real DB integration pending from DB team", C_WARNING },
                { "ℹ", "Run Payroll tab ready — enter BatchID to begin", C_PRIMARY }
        };
        for (String[] a : activities) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 8;");
            Label icon = new Label(a[0]);
            icon.setStyle("-fx-text-fill: " + a[2] + "; -fx-font-size: 13px;");
            Label msg = new Label(a[1]);
            msg.setStyle("-fx-text-fill: " + C_TEXT_BODY + "; -fx-font-size: 12px;");
            row.getChildren().addAll(icon, msg);
            activityList.getChildren().add(row);
        }
        recentActivity.getChildren().add(activityList);

        container.getChildren().addAll(stats, grid, recentActivity);
        return container;
    }

    // ==========================================================================
    // VIEW 2 — RUN PAYROLL
    // ==========================================================================

    private Node createRunPayrollView() {
        VBox container = contentBase("Run Payroll", "Configure and execute a payroll batch run.");

        // ── Batch configuration form ───────────────────────────────────────────
        VBox formPanel = panel("Batch Configuration");
        GridPane form = new GridPane();
        form.setHgap(20);
        form.setVgap(14);

        TextField batchIDField = styledField("BATCH-2025-06", 250);
        TextField payPeriodField = styledField("2025-06", 150);
        ComboBox<String> modeBox = new ComboBox<>(
                FXCollections.observableArrayList("Full Batch (All Employees)", "Single Employee", "Department Only"));
        modeBox.setValue("Full Batch (All Employees)");
        modeBox.setStyle("-fx-background-radius: 8; -fx-padding: 6 10;");
        modeBox.setPrefWidth(250);

        form.add(formLabel("Batch ID"), 0, 0);
        form.add(batchIDField, 1, 0);
        form.add(formLabel("Pay Period"), 0, 1);
        form.add(payPeriodField, 1, 1);
        form.add(formLabel("Run Mode"), 0, 2);
        form.add(modeBox, 1, 2);

        // ── Action buttons ─────────────────────────────────────────────────────
        HBox buttonRow = new HBox(12);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        buttonRow.setPadding(new Insets(8, 0, 0, 0));

        Button runBtn = primaryButton("▶  Run Payroll Batch");
        Button clearBtn = secondaryButton("✕  Clear Results");

        // Progress indicator (hidden until run starts)
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressBar.setVisible(false);
        progressBar.setStyle("-fx-accent: " + C_PRIMARY + ";");

        Label runStatusLabel = new Label("");
        runStatusLabel.setStyle("-fx-text-fill: " + C_TEXT_BODY + "; -fx-font-size: 12px;");

        buttonRow.getChildren().addAll(runBtn, clearBtn, progressBar, runStatusLabel);
        formPanel.getChildren().addAll(form, buttonRow);

        // ── Results table ──────────────────────────────────────────────────────
        VBox resultsPanel = panel("Payroll Results");
        Label resultsHint = new Label("Results will appear here after running a batch.");
        resultsHint.setStyle("-fx-text-fill: " + C_TEXT_BODY + "; -fx-font-size: 12px;");

        TableView<PayrollRowModel> table = createPayrollTable();
        table.setItems(payrollResults);
        table.setVisible(false);
        table.setPrefHeight(350);

        resultsPanel.getChildren().addAll(resultsHint, table);

        // ── Wire up Run button ─────────────────────────────────────────────────
        runBtn.setOnAction(e -> handleRunPayroll(
                batchIDField.getText().trim(),
                payPeriodField.getText().trim(),
                progressBar, runStatusLabel, resultsHint, table));

        clearBtn.setOnAction(e -> {
            payrollResults.clear();
            table.setVisible(false);
            resultsHint.setVisible(true);
            progressBar.setVisible(false);
            runStatusLabel.setText("");
            updateStatus("Results cleared.");
        });

        container.getChildren().addAll(formPanel, resultsPanel);
        return container;
    }

    /**
     * Handles the Run Payroll button click.
     *
     * TODO — INTEGRATION:
     * Replace the mock simulation below with real calls:
     * IPayrollRepository repo = new MockPayrollRepository(); // or
     * RealPayrollRepository
     * List<String> ids = repo.getAllActiveEmployeeIDs();
     * PayRunController controller = new PayRunController(batchID, payPeriod,
     * facade, auditLogger);
     * List<PayrollRecord> records = controller.executeBatchPayroll(employees);
     * // Then map each PayrollRecord → PayrollRowModel and add to payrollResults
     */
    private void handleRunPayroll(String batchID, String payPeriod,
            ProgressBar progress, Label statusLabel,
            Label hint, TableView<PayrollRowModel> table) {

        // Basic validation
        if (batchID.isEmpty() || payPeriod.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Fields",
                    "Please enter both a Batch ID and Pay Period before running.");
            return;
        }

        // Run in background thread so UI doesn't freeze
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0.1, 1.0);
                Platform.runLater(() -> {
                    progress.setVisible(true);
                    progress.setProgress(0.1);
                    statusLabel.setText("Validating pay period...");
                    updateStatus("Running batch: " + batchID);
                });

                Thread.sleep(400); // Simulates DB validation

                // ── TODO: Replace this block with real PayRunController call ──
                // Simulate processing 10 mock employees
                List<PayrollRowModel> mockResults = generateMockResults(batchID, payPeriod);
                // ─────────────────────────────────────────────────────────────

                updateProgress(0.5, 1.0);
                Platform.runLater(() -> {
                    progress.setProgress(0.5);
                    statusLabel.setText("Processing employees...");
                });

                Thread.sleep(600);

                Platform.runLater(() -> {
                    progress.setProgress(1.0);
                    statusLabel.setStyle("-fx-text-fill: " + C_SUCCESS + "; -fx-font-size: 12px;");
                    statusLabel.setText("✔  Batch complete — " + mockResults.size() + " records processed.");
                    payrollResults.setAll(mockResults);
                    hint.setVisible(false);
                    table.setVisible(true);
                    updateStatus("Batch " + batchID + " complete.");

                    // Add to audit log
                    auditLogEntries.add("[AUDIT] " + now() + " | Batch=" + batchID
                            + " | Period=" + payPeriod + " | Processed=" + mockResults.size() + " employees");
                });

                return null;
            }
        };

        new Thread(task).start();
    }

    /**
     * Generates mock PayrollRowModel data.
     * TODO: Remove this method when PayRunController integration is complete.
     * Replace with: records.stream().map(PayrollRowModel::fromRecord).collect(...)
     */
    private List<PayrollRowModel> generateMockResults(String batchID, String payPeriod) {
        List<PayrollRowModel> list = new ArrayList<>();
        Object[][] data = {
                { "EMP001", "Arjun Sharma", "Engineering", "₹80,000", "₹9,600", "₹8,800", "₹200", "₹65,400", "✔ OK" },
                { "EMP002", "Priya Nair", "Finance", "₹55,000", "₹6,600", "₹5,033", "₹200",
                        "⚠ MISSING_TAX_REGIME → defaulted OLD", "⚠" },
                { "EMP003", "Ravi Kumar", "HR", "₹35,000", "—", "—", "—", "✗ MISSING_WORK_STATE → skipped", "✗" },
                { "EMP004", "Meera Iyer", "Operations", "₹52,000", "₹6,240", "₹5,200", "₹100",
                        "⚠ EXCEEDS_CLAIM_LIMIT → capped", "⚠" },
                { "EMP005", "Li Wei", "Product", "S$8,500", "—", "S$612", "—", "✔ Singapore strategy applied", "✔ OK" },
                { "EMP006", "John Mitchell", "Sales", "$7,500", "—", "$583", "—", "✔ US Federal (Married) applied",
                        "✔ OK" },
                { "EMP007", "Suresh Babu", "Logistics", "₹22,000", "₹2,640", "₹350", "₹150",
                        "⚠ NEGATIVE_NET_PAY → arrears set", "⚠" },
                { "EMP008", "Ananya Krishnan", "Design", "₹45,000", "₹5,400", "₹3,625", "₹200",
                        "⚠ MISSING_PERFORMANCE_RATING → bonus=0", "⚠" },
                { "EMP009", "Vikram Malhotra", "Engineering", "₹1,50,000", "₹18,000", "₹22,916", "₹200",
                        "✔ Gratuity provision included", "✔ OK" },
                { "EMP010", "Divya Reddy", "Support", "₹28,000", "₹3,360", "₹1,933", "₹150", "✔ OK", "✔ OK" },
        };
        for (Object[] row : data) {
            list.add(new PayrollRowModel(
                    batchID + "-" + row[0],
                    (String) row[0], (String) row[1], (String) row[2],
                    (String) row[3], (String) row[4], (String) row[5],
                    (String) row[6], (String) row[7], (String) row[8]));
        }
        return list;
    }

    /** Builds the payroll results TableView with all relevant columns. */
    @SuppressWarnings("unchecked")
    private TableView<PayrollRowModel> createPayrollTable() {
        TableView<PayrollRowModel> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: transparent;");

        TableColumn<PayrollRowModel, String> colEmp = col("Emp ID", "empID", 80);
        TableColumn<PayrollRowModel, String> colName = col("Name", "name", 130);
        TableColumn<PayrollRowModel, String> colDept = col("Department", "department", 110);
        TableColumn<PayrollRowModel, String> colBasic = col("Basic Pay", "basicPay", 100);
        TableColumn<PayrollRowModel, String> colPF = col("PF", "pf", 80);
        TableColumn<PayrollRowModel, String> colTDS = col("TDS", "tds", 90);
        TableColumn<PayrollRowModel, String> colPT = col("PT", "pt", 70);
        TableColumn<PayrollRowModel, String> colNet = col("Net Pay", "netPay", 100);
        TableColumn<PayrollRowModel, String> colStatus = col("Status", "status", 200);

        // Colour-code the status column
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                if (item.startsWith("✔"))
                    setStyle("-fx-text-fill: " + C_SUCCESS + "; -fx-font-size: 11px;");
                else if (item.startsWith("⚠"))
                    setStyle("-fx-text-fill: " + C_WARNING + "; -fx-font-size: 11px;");
                else if (item.startsWith("✗"))
                    setStyle("-fx-text-fill: " + C_DANGER + "; -fx-font-size: 11px;");
                else
                    setStyle("-fx-font-size: 11px;");
            }
        });

        table.getColumns().addAll(colEmp, colName, colDept, colBasic, colPF, colTDS, colPT, colNet, colStatus);
        return table;
    }

    // ==========================================================================
    // VIEW 3 — EMPLOYEES
    // ==========================================================================

    private Node createEmployeesView() {
        VBox container = contentBase("Employee Directory",
                "All active employees loaded from MockPayrollRepository.");

        // ── Search + filter row ────────────────────────────────────────────────
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchBox = new TextField();
        searchBox.setPromptText("Search by name, department, grade...");
        searchBox.setPrefWidth(320);
        searchBox.setStyle("-fx-background-radius: 8; -fx-padding: 8 12; " +
                "-fx-border-color: " + C_BORDER + "; -fx-border-radius: 8;");

        Region s = new Region();
        HBox.setHgrow(s, Priority.ALWAYS);

        Label countLabel = new Label(employeeList.size() + " employees");
        countLabel.setStyle("-fx-text-fill: " + C_TEXT_BODY + "; -fx-font-size: 12px;");

        topBar.getChildren().addAll(searchBox, s, countLabel);

        // ── Employee table ─────────────────────────────────────────────────────
        VBox tablePanel = panel("All Records");

        TableView<EmployeeRowModel> empTable = new TableView<>();
        empTable.setItems(employeeList);
        empTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        empTable.setPrefHeight(420);

        empTable.getColumns().addAll(
                col("Emp ID", "empID", 80),
                col("Name", "name", 140),
                col("Department", "department", 120),
                col("Grade", "grade", 70),
                col("Basic Pay", "basicPay", 100),
                col("Country", "country", 80),
                col("Tax Regime", "taxRegime", 90),
                col("State", "state", 110),
                col("Years Svc", "yearsService", 80));

        // Live search filter
        searchBox.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                empTable.setItems(employeeList);
            } else {
                String lower = val.toLowerCase();
                ObservableList<EmployeeRowModel> filtered = FXCollections.observableArrayList();
                for (EmployeeRowModel e : employeeList) {
                    if (e.getName().toLowerCase().contains(lower)
                            || e.getDepartment().toLowerCase().contains(lower)
                            || e.getGrade().toLowerCase().contains(lower))
                        filtered.add(e);
                }
                empTable.setItems(filtered);
            }
        });

        tablePanel.getChildren().add(empTable);
        container.getChildren().addAll(topBar, tablePanel);
        return container;
    }

    // ==========================================================================
    // VIEW 4 — AUDIT LOG
    // ==========================================================================

    private Node createAuditLogView() {
        VBox container = contentBase("Audit Log",
                "All actions logged by AuditLogger during the last batch run.");

        VBox logPanel = panel("Log Entries  (" + auditLogEntries.size() + " entries)");

        // ── Log entries list ───────────────────────────────────────────────────
        ListView<String> logView = new ListView<>(auditLogEntries);
        logView.setPrefHeight(500);
        logView.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 10;");

        // Colour-code each log line by type
        logView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #0f172a;");
                    return;
                }
                setText(item);
                setStyle("-fx-font-family: monospace; -fx-font-size: 11px; " +
                        "-fx-background-color: #0f172a; -fx-padding: 4 12;");
                if (item.contains("[ERROR]"))
                    setStyle(getStyle() + "-fx-text-fill: #f87171;");
                else if (item.contains("[WARN]"))
                    setStyle(getStyle() + "-fx-text-fill: #fbbf24;");
                else if (item.contains("[AUDIT]"))
                    setStyle(getStyle() + "-fx-text-fill: #86efac;");
                else
                    setStyle(getStyle() + "-fx-text-fill: #94a3b8;");
            }
        });

        // Empty state
        if (auditLogEntries.isEmpty()) {
            Label empty = new Label("No log entries yet. Run a payroll batch to populate this view.");
            empty.setStyle("-fx-text-fill: " + C_TEXT_BODY + "; -fx-font-size: 12px;");
            logPanel.getChildren().add(empty);
        } else {
            logPanel.getChildren().add(logView);
        }

        // ── Action buttons ─────────────────────────────────────────────────────
        HBox btnRow = new HBox(12);
        Button clearLog = secondaryButton("Clear Log");
        Button addSample = secondaryButton("+ Add Sample Entries");

        clearLog.setOnAction(e -> {
            auditLogEntries.clear();
            viewCache.put("Audit Log", createAuditLogView());
            switchView(activeNavBtn, "Audit Log");
        });

        addSample.setOnAction(e -> {
            // TODO: Replace with auditLogger.getAllEntries() when backend is wired
            auditLogEntries.addAll(
                    "[AUDIT] " + now() + " | EmpID=EMP001 | Action=TDS_CALCULATED | By=SYSTEM",
                    "[AUDIT] " + now() + " | EmpID=EMP001 | Action=PAYSLIP_GENERATED | By=SYSTEM",
                    "[WARN]  " + now() + " | EmpID=EMP002 | MISSING_TAX_REGIME → defaulted to OLD",
                    "[ERROR] " + now() + " | EmpID=EMP003 | MAJOR → MISSING_WORK_STATE, employee skipped",
                    "[WARN]  " + now() + " | EmpID=EMP004 | EXCEEDS_CLAIM_LIMIT → capped at grade limit",
                    "[WARN]  " + now() + " | EmpID=EMP007 | NEGATIVE_NET_PAY → arrears carried forward",
                    "[AUDIT] " + now() + " | Batch=BATCH-2025-06 | Action=BATCH_COMPLETE | By=SYSTEM");
            viewCache.put("Audit Log", createAuditLogView());
            switchView(activeNavBtn, "Audit Log");
        });

        btnRow.getChildren().addAll(clearLog, addSample);
        container.getChildren().addAll(logPanel, btnRow);
        return container;
    }

    // ==========================================================================
    // MOCK DATA LOADING
    // ==========================================================================

    /**
     * Loads employee display data from hardcoded mock.
     *
     * TODO — INTEGRATION:
     * Replace this with:
     * IPayrollRepository repo = new MockPayrollRepository();
     * for (String id : repo.getAllActiveEmployeeIDs()) {
     * PayrollDataPackage pkg = repo.fetchEmployeeData(id, "2025-06");
     * employeeList.add(EmployeeRowModel.fromPackage(pkg));
     * }
     */
    private void loadMockEmployees() {
        Object[][] data = {
                { "EMP001", "Arjun Sharma", "Engineering", "L3", "₹80,000", "IN", "NEW", "Karnataka", 6 },
                { "EMP002", "Priya Nair", "Finance", "L2", "₹55,000", "IN", "", "Maharashtra", 3 },
                { "EMP003", "Ravi Kumar", "Human Resources", "L1", "₹35,000", "IN", "OLD", "", 1 },
                { "EMP004", "Meera Iyer", "Operations", "L2", "₹52,000", "IN", "OLD", "Tamil Nadu", 4 },
                { "EMP005", "Li Wei", "Product", "L4", "S$8,500", "SG", "SG_STD", "Singapore", 7 },
                { "EMP006", "John Mitchell", "Sales", "L3", "$7,500", "US", "FEDERAL", "California", 5 },
                { "EMP007", "Suresh Babu", "Logistics", "L1", "₹22,000", "IN", "OLD", "West Bengal", 2 },
                { "EMP008", "Ananya Krishnan", "Design", "CONTRACT", "₹45,000", "IN", "NEW", "Karnataka", 2 },
                { "EMP009", "Vikram Malhotra", "Engineering", "L5", "₹1,50,000", "IN", "OLD", "Maharashtra", 9 },
                { "EMP010", "Divya Reddy", "Customer Support", "L1", "₹28,000", "IN", "OLD", "Andhra Pradesh", 1 },
        };
        for (Object[] d : data) {
            employeeList.add(new EmployeeRowModel(
                    (String) d[0], (String) d[1], (String) d[2], (String) d[3],
                    (String) d[4], (String) d[5], (String) d[6], (String) d[7],
                    String.valueOf(d[8])));
        }
    }

    // ==========================================================================
    // REUSABLE UI COMPONENT HELPERS
    // ==========================================================================

    /** Standard page content wrapper with title + subtitle. */
    private VBox contentBase(String title, String subtitle) {
        VBox box = new VBox(20);
        box.setPadding(new Insets(32, 40, 32, 40));

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + C_TEXT_HEAD + ";");
        Label s = new Label(subtitle);
        s.setStyle("-fx-text-fill: " + C_TEXT_BODY + "; -fx-font-size: 13px;");

        VBox header = new VBox(4, t, s);
        box.getChildren().add(header);
        return box;
    }

    /** White rounded card panel with a bold title. */
    private VBox panel(String title) {
        VBox p = new VBox(14);
        p.setPadding(new Insets(20));
        p.setStyle("-fx-background-color: " + C_SURFACE + "; -fx-background-radius: 14;");
        p.setEffect(new DropShadow(12, 0, 3, Color.color(0, 0, 0, 0.06)));

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + C_TEXT_HEAD + ";");

        // Subtle colour accent bar at top
        Rectangle bar = new Rectangle(36, 3);
        bar.setFill(Color.web(C_PRIMARY));
        bar.setArcWidth(3);
        bar.setArcHeight(3);

        p.getChildren().addAll(bar, t);
        return p;
    }

    /** 4-field stat card. */
    private VBox statCard(String title, String value, String icon, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle("-fx-background-color: " + C_SURFACE + "; -fx-background-radius: 14;");
        card.setEffect(new DropShadow(10, 0, 3, Color.color(0, 0, 0, 0.05)));

        Label i = new Label(icon);
        i.setStyle("-fx-font-size: 18px; -fx-background-color: " + C_PRIMARY_LT + "; " +
                "-fx-padding: 8 10; -fx-background-radius: 10; -fx-text-fill: " + color + ";");

        Label t = new Label(title.toUpperCase());
        t.setStyle("-fx-text-fill: " + C_TEXT_BODY + "; -fx-font-size: 10px; -fx-font-weight: bold;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + C_TEXT_HEAD + ";");

        card.getChildren().addAll(i, t, v);
        return card;
    }

    /** Indigo primary action button. */
    private Button primaryButton(String text) {
        Button b = new Button(text);
        b.setPadding(new Insets(10, 22, 10, 22));
        b.setStyle("-fx-background-color: " + C_PRIMARY + "; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #3730a3; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: " + C_PRIMARY + "; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand;"));
        return b;
    }

    /** Ghost secondary button. */
    private Button secondaryButton(String text) {
        Button b = new Button(text);
        b.setPadding(new Insets(9, 18, 9, 18));
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: " + C_PRIMARY + "; " +
                "-fx-border-color: " + C_BORDER + "; -fx-border-radius: 10; -fx-background-radius: 10; " +
                "-fx-cursor: hand;");
        b.setOnMouseEntered(
                e -> b.setStyle("-fx-background-color: " + C_PRIMARY_LT + "; -fx-text-fill: " + C_PRIMARY + "; " +
                        "-fx-border-color: " + C_PRIMARY + "; -fx-border-radius: 10; " +
                        "-fx-background-radius: 10; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: " + C_PRIMARY + "; " +
                "-fx-border-color: " + C_BORDER + "; -fx-border-radius: 10; " +
                "-fx-background-radius: 10; -fx-cursor: hand;"));
        return b;
    }

    /** Full-width action button for quick action list. */
    private Button actionButton(String text, String color, Runnable action) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPadding(new Insets(11, 16, 11, 16));
        b.setAlignment(Pos.CENTER_LEFT);
        b.setStyle("-fx-background-color: " + color + "18; -fx-text-fill: " + color + "; " +
                "-fx-background-radius: 10; -fx-font-size: 13px; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-font-size: 13px; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: " + color + "18; -fx-text-fill: " + color + "; " +
                "-fx-background-radius: 10; -fx-font-size: 13px; -fx-cursor: hand;"));
        b.setOnAction(e -> action.run());
        return b;
    }

    /** Status row with green/red indicator dot. */
    private HBox statusRow(String label, String value, boolean ok) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: " + (ok ? C_SUCCESS : C_WARNING) + "; -fx-font-size: 10px;");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: " + C_TEXT_HEAD + "; -fx-font-size: 12px;");
        lbl.setPrefWidth(160);

        Label val = new Label(value);
        val.setStyle(
                "-fx-text-fill: " + (ok ? C_SUCCESS : C_WARNING) + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        row.getChildren().addAll(dot, lbl, val);
        return row;
    }

    /** Coloured badge label. */
    private Label badge(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + color + "20; -fx-text-fill: " + color + "; " +
                "-fx-padding: 2 8; -fx-background-radius: 6; -fx-font-size: 10px; -fx-font-weight: bold;");
        return l;
    }

    /** Standard form label. */
    private Label formLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + C_TEXT_BODY + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        l.setPrefWidth(100);
        return l;
    }

    /** Styled text field. */
    private TextField styledField(String prompt, double width) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setText(prompt);
        f.setPrefWidth(width);
        f.setStyle("-fx-background-radius: 8; -fx-padding: 8 12; " +
                "-fx-border-color: " + C_BORDER + "; -fx-border-radius: 8;");
        return f;
    }

    /** Equal-column GridPane. */
    private GridPane grid(int cols) {
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(16);
        for (int i = 0; i < cols; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100.0 / cols);
            g.getColumnConstraints().add(c);
        }
        return g;
    }

    /** Generic TableColumn factory. */
    private <T> TableColumn<T, String> col(String header, String property, double minWidth) {
        TableColumn<T, String> c = new TableColumn<>(header);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setMinWidth(minWidth);
        c.setStyle("-fx-font-size: 12px;");
        return c;
    }

    /** Navigates to a named view programmatically. */
    private void navigateTo(String viewKey) {
        Button btn = findNavButton(viewKey);
        if (btn != null)
            switchView(btn, viewKey);
    }

    /** Alert helper. */
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.show();
    }

    /** Current timestamp string. */
    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // ==========================================================================
    // DATA MODELS (inner classes — one per TableView)
    // ==========================================================================

    /**
     * Row model for the payroll results TableView.
     *
     * TODO — INTEGRATION:
     * Add a static factory method:
     * public static PayrollRowModel fromRecord(PayrollRecord r, Employee e) { ... }
     * Then call it after PayRunController.executeBatchPayroll() returns.
     */
    public static class PayrollRowModel {
        private final SimpleStringProperty recordID, empID, name, department,
                basicPay, pf, tds, pt, netPay, status;

        public PayrollRowModel(String recordID, String empID, String name, String dept,
                String basicPay, String pf, String tds, String pt,
                String netPay, String status) {
            this.recordID = new SimpleStringProperty(recordID);
            this.empID = new SimpleStringProperty(empID);
            this.name = new SimpleStringProperty(name);
            this.department = new SimpleStringProperty(dept);
            this.basicPay = new SimpleStringProperty(basicPay);
            this.pf = new SimpleStringProperty(pf);
            this.tds = new SimpleStringProperty(tds);
            this.pt = new SimpleStringProperty(pt);
            this.netPay = new SimpleStringProperty(netPay);
            this.status = new SimpleStringProperty(status);
        }

        public String getRecordID() {
            return recordID.get();
        }

        public String getEmpID() {
            return empID.get();
        }

        public String getName() {
            return name.get();
        }

        public String getDepartment() {
            return department.get();
        }

        public String getBasicPay() {
            return basicPay.get();
        }

        public String getPf() {
            return pf.get();
        }

        public String getTds() {
            return tds.get();
        }

        public String getPt() {
            return pt.get();
        }

        public String getNetPay() {
            return netPay.get();
        }

        public String getStatus() {
            return status.get();
        }
    }

    /**
     * Row model for the employee directory TableView.
     *
     * TODO — INTEGRATION:
     * Add a static factory method:
     * public static EmployeeRowModel fromPackage(PayrollDataPackage pkg) { ... }
     */
    public static class EmployeeRowModel {
        private final SimpleStringProperty empID, name, department, grade,
                basicPay, country, taxRegime, state, yearsService;

        public EmployeeRowModel(String empID, String name, String dept, String grade,
                String basicPay, String country, String taxRegime,
                String state, String yearsService) {
            this.empID = new SimpleStringProperty(empID);
            this.name = new SimpleStringProperty(name);
            this.department = new SimpleStringProperty(dept);
            this.grade = new SimpleStringProperty(grade);
            this.basicPay = new SimpleStringProperty(basicPay);
            this.country = new SimpleStringProperty(country);
            this.taxRegime = new SimpleStringProperty(taxRegime);
            this.state = new SimpleStringProperty(state);
            this.yearsService = new SimpleStringProperty(yearsService);
        }

        public String getEmpID() {
            return empID.get();
        }

        public String getName() {
            return name.get();
        }

        public String getDepartment() {
            return department.get();
        }

        public String getGrade() {
            return grade.get();
        }

        public String getBasicPay() {
            return basicPay.get();
        }

        public String getCountry() {
            return country.get();
        }

        public String getTaxRegime() {
            return taxRegime.get();
        }

        public String getState() {
            return state.get();
        }

        public String getYearsService() {
            return yearsService.get();
        }
    }

    // ==========================================================================
    // MAIN
    // ==========================================================================

    public static void main(String[] args) {
        launch(args);
    }
}
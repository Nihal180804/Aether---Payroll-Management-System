# Graph Report - .  (2026-04-22)

## Corpus Check
- Corpus is ~17,692 words - fits in a single context window. You may not need a graph.

## Summary
- 383 nodes · 696 edges · 26 communities detected
- Extraction: 71% EXTRACTED · 29% INFERRED · 0% AMBIGUOUS · INFERRED: 200 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]

## God Nodes (most connected - your core abstractions)
1. `PayrollRecord` - 37 edges
2. `PayrollDashboardUI` - 35 edges
3. `MockPayrollRepository` - 24 edges
4. `Employee` - 23 edges
5. `Builder` - 20 edges
6. `PayrollRepositoryImpl` - 16 edges
7. `PayrollPresenterImpl` - 15 edges
8. `PayrollRowModel` - 12 edges
9. `EmployeeRowModel` - 11 edges
10. `PayrollPresenter` - 8 edges

## Surprising Connections (you probably didn't know these)
- `Final Maven JavaFX Run` --semantically_similar_to--> `Maven JavaFX Run`  [INFERRED] [semantically similar]
  run_log_final.txt → run_log.txt
- `50 Active Employees` --conceptually_related_to--> `15 Active Employees`  [INFERRED]
  run_log_final.txt → run_log.txt
- `Successful Employee Data Fetches` --conceptually_related_to--> `Maven Build Failure`  [INFERRED]
  run_log_final.txt → run_log.txt
- `Final Maven JavaFX Run` --references--> `Malformed Maven systemPath Warning`  [EXTRACTED]
  run_log_final.txt → run_log.txt

## Hyperedges (group relationships)
- **Failed Startup Caused By Null Currency Code** — run_log_payroll_presenter_impl, run_log_currency_code_null, run_log_application_start_failure, run_log_build_failure [EXTRACTED 1.00]
- **Successful Final Employee Loading** — run_log_final_maven_javafx_run, run_log_final_active_employee_ids_50, run_log_final_employee_range_aether_001_050, run_log_final_successful_employee_fetches, run_log_final_pay_period_2025_06 [EXTRACTED 1.00]
- **Shared Runtime Warning Context** — run_log_malformed_systempath_warning, run_log_restricted_native_access_warning, run_log_slf4j_nop_logger, run_log_final_unsafe_allocate_memory_warning [INFERRED 0.75]

## Communities

### Community 0 - "Community 0"
Cohesion: 0.08
Nodes (3): PayrollFacade, PayrollRecord, DigitalPayslipGenerator

### Community 1 - "Community 1"
Cohesion: 0.05
Nodes (11): Employee, SalaryGradeStructure, ApprovedClaimDTO, BonusDistributor, ExpenseDataProvider, LossOfPayTracker, MockExpenseProvider, ReimbursementTracker (+3 more)

### Community 2 - "Community 2"
Cohesion: 0.14
Nodes (1): PayrollDashboardUI

### Community 3 - "Community 3"
Cohesion: 0.1
Nodes (4): AuditLogger, PayrollPresenterImpl, PayrollRepositoryImpl, PayrollSystemFactory

### Community 4 - "Community 4"
Cohesion: 0.11
Nodes (4): EmployeeRowModel, PayrollRowModel, Spacer, TaxStrategyFactory

### Community 5 - "Community 5"
Cohesion: 0.07
Nodes (11): AttendanceLogTimeout, DuplicateClaimId, ExceedsClaimLimit, InvalidPayPeriod, MissingBaseSalary, MissingPerformanceRating, MissingTaxRegime, MissingWorkState (+3 more)

### Community 6 - "Community 6"
Cohesion: 0.29
Nodes (1): MockPayrollRepository

### Community 7 - "Community 7"
Cohesion: 0.1
Nodes (22): 15 Active Employees, Application Start Failure, Maven Build Failure, Null currencyCode, Employee AETHER_EMP_001, 50 Active Employees, Employees AETHER_001 through AETHER_050, Final Maven JavaFX Run (+14 more)

### Community 8 - "Community 8"
Cohesion: 0.18
Nodes (1): Builder

### Community 9 - "Community 9"
Cohesion: 0.11
Nodes (6): IncomeTaxTDS, IndiaNewRegime, IndiaOldRegime, Singapore, TaxStrategy, USFederal

### Community 10 - "Community 10"
Cohesion: 0.18
Nodes (2): PayrollException, PayRunController

### Community 11 - "Community 11"
Cohesion: 0.22
Nodes (1): PayrollPresenter

### Community 12 - "Community 12"
Cohesion: 0.48
Nodes (1): AetherSeeder

### Community 13 - "Community 13"
Cohesion: 0.33
Nodes (1): IPayrollRepository

### Community 14 - "Community 14"
Cohesion: 0.67
Nodes (1): Test

### Community 15 - "Community 15"
Cohesion: 0.67
Nodes (1): BatchResult

### Community 16 - "Community 16"
Cohesion: 0.67
Nodes (1): EmployeeViewModel

### Community 17 - "Community 17"
Cohesion: 0.67
Nodes (1): PayrollResultViewModel

### Community 18 - "Community 18"
Cohesion: 0.67
Nodes (1): AppTest

### Community 19 - "Community 19"
Cohesion: 1.0
Nodes (1): AttendanceDTO

### Community 20 - "Community 20"
Cohesion: 1.0
Nodes (1): EmployeeDTO

### Community 21 - "Community 21"
Cohesion: 1.0
Nodes (1): FinancialsDTO

### Community 22 - "Community 22"
Cohesion: 1.0
Nodes (1): PayrollDataPackage

### Community 23 - "Community 23"
Cohesion: 1.0
Nodes (1): PayrollResultDTO

### Community 24 - "Community 24"
Cohesion: 1.0
Nodes (1): TaxContextDTO

### Community 25 - "Community 25"
Cohesion: 1.0
Nodes (2): JavaFX Graphics Runtime, Unsafe allocateMemory Deprecation Warning

## Knowledge Gaps
- **16 isolated node(s):** `AttendanceDTO`, `EmployeeDTO`, `FinancialsDTO`, `PayrollDataPackage`, `PayrollResultDTO` (+11 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Community 19`** (2 nodes): `AttendanceDTO`, `AttendanceDTO.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 20`** (2 nodes): `EmployeeDTO`, `EmployeeDTO.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 21`** (2 nodes): `FinancialsDTO`, `FinancialsDTO.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 22`** (2 nodes): `PayrollDataPackage`, `PayrollDataPackage.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 23`** (2 nodes): `PayrollResultDTO`, `PayrollResultDTO.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 24`** (2 nodes): `TaxContextDTO.java`, `TaxContextDTO`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 25`** (2 nodes): `JavaFX Graphics Runtime`, `Unsafe allocateMemory Deprecation Warning`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PayrollException` connect `Community 10` to `Community 5`?**
  _High betweenness centrality (0.120) - this node is a cross-community bridge._
- **Why does `Employee` connect `Community 1` to `Community 0`, `Community 9`?**
  _High betweenness centrality (0.068) - this node is a cross-community bridge._
- **Why does `MockPayrollRepository` connect `Community 6` to `Community 2`, `Community 4`?**
  _High betweenness centrality (0.059) - this node is a cross-community bridge._
- **What connects `AttendanceDTO`, `EmployeeDTO`, `FinancialsDTO` to the rest of the system?**
  _16 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.08 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.05 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.14 - nodes in this community are weakly interconnected._
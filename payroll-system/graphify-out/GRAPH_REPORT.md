# Graph Report - C:\Users\nihal\Documents\6th_Sem\ooad\PayrollManagement\payroll-system  (2026-04-24)

## Corpus Check
- 39 files · ~23,318 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 453 nodes · 909 edges · 28 communities detected
- Extraction: 65% EXTRACTED · 35% INFERRED · 0% AMBIGUOUS · INFERRED: 321 edges (avg confidence: 0.8)
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
- [[_COMMUNITY_Community 26|Community 26]]
- [[_COMMUNITY_Community 27|Community 27]]

## God Nodes (most connected - your core abstractions)
1. `PayrollDashboardUI` - 37 edges
2. `PayrollRecord` - 37 edges
3. `MockPayrollRepository` - 24 edges
4. `Employee` - 23 edges
5. `Builder` - 20 edges
6. `PayrollPresenterImpl` - 18 edges
7. `PayrollRepositoryImpl` - 16 edges
8. `EmployeeRowModel` - 15 edges
9. `LeaveDetailsDTO` - 13 edges
10. `PayrollRowModel` - 12 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Communities

### Community 0 - "Community 0"
Cohesion: 0.06
Nodes (5): PayrollFacade, PayrollRecord, PayrollServiceImpl, PayslipExportService, DigitalPayslipGenerator

### Community 1 - "Community 1"
Cohesion: 0.09
Nodes (3): AppTest, PayrollDashboardUI, PayrollPresenterImpl

### Community 2 - "Community 2"
Cohesion: 0.06
Nodes (7): Employee, IncomeTaxTDS, IndiaNewRegime, IndiaOldRegime, Singapore, TaxStrategy, USFederal

### Community 3 - "Community 3"
Cohesion: 0.1
Nodes (4): EmployeeRowModel, PayrollRowModel, Spacer, TaxStrategyFactory

### Community 4 - "Community 4"
Cohesion: 0.07
Nodes (11): AttendanceLogTimeout, DuplicateClaimId, ExceedsClaimLimit, InvalidPayPeriod, MissingBaseSalary, MissingPerformanceRating, MissingTaxRegime, MissingWorkState (+3 more)

### Community 5 - "Community 5"
Cohesion: 0.11
Nodes (7): SalaryGradeStructure, BonusDistributor, LossOfPayTracker, ReimbursementTracker, Services, SeverancePay, StatuaryDeduction

### Community 6 - "Community 6"
Cohesion: 0.27
Nodes (1): MockPayrollRepository

### Community 7 - "Community 7"
Cohesion: 0.16
Nodes (2): LeaveDataProviderImpl, LeaveDetailsDTO

### Community 8 - "Community 8"
Cohesion: 0.16
Nodes (2): PayrollRepositoryImpl, PayrollSystemFactory

### Community 9 - "Community 9"
Cohesion: 0.24
Nodes (1): Builder

### Community 10 - "Community 10"
Cohesion: 0.13
Nodes (3): ApprovedClaimDTO, DatabaseConfig, PayrollDataProviderImpl

### Community 11 - "Community 11"
Cohesion: 0.14
Nodes (3): AuditLogger, PayrollException, PayRunController

### Community 12 - "Community 12"
Cohesion: 0.17
Nodes (1): PayrollPresenter

### Community 13 - "Community 13"
Cohesion: 0.2
Nodes (2): IPayrollService, PayslipSummary

### Community 14 - "Community 14"
Cohesion: 0.48
Nodes (1): AetherSeeder

### Community 15 - "Community 15"
Cohesion: 0.33
Nodes (1): IPayrollRepository

### Community 16 - "Community 16"
Cohesion: 0.67
Nodes (1): Test

### Community 17 - "Community 17"
Cohesion: 0.67
Nodes (1): BatchResult

### Community 18 - "Community 18"
Cohesion: 0.67
Nodes (1): EmployeeViewModel

### Community 19 - "Community 19"
Cohesion: 0.67
Nodes (1): PayrollResultViewModel

### Community 20 - "Community 20"
Cohesion: 0.67
Nodes (1): ExpenseDataProvider

### Community 21 - "Community 21"
Cohesion: 0.67
Nodes (1): ExpenseDataProviderImpl

### Community 22 - "Community 22"
Cohesion: 1.0
Nodes (1): AttendanceDTO

### Community 23 - "Community 23"
Cohesion: 1.0
Nodes (1): EmployeeDTO

### Community 24 - "Community 24"
Cohesion: 1.0
Nodes (1): FinancialsDTO

### Community 25 - "Community 25"
Cohesion: 1.0
Nodes (1): PayrollDataPackage

### Community 26 - "Community 26"
Cohesion: 1.0
Nodes (1): PayrollResultDTO

### Community 27 - "Community 27"
Cohesion: 1.0
Nodes (1): TaxContextDTO

## Knowledge Gaps
- **7 isolated node(s):** `AttendanceDTO`, `EmployeeDTO`, `FinancialsDTO`, `PayrollDataPackage`, `PayrollResultDTO` (+2 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Community 22`** (2 nodes): `AttendanceDTO`, `AttendanceDTO.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 23`** (2 nodes): `EmployeeDTO.java`, `EmployeeDTO`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 24`** (2 nodes): `FinancialsDTO.java`, `FinancialsDTO`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 25`** (2 nodes): `PayrollDataPackage.java`, `PayrollDataPackage`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 26`** (2 nodes): `PayrollResultDTO.java`, `PayrollResultDTO`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 27`** (2 nodes): `TaxContextDTO.java`, `TaxContextDTO`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PayrollException` connect `Community 11` to `Community 4`?**
  _High betweenness centrality (0.109) - this node is a cross-community bridge._
- **Why does `Employee` connect `Community 2` to `Community 0`, `Community 5`?**
  _High betweenness centrality (0.070) - this node is a cross-community bridge._
- **Why does `MockPayrollRepository` connect `Community 6` to `Community 1`?**
  _High betweenness centrality (0.053) - this node is a cross-community bridge._
- **What connects `AttendanceDTO`, `EmployeeDTO`, `FinancialsDTO` to the rest of the system?**
  _7 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.06 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.09 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.06 - nodes in this community are weakly interconnected._
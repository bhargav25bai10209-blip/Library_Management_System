# Project Statement: LibriTrack Pro

## 1. Problem Statement
Traditional academic and institutional libraries struggle with manual record keeping, opaque circulation processes, inventory discrepancies, and inconsistent fine calculations. When books are checked out or returned manually, errors frequently arise in tracking due dates, enforcing membership-specific borrow quotas, and notifying waiting patrons when in-demand titles are returned. Furthermore, static penalty calculations fail to account for differing patron tiers (such as students versus research faculty), leading to administrative friction. 

**LibriTrack Pro** addresses these operational challenges by providing an automated, modular, and extensible Library Management System developed in Java 17. The system establishes clear Role-Based Access Control (RBAC), algorithmic fine computation using the Strategy design pattern, an event-driven reservation waitlist using the Observer pattern, and transparent data persistence with atomic state guarantees.

---

## 2. Scope of the Project
The scope of LibriTrack Pro spans the essential operations required to administer a university or institutional library:

- **Catalog & Inventory Control**: Cataloging titles across disciplines, managing physical copies, tracking real-time availability, and indexing rack locations.
- **Patron Tier Management**: Structuring student and faculty profiles with individualized borrow caps (3 for students, 7 for faculty) and loan periods (14 days vs. 30 days).
- **Circulation Lifecycle**: Complete loan check-out, renewal with guardrails against hoarding, check-in, and automated overdue penalty calculation.
- **Priority Hold Queues**: Priority-weighted reservations that ensure fair, tier-aware allocation of high-demand resources.
- **Financial Audit & Receipts**: Settlement of overdue fines and instant generation of verifiable payment receipts.
- **Analytics & Health Metrics**: Real-time administrative reporting on title circulation frequency, overdue audits, and inventory utilization.

### Out of Scope
- Physical RFID gate hardware integration (simulated via ISBN identifiers).
- Direct external payment gateway integration (simulated via validated receipt transaction records).

---

## 3. Target Users

| User Persona | Primary Responsibilities & Interaction |
| :--- | :--- |
| **System Administrator** | Oversees library operations, provisions staff credentials, manages account statuses, and audits high-level institutional analytics and revenue collection. |
| **Librarian / Circulation Staff** | Catalogs acquisitions, manages physical book inventory, performs desk check-outs/check-ins, audits overdue loans, and collects penalty fees. |
| **Student Members** | Searches the catalog, borrows academic textbooks up to their 3-book quota, tracks due dates, places reservations on loaned books, and settles fines. |
| **Faculty Members** | Borrows research materials up to their 7-book quota with extended 30-day loan terms and 3-day grace periods, and receives priority in reservation waitlists. |
| **Guests / Prospective Patrons** | Explores catalog availability and book rack locations in read-only mode without authentication. |

---

## 4. High-Level Features

1. **Role-Based Access Control (RBAC)**: Secure multi-tier authentication powered by salted SHA-256 password hashing.
2. **Dynamic Search & Filtering**: Multi-attribute query engine searching across title, author, ISBN, and genre with instant availability indicators.
3. **Strategy-Based Penalty Engine**: Algorithmic separation of fine policies using the Strategy Pattern (`StudentFineStrategy` vs. `FacultyFineStrategy`).
4. **Observer-Driven Reservation Queue**: Priority-ordered waitlist (`Faculty > Student > FIFO`) with automatic notification dispatch upon item return.
5. **ACID-like Atomic File Storage**: Zero-dependency persistence engine providing JSON-based file storage with atomic writes to prevent data corruption.
6. **Executive Analytics Dashboard**: Instant calculation of catalog utilization, most borrowed titles, overdue risk exposures, and revenue totals.

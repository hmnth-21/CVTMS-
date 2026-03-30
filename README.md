# Campus Vehicular Traffic Management System (CVTMS) - Week 1 Core

CVTMS is a Java-based console application designed to manage and monitor vehicular traffic within a university campus environment according to the provided Software Requirements Specification (SRS). This implementation focuses exclusively on Phase 1 (Week 1) core features and uses PostgreSQL as the database backend.

## Key Features

- **Authentication System**: Role-based access for ADMIN and SECURITY personnel, using jBCrypt for secure password hashing.
- **Vehicle & Owner Registration**: Register owners and vehicles, categorized cleanly by type (CAMPUS, CAB, DELIVERY, WORK, EXTERNAL).
- **Entry & Exit Logging**: Prevent duplicate active entries. 
- **Overstay Detection**: Flag external/visitor vehicles overstaying limits; log justifications.
- **Incident Recording**: Record and view incidents involving vehicles (e.g., speeding, unauthorized parking).
- **Statistics & Reporting**: Daily entry/exit stats, vehicle type distribution, and overstay summaries.
- **Extended Search**: Search traffic logs by registration number and date range.
- **Movement History**: View full movement history for any registered vehicle.
- **Robust Validation**: Hardened input validation and edge-case handling.
- **Testing Suite**: Comprehensive unit tests for all core services.
- **Polished Console UI**: Improved formatting and navigation for a professional experience.

## Prerequisites

- Java Development Kit (JDK) 8 or higher
- **Maven** (to manage dependencies like PostgreSQL JDBC and jBCrypt)
- **PostgreSQL** Database installed locally and running on port `5432`.

## Installation and Configuration

1. **Database Setup**
   Ensure your local PostgreSQL server is running.
   Create a database called `cvtms`:
   ```sql
   CREATE DATABASE cvtms;
   ```
   > Note: In `src/com/cvtms/dao/DatabaseConnectionManager.java`, the default postgres connection expects username `postgres` and password `admin`. Update these strings if your local postgres credentials differ!

2. **Build and Run the Application**
   This project is Maven-based. You can compile and execute it using the Maven `exec` plugin:
   ```bash
   mvn clean compile exec:java
   ```

## Default Credentials

The schema initialization code will automatically seed a default Admin account on first run:
- **Username**: `admin`
- **Password**: `admin`

## Design Constraints Fulfilled
- ✅ No Javascript, Python, PHP, or GUI frameworks. Console-based pure Java.
- ✅ Structured Maven application.
- ✅ Uses PostgreSQL backend exclusively via standard JDBC interface.
- ✅ Deferred everything strictly defined for Week 2 into a plaintext tracking file.

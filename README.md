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
- Node.js + npm (for serving the frontend)
- **PostgreSQL** Database installed locally and running on port `5432`.

## Installation and Configuration

1. **Database Setup**
   Ensure your local PostgreSQL server is running.
   Create a database called `cvtms`:

   ```sql
   CREATE DATABASE cvtms;
   ```

   > Note: In `src/main/java/com/cvtms/dao/DatabaseConnectionManager.java`, the default postgres connection expects username `postgres` and password `admin`. Update these strings if your local postgres credentials differ.

2. **Start Backend API (single command)**
   From the project root on Windows:

   ```powershell
   npm run backend
   ```

   This command now does all of the following for you:
   - Cleans and compiles backend classes from `src/main/java` into `bin`
   - Starts the API server (`com.cvtms.api.ApiServer`) on `http://localhost:8080`

3. **Start Frontend**
   In a separate terminal:

   ```powershell
   npm run dev
   ```

   Frontend runs on `http://localhost:3000`.

4. **Optional: Run Console App Instead of API**
   ```powershell
   npm run backend:console
   ```

## Helpful Commands

- Build backend only:

  ```powershell
  .\build.bat
  ```

- Run API backend directly (after build):

  ```powershell
  .\run.bat api
  ```

- Run console backend directly (after build):

  ```powershell
  .\run.bat
  ```

- Run tests:
  ```powershell
  mvn test
  ```

## API Health Check

To verify backend is up:

```powershell
Invoke-WebRequest -UseBasicParsing "http://localhost:8080/api/dashboard/summary" | Select-Object -ExpandProperty StatusCode
```

Expected output: `200`

## Troubleshooting

- **Address already in use: bind (8080)**
  Another process is already using port 8080. Stop that process and rerun backend.

  ```powershell
  netstat -ano | findstr :8080
  Stop-Process -Id <PID> -Force
  npm run backend
  ```

- **Dozens of JUnit errors while building backend**
  Use the updated `build.bat` / `npm run backend`. It compiles only `src/main/java` and excludes test sources.

## Default Credentials

The schema initialization code will automatically seed a default Admin account on first run:

- **Username**: `admin`
- **Password**: `admin`

## Design Constraints Fulfilled

- ✅ No Javascript, Python, PHP, or GUI frameworks. Console-based pure Java.
- ✅ Structured Maven application.
- ✅ Uses PostgreSQL backend exclusively via standard JDBC interface.
- ✅ Deferred everything strictly defined for Week 2 into a plaintext tracking file.

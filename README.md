# Campus Vehicular Traffic Management System (CVTMS)

CVTMS is a Java and PostgreSQL based system for managing campus vehicle operations. The project includes a backend API, a browser-based frontend, and a console entry point. It supports day-to-day gate operations, incident handling, reporting, and admin user management.

## Overview

The system is designed around a clear layered architecture:

- API layer: HTTP endpoints for frontend integration
- Service layer: business workflows and validations
- DAO layer: SQL data access through JDBC
- Database layer: PostgreSQL persistence
- Frontend layer: role-based operational interface

The project currently provides practical support for:

- Authentication for ADMIN and SECURITY roles
- Vehicle registration and owner mapping
- Entry and exit recording with active-entry safeguards
- Vehicle movement history and extended traffic log search
- Incident recording and incident listing
- Dashboard metrics and distribution reporting
- Admin user listing and admin-controlled user deletion
- Overstay tracking and overstay list reporting

## Technology Stack

- Java (JDK 8+)
- Maven
- PostgreSQL
- JDBC
- jBCrypt
- Static frontend (HTML, CSS, JavaScript)
- Node.js `serve` for local frontend hosting

## Prerequisites

Before running the system, ensure the following are installed:

- JDK 8 or above
- Maven
- PostgreSQL (running on `localhost:5432`)
- Node.js and npm

## Database Setup

1. Create the database:

```sql
CREATE DATABASE cvtms;
```

2. Confirm database credentials in [src/main/java/com/cvtms/dao/DatabaseConnectionManager.java](src/main/java/com/cvtms/dao/DatabaseConnectionManager.java).

Default values in the project are:

- user: `postgres`
- password: `admin`
- database: `cvtms`

The application initializes tables automatically at startup and seeds a default admin account if no users exist.

## Running the Project

Use two terminals from the project root.

1. Start backend API:

```powershell
npm run backend
```

2. Start frontend:

```powershell
npm run dev
```

3. Open frontend:

- `http://localhost:3000`

### Optional Console Mode

To run the console application instead of API mode:

```powershell
npm run backend:console
```

## Default Login

If no user exists, the system seeds:

- username: `admin`
- password: `admin`
- role: `ADMIN`

## Main API Endpoints

Authentication and users:

- `POST /api/login`
- `POST /api/register`
- `GET /api/users`
- `DELETE /api/user`

Vehicles and traffic:

- `GET /api/vehicles/inside`
- `POST /api/vehicle/register`
- `POST /api/entry`
- `POST /api/exit`
- `GET /api/vehicle/search`
- `GET /api/vehicle/movement`
- `GET /api/logs/search`

Incidents and reporting:

- `GET /api/incidents`
- `POST /api/incidents`
- `GET /api/dashboard/summary`
- `GET /api/overstays`

## Admin Features Available in Frontend

- Dashboard statistics and distribution
- Vehicles currently inside (dedicated admin view)
- Overstay list
- Full incident list
- Full user list
- Register security users
- Delete security users (admin credential confirmation required)

## Build and Test Commands

Build backend classes:

```powershell
.\build.bat
```

Run API directly:

```powershell
.\run.bat api
```

Run console directly:

```powershell
.\run.bat
```

Run tests:

```powershell
mvn test
```

## Health Check

Verify that API is running:

```powershell
Invoke-WebRequest -UseBasicParsing "http://localhost:8080/api/dashboard/summary" | Select-Object -ExpandProperty StatusCode
```

Expected result: `200`

## Troubleshooting

### Port 8080 already in use

Find the process and stop it, then restart backend:

```powershell
netstat -ano | findstr :8080
Stop-Process -Id <PID> -Force
npm run backend
```

### Backend compile fails with test-related errors

Use [build.bat](build.bat) or `npm run backend`, which compile main sources for runtime startup.

### Frontend shows stale behavior after changes

Perform a hard refresh in the browser (Ctrl+F5) to reload updated JavaScript.

## Repository Structure

- Backend source: [src/main/java](src/main/java)
- Backend tests: [src/test/java](src/test/java)
- Frontend: [frontend](frontend)
- Build scripts: [build.bat](build.bat), [run.bat](run.bat)
- Maven config: [pom.xml](pom.xml)
- npm scripts: [package.json](package.json)

## Notes

This project uses real backend and database data for all operational and admin views. No hardcoded demo records are required for core workflows.

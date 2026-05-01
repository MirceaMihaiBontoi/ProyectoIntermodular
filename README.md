# Reservation System v2.0 (Academic Project)

> [!NOTE]
> **Academic Disclaimer**: This is a didactic project developed for educational purposes. It is not intended for production use.

A dynamic Java application featuring a **Modular JavaFX Desktop Application** integrated with a **Real-Time Web Dashboard** powered by Javalin 7.

## 📝 Project Context & Evolution

Originally, this exercise required a standard Java application with JavaFX to handle basic CRUD operations on a database. However, the project has been significantly expanded to showcase a more complex full-stack architecture:

1.  **Desktop App (Admin Panel)**: The JavaFX application now serves as a high-level **Database Administrator Panel**, allowing full control over all tables, schema synchronization, and system monitoring.
2.  **Web Interface (Staff Portal)**: A web server was added to provide a dedicated **Staff Management Interface**. This portal is specifically tailored for staff members to manage reservations in real-time, providing a simplified and modern workflow compared to the full admin panel.

## 🏗️ Architecture Overview

The system follows a multi-layer architecture designed to demonstrate clean code and decoupling principles:

- **Modular Backend (REST API)**: A Javalin 7 server running as a daemon thread, providing a JSON API and WebSocket synchronization.
- **Dynamic Desktop Frontend (JavaFX)**: A native UI that loads screens via FXML and uses **JDBC `DatabaseMetaData`** (through `MetadataDAO`) to generate forms and tables for administrative tasks.
- **Web Dashboard**: A modern, responsive interface built with ES6 modules for staff-specific reservation management.
- **Data Access Layer (DAO)**:
  - `GenericDAO`: **Dynamic SQL** built from table and column names, with `PreparedStatement` placeholders for values (`Map`-based insert/update/delete) plus helpers for reading rows as JavaFX `StringProperty` lists or generic `Map`s for the REST API.
  - `Domain DAOs`: Specific logic for `Usuario` and `Reserva`.
- **Security**: Password hashing using the `at.favre.lib:bcrypt` modular library.

## 📁 Project Structure

```text
ProyectoIntermodular/
├── database/              # Persistence layer (SQLite)
├── src/main/java/         # Source code (Modular)
│   ├── com.reservas.app/
│   │   ├── controller/    # JavaFX Controllers (Admin UI)
│   │   ├── dao/           # Data Layer (Generic & Domain)
│   │   ├── util/          # UI Helpers & Form/Table Managers
│   │   ├── web/           # Javalin Server & API Handlers (Web Portal)
│   │   └── App.java       # Main Entry Point
│   └── module-info.java   # JPMS Module Configuration
├── src/main/resources/    # FXML & CSS Assets
├── web/                   # Web Interface (Staff Portal Assets)
└── pom.xml                # Maven configuration
```

## 📚 Educational Tech Stack

- **Language**: Java 17+
- **Desktop UI**: JavaFX 26.0.1
- **Web Engine**: Javalin 7.2.0 (Jetty 12)
- **Persistence**: SQLite JDBC 3.53.0.0
- **Security**: BCrypt 0.10.2

## 🛠️ Usage

### 1. Prerequisites
- **JDK 17** or higher.
- **Maven 3.8+**.

### 2. Execution
```bash
mvn clean compile
mvn javafx:run
```

### 3. Web Access
Staff Portal: **[http://localhost:3000](http://localhost:3000)**

## 🔄 Real-Time Synchronization

The system implements a bidirectional refresh mechanism:
1. **Web → JavaFX**: Changes in the staff portal trigger a callback that refreshes the Admin Panel via `Platform.runLater()`.
2. **JavaFX → Web**: Administrative changes trigger a WebSocket broadcast that updates all staff portals instantly.

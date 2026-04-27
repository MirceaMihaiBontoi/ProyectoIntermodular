# Reservation System

Dynamic JavaFX CRUD application for reservation management with SQLite database.

## Features

- Dynamic interface automatically generated from database schema
- Full CRUD operations support (Create, Read, Update, Delete)
- Data type validation (dates with DatePicker, dropdowns for enums)
- Password hashing with BCrypt for secure password storage
- Foreign keys enabled with CASCADE in SQLite
- Integrated SQL console with support for multiple operations
- Automatic data and dropdown refresh across all tabs
- Specialized cascade logic for usuario table (administrador/usuarionormal)

## Project Structure

- `database/`: SQL scripts and SQLite database
- `src/main/java/com/reservas/app/`: Application source code
  - `dao/`: Data access layer (GenericDAO, MetadataDAO, UsuarioDAO, DatabaseManager)
  - `controller/`: JavaFX controllers (PrimaryController, DynamicTableController)
  - `util/`: UI utilities (FormManager, TableManager, DialogHelper)
  - `model/`: Data models (ForeignKey)
  - `util/fields/`: Dynamic form fields (TextFormField, ComboFormField, DateFormField)
- `src/test/java/com/reservas/app/`: Test suite
  - `dao/`: DAO unit tests (DatabaseManagerTest, GenericDAOTest, MetadataDAOTest, UsuarioDAOTest, CascadeDeleteTest)
  - `integration/`: Integration tests (ReservaIntegrationTest)

## Requirements

- Java 11 or higher
- Maven 3.6 or higher
- JavaFX 17

## Installation and Execution

```bash
# Compile the project
mvn compile

# Run the application
mvn javafx:run

# Run tests
mvn test
```

## Database

The database is automatically initialized from `database/scripts/sistema_reservas_lite.sql` if it doesn't exist. Foreign keys are enabled by default to support CASCADE.

## SQL Console

The SQL console supports multiple operations separated by semicolons:
- SELECT: Displays results in a table
- INSERT/UPDATE/DELETE: Shows affected rows
- Results appear in a temporary "SQL Results" tab

## Cascade Logic

- ON DELETE CASCADE and ON UPDATE CASCADE are handled at the database level
- For the usuario table, a record is automatically created in `administrador` or `usuarionormal` based on user type
- User type changes manage migration between child tables

## Security

- Passwords are automatically hashed using BCrypt before storage
- BCrypt uses salt and is designed for password hashing (not for general encryption)
- Passwords cannot be retrieved in plain text, only verified
- Use `UsuarioDAO.verifyPassword(userId, plainPassword)` to verify credentials

## Tests

The project includes comprehensive test coverage:

**Unit Tests (35 tests total)**
- DatabaseManagerTest: Database initialization and connection tests
- GenericDAOTest: CRUD operations tests
- MetadataDAOTest: Metadata retrieval tests
- UsuarioDAOTest: User cascade logic tests
- CascadeDeleteTest: CASCADE DELETE functionality tests

**Integration Tests**
- ReservaIntegrationTest: Complete reservation workflow tests

All tests verify:
- CRUD operations work correctly
- Cascade logic for usuario table functions properly
- CASCADE DELETE operations work as expected
- Foreign key constraints are enforced

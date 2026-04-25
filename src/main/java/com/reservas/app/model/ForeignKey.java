package com.reservas.app.model;

/**
 * DATA MODEL: Represents a Foreign Key relationship in the database.
 */
public record ForeignKey(String column, String refTable, String refColumn) {
}

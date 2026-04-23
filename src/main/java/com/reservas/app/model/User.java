package com.reservas.app.model;

import java.time.LocalDate;

// Represents a user in the system
public class User {
    private int id;
    private String email;
    private String password;
    private String name;
    private LocalDate birthDate;
    private String userType; // "Administrator" or "Normal"

    public User() {}

    public User(int id, String email, String password, String name, LocalDate birthDate, String userType) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.birthDate = birthDate;
        this.userType = userType;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    @Override
    public String toString() {
        return name + " (" + userType + ")";
    }
}

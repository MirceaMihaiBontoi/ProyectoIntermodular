package com.reservas.app.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for password hashing and verification using BCrypt.
 * BCrypt is a secure password hashing algorithm that automatically handles salting.
 */
public final class PasswordHasher {

    private PasswordHasher() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Hashes a password using BCrypt.
     * BCrypt automatically generates a salt and includes it in the hash.
     * 
     * @param plainPassword The plain text password to hash
     * @return The hashed password
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    /**
     * Verifies a plain text password against a hashed password.
     * 
     * @param plainPassword The plain text password to check
     * @param hashedPassword The hashed password to verify against
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}

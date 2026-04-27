package com.reservas.app.dao;

import com.reservas.app.util.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PasswordHasher to verify password hashing and verification.
 */
class PasswordHasherTest {

    @Test
    void testHashPassword() {
        String plainPassword = "test123";
        String hashedPassword = PasswordHasher.hashPassword(plainPassword);

        assertNotNull(hashedPassword, "Hashed password should not be null");
        assertNotEquals(plainPassword, hashedPassword, "Hashed password should not equal plain password");
        assertTrue(hashedPassword.length() > 50, "BCrypt hash should be long enough");
    }

    @Test
    void testHashPasswordProducesDifferentHashesForSamePassword() {
        String plainPassword = "test123";
        String hash1 = PasswordHasher.hashPassword(plainPassword);
        String hash2 = PasswordHasher.hashPassword(plainPassword);

        assertNotEquals(hash1, hash2, "BCrypt should produce different hashes for same password (due to random salt)");
    }

    @Test
    void testVerifyPassword() {
        String plainPassword = "test123";
        String hashedPassword = PasswordHasher.hashPassword(plainPassword);

        assertTrue(PasswordHasher.verifyPassword(plainPassword, hashedPassword), 
            "Verification should succeed with correct password");
    }

    @Test
    void testVerifyPasswordWithWrongPassword() {
        String plainPassword = "test123";
        String hashedPassword = PasswordHasher.hashPassword(plainPassword);

        assertFalse(PasswordHasher.verifyPassword("wrongpassword", hashedPassword), 
            "Verification should fail with wrong password");
    }

    @Test
    void testVerifyPasswordWithNullHash() {
        assertFalse(PasswordHasher.verifyPassword("test123", null), 
            "Verification should fail with null hash");
    }
}

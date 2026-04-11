/**
 * PasswordUtils — bcrypt password hashing helpers.
 *
 * Why bcrypt?
 * ───────────
 * Bcrypt is a key-derivation function designed to be slow.  Unlike fast
 * hashes (MD5, SHA-256), its cost factor makes brute-force and dictionary
 * attacks impractical even if the database is leaked.
 *
 * Cost factor 12 — each hash requires ~250 ms on modern hardware.
 * That is acceptable for a login request but prohibitively expensive for
 * an attacker iterating over millions of guesses.
 *
 * hashPassword(plain)
 *   Generates a new random 128-bit salt and returns the bcrypt hash.
 *   A new salt is generated on every call, so two identical passwords
 *   produce different hashes — preventing rainbow-table lookups.
 *
 * checkPassword(plain, hash)
 *   Extracts the salt embedded in the stored hash, re-hashes the
 *   candidate password, and compares in constant time to prevent
 *   timing attacks.
 *
 * Passwords are NEVER stored in plain text anywhere in this codebase.
 */
package umm3601.auth;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

  public static String hashPassword(String plain) {
    return BCrypt.hashpw(plain, BCrypt.gensalt(12));
  }

  public static boolean checkPassword(String plain, String hash) {
    return BCrypt.checkpw(plain, hash);
  }
}


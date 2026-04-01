package pl.jms.auth.core.security;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHasher {

    private static final int ROUNDS = 12;

    public String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(ROUNDS));
    }

    public boolean verify(String plain, String hash) {
        if (plain == null || hash == null || hash.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plain, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

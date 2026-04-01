package pl.jms.auth.core.auth;

public enum LoginAttemptResult {
    SUCCESS,
    WRONG_PASSWORD,
    RATE_LIMITED,
    LOCKED_OUT,
    NOT_REGISTERED
}

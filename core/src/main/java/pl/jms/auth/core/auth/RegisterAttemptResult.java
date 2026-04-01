package pl.jms.auth.core.auth;

public enum RegisterAttemptResult {
    SUCCESS,
    ALREADY_REGISTERED,
    PASSWORDS_MISMATCH,
    RULE_VIOLATION_LENGTH,
    RULE_VIOLATION_DIGITS,
    RULE_VIOLATION_SPECIAL,
    RATE_LIMITED,
    LOCKED_OUT
}

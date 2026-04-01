package pl.jms.auth.core.security;

import pl.jms.auth.core.config.AuthConfig;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PasswordRulesEngine {

    private final AuthConfig.PasswordRules rules;

    public PasswordRulesEngine(AuthConfig.PasswordRules rules) {
        this.rules = rules;
    }

    public List<String> validate(String password) {
        List<String> violations = new ArrayList<>();
        if (password == null || password.length() < rules.minLength()) {
            violations.add("length");
        }
        if (rules.minUniqueDigitCount() > 0) {
            Set<Character> digits = new LinkedHashSet<>();
            for (char c : password.toCharArray()) {
                if (Character.isDigit(c)) {
                    digits.add(c);
                }
            }
            if (digits.size() < rules.minUniqueDigitCount()) {
                violations.add("digits");
            }
        }
        if (rules.minSpecialCharacters() > 0) {
            int specials = 0;
            for (char c : password.toCharArray()) {
                if (!Character.isLetterOrDigit(c)) {
                    specials++;
                }
            }
            if (specials < rules.minSpecialCharacters()) {
                violations.add("special");
            }
        }
        return violations;
    }
}

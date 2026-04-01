package pl.jms.auth.core.model;

import java.util.Objects;
import java.util.UUID;

public final class Account {

    private UUID uuid;
    private String name;
    private String nameLower;
    private String passwordLegacy;
    private String passwordHash;
    private boolean premium;
    private boolean registered;
    private boolean titlesEnabled;
    private String lastIp;
    private Long lastLoginEpochMs;

    public Account(
            UUID uuid,
            String name,
            String nameLower,
            String passwordLegacy,
            String passwordHash,
            boolean premium,
            boolean registered,
            boolean titlesEnabled,
            String lastIp,
            Long lastLoginEpochMs
    ) {
        this.uuid = Objects.requireNonNull(uuid);
        this.name = Objects.requireNonNull(name);
        this.nameLower = nameLower != null ? nameLower : name.toLowerCase();
        this.passwordLegacy = passwordLegacy;
        this.passwordHash = passwordHash;
        this.premium = premium;
        this.registered = registered;
        this.titlesEnabled = titlesEnabled;
        this.lastIp = lastIp;
        this.lastLoginEpochMs = lastLoginEpochMs;
    }

    public static Account newPending(UUID uuid, String name) {
        String lower = name.toLowerCase();
        return new Account(uuid, name, lower, null, null, false, false, true, null, null);
    }

    public UUID uuid() {
        return uuid;
    }

    public void reassignUuid(UUID newUuid) {
        this.uuid = Objects.requireNonNull(newUuid);
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.nameLower = name.toLowerCase();
    }

    public String nameLower() {
        return nameLower;
    }

    public String passwordLegacy() {
        return passwordLegacy;
    }

    public void setPasswordLegacy(String passwordLegacy) {
        this.passwordLegacy = passwordLegacy;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean premium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public boolean registered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public boolean titlesEnabled() {
        return titlesEnabled;
    }

    public void setTitlesEnabled(boolean titlesEnabled) {
        this.titlesEnabled = titlesEnabled;
    }

    public String lastIp() {
        return lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }

    public Long lastLoginEpochMs() {
        return lastLoginEpochMs;
    }

    public void setLastLoginEpochMs(Long lastLoginEpochMs) {
        this.lastLoginEpochMs = lastLoginEpochMs;
    }

    public boolean nameMatchesConnection(String connectionName) {
        return name.equalsIgnoreCase(connectionName);
    }
}

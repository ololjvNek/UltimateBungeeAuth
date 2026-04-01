package pl.jms.auth.core.auth;

import pl.jms.auth.core.model.Account;

public record AccountJoinResult(boolean nameClaimedByOtherUuid, Account accountHolderForName, Account activeAccount) {
    public static AccountJoinResult ok(Account active) {
        return new AccountJoinResult(false, null, active);
    }

    public static AccountJoinResult conflict(Account otherUuidHolder) {
        return new AccountJoinResult(true, otherUuidHolder, null);
    }
}

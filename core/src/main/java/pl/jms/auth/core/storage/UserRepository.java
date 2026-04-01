package pl.jms.auth.core.storage;

import pl.jms.auth.core.model.Account;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    void upsert(Account account);

    void update(Account account);

    void delete(UUID uuid);

    void changeUuid(UUID oldUuid, UUID newUuid);

    Optional<Account> findByUuid(UUID uuid);

    Optional<Account> findByNameLower(String nameLower);

    Collection<Account> loadAll();
}

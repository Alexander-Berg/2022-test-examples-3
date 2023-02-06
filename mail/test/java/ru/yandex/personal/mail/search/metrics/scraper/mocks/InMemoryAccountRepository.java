package ru.yandex.personal.mail.search.metrics.scraper.mocks;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import ru.yandex.personal.mail.search.metrics.scraper.services.account.AccountDraft;
import ru.yandex.personal.mail.search.metrics.scraper.services.account.AccountRepository;

public class InMemoryAccountRepository implements AccountRepository {

    private Set<SimpleAccount> accounts = new HashSet<>();

    @Override
    public boolean hasAccount(String system, String accountName) {
        return accounts.contains(new SimpleAccount(system, accountName));
    }

    @Override
    public void deleteAccount(String system, String accountName) {
        accounts.remove(new SimpleAccount(system, accountName));
    }

    @Override
    public void writeAccount(AccountDraft draft) {
        accounts.add(new SimpleAccount(draft.getSystemName(), draft.getAccountName()));
    }

    @Override
    public Path getAccountPath(String system, String accountName) {
        return null;
    }
}

package ru.yandex.market.abo.mm.db;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.mm.model.Account;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.abo.mm.model.AccountType.REGULAR;

/**
 * @author antipov93.
 */
public class DbMailAccountServiceTest extends EmptyTest {

    @Autowired
    private DbMailAccountService dbMailAccountService;

    @Test
    void testLoadAccountsByHypIds() {
        List<String> emails = Arrays.asList("abo@yandex-team.ru", "iabo@yandex-team.ru");
        IntStream.range(0, 2).forEach(i ->
                assertNotNull(dbMailAccountService.storeAccount(emails.get(i), (long) i, REGULAR))
        );

        List<Account> accounts = dbMailAccountService.loadForMailCollecting();
        assertEquals(2, accounts.size());
        assertEquals(new HashSet<>(emails), accounts.stream().map(Account::getEmail).collect(toSet()));

        IntStream.range(0, 2).forEach(i ->
                assertTrue(dbMailAccountService.storeTicketAccountBinding(i, accounts.get(i).getId()))
        );

        Map<Long, Account> ticketsToAccounts = dbMailAccountService.loadAccountsByHypIds(Arrays.asList(0L, 1L));
        assertEquals(2, ticketsToAccounts.size());
    }

    @Test
    void testLoadAccountByHypId() {
        long hypId = 1;
        long userId = 1;

        assertNotNull(dbMailAccountService.storeAccount("abo@yandex-team.ru", userId, REGULAR));
        List<Account> accounts = dbMailAccountService.loadForMailCollecting();
        assertEquals(1, accounts.size());
        Account account = accounts.get(0);
        assertTrue(dbMailAccountService.storeTicketAccountBinding(hypId, account.getId()));

        Account loaded = dbMailAccountService.loadAccountByHypId(hypId);
        assertEquals(account, loaded);

        assertNull(dbMailAccountService.loadAccountByHypId(0));
    }
}

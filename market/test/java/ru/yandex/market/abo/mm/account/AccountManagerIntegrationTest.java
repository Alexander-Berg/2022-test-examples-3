package ru.yandex.market.abo.mm.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.mm.db.DbMailAccountService;
import ru.yandex.market.abo.mm.model.Account;
import ru.yandex.market.abo.mm.model.AccountStatus;
import ru.yandex.market.abo.mm.model.AccountType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 * @date 23.10.2008
 */
class AccountManagerIntegrationTest extends EmptyTest {
    private static final int USER_ID = 23075721;
    private static final int HYP_ID = 1198240;
    private static final String EMAIL = "mr@robot";

    @Autowired
    private AccountManager accountManager;

    @Autowired
    private DbMailAccountService accountService;

    @BeforeEach
    void setUp() {
        assertNotNull(accountService.storeAccount(EMAIL, (long) USER_ID, AccountType.REGULAR));
    }

    /**
     * Account already taken - return -1 without any rebinding.
     */
    @Test
    void getAccountToTicket_alreadyTaken() {
        Account account = loadStoredAccount();

        accountService.storeTicketAccountBinding(HYP_ID, account.getId());
        assertEquals(-1, accountManager.getAccountToTicket(HYP_ID, USER_ID));
    }

    /**
     * Available account for HYP_ID, USER_ID exists - return it.
     */
    @Test
    void getAccountToTicket_availableExists() {
        assertEquals(-1, accountService.getAvailableAccount(HYP_ID, USER_ID));
        assertTrue(accountService.isTicketFreeFromAccount(HYP_ID));

        Account account = loadStoredAccount();
        accountService.setAccountStatus(account.getId(), AccountStatus.ACTIVE);
        accountService.storeAccountBinding((int) account.getId(), USER_ID, (long) USER_ID);
        assertEquals(account.getId(), accountService.getAvailableAccount(HYP_ID, USER_ID));

        assertEquals(account.getId(), accountManager.getAccountToTicket(HYP_ID, USER_ID));
        assertFalse(accountService.isTicketFreeFromAccount(HYP_ID));
    }

    /**
     * No bound account exists - bind new.
     */
    @Test
    void getAccountToTicket_noAvailable() {
        assertTrue(accountService.isTicketFreeFromAccount(HYP_ID));
        assertTrue(accountService.loadAccountsByStatus(AccountStatus.ACTIVE).isEmpty());

        Account account = loadStoredAccount();
        accountService.setAccountStatus(account.getId(), AccountStatus.ACTIVE);
        assertEquals(-1, accountService.getAvailableAccount(HYP_ID, USER_ID));

        assertEquals(account.getId(), accountManager.getAccountToTicket(HYP_ID, USER_ID));
        assertEquals(account.getId(), accountService.getAvailableAccount(HYP_ID, USER_ID));
    }

    @Test
    void processNewAccounts() {
        accountManager.processNewAccounts();
    }

    @Test
    void processCheckAccounts() {
        accountManager.processCheckAccounts();
    }

    private Account loadStoredAccount() {
        return accountService.loadForMailCollecting().stream()
                .filter(acc -> acc.getEmail().equals(EMAIL)).findFirst().orElseThrow(RuntimeException::new);
    }

}

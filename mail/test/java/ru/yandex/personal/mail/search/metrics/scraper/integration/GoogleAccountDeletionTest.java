package ru.yandex.personal.mail.search.metrics.scraper.integration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.personal.mail.search.metrics.scraper.controllers.AccountController;
import ru.yandex.personal.mail.search.metrics.scraper.metrics.MetricsCompatibleSystemManager;
import ru.yandex.personal.mail.search.metrics.scraper.services.account.AccountException;
import ru.yandex.personal.mail.search.metrics.scraper.services.account.AccountManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Using junit vintage (4) for spring 4 compatibility
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestingConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GoogleAccountDeletionTest {
    private static final String SYSTEM_NAME = "google";
    private static final String ACCOUNT_NAME = "acc";

    @Autowired
    private MetricsCompatibleSystemManager serviceManager;

    @Autowired
    private AccountManager accountManager;

    @Autowired
    private AccountController accountController;

    @Test
    public void deleteGoogleAccount() {
        if (accountManager.has(SYSTEM_NAME, ACCOUNT_NAME)) {
            accountController.deleteAccount(SYSTEM_NAME, ACCOUNT_NAME);
        }

        AccountTestTools.createAccount(accountController, SYSTEM_NAME, ACCOUNT_NAME);

        accountController.deleteAccount(SYSTEM_NAME, ACCOUNT_NAME);

        assertFalse(accountManager.has(SYSTEM_NAME, ACCOUNT_NAME));
        assertThrows(AccountException.class, () -> accountManager.getConfiguration(SYSTEM_NAME, ACCOUNT_NAME));
    }
}

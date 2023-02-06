package ru.yandex.personal.mail.search.metrics.scraper.integration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.personal.mail.search.metrics.scraper.controllers.AccountController;
import ru.yandex.personal.mail.search.metrics.scraper.metrics.MetricsCompatibleMailSystem;
import ru.yandex.personal.mail.search.metrics.scraper.metrics.MetricsCompatibleSystemManager;
import ru.yandex.personal.mail.search.metrics.scraper.services.account.AccountConfiguration;
import ru.yandex.personal.mail.search.metrics.scraper.services.account.AccountManager;
import ru.yandex.personal.mail.search.metrics.scraper.services.account.AccountProperties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Using junit vintage (4) for spring 4 compatibility
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestingConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GoogleAccountCreationTest {
    private static final String SYSTEM_NAME = "google";
    private static final String ACCOUNT_NAME = "acc";

    @Autowired
    private MetricsCompatibleSystemManager serviceManager;

    @Autowired
    private AccountManager accountManager;

    @Autowired
    private AccountController accountController;

    @Test
    public void newGoogleAccountCreation() {
        if (accountManager.has(SYSTEM_NAME, ACCOUNT_NAME)) {
            accountController.deleteAccount(SYSTEM_NAME, ACCOUNT_NAME);
        }

        accountController.addAccount(SYSTEM_NAME, ACCOUNT_NAME, new AccountProperties(""));
        accountController.putAccountPart(SYSTEM_NAME, ACCOUNT_NAME, "StoredCredential",
                new byte[AccountTestTools.AVG_TOKEN_SIZE]);
        accountController.putAccountPart(SYSTEM_NAME, ACCOUNT_NAME, "credentials",
                new byte[AccountTestTools.AVG_TOKEN_SIZE]);
        accountController.finishAccountInit(SYSTEM_NAME, ACCOUNT_NAME);

        assertTrue(accountManager.has(SYSTEM_NAME, ACCOUNT_NAME));

        AccountConfiguration config = accountManager.getConfiguration(SYSTEM_NAME, ACCOUNT_NAME);

        MetricsCompatibleMailSystem service = serviceManager.getSystem(config);
        assertNotNull(service);
    }
}

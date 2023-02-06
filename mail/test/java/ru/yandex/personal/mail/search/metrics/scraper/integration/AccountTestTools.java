package ru.yandex.personal.mail.search.metrics.scraper.integration;

import ru.yandex.personal.mail.search.metrics.scraper.controllers.AccountController;
import ru.yandex.personal.mail.search.metrics.scraper.services.account.AccountProperties;

public final class AccountTestTools {
    public static final int AVG_TOKEN_SIZE = 1000;

    private AccountTestTools() {
    }

    public static void createAccount(AccountController accountController, String systemName, String accountName) {
        accountController.addAccount(systemName, accountName, new AccountProperties(""));
        accountController.putAccountPart(systemName, accountName, "StoredCredential",
                new byte[AccountTestTools.AVG_TOKEN_SIZE]);
        accountController.putAccountPart(systemName, accountName, "credentials",
                new byte[AccountTestTools.AVG_TOKEN_SIZE]);
        accountController.finishAccountInit(systemName, accountName);
    }
}

package ru.yandex.market.crm.campaign.test.utils;

import java.util.Set;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.dao.AccountsDao;
import ru.yandex.market.crm.domain.Account;
import ru.yandex.market.mcrm.utils.test.StatefulHelper;

@Component
public class AccountsTeslHelper implements StatefulHelper {
    private final AccountsDao accountsDao;

    public AccountsTeslHelper(AccountsDao accountsDao) {
        this.accountsDao = accountsDao;
    }

    public Account prepareAccount(String accountId, Set<String> accessibleMobileApps) {
        var account = new Account()
                .setId(accountId)
                .setName(accountId)
                .setAccessibleMobileApps(accessibleMobileApps);
        accountsDao.addAccount(account);

        return account;
    }

    @Override
    public void setUp() {
        prepareMarketApp();
    }

    @Override
    public void tearDown() {
    }

    private void prepareMarketApp() {
        prepareAccount(Account.MARKET_ACCOUNT, Set.of(MobileApplication.MARKET_APP));
    }
}

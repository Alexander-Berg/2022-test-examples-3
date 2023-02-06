package ru.yandex.market.loyalty.core.dao;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.BlacklistRecord;
import ru.yandex.market.loyalty.core.service.UserBlacklistService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserBlackListDaoTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private UserBlackListDao userBlackListDao;
    @Autowired
    private UserBlacklistService userBlacklistService;

    @Test
    public void insertFromScratch() {
        userBlackListDao.updateRecords(ImmutableSet.of(
                new BlacklistRecord.Email("first@example.com"),
                new BlacklistRecord.Email("second@example.com")
        ));
        userBlacklistService.reloadBlacklist();

        assertTrue(userBlacklistService.emailInBlacklist("first@example.com"));
        assertTrue(userBlacklistService.emailInBlacklist("second@example.com"));
    }

    @Test
    public void deleteByUpdate() {
        userBlackListDao.updateRecords(ImmutableSet.of(
                new BlacklistRecord.Email("first@example.com"),
                new BlacklistRecord.Email("second@example.com")
        ));

        userBlackListDao.updateRecords(ImmutableSet.of(
                new BlacklistRecord.Email("second@example.com")
        ));
        userBlacklistService.reloadBlacklist();

        assertFalse(userBlacklistService.emailInBlacklist("first@example.com"));
        assertTrue(userBlacklistService.emailInBlacklist("second@example.com"));
    }

    @Test
    public void addByUpdate() {
        userBlackListDao.updateRecords(ImmutableSet.of(
                new BlacklistRecord.Email("first@example.com"),
                new BlacklistRecord.Email("second@example.com")
        ));

        userBlackListDao.updateRecords(ImmutableSet.of(
                new BlacklistRecord.Email("first@example.com"),
                new BlacklistRecord.Email("second@example.com"),
                new BlacklistRecord.Email("third@example.com")
        ));
        userBlacklistService.reloadBlacklist();

        assertTrue(userBlacklistService.emailInBlacklist("first@example.com"));
        assertTrue(userBlacklistService.emailInBlacklist("second@example.com"));
        assertTrue(userBlacklistService.emailInBlacklist("third@example.com"));
    }
}

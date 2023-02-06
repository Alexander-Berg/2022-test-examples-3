package ru.yandex.market.loyalty.core.dao;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.DataVersion;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BadUeUsersDaoTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private BadUeUsersDao badUeUsersDao;
    @Autowired
    private DataVersionDao dataVersionDao;

    @Test
    public void saveUidsBatch() {
        List<Long> testUids = Arrays.asList(100L, 101L, 102L, 103L);

        dataVersionDao.saveDataVersion(DataVersion.BAD_UE_USERS, 1);
        badUeUsersDao.addUids(1, testUids);

        assertEquals(Sets.newHashSet(testUids), badUeUsersDao.getAllRecords());
    }

    @Test
    public void saveNewUidsBatch() {
        List<Long> uids = Arrays.asList(100L, 101L, 102L, 103L);
        List<Long> newUids = Arrays.asList(101L, 102L, 103L);

        dataVersionDao.saveDataVersion(DataVersion.BAD_UE_USERS, 1);
        badUeUsersDao.addUids(1, uids);

        dataVersionDao.saveDataVersion(DataVersion.BAD_UE_USERS, 2);
        badUeUsersDao.addUids(2, newUids);

        badUeUsersDao.cleanupOldVersions();

        assertEquals(Sets.newHashSet(newUids), badUeUsersDao.getAllRecords());
    }

}

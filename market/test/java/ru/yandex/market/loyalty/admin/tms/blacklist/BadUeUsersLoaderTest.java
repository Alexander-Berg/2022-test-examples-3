package ru.yandex.market.loyalty.admin.tms.blacklist;

import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.dao.BadUeUsersDao;
import ru.yandex.market.loyalty.core.dao.DataVersionDao;
import ru.yandex.market.loyalty.core.model.DataVersion;
import ru.yandex.market.loyalty.test.TestFor;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@MockBean(BadUeUsersYtDao.class)
@TestFor(BadUeUsersLoader.class)
public class BadUeUsersLoaderTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private BadUeUsersYtDao badUeUsersYtDao;
    @Autowired
    private BadUeUsersLoader badUeUsersLoader;
    @Autowired
    private BadUeUsersDao badUeUsersDao;
    @Autowired
    private DataVersionDao dataVersionDao;

    @Test
    public void shouldWork() {
        var uidsBefore = List.of(100L, 101L, 102L);
        var uidsAfter = List.of(102L, 103L, 104L);

        doAnswer(invocation -> {
            invocation.getArgument(1, Consumer.class).accept(uidsAfter);
            return null;
        }).when(badUeUsersYtDao).loadBlacklistUidBatch(eq(10), any());

        var initSeqValue = dataVersionDao.createDataVersionNum();
        dataVersionDao.saveDataVersion(DataVersion.BAD_UE_USERS, initSeqValue);
        badUeUsersDao.addUids(initSeqValue, uidsBefore);
        assertTrue("BeforeUids weren't added to the DB", badUeUsersDao.getAllRecords().containsAll(uidsBefore));

        badUeUsersLoader.loadBadUeUsers(10);

        var allNewUids = badUeUsersDao.getAllRecords();
        assertTrue("Not all afterUids exist in the DB", allNewUids.containsAll(uidsAfter));
        assertTrue(
                "There is an beforeUid in the DB",
                !allNewUids.contains(uidsBefore.get(0)) && !allNewUids.contains(uidsBefore.get(1))
        );
    }
}

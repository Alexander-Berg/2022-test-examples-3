package ru.yandex.market.loyalty.admin.tms.repsoitory;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.tms.repository.Shedlock;
import ru.yandex.market.loyalty.admin.tms.repository.ShedlockDao;
import ru.yandex.market.loyalty.admin.tms.repository.ShedlockDisabledHistory;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ShedlockDaoTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String SHEDLOCK_NAME = "SHEDLOCK_NAME";

    @Autowired
    private ShedlockDao dao;

    @Test
    public void insert() {
        Shedlock shedlock = new Shedlock(SHEDLOCK_NAME, null, null, null, null);
        dao.insert(shedlock);

        Shedlock shedlockFromDb = dao.find(SHEDLOCK_NAME);

        assertEquals(shedlock, shedlockFromDb);
    }

    @Test
    public void findAll() {
        Shedlock shedlock = new Shedlock("findAll" + 2, null, null, null, null);
        dao.insert(shedlock);
        Shedlock shedlock2 = new Shedlock("findAll" + 3, null, null, null, null);
        dao.insert(shedlock2);

        Collection<Shedlock> all = dao.findAll();

        assertTrue(all.contains(shedlock));
        assertTrue(all.contains(shedlock2));
    }

    @Test
    public void updateDisabledUntil() {
        Shedlock shedlock = new Shedlock("updateDisabledUntil", null, null, null, null);
        dao.insert(shedlock);

        Date date = new Date();

        dao.updateDisabledUntil("updateDisabledUntil", date);

        Shedlock shedlock1 = dao.find("updateDisabledUntil");

        assertNotNull(shedlock1);
        assertEquals(date, shedlock1.getDisabledUntil());
    }

    @Test
    public void findAllDisabledHistory() {
        dao.insertDisabledHistory("findAllDisabledHistory", null, new Date(), "message1", "someUser");

        Collection<ShedlockDisabledHistory> allDisabledHistory = dao.findAllDisabledHistory();

        Optional<String> any = allDisabledHistory.stream()
                .map(ShedlockDisabledHistory::getName)
                .filter("findAllDisabledHistory"::contains)
                .findAny();
        assertTrue(any.isPresent());
    }
}

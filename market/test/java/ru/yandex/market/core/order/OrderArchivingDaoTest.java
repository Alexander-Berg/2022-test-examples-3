package ru.yandex.market.core.order;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

@DbUnitDataSet(before = "OrderArchivationDaoTest.before.csv")
public class OrderArchivingDaoTest extends FunctionalTest {

    public static final long ARCHIVED_ORDER_ID = 100L;
    public static final long NOT_ARCHIVED_ORDER_ID = 101L;
    public static final long NOT_EXISTED_ORDER_ID = 102L;

    @Autowired
    private OrderArchivingDao orderArchivingDao;

    @Test
    void testArchivedOrder() {
        boolean archived = orderArchivingDao.isArchived(ARCHIVED_ORDER_ID);
        Assertions.assertTrue(archived);
    }

    @Test
    void testNotArchivedOrder() {
        boolean archived = orderArchivingDao.isArchived(NOT_ARCHIVED_ORDER_ID);
        Assertions.assertFalse(archived);
    }

    @Test
    void testNotExistedOrder() {
        boolean archived = orderArchivingDao.isArchived(NOT_EXISTED_ORDER_ID);
        Assertions.assertFalse(archived);
    }
}

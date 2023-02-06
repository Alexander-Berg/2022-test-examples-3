package ru.yandex.market.core.order;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.order.model.MbiOrder;
import ru.yandex.market.core.order.model.MbiOrderBuilder;
import ru.yandex.market.core.order.model.MbiOrderStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Тесты для {@link CpaOrderStatusHistoryDao}.
 *
 * @author vbudnev
 */
class CpaOrderStatusHistoryDaoTest extends FunctionalTest {
    private static final LocalDateTime DT_2018_01_01_010101 = LocalDateTime.of(2018, 1, 1, 1, 1, 1);
    private static final LocalDateTime DT_2018_01_02_101010 = LocalDateTime.of(2018, 1, 2, 10, 10, 10);
    private static final LocalDateTime DT_2018_01_03_101010 = LocalDateTime.of(2018, 1, 3, 10, 10, 10);

    @Autowired
    private CpaOrderStatusHistoryDao cpaOrderStatusHistoryDao;

    private static MbiOrder getSimpleMbiOrder(long orderId, MbiOrderStatus status) {
        Date simpleDate = DateUtil.asDate(DT_2018_01_01_010101);
        return new MbiOrderBuilder()
                .setId(orderId)
                .setStatus(status)
                .setCreationDate(simpleDate)
                .setTrantime(simpleDate)
                .build();
    }

    @DbUnitDataSet(
            before = "db/CpaOrderStatusHistoryDaoTest.before.csv",
            after = "db/CpaOrderStatusHistoryDaoTest.after.csv"
    )
    @Test
    void test_store() {

        //эта запись останется без изменений
        cpaOrderStatusHistoryDao.addStatus(1, MbiOrderStatus.PROCESSING, DT_2018_01_01_010101, 1);
        //эти записи будут добавлены
        cpaOrderStatusHistoryDao.addStatus(1, MbiOrderStatus.DELIVERY, DT_2018_01_01_010101, 2);
        //trantime одинаковое
        cpaOrderStatusHistoryDao.addStatus(1, MbiOrderStatus.DELIVERED, DT_2018_01_02_101010, 3);
        cpaOrderStatusHistoryDao.addStatus(1, MbiOrderStatus.CANCELLED_IN_DELIVERY, DT_2018_01_02_101010, 4);

        //trantime разное но заказ и статус один, добавлена только первая
        cpaOrderStatusHistoryDao.addStatus(100, MbiOrderStatus.DELIVERY, DT_2018_01_01_010101, 1);
        cpaOrderStatusHistoryDao.addStatus(100, MbiOrderStatus.DELIVERY, DT_2018_01_02_101010, 2);
    }

    @DbUnitDataSet(before = "db/CpaOrderStatusHistoryDaoTest.getLastChangedStatusTime.before.csv")
    @Test
    void test_getLastChangedStatusTime() {
        Map<Long, LocalDateTime> timeByOrderId = cpaOrderStatusHistoryDao.getLastChangedStatusTime(
                ImmutableSet.of(1L, 2L, 3L, 4L, 5L, 6L, 100L)
        );

        assertThat(
                timeByOrderId,
                is(ImmutableMap.builder()
                        .put(1L, DT_2018_01_03_101010)
                        .put(2L, DT_2018_01_02_101010)
                        .put(3L, DT_2018_01_03_101010)
                        .put(4L, DT_2018_01_03_101010)
                        .put(5L, DT_2018_01_03_101010)
                        .put(6L, DT_2018_01_03_101010)
                        //100 - no info
                        .build()
                )
        );
    }

    @DbUnitDataSet(before = "db/CpaOrderStatusHistoryDaoTest.getStatusTimeByOrderIdAndStatus.before.csv")
    @Test
    void test_getStatusTimeByOrderIdAndStatus() {
        LocalDateTime deliveredStatusTime = cpaOrderStatusHistoryDao.getStatusTimeByOrderIdAndStatus(
                1L, MbiOrderStatus.DELIVERED
        );

        LocalDateTime cancelledStatusTime = cpaOrderStatusHistoryDao.getStatusTimeByOrderIdAndStatus(
                1L, MbiOrderStatus.CANCELLED_IN_DELIVERY
        );

        assertThat(
                deliveredStatusTime,
                is(DT_2018_01_02_101010)
        );

        assertThat(
                cancelledStatusTime,
                is(DT_2018_01_02_101010)
        );

    }

}

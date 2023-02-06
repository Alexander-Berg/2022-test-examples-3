package ru.yandex.market.admin.service.remote;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.status.UIFinanceStatus;
import ru.yandex.market.admin.ui.model.status.UIShopStatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoteShopStatusServiceTest extends FunctionalTest {
    @Autowired
    RemoteShopStatusService remoteShopStatusService;

    /**
     * Note: дефолтный фид не возвращается в статусе магазина.
     */
    @Test
    @DbUnitDataSet(before = "RemoteShopStatusServiceTest.getShopStatusTest.before.csv")
    void getShopStatusTest() {
        UIShopStatus shopStatus = remoteShopStatusService.getShopStatus(1264);

        long brokenFeedsCount = shopStatus.getLongField(UIShopStatus.BROKEN_FEEDS_COUNT);
        assertEquals(3L, brokenFeedsCount);

        String actualFeedLogs = shopStatus.getStringField(UIShopStatus.FEED_LOGS);
        String expectedFeedLogs = "" +
                "FEED_ID:1612\n" +
                "PARSE_LOG1612\n" +
                "FEED_ID:1613\n" +
                "PARSE_LOG1613\n" +
                "FEED_ID:1614\n" +
                "PARSE_LOG1614\n";
        assertEquals(expectedFeedLogs, actualFeedLogs);

        UIFinanceStatus financeStatus = (UIFinanceStatus) shopStatus.getField(UIShopStatus.FINANCE);
        String actualRemainder = financeStatus.getStringField(UIFinanceStatus.REMAINDER);
        long actualDaysToSpend = financeStatus.getLongField(UIFinanceStatus.DAYS_TO_SPEND);
        assertEquals("111.55", actualRemainder);
        assertEquals(3, actualDaysToSpend);
    }
}

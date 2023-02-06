package ru.yandex.market.ff4shops.api.xml.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;

import static ru.yandex.market.ff4shops.util.FfAsserts.assertXmlEquals;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class CancelOrderTest extends FunctionalTest {


    @Autowired
    TestableClock clock;

    @Test
    @DbUnitDataSet(before = "CancelOrderTest.before.csv", after = "CancelOrderTest.after.csv")
    void success() {
        clock.setFixed(DateTimes.toInstantAtDefaultTz(2020, 1, 1, 15, 30, 0),
                DateTimeUtils.MOSCOW_ZONE);
        assertCancelOrder(
                "ru/yandex/market/ff4shops/api/xml/order/request/cancel_order.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/cancel_order_success.xml"
        );
    }

    private void assertCancelOrder(String requestPath, String responsePath) {
        String result = FunctionalTestHelper.postForXml(
                urlBuilder.url("orders", "cancelOrder"),
                extractFileContent(requestPath)
        ).getBody();
        assertXmlEquals(extractFileContent(responsePath), result);
    }

}

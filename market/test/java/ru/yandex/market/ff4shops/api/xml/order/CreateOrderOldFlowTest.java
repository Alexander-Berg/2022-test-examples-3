package ru.yandex.market.ff4shops.api.xml.order;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;

import static ru.yandex.market.ff4shops.util.FfAsserts.assertXmlEquals;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DbUnitDataSet(before = "environment.before.old_flow.csv")
class CreateOrderOldFlowTest extends FunctionalTest {

    @Test
    public void assertCreateOrderWithOldFlow() {
        assertXmlEquals(
                extractFileContent("ru/yandex/market/ff4shops/api/xml/order/response/create_order_success.xml"),
                FunctionalTestHelper.postForXml(
                        urlBuilder.url("orders", "createOrder"),
                        extractFileContent("ru/yandex/market/ff4shops/api/xml/order/request/create_order.xml")
                ).getBody()
        );
    }
}

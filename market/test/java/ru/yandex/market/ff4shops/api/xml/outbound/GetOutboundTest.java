package ru.yandex.market.ff4shops.api.xml.outbound;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;

import static ru.yandex.market.ff4shops.util.FfAsserts.assertXmlEquals;
import static ru.yandex.market.ff4shops.util.FunctionalTestHelper.postForXml;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Получение данных отправки")
public class GetOutboundTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "getOutbound.before.csv")
    @DisplayName("Отправка существует в БД")
    void getOutbound() {
        verifyGetOutbound("ru/yandex/market/ff4shops/api/xml/outbound/response/get_outbound_success.xml");
    }

    @Test
    @DbUnitDataSet(before = "getOutbound_orderIds.before.csv")
    @DisplayName("Заказы в отправке")
    void getOutboundOrderIds() {
        verifyGetOutbound("ru/yandex/market/ff4shops/api/xml/outbound/response/get_outbound_order_ids.xml");
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Отправка отсутствует в БД")
    void getOutbound_EmptyResponse() {
        verifyGetOutbound("ru/yandex/market/ff4shops/api/xml/outbound/response/get_outbound_response_empty.xml");
    }

    private void verifyGetOutbound(String responsePath) {
        String result = postForXml(
                urlBuilder.url("outbounds", "getOutbound"),
                extractFileContent("ru/yandex/market/ff4shops/api/xml/outbound/request/get_outbound.xml")
        ).getBody();

        assertXmlEquals(extractFileContent(responsePath), result);
    }
}

package ru.yandex.market.ff4shops.api.xml.outbound;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;

import static ru.yandex.market.ff4shops.util.FfAsserts.assertXmlEquals;
import static ru.yandex.market.ff4shops.util.FunctionalTestHelper.postForXml;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Получение истории статусов нескольких отправок")
public class GetOutboundStatusHistoryTest extends FunctionalTest {
    @Test
    @DbUnitDataSet(
            before = "getOutboundStatusHistory.before.csv",
            after = "getOutboundStatusHistory.before.csv"
    )
    @DisplayName("Все отправки существуют в БД")
    void getOutboundStatusHistory() {
        makeCallAndCheckResponse(
                "ru/yandex/market/ff4shops/api/xml/outbound/request/get_outbound_status_history.xml",
                "ru/yandex/market/ff4shops/api/xml/outbound/response/get_outbound_status_history_success.xml"
        );
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Некоторые отправки отсутствуют в БД")
    void getOutboundStatusHistory_EmptyResponse() {
        makeCallAndCheckResponse(
                "ru/yandex/market/ff4shops/api/xml/outbound/request/get_outbound_status_history.xml",
                "ru/yandex/market/ff4shops/api/xml/outbound/response/get_outbound_status_history_response_empty.xml"
        );
    }

    private void makeCallAndCheckResponse(String requestPath, String responsePath) {
        assertXmlEquals(
                extractFileContent(responsePath),
                postForXml(
                        urlBuilder.url("outbounds", "getOutboundStatusHistory"),
                        extractFileContent(requestPath)
                ).getBody()
        );
    }
}

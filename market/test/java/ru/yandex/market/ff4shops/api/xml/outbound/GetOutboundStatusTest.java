package ru.yandex.market.ff4shops.api.xml.outbound;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;

import static ru.yandex.market.ff4shops.util.FfAsserts.assertXmlEquals;
import static ru.yandex.market.ff4shops.util.FunctionalTestHelper.postForXml;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Получение статуса нескольких отправок")
public class GetOutboundStatusTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(
            before = "getOutboundStatus.before.csv",
            after = "getOutboundStatus.before.csv"
    )
    @DisplayName("Все отправки существуют в БД")
    void getOutboundStatus() {
        makeCallAndCheckResponse(
                "ru/yandex/market/ff4shops/api/xml/outbound/request/get_outbound_status.xml",
                "ru/yandex/market/ff4shops/api/xml/outbound/response/get_outbound_status_success.xml"
        );
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Некоторые отправки отсутствуют в БД")
    void getOutboundStatus_EmptyResponse() {
        makeCallAndCheckResponse(
                "ru/yandex/market/ff4shops/api/xml/outbound/request/get_outbound_status.xml",
                "ru/yandex/market/ff4shops/api/xml/outbound/response/get_outbound_status_response_empty.xml"
        );
    }

    private void makeCallAndCheckResponse(String requestPath, String responsePath) {
        assertXmlEquals(
                extractFileContent(responsePath),
                postForXml(
                        urlBuilder.url("outbounds", "getOutboundStatus"),
                        extractFileContent(requestPath)
                ).getBody()
        );
    }
}

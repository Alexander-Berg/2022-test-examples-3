package ru.yandex.market.ff4shops.api.xml.outbound;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;

import static ru.yandex.market.ff4shops.util.FfAsserts.assertXmlEquals;
import static ru.yandex.market.ff4shops.util.FunctionalTestHelper.postForXml;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Создание отправки")
class PutOutboundTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(
            after = "putOutbound.after.csv"
    )
    @DisplayName("Отправка отсутствует в БД")
    void successOutboundNotExists() {
        makeCallAndCheckResponse();
    }

    @Test
    @DbUnitDataSet(
            before = "putOutbound.before.csv",
            after = "putOutbound.after.csv"
    )
    @DisplayName("Отправка существует в БД")
    void successOutboundAlreadyExists() {
        makeCallAndCheckResponse();
    }

    private void makeCallAndCheckResponse() {
        assertXmlEquals(
                extractFileContent("ru/yandex/market/ff4shops/api/xml/outbound/response/put_outbound_success.xml"),
                postForXml(
                        urlBuilder.url("outbounds", "putOutbound"),
                        extractFileContent("ru/yandex/market/ff4shops/api/xml/outbound/request/put_outbound.xml")
                ).getBody()
        );
    }
}

package ru.yandex.market.logistics.logistics4shops.external.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.utils.RestAssuredFactory;

@DisplayName("Создать или обновить заявку на отправку")
@DatabaseSetup("/external/controller/putOutbound/prepare.xml")
class ExternalPutOutboundControllerTest extends AbstractIntegrationTest {
    private static final String URL = "/external/outbounds/putOutbound";

    @Test
    @DisplayName("Отправка существует")
    @ExpectedDatabase(
        value = "/external/controller/putOutbound/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void outboundExist() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/putOutbound/request/outbound_exist.xml",
            "external/controller/putOutbound/response/outbound_exist.xml"
        );
    }

    @Test
    @DisplayName("Отправка существует, в запросе обновляется интервал")
    @ExpectedDatabase(
        value = "/external/controller/putOutbound/outbound_interval_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void outboundExistUpdateInterval() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/putOutbound/request/outbound_exist_update_interval.xml",
            "external/controller/putOutbound/response/outbound_exist.xml"
        );
    }

    @Test
    @DisplayName("Отправка не существует, transportationId с ожидаемым префиксом")
    @ExpectedDatabase(
        value = "/external/controller/putOutbound/outbound_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void outboundNotExist() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/putOutbound/request/outbound_not_exist.xml",
            "external/controller/putOutbound/response/outbound_not_exist.xml"
        );
    }

    @Test
    @DisplayName("Отправка не существует, transportationId без префикса")
    @ExpectedDatabase(
        value = "/external/controller/putOutbound/outbound_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void outboundNotExistEmptyTransportationIdPrefix() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/putOutbound/request/outbound_not_exist_empty_tid_prefix.xml",
            "external/controller/putOutbound/response/outbound_not_exist.xml"
        );
    }

    @Test
    @DisplayName("Отправка не существует, transportationId с некорректным префиксом")
    @ExpectedDatabase(
        value = "/external/controller/putOutbound/outbound_created_wrong_tid_prefix.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void outboundNotExistIncorrectTransportationIdPrefix() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/putOutbound/request/outbound_not_exist_wrong_tid_prefix.xml",
            "external/controller/putOutbound/response/outbound_not_exist.xml"
        );
    }

    @Test
    @DisplayName("Отправка не существует, в запросе нет transportationId")
    @ExpectedDatabase(
        value = "/external/controller/putOutbound/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void outboundNotExistNoTransportationId() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/putOutbound/request/outbound_not_exist_no_outbound_no_transportation_id.xml",
            "external/controller/putOutbound/response/outbound_not_exist_no_outbound_no_transportation_id.xml"
        );
    }

    @Test
    @DisplayName("Не указан интервал")
    @ExpectedDatabase(
        value = "/external/controller/putOutbound/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noInterval() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/putOutbound/request/outbound_without_interval.xml",
            "external/controller/putOutbound/response/outbound_without_interval.xml"
        );
    }
}

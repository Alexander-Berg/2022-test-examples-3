package ru.yandex.market.logistics.logistics4shops.external.controller;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.utils.RestAssuredFactory;

@DisplayName("Получение истории статусов заказов")
@DatabaseSetup("/external/controller/getOutboundStatusHistory/prepare.xml")
@ParametersAreNonnullByDefault
class ExternalGetOutboundStatusHistoryControllerTest extends AbstractIntegrationTest {
    private static final String URL = "external/outbounds/getOutboundStatusHistory";

    @Test
    @DisplayName("Все отправки есть в бд")
    void allOutboundsExist() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOutboundStatusHistory/request/all_in_l4s.xml",
            "external/controller/getOutboundStatusHistory/response/all_in_l4s.xml"
        );
    }

    @Test
    @DisplayName("Некоторых отправок нет")
    void someOutboundsNotExistSuccess() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOutboundStatusHistory/request/some_in_l4s.xml",
            "external/controller/getOutboundStatusHistory/response/some_not_exist.xml"
        );
    }

    @Test
    @DisplayName("Всех отправок нет")
    void outboundsNotExist() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOutboundStatusHistory/request/none_in_l4s.xml",
            "external/controller/getOutboundStatusHistory/response/all_not_exist.xml"
        );
    }

    @Test
    @DisplayName("Пустой идентификатор")
    void emptyResourceId() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOutboundStatusHistory/request/empty_resource_id.xml",
            "external/controller/getOutboundStatusHistory/response/validation_fail.xml"
        );
    }

    @Test
    @DisplayName("Нет идентификаторов")
    void emptyResourceIds() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOutboundStatusHistory/request/empty.xml",
            "external/controller/getOutboundStatusHistory/response/validation_fail.xml"
        );
    }
}

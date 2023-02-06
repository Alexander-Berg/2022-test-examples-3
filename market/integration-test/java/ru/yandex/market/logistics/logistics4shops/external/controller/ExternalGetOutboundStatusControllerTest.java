package ru.yandex.market.logistics.logistics4shops.external.controller;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.utils.RestAssuredFactory;

@DisplayName("Получение статусов отправок")
@DatabaseSetup("/external/controller/getOutboundStatus/prepare.xml")
@ParametersAreNonnullByDefault
class ExternalGetOutboundStatusControllerTest extends AbstractIntegrationTest {
    private static final String URL = "external/outbounds/getOutboundStatus";

    @Test
    @DisplayName("Все отправки есть в бд")
    void allOutboundsExist() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOutboundStatus/request/all_in_l4s.xml",
            "external/controller/getOutboundStatus/response/all_in_l4s.xml"
        );
    }

    @Test
    @DisplayName("Всех отправок нет")
    void allOutboundsNotExist() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOutboundStatus/request/none_in_l4s.xml",
            "external/controller/getOutboundStatus/response/all_not_exist.xml"
        );
    }

    @Test
    @DisplayName("Некоторых отправок нет")
    void someOutboundsNotExist() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOutboundStatus/request/some_in_l4s.xml",
            "external/controller/getOutboundStatus/response/some_not_exist.xml"
        );
    }

    @Test
    @DisplayName("Пустой идентификатор")
    void emptyResourceId() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOutboundStatus/request/empty_resource_id.xml",
            "external/controller/getOutboundStatus/response/validation_fail.xml"
        );
    }

    @Test
    @DisplayName("Нет идентификаторов")
    void emptyResourceIds() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOutboundStatus/request/no_resource_id.xml",
            "external/controller/getOutboundStatus/response/validation_fail.xml"
        );
    }
}

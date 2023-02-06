package ru.yandex.market.logistics.logistics4shops.external.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.utils.RestAssuredFactory;

@DisplayName("Получение истории статусов заказа")
@DatabaseSetup("/external/controller/getOrderHistory/prepare.xml")
class ExternalGetOrderHistoryControllerTest extends AbstractIntegrationTest {
    private static final String URL = "/external/orders/getOrderHistory";

    @Test
    @DisplayName("История есть в базе")
    void exist() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrderHistory/request/exist.xml",
            "external/controller/getOrderHistory/response/exist.xml"
        );
    }

    @Test
    @DisplayName("Истории нет в базе")
    void notExistInL4S() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrderHistory/request/not_exist.xml",
            "external/controller/getOrderHistory/response/not_exist.xml"
        );
    }

    @Test
    @DisplayName("Пустой запрос")
    void empty() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrderHistory/request/empty.xml",
            "external/controller/getOrderHistory/response/validation_failed.xml"
        );
    }
}

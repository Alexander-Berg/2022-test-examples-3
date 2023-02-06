package ru.yandex.market.logistics.logistics4shops.external.controller;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.utils.RestAssuredFactory;

@DisplayName("Получение статусов заказов")
@DatabaseSetup("/external/controller/getOrdersStatus/prepare.xml")
@ParametersAreNonnullByDefault
class ExternalGetOrdersStatusControllerTest extends AbstractIntegrationTest {
    private static final String URL = "/external/orders/getOrdersStatus";

    @Test
    @DisplayName("Все статусы есть в базе")
    void allCheckpointsExist() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrdersStatus/request/all_checkpoints_exist.xml",
            "external/controller/getOrdersStatus/response/all_checkpoints_exist.xml"
        );
    }

    @Test
    @DisplayName("Части статусов нет в базе")
    void notAllCheckpointsExist() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrdersStatus/request/not_all_checkpoints_exist.xml",
            "external/controller/getOrdersStatus/response/not_all_checkpoints_exist.xml"
        );
    }

    @Test
    @DisplayName("Всех статусов нет в базе")
    void allCheckpointsNotExist() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrdersStatus/request/all_checkpoints_not_exist.xml",
            "external/controller/getOrdersStatus/response/empty.xml"
        );
    }

    @Test
    @DisplayName("Пустой запрос")
    void empty() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrdersStatus/request/empty.xml",
            "external/controller/getOrdersStatus/response/validation_fail.xml"
        );
    }

    @Test
    @DisplayName("В запросе есть одинаковые идентификаторы")
    void duplicates() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrdersStatus/request/duplicates.xml",
            "external/controller/getOrdersStatus/response/duplicates.xml"
        );
    }
}

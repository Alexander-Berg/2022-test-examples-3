package ru.yandex.market.api.partner.controllers.order;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import ru.yandex.market.api.partner.context.Format;

class OrderControllerItemInstanceTest extends AbstractOrderItemControllerTest {

    @Test
    void test_putOrderItemInstances_Ok_xml() {
        testApiInteractionThroughCheckouter(
                "api-put-item-instances-ok-request.xml",
                "api-put-item-instances-answer.xml",
                "checkouter-put-item-instances-ok-answer.json",
                "checkouter-put-item-instances-ok-request.json",
                Format.XML,
                HttpStatus.OK,
                DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                DROPSHIP_BY_SELLER_ID,
                SOME_ORDER_ID
        );
    }

    @Test
    void test_putOrderItemInstances_Ok_json() {
        testApiInteractionThroughCheckouter(
                "api-put-item-instances-ok-request.json",
                "api-put-item-instances-answer.json",
                "checkouter-put-item-instances-ok-answer.json",
                "checkouter-put-item-instances-ok-request.json",
                Format.JSON,
                HttpStatus.OK,
                DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                DROPSHIP_BY_SELLER_ID,
                SOME_ORDER_ID
        );
    }

    /**
     * Пустой ответ от чекаутера приводит к 500ке, так как при это исключительная ситуация и что-то пошло нетак.
     * <p>
     * Содержимое запроса ПАПИ не имеет значение для теста, просто должно быть валидным.
     * Ответ ПАПИ также игнорируется.
     */
    @Test
    void test_putOrderItems_when_checkouterAnswersWithEmptyBody_should_throw() {
        Assertions.assertThrows(
                Exception.class,
                () -> testApiInteractionThroughCheckouter(
                        "api-put-item-instances-ok-request.json",
                        "api-put-item-instances-answer.json",
                        "checkouter-put-items-empty-answer.json",
                        "checkouter-put-items-ok-request.json",
                        Format.XML,
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                        DROPSHIP_BY_SELLER_ID,
                        SOME_ORDER_ID
                )
        );
    }

    @Test
    void test_putOrderItemInstances_bad_request_xml() {
        testApiInteractionThroughCheckouterWithBadRequest(
                "api-put-item-instances-ok-request.xml",
                "api-put-item-instances-bad-request-answer.xml",
                "checkouter-put-item-instances-bad-request-answer.json",
                "checkouter-put-item-instances-ok-request.json",
                Format.XML,
                HttpStatus.BAD_REQUEST,
                DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                DROPSHIP_BY_SELLER_ID,
                SOME_ORDER_ID
        );
    }

    @Test
    void test_putOrderItemInstances_bad_request_json() {
        testApiInteractionThroughCheckouterWithBadRequest(
                "api-put-item-instances-ok-request.json",
                "api-put-item-instances-bad-request-answer.json",
                "checkouter-put-item-instances-bad-request-answer.json",
                "checkouter-put-item-instances-ok-request.json",
                Format.JSON,
                HttpStatus.BAD_REQUEST,
                DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                DROPSHIP_BY_SELLER_ID,
                SOME_ORDER_ID
        );
    }


    @Override
    String getResourcePath() {
        return "resources/order-item-instances/";
    }

    @Override
    String getCheckouterRequestUrl() {
        return "%s/orders/%s/items/instances?clientRole=SHOP&clientId=%s&shopId=";
    }

    @Override
    String getApiRequestUrl() {
        return "%s/campaigns/%s/orders/%s/cis.%s?clientId=%s";
    }
}

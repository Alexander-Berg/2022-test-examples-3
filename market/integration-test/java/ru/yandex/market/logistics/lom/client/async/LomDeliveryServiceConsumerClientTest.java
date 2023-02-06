package ru.yandex.market.logistics.lom.client.async;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.gateway.common.model.common.Courier;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCode;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.gateway.common.model.common.Person;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderDeliveryDate;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.ShipmentType;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

class LomDeliveryServiceConsumerClientTest extends AbstractClientTest {
    private static final Long PARTNER_ID = 1001L;
    private static final String BARCODE = "LOinttest-1";
    private static final Long SEQUENCE_ID = 1L;

    @Autowired
    private LomDeliveryServiceConsumerClient consumerClient;

    @Test
    void createOrderSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/processing/ds/createSuccess"))
            .andExpect(jsonRequestContent("request/order/processing/create_success.json"))
            .andRespond(withSuccess());

        consumerClient.setCreateOrderSuccess(
            SEQUENCE_ID,
            BARCODE,
            null,
            "test-external-id",
            PARTNER_ID
        );
    }

    @Test
    void createOrderSuccessNotFound() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/processing/ds/createSuccess"))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND)
                    .body(extractFileContent("response/order/order_not_found.json"))
                    .contentType(MediaType.APPLICATION_JSON)
            );

        softly.assertThatThrownBy(
            () -> consumerClient.setCreateOrderSuccess(
                SEQUENCE_ID,
                "LO-800",
                null,
                "test-external-id",
                PARTNER_ID
            )
        )
            .isInstanceOf(HttpTemplateException.class)
            .hasMessage("Http request exception: status <404>, response body <{\n"
                + "   \"message\": \"Failed to find [ORDER] with id [1]\",\n"
                + "   \"resourceType\": \"ORDER\",\n"
                + "   \"identifier\": \"1\"\n"
                + "}\n"
                + ">.");
    }

    @Test
    void createOrderError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/processing/ds/createError"))
            .andExpect(jsonRequestContent("request/order/processing/create_error.json"))
            .andRespond(withSuccess());

        consumerClient.setCreateOrderError(SEQUENCE_ID, BARCODE, PARTNER_ID, "error message");
    }

    @Test
    void createIntakeSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/shipments/ds/createIntakeSuccess"))
            .andExpect(jsonRequestContent("request/shipment/create_intake_success.json"))
            .andRespond(withSuccess());

        consumerClient.setCreateIntakeSuccess(SEQUENCE_ID, 1L, "3123312", PARTNER_ID);
    }

    @Test
    void createIntakeError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/shipments/ds/createIntakeError"))
            .andExpect(jsonRequestContent("request/shipment/create_intake_error.json"))
            .andRespond(withSuccess());

        consumerClient.setCreateIntakeError(SEQUENCE_ID, 1L, PARTNER_ID, "error message");
    }

    @Test
    void createSelfExportSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/shipments/ds/createSelfExportSuccess"))
            .andExpect(jsonRequestContent("request/shipment/create_self_export_success.json"))
            .andRespond(withSuccess());

        consumerClient.setCreateSelfExportSuccess(
            SEQUENCE_ID,
            1L,
            "3123312",
            PARTNER_ID
        );
    }

    @Test
    void createSelfExportError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/shipments/ds/createSelfExportError"))
            .andExpect(jsonRequestContent("request/shipment/create_self_export_error.json"))
            .andRespond(withSuccess());

        consumerClient.setCreateSelfExportError(SEQUENCE_ID, 1L, PARTNER_ID, "error message");
    }

    @Test
    void setLabelSuccess() {
        mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/labels/setSuccess"))
            .andExpect(jsonRequestContent("request/document/set_label.json"))
            .andRespond(withSuccess());

        consumerClient.setOrderLabelSuccess(SEQUENCE_ID, "LOinttest-1", "2", "https://localhost", PARTNER_ID);
    }

    @Test
    void setLabelError() {
        mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/labels/setError"))
            .andExpect(jsonRequestContent("request/document/set_label_error.json"))
            .andRespond(withSuccess());

        consumerClient.setOrderLabelError(SEQUENCE_ID, "LOinttest-1", PARTNER_ID);
    }

    @Test
    void createRegistrySuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/registries/ds/createSuccess"))
            .andExpect(content().json("{\"sequenceId\":1,\"registryId\":\"1\",\"externalId\":\"e1\"}", true))
            .andRespond(withSuccess());

        consumerClient.setCreateRegisterSuccess(SEQUENCE_ID, "1", "e1");
    }

    @Test
    void createRegistryError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/registries/ds/createError"))
            .andExpect(content().json("{\"sequenceId\":1,\"registryId\":\"1\",\"message\":\"error message\",\n" +
                "  \"techError\": false}", true))
            .andRespond(withSuccess());

        consumerClient.setCreateRegisterError(SEQUENCE_ID, "1", "error message");
    }

    @Test
    void setAcceptanceCertificateSuccess() {
        mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/get-acceptance-certificates/ds/setSuccess"))
            .andExpect(jsonRequestContent("request/document/set_acceptance_certificate.json"))
            .andRespond(withSuccess());

        consumerClient.setGetAttachedDocsSuccess(
            SEQUENCE_ID,
            ResourceId.builder().setYandexId("4").build(),
            ShipmentType.ACCEPTANCE,
            new DateTime("2019-08-05T13:02:01"),
            10L,
            "https://some.nice/doc/url"
        );
    }

    @Test
    void setAcceptanceCertificateError() {
        mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/get-acceptance-certificates/ds/setError"))
            .andExpect(jsonRequestContent("request/document/set_acceptance_certificate_error.json"))
            .andRespond(withSuccess());

        consumerClient.setGetAttachedDocsError(
            SEQUENCE_ID,
            ResourceId.builder().setYandexId("4").build(),
            10L,
            "Error 100"
        );
    }

    @Test
    void setCancelOrderSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/cancel/success"))
            .andExpect(jsonRequestContent("request/order/cancel/set_order_cancel_success.json"))
            .andRespond(withSuccess());
        consumerClient.setCancelOrderSuccess(SEQUENCE_ID, "LO143", 48L);
    }

    @Test
    void setCancelOrderError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/cancel/error"))
            .andExpect(jsonRequestContent("request/order/cancel/set_order_cancel_error.json"))
            .andRespond(withSuccess());
        consumerClient.setCancelOrderError(SEQUENCE_ID, "LO143", 48L, false, "Error 100", 9999);
    }

    @Test
    void setUpdateOrderItemsSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/update-items/success"))
            .andExpect(jsonRequestContent("request/order/items/update_items_success.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderItemsSuccess(SEQUENCE_ID, "LO143", 48L);
    }

    @Test
    void setUpdateOrderItemsError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/update-items/error"))
            .andExpect(jsonRequestContent("request/order/items/update_items_error.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderItemsError(SEQUENCE_ID, "LO143", 48L, "Something went wrong", 9999);
    }

    @Test
    void setUpdateOrderItemsInstancesSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/update-items-instances/success"))
            .andExpect(jsonRequestContent("request/order/items/update_items_success.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderItemsInstancesSuccess(SEQUENCE_ID, "LO143", 48L);
    }

    @Test
    void setUpdateOrderItemsInstancesError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/update-items-instances/error"))
            .andExpect(jsonRequestContent("request/order/items/update_items_error.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderItemsInstancesError(SEQUENCE_ID, "LO143", 48L, "Something went wrong", 9999);
    }

    @Test
    void setUpdateOrderSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/processing/ds/updateSuccess"))
            .andExpect(jsonRequestContent("request/order/processing/update_success.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderSuccess(SEQUENCE_ID, "LO143", 48L);
    }

    @Test
    void setUpdateOrderError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/processing/ds/updateError"))
            .andExpect(jsonRequestContent("request/order/processing/update_error.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderError(SEQUENCE_ID, "LO143", 48L, 9999, "Something went wrong");
    }

    @Test
    void setGetOrdersDeliveryDateSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/getDeliveryDatesSuccess"))
            .andExpect(jsonRequestContent("request/order/ds/deliveryDates/get_success.json"))
            .andRespond(withSuccess());

        List<OrderDeliveryDate> orderDeliveryDates = List.of(
            new OrderDeliveryDate(
                ResourceId.builder().setYandexId("123").build(),
                DateTime.fromLocalDateTime(LocalDateTime.of(2021, 3, 1, 10, 0)),
                TimeInterval.of(LocalTime.of(10, 0), LocalTime.of(20, 0)),
                "Delivery Dates"
            )
        );

        consumerClient.setGetOrdersDeliveryDateSuccess(SEQUENCE_ID, orderDeliveryDates, 48L);
    }

    @Test
    void setGetOrdersDeliveryDateError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/getDeliveryDatesError"))
            .andExpect(jsonRequestContent("request/order/ds/deliveryDates/get_tech_error.json"))
            .andRespond(withSuccess());

        List<ResourceId> orderIds = List.of(
            ResourceId.builder().setYandexId("123").build()
        );

        consumerClient.setGetOrdersDeliveryDateError(SEQUENCE_ID, orderIds, 48L, true, "Unexpected technical error");
    }

    @Test
    void setUpdateOrderDeliveryDateSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/updateDeliveryDateSuccess"))
            .andExpect(jsonRequestContent("request/order/ds/deliveryDates/update_success.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderDeliveryDateSuccess(SEQUENCE_ID, "test-barcode", 123L);
    }

    @Test
    void setUpdateOrderDeliveryDateError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/updateDeliveryDateError"))
            .andExpect(jsonRequestContent("request/order/ds/deliveryDates/update_error.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderDeliveryDateError(SEQUENCE_ID, "test-barcode", 123L, 9999, "Something went wrong");
    }

    @Test
    void setUpdateOrderRecipientSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/updateRecipientSuccess"))
            .andExpect(jsonRequestContent("request/order/ds/recipient/update_success.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderRecipientSuccess(SEQUENCE_ID, "test-barcode", 123L);
    }

    @Test
    void setUpdateOrderRecipientError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/updateRecipientError"))
            .andExpect(jsonRequestContent("request/order/ds/recipient/update_error.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderRecipientError(
            SEQUENCE_ID,
            "test-barcode",
            123L,
            true,
            9999,
            "Something went wrong"
        );
    }

    @Test
    void setUpdateOrderTransferCodesSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/updateTransferCodes/success"))
            .andExpect(jsonRequestContent("request/order/processing/update_success.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderTransferCodesSuccess(SEQUENCE_ID, "LO143", 48L);
    }

    @Test
    void setUpdateOrderTransferCodesError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/updateTransferCodes/error"))
            .andExpect(jsonRequestContent("request/order/processing/update_error.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderTransferCodesError(SEQUENCE_ID, "LO143", 48L, 9999, "Something went wrong");
    }

    @Test
    void setUpdateCourierSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/updateCourier/success"))
            .andExpect(jsonRequestContent("request/order/processing/update_success.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateCourierSuccess(SEQUENCE_ID, "LO143", 48L);
    }

    @Test
    void setUpdateCourierError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/updateCourier/error"))
            .andExpect(jsonRequestContent("request/order/processing/update_error.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateCourierError(SEQUENCE_ID, "LO143", 48L, 9999, "Something went wrong");
    }

    @Test
    void setGetCourierSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/getCourier/success"))
            .andExpect(jsonRequestContent("request/order/ds/courier/get_success.json"))
            .andRespond(withSuccess());

        consumerClient.setGetCourierSuccess(
            SEQUENCE_ID,
            "LO143",
            48L,
            Courier.builder()
                .setPersons(List.of(new Person("Иван", "Иванов", null)))
                .build(),
            new OrderTransferCodes.OrderTransferCodesBuilder()
                .setInbound(new OrderTransferCode.OrderTransferCodeBuilder()
                    .setVerification("12345")
                    .setElectronicAcceptanceCertificate("asd123")
                    .build()
                )
                .setOutbound(new OrderTransferCode.OrderTransferCodeBuilder()
                    .setVerification("12345")
                    .setElectronicAcceptanceCertificate("asd123")
                    .build()
                )
                .build()
        );
    }

    @Test
    void setGetCourierError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ds/getCourier/error"))
            .andExpect(jsonRequestContent("request/order/ds/courier/get_error.json"))
            .andRespond(withSuccess());

        consumerClient.setGetCourierError(SEQUENCE_ID, "LO143", 48L, 9999, "Something went wrong");
    }
}

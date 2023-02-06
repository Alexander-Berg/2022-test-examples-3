package ru.yandex.market.logistics.lom.client.async;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

class LomFulfillmentConsumerClientTest extends AbstractClientTest {

    private static final String BARCODE = "LO-300";
    private static final String SHIPMENT_ID = "123";
    private static final Long PARTNER_ID = 1001L;
    private static final String DOCUMENT_URL = "http://example.com/test.pdf";
    private static final Long SEQUENCE_ID = 1L;

    @Autowired
    private LomFulfillmentConsumerClient consumerClient;

    @Test
    void createRegistryFulfillmentSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/registries/ff/createSuccess"))
            .andExpect(content().json("{\"sequenceId\": 1,\"registryId\":\"1\",\"externalId\":\"e1\"}", true))
            .andRespond(withSuccess());

        consumerClient.setCreateRegisterSuccess(SEQUENCE_ID, "1", "e1");
    }

    @Test
    void createRegistryError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/registries/ff/createError"))
            .andExpect(content().json("{\"sequenceId\":1,\"registryId\":\"1\",\"message\":\"error message\"," +
                "\"techError\": false}", true))
            .andRespond(withSuccess());

        consumerClient.setCreateRegisterError(SEQUENCE_ID, "1", "error message");
    }

    @Test
    void createReturnRegistrySuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/returnRegistries/createSuccess"))
            .andExpect(jsonRequestContent("request/shipment/create_return_registry_success.json"))
            .andRespond(withSuccess());

        consumerClient.setCreateReturnRegisterSuccess(SEQUENCE_ID, "1", List.of("order1", "order2"));
    }

    @Test
    void createReturnRegistryError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/returnRegistries/createError"))
            .andExpect(jsonRequestContent("request/shipment/create_return_registry_error.json"))
            .andRespond(withSuccess());

        consumerClient.setCreateReturnRegisterError(
            SEQUENCE_ID,
            "1",
            List.of("order1", "order2"),
            "error message"
        );
    }

    @Test
    void createIntakeSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/shipments/ff/createIntakeSuccess"))
            .andExpect(jsonRequestContent("request/shipment/create_intake_success.json"))
            .andRespond(withSuccess());

        consumerClient.setCreateIntakeSuccess(SEQUENCE_ID, 1L, "3123312", PARTNER_ID);
    }

    @Test
    void createIntakeError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/shipments/ff/createIntakeError"))
            .andExpect(jsonRequestContent("request/shipment/create_intake_error.json"))
            .andRespond(withSuccess());

        consumerClient.setCreateIntakeError(SEQUENCE_ID, 1L, PARTNER_ID, "error message");
    }

    @Test
    void createSelfExportSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/shipments/ff/createSelfExportSuccess"))
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
            .andExpect(requestTo(uri + "/shipments/ff/createSelfExportError"))
            .andExpect(jsonRequestContent("request/shipment/create_self_export_error.json"))
            .andRespond(withSuccess());

        consumerClient.setCreateSelfExportError(SEQUENCE_ID, 1L, PARTNER_ID, "error message");
    }

    @Test
    void createOrderSuccess() throws IOException {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/processing/ff/createSuccess"))
            .andExpect(jsonRequestContent("request/order/processing/create_success_ff.json"))
            .andRespond(withSuccess());

        consumerClient.setCreateOrderSuccess(
            SEQUENCE_ID,
            BARCODE,
            "test-external-id",
            PARTNER_ID
        );
    }

    @Test
    void createOrderSuccessNotFound() throws IOException {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/processing/ff/createSuccess"))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND)
                    .body(extractFileContent("response/order/order_not_found.json"))
                    .contentType(MediaType.APPLICATION_JSON)
            );

        softly.assertThatThrownBy(
            () -> consumerClient.setCreateOrderSuccess(
                SEQUENCE_ID,
                "LO-800",
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
    void createOrderError() throws IOException {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/processing/ff/createError"))
            .andExpect(jsonRequestContent("request/order/processing/create_error_ff.json"))
            .andRespond(withSuccess());

        consumerClient.setCreateOrderError(SEQUENCE_ID, "LO-300", PARTNER_ID, null, "error message");
    }

    @Test
    void createAcceptanceCertificateSuccess() {
        mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/get-acceptance-certificates/ff/setSuccess"))
            .andExpect(jsonRequestContent("request/document/set_acceptance_certificate_ff.json"))
            .andRespond(withSuccess());

        consumerClient.setGetAttachedDocsSuccess(
            SEQUENCE_ID,
            SHIPMENT_ID,
            PARTNER_ID,
            DOCUMENT_URL
        );
    }

    @Test
    void createAcceptanceCertificateError() {
        mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/get-acceptance-certificates/ff/setError"))
            .andExpect(jsonRequestContent("request/document/set_acceptance_certificate_error_ff.json"))
            .andRespond(withSuccess());

        consumerClient.setGetAttachedDocsError(
            SEQUENCE_ID,
            SHIPMENT_ID,
            PARTNER_ID,
            "error message"
        );
    }

    @Test
    void setCancelOrderSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ff/cancel/success"))
            .andExpect(jsonRequestContent("request/order/cancel/set_order_cancel_success.json"))
            .andRespond(withSuccess());

        consumerClient.setCancelOrderSuccess(SEQUENCE_ID, "LO143", 48L);
    }

    @Test
    void setCancelOrderError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ff/cancel/error"))
            .andExpect(jsonRequestContent("request/order/cancel/set_order_cancel_error.json"))
            .andRespond(withSuccess());

        consumerClient.setCancelOrderError(SEQUENCE_ID, "LO143", 48L, false, "Error 100", 9999);
    }

    @Test
    void setUpdateOrderSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/processing/ff/updateSuccess"))
            .andExpect(jsonRequestContent("request/order/processing/update_success.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderSuccess(SEQUENCE_ID, "LO143", 48L);
    }

    @Test
    void setUpdateOrderError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/processing/ff/updateError"))
            .andExpect(jsonRequestContent("request/order/processing/update_error.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderError(SEQUENCE_ID, "LO143", 48L, 9999, "Something went wrong");
    }

    @Test
    void setUpdateOrderItemsSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ff/update-items/success"))
            .andExpect(jsonRequestContent("request/order/items/update_items_success.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderItemsSuccess(SEQUENCE_ID, "LO143", 48L);
    }

    @Test
    void setUpdateOrderItemsError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ff/update-items/error"))
            .andExpect(jsonRequestContent("request/order/items/update_items_error.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateOrderItemsError(SEQUENCE_ID, "LO143", 48L, "Something went wrong", 9999);
    }

    @Test
    void setGetOrderSuccess() {
        mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/orders/ff/get/success"))
            .andExpect(jsonRequestContent("request/order/set/success.json"))
            .andRespond(withSuccess());

        consumerClient.setGetOrderSuccess(SEQUENCE_ID, 48L, null);
    }

    @Test
    void setGetOrderError() {
        mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/orders/ff/get/error"))
            .andExpect(jsonRequestContent("request/order/set/error.json"))
            .andRespond(withSuccess());

        consumerClient.setGetOrderError(SEQUENCE_ID, "LO143", 48L, "Something went wrong");
    }

    @Test
    void setUpdateCourierSuccess() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ff/updateCourier/success"))
            .andExpect(jsonRequestContent("request/order/processing/update_success.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateCourierSuccess(SEQUENCE_ID, "LO143", 48L);
    }

    @Test
    void setUpdateCourierError() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/ff/updateCourier/error"))
            .andExpect(jsonRequestContent("request/order/processing/update_error.json"))
            .andRespond(withSuccess());

        consumerClient.setUpdateCourierError(SEQUENCE_ID, "LO143", 48L, 9999, "Something went wrong");
    }
}

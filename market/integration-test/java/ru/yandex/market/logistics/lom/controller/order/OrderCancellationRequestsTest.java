package ru.yandex.market.logistics.lom.controller.order;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_AND_SERVICE_HEADERS;
import static ru.yandex.market.logistics.lom.utils.TestUtils.NOT_EMPTY_ERROR_MESSAGE;
import static ru.yandex.market.logistics.lom.utils.TestUtils.fieldValidationErrorMatcher;
import static ru.yandex.market.logistics.lom.utils.TestUtils.objectValidationErrorMatcher;
import static ru.yandex.market.logistics.lom.utils.TestUtils.toHttpHeaders;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class OrderCancellationRequestsTest extends AbstractContextualTest {

    @Autowired
    private TvmClientApi tvmClientApi;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(strings = {
        "controller/order/cancellationrequests/request/confirm_request_id.json",
        "controller/order/cancellationrequests/request/confirm_barcode.json"
    })
    @DisplayName("Подтверждение заявки на отмену заказа")
    @DatabaseSetup("/controller/order/cancellationrequests/before/setup.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancellationrequests/after/confirm_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void confirmOrder(String request) throws Exception {
        confirm(
            request,
            "controller/order/cancellationrequests/response/confirm_order.json"
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(strings = {
        "controller/order/cancellationrequests/request/confirm_null_reason.json",
        "controller/order/cancellationrequests/request/confirm_empty_reason.json"
    })
    @DisplayName("Подтверждение заявки с пустой причиной")
    @DatabaseSetup("/controller/order/cancellationrequests/before/setup.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancellationrequests/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void confirmOrderEmptyReason(String request) throws Exception {
        confirmRequestWithEmptyReason(request);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(strings = {
        "controller/order/cancellationrequests/request/confirm_empty_request_id.json",
        "controller/order/cancellationrequests/request/confirm_empty_barcode.json",
        "controller/order/cancellationrequests/request/confirm_without_ids.json"
    })
    @DisplayName("Подтверждение заявки на отмену заказа без идентификаторов")
    @DatabaseSetup("/controller/order/cancellationrequests/before/setup.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancellationrequests/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void confirmOrderEmptyIds(String request) throws Exception {
        confirmRequestWithoutIds(
            request,
            "cancellationOrderConfirmationDto",
            "request ids or order barcodes must be specified"
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(strings = {
        "controller/order/cancellationrequests/request/confirm_null_barcode.json",
        "controller/order/cancellationrequests/request/confirm_null_request_id.json"
    })
    @DisplayName("Подтверждение заявки на отмену заказа с null идентификаторами (отсутствие изменений)")
    @DatabaseSetup("/controller/order/cancellationrequests/before/setup.xml")
    @ExpectedDatabase(
        value = "/controller/order/cancellationrequests/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void confirmOrderNullIds(String request) throws Exception {
        confirm(
            request,
            "controller/order/cancellationrequests/response/confirm_empty.json"
        );
    }

    private void confirmRequestWithEmptyReason(String request) throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        mockMvc.perform(
            post("/cancellation-requests/order/confirm")
                .headers(toHttpHeaders(USER_AND_SERVICE_HEADERS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(request))
        )
            .andExpect(status().isBadRequest())
            .andExpect(fieldValidationErrorMatcher("reason", NOT_EMPTY_ERROR_MESSAGE));
    }

    private void confirmRequestWithoutIds(
        String request,
        String field,
        String error
    ) throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        mockMvc.perform(
            post("/cancellation-requests/order/confirm")
                .headers(toHttpHeaders(USER_AND_SERVICE_HEADERS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(request))
        )
            .andExpect(status().isBadRequest())
            .andExpect(objectValidationErrorMatcher(field, error));
    }

    private void confirm(String request, String response) throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        mockMvc.perform(
            post("/cancellation-requests/order/confirm")
                .headers(toHttpHeaders(USER_AND_SERVICE_HEADERS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(request))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(response));
    }
}

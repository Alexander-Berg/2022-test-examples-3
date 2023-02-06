package ru.yandex.market.logistics.lom.controller.order.processing;

import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderRecipientErrorDto;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderRecipientSuccessDto;
import ru.yandex.market.logistics.lom.repository.OrderRepository;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/order/recipient/async/before/setup.xml")
public class DeliveryServiceUpdateRecipientAsyncControllerTest extends AbstractContextualTest {
    private static final Instant FIXED_TIME = Instant.parse("2021-03-02T10:00:00Z");

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Успешный ответ")
    @ExpectedDatabase(
        value = "/controller/order/recipient/async/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successResponse() throws Exception {
        UpdateOrderRecipientSuccessDto request = new UpdateOrderRecipientSuccessDto(10L, "1001", 1L);
        performSuccessRequest(request)
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Неуспешный ответ: Беру - бизнес-ошибка с кодом 4000")
    @ExpectedDatabase(
        value = "/controller/order/recipient/async/after/fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void failResponseCode4000() throws Exception {
        performErrorRequest(createErrorDto(false, 4000))
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.NOTIFY_ORDER_UPDATE_RECIPIENT_ERROR);
    }

    @Test
    @DisplayName("Неуспешный ответ: Беру - бизнес-ошибка с кодом отличным от 4000")
    @ExpectedDatabase(
        value = "/controller/order/recipient/async/after/error_code_not_4000.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void failResponseCodeNot4000() throws Exception {
        performErrorRequest(createErrorDto(false, 9404))
            .andExpect(status().isOk());

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.NOTIFY_ORDER_UPDATE_RECIPIENT_ERROR);
    }

    @Test
    @DisplayName("Неуспешный ответ: Беру - техническая ошибка")
    @ExpectedDatabase(
        value = "/controller/order/recipient/async/after/error_is_tech.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void failResponseTechError() throws Exception {
        performErrorRequest(createErrorDto(true, null))
            .andExpect(status().isOk());

        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.NOTIFY_ORDER_UPDATE_RECIPIENT_ERROR);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("invalidResponseForYadoOrder")
    @DisplayName("Неуспешный ответ: ЯДо - любая ошибка")
    @ExpectedDatabase(
        value = "/controller/order/recipient/async/after/fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void failResponseYadoOrder(UpdateOrderRecipientErrorDto dto) throws Exception {
        Order order = orderRepository.findByBarcode(dto.getBarcode()).orElseThrow();
        order.setPlatformClient(PlatformClient.YANDEX_DELIVERY);
        orderRepository.save(order);

        performErrorRequest(dto)
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.NOTIFY_ORDER_UPDATE_RECIPIENT_ERROR);
    }

    @Test
    @DisplayName("Невалидный успешный ответ: некорректный sequenseId")
    @ExpectedDatabase(
        value = "/controller/order/recipient/async/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void invalidSuccessResponseNotFoundSequenseId() throws Exception {
        performSuccessRequest(new UpdateOrderRecipientSuccessDto(11L, "1001", 1L))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [BUSINESS_PROCESS] with id [11]"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Невалидный успешный ответ")
    @MethodSource("invalidSuccessResponse")
    @ExpectedDatabase(
        value = "/controller/order/recipient/async/after/invalid_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void invalidSuccessResponse(
        String name,
        UpdateOrderRecipientSuccessDto request,
        ResultMatcher result
    ) throws Exception {
        performSuccessRequest(request)
            .andExpect(status().is4xxClientError())
            .andExpect(result);
    }

    @Nonnull
    private static Stream<Arguments> invalidResponseForYadoOrder() {
        return Stream.of(
            Arguments.of(createErrorDto(true, null)),
            Arguments.of(createErrorDto(false, 9404)),
            Arguments.of(createErrorDto(false, 4000))
        );
    }

    @Nonnull
    private static Stream<Arguments> invalidSuccessResponse() {
        return Stream.of(
            Arguments.of(
                "некорректный barcode",
                new UpdateOrderRecipientSuccessDto(10L, "1002", 1L),
                errorMessage("Incorrect barcode 1002 for segment change request 1")
            ),
            Arguments.of(
                "некорректный updateRequestId",
                new UpdateOrderRecipientSuccessDto(10L, "1001", 11L),
                errorMessage("Failed to find [ORDER_CHANGE_SEGMENT_REQUEST] with id [11]")
            )
        );
    }

    @Nonnull
    private ResultActions performSuccessRequest(UpdateOrderRecipientSuccessDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/ds/updateRecipientSuccess", request));
    }

    @Nonnull
    private ResultActions performErrorRequest(UpdateOrderRecipientErrorDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/ds/updateRecipientError", request));
    }

    @Nonnull
    private static UpdateOrderRecipientErrorDto createErrorDto(boolean isTechError, Integer errorCode) {
        return new UpdateOrderRecipientErrorDto(
            10L,
            "1001",
            1L,
            isTechError,
            "fail",
            errorCode
        );
    }
}

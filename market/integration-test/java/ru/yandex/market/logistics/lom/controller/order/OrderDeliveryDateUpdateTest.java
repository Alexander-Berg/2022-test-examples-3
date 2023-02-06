package ru.yandex.market.logistics.lom.controller.order;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderDeliveryDateRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static ru.yandex.market.logistics.lom.utils.TestUtils.NOT_NULL_ERROR_MESSAGE;
import static ru.yandex.market.logistics.lom.utils.TestUtils.fieldValidationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Подтверждение обновления товаров в заказе")
@ParametersAreNonnullByDefault
@DatabaseSetup("/controller/order/deliverydate/before/setup.xml")
class OrderDeliveryDateUpdateTest extends AbstractContextualTest {
    private static final ChangeOrderRequestPayload PAYLOAD = PayloadFactory.createChangeOrderRequestPayload(1, "1", 1);
    private static final Instant FIXED_TIME = Instant.parse("2021-03-10T10:00:00Z");

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Успешное создание заявки на изменение даты")
    @ExpectedDatabase(
        value = "/controller/order/deliverydate/after/existing_change_request_same_ext_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateDelvieryDateSuccess() throws Exception {
        performRequest(validRequestBuilder().build())
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD);
    }

    @Test
    @DisplayName("Попытка создать еще одну заявку с новым externalId")
    @DatabaseSetup(
        value = "/controller/order/deliverydate/after/existing_change_request_different_ext_id.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/deliverydate/after/existing_change_request_different_ext_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateDelvieryDateDuplicateDifferentExternalIds() throws Exception {
        performRequest(validRequestBuilder().build())
            .andExpect(status().isConflict())
            .andExpect(errorMessage(
                "Active Change Request with type = DELIVERY_DATE is already exists for order 1001"
            ));
    }

    @Test
    @DisplayName("Попытка создать еще одну заявку с externalId как у уже существующей")
    @DatabaseSetup(
        value = "/controller/order/deliverydate/after/existing_change_request_same_ext_id.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/deliverydate/after/existing_change_request_same_ext_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateDelvieryDateDuplicateSameExternalIds() throws Exception {
        performRequest(validRequestBuilder().build())
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskNotCreated(QueueType.CHANGE_ORDER_REQUEST);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validateRequestArguments")
    @DisplayName("Валидация запроса")
    @DatabaseSetup("/controller/order/deliverydate/before/setup.xml")
    void validateRequest(
        String displayName,
        UpdateOrderDeliveryDateRequestDto.UpdateOrderDeliveryDateRequestDtoBuilder request,
        ResultMatcher resultMatcher
    ) throws Exception {
        performRequest(request.build())
            .andExpect(status().isBadRequest())
            .andExpect(resultMatcher);
        assertOrderHistoryNeverChanged(1001L);
    }

    private static Stream<Arguments> validateRequestArguments() {
        return Stream.of(
            Arguments.of(
                "barcode заказа не указан",
                validRequestBuilder().barcode(null),
                fieldValidationErrorMatcher("barcode", NOT_NULL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "min date не указана",
                validRequestBuilder().dateMin(null),
                fieldValidationErrorMatcher("dateMin", NOT_NULL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "max date не указана",
                validRequestBuilder().dateMax(null),
                fieldValidationErrorMatcher("dateMax", NOT_NULL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "min date > max date",
                validRequestBuilder().dateMin(LocalDate.of(2021, 3, 11)),
                errorMessage("New delivery date max must be greater or equal to min")
            ),
            Arguments.of(
                "min date < today",
                validRequestBuilder().dateMin(LocalDate.of(2021, 3, 3)),
                errorMessage("New delivery date must be in the future or today")
            )
        );
    }

    @Nonnull
    private static UpdateOrderDeliveryDateRequestDto.UpdateOrderDeliveryDateRequestDtoBuilder validRequestBuilder() {
        return UpdateOrderDeliveryDateRequestDto.builder()
            .barcode("1001")
            .dateMin(LocalDate.of(2021, 3, 10))
            .dateMax(LocalDate.of(2021, 3, 10))
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(20, 0))
            .reason(ChangeOrderRequestReason.UNKNOWN)
            .changeRequestExternalId(123456L);
    }

    @Nonnull
    private ResultActions performRequest(UpdateOrderDeliveryDateRequestDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/updateDeliveryDate", request));
    }
}

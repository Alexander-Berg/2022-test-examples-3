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
import ru.yandex.market.logistics.lom.model.dto.UpdateLastMileRequestDto;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.utils.UpdateLastMileUtils;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static ru.yandex.market.logistics.lom.utils.TestUtils.NOT_NULL_ERROR_MESSAGE;
import static ru.yandex.market.logistics.lom.utils.TestUtils.fieldValidationErrorMatcher;
import static ru.yandex.market.logistics.lom.utils.TestUtils.objectValidationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Изменение последней мили")
@DatabaseSetup("/controller/order/lastmile/before/setup.xml")
@ParametersAreNonnullByDefault
public class OrderLastMileUpdateTest extends AbstractContextualTest {
    private static final ChangeOrderRequestPayload PAYLOAD = PayloadFactory.createChangeOrderRequestPayload(
        1,
        "1",
        1
    );

    private static final Instant FIXED_TIME = Instant.parse("2021-03-01T00:00:00.00Z");

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @DisplayName("Успешный запрос на обновление последней мили (адреса или/и даты/интервалов доставки)")
    @Test
    @ExpectedDatabase(
        value = "/controller/order/lastmile/after/change_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateLastMileSuccess() throws Exception {
        performRequest(
            UpdateLastMileUtils.validBuilder(UpdateLastMileUtils.validPayloadBuilder(), DeliveryType.COURIER).build()
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/lastmile/after/update_last_mile_response.json",
                "created",
                "updated"
            ));
        queueTaskChecker.assertQueueTaskCreated(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD);
    }

    @DisplayName("Успешный запрос на обновление последней мили с orderId и без checkouterId")
    @Test
    @ExpectedDatabase(
        value = "/controller/order/lastmile/after/change_request_created_with_order_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateLastMileSuccessWithOrderId() throws Exception {
        performRequest(
            UpdateLastMileUtils
                .validBuilder(
                    UpdateLastMileUtils.validPayloadBuilder()
                        .checkouterChangeRequestId(null),
                    DeliveryType.COURIER
                )
                .barcode(null)
                .orderId(1L)
                .build()
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/lastmile/after/update_last_mile_response_with_order_id.json",
                "created",
                "updated"
            ));
        queueTaskChecker.assertQueueTaskCreated(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD);
    }

    @DisplayName("Успешный запрос на обновление последней мили с пустым комментарием")
    @Test
    void testLastMileSuccessWithEmptyComment() throws Exception {
        performRequest(
            UpdateLastMileUtils.validBuilder(
                    UpdateLastMileUtils.validPayloadBuilder().comment(null),
                    DeliveryType.COURIER
                )
                .build()
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/lastmile/after/update_last_mile_response_without_comment.json",
                "created",
                "updated"
            ));
        queueTaskChecker.assertQueueTaskCreated(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD);
    }

    @Test
    @DisplayName("Успешный запрос на обновление последней мили с null маршрутом")
    void testLastMileSuccessWithRouteBeingNull() throws Exception {
        performRequest(
            UpdateLastMileUtils.validBuilder(
                    UpdateLastMileUtils.validPayloadBuilder(),
                    DeliveryType.COURIER
                )
                .route(null)
                .build()
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/lastmile/after/update_last_mile_response.json",
                "created",
                "updated"
            ));
        queueTaskChecker.assertQueueTaskCreated(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD);
    }

    @DisplayName("Повторный запрос на обновление последней мили")
    @Test
    @DatabaseSetup(
        value = "/controller/order/lastmile/after/change_request_created.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/lastmile/after/change_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateLastMileDuplicate() throws Exception {
        performRequest(
            UpdateLastMileUtils.validBuilder(UpdateLastMileUtils.validPayloadBuilder(), DeliveryType.COURIER).build()
        )
            .andExpect(status().isConflict())
            .andExpect(errorMessage(
                "Active Change Request with type = LAST_MILE is already exists for order 1001"
            ));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validateRequestArguments")
    @DisplayName("Обновление последней мили с невалидным телом запроса")
    void testUpdateLastMileWithInvalidRequestBody(
        @SuppressWarnings("unused") String displayName,
        UpdateLastMileRequestDto.UpdateLastMileRequestDtoBuilder requestBuilder,
        ResultMatcher resultMatcher
    ) throws Exception {
        performRequest(requestBuilder.build())
            .andExpect(status().isBadRequest())
            .andExpect(resultMatcher);
        assertOrderHistoryNeverChanged(1001L);
    }

    private static Stream<Arguments> validateRequestArguments() {
        return Stream.of(
            Arguments.of(
                "orderId и barcode не указаны",
                UpdateLastMileUtils.validBuilder(
                        UpdateLastMileUtils.validPayloadBuilder(),
                        DeliveryType.COURIER
                    )
                    .barcode(null),
                objectValidationErrorMatcher(
                    "updateLastMileRequestDto",
                    "at least one field must not be null: [orderId, barcode]"
                )
            ),
            Arguments.of(
                "тип доставки не указан",
                UpdateLastMileUtils.validBuilder(
                    UpdateLastMileUtils.validPayloadBuilder(),
                    null
                ),
                fieldValidationErrorMatcher("deliveryType", NOT_NULL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "адрес не указан",
                UpdateLastMileUtils.validBuilder(
                    UpdateLastMileUtils.validPayloadBuilder().address(null),
                    DeliveryType.COURIER
                ),
                fieldValidationErrorMatcher("payload.address", NOT_NULL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "max date не указана",
                UpdateLastMileUtils.validBuilder(
                    UpdateLastMileUtils.validPayloadBuilder().dateMax(null),
                    DeliveryType.COURIER
                ),
                fieldValidationErrorMatcher("payload.dateMax", NOT_NULL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "min date не указана",
                UpdateLastMileUtils.validBuilder(
                    UpdateLastMileUtils.validPayloadBuilder().dateMin(null),
                    DeliveryType.COURIER
                ),
                fieldValidationErrorMatcher("payload.dateMin", NOT_NULL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "min date < now",
                UpdateLastMileUtils.validBuilder(
                    UpdateLastMileUtils.validPayloadBuilder().dateMin(LocalDate.of(2021, 2, 1)),
                    DeliveryType.COURIER
                ),
                fieldValidationErrorMatcher("payload.dateMin", "delivery date must not be earlier than today")
            ),
            Arguments.of(
                "max date < min date",
                UpdateLastMileUtils.validBuilder(
                    UpdateLastMileUtils.validPayloadBuilder().dateMax(LocalDate.of(2021, 3, 9)),
                    DeliveryType.COURIER
                ),
                fieldValidationErrorMatcher(
                    "payload",
                    "delivery interval min date must be before or equal to max date"
                )
            ),
            Arguments.of(
                "end time < start time",
                UpdateLastMileUtils.validBuilder(
                    UpdateLastMileUtils.validPayloadBuilder().endTime(LocalTime.of(8, 0)),
                    DeliveryType.COURIER
                ),
                fieldValidationErrorMatcher(
                    "payload",
                    "from must be earlier than to"
                )
            )
        );
    }

    @Nonnull
    private ResultActions performRequest(UpdateLastMileRequestDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/updateLastMile", request));
    }
}

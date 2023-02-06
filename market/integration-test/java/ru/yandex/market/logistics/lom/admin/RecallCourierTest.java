package ru.yandex.market.logistics.lom.admin;

import java.time.Instant;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.validators.enums.ExpressRecallCourierValidationError;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_HEADERS;
import static ru.yandex.market.logistics.lom.utils.TestUtils.toHttpHeaders;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Перевызов курьера")
@DatabaseSetup("/controller/admin/order/before/orders_for_recall.xml")
public class RecallCourierTest extends AbstractContextualTest {

    private static final Instant VALID_TIME = Instant.parse("2022-04-20T04:40:00.00Z");

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        clock.setFixed(VALID_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Не перевызываем")
    @ExpectedDatabase(
        value = "/controller/admin/order/before/orders_for_recall.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void doNotRecall(
        @SuppressWarnings("unused") String name,
        long orderId,
        ExpressRecallCourierValidationError code
    ) throws Exception {
        recall(orderId).andExpect(status().isOk());

        softly.assertThat(backLogCaptor.getResults().stream().anyMatch(line -> line.contains(code.name()))).isTrue();
        softly.assertThat(courierRecalled(orderId)).isFalse();
    }

    @Nonnull
    private static Stream<Arguments> doNotRecall() {
        return Stream.of(
            Arguments.of(
                "Не перевызываем: Есть активная заявка на отмену",
                1L,
                ExpressRecallCourierValidationError.ORDER_HAS_CANCELLATION_REQUEST
            ),
            Arguments.of(
                "Не перевызываем: нет CALL_COURIER-сегмента",
                2L,
                ExpressRecallCourierValidationError.ORDER_NOT_EXPRESS
            ),
            Arguments.of(
                "Не перевызываем: нет 47чп и упавшего бп",
                4L,
                ExpressRecallCourierValidationError.DATE_NOT_CHANGED_BY_DELIVERY
            ),
            Arguments.of("Заказ не найден", 10000L, ExpressRecallCourierValidationError.ORDER_NOT_FOUND),
            Arguments.of("Заказ в широкий слот", 7L, ExpressRecallCourierValidationError.ORDER_NOT_EXPRESS)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Отменяем заказ: склад не работает")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/cancel_order_5.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void warehouseClosed(@SuppressWarnings("unused") String name, String date) throws Exception {
        clock.setFixed(Instant.parse(date), DateTimeUtils.MOSCOW_ZONE);
        recall(5L).andExpect(status().isOk());

        softly.assertThat(backLogCaptor.getResults().stream().anyMatch(line -> line.contains(
                "level=INFO\t"
                    + "format=plain\t"
                    + "code=WAREHOUSE_CLOSED\t"
                    + "payload=Courier for order 5 was not recalled: WAREHOUSE_CLOSED\t"
                    + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t"
                    + "tags=COURIER_NOT_RECALLED\t"
                    + "entity_types=order,lom_order\t"
                    + "entity_values=order:100500,lom_order:5"
            )))
            .isTrue();

        softly.assertThat(backLogCaptor.getResults().stream().anyMatch(line -> line.contains(
                "level=INFO\t"
                    + "format=plain\t"
                    + "code=CREATE_ORDER_CANCELLATION_REQUEST\t"
                    + "payload=New order cancellation request\t"
                    + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t"
                    + "entity_types=order,lom_order,requestId\t"
                    + "entity_values=order:100500,lom_order:5,requestId:1\t"
                    + "extra_keys=reason\t"
                    + "extra_values=COURIER_NOT_FOUND"
            )))
            .isTrue();
    }

    @Nonnull
    private static Stream<Arguments> warehouseClosed() {
        return Stream.of(
            Arguments.of("Не совпадает день работы склада", "2022-04-21T13:20:00.00Z"),
            Arguments.of("До начала работы склада", "2022-04-20T04:20:00.00Z"),
            Arguments.of("После начала работы склада", "2022-04-20T08:10:00.00Z"),
            Arguments.of("Меньше, чем за необходимый оффсет до конца работы склада", "2022-04-20T06:40:00.00Z")
        );
    }

    @Test
    @DisplayName("Перевызываем")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/recall.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() throws Exception {
        recall(5L).andExpect(status().isOk());
        softly.assertThat(courierRecalled(5)).isTrue();
    }

    @Test
    @DisplayName("Перевызываем - кргулосуточный склад")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/recall-24-hours.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successWith24HourWarehouse() throws Exception {
        clock.setFixed(Instant.parse("2022-04-20T17:59:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
        recall(6L).andExpect(status().isOk());
        softly.assertThat(courierRecalled(6)).isTrue();
    }

    @Test
    @DisplayName("Перевызываем - есть валидный бп")
    @DatabaseSetup(value = "/controller/admin/order/before/call_courier_error_bp.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/controller/admin/order/after/recall_4.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successWithBusinessProcess() throws Exception {
        recall(4L).andExpect(status().isOk());
        softly.assertThat(courierRecalled(4)).isTrue();
    }

    @Test
    @DisplayName("Не перевызываем - есть бп, но не в статусе QUEUE_TASK_ERROR")
    @DatabaseSetup(
        value = "/controller/admin/order/before/call_courier_enqueued_bp.xml",
        type = DatabaseOperation.INSERT
    )
    void failWithBusinessProcessInWrongStatus() throws Exception {
        recall(4L).andExpect(status().isOk());
        softly.assertThat(courierRecalled(4)).isFalse();
    }

    @Test
    @DisplayName("Не перевызываем - есть бп, но не CALL_COURIER")
    @DatabaseSetup(
        value = "/controller/admin/order/before/not_call_courier_error_bp.xml",
        type = DatabaseOperation.INSERT
    )
    void failWithBusinessProcessWithWrongQueueType() throws Exception {
        recall(4L).andExpect(status().isOk());
        softly.assertThat(courierRecalled(4)).isFalse();
    }

    @Test
    @DisplayName("Не перевызываем - есть бп, но для другого сегмента")
    @DatabaseSetup(
        value = "/controller/admin/order/before/call_courier_error_bp_wrong_segment.xml",
        type = DatabaseOperation.INSERT
    )
    void failWithBusinessProcessForAnotherSegment() throws Exception {
        recall(4L).andExpect(status().isOk());
        softly.assertThat(courierRecalled(4)).isFalse();
    }

    private boolean courierRecalled(long orderId) {
        return backLogCaptor.getResults().stream().anyMatch(line -> line.contains(
            "level=INFO\t"
                + "format=plain\t"
                + "code=COURIER_RECALLED\t"
                + "payload=Courier recalled\t"
                + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t"
                + "tags=COURIER_RECALLED\t"
                + "entity_types=order,lom_order\t"
                + "entity_values=order:100500,lom_order:" + orderId
        ));
    }

    @Nonnull
    private ResultActions recall(long id) throws Exception {
        return mockMvc.perform(
            post("/admin/orders/recall-courier")
                .headers(toHttpHeaders(USER_HEADERS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"id\":%d}", id))
        );
    }
}

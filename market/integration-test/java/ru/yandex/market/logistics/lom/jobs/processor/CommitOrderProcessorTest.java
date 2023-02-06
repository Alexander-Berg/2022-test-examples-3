package ru.yandex.market.logistics.lom.jobs.processor;

import java.time.Instant;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.controller.order.OrderHistoryTestUtil;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdAuthorPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class CommitOrderProcessorTest extends AbstractContextualTest {
    @Autowired
    private CommitOrderProcessor commitOrderProcessor;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2019-06-12T00:00:00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Успешно оформить заказ")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOrderSuccess() {
        commitOrderProcessor.processPayload(getPayload());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, null);
    }

    @Test
    @DisplayName("Ошибка оформления заказа")
    @DatabaseSetup("/controller/commit/before/commit_order_fail.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_fail.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOrderFail() {
        commitOrderProcessor.processPayload(getPayload());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, null);
    }

    @Test
    @DisplayName("Ошибка оформления заказа - заказ был отменен")
    @DatabaseSetup("/controller/commit/before/commit_order_fail.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = {
            "/service/common/before/waybill_segment_storage_unit.xml",
            "/controller/commit/before/cancel_order.xml"
        },
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_fail_cancelled.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOrderFailCancelled() {
        softly.assertThat(commitOrderProcessor.processPayload(getPayload()))
            .isEqualTo(ProcessingResult.unprocessed("Заказ уже не черновик, а в статусе CANCELLED"));
    }

    @Nonnull
    private OrderIdAuthorPayload getPayload() {
        OrderIdAuthorPayload payload = new OrderIdAuthorPayload("1", ORDER_ID, null);
        payload.setSequenceId(1L);
        return payload;
    }
}

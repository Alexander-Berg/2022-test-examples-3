package ru.yandex.market.logistics.lom.service.tracker.listener.implementation;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.lom.dto.queue.LomSegmentCheckpoint;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;
import ru.yandex.market.logistics.lom.repository.OrderRepository;
import ru.yandex.market.logistics.lom.utils.WaybillUtils;

@DisplayName("Тест на обработку 37, 47 и 90 чекпоинтов в сегментах Яндекс.Go заказов на часовые слоты")
class DeferredCourierOrderCheckpointListenerTest extends AbstractCheckpointListenerTest {

    @Autowired
    private DeferredCourierOrderCheckpointListener listener;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DatabaseSetup("/service/listener/deferredCourierOrder/before/setup.xml")
    @ExpectedDatabase(
        value = "/service/listener/deferredCourierOrder/after/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Подходящий чекпоинт обработан корректно - создан change order request для перевода заказа в ondemand")
    void testProcess() {
        transactionTemplate.execute(arg -> {
            Order order = orderRepository.getById(1L);
            WaybillSegment segment = WaybillUtils.getSegmentById(order, 3L);
            LomSegmentCheckpoint checkpoint =
                createCheckpoint(SegmentStatus.TRANSIT_UPDATED_BY_RECIPIENT);

            listener.process(order, checkpoint, segment, null);

            return null;
        });
    }

    @Test
    @DatabaseSetup("/service/listener/deferredCourierOrder/before/setup.xml")
    @ExpectedDatabase(
        value = "/service/listener/deferredCourierOrder/after/expected_call_courier_by_user.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("При обработке 37го чп создан change order request с причиной CALL_COURIER_BY_USER")
    void processingOf37CheckpointLeadsToCallCourierByUserReason() {
        transactionTemplate.execute(arg -> {
            Order order = orderRepository.getById(1L);
            WaybillSegment segment = WaybillUtils.getSegmentById(order, 3L);
            LomSegmentCheckpoint checkpoint =
                createCheckpoint(SegmentStatus.TRANSIT_ON_DEMAND_DELIVERY_REQUESTED);

            listener.process(order, checkpoint, segment, null);

            return null;
        });
    }

    @ParameterizedTest
    @MethodSource("newCheckpointArguments")
    @DisplayName("Проверка применимости процессора для обработки сегмента")
    void testNeedProcessing(
        @SuppressWarnings("unused") String displayName,
        @Nullable SegmentType segmentType,
        @Nonnull SegmentStatus segmentStatus,
        boolean hasTag,
        boolean expected
    ) {
        WaybillSegment segment = createWaybillSegment(segmentType, hasTag);
        Order order = new Order().setWaybill(List.of(segment));
        LomSegmentCheckpoint checkpoint = createCheckpoint(segmentStatus);

        softly.assertThat(listener.needProcessing(order, checkpoint, segment))
            .as("Asserting that the listener is " + (expected ? "" : "NOT ") + "eligible for processing the checkpoint")
            .isEqualTo(expected);
    }

    @Nonnull
    private WaybillSegment createWaybillSegment(@Nullable SegmentType segmentType, boolean hasTag) {
        WaybillSegment waybillSegment = new WaybillSegment()
            .setSegmentType(segmentType);

        return hasTag ? waybillSegment.addTag(WaybillSegmentTag.DEFERRED_COURIER) : waybillSegment;
    }

    @SuppressWarnings({"unused", "checkstyle:MethodLength"})
    @Nonnull
    private static Stream<Arguments> newCheckpointArguments() {
        return Stream.of(
            Arguments.of(
                "GO_PLATFORM, подходящий чекпоинт 37",
                SegmentType.GO_PLATFORM,
                SegmentStatus.TRANSIT_ON_DEMAND_DELIVERY_REQUESTED,
                true,
                true
            ),
            Arguments.of(
                "GO_PLATFORM, подходящий чекпоинт 47",
                SegmentType.GO_PLATFORM,
                SegmentStatus.TRANSIT_UPDATED_BY_DELIVERY,
                true,
                true
            ),
            Arguments.of(
                "GO_PLATFORM, подходящий чекпоинт 90",
                SegmentType.GO_PLATFORM,
                SegmentStatus.TRANSIT_DELIVERY_ATTEMPT_FAILED,
                true,
                true
            ),
            Arguments.of(
                "GO_PLATFORM, неподходящий чекпоинт 46",
                SegmentType.GO_PLATFORM,
                SegmentStatus.TRANSIT_UPDATED_BY_RECIPIENT,
                true,
                false
            ),
            Arguments.of(
                "GO_PLATFORM, неподходящий чекпоинт 48",
                SegmentType.GO_PLATFORM,
                SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT,
                true,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 37 - другой тип сегмента (PICKUP)",
                SegmentType.PICKUP,
                SegmentStatus.TRANSIT_ON_DEMAND_DELIVERY_REQUESTED,
                true,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 47 - другой тип сегмента (PICKUP)",
                SegmentType.PICKUP,
                SegmentStatus.TRANSIT_UPDATED_BY_DELIVERY,
                true,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 90 - другой тип сегмента (PICKUP)",
                SegmentType.PICKUP,
                SegmentStatus.TRANSIT_DELIVERY_ATTEMPT_FAILED,
                true,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 46 - другой тип сегмента (PICKUP)",
                SegmentType.PICKUP,
                SegmentStatus.TRANSIT_UPDATED_BY_RECIPIENT,
                true,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 48 - другой тип сегмента (PICKUP)",
                SegmentType.PICKUP,
                SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT,
                true,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 37 - другой тип сегмента (COURIER)",
                SegmentType.COURIER,
                SegmentStatus.TRANSIT_ON_DEMAND_DELIVERY_REQUESTED,
                true,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 47 - другой тип сегмента (COURIER)",
                SegmentType.COURIER,
                SegmentStatus.TRANSIT_UPDATED_BY_DELIVERY,
                true,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 90 - другой тип сегмента (COURIER)",
                SegmentType.COURIER,
                SegmentStatus.TRANSIT_DELIVERY_ATTEMPT_FAILED,
                true,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 46 - другой тип сегмента (COURIER)",
                SegmentType.COURIER,
                SegmentStatus.TRANSIT_UPDATED_BY_RECIPIENT,
                true,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 48 - другой тип сегмента (COURIER)",
                SegmentType.COURIER,
                SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT,
                true,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 37 - тип сегмента null",
                null,
                SegmentStatus.TRANSIT_ON_DEMAND_DELIVERY_REQUESTED,
                true,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 47 - тип сегмента null",
                null,
                SegmentStatus.TRANSIT_UPDATED_BY_DELIVERY,
                true,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 90 - тип сегмента null",
                null,
                SegmentStatus.TRANSIT_DELIVERY_ATTEMPT_FAILED,
                true,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 46 - тип сегмента null",
                null,
                SegmentStatus.TRANSIT_UPDATED_BY_RECIPIENT,
                true,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 48 - тип сегмента null",
                null,
                SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT,
                true,
                false
            ),
            Arguments.of(
                "COURIER, чекпоинт 37 - нет тега",
                SegmentType.COURIER,
                SegmentStatus.TRANSIT_ON_DEMAND_DELIVERY_REQUESTED,
                false,
                false
            ),
            Arguments.of(
                "COURIER, чекпоинт 47 - нет тега",
                SegmentType.COURIER,
                SegmentStatus.TRANSIT_UPDATED_BY_DELIVERY,
                false,
                false
            ),
            Arguments.of(
                "COURIER, чекпоинт 90 - нет тега",
                SegmentType.COURIER,
                SegmentStatus.TRANSIT_DELIVERY_ATTEMPT_FAILED,
                false,
                false
            ),
            Arguments.of(
                "COURIER, неподходящий чекпоинт 46 - нет тега",
                SegmentType.COURIER,
                SegmentStatus.TRANSIT_UPDATED_BY_RECIPIENT,
                false,
                false
            ),
            Arguments.of(
                "COURIER, неподходящий чекпоинт 48 - нет тега",
                SegmentType.COURIER,
                SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT,
                false,
                false
            ),
            Arguments.of(
                "GO_PLATFORM, чекпоинт 37 - нет тега",
                SegmentType.GO_PLATFORM,
                SegmentStatus.TRANSIT_ON_DEMAND_DELIVERY_REQUESTED,
                false,
                false
            ),
            Arguments.of(
                "GO_PLATFORM, чекпоинт 47 - нет тега",
                SegmentType.GO_PLATFORM,
                SegmentStatus.TRANSIT_UPDATED_BY_DELIVERY,
                false,
                false
            ),
            Arguments.of(
                "GO_PLATFORM, чекпоинт 90 - нет тега",
                SegmentType.GO_PLATFORM,
                SegmentStatus.TRANSIT_DELIVERY_ATTEMPT_FAILED,
                false,
                false
            ),
            Arguments.of(
                "GO_PLATFORM, неподходящий чекпоинт 46 - нет тега",
                SegmentType.GO_PLATFORM,
                SegmentStatus.TRANSIT_UPDATED_BY_RECIPIENT,
                false,
                false
            ),
            Arguments.of(
                "GO_PLATFORM, неподходящий чекпоинт 48 - нет тега",
                SegmentType.GO_PLATFORM,
                SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT,
                false,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 37 - нет тега и другой тип сегмента (PICKUP)",
                SegmentType.PICKUP,
                SegmentStatus.TRANSIT_ON_DEMAND_DELIVERY_REQUESTED,
                false,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 47 - нет тега и другой тип сегмента (PICKUP)",
                SegmentType.PICKUP,
                SegmentStatus.TRANSIT_UPDATED_BY_DELIVERY,
                false,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 90 - нет тега и другой тип сегмента (PICKUP)",
                SegmentType.PICKUP,
                SegmentStatus.TRANSIT_DELIVERY_ATTEMPT_FAILED,
                false,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 46 - нет тега и другой тип сегмента (PICKUP)",
                SegmentType.PICKUP,
                SegmentStatus.TRANSIT_UPDATED_BY_RECIPIENT,
                false,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 48 - нет тега и другой тип сегмента (PICKUP)",
                SegmentType.PICKUP,
                SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT,
                false,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 37 - нет тега и другой тип сегмента (COURIER)",
                SegmentType.PICKUP,
                SegmentStatus.TRANSIT_ON_DEMAND_DELIVERY_REQUESTED,
                false,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 47 - нет тега и другой тип сегмента (COURIER)",
                SegmentType.PICKUP,
                SegmentStatus.TRANSIT_UPDATED_BY_DELIVERY,
                false,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 90 - нет тега и другой тип сегмента (COURIER)",
                SegmentType.PICKUP,
                SegmentStatus.TRANSIT_DELIVERY_ATTEMPT_FAILED,
                false,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 46 - нет тега и другой тип сегмента (COURIER)",
                SegmentType.COURIER,
                SegmentStatus.TRANSIT_UPDATED_BY_RECIPIENT,
                false,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 48 - нет тега и другой тип сегмента (COURIER)",
                SegmentType.PICKUP,
                SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT,
                false,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 37 - нет тега и тип сегмента null",
                null,
                SegmentStatus.TRANSIT_ON_DEMAND_DELIVERY_REQUESTED,
                false,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 47 - нет тега и тип сегмента null",
                null,
                SegmentStatus.TRANSIT_UPDATED_BY_DELIVERY,
                false,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 90 - нет тега и тип сегмента null",
                null,
                SegmentStatus.TRANSIT_DELIVERY_ATTEMPT_FAILED,
                false,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 46 - нет тега и тип сегмента null",
                null,
                SegmentStatus.TRANSIT_UPDATED_BY_RECIPIENT,
                false,
                false
            ),
            Arguments.of(
                "Неподходящий чекпоинт 48 - нет тега и тип сегмента null",
                null,
                SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT,
                false,
                false
            )
        );
    }
}

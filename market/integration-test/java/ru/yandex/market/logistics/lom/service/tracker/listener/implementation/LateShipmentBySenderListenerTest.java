package ru.yandex.market.logistics.lom.service.tracker.listener.implementation;

import java.time.Instant;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
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
import ru.yandex.market.logistics.lom.repository.OrderRepository;
import ru.yandex.market.logistics.lom.utils.WaybillUtils;

@DisplayName("Обработка приемки на дропоффе C2C заказа")
@DatabaseSetup("/service/listener/c2cDropoffInbound/before/setup.xml")
class LateShipmentBySenderListenerTest extends AbstractCheckpointListenerTest {

    @Autowired
    private LateShipmentBySenderListener listener;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Приходит 110 чекпоинт и создается change order request на изменение даты доставки")
    @ExpectedDatabase(
        value = "/service/listener/c2cDropoffInbound/after/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testProcess() {
        transactionTemplate.execute(st -> {
            Order order = orderRepository.getById(1L);
            WaybillSegment segment = WaybillUtils.getSegmentById(order, 11L);
            LomSegmentCheckpoint checkpoint = createCheckpoint(
                SegmentStatus.IN,
                Instant.parse("2022-06-24T20:00:00Z")
            );

            listener.process(order, checkpoint, segment, null);
            return null;
        });
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Фильтрация чекпоинтов, все заказы платформы YANDEX_GO")
    @DatabaseSetup(
        value = "/service/listener/c2cDropoffInbound/before/enable_rdd_flag.xml",
        type = DatabaseOperation.INSERT
    )
    void needToProcessYandexGo(
        @SuppressWarnings("unused") String caseName,
        long orderId,
        long segmentId,
        SegmentStatus statusInCheckpoint,
        Instant checkpointDate,
        boolean expected
    ) {
        transactionTemplate.execute(st -> {
            Order order = orderRepository.getById(orderId);
            WaybillSegment segment = WaybillUtils.getSegmentById(order, segmentId);
            LomSegmentCheckpoint checkpoint = createCheckpoint(
                statusInCheckpoint,
                checkpointDate
            );

            softly.assertThat(listener.needProcessing(order, checkpoint, segment))
                .isEqualTo(expected);

            return null;
        });
    }

    @Nonnull
    @SuppressWarnings("unused")
    private static Stream<Arguments> needToProcessYandexGo() {
        return Stream.of(
            Arguments.of(
                "IN на дропоффе",
                1L,
                11L,
                SegmentStatus.IN,
                Instant.parse("2022-06-24T20:00:00Z"),
                true
            ),
            Arguments.of(
                "IN на первом СЦ (нет сегмента YANDEX_GO_SHOP)",
                2L,
                21L,
                SegmentStatus.IN,
                Instant.parse("2022-06-24T20:00:00Z"),
                true
            ),
            Arguments.of(
                "IN на первом СЦ после сегмента YANDEX_GO_SHOP",
                3L,
                32L,
                SegmentStatus.IN,
                Instant.parse("2022-06-24T20:00:00Z"),
                true
            ),
            Arguments.of(
                "IN на дропоффе, не указан сдвиг таймзоны в сегменте",
                6L,
                61L,
                SegmentStatus.IN,
                Instant.parse("2022-06-25T00:00:00Z"),
                true
            ),
            Arguments.of(
                "Заказ не платформы YANDEX_GO",
                4L,
                41L,
                SegmentStatus.IN,
                Instant.parse("2022-06-24T20:00:00Z"),
                false
            ),
            Arguments.of(
                "Статус в чекпоинте не IN",
                1L,
                11L,
                SegmentStatus.OUT,
                Instant.parse("2022-06-24T20:00:00Z"),
                false
            ),
            Arguments.of(
                "Нет опоздания отгрузки",
                1L,
                11L,
                SegmentStatus.IN,
                Instant.parse("2022-06-24T19:59:59Z"),
                false
            ),
            Arguments.of(
                "IN на первом сегменте, не СЦ и не дропофф",
                5L,
                51L,
                SegmentStatus.IN,
                Instant.parse("2022-06-24T20:00:00Z"),
                false
            ),
            Arguments.of(
                "IN на втором СЦ",
                2L,
                23L,
                SegmentStatus.IN,
                Instant.parse("2022-06-24T20:00:00Z"),
                false
            ),
            Arguments.of(
                "IN на дропоффе, не указана ожидаемая дата отгрузки на сегменте",
                7L,
                71L,
                SegmentStatus.IN,
                Instant.parse("2022-06-24T20:00:00Z"),
                false
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Фильтрация чекпоинтов, только С2С заказы")
    @DatabaseSetup(
        value = "/service/listener/c2cDropoffInbound/before/c2c_order_need_to_process.xml",
        type = DatabaseOperation.INSERT
    )
    void needToProcessC2COnly(
        @SuppressWarnings("unused") String caseName,
        long orderId,
        long segmentId,
        SegmentStatus statusInCheckpoint,
        boolean expected
    ) {
        transactionTemplate.execute(st -> {
            Order order = orderRepository.getById(orderId);
            WaybillSegment segment = WaybillUtils.getSegmentById(order, segmentId);
            LomSegmentCheckpoint checkpoint = createCheckpoint(
                statusInCheckpoint,
                Instant.parse("2022-06-24T20:00:00Z")
            );

            softly.assertThat(listener.needProcessing(order, checkpoint, segment))
                .isEqualTo(expected);

            return null;
        });
    }

    @Nonnull
    @SuppressWarnings("unused")
    private static Stream<Arguments> needToProcessC2COnly() {
        return Stream.of(
            Arguments.of(
                "IN на дропоффе",
                1L,
                11L,
                SegmentStatus.IN,
                true
            ),
            Arguments.of(
                "IN на первом СЦ (не дропофф)",
                2L,
                21L,
                SegmentStatus.IN,
                false
            ),
            Arguments.of(
                "Статус в чекпоинте не IN",
                1L,
                11L,
                SegmentStatus.OUT,
                false
            ),
            Arguments.of(
                "IN на дропоффе, не C2C заказ",
                6L,
                61L,
                SegmentStatus.IN,
                false
            )
        );
    }
}

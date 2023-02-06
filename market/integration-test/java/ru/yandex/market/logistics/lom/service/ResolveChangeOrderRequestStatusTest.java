package ru.yandex.market.logistics.lom.service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.ChangeOrderRequest;
import ru.yandex.market.logistics.lom.entity.ChangeOrderSegmentRequest;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderSegmentRequestStatus;
import ru.yandex.market.logistics.lom.jobs.processor.AbstractUpdateChangeOrderRequestProcessor;
import ru.yandex.market.logistics.lom.jobs.producer.NotifyOrderErrorToMqmProducer;
import ru.yandex.market.logistics.lom.service.order.ChangeOrderRequestService;
import ru.yandex.market.logistics.lom.service.order.OrderService;
import ru.yandex.market.logistics.lom.service.order.UpdateOrderItemsStatsService;

@ParametersAreNonnullByDefault
@DisplayName("Вычисление статуса заявки на изменение заказа по статусам заявок на изменение сегмента заказа")
@DatabaseSetup("/controller/order/resolveChangeRequestStatus/prepare_data.xml")
class ResolveChangeOrderRequestStatusTest extends AbstractContextualTest {

    private static final Map<ChangeOrderRequestStatus, Supplier<Stream<Arguments>>> STATUSES_SUPPLIERS =
        Map.of(
            ChangeOrderRequestStatus.SUCCESS, ResolveChangeOrderRequestStatusTest::success,
            ChangeOrderRequestStatus.FAIL, ResolveChangeOrderRequestStatusTest::fail,
            ChangeOrderRequestStatus.REQUIRED_SEGMENT_FAIL, ResolveChangeOrderRequestStatusTest::requiredFail,
            ChangeOrderRequestStatus.REQUIRED_SEGMENT_SUCCESS, ResolveChangeOrderRequestStatusTest::requiredSuccess
        );

    private static final Set<ChangeOrderRequestStatus> IMPOSSIBLE_STATUSES = EnumSet.of(
        ChangeOrderRequestStatus.PARTNER_FAIL,
        ChangeOrderRequestStatus.TECH_FAIL
    );

    @Autowired
    private ChangeOrderRequestService changeOrderRequestService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private UpdateOrderItemsStatsService updateOrderItemsStatsService;
    @Autowired
    private NotifyOrderErrorToMqmProducer notifyOrderErrorToMqmProducer;

    private TestingProcessor processor;

    @BeforeEach
    void setup() {
        processor = new TestingProcessor(
            changeOrderRequestService,
            orderService,
            updateOrderItemsStatsService,
            notifyOrderErrorToMqmProducer
        );
    }

    @MethodSource
    @DisplayName("Вычисление статуса")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void resolveStatus(
        @SuppressWarnings("unused") String name,
        Set<ChangeOrderSegmentRequest> requests,
        ChangeOrderRequestStatus status
    ) {
        checkStatus(changeOrderRequest(requests), status);
    }

    @Nonnull
    private static Stream<Arguments> resolveStatus() {
        return StreamEx.of(requiredFail())
            .append(requiredSuccess())
            .append(success())
            .append(fail())
            .append(nullStatus());
    }

    @Nonnull
    private static Stream<Arguments> requiredFail() {
        return Stream.of(
            Pair.of(
                "Один обязательный сегмент в статусе FAIL, два обычных в статусе SUCCESS",
                Set.of(
                    changeOrderSegmentRequest().setRequired(true).setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest(),
                    changeOrderSegmentRequest()
                )
            ),
            Pair.of(
                "Два обязательных сегмента - один в статусе FAIL, два обычных в статусе SUCCESS",
                Set.of(
                    changeOrderSegmentRequest().setRequired(true).setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest().setRequired(true),
                    changeOrderSegmentRequest(),
                    changeOrderSegmentRequest()
                )
            ),
            Pair.of(
                "Два обязательных сегмента в статусе FAIL, два обычных в статусе SUCCESS",
                Set.of(
                    changeOrderSegmentRequest().setRequired(true).setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest().setRequired(true).setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest(),
                    changeOrderSegmentRequest()
                )
            ),
            Pair.of(
                "Обязательный сегмент в статусе FAIL, достаточный в SUCCESS, обычные в SUCCESS",
                Set.of(
                    changeOrderSegmentRequest().setRequired(true).setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest().setSufficient(Boolean.TRUE),
                    changeOrderSegmentRequest(),
                    changeOrderSegmentRequest()
                )
            )
        )
            .map(p -> Arguments.of(
                "REQUIRED_SEGMENT_FAIL:" + p.getLeft(),
                p.getRight(),
                ChangeOrderRequestStatus.REQUIRED_SEGMENT_FAIL
            ));
    }

    /**
     * Невалидные случаи {@link AbstractUpdateChangeOrderRequestProcessor#resolveStatus}.
     *
     * @see AbstractUpdateChangeOrderRequestProcessor
     */
    @Nonnull
    private static Stream<Arguments> nullStatus() {
        return Stream.of(
            Pair.of(
                "Один достаточный в FAIL, обычные в SUCCESS",
                Set.of(
                    changeOrderSegmentRequest().setSufficient(Boolean.TRUE)
                        .setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest(),
                    changeOrderSegmentRequest()
                )
            ), // невалидный случай, см. комментарий к AbstractUpdateChangeOrderRequestProcessor::resolveStatus
            Pair.of(
                "Обязательный сегмент в статусе SUCCESS, достаточный в FAIL, обычные в SUCCESS",
                Set.of(
                    changeOrderSegmentRequest().setRequired(true),
                    changeOrderSegmentRequest().setSufficient(Boolean.TRUE)
                        .setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest(),
                    changeOrderSegmentRequest()
                ) // невалидный случай, см. комментарий к AbstractUpdateChangeOrderRequestProcessor::resolveStatus
            )
        )
            .map(p -> Arguments.of("null: " + p.getLeft(), p.getRight(), null));
    }

    @Nonnull
    private static Stream<Arguments> requiredSuccess() {
        return Stream.of(
            Pair.of(
                "Один необходимый сегмент в статусе SUCCESS, обычные сегменты в статусе FAIL",
                Set.of(
                    changeOrderSegmentRequest().setRequired(true).setStatus(ChangeOrderSegmentRequestStatus.SUCCESS),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL)
                )
            ),
            Pair.of(
                "Один достаточный сегмент в статусе SUCCESS, обычные сегменты в статусе FAIL",
                Set.of(
                    changeOrderSegmentRequest()
                        .setSufficient(Boolean.TRUE)
                        .setStatus(ChangeOrderSegmentRequestStatus.SUCCESS),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL)
                )
            ),
            Pair.of(
                "Два необходимых сегмента в успешном статусе, обычные сегменты в статусе FAIL",
                Set.of(
                    changeOrderSegmentRequest().setRequired(true).setStatus(ChangeOrderSegmentRequestStatus.SUCCESS),
                    changeOrderSegmentRequest()
                        .setRequired(true)
                        .setStatus(ChangeOrderSegmentRequestStatus.SEGMENT_NOT_STARTED),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL)
                )
            ),
            Pair.of(
                "Два достаточных сегмента в успешном статусе, обычные сегменты в статусе FAIL",
                Set.of(
                    changeOrderSegmentRequest()
                        .setSufficient(Boolean.TRUE)
                        .setStatus(ChangeOrderSegmentRequestStatus.SUCCESS),
                    changeOrderSegmentRequest()
                        .setSufficient(Boolean.TRUE)
                        .setStatus(ChangeOrderSegmentRequestStatus.SEGMENT_NOT_STARTED),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL)
                )
            ),
            Pair.of(
                "Два достаточных сегмента в успешном статусе и обязательный, обычные сегменты в статусе FAIL",
                Set.of(
                    changeOrderSegmentRequest()
                        .setSufficient(Boolean.TRUE)
                        .setStatus(ChangeOrderSegmentRequestStatus.SUCCESS),
                    changeOrderSegmentRequest()
                        .setSufficient(Boolean.TRUE)
                        .setStatus(ChangeOrderSegmentRequestStatus.SEGMENT_NOT_STARTED),
                    changeOrderSegmentRequest().setRequired(true)
                        .setStatus(ChangeOrderSegmentRequestStatus.SEGMENT_NOT_STARTED),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL)
                )
            ),
            Pair.of(
                "Два обязательных сегмента в успешном статусе и достаточный, обычные сегменты в статусе FAIL",
                Set.of(
                    changeOrderSegmentRequest().setRequired(true).setStatus(ChangeOrderSegmentRequestStatus.SUCCESS),
                    changeOrderSegmentRequest()
                        .setRequired(true)
                        .setStatus(ChangeOrderSegmentRequestStatus.SEGMENT_NOT_STARTED),
                    changeOrderSegmentRequest()
                        .setSufficient(Boolean.TRUE)
                        .setStatus(ChangeOrderSegmentRequestStatus.SEGMENT_NOT_STARTED),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL)
                )
            ),
            Pair.of(
                "Два достаточных сегмента - один в успешном статусе, обычные сегменты в статусе FAIL",
                Set.of(
                    changeOrderSegmentRequest().setSufficient(Boolean.TRUE)
                        .setStatus(ChangeOrderSegmentRequestStatus.SEGMENT_NOT_STARTED),
                    changeOrderSegmentRequest()
                        .setSufficient(Boolean.TRUE)
                        .setStatus(ChangeOrderSegmentRequestStatus.FAIL)
                )
            ),
            Pair.of(
                "Два достаточных, один в FAIL, обычные в SUCCESS",
                Set.of(
                    changeOrderSegmentRequest().setSufficient(Boolean.TRUE)
                        .setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest().setSufficient(Boolean.TRUE),
                    changeOrderSegmentRequest(),
                    changeOrderSegmentRequest()
                )
            )
        )
            .map(p -> Arguments.of(
                "REQUIRED_SEGMENT_SUCCESS: " + p.getLeft(),
                p.getRight(),
                ChangeOrderRequestStatus.REQUIRED_SEGMENT_SUCCESS
            ));

    }

    @Nonnull
    private static Stream<Arguments> fail() {
        return Stream.of(
            Pair.of(
                "Все заявки FAIL, все сегменты не required и не sufficient",
                repeat(changeOrderSegmentRequest().setSufficient(Boolean.FALSE)
                    .setStatus(ChangeOrderSegmentRequestStatus.FAIL))
            ),
            Pair.of(
                "Все заявки FAIL, все сегменты required и sufficient = null",
                repeat(changeOrderSegmentRequest().setRequired(true).setStatus(ChangeOrderSegmentRequestStatus.FAIL))
            ),
            Pair.of(
                "Все заявки FAIL, все сегменты required и не sufficient",
                repeat(changeOrderSegmentRequest(true, false, ChangeOrderSegmentRequestStatus.FAIL))
            ),
            Pair.of(
                "Все заявки FAIL, все сегменты не required и sufficient",
                repeat(changeOrderSegmentRequest().setSufficient(Boolean.TRUE)
                    .setStatus(ChangeOrderSegmentRequestStatus.FAIL))
            ),
            Pair.of(
                "Все заявки FAIL, все сегменты не required и sufficient = null",
                repeat(changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL))
            ),
            Pair.of(
                "Все заявки FAIL, есть один обязательный сегмент",
                Set.of(
                    changeOrderSegmentRequest().setRequired(true).setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL)
                )
            ),
            Pair.of(
                "Все заявки FAIL, есть один обязательный и один достаточный сегмент",
                Set.of(
                    changeOrderSegmentRequest().setRequired(true).setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest()
                        .setSufficient(Boolean.TRUE)
                        .setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL)
                )
            ),
            Pair.of(
                "Все заявки FAIL, есть один достаточный сегмент",
                Set.of(
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest()
                        .setSufficient(Boolean.TRUE)
                        .setStatus(ChangeOrderSegmentRequestStatus.FAIL),
                    changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.FAIL)
                )
            ),
            Pair.of(
                "Один достаточный сегмент в статусе FAIL",
                Set.of(changeOrderSegmentRequest().setSufficient(Boolean.TRUE)
                    .setStatus(ChangeOrderSegmentRequestStatus.FAIL))
            ),
            Pair.of(
                "Один необходимый сегмент в статусе FAIL",
                Set.of(changeOrderSegmentRequest().setRequired(true).setStatus(ChangeOrderSegmentRequestStatus.FAIL))
            )
        )
            .map(p -> Arguments.of("FAIL: " + p.getLeft(), p.getRight(), ChangeOrderRequestStatus.FAIL));
    }

    @Nonnull
    private static Stream<Arguments> success() {
        return Stream.of(
            //required = false sufficient = false
            Pair.of(
                "Все заявки SUCCESS, все сегменты не required и не sufficient",
                repeat(changeOrderSegmentRequest().setSufficient(Boolean.FALSE))
            ),
            Pair.of(
                "Все заявки SEGMENT_NOT_STARTED, все сегменты не required и не sufficient",
                repeat(
                    changeOrderSegmentRequest().setSufficient(Boolean.FALSE)
                        .setStatus(ChangeOrderSegmentRequestStatus.SEGMENT_NOT_STARTED)
                )
            ),
            //required = true sufficient = null
            Pair.of(
                "Все заявки SUCCESS, все сегменты required и sufficient = null",
                repeat(changeOrderSegmentRequest().setRequired(true))
            ),
            Pair.of(
                "Все заявки SEGMENT_NOT_STARTED, все сегменты required и sufficient = null",
                repeat(changeOrderSegmentRequest().setRequired(true)
                    .setStatus(ChangeOrderSegmentRequestStatus.SEGMENT_NOT_STARTED))
            ),
            //required = true sufficient = false
            Pair.of(
                "Все заявки SUCCESS, все сегменты required и не sufficient",
                repeat(changeOrderSegmentRequest().setRequired(true).setSufficient(true))
            ),
            Pair.of(
                "Все заявки SEGMENT_NOT_STARTED, все сегменты required и не sufficient",
                repeat(changeOrderSegmentRequest(true, false, ChangeOrderSegmentRequestStatus.SEGMENT_NOT_STARTED))
            ),
            //required = false sufficient = true
            Pair.of(
                "Все заявки SUCCESS, все сегменты не required и sufficient",
                repeat(changeOrderSegmentRequest().setSufficient(Boolean.TRUE))
            ),
            Pair.of(
                "Все заявки SEGMENT_NOT_STARTED, все сегменты не required и sufficient",
                repeat(changeOrderSegmentRequest()
                    .setSufficient(Boolean.TRUE)
                    .setStatus(ChangeOrderSegmentRequestStatus.SEGMENT_NOT_STARTED))
            ),
            //required = false sufficient = null
            Pair.of(
                "Все заявки SUCCESS, все сегменты не required и sufficient = null",
                repeat(changeOrderSegmentRequest())
            ),
            Pair.of(
                "Все заявки SEGMENT_NOT_STARTED, все сегменты не required и sufficient = null",
                repeat(changeOrderSegmentRequest().setStatus(ChangeOrderSegmentRequestStatus.SEGMENT_NOT_STARTED))
            ),
            Pair.of(
                "Все заявки SUCCESS, один обязательный сегмент",
                Set.of(
                    changeOrderSegmentRequest().setRequired(true),
                    changeOrderSegmentRequest(),
                    changeOrderSegmentRequest()
                )
            ),
            Pair.of(
                "Все заявки SUCCESS, один обязательный и один достаточный  сегмент",
                Set.of(
                    changeOrderSegmentRequest().setRequired(true),
                    changeOrderSegmentRequest().setSufficient(Boolean.TRUE),
                    changeOrderSegmentRequest()
                )
            ),
            Pair.of(
                "Все заявки SUCCESS, один достаточный  сегмент",
                Set.of(
                    changeOrderSegmentRequest(),
                    changeOrderSegmentRequest().setSufficient(Boolean.TRUE),
                    changeOrderSegmentRequest()
                )
            )
        )
            .map(p -> Arguments.of("SUCCESS: " + p.getLeft(), p.getRight(), ChangeOrderRequestStatus.SUCCESS));
    }

    @Test
    @DisplayName("Для всех возможных статусов есть тест")
    void testExistForAllFinalStatusesAndNull() {
        Set<ChangeOrderRequestStatus> collect = EnumSet.allOf(ChangeOrderRequestStatus.class).stream()
            .filter(status -> !status.isActive() && !IMPOSSIBLE_STATUSES.contains(status))
            .filter(status -> !STATUSES_SUPPLIERS.containsKey(status)).collect(Collectors.toSet());

        softly.assertThat(collect.isEmpty());
    }

    @ParametersAreNonnullByDefault
    private static class TestingProcessor extends AbstractUpdateChangeOrderRequestProcessor {

        TestingProcessor(
            ChangeOrderRequestService changeOrderRequestService,
            OrderService orderService,
            UpdateOrderItemsStatsService updateOrderItemsStatsService,
            NotifyOrderErrorToMqmProducer notifyOrderErrorToMqmProducer
        ) {
            super(changeOrderRequestService, orderService, updateOrderItemsStatsService, notifyOrderErrorToMqmProducer);
        }

        @Nullable
        @Override
        public ChangeOrderRequestStatus resolveStatus(ChangeOrderRequest changeOrderRequest) {
            return super.resolveStatus(changeOrderRequest);
        }
    }

    private void checkStatus(ChangeOrderRequest request, ChangeOrderRequestStatus fail) {
        softly.assertThat(processor.resolveStatus(request)).isEqualTo(fail);
    }

    @Nonnull
    private static ChangeOrderSegmentRequest changeOrderSegmentRequest() {
        return changeOrderSegmentRequest(false, null, ChangeOrderSegmentRequestStatus.SUCCESS);
    }

    @Nonnull
    private static Set<ChangeOrderSegmentRequest> repeat(ChangeOrderSegmentRequest request) {
        return Stream.generate(
            () -> new ChangeOrderSegmentRequest()
                .setStatus(request.getStatus())
                .setSufficient(request.getSufficient())
                .setRequired(request.isRequired())
        )
            .limit(3)
            .collect(Collectors.toSet());
    }

    @Nonnull
    private static ChangeOrderSegmentRequest changeOrderSegmentRequest(
        boolean isRequired,
        @Nullable Boolean isSufficient,
        ChangeOrderSegmentRequestStatus status
    ) {
        return new ChangeOrderSegmentRequest().setRequired(isRequired).setSufficient(isSufficient).setStatus(status);
    }

    @Nonnull
    private ChangeOrderRequest changeOrderRequest(Set<ChangeOrderSegmentRequest> segmentRequests) {
        return new ChangeOrderRequest().setChangeOrderSegmentRequests(segmentRequests);
    }
}

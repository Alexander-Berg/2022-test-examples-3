package ru.yandex.market.tpl.carrier.core.service.yt.runstatus;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunItem;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.carrier.core.domain.usershift.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.OrderReturnTask;
import ru.yandex.market.tpl.carrier.core.domain.usershift.OrderReturnTaskStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftStatus;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

@RequiredArgsConstructor(onConstructor_ = @Autowired)

@CoreTestV2
public class YtRunStatusMultiItemTest {
    private final RunGenerator runGenerator;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final YtRunStatusProducer producer;
    private final RunRepository runRepository;
    private final TransactionTemplate transactionTemplate;
    private final OrderWarehouseGenerator orderWarehouseGenerator;

    private Run run;
    private RunItem firstRunItem;
    private RunItem secondRunItem;


    @BeforeEach
    void setUp() {
        run = runGenerator.generate(t -> t
                .clearItems()
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .build())
                        .orderNumber(1)
                        .fromIndex(1)
                        .toIndex(4)
                        .build()
                )
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .build())
                        .orderNumber(2)
                        .fromIndex(2)
                        .toIndex(3)
                        .build()
                ));
        firstRunItem = run.streamRunItems().filterBy(RunItem::getOrderNumber, 1).findFirst().get();
        secondRunItem = run.streamRunItems().filterBy(RunItem::getOrderNumber, 2).findFirst().get();
    }

    @Test
    void shouldProcessOutbound() {
        producer.produce(
                run.getId(),
                firstRunItem.getId(),
                YtRunStatus.OUTBOUND,
                "Иванов",
                "Иван",
                "Иванович",
                "в921сн",
                null
        );
        dbQueueTestUtil.executeAllQueueItems(QueueType.YT_RUN_STATUS);

        transactionTemplate.execute(tc -> {
            var runConfirmed = runRepository.findByIdOrThrow(run.getId());
            var userShift = runConfirmed.getFirstAssignedShift();

            Assertions.assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
            Assertions.assertThat(userShift.streamCollectDropshipTasks().findFirst().get().getStatus()).isEqualTo(CollectDropshipTaskStatus.FINISHED);

            return null;
        });
    }

    @Test
    void shouldNotFailIfOutboundOutOfOrder() {
        producer.produce(
                run.getId(),
                secondRunItem.getId(),
                YtRunStatus.OUTBOUND,
                "Иванов",
                "Иван",
                "Иванович",
                "в921сн",
                null
        );
        dbQueueTestUtil.executeAllQueueItems(QueueType.YT_RUN_STATUS);
        transactionTemplate.execute(tc -> {
            var runConfirmed = runRepository.findByIdOrThrow(run.getId());
            var userShift = runConfirmed.streamUserShifts().findFirst().get();

            Assertions.assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
            Assertions.assertThat(userShift.streamCollectDropshipTasks()
                            .map(CollectDropshipTask::getStatus)
                            .collect(Collectors.toList()))
                            .containsExactly(CollectDropshipTaskStatus.NOT_STARTED, CollectDropshipTaskStatus.NOT_STARTED);

            return null;
        });
    }

    @Test
    void shouldNotFailIfInboundOutOfOrder() {
        producer.produce(
                run.getId(),
                secondRunItem.getId(),
                YtRunStatus.INBOUND,
                "Иванов",
                "Иван",
                "Иванович",
                "в921сн",
                null
        );
        dbQueueTestUtil.executeAllQueueItems(QueueType.YT_RUN_STATUS);
        transactionTemplate.execute(tc -> {
            var runConfirmed = runRepository.findByIdOrThrow(run.getId());
            var userShift = runConfirmed.streamUserShifts().findFirst().get();

            Assertions.assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
            Assertions.assertThat(userShift.streamCollectDropshipTasks()
                            .map(CollectDropshipTask::getStatus)
                            .collect(Collectors.toList()))
                    .containsExactly(CollectDropshipTaskStatus.NOT_STARTED, CollectDropshipTaskStatus.NOT_STARTED);
            Assertions.assertThat(userShift.streamReturnRoutePoints().flatMap(RoutePoint::streamReturnTasks)
                            .map(OrderReturnTask::getStatus)
                            .collect(Collectors.toList()))
                    .containsExactly(OrderReturnTaskStatus.NOT_STARTED, OrderReturnTaskStatus.NOT_STARTED);


            return null;
        });
    }
    @Test
    void shouldProcessInbound() {
        producer.produce(
                run.getId(),
                firstRunItem.getId(),
                YtRunStatus.OUTBOUND,
                "Иванов",
                "Иван",
                "Иванович",
                "в921сн",
                null
        );
        producer.produce(
                run.getId(),
                secondRunItem.getId(),
                YtRunStatus.OUTBOUND,
                "Иванов",
                "Иван",
                "Иванович",
                "в921сн",
                null
        );
        producer.produce(
                run.getId(),
                secondRunItem.getId(),
                YtRunStatus.INBOUND,
                "Иванов",
                "Иван",
                "Иванович",
                "в921сн",
                null
        );
        producer.produce(
                run.getId(),
                firstRunItem.getId(),
                YtRunStatus.INBOUND,
                "Иванов",
                "Иван",
                "Иванович",
                "в921сн",
                null
        );
        dbQueueTestUtil.executeAllQueueItems(QueueType.YT_RUN_STATUS);

        transactionTemplate.execute(tc -> {
            var runConfirmed = runRepository.findByIdOrThrow(run.getId());
            var userShift = runConfirmed.streamUserShifts().findFirst().get();

            Assertions.assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_FINISHED);

            return null;
        });
    }
}

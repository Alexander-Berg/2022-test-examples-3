package ru.yandex.market.tpl.carrier.core.service.yt.runstatus;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunItem;
import ru.yandex.market.tpl.carrier.core.domain.run.RunItemAbstract;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

@RequiredArgsConstructor(onConstructor_=@Autowired)

@CoreTestV2
public class YtRunStatusTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private final RunHelper runHelper;
    private final RunGenerator runGenerator;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final YtRunStatusProducer producer;
    private final RunRepository runRepository;
    private final TransactionTemplate transactionTemplate;

    private Run run;
    private Run run2;

    @BeforeEach
    void setUp() {
        run = runGenerator.generate();
        run2 = runGenerator.generate();
    }

    @Test
    void shouldConfirm() {

        producer.produce(
                run.getId(),
                null,
                YtRunStatus.CONFIRMED,
                "",
                "",
                "",
                "",
                ""
        );
        dbQueueTestUtil.executeAllQueueItems(QueueType.YT_RUN_STATUS);

        transactionTemplate.execute(tc -> {
            var runConfirmed = runRepository.findByIdOrThrow(run.getId());
            Assertions.assertThat(runConfirmed.getStatus()).isEqualTo(RunStatus.CONFIRMED);
            return null;
        });
    }

    @Test
    void shouldAssign() {
        producer.produce(
                run.getId(),
                null,
                YtRunStatus.OUTBOUND_READY,
                "Иванов",
                "Иван",
                "Иванович",
                "в921сн",
                null
        );
        dbQueueTestUtil.executeAllQueueItems(QueueType.YT_RUN_STATUS);

        transactionTemplate.execute(tc -> {
            var runConfirmed = runRepository.findByIdOrThrow(run.getId());
            Assertions.assertThat(runConfirmed.getStatus()).isEqualTo(RunStatus.ASSIGNED);
            Assertions.assertThat(runConfirmed.getUser()).isNotNull();
            Assertions.assertThat(runConfirmed.getUser().getLastName()).isEqualTo("Иванов");
            Assertions.assertThat(runConfirmed.getUser().getFirstName()).isEqualTo("Иван");
            Assertions.assertThat(runConfirmed.getUser().getPatronymic()).isEqualTo("Иванович");
            Assertions.assertThat(runConfirmed.getTransport()).isNotNull();
            Assertions.assertThat(runConfirmed.getTransport().getNumber()).isEqualTo("B921CH");

            UserShift userShift = runConfirmed.getFirstAssignedShift();
            Assertions.assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
            return null;
        });
    }

    @Test
    void shouldAssignIfRunItemIdSpecified() {
        producer.produce(
                run.getId(),
                run.streamRunItems().findFirst().map(RunItem::getId).get(),
                YtRunStatus.OUTBOUND_READY,
                "Иванов",
                "Иван",
                "Иванович",
                "в921сн",
                null
        );
        dbQueueTestUtil.executeAllQueueItems(QueueType.YT_RUN_STATUS);

        transactionTemplate.execute(tc -> {
            var runConfirmed = runRepository.findByIdOrThrow(run.getId());
            Assertions.assertThat(runConfirmed.getStatus()).isEqualTo(RunStatus.STARTED);
            Assertions.assertThat(runConfirmed.getUser()).isNotNull();
            Assertions.assertThat(runConfirmed.getUser().getLastName()).isEqualTo("Иванов");
            Assertions.assertThat(runConfirmed.getUser().getFirstName()).isEqualTo("Иван");
            Assertions.assertThat(runConfirmed.getUser().getPatronymic()).isEqualTo("Иванович");
            Assertions.assertThat(runConfirmed.getTransport()).isNotNull();
            Assertions.assertThat(runConfirmed.getTransport().getNumber()).isEqualTo("B921CH");

            UserShift userShift = runConfirmed.getFirstAssignedShift();
            Assertions.assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
            return null;
        });
    }

    @Test
    void shouldProcessOutbound() {
        producer.produce(
                run.getId(),
                run.streamRunItems().map(RunItem::getId).findFirst().get(),
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
    void shouldProcessTransit() {
        producer.produce(
                run.getId(),
                run.streamRunItems().map(RunItem::getId).findFirst().get(),
                YtRunStatus.TRANSIT,
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
    void shouldProcessOutboundIdempotently() {
        producer.produce(
                run.getId(),
                run.streamRunItems().map(RunItem::getId).findFirst().get(),
                YtRunStatus.OUTBOUND,
                "Иванов",
                "Иван",
                "Иванович",
                "в921сн",
                null
        );
        producer.produce(
                run.getId(),
                run.streamRunItems().map(RunItem::getId).findFirst().get(),
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
    void shouldProcessInboundIdempotently() {
        producer.produce(
                run.getId(),
                run.streamRunItems().map(RunItem::getId).findFirst().get(),
                YtRunStatus.INBOUND,
                "Иванов",
                "Иван",
                "Иванович",
                "в921сн",
                null
        );
        producer.produce(
                run.getId(),
                run.streamRunItems().map(RunItem::getId).findFirst().get(),
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

    @Test
    void shouldSwitchActiveShift() {
        producer.produce(
                run.getId(),
                run.streamRunItems().map(RunItemAbstract::getId).findFirst().get(),
                YtRunStatus.OUTBOUND_READY,
                "Иванов",
                "Иван",
                "Иванович",
                "в921сн",
                null
        );

        producer.produce(
                run2.getId(),
                run2.streamRunItems().map(RunItemAbstract::getId).findFirst().get(),
                YtRunStatus.OUTBOUND_READY,
                "Иванов",
                "Иван",
                "Иванович",
                "в921сн",
                null
        );
        dbQueueTestUtil.executeAllQueueItems(QueueType.YT_RUN_STATUS);

        transactionTemplate.execute(tc -> {
            run = runRepository.findByIdOrThrow(run.getId());
            run2 = runRepository.findByIdOrThrow(run2.getId());

            UserShift userShift1 = run.streamUserShifts().findFirst().get();
            UserShift userShift2 = run2.streamUserShifts().findFirst().get();

            Assertions.assertThat(userShift1.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
            Assertions.assertThat(userShift2.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);

            return null;
        });
    }
}

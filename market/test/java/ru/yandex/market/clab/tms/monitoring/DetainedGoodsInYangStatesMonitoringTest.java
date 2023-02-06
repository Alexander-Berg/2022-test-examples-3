package ru.yandex.market.clab.tms.monitoring;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.clab.common.service.good.GoodRepositoryStub;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DetainedGoodsInYangStatesMonitoringTest {

    private DetainedGoodsInYangStatesMonitoring monitoring;
    private GoodRepositoryStub goodRepository;

    @Before
    public void before() {
        goodRepository = new GoodRepositoryStub();
        monitoring = new DetainedGoodsInYangStatesMonitoring(goodRepository);
    }

    @Test
    public void emptyIsOk() {
        ComplexMonitoring.Result result = monitoring.check();

        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
        assertThat(result.getMessage()).isEqualTo("OK");
    }

    @Test
    public void yangTaskReadyStateUpdateRecentlyIsOk() {
        goodRepository.save(new Good()
            .setState(GoodState.YANG_TASK_READY)
            .setCurrentStateDate(LocalDateTime.now().minusHours(23)));

        ComplexMonitoring.Result result = monitoring.check();

        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
        assertThat(result.getMessage()).isEqualTo("OK");
    }

    @Test
    public void yangTaskStateUpdateRecentlyIsOk() {
        goodRepository.save(new Good()
            .setState(GoodState.YANG_TASK)
            .setCurrentStateDate(LocalDateTime.now().minusDays(6)));

        ComplexMonitoring.Result result = monitoring.check();

        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
        assertThat(result.getMessage()).isEqualTo("OK");
    }

    @Test
    public void yangTaskReadyStateUpdateLongAgoIsWarning() {
        goodRepository.save(new Good()
            .setState(GoodState.YANG_TASK_READY)
            .setCurrentStateDate(LocalDateTime.now().minusHours(25)));

        ComplexMonitoring.Result result = monitoring.check();

        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(result.getMessage()).isEqualTo("found 1 detained goods in state Готов к выдаче в задание : 0");
    }

    @Test
    public void yangTaskStatesUpdateLongAgoIsWarning() {
        goodRepository.save(new Good()
            .setState(GoodState.YANG_TASK)
            .setCurrentStateDate(LocalDateTime.now().minusDays(8)));

        ComplexMonitoring.Result result = monitoring.check();

        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(result.getMessage()).isEqualTo("found 1 detained goods in state Выдан в задание : 0");
    }

    @Test
    public void manyYangTaskStateUpdateLongAgoIsWarning() {
        Stream.generate(() -> LocalDateTime.now().minusDays(8))
            .map(ts -> new Good()
                .setState(GoodState.YANG_TASK)
                .setCurrentStateDate(ts))
            .limit(11)
            .forEach(goodRepository::save);

        ComplexMonitoring.Result result = monitoring.check();

        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(result.getMessage())
            .isEqualTo("found 11 detained goods in state Выдан в задание : 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, ...");
    }
}

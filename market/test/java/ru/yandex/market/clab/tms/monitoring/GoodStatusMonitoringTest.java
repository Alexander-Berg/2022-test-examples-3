package ru.yandex.market.clab.tms.monitoring;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.clab.common.service.good.GoodRepositoryStub;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodErrorType;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 25.12.2018
 */
public class GoodStatusMonitoringTest {

    private static final int ERROR_GOODS_TO_CRITICAL = 43;
    private GoodStatusMonitoring goodStatusMonitoring;
    private GoodRepositoryStub goodRepository;

    @Before
    public void before() {
        goodRepository = new GoodRepositoryStub();
        goodStatusMonitoring = new GoodStatusMonitoring(goodRepository);
    }

    @Test
    public void emptyIsOk() {
        ComplexMonitoring.Result result = goodStatusMonitoring.check();

        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
        assertThat(result.getMessage()).isEqualTo("OK");
    }

    @Test
    public void noGoodsWithErrorsIsOk() {
        Stream.of(GoodState.values())
            .map(this::good)
            .forEach(goodRepository::save);

        ComplexMonitoring.Result result = goodStatusMonitoring.check();

        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
        assertThat(result.getMessage()).isEqualTo("OK");
    }

    @Test
    public void fewGoodsWithErrorIsWarning() {
        Stream.of(GoodErrorType.values())
            .map(errorType -> {
                Good good = good(GoodState.ACCEPTED);
                good.setErrorType(errorType);
                return good;
            })
            .limit(4)
            .forEach(goodRepository::save);

        ComplexMonitoring.Result result = goodStatusMonitoring.check();

        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(result.getMessage()).isEqualTo("found 4 goods with errors; 0, 1, 2, 3");
    }

    @Test
    public void moveThenCriticalLimitShouldBeCritical() {
        Stream.generate(() -> GoodErrorType.NO_SS_BARCODES)
            .map(errorType -> {
                Good good = good(GoodState.ACCEPTED);
                good.setErrorType(errorType);
                return good;
            })
            .limit(2 * ERROR_GOODS_TO_CRITICAL + 1)
            .forEach(goodRepository::save);

        ComplexMonitoring.Result result = goodStatusMonitoring.check();

        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(result.getMessage()).isEqualTo("found 87 goods with errors; 0, 1, 2, 3, 4, 5, 6, ...");
    }

    private Good good(GoodState state) {
        Good good = new Good();
        good.setState(state);
        return good;
    }
}

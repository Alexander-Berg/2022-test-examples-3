package ru.yandex.cs.placement.tms.mbo;

import java.time.Clock;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;

class LoadMboBrandsExecutorFunctionalTest extends AbstractCsPlacementTmsFunctionalTest {

    private final LoadMboBrandsExecutor loadMboBrandsExecutor;
    private final Clock clock;

    @Autowired
    LoadMboBrandsExecutorFunctionalTest(LoadMboBrandsExecutor loadMboBrandsExecutor,
                                        Clock clock) {
        this.loadMboBrandsExecutor = loadMboBrandsExecutor;
        this.clock = clock;
    }

    @DisplayName("Вставить новые, обновить уже существующие, логически удалить отсутствующие в выгрузке бренды")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/mbo/LoadMboBrandsExecutorFunctionalTest/testLoadBrands/before.csv",
            after = "/ru/yandex/cs/placement/tms/mbo/LoadMboBrandsExecutorFunctionalTest/testLoadBrands/after.csv"
    )
    @Test
    void testLoadBrands() {
        Mockito.when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, 12, 22, 0, 10)));
        loadMboBrandsExecutor.doJob(null);
    }
}

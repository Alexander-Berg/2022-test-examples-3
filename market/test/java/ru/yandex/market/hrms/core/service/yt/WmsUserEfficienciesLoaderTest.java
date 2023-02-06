package ru.yandex.market.hrms.core.service.yt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.warehouse.jdbc.WmsPerformanceHistoryClickhouseRepo;
import ru.yandex.market.hrms.core.domain.yt.WmsUserEfficiencyDto;
import ru.yandex.market.hrms.core.service.util.HrmsDateTimeUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class WmsUserEfficienciesLoaderTest extends AbstractCoreTest {
    private static final LocalDate DATE = LocalDate.parse("2022-06-12");

    @MockBean
    private WmsPerformanceHistoryClickhouseRepo chRepo;
    @Autowired
    private WmsUserEfficienciesLoader loader;

    @Test
    public void shouldNotReturnDuplicates() {
        var first = new WmsUserEfficiencyDto("any", DATE, BigDecimal.ZERO, BigDecimal.valueOf(12),
                HrmsDateTimeUtil.toInstant(DATE.plusDays(6)));
        var second = new WmsUserEfficiencyDto("any", DATE, BigDecimal.ONE, BigDecimal.valueOf(123),
                HrmsDateTimeUtil.toInstant(DATE.plusDays(7)));

        when(chRepo.loadEfficiencies(any()))
                .thenReturn(List.of(first, second));

        var loaded = loader.loadEfficiencies(null);
        Assertions.assertArrayEquals(new WmsUserEfficiencyDto[]{second}, loaded.toArray());
    }

}
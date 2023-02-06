package ru.yandex.market.hrms.core.domain.outstaff;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;

@DbUnitDataSet(before = "OutstaffShiftPropertyTest.before.csv")
class OutstaffShiftPropertyRepoTest extends AbstractCoreTest {

    private static final long SOFINO = 1L;

    @Autowired
    OutstaffShiftPropertyRepo outstaffShiftPropertyRepo;

    @Autowired
    Clock clock;

    @Test
    void simpleTest() {
        mockClock(LocalDate.of(2021, 1, 1));
        // assert
        var domainShift =
                outstaffShiftPropertyRepo.getByDomainIdAndInterval(SOFINO, LocalDate.now(clock), LocalDate.now(clock));
        Assertions.assertThat(domainShift)
                .containsExactlyInAnyOrder(OutstaffShiftProperty.builder()
                                .id(10L)
                                .domainId(SOFINO)
                                .shiftStartTime(LocalTime.of(4, 0))
                                .shiftEndTime(LocalTime.of(20, 0))
                                .controlShiftTime(LocalTime.of(10, 30))
                                .build(),
                        OutstaffShiftProperty.builder()
                                .id(20L)
                                .domainId(SOFINO)
                                .shiftStartTime(LocalTime.of(16, 0))
                                .shiftEndTime(LocalTime.of(4, 0))
                                .controlShiftTime(LocalTime.of(10, 30))
                                .build()
                );
    }
}

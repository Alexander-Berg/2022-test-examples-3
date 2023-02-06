package ru.yandex.market.hrms.core.domain.outstaff;

import java.time.Instant;
import java.util.List;

import com.google.common.collect.BoundType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

@DbUnitDataSet(before = "OutstaffTimexServiceTest.before.csv")
class OutstaffTimexServiceTest extends AbstractCoreTest {

    @Autowired
    OutstaffTimexService outstaffTimexService;

    @Test
    void empty() {
        var events = outstaffTimexService.loadByOutstaffIds(
                1, LocalDateInterval.valueOf("2021-06-01/2021-06-01", BoundType.CLOSED), List.of(100L));
        Assertions.assertThat(events).isEmpty();
    }

    @Test
    @DbUnitDataSet(before = "OutstaffTimexServiceTest.simple.csv")
    void simple() {
        var events = outstaffTimexService.loadByOutstaffIds(
                1, LocalDateInterval.valueOf("2021-06-01/2021-06-01", BoundType.CLOSED), List.of(100L));
        Assertions.assertThat(events).containsExactlyInAnyOrder(
                OutstaffTimexLog.builder()
                        .outstaffId(100L)
                        .area("ФФЦ Софьино - операционный зал")
                        .enterTs(Instant.parse("2021-06-01T09:00:00.00Z"))
                        .leaveTs(Instant.parse("2021-06-01T10:00:00.00Z"))
                        .build()
        );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffTimexServiceTest.several.csv")
    void several() {
        var events = outstaffTimexService.loadByOutstaffIds(
                1, LocalDateInterval.valueOf("2021-06-01/2021-06-01", BoundType.CLOSED), List.of(100L));
        Assertions.assertThat(events).containsExactlyInAnyOrder(
                OutstaffTimexLog.builder()
                        .outstaffId(100L)
                        .area("ФФЦ Софьино - операционный зал")
                        .enterTs(Instant.parse("2021-06-01T09:00:00.00Z"))
                        .leaveTs(Instant.parse("2021-06-01T10:00:00.00Z"))
                        .build(),
                OutstaffTimexLog.builder()
                        .outstaffId(100L)
                        .area("ФФЦ Софьино - операционный зал")
                        .enterTs(Instant.parse("2021-06-01T12:00:00.00Z"))
                        .leaveTs(Instant.parse("2021-06-01T13:00:00.00Z"))
                        .build(),
                OutstaffTimexLog.builder()
                        .outstaffId(100L)
                        .area("ФФЦ Софьино - операционный зал")
                        .enterTs(Instant.parse("2021-06-01T13:00:00.00Z"))
                        .leaveTs(Instant.parse("2021-06-01T14:00:00.00Z"))
                        .build(),
                OutstaffTimexLog.builder()
                        .outstaffId(100L)
                        .area("ФФЦ Софьино - операционный зал")
                        .enterTs(Instant.parse("2021-06-01T15:00:00.00Z"))
                        .leaveTs(Instant.parse("2021-06-01T15:00:00.00Z"))
                        .build()
        );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffTimexServiceTest.ignoreTimex.csv")
    void ignoreTimex() {
        var events = outstaffTimexService.loadByOutstaffIds(
                1, LocalDateInterval.valueOf("2021-06-01/2021-06-01", BoundType.CLOSED), List.of(100L));
        Assertions.assertThat(events).containsExactlyInAnyOrder(
                OutstaffTimexLog.builder()
                        .outstaffId(100L)
                        .area("ФФЦ Софьино - операционный зал")
                        .enterTs(Instant.parse("2021-06-01T09:00:00.00Z"))
                        .leaveTs(Instant.parse("2021-06-01T10:00:00.00Z"))
                        .build()
        );
    }
}

package ru.yandex.market.hrms.core.service.outstaff.loader;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.domain.repo.Domain;
import ru.yandex.market.hrms.core.service.outstaff.loader.base.OutstaffWmsClickHouseLoader;
import ru.yandex.market.hrms.core.service.outstaff.shift.OutstaffData;
import ru.yandex.market.hrms.core.service.outstaff.shift.OutstaffEvent;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

@DbUnitDataSet(before = "OutstaffWmsClickHouseLoaderTest.before.csv")
public class OutstaffWmsClickHouseLoaderTest extends AbstractCoreTest {

    @Autowired
    OutstaffWmsClickHouseLoader outstaffWmsClickHouseLoader;

    @Test
    void loadData() {
        LocalDate date = LocalDate.of(2021, 6, 1);
        Map<OutstaffData, List<OutstaffEvent>> load = outstaffWmsClickHouseLoader.load(
                Domain.builder().id(1L).timezone(ZoneId.of("Europe/Moscow")).build(),
                new LocalDateInterval(date, date),
                Set.of(100L)
        );
        Map<OutstaffData, List<OutstaffEvent>> map = Map.of(
                OutstaffData.builder()
                        .outstaffId(100L)
                        .build(), List.of(
                        OutstaffEvent.builder()
                                .type(OutstaffEvent.Type.WMS_CLICKHOUSE)
                                .firstTs(Instant.parse("2021-05-31T21:00:00Z"))
                                .lastTs(Instant.parse("2021-05-31T22:00:00Z"))
                                .build(),
                        OutstaffEvent.builder()
                                .type(OutstaffEvent.Type.WMS_CLICKHOUSE)
                                .firstTs(Instant.parse("2021-05-31T22:00:00Z"))
                                .lastTs(Instant.parse("2021-05-31T23:00:00Z"))
                                .build(),
                        OutstaffEvent.builder()
                                .type(OutstaffEvent.Type.WMS_CLICKHOUSE)
                                .firstTs(Instant.parse("2021-06-01T20:00:00Z"))
                                .lastTs(Instant.parse("2021-06-01T21:00:00Z"))
                                .build()
                )
        );
        Assertions.assertThat(load).isEqualTo(map);
    }

}

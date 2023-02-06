package ru.yandex.market.hrms.core.domain.ws.sc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.ws.sc.repo.ScActionStatClickhouseRepo;
import ru.yandex.market.hrms.core.domain.ws.sc.repo.ScActionStatEntity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class ScActionStatImportServiceTest extends AbstractCoreTest {

    @MockBean
    private ScActionStatClickhouseRepo scActionStatClickhouseRepo;

    @Autowired
    private ScActionStatImportService scActionStatImportService;

    @Test
    @DbUnitDataSet(before = "ScActionStatImportServiceTest.before.csv",
                   after = "ScActionStatImportServiceTest.after.csv")
    public void scStatsShouldBeUpdated() {
        Mockito.when(scActionStatClickhouseRepo.loadDataNotEarlierThan(
                eq(LocalDate.of(2021, 9, 15)), any()))
        .thenReturn(List.of(
                new ScActionStatEntity(
                        null,
                        Instant.parse("2021-09-15T13:00:00Z").atOffset(ZoneOffset.UTC).toInstant(),
                        Instant.parse("2021-09-15T13:02:11Z").atOffset(ZoneOffset.UTC).toInstant(),
                        Instant.parse("2021-09-15T13:55:09Z").atOffset(ZoneOffset.UTC).toInstant(),
                        1234L,
                        5678L
                ),
                new ScActionStatEntity(
                        null,
                        Instant.parse("2021-09-15T14:00:00Z").atOffset(ZoneOffset.UTC).toInstant(),
                        Instant.parse("2021-09-15T14:05:00Z").atOffset(ZoneOffset.UTC).toInstant(),
                        Instant.parse("2021-09-15T11:46:00Z").atOffset(ZoneOffset.UTC).toInstant(),
                        1234L,
                        5678L
                ),
                new ScActionStatEntity(
                        null,
                        Instant.parse("2021-09-15T15:00:00Z").atOffset(ZoneOffset.UTC).toInstant(),
                        Instant.parse("2021-09-15T15:10:00Z").atOffset(ZoneOffset.UTC).toInstant(),
                        Instant.parse("2021-09-15T15:55:00Z").atOffset(ZoneOffset.UTC).toInstant(),
                        1234L,
                        5678L
                ),                new ScActionStatEntity(
                        null,
                        Instant.parse("2021-09-15T16:00:00Z").atOffset(ZoneOffset.UTC).toInstant(),
                        Instant.parse("2021-09-15T16:08:00Z").atOffset(ZoneOffset.UTC).toInstant(),
                        Instant.parse("2021-09-15T16:48:00Z").atOffset(ZoneOffset.UTC).toInstant(),
                        1234L,
                        5678L
                ),                new ScActionStatEntity(
                        null,
                        Instant.parse("2021-09-15T17:00:00Z").atOffset(ZoneOffset.UTC).toInstant(),
                        Instant.parse("2021-09-15T17:07:00Z").atOffset(ZoneOffset.UTC).toInstant(),
                        Instant.parse("2021-09-15T17:22:00Z").atOffset(ZoneOffset.UTC).toInstant(),
                        1234L,
                        5678L
                )
        ));

        scActionStatImportService.importForDate(LocalDate.of(2021, 9, 15));
    }
}

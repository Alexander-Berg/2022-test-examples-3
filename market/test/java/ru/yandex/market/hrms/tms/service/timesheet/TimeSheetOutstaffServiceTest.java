package ru.yandex.market.hrms.tms.service.timesheet;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.model.domain.DomainType;
import ru.yandex.market.hrms.core.service.domain.DomainService;
import ru.yandex.market.hrms.core.service.timesheet.TimeSheetService;

@DbUnitDataSet(before = "TimeSheetOutstaffServiceTest.before.csv")
class TimeSheetOutstaffServiceTest extends AbstractCoreTest {

    @Autowired
    private TimeSheetService timeSheetService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Map<DomainType, Long> domainIds = Map.of(
            DomainType.FFC, 4L,
            DomainType.SC, 42L,
            DomainType.RW, 52L
    );

    @ParameterizedTest
    @CsvSource({
            "FFC,TimeSheetOutstaffServiceTest.ffc.json",
            "SC,TimeSheetOutstaffServiceTest.sc.json",
            "RW,TimeSheetOutstaffServiceTest.rw.json"
    })
    public void happyPath(DomainType domainType, String expectedJsonFilepath) throws Exception {
        var now = Instant.parse("2022-02-14T18:00:00+03:00");
        mockClock(LocalDateTime.ofInstant(now, ZoneId.systemDefault()));
        YearMonth yearMonth = YearMonth.of(2022, 2);
        var domain = domainService.findByIdOrThrow(domainIds.get(domainType));
        var result = timeSheetService.getOutstaffTimeSheet(domain, yearMonth);
        var resultJson = objectMapper.writeValueAsString(result);

        JSONAssert.assertEquals(loadFromFile(expectedJsonFilepath), resultJson, false);
    }
}

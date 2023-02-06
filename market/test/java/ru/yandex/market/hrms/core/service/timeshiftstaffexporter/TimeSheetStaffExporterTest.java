package ru.yandex.market.hrms.core.service.timeshiftstaffexporter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeType;
import ru.yandex.market.hrms.core.domain.position.PositionType;
import ru.yandex.market.hrms.core.domain.time_sheet.StaffTimeSheetYtView;
import ru.yandex.market.hrms.core.service.yt.TimeSheetStaffExporter;
import ru.yandex.market.hrms.core.service.yt.YtService;
import ru.yandex.market.hrms.model.domain.DomainType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

@DbUnitDataSet(before = "CommonSet.before.csv")
public class TimeSheetStaffExporterTest extends AbstractCoreTest {

    @Autowired
    TimeSheetStaffExporter timeSheetStaffExporter;

    @SpyBean
    YtService ytService;


    @Test
    void removeOldDomains()  {

        YearMonth period = YearMonth.of(2022, 1);

        LocalDate start = period.atDay(1);

        List<StaffTimeSheetYtView> rows = new ArrayList<>();

        DateTimeUtil.asStream(new LocalDateInterval(period.atDay(11), period.atEndOfMonth())).reverseSorted()
                .forEach(date -> rows.add(
                        StaffTimeSheetYtView.builder()
                                .date(date.toString())
                                .employeeId(6132L)
                                .name("Палагнюк Наталья Сергеевна")
                                .staffLogin("n-palagnyuk")
                                .position("Диспетчер")
                                .employeeType(EmployeeType.EXTERNAL.name())
                                .positionType(PositionType.ASSISTIVE.getName())
                                .joinedAt("2019-11-19")
                                .workedTime(0L)
                                .warehouseId(172L)
                                .domainId(1L)
                                .domainName("ФФЦ Софьино")
                                .domainType(DomainType.FFC.name())
                                .build())
                );

        DateTimeUtil.asStream(new LocalDateInterval(start, period.atDay(10))).reverseSorted()
                .forEach(date -> rows.add(
                        StaffTimeSheetYtView.builder()
                                .date(date.toString())
                                .employeeId(6132L)
                                .name("Палагнюк Наталья Сергеевна")
                                .staffLogin("n-palagnyuk")
                                .position("Диспетчер")
                                .employeeType(EmployeeType.EXTERNAL.name())
                                .positionType(PositionType.ASSISTIVE.getName())
                                .joinedAt("2019-11-19")
                                .workedTime(0L)
                                .warehouseId(172L)
                                .domainId(1L)
                                .domainName("ФФЦ Софьино")
                                .domainType(DomainType.FFC.name())
                                .departmentName("[Старое] Обработка первичных документов")
                                .shiftName("Обработка первичных документов (1 смена)")
                                .departmentId(112L)
                                .shiftId(113L)
                                .build())
                );




        Mockito.doNothing().when(ytService)
                .export(rows,
                        StaffTimeSheetYtView.class,
                        "",
                        "staff_time_sheet",
                        "2022-01",
                        true);

        timeSheetStaffExporter.uploadDataForDateV2(period);
    }
}

package ru.yandex.market.hrms.core.service.outstaff.document.excel;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.domain.repo.Domain;
import ru.yandex.market.hrms.core.domain.outstaff.OutstaffCompanyEntity;
import ru.yandex.market.hrms.core.service.outstaff_document.excel.OutstaffTimesheetExcelLoaderService;
import ru.yandex.market.hrms.core.service.outstaff_document.excel.OutstaffTimesheetExcelSerializer;
import ru.yandex.market.hrms.model.outstaff.timesheet.OutstaffTimesheetExcel;
import ru.yandex.market.tpl.common.excel.matchers.ExcelSheetMatcher;

import static org.hamcrest.core.AnyOf.anyOf;
import static ru.yandex.market.tpl.common.excel.matchers.ExcelSheetMatcher.CellMatcher.numberCell;
import static ru.yandex.market.tpl.common.excel.matchers.ExcelSheetMatcher.CellMatcher.row;
import static ru.yandex.market.tpl.common.excel.matchers.ExcelSheetMatcher.CellMatcher.rows;
import static ru.yandex.market.tpl.common.excel.matchers.ExcelSheetMatcher.CellMatcher.textCell;

@DbUnitDataSet(schema = "public", before = "OutstaffTimesheetExcelLoaderServiceTest.before.csv")
public class OutstaffTimesheetExcelLoaderServiceTest extends AbstractCoreTest {

    private final static int PERSON_ROW_NUM_START = 12;
    private final static int OPERATION_ROW_NUM_START = 12;
    private final static String LOGIN_CREATED_BY = "robot-test";

    @Autowired
    private OutstaffTimesheetExcelLoaderService sut;

    @Autowired
    private OutstaffTimesheetExcelSerializer serializer;

    @Test
    void shouldEmptyTimesheetWhenNoWorkingData() {
        mockClock(LocalDateTime.of(2022, 1, 17, 11, 0, 0));
        Domain domain = createDomain(1L, "Софьино", ZoneId.of("Europe/Moscow"));
        OutstaffCompanyEntity company = createCompany(1L, "Umbrella");

        OutstaffTimesheetExcel model =
                sut.loadTimesheetModel(domain, company, LocalDate.of(2022, 1, 12), null, LOGIN_CREATED_BY);
        byte[] resultExcel = serializer.serialize(model);

        MatcherAssert.assertThat(resultExcel, buildExcelHeaderMatcher(
                "Umbrella", "Софьино", "с 12.01.2022 по 12.01.2022", 0, "17.01.2022 11:00", LOGIN_CREATED_BY));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutstaffTimesheetExcelLoaderServiceTest.HasWmsLogs.before.csv")
    void shouldTimesheetWithWmsWhenHasWmsLogs() {
        mockClock(LocalDateTime.of(2022, 2, 5, 16, 0, 0));
        Domain domain = createDomain(1L, "Софьино", ZoneId.of("Europe/Moscow"));
        OutstaffCompanyEntity company = createCompany(1L, "Umbrella");

        OutstaffTimesheetExcel model =
                sut.loadTimesheetModel(domain, company, LocalDate.of(2022, 1, 25), null, "robot-test");
        byte[] resultExcel = serializer.serialize(model);

        MatcherAssert.assertThat(resultExcel, buildExcelHeaderMatcher(
                "Umbrella", "Софьино", "с 25.01.2022 по 25.01.2022", 1, "05.02.2022 16:00", LOGIN_CREATED_BY));
        MatcherAssert.assertThat(resultExcel,
                ExcelSheetMatcher.sheetWithCells(
                        0, rows(buildPersonRowMatcher(
                                PERSON_ROW_NUM_START,
                                "эдвард мунк тестович(out)", "14:34", "21:34", "2 смена",
                                "с применением ПРТ", "TESTQUEUE-1", "Нет")
                        )
                )
        );
        MatcherAssert.assertThat(resultExcel,
                ExcelSheetMatcher.sheetWithCells(
                        1, rows(buildOperationRowMatcher(
                                        OPERATION_ROW_NUM_START,
                                        "эдвард мунк тестович", "user-100", "2 смена", "14:34", "14:44",
                                        "Отбор товаров шт", "штуки", 10.
                                )
                        )
                )
        );
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutstaffTimesheetExcelLoaderServiceTest.HasScLogsAndNpo.before.csv")
    void shouldTimesheetWithioutScAndWithNpoWhenHasScLogsAndNpo() {
        mockClock(LocalDateTime.of(2022, 2, 6, 21, 0, 0));
        Domain domain = createDomain(55L, "СЦ МСК Строгино", ZoneId.of("Europe/Moscow"));
        OutstaffCompanyEntity company = createCompany(1L, "Umbrella");

        OutstaffTimesheetExcel model =
                sut.loadTimesheetModel(domain, company, LocalDate.of(2022, 1, 30), null, LOGIN_CREATED_BY);
        byte[] resultExcel = serializer.serialize(model);

        MatcherAssert.assertThat(resultExcel, buildExcelHeaderMatcher(
                "Umbrella", "СЦ МСК Строгино", "с 30.01.2022 по 30.01.2022", 1, "06.02.2022 21:00", LOGIN_CREATED_BY));
        MatcherAssert.assertThat(resultExcel,
                ExcelSheetMatcher.sheetWithCells(
                        0, rows(buildPersonRowMatcher(
                                PERSON_ROW_NUM_START,
                                "Дзержинский Феликс Эдмундович", "15:00", "17:33", "2 смена",
                                "по товарам и заказам, прочим работам", "", "Нет")
                        )
                )
        );
        MatcherAssert.assertThat(resultExcel,
                ExcelSheetMatcher.sheetWithCells(
                        1, rows(buildOperationRowMatcher(
                                        OPERATION_ROW_NUM_START,
                                        "Дзержинский Феликс Эдмундович", "sof-ivanov", "2 смена",
                                        "15:00", "15:15", "Хозяйственные работы", "час", 0.25
                                )
                        )
                )
        );
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "OutstaffTimesheetExcelLoaderServiceTest.HasWmsAndTimexLogs.before.csv")
    void shouldTimesheetWithTimexAndWmsWhenHasTimexLogsAndWms() {
        mockClock(LocalDateTime.of(2022, 2, 6, 21, 0, 0));
        Domain domain = createDomain(1L, "Софьино", ZoneId.of("Europe/Moscow"));
        OutstaffCompanyEntity company = createCompany(1L, "Umbrella");

        OutstaffTimesheetExcel model =
                sut.loadTimesheetModel(domain, company, LocalDate.of(2022, 1, 25), null, LOGIN_CREATED_BY);
        byte[] resultExcel = serializer.serialize(model);

        MatcherAssert.assertThat(resultExcel, buildExcelHeaderMatcher(
                "Umbrella", "Софьино", "с 25.01.2022 по 25.01.2022", 1, "06.02.2022 21:00", LOGIN_CREATED_BY));
        MatcherAssert.assertThat(resultExcel,
                ExcelSheetMatcher.sheetWithCells(
                        0, rows(buildPersonRowMatcher(
                                PERSON_ROW_NUM_START,
                                "эдвард мунк тестович", "14:34", "21:34", "2 смена",
                                "с применением ПРТ", "", "Да")
                        )
                )
        );
        MatcherAssert.assertThat(resultExcel,
                ExcelSheetMatcher.sheetWithCells(
                        1, rows(buildOperationRowMatcher(
                                        OPERATION_ROW_NUM_START,
                                        "эдвард мунк тестович", "user-100", "2 смена",
                                        "12:01", "14:01", "ФФЦ Софьино - операционный зал", "", 0.
                                ), buildOperationRowMatcher(
                                        OPERATION_ROW_NUM_START + 1,
                                        "эдвард мунк тестович", "user-100", "2 смена",
                                        "14:34", "14:44", "Отбор товаров шт", "штуки", 50.
                                )
                        )
                )
        );
    }

    private Domain createDomain(Long id, String name, ZoneId zoneId) {
        return Domain.builder()
                .id(id)
                .name(name)
                .timezone(zoneId)
                .build();
    }

    private OutstaffCompanyEntity createCompany(Long id, String name) {
        return OutstaffCompanyEntity.builder()
                .id(id)
                .name(name)
                .build();
    }

    private ExcelSheetMatcher buildExcelHeaderMatcher(String companyName, String domainName,
                                                      String period, int countPersons,
                                                      String createdAt, String createdBy) {
        return ExcelSheetMatcher.sheetWithCells(
                rows(
                        row(2, textCell(3, CoreMatchers.is(companyName))),
                        row(3, textCell(3, CoreMatchers.is(domainName))),
                        row(4, textCell(3, CoreMatchers.is(period))),
                        row(5, numberCell(3, Matchers.closeTo(countPersons, 0.000_001))),
                        row(6, textCell(3, CoreMatchers.is(createdAt))),
                        row(7, textCell(3, CoreMatchers.is(createdBy)))
                )
        );
    }

    private ExcelSheetMatcher.CellMatcher.Row buildPersonRowMatcher(int rowNum, String fullName, String start,
                                                                    String finish, String shiftType,
                                                                    String serviceType, String tickets,
                                                                    String isBiometry) {
        return ExcelSheetMatcher.CellMatcher.row(rowNum,
                textCell(2, CoreMatchers.is(fullName)), textCell(3, CoreMatchers.is(start)),
                textCell(4, CoreMatchers.is(finish)), textCell(5, CoreMatchers.is(shiftType)),
                textCell(6, CoreMatchers.is(serviceType)), textCell(7, CoreMatchers.is(tickets)),
                textCell(8, CoreMatchers.is(isBiometry))
        );
    }

    private ExcelSheetMatcher.CellMatcher.Row buildOperationRowMatcher(int rowNum, String fullName, String login,
                                                                       String shiftType, String start, String finish,
                                                                       String operationType, String unitType,
                                                                       double units) {
        return ExcelSheetMatcher.CellMatcher.row(rowNum,
                textCell(2, CoreMatchers.is(fullName)), textCell(3, CoreMatchers.is(login)),
                textCell(4, CoreMatchers.is(shiftType)), textCell(5, CoreMatchers.is(start)),
                textCell(6, CoreMatchers.is(finish)), textCell(7, CoreMatchers.is(operationType)),
                textCell(8, CoreMatchers.is(unitType)), numberCell(9, Matchers.closeTo(units, 0.000_001))
        );
    }
}


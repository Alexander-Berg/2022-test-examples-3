package ru.yandex.market.hrms.api.controller.calendar.excel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;

import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;
import ru.yandex.market.tpl.common.excel.matchers.ExcelSheetMatcher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(schema = "public", before = "CalendarControllerExcelTest.before.csv")
public class CalendarControllerExcelTest extends AbstractApiTest {

    private static final double ERROR = 0.01;

    @Disabled
    @Test
    void shouldReturnExcelViewInFile() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 17, 11, 0, 0));

        MvcResult result = mockMvc.perform(get("/lms/calendar/excel")
                        .queryParam("date", "2021-02")
                        .queryParam("groupId", "2")
                        .queryParam("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andReturn();

        File resultFile = new File("result.xlsx");
        try (FileOutputStream fos = new FileOutputStream(resultFile)) {
            IOUtils.copy(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()), fos);
        }
    }

    @Test
    void shouldReturnExcelViewFFC() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 17, 11, 0, 0));

        MvcResult result = mockMvc.perform(get("/lms/calendar/excel")
                        .queryParam("date", "2021-02")
                        .queryParam("groupId", "2")
                        .queryParam("domainId", "1")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''%D0%A2%D0%B0%D0%B1%D0" +
                        "%B5%D0%BB%D1%8C_%D0%9A%D0%BE%D1%80%D0%BD%D0%B5%D0%B2%D0%B0%D1%8F%20%D0%B3%D1%80%D1%83%D0%BF" +
                        "%D0%BF%D0%B0_%D0%9B%D0%B5%D0%B2%D0%B0%D1%8F%20%D0%B3%D1%80%D1%83%D0%BF%D0%BF%D0" +
                        "%B0_20210217_110000.xlsx"))
                .andReturn();

        byte[] response = result.getResponse().getContentAsByteArray();

        MatcherAssert.assertThat(response, ExcelSheetMatcher.sheetWithCells(
                ExcelSheetMatcher.CellMatcher.rows(
                        ExcelSheetMatcher.CellMatcher.row(
                                1,
                                ExcelSheetMatcher.CellMatcher.textCell(1, CoreMatchers.is("Табель"))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                2,
                                ExcelSheetMatcher.CellMatcher.emptyCell(1)
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                3,
                                ExcelSheetMatcher.CellMatcher.textCell(1, CoreMatchers.is("Корневая группа • Левая " +
                                        "группа"))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                4,
                                ExcelSheetMatcher.CellMatcher.emptyCell(1)
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                5,
                                ExcelSheetMatcher.CellMatcher.localDateTimeCell(1,
                                        CoreMatchers.is(LocalDateTime.of(2021, 2, 17, 11, 0, 0)))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                7,
                                ExcelSheetMatcher.CellMatcher.textCell(1, CoreMatchers.is("№ п/п")),
                                ExcelSheetMatcher.CellMatcher.textCell(2, CoreMatchers.is("Сотрудник")),
                                ExcelSheetMatcher.CellMatcher.textCell(3, CoreMatchers.is("Участок")),
                                ExcelSheetMatcher.CellMatcher.textCell(4, CoreMatchers.is("Должность")),
                                ExcelSheetMatcher.CellMatcher.textCell(5, CoreMatchers.is("Логин Staff")),
                                ExcelSheetMatcher.CellMatcher.textCell(6, CoreMatchers.is("Логин WMS")),
                                ExcelSheetMatcher.CellMatcher.textCell(7, CoreMatchers.is("Дата приема"))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                8,
                                ExcelSheetMatcher.CellMatcher.textCell(1, CoreMatchers.is("Корневая группа • Левая " +
                                        "группа"))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                9,
                                ExcelSheetMatcher.CellMatcher.numberCell(1, Matchers.closeTo(1, ERROR)),
                                ExcelSheetMatcher.CellMatcher.textCell(2, CoreMatchers.is("Катя")),
                                ExcelSheetMatcher.CellMatcher.textCell(3, CoreMatchers.is("Корневая группа • Левая " +
                                        "группа")),
                                ExcelSheetMatcher.CellMatcher.textCell(4, CoreMatchers.is("Бригадир")),
                                ExcelSheetMatcher.CellMatcher.textCell(5, CoreMatchers.is("kukabara")),
                                ExcelSheetMatcher.CellMatcher.textCell(6, CoreMatchers.is("")),
                                ExcelSheetMatcher.CellMatcher.textCell(7, CoreMatchers.is("2021-01-01"))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                10,
                                ExcelSheetMatcher.CellMatcher.textCell(1, CoreMatchers.is("Корневая группа • Левая " +
                                        "группа • Второй внук"))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                11,
                                ExcelSheetMatcher.CellMatcher.numberCell(1, Matchers.closeTo(2, ERROR)),
                                ExcelSheetMatcher.CellMatcher.textCell(2, CoreMatchers.is("Тимур")),
                                ExcelSheetMatcher.CellMatcher.textCell(3, CoreMatchers.is("Корневая группа • Левая " +
                                        "группа • Второй внук")),
                                ExcelSheetMatcher.CellMatcher.textCell(4, CoreMatchers.is("Кладовщик")),
                                ExcelSheetMatcher.CellMatcher.textCell(5, CoreMatchers.is("timursha")),
                                ExcelSheetMatcher.CellMatcher.textCell(6, CoreMatchers.is("sof-timursha")),
                                ExcelSheetMatcher.CellMatcher.textCell(7, CoreMatchers.is("2021-01-01"))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                12,
                                ExcelSheetMatcher.CellMatcher.textCell(1, CoreMatchers.is("Корневая группа • Левая " +
                                        "группа • Первый внук"))

                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                13,
                                ExcelSheetMatcher.CellMatcher.numberCell(1, Matchers.closeTo(3, ERROR)),
                                ExcelSheetMatcher.CellMatcher.textCell(2, CoreMatchers.is("Андрей Антипов")),
                                ExcelSheetMatcher.CellMatcher.textCell(3, CoreMatchers.is("Корневая группа • Левая " +
                                        "группа • Первый внук")),
                                ExcelSheetMatcher.CellMatcher.textCell(4, CoreMatchers.is("Кладовщик")),
                                ExcelSheetMatcher.CellMatcher.textCell(5, CoreMatchers.is("antipov93")),
                                ExcelSheetMatcher.CellMatcher.textCell(6, CoreMatchers.is("sof-antipov93")),
                                ExcelSheetMatcher.CellMatcher.textCell(7, CoreMatchers.is("2021-01-01"))
                        )
                )
        ));
    }

    @Test
    void shouldReturnExcelViewSC() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 17, 11, 0, 0));

        MvcResult result = mockMvc.perform(get("/lms/calendar/excel")
                        .queryParam("date", "2021-02")
                        .queryParam("groupId", "8")
                        .queryParam("domainId", "55")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''%D0%A2%D0%B0%D0%B1%D0" +
                        "%B5%D0%BB%D1%8C_%D0%9A%D0%BE%D1%80%D0%BD%D0%B5%D0%B2%D0%BE%D0%B9%20%D0%B2%D0%BD%D1%83%D0" +
                        "%BA_20210217_110000.xlsx"))
                .andReturn();

        byte[] response = result.getResponse().getContentAsByteArray();

        MatcherAssert.assertThat(response, ExcelSheetMatcher.sheetWithCells(
                ExcelSheetMatcher.CellMatcher.rows(
                        ExcelSheetMatcher.CellMatcher.row(
                                1,
                                ExcelSheetMatcher.CellMatcher.textCell(1, CoreMatchers.is("Табель"))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                2,
                                ExcelSheetMatcher.CellMatcher.emptyCell(1)
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                3,
                                ExcelSheetMatcher.CellMatcher.textCell(1, CoreMatchers.is("Корневой внук"))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                4,
                                ExcelSheetMatcher.CellMatcher.emptyCell(1)
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                5,
                                ExcelSheetMatcher.CellMatcher.localDateTimeCell(1,
                                        CoreMatchers.is(LocalDateTime.of(2021, 2, 17, 11, 0, 0)))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                7,
                                ExcelSheetMatcher.CellMatcher.textCell(1, CoreMatchers.is("№ п/п")),
                                ExcelSheetMatcher.CellMatcher.textCell(2, CoreMatchers.is("Сотрудник")),
                                ExcelSheetMatcher.CellMatcher.textCell(3, CoreMatchers.is("Участок")),
                                ExcelSheetMatcher.CellMatcher.textCell(4, CoreMatchers.is("Должность")),
                                ExcelSheetMatcher.CellMatcher.textCell(5, CoreMatchers.is("Логин Staff")),
                                ExcelSheetMatcher.CellMatcher.textCell(6, CoreMatchers.is("Логин SC")),
                                ExcelSheetMatcher.CellMatcher.textCell(7, CoreMatchers.is("Дата приема"))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                8,
                                ExcelSheetMatcher.CellMatcher.textCell(1, CoreMatchers.is("Корневой внук"))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                9,
                                ExcelSheetMatcher.CellMatcher.numberCell(1, Matchers.closeTo(1, ERROR)),
                                ExcelSheetMatcher.CellMatcher.textCell(2, CoreMatchers.is("Бони")),
                                ExcelSheetMatcher.CellMatcher.textCell(3, CoreMatchers.is("Корневой внук")),
                                ExcelSheetMatcher.CellMatcher.textCell(4, CoreMatchers.is("Кладовщик")),
                                ExcelSheetMatcher.CellMatcher.textCell(5, CoreMatchers.is("bongar")),
                                ExcelSheetMatcher.CellMatcher.textCell(6, CoreMatchers.is("bongar@hrms-sc.ru")),
                                ExcelSheetMatcher.CellMatcher.textCell(7, CoreMatchers.is("2021-01-01"))
                        ))));
    }

    @Test
    void shouldReturnExcelViewRW() throws Exception {
        mockClock(LocalDateTime.of(2021, 2, 17, 11, 0, 0));

        MvcResult result = mockMvc.perform(get("/lms/calendar/excel")
                        .queryParam("date", "2021-02")
                        .queryParam("groupId", "9")
                        .queryParam("domainId", "52")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap().get(HrmsPlugin.HrmsRoles.OPERATION_MANAGER).getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''%D0%A2%D0%B0%D0%B1%D0" +
                        "%B5%D0%BB%D1%8C_%D0%9A%D0%BE%D1%80%D0%BD%D0%B5%D0%B2%D0%BE%D0%B9%20%D0%B2%D0%BD%D1%83%D0" +
                        "%BA2_20210217_110000.xlsx"))
                .andReturn();

        byte[] response = result.getResponse().getContentAsByteArray();

        MatcherAssert.assertThat(response, ExcelSheetMatcher.sheetWithCells(
                ExcelSheetMatcher.CellMatcher.rows(
                        ExcelSheetMatcher.CellMatcher.row(
                                1,
                                ExcelSheetMatcher.CellMatcher.textCell(1, CoreMatchers.is("Табель"))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                2,
                                ExcelSheetMatcher.CellMatcher.emptyCell(1)
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                3,
                                ExcelSheetMatcher.CellMatcher.textCell(1, CoreMatchers.is("Корневой внук2"))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                4,
                                ExcelSheetMatcher.CellMatcher.emptyCell(1)
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                5,
                                ExcelSheetMatcher.CellMatcher.localDateTimeCell(1,
                                        CoreMatchers.is(LocalDateTime.of(2021, 2, 17, 11, 0, 0)))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                7,
                                ExcelSheetMatcher.CellMatcher.textCell(1, CoreMatchers.is("№ п/п")),
                                ExcelSheetMatcher.CellMatcher.textCell(2, CoreMatchers.is("Сотрудник")),
                                ExcelSheetMatcher.CellMatcher.textCell(3, CoreMatchers.is("Участок")),
                                ExcelSheetMatcher.CellMatcher.textCell(4, CoreMatchers.is("Должность")),
                                ExcelSheetMatcher.CellMatcher.textCell(5, CoreMatchers.is("Логин Staff")),
                                ExcelSheetMatcher.CellMatcher.textCell(6, CoreMatchers.is("Логин SC")),
                                ExcelSheetMatcher.CellMatcher.textCell(7, CoreMatchers.is("Логин WMS")),
                                ExcelSheetMatcher.CellMatcher.textCell(8, CoreMatchers.is("Дата приема"))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                8,
                                ExcelSheetMatcher.CellMatcher.textCell(1, CoreMatchers.is("Корневой внук2"))
                        ),
                        ExcelSheetMatcher.CellMatcher.row(
                                9,
                                ExcelSheetMatcher.CellMatcher.numberCell(1, Matchers.closeTo(1, ERROR)),
                                ExcelSheetMatcher.CellMatcher.textCell(2, CoreMatchers.is("Ласло")),
                                ExcelSheetMatcher.CellMatcher.textCell(3, CoreMatchers.is("Корневой внук2")),
                                ExcelSheetMatcher.CellMatcher.textCell(4, CoreMatchers.is("Кладовщик")),
                                ExcelSheetMatcher.CellMatcher.textCell(5, CoreMatchers.is("bongarik")),
                                ExcelSheetMatcher.CellMatcher.textCell(6, CoreMatchers.is("bongarick@hrms-sc.ru")),
                                ExcelSheetMatcher.CellMatcher.textCell(7, CoreMatchers.is("sof-bongarick")),
                                ExcelSheetMatcher.CellMatcher.textCell(8, CoreMatchers.is("2021-01-01"))
                        ))));
    }
}

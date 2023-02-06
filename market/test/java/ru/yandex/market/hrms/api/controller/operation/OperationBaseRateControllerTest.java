package ru.yandex.market.hrms.api.controller.operation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.api.facade.operation.OperationFacade;
import ru.yandex.market.hrms.api.util.ExcelContentExtractUtil;
import ru.yandex.market.hrms.core.domain.operation.EmployeeGroup;
import ru.yandex.market.hrms.core.domain.operation.rates.OperationBaseRateXlsxContentExtractResult;
import ru.yandex.market.hrms.core.domain.operation.rates.OperationBaseRateXlsxContentRow;
import ru.yandex.market.hrms.model.operation.OperationBaseRateUnitView;
import ru.yandex.market.hrms.model.operation.OperationBaseRateView;
import ru.yandex.market.hrms.model.outstaff.BaseRateOutstaffCompanyView;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "OperationBaseRateControllerTest.before.csv")
public class OperationBaseRateControllerTest extends AbstractApiTest {

    @Autowired
    private OperationFacade operationFacade;

    @Test
    public void shouldReturnBaseRatesForStaff() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/base-rate")
                        .queryParam("group", EmployeeGroup.STAFF.toString())
                        .queryParam("dateFrom", "2021-07-24")
                        .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("OperationBaseRateControllerTestGetRates.json")));
    }

    @Test
    public void shouldReturnBaseRatesXlsxForStaff() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/base-rate/download")
                        .queryParam("group", EmployeeGroup.STAFF.toString())
                        .queryParam("dateFrom", "2021-07-24")
                        .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk())
                .andDo(result -> {
                    var sheetBytes = result.getResponse().getContentAsByteArray();
                    var file = new MockMultipartFile(
                            "file",
                            "excel.xls",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            sheetBytes);

                    var content = ExcelContentExtractUtil
                            .getSheetContentRows(file, EmployeeGroup.STAFF, List.of("1", "2", "3"));

                    assertThat(content.rows().size(), is(1));
                    var row = content.rows().get(0);
                    assertThat(row, hasProperty("unit", is("штуки")));
                    assertThat(row, hasProperty("operationGroup", is("Консолидация")));
                    assertThat(row, hasProperty("companyPrices",
                            is(Map.of(EmployeeGroup.STAFF.toString(), 50.0))));
                });
    }

    @Test
    @DbUnitDataSet(after = "OperationBaseRateControllerTest.after.csv")
    public void shouldUploadXlsxForStaff() throws Exception {
        var file = new MockMultipartFile(
                "file",
                "excel.xls",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                loadFileAsBytes("OperationBaseRateControllerTest.xls")
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/lms/base-rate/upload")
                        .file(file)
                        .queryParam("group", "STAFF")
                        .queryParam("dateFrom", "2021-07-25")
                        .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk());

        OperationBaseRateView tableView = operationFacade
                .getRatesForDomain(1L, EmployeeGroup.STAFF, LocalDate.of(2021, 7, 25));

        assertThat(tableView, hasProperty("warehouseName", is("Софьино")));
        assertThat(tableView.getGroups().size(), is(1));
        assertThat(tableView.getGroups().get(0).getRates().get(0).getUnit(), is("штуки"));
        assertThat(tableView.getGroups().get(0), hasProperty("groupName", is("Консолидация")));
        assertThat(tableView.getGroups().get(0).getRates().get(0).getPrices().get(EmployeeGroup.STAFF.toString()),
                is(new BigDecimal("100.33")));
    }

    @Test
    public void shouldFailedWithWrongUnitsUploadXlsxForStaff() throws Exception {
        var file = new MockMultipartFile(
                "file",
                "excel.xls",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                loadFileAsBytes("OperationBaseRateWrongMeasureControllerTest.xls")
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/lms/base-rate/upload")
                        .file(file)
                        .queryParam("group", "STAFF")
                        .queryParam("dateFrom", "2022-07-25")
                        .cookie(new Cookie("yandex_login", "alex-fill")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldReturnBaseRatesForOutstaff() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/base-rate")
                        .queryParam("group", EmployeeGroup.OUTSTAFF.toString())
                        .queryParam("dateFrom", "2021-07-24")
                        .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        loadFromFile("OperationBaseRateControllerTestGetRatesForOutstaff.json")));
    }

    @Test
    public void shouldReturnBaseRatesXlsxForOutstaff() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/base-rate/download")
                        .queryParam("group", EmployeeGroup.OUTSTAFF.toString())
                        .queryParam("dateFrom", "2021-07-24")
                        .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk())
                .andDo(result -> {
                    byte[] sheetBytes = result.getResponse().getContentAsByteArray();
                    MockMultipartFile file = new MockMultipartFile(
                            "file",
                            "excel.xls",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            sheetBytes);

                    OperationBaseRateXlsxContentExtractResult sheetContent = ExcelContentExtractUtil
                            .getSheetContentRows(file, EmployeeGroup.OUTSTAFF, List.of("1", "2", "3"));
                    List<OperationBaseRateXlsxContentRow> rows = sheetContent.rows();

                    assertThat(rows.size(), is(1));
                    OperationBaseRateXlsxContentRow row = rows.get(0);
                    assertThat(row, hasProperty("unit", is("штуки")));
                    assertThat(row, hasProperty("operationGroup", is("АутстаффКонсолидация")));
                    assertThat(row, hasProperty("companyPrices", is(Map.of("даглогистика", 50.0))));
                });
    }

    @Test
    @DbUnitDataSet(after = "ShouldUploadXlsxForOutstaff.after.csv")
    public void shouldUploadXlsxForOutstaff() throws Exception {
        var file = new MockMultipartFile(
                "file",
                "excel.xls",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                loadFileAsBytes("OperationBaseRateControllerTestOutstaff.xls")
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/lms/base-rate/upload")
                        .file(file)
                        .queryParam("group", "OUTSTAFF")
                        .queryParam("dateFrom", "2021-07-25")
                        .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk());

        OperationBaseRateView tableView = operationFacade
                .getRatesForDomain(1L, EmployeeGroup.OUTSTAFF, LocalDate.of(2021, 7, 25));

        assertThat(tableView, hasProperty("warehouseName", is("Софьино")));
        var groups = tableView.getGroups();
        assertThat(groups.size(), is(1));
        assertThat(groups.get(0).getGroupName(), is("АутстаффКонсолидация"));
        assertThat(groups.get(0).getRates(), is(List.of(new OperationBaseRateUnitView("штуки", Map.of(
                "148", new BigDecimal(100).setScale(1, RoundingMode.HALF_UP),
                "149", new BigDecimal(300).setScale(1, RoundingMode.HALF_UP),
                "150", new BigDecimal(200).setScale(1, RoundingMode.HALF_UP))))));

        var outstaffCompanyMap = tableView
                .getOutstaffCompanies()
                .stream()
                .collect(Collectors.toMap(BaseRateOutstaffCompanyView::getId, Function.identity()));

        assertThat(outstaffCompanyMap.get("148").getName(), is("даглогистика"));
        assertThat(outstaffCompanyMap.get("149").getName(), is("Ещё какая-то логистика"));
        assertThat(outstaffCompanyMap.get("150").getName(), is("Яндекс.Логистика"));
    }

    @Test
    @DbUnitDataSet(after = "ShouldUploadXlsxForAllOutstaff.after.csv")
    public void shouldUploadXlsxForAllOutstaff() throws Exception {
        var file = new MockMultipartFile(
                "file",
                "excel.xls",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                loadFileAsBytes("OperationBaseRateControllerTestForAllOutstaff.xls")
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/lms/base-rate/upload")
                        .file(file)
                        .queryParam("group", "OUTSTAFF")
                        .queryParam("dateFrom", "2021-07-25")
                        .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk());
    }
}

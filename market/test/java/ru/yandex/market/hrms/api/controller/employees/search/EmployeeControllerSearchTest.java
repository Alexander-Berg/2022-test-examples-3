package ru.yandex.market.hrms.api.controller.employees.search;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EmployeeControllerSearchTest extends AbstractApiTest {

    @BeforeEach
    public void beforeEach() {
        mockClock(LocalDateTime.of(2021, 2, 1, 14, 4, 4));
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "EmployeeControllerSearchTest.before.csv")
    @CsvSource({
            "Оксана Сновалкина,null,oksana_only.json",
            "оксана сновалкина,null,oksana_only.json",
            "оКСана сноваЛКина,null,oksana_only.json",
            "Снов,null,oksana_only.json",
            "Окса,null,oksana_only.json",
            "okssnovalkina,null,oksana_only.json",
            "oks,null,oksana_only.json",
            "Але,null,alena_alexey.json",
            "yudi,null,alena_yudina.json",
            "oks,1,oksana_only.json",
            "oks,3,empty.json",
            "oks,5,oksana_only.json",
            "oks,7,empty.json",
            "oks-_snovalkina,null,empty.json",
            "sof-oksn,null,oksana_only.json",
            "sof-oksnisnova,1,oksana_only.json",
            "sof-oksnaaa,1,empty.json"
    })
    void shouldSearchByExactMatch(String query, String group, String fileName) throws Exception {
        MockHttpServletRequestBuilder builder = get("/lms/employees")
                .param("name", query);
        if (!"null".equals(group)) {
            builder.param("groupId", group);
        }
        mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile(fileName), true));
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "EmployeeControllerSearchTest.before.csv")
    @CsvSource({"alen, ,'[\"sof-alen-kulag\",\"sof-alena-yudi\"]'",
            "ale, ,'[\"sof-aleivgorbu\",\"sof-alen-kulag\",\"sof-alena-yudi\",\"sof-anaalaleks\"]'",
            "ale, 2021-01-20 ,'[\"sof-alegorshko\"]'"
    })
    void shouldSuggestWmsLogins(String wmsLoginQuery, LocalDate checkDate, String expectedResult) throws Exception {
        MockHttpServletRequestBuilder builder = get("/lms/employees/suggest")
                .param("wmsLoginQuery", wmsLoginQuery);
        if (checkDate != null) {
            builder.param("checkDate", checkDate.toString());
        }
        mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andExpect(content().string(expectedResult));
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "EmployeeControllerSearchTest.before.csv")
    @CsvSource({"vas,1,'[\"sof-vasilevk\"]'",
            "vas,2,'[\"sof-vasserman\"]'"
    })
    void shouldSuggestOutStaffWmsLogins(String wmsLoginQuery, String domainId, String expectedResult) throws Exception {
        mockClock(LocalDateTime.of(2021, 8, 1, 14, 4, 4));
        MockHttpServletRequestBuilder builder = get("/lms/employees/suggest")
                .param("wmsLoginQuery", wmsLoginQuery)
                .param("domainId", domainId);

        mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andExpect(content().string(expectedResult));
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "EmployeeControllerSearchTestSc.before.csv")
    @CsvSource({"alen, ,1,'[\"sof-alen-kulag@hrms-sc.ru\",\"sof-alena-yudi@hrms-sc.ru\"]'",
            "ale, ,1,'[\"sof-alen-kulag@hrms-sc.ru\",\"sof-alegorshko@hrms-sc.ru\",\"sof-aleivgorbu@hrms-sc.ru\"," +
                    "\"sof-alena-yudi@hrms-sc.ru\",\"sof-anaalaleks@hrms-sc.ru\"]'",
            "ale, 2021-01-20 ,1,'[\"sof-anaalaleks@hrms-sc.ru\"]'",
            "vas, ,50, '[\"trn-vasilev-k@hrms-sc.ru\",\"trn-vasserman-iv@hrms-sc.ru\"]'"
    })
    void shouldSuggestYandexLogins(String yandexLoginQuery,
                                   LocalDate checkDate,
                                   String domainId,
                                   String expectedResult) throws Exception {
        MockHttpServletRequestBuilder builder = get("/lms/employees/suggest")
                .param("wmsLoginQuery", yandexLoginQuery)
                .param("domainId", domainId);
        if (checkDate != null) {
            builder.param("checkDate", checkDate.toString());
        }
        mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andExpect(content().string(expectedResult));
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "EmployeeControllerSearchTest.before.csv")
    @CsvSource({
            "svs-s,1,svs-ser.json",
            "sof-marsash,1,svs-ser.json",
            "Марина Шарико,1,svs-ser.json",
            "ale,1,ale.json",
            "outstaff,1,outstaff.json"
    })
    void shouldSuggestLogins(String loginQuery,
                             String domainId,
                             String fileName) throws Exception {
        MockHttpServletRequestBuilder builder = get("/lms/employees/suggest-new")
                .param("loginQuery", loginQuery)
                .param("domainId", domainId);

        mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile(fileName), false));
    }
}

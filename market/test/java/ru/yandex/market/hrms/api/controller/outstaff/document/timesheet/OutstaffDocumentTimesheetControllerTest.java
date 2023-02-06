package ru.yandex.market.hrms.api.controller.outstaff.document.timesheet;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.servlet.http.Cookie;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.service.s3.S3Service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(schema = "public", before = "OutstaffDocumentTimesheetControllerTest.before.csv")
public class OutstaffDocumentTimesheetControllerTest extends AbstractApiTest {

    @MockBean
    private S3Service s3Service;

    @Test
    public void shouldErrorWhenDomainIsNotExists() throws Exception {
        mockMvc.perform(get("/lms/outstaff/documents/timesheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("domainId", "777")
                )
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldReturnEmptyPageWhenCompanyIsNotExists() throws Exception {
        mockMvc.perform(get("/lms/outstaff/documents/timesheets")
                .contentType(MediaType.APPLICATION_JSON)
                .param("domainId", "1")
                .param("companyIds", "777")
        )
        .andExpect(status().isOk())
        .andExpect(content().json(loadFromFile("search_empty_response.json")));
    }

    @Test
    public void shouldReturnPageWhenFiltersAreEmpty() throws Exception {
        mockMvc.perform(get("/lms/outstaff/documents/timesheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("domainId", "1")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("search_filters_are_empty.json"), true));
    }

    @Test
    public void shouldReturnPageWhenFiltersAreExists() throws Exception {
        mockMvc.perform(get("/lms/outstaff/documents/timesheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("domainId", "2")
                        .param("companyId", "1")
                        .param("shiftTypes", "FIRST_SHIFT")
                        .param("dateFrom", "2022-01-01")
                        .param("dateTo", "2022-01-03")
                        .param("statuses", "NOT_APPROVED")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("search_filters_are_exist.json")));
    }

    @Test
    public void shouldReturnPageWhenShiftTypeIsNotActual() throws Exception {
        mockMvc.perform(get("/lms/outstaff/documents/timesheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("domainId", "2")
                        .param("companyId", "1")
                        .param("shiftTypes", "THIRD_SHIFT")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("search_shift_type_is_not_actual.json")));
    }

    @Test
    public void shouldReturnPageWhenFiltersAreMultiple() throws Exception {
        mockMvc.perform(get("/lms/outstaff/documents/timesheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("domainId", "2")
                        .param("companyId", "1", "2")
                        .param("shiftTypes", "FIRST_SHIFT", "SECOND_SHIFT", "THIRD_SHIFT")
                        .param("dateFrom", "2022-01-01")
                        .param("dateTo", "2022-01-03")
                        .param("statuses", "NOT_APPROVED", "SENT")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("search_filters_are_multiple.json"), true));
    }

    @Test
    public void shouldApproveTimesheetWhenNotApproved() throws Exception {
        mockClock(LocalDateTime.of(2022, 3, 4, 12, 12, 14));

        mockMvc.perform(post("/lms/outstaff/documents/timesheets/{id}/approval", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("domainId", "1")
                        .cookie(new Cookie("yandex_login", "donald_duck"))
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("approve_timesheet.json"), true));
    }

    @Test
    public void shouldNotApproveTimesheetWhenHasAlreadyApproved() throws Exception {
        mockMvc.perform(post("/lms/outstaff/documents/timesheets/{id}/approval", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("domainId", "1")
                )
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value("ЛУВР уже подтвержден"));
    }

    @Test
    public void shouldNotApproveTimesheetWhenNotExistTimesheet() throws Exception {
        mockMvc.perform(post("/lms/outstaff/documents/timesheets/{id}/approval", "777")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("domainId", "1")
                )
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value(Matchers.containsStringIgnoringCase("entity not found")));
    }

    @Test
    public void shouldRecreateTimesheetWhenExistTimesheet() throws Exception {
        mockClock(Instant.parse("2022-06-01T09:30:00Z"));

        mockMvc.perform(post("/lms/outstaff/documents/timesheets/{id}/recreation", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie("yandex_login", "darkwing_duck"))
                        .param("domainId", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").value(Matchers.equalTo("2022-06-01T12:30:00+03:00")));

        verify(s3Service, times(1)).putObject(any(), any(), any());
    }

    @Test
    public void shouldNotRecreateTimesheetWhenNotExistTimesheet() throws Exception {
        mockMvc.perform(post("/lms/outstaff/documents/timesheets/{id}/recreation", "777")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("domainId", "1")
                )
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value(Matchers.containsStringIgnoringCase("entity not found")));
    }

    @Test
    public void shouldNotRecreateTimesheetWhenTimesheetWasApproved() throws Exception {
        mockClock(Instant.parse("2021-06-01T09:30:00Z"));

        mockMvc.perform(post("/lms/outstaff/documents/timesheets/{id}/recreation", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("domainId", "1")
                )
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value(Matchers.containsStringIgnoringCase("Невозможно переформировать ЛУВР")));
    }

    @Test
    public void shouldDownloadTimesheetWhenFileExists() throws Exception {
        when(s3Service.getObject(any(), any())).thenReturn(Optional.of(new byte[1]));

        mockMvc.perform(get("/lms/outstaff/documents/timesheets/{timesheetId}/data", "6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("domainId", "2")
                )
                .andExpect(status().isOk());
    }

    @Test
    public void shouldNotDownloadTimesheetWhenFileNotExists() throws Exception {
        mockMvc.perform(get("/lms/outstaff/documents/timesheets/{timesheetId}/data", "6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("domainId", "2")
                )
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value(Matchers.containsStringIgnoringCase("Не найден файл ЛУВРа в хранилище")));
    }

    @Test
    public void shouldNotDownloadTimesheetWhenLinkNotExists() throws Exception {
        mockMvc.perform(get("/lms/outstaff/documents/timesheets/{timesheetId}/data", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("domainId", "1")
                )
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value(Matchers.containsStringIgnoringCase("Ссылка на файл в хранилище не найдена")));
    }
}

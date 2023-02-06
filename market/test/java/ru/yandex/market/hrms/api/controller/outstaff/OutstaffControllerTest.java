package ru.yandex.market.hrms.api.controller.outstaff;

import java.time.LocalDate;

import javax.servlet.http.Cookie;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.domain.outstaff.repo.OutstaffFactRepo;
import ru.yandex.market.hrms.model.overtime.ShiftType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(schema = "public", before = "OutstaffControllerTest.before.csv")
public class OutstaffControllerTest extends AbstractApiTest {

    @Autowired
    private OutstaffFactRepo outstaffFactRepo;

    @Test
    void shouldReturn400WhenNoRequiredDataSpecified() throws Exception {
        mockMvc.perform(post("/lms/outstaff")
                .cookie(new Cookie("yandex_login", "magomedovgh"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {
                         }
                         """)
        ).andExpect(status().is4xxClientError());
    }

    @Test
    void shouldReturn400WhenQuantityIsNegative() throws Exception {
        mockMvc.perform(post("/lms/outstaff")
                .cookie(new Cookie("yandex_login", "magomedovgh"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {
                           "domainId": 1,
                           "shiftType": "DAY",
                           "date": "2021-02-01",
                           "quantity": -1
                         }
                         """)
        ).andExpect(status().is4xxClientError());
    }

    @Test
    void shouldReturn400WhenRecordAlreadyExists() throws Exception {
        mockMvc.perform(post("/lms/outstaff")
                .cookie(new Cookie("yandex_login", "magomedovgh"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {
                           "domainId": 1,
                           "shiftType": "DAY",
                           "date": "2021-02-03",
                           "quantity": 100
                         }
                         """)
        ).andExpect(status().is4xxClientError());
    }

    @Test
    void shouldSaveAllFields() throws Exception {
        mockMvc.perform(post("/lms/outstaff")
                .cookie(new Cookie("yandex_login", "magomedovgh"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                         {
                           "domainId": 1,
                           "shiftType": "DAY",
                           "date": "2021-02-09",
                           "quantity": 100
                         }
                         """)
        ).andExpect(status().isOk());

        var insertedData = outstaffFactRepo.findAll()
                .stream()
                .filter(x -> x.getDate().equals(LocalDate.of(2021, 2, 1)) &&
                        x.getShiftType() == ShiftType.DAY &&
                        x.getDomain().getId() == 1 &&
                        x.getQuantity() == 100);

        MatcherAssert.assertThat(insertedData, Matchers.notNullValue());
    }

    @Test
    void shouldReturnPage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff")
                .cookie(new Cookie("yandex_login", "magomedovgh"))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("OutstaffControllerTestPage.json")));
    }

    @Test
    void shouldReturnCard() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff/1")
                .cookie(new Cookie("yandex_login", "magomedovgh"))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("OutstaffControllerTestCard.json")));
    }

    @Test
    void shouldDelete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/lms/outstaff/1")
                .cookie(new Cookie("yandex_login", "magomedovgh"))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());

        var deleted = outstaffFactRepo.findById(1L).orElse(null);
        MatcherAssert.assertThat(deleted, Matchers.hasProperty("deletedAt", Matchers.notNullValue()));
    }

    @Test
    void searchWmsOutstaffs() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff/search")
                .queryParam("query", "сал")
                .queryParam("domainId", "1")
                        .cookie(new Cookie("yandex_login","123"))
                .header("X-Admin-Roles", "ROLE_OPERATION_MANAGER")
        )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("search_outstaffs.json"), true));
    }

    @Test
    void searchScOutstaffs() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/outstaff/search")
                        .queryParam("query", "дзер")
                        .queryParam("domainId", "1")
                        .cookie(new Cookie("yandex_login","123"))
                        .header("X-Admin-Roles", "ROLE_OPERATION_MANAGER")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("search_sc_outstaffs.json"), true));
    }

}

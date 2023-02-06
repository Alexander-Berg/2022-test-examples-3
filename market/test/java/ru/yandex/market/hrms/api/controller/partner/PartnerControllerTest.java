package ru.yandex.market.hrms.api.controller.partner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.service.timex.TimexApiFacadeNew;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(schema = "public", before = "PartnerControllerTest.before.csv")
public class PartnerControllerTest extends AbstractApiTest {

    @MockBean
    TimexApiFacadeNew timexApiFacadeNew;

    @Test
    void shouldLoadAllByPartnerIdForAdmin() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=0&size=10&partnerId=326611127")
                        .cookie(new Cookie("yandex_login", "robot-out-hrms"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("robot-out-hrms")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_PAGE_PRODUCTION_CALENDAR_SEE_ALL))))
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("loadByPartnerId.json")));
    }

    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.calendarData.before.csv")
    void shouldLoadOnlyActiveOuts() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=0&size=10&partnerId=326611127" +
                                "&isActive=true")
                        .cookie(new Cookie("yandex_login", "robot-out-hrms"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("robot-out-hrms")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_PAGE_PRODUCTION_CALENDAR_SEE_ALL))))
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("loadActiveByPartnerId.json")));
    }

    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.calendarData.before.csv")
    void shouldLoadOnlyInactiveOuts() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=0&size=10&partnerId=326611127" +
                                "&isActive=false")
                        .cookie(new Cookie("yandex_login", "robot-out-hrms"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("robot-out-hrms")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_PAGE_PRODUCTION_CALENDAR_SEE_ALL))))
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("loadInactiveByPartnerId.json")));
    }

    @Test
    void shouldLoadAllByPartnerId() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=0&size=10&partnerId=326611127")
                        .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("loadByPartnerId.json")));
    }

    @Test
    void tryLoadWithPartnerIdZero() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=0&size=10&partnerId=0")
                        .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(content().json("""
                        {"pageInfo":{"page":0,"totalPage":0,"totalItems":0},"content":[]}
                        """));
    }

    @Test
    void tryLoadWithoutPartnerId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=0&size=10")
                        .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(content().json("""
                        {"pageInfo":{"page":0,"totalPage":0,"totalItems":0},"content":[]}
                        """));
    }

    @Test
    void shouldLoadAllByDomainId() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=0&size=10&partnerId=326611128" +
                                "&areaDomainId=2")
                        .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("loadByDomainId.json")));
    }

    @Test
    void shouldLoadAllByName() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=0&size=10&partnerId=326611128" +
                                "&name=еВиЧ")
                        .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("loadByName.json")));
    }

    @Test
    void shouldLoadAllByPosition() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=0&size=10&partnerId=326611127" +
                                "&position=Оператор ПРТ")
                        .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("loadByPosition.json")));
    }

    @Test
    void shouldLoadAllByDocsReadinessFull() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=0&size=10&partnerId=326611127" +
                                "&docsReadiness=FULL")
                        .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("loadByDocsReadinessFull.json")));
    }

    @Test
    void shouldLoadAllByDocsReadinessPartial() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=0&size=10&partnerId=326611127" +
                                "&docsReadiness=PARTIAL")
                        .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("loadByDocsReadinessPartial.json")));
    }

    @Test
    void shouldLoadAllByDocsBlockStatusActive() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=0&size=10&partnerId=326611129" +
                                "&blockStatus=ACTIVE")
                        .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("loadByBlockStatusActive.json")));
    }

    @Test
    void shouldLoadAllByDocsBlockStatusBlocked() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=0&size=10&partnerId=326611129" +
                                "&blockStatus=BLOCKED")
                        .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("loadByBlockStatusBlocked.json")));
    }

    @Test
    void shouldLoadFirstPage() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=0&size=3&partnerId=326611127")
                        .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("loadFirstPage.json")));
    }

    @Test
    void shouldLoadSecondPage() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=1&size=1&partnerId=326611127")
                        .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("loadThirdPage.json")));
    }

    @Test
    @DbUnitDataSet(after = "PartnerControllerTest.blockedUser.after.csv")
    void shouldBlockPerson() throws Exception {
        mockClock(LocalDateTime.parse("2022-03-30T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        doNothing().when(timexApiFacadeNew).removeEmployee(any());

        mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person/deactivate")
                        .content("""
                                  [
                                        {
                                            "outstaffId": "8",
                                            "areaDomainId" : "2",
                                            "deactivationReason": "INCOMPLETE_SET_OF_DOCUMENTS"
                                        },
                                        {
                                            "outstaffId": "7",
                                            "areaDomainId" : "1",
                                            "deactivationReason": "INCOMPLETE_SET_OF_DOCUMENTS"
                                        },
                                         {
                                            "outstaffId": "0",
                                            "areaDomainId" : "0",
                                            "deactivationReason": "INCOMPLETE_SET_OF_DOCUMENTS"
                                        },
                                         {
                                            "outstaffId": "8",
                                            "areaDomainId" : "1",
                                            "deactivationReason": "INCOMPLETE_SET_OF_DOCUMENTS"
                                        }
                                  ]
                                """)
                        .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("blockPersonIncompleteSetOfDocs.json")));
    }

    @Test
    void blockException() throws Exception {
        mockClock(LocalDateTime.parse("2022-03-30T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person/deactivate")
                .content("""
                          [
                                {
                                    "outstaffId": "8",
                                    "areaDomainId" : "2",
                                    "deactivationReason": "INCOMPLETE_SET_OF_DOCUMENTS"
                                },
                                {
                                    "outstaffId": "7",
                                    "areaDomainId" : "1",
                                    "deactivationReason": "INCOMPLETE_SET_OF_DOCUMENTS"
                                },
                                 {
                                    "outstaffId": "888",
                                    "areaDomainId" : "0",
                                    "deactivationReason": "INCOMPLETE_SET_OF_DOCUMENTS"
                                },
                                 {
                                    "outstaffId": "8",
                                    "areaDomainId" : "1",
                                    "deactivationReason": "INCOMPLETE_SET_OF_DOCUMENTS"
                                }
                          ]
                        """)
                .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(before = "PartnerControllerTest.NewOutPage.before.csv",
            after = "PartnerControllerTest.blockedUser.after.csv")
    void shouldBlockPersonWithOutstaffV2() throws Exception {
        mockClock(LocalDateTime.parse("2022-03-30T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person/deactivate")
                        .content("""
                                  [
                                        {
                                            "outstaffId": "2",
                                            "areaDomainId" : "2",
                                            "deactivationReason": "INCOMPLETE_SET_OF_DOCUMENTS"
                                        },
                                        {
                                            "outstaffId": "1",
                                            "areaDomainId" : "1",
                                            "deactivationReason": "INCOMPLETE_SET_OF_DOCUMENTS"
                                        },
                                         {
                                            "outstaffId": "0",
                                            "areaDomainId" : "0",
                                            "deactivationReason": "INCOMPLETE_SET_OF_DOCUMENTS"
                                        },
                                         {
                                            "outstaffId": "2",
                                            "areaDomainId" : "1",
                                            "deactivationReason": "INCOMPLETE_SET_OF_DOCUMENTS"
                                        }
                                  ]
                                """)
                        .cookie(new Cookie("yandex_login", "ppv-vldmr"))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("blockPersonIncompleteSetOfDocs.json")));
    }
}

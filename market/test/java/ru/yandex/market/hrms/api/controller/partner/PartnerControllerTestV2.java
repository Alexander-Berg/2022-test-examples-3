package ru.yandex.market.hrms.api.controller.partner;

import java.time.LocalDate;
import java.util.List;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(schema = "public", before = "PartnerControllerTestV2.OutstaffV2.before.csv")
public class PartnerControllerTestV2 extends AbstractApiTest {

    @Test
    @DbUnitDataSet(schema = "public", before = "PartnerControllerTestV2.Environment.before.csv")
    void shouldLoadAllByPartnerIdForAdminFromOutstaffV2() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person?page=0&size=10&partnerId=326611129")
                        .cookie(new Cookie("yandex_login", "robot-out-hrms"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("robot-out-hrms")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_PAGE_PRODUCTION_CALENDAR_SEE_ALL))))
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("loadAllOutstaffV2.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "PartnerControllerTestV2.Environment.before.csv")
    void shouldLoadAllByFilterAdminFromOutstaffV2Wms() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person")
                        .param("page", "0")
                        .param("size", "10")
                        .param("partnerId", "326611129")
                        .param("areaDomainId", "1")
                        .param("position", "клад")
                        .param("name", "pod-zero2")
                        .param("docsReadiness", "FULL")
                        .param("isActive", "false")
                        .param("blockStatus", "ACTIVE")
                        .cookie(new Cookie("yandex_login", "robot-out-hrms"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("robot-out-hrms")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_PAGE_PRODUCTION_CALENDAR_SEE_ALL))))
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("findByFilter.json")));
    }


    @Test
    @DbUnitDataSet(schema = "public", before = "PartnerControllerTestV2.Environment.before.csv")
    void shouldLoadAllByFilterAdminFromOutstaffV2Sc() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/person")
                        .param("page", "0")
                        .param("size", "10")
                        .param("partnerId", "326611129")
                        .param("areaDomainId", "1")
                        .param("position", "клад")
                        .param("name", "first")
                        .param("docsReadiness", "FULL")
                        .param("isActive", "false")
                        .param("blockStatus", "ACTIVE")
                        .cookie(new Cookie("yandex_login", "robot-out-hrms"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("robot-out-hrms")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_PAGE_PRODUCTION_CALENDAR_SEE_ALL))))
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("findByFilter.json")));
    }

    @Test
    @DbUnitDataSet(schema = "public", before = "PartnerControllerTestV2.Environment.before.csv")
    void loadSingleOutstaff() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/outstaff/3")
                        .cookie(new Cookie("yandex_login", "robot-out-hrms"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("robot-out-hrms")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_PAGE_PRODUCTION_CALENDAR_SEE_ALL))))
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("soloOut.json")));
    }

    @Test
    void loadSingleOldOutstaff() throws Exception {
        mockClock(LocalDate.of(2022, 4, 5));
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/external/partner/outstaff/9")
                        .cookie(new Cookie("yandex_login", "robot-out-hrms"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("robot-out-hrms")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_PAGE_PRODUCTION_CALENDAR_SEE_ALL))))
                ).andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("soloOldOut.json")));
    }
}

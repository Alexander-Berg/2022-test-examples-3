package ru.yandex.market.hrms.api.controller.partner.create;

import java.time.LocalDateTime;
import java.util.List;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled
@DbUnitDataSet(schema = "public")
public class CreateNewOutstaffPersonTest extends AbstractApiTest {

    @BeforeEach
    void prepare() {
        mockClock(LocalDateTime.of(2022, 8, 3, 11, 42, 56));
    }

    @Test
    @DbUnitDataSet(
            before = "CreateNewOutstaffPerson.onlyOldScheme.before.csv",
            after = "CreateNewOutstaffPerson.onlyOldScheme.onlySC.after.csv")
    @DisplayName(value = "Бригадир на СЦ: минимальный набор документов. Только старая архитектура")
    void createBrigadierInSCOldScheme() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person")
                .content(loadFromFile("brigadierForSCdata.json"))
                .cookie(new Cookie("yandex_login", "robot-out-hrms"))
                .contentType(MediaType.APPLICATION_JSON)
                .with(SecurityMockMvcRequestPostProcessors.user("robot-out-hrms")
                        .authorities(List.of(
                                new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_EXTERNAL_PARTNER_PERSON_EDIT)))))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
            before = "CreateNewOutstaffPerson.onlyOldScheme.before.csv",
            after = "CreateNewOutstaffPerson.onlyOldScheme.onlyFFC.after.csv")
    @DisplayName(value = "Оператор на ФФЦ (не РФ): максимальный набор документов. Только старая архитектура")
    void createOperatorInFFCOldScheme() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person")
                .content(loadFromFile("operatorForFFCdata.json"))
                .cookie(new Cookie("yandex_login", "ne-robot-out-hrms"))
                .contentType(MediaType.APPLICATION_JSON)
                .with(SecurityMockMvcRequestPostProcessors.user("ne-robot-out-hrms")
                        .authorities(List.of(
                                new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_EXTERNAL_PARTNER_PERSON_EDIT)))))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
            before = "CreateNewOutstaffPerson.onlyNewScheme.before.csv",
            after = "CreateNewOutstaffPerson.onlyNewScheme.onlySC.after.csv")
    @DisplayName(value = "Бригадир на СЦ: минимальный набор документов. Только новая архитектура")
    void createBrigadierInSCNewScheme() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person")
                    .content(loadFromFile("brigadierForSCdata.json"))
                    .cookie(new Cookie("yandex_login", "robot-out-hrms"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(SecurityMockMvcRequestPostProcessors.user("robot-out-hrms")
                            .authorities(List.of(
                                    new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_EXTERNAL_PARTNER_PERSON_EDIT)))))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
            before = "CreateNewOutstaffPerson.onlyNewScheme.before.csv",
            after = "CreateNewOutstaffPerson.onlyNewScheme.onlyFFC.after.csv")
    @DisplayName(value = "Оператор на ФФЦ (не РФ): максимальный набор документов. Только новая архитектура")
    void createOperatorInFFCNewScheme() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person")
                        .content(loadFromFile("operatorForFFCdata.json"))
                        .cookie(new Cookie("yandex_login", "ne-robot-out-hrms"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("ne-robot-out-hrms")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_EXTERNAL_PARTNER_PERSON_EDIT)))))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
            before = "CreateNewOutstaffPerson.bothSchemas.before.csv",
            after = "CreateNewOutstaffPerson.bothSchemas.onlySC.after.csv")
    @DisplayName(value = "Бригадир на СЦ: минимальный набор документов. Обе архитектуры")
    void createBrigadier() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person")
                        .content(loadFromFile("brigadierForSCdata.json"))
                        .cookie(new Cookie("yandex_login", "robot-out-hrms"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("robot-out-hrms")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_EXTERNAL_PARTNER_PERSON_EDIT)))))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
            before = "CreateNewOutstaffPerson.bothSchemas.before.csv",
            after = "CreateNewOutstaffPerson.bothSchemas.onlyFFC.after.csv")
    @DisplayName(value = "Оператор на ФФЦ (не РФ): максимальный набор документов. Обе архитектуры")
    void createOperator() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person")
                        .content(loadFromFile("operatorForFFCdata.json"))
                        .cookie(new Cookie("yandex_login", "ne-robot-out-hrms"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("ne-robot-out-hrms")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_EXTERNAL_PARTNER_PERSON_EDIT)))))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
            before = "CreateNewOutstaffPerson.bothSchemas.before.csv",
            after = "CreateNewOutstaffPerson.adminProfile.after.csv")
    @DisplayName(value = "Создание из-под учетки админа")
    void createOperatorWithAdminProfile() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person")
                        .content(loadFromFile("operatorForFFCdata.json"))
                        .cookie(new Cookie("yandex_login", "aboba"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("aboba")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_EXTERNAL_PARTNER_PERSON_EDIT),
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_PAGE_PRODUCTION_CALENDAR_SEE_ALL)))))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
            before = "CreateNewOutstaffPerson.bothSchemas.before.csv",
            after = "CreateNewOutstaffPerson.bothSchemas.before.csv")
    @DisplayName(value = "Создание c неполным списком документов, нужно отказать")
    void createOperatorWithoutNecessaryDocument() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person")
                        .content(loadFromFile("operatorForFFCdata.json"))
                        .cookie(new Cookie("yandex_login", "aboba"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("aboba")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_EXTERNAL_PARTNER_PERSON_EDIT)))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(
            before = "CreateNewOutstaffPerson.bothSchemas.before.csv",
            after = "CreateNewOutstaffPerson.bothSchemas.before.csv")
    @DisplayName(value = "Создание c полным списком документов, в одном не проставлено нужное поле, нужно отказать")
    void createOperatorWithoutNecessaryField() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person")
                        .content(loadFromFile("operatorForFFCdata.json"))
                        .cookie(new Cookie("yandex_login", "aboba"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("aboba")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_EXTERNAL_PARTNER_PERSON_EDIT)))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(
            before = "CreateNewOutstaffPerson.bothSchemas.before.csv",
            after = "CreateNewOutstaffPerson.bothSchemas.before.csv")
    @DisplayName(value = "Создание из-под левой учетки, нужно отказать в доступе")
    void createOperatorWithUnknownProfile() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person")
                        .content(loadFromFile("operatorForFFCdata.json"))
                        .cookie(new Cookie("yandex_login", "aboba"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("aboba")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_EXTERNAL_PARTNER_PERSON_EDIT)))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(
            before = "CreateNewOutstaffPerson.bothSchemas.before.csv",
            after = "CreateNewOutstaffPerson.bothSchemas.before.csv")
    @DisplayName("Попытка завести сотрудника задним числом, нужно отказать")
    void forbidCreateWithPastDate() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person")
                        .content(loadFromFile("operatorForFFCdata.incorrectStartDate.json"))
                        .cookie(new Cookie("yandex_login", "aboba"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("aboba")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_EXTERNAL_PARTNER_PERSON_EDIT),
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_PAGE_PRODUCTION_CALENDAR_SEE_ALL)))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(
            before = "CreateNewOutstaffPerson.onlyOldScheme.before.csv",
            after = "CreateNewOutstaffPerson.onlyOldScheme.before.csv")
    @DisplayName(value = "Логин от другой компании, нужно отказать в доступе")
    void forbidCreateForOtherCompany() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person")
                        .content(loadFromFile("operatorForFFCdata.json"))
                        .cookie(new Cookie("yandex_login", "robot-out-hrms"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("robot-out-hrms")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_EXTERNAL_PARTNER_PERSON_EDIT)))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(
            before = "CreateNewOutstaffPerson.bothSchemas.before.csv",
            after = "CreateNewOutstaffPerson.bothSchemas.before.csv")
    @DisplayName(value = "Указан несуществующий id площадки, нужно отказать")
    void forbidCreateWithUnknownDomainId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/external/partner/person")
                        .content(loadFromFile("operatorForFFCdata.unknownDomain.json"))
                        .cookie(new Cookie("yandex_login", "aboba"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user("aboba")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_EXTERNAL_PARTNER_PERSON_EDIT),
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_PAGE_PRODUCTION_CALENDAR_SEE_ALL)))))
                .andExpect(status().is4xxClientError());
    }
}

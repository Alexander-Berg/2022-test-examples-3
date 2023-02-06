package ru.yandex.market.tpl.internal.controller.partner;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.tpl.core.service.company.PartnerCompanyRoleService;
import ru.yandex.market.tpl.core.service.company.PartnerCompanyService;
import ru.yandex.market.tpl.core.service.mbi.MbiCompanyService;
import ru.yandex.market.tpl.core.service.partnerka.PartnerFeatureFlagService;
import ru.yandex.market.tpl.core.service.partnerka.PartnerkaCommandService;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.internal.BaseShallowTest;
import ru.yandex.market.tpl.internal.WebLayerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.core.mvc.PartnerCompanyHandler.COMPANY_HEADER;

@WebLayerTest(PartnerCompanyController.class)
public class PartnerCompanyControllerTest extends BaseShallowTest {

    @MockBean
    private PartnerkaCommandService commandService;
    @MockBean
    private PartnerCompanyService companyService;
    @MockBean
    private PartnerCompanyRoleService partnerCompanyRoleService;
    @MockBean
    private MbiCompanyService mbiCompanyService;
    @MockBean
    private PartnerFeatureFlagService partnerFeatureFlagService;
    @MockBean
    private SortingCenterPropertyService sortingCenterPropertyService;

    @Test
    void createCompany() throws Exception {
        MockHttpServletRequestBuilder builder = post("/internal/partner/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .header(COMPANY_HEADER, 1)
                .content(getFileContent("partner/company/request_create_company.json"));
        mockMvc.perform(builder).andExpect(status().is2xxSuccessful());
    }

    @Test
    void pageable() throws Exception {
        mockMvc.perform(get("/internal/partner/companies")
                        .header(COMPANY_HEADER, 1)
                        .param("page", "5")
                        .param("size", "10")
                        .param("sort", "login,desc")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(companyService).findAll(any(), pageableCaptor.capture(), any());
        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(5);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(
                Sort.by(Sort.Direction.DESC, "login")
                        .and(Sort.by(Sort.Direction.ASC, "name")));
    }

    @Test
    void createCompanyWithoutName() throws Exception {
        mockMvc.perform(
                        post("/internal/partner/companies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("partner/company/request_create_company_without_name.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createCompanyWithEmptyName() throws Exception {
        mockMvc.perform(
                        post("/internal/partner/companies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("partner/company/request_create_company_with_empty_name.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createCompanyWithoutLogin() throws Exception {
        mockMvc.perform(
                        post("/internal/partner/companies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("partner/company/request_create_company_without_login.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createCompanyWithInvalidDomainLogin() throws Exception {
        mockMvc.perform(
                        post("/internal/partner/companies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("partner/company" +
                                        "/request_create_company_with_invalid_domain_login" +
                                        ".json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createCompanyWithoutPhone() throws Exception {
        mockMvc.perform(
                        post("/internal/partner/companies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("partner/company/request_create_company_without_phone.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createCompanyWithInvalidPhoneFormat() throws Exception {
        mockMvc.perform(
                        post("/internal/partner/companies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("partner/company" +
                                        "/request_create_company_with_invalid_phone_format" +
                                        ".json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createCompanyWithoutTaxpayer() throws Exception {
        mockMvc.perform(
                        post("/internal/partner/companies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("partner/company/request_create_company_without_taxpayer" +
                                        ".json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createCompanyWithInvalidTaxpayerFormat() throws Exception {
        mockMvc.perform(
                        post("/internal/partner/companies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("partner/company/request_create_company_with_invalid_taxpayer" +
                                        ".json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createCompanyWithoutJurAddress() throws Exception {
        mockMvc.perform(
                        post("/internal/partner/companies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("partner/company/request_create_company_without_jur_address" +
                                        ".json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createCompanyWithEmptyJurAddress() throws Exception {
        mockMvc.perform(
                        post("/internal/partner/companies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("partner/company" +
                                        "/request_create_company_with_empty_jur_address.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createCompanyWithoutId() throws Exception {
        mockMvc.perform(
                        post("/internal/partner/companies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("partner/company/request_create_company_without_id.json")))
                .andExpect(status().is4xxClientError());
    }
}

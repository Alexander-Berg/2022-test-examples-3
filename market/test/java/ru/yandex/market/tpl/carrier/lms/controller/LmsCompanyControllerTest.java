package ru.yandex.market.tpl.carrier.lms.controller;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.company.CarrierCompanyQueryService;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.company.CompanyType;
import ru.yandex.market.tpl.carrier.core.domain.company.LegalForm;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.lms.company.LmsCompanyCreateDto;
import ru.yandex.market.tpl.carrier.planner.lms.company.LmsCompanyDsMappingCreateDto;
import ru.yandex.market.tpl.carrier.planner.lms.company.view.LmsCompanyType;
import ru.yandex.market.tpl.carrier.planner.lms.company.view.LmsLegalForm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class LmsCompanyControllerTest extends LmsControllerTest {

    private final TestUserHelper testUserHelper;
    private final DsRepository dsRepository;
    private final ObjectMapper tplObjectMapper;
    private final CarrierCompanyQueryService companyQueryService;

    private Company company;
    private Company company2;
    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        deliveryService = dsRepository.findByIdOrThrow(DeliveryService.DEFAULT_DS_ID);
        company2 = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(135136)
                .login("anotherLogin@yandex.ru")
                .companyName("Другой партнер")
                .deliveryServiceIds(Set.of())
                .build());
    }

    @SneakyThrows
    @Test
    void shouldGetCompanyMapping() {
        mockMvc.perform(get("/LMS/carrier/delivery-services")
                .param("companyId", String.valueOf(company2.getId()))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @SneakyThrows
    @Test
    void shouldGetCompanyMappingNew() {
        mockMvc.perform(get("/LMS/carrier/companies/{companyId}/delivery-service/new", company2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item.values.companyId").value(company2.getId()));
    }

    @SneakyThrows
    @Test
    void shouldCreateDeliveryServiceMapping() {
        mockMvc.perform(post("/LMS/carrier/companies/delivery-service/mapping")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(LmsCompanyDsMappingCreateDto.builder()
                        .companyId(company2.getId())
                        .deliveryServiceId(deliveryService.getId())
                        .build()
                ))
        )
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(company2.getId())));

        mockMvc.perform(get("/LMS/carrier/delivery-services")
                .param("companyId", String.valueOf(company2.getId()))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].values.deliveryServiceId").value(deliveryService.getId()))
        ;
    }

    @SneakyThrows
    @Test
    void shouldCreateNewCompany() {
        var request = LmsCompanyCreateDto.builder()
                .campaignId(10001L)
                .name("ООО Туда-Сюда")
                .login("who cares?")
                .phoneNumber("88005353535")
                .taxpayerNumber("1919191922")
                .juridicalAddress("не указан")
                .naturalAddress("не указан")
                .legalForm(LmsLegalForm.OOO)
                .ogrn("1231232452345")
                .type(LmsCompanyType.LINEHAUL)
                .contractId(null)
                .contractDate(null)
                .build();

        String newCompanyId = mockMvc.perform(post("/LMS/carrier/companies")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Company newCompany = companyQueryService.findByIdOrThrow(Long.valueOf(newCompanyId));

        assertThat(newCompany).isNotNull();
        assertThat(newCompany.getDsmId()).isNotNull();
        assertThat(newCompany.getCampaignId()).isEqualTo(request.getCampaignId());
        assertThat(newCompany.getName()).isEqualTo(request.getName());
        assertThat(newCompany.getLogin()).isEqualTo(request.getLogin());
        assertThat(newCompany.getPhoneNumber()).isEqualTo(request.getPhoneNumber());
        assertThat(newCompany.getTaxpayerNumber()).isEqualTo(request.getTaxpayerNumber());
        assertThat(newCompany.getJuridicalAddress()).isEqualTo(request.getJuridicalAddress());
        assertThat(newCompany.getNaturalAddress()).isEqualTo(request.getNaturalAddress());
        assertThat(newCompany.isDeactivated()).isEqualTo(false);
        assertThat(newCompany.isSuperCompany()).isEqualTo(false);
        assertThat(newCompany.getLegalForm()).isEqualTo(LegalForm.OOO);
        assertThat(newCompany.getOgrn()).isEqualTo(request.getOgrn());
        assertThat(newCompany.getType()).isEqualTo(CompanyType.LINEHAUL);
        assertThat(newCompany.getContractId()).isEqualTo(request.getContractId());
        assertThat(newCompany.getContractDate()).isEqualTo(request.getContractDate());
    }

}

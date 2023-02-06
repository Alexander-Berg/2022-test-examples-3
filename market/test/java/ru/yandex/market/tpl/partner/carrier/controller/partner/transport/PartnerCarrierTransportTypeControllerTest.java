package ru.yandex.market.tpl.partner.carrier.controller.partner.transport;


import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportType;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportTypeRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportTypeSource;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.transport.PartnerTransportTypeDto;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_={@Autowired})
public class PartnerCarrierTransportTypeControllerTest extends BaseTplPartnerCarrierWebIntTest {

    private final TestUserHelper testUserHelper;
    private final TransportTypeRepository transportTypeRepository;
    private final ObjectMapper tplObjectMapper;

    private TransportType transportType;

    private Company company1;
    private Company company2;


    @BeforeEach
    void setUp() {
        company1 = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName("Company1")
                .campaignId(123L)
                .login("company1@yanex.ru")
                .build());
        company2 = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName("Company 2")
                .campaignId(234L)
                .login("company2@yandex.ru")
                .build()
        );

        transportType = transportTypeRepository.save(
                TransportType.builder()
                .name("Машинка")
                .capacity(BigDecimal.TEN)
                .routingPriority(100)
                .palletsCapacity(123)
                .company(company1)
                .source(TransportTypeSource.CARRIER)
                .build()
        );
    }

    @SneakyThrows
    @Test
    void shouldGetTransportTypes() {
        mockMvc.perform(
                get("/internal/partner/transportTypes")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company1.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));

        mockMvc.perform(
                get("/internal/partner/transportTypes")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company2.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @SneakyThrows
    @Test
    void shouldCreateTransportTypes() {
        PartnerTransportTypeDto partnerTransportTypeDto = PartnerTransportTypeDto.builder()
                .name("Новый тип транспорта")
                .capacity(123d)
                .palletsCapacity(321)
                .build();

        mockMvc.perform(post("/internal/partner/transportTypes")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company1.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(partnerTransportTypeDto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        mockMvc.perform(get("/internal/partner/transportTypes")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company1.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));
    }

    @SneakyThrows
    @Test
    void shouldUpdateTransportType() {
        PartnerTransportTypeDto partnerTransportTypeDto = PartnerTransportTypeDto.builder()
                .name("Новый тип транспорта")
                .capacity(123d)
                .palletsCapacity(678)
                .build();

        mockMvc.perform(put("/internal/partner/transportTypes/{id}", transportType.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company1.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(partnerTransportTypeDto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());


        var response = mockMvc.perform(
                get("/internal/partner/transportTypes")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company1.getCampaignId())
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FakePage responsePage = tplObjectMapper.readValue(response, FakePage.class);

        Assertions.assertThat(responsePage.getContent())
                .anyMatch(t -> "Новый тип транспорта".equals(t.getName()))
                .anyMatch(t -> Math.abs(t.getCapacity() - 123d) < 0.001)
                .anyMatch(t -> t.getPalletsCapacity() == 678);

    }

    @SneakyThrows
    @Test
    void shouldDeleteTransportType() {
        mockMvc.perform(delete("/internal/partner/transportTypes/{id}", transportType.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company1.getCampaignId())
        )
                .andExpect(status().isOk());


        mockMvc.perform(get("/internal/partner/transportTypes")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company1.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(0)));
    }

    @Data
    public static class FakePage {
        private List<PartnerTransportTypeDto> content;
    }
}

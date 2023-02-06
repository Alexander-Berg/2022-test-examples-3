package ru.yandex.market.tpl.internal.controller.partner.filters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.yandex.market.tpl.api.model.company.PartnerCompanyFilterOptionDto;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.internal.controller.BaseTplIntWebTest;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.core.mvc.PartnerCompanyHandler.COMPANY_HEADER;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class PartnerCompaniesFilterControllerTest extends BaseTplIntWebTest {

    private static final long SC_ID = 44L;
    private static final String COMPANY_NAME = "Second";

    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;

    @BeforeEach
    void setUp() {
        testUserHelper.findOrCreateSuperCompany();
        testUserHelper.findOrCreateCompany(
                TestUserHelper.CompanyGenerateParam
                        .builder()
                        .companyName("First")
                        .campaignId(11)
                        .build()
        );
        testUserHelper.findOrCreateCompany(
                TestUserHelper.CompanyGenerateParam
                        .builder()
                        .companyName(COMPANY_NAME)
                        .campaignId(12)
                        .sortingCenterIds(Set.of(SC_ID))
                        .build()
        );
    }


    List<PartnerCompanyFilterOptionDto> makeRequest(Function<MockHttpServletRequestBuilder, MockHttpServletRequestBuilder> customizeRequest) throws Exception {
        RequestBuilder req = customizeRequest.apply(
                get("/internal/partner/filters/companies")
                        .header(COMPANY_HEADER, Company.DEFAULT_CAMPAIGN_ID)
        );

        String response = mockMvc.perform(req)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<PartnerCompanyFilterOptionDto> responseObj = tplObjectMapper.readValue(
                response,
                new TypeReference<List<PartnerCompanyFilterOptionDto>>() {
                }
        );

        return responseObj;
    }

    @Test
    void shouldReturnCompaniesFilterOptionsAll() throws Exception {
        List<PartnerCompanyFilterOptionDto> responseObj = makeRequest(req -> req);

        Assertions.assertThat(responseObj.size()).isEqualTo(3);
    }

    @Test
    void shouldReturnCompaniesFilterOptionsBySC() throws Exception {
        List<PartnerCompanyFilterOptionDto> responseObj = makeRequest(
                req -> req.param("sortingCenterIds", String.valueOf(SC_ID))
        );

        Assertions.assertThat(responseObj.size()).isEqualTo(1);
        Assertions.assertThat(
                responseObj.stream().filter(comp -> COMPANY_NAME.equals(comp.getName())).findFirst().isPresent()
        ).isEqualTo(true);

    }
}

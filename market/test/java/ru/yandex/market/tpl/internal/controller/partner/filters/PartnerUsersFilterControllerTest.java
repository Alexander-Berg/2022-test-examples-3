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
import ru.yandex.market.tpl.api.model.user.partner.PartnerReportCourierOptionDto;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.internal.controller.BaseTplIntWebTest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.core.mvc.PartnerCompanyHandler.COMPANY_HEADER;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class PartnerUsersFilterControllerTest extends BaseTplIntWebTest {

    private static final long COURIER_UID = 114L;

    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;
    private final CompanyRepository companyRepository;

    private Company superCompany;

    @BeforeEach
    void setUp() {
        superCompany = testUserHelper.findOrCreateSuperCompany(777L, null);
        testUserHelper.findOrCreateUser(111L);
        testUserHelper.findOrCreateUser(112L);
        testUserHelper.findOrCreateUser(113L);
        testUserHelper.findOrCreateUserWithoutSchedule(114L, superCompany);
    }


    List<PartnerReportCourierOptionDto> makeRequest(Function<MockHttpServletRequestBuilder, MockHttpServletRequestBuilder> customizeRequest) throws Exception {
        RequestBuilder req = customizeRequest.apply(
                get("/internal/partner/filters/courier")
                        .header(COMPANY_HEADER, superCompany.getCampaignId())
        );

        String response = mockMvc.perform(req)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String,List<PartnerReportCourierOptionDto>> responseObj = tplObjectMapper.readValue(
                response,
                new TypeReference<Map<String,List<PartnerReportCourierOptionDto>>>(){}
        );

        List<PartnerReportCourierOptionDto> couriers = responseObj.get("options");

        return couriers;
    }

    @Test
    void shouldReturnCouriersFilterOptionsAll() throws Exception {
        List<PartnerReportCourierOptionDto> couriers = makeRequest(req -> req);

        Assertions.assertThat(couriers.size()).isEqualTo(4);
    }

    @Test
    void shouldReturnCompaniesFilterOptionsByCompanyName() throws Exception {
        List<PartnerReportCourierOptionDto> couriers = makeRequest(
                req -> req.param("companyName", superCompany.getName())
        );

        Assertions.assertThat(couriers.size()).isEqualTo(1);
        Assertions.assertThat(
                couriers.stream().filter(courier -> COURIER_UID == courier.getCourierUid()).findFirst().isPresent()
        ).isEqualTo(true);

    }

    @Test
    void shouldReturnCompaniesFilterOptionsBySC() throws Exception {
        List<PartnerReportCourierOptionDto> couriers = makeRequest(
                req -> req.param("sortingCenterIds", String.valueOf(SortingCenter.DEFAULT_SC_ID))
        );

        Assertions.assertThat(couriers.size()).isEqualTo(3);
    }
}

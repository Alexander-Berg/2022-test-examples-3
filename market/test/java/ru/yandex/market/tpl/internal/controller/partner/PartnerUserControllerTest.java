package ru.yandex.market.tpl.internal.controller.partner;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.model.user.UserRole;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserListEntryDto;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserRegionDto;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserRoutingPropertiesDto;
import ru.yandex.market.tpl.core.domain.partner.PartnerkaCommandRepository;
import ru.yandex.market.tpl.core.service.company.CompanyAuthenticationService;
import ru.yandex.market.tpl.core.service.company.PartnerCompanyRoleService;
import ru.yandex.market.tpl.core.service.partnerka.PartnerkaCommandService;
import ru.yandex.market.tpl.core.service.user.partner.PartnerUserService;
import ru.yandex.market.tpl.core.service.user.partner.UserStatusService;
import ru.yandex.market.tpl.core.service.user.personal.data.UserPersonalDataService;
import ru.yandex.market.tpl.internal.BaseShallowTest;
import ru.yandex.market.tpl.internal.WebLayerTest;
import ru.yandex.market.tpl.internal.service.report.UserReportService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.core.mvc.PartnerCompanyHandler.COMPANY_HEADER;

/**
 * @author a-bryukhov
 */
@WebLayerTest(PartnerUserController.class)
class PartnerUserControllerTest extends BaseShallowTest {

    @MockBean
    private PartnerUserService partnerUserService;
    @MockBean
    private PartnerkaCommandRepository partnerkaCommandRepository;
    @MockBean
    private PartnerkaCommandService commandService;
    @MockBean
    private UserReportService userReportService;
    @MockBean
    private UserStatusService userStatusService;
    @MockBean
    private UserPersonalDataService userPersonalDataService;
    @MockBean
    private PartnerCompanyRoleService partnerCompanyRoleService;
    @MockBean
    CompanyAuthenticationService companyAuthenticationService;

    @Test
    void shouldReturnCouriers() throws Exception {
        when(partnerUserService.findAll(any(), any(), any())).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(2);

            var couriers = List.of(
                    createCourier(1L, false),
                    createCourier(2L, true)
            );

            return new PageImpl<>(couriers, pageable, 9);
        });

        mockMvc.perform(get("/internal/partner/users?size=2").contentType(MediaType.APPLICATION_JSON)
                        .header(COMPANY_HEADER, 1))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("partner/response_couriers.json"), true));
    }

    private PartnerUserListEntryDto createCourier(long id, boolean routingProps) {
        var courier = new PartnerUserListEntryDto();
        courier.setId(id);
        courier.setName("Курьер Курьер");
        courier.setRole(UserRole.ADMIN);
        courier.setRegionList(List.of(new PartnerUserRegionDto("Якиманка"), new PartnerUserRegionDto("Замоскворечье")));
        courier.setRecipientCallEnabled(false);

        if (routingProps) {
            var routingProperties = new PartnerUserRoutingPropertiesDto();
            routingProperties.setFootMultiplier(BigDecimal.TEN);
            routingProperties.setCarMultiplier(BigDecimal.ONE);
            routingProperties.setCarSharedServiceMultiplier(BigDecimal.TEN);
            routingProperties.setCarServiceMultiplier(BigDecimal.ONE);
            routingProperties.setCarEnabled(true);
            routingProperties.setFootEnabled(false);
            courier.setRoutingProperties(routingProperties);
        }

        return courier;
    }
}

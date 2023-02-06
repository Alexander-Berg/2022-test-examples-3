 package ru.yandex.market.tpl.internal.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.shops.SimpleShopRegistrationRequest;
import ru.yandex.market.mbi.api.client.entity.shops.SimpleShopRegistrationResponse;
import ru.yandex.market.tpl.api.model.company.PartnerCompanyRequestDto;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.service.company.PartnerCompanyService;
import ru.yandex.market.tpl.core.service.mbi.MbiCompanyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ExtendWith(SpringExtension.class)
public class MbiCompanyServiceTest {

    private static final String COMPANY_LOGIN = "company-email";
    private static final Long COMPANY_LOGIN_UID = 123L;
    private static final Long CAMPAIGN_ID = 1234567890L;

    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @MockBean
    private BlackboxClient blackboxClient;
    @MockBean
    private MbiApiClient mbiApiClient;
    @MockBean
    private PartnerCompanyService partnerCompanyService;

    private MbiCompanyService mbiCompanyService;

    @BeforeEach
    void init() {
        mbiCompanyService =
                new MbiCompanyService(configurationProviderAdapter, partnerCompanyService, blackboxClient,
                        mbiApiClient);
    }

    @Test
    void createCompanyAndMbiCabinet() {
        given(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.CREATE_MBI_CABINET_AT_CREATE_COMPANY))
                .willReturn(Boolean.TRUE);

        given(blackboxClient.getUidForLogin(COMPANY_LOGIN)).willReturn(COMPANY_LOGIN_UID);

        given(mbiApiClient.simpleRegisterShop(
                eq(COMPANY_LOGIN_UID),
                eq(COMPANY_LOGIN_UID),
                any(SimpleShopRegistrationRequest.class))
        )
                .willReturn(shopRegistrationResponse());

        mbiCompanyService.create(
                companyDto(),
                CampaignType.TPL
        );

        ArgumentCaptor<PartnerCompanyRequestDto> captor =
                ArgumentCaptor.forClass(PartnerCompanyRequestDto.class);
        verify(partnerCompanyService, times(1)).create(captor.capture());
        assertThat(captor.getValue().getCampaignId()).isEqualTo(CAMPAIGN_ID);
    }

    private SimpleShopRegistrationResponse shopRegistrationResponse() {
        SimpleShopRegistrationResponse response = new SimpleShopRegistrationResponse();
        response.setCampaignId(CAMPAIGN_ID);
        return response;
    }

    private PartnerCompanyRequestDto companyDto() {
        return PartnerCompanyRequestDto.builder()
                .login(COMPANY_LOGIN + "@yandex.ru")
                .build();
    }
}

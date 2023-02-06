package ru.yandex.direct.api.v5.testing.configuration;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.direct.api.v5.configuration.ApiConfiguration;
import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.adgroups.converter.AdGroupsAddRequestConverter;
import ru.yandex.direct.api.v5.entity.adgroups.converter.AdGroupsUpdateRequestConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.core.configuration.CoreConfiguration;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;

import static org.mockito.Mockito.mock;

@Configuration
@Import({CoreTestingConfiguration.class, ApiConfiguration.class})
public class Api5TestingConfiguration {

    @MockBean(name = ApiConfiguration.API_AUTHENTICATION_SOURCE)
    public ApiAuthenticationSource apiAuthenticationSource;

    @SpyBean
    public AdGroupsAddRequestConverter addRequestConverter;

    @SpyBean
    public AdGroupsUpdateRequestConverter updateRequestConverter;

    @SpyBean
    public ResultConverter resultConverter;

    @Bean(name = CoreConfiguration.ACCESSIBLE_CAMPAIGN_CHEKER_PROVIDER)
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public RequestCampaignAccessibilityCheckerProvider accessibilityCheckerProvider() {
        RequestCampaignAccessibilityCheckerProvider types = new RequestCampaignAccessibilityCheckerProvider();
        types.setApi5();
        return types;
    }

    @Bean
    public GenericApiService genericApiService(RequestCampaignAccessibilityCheckerProvider accessibilityCheckerProvider) {
        ApiContextHolder apiContextHolder = new ApiContextHolder() {
            @NotNull
            @Override
            public ApiContext get() {
                return new ApiContext();
            }
        };
        return new GenericApiService(apiContextHolder,
                mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class),
                accessibilityCheckerProvider);
    }

}

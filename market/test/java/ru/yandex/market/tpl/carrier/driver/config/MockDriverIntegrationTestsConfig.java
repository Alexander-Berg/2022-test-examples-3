package ru.yandex.market.tpl.carrier.driver.config;

import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.tpl.carrier.driver.service.app.link.PublicLinkResolverService;
import ru.yandex.market.tpl.carrier.driver.service.photo.AvatarnicaPhotoSaver;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.common.xiva.subscribe.XivaSubscriptionTvmClient;
import ru.yandex.mj.generated.client.yandex_magistral.api.YandexMagistralApiClient;
import ru.yandex.passport.tvmauth.TvmClient;

@MockBean(classes = {
        AvatarnicaPhotoSaver.class,
        BlackboxClient.class,
        XivaSubscriptionTvmClient.class,
        TvmClient.class,
        YandexMagistralApiClient.class
}, answer = Answers.RETURNS_DEEP_STUBS)
@Configuration
public class MockDriverIntegrationTestsConfig {

    @Bean
    public PublicLinkResolverService publicLinkResolverService() {
        PublicLinkResolverService publicLinkResolverService = Mockito.mock(PublicLinkResolverService.class);
        Mockito.when(publicLinkResolverService.getResolvedPublicLink(Mockito.anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        return publicLinkResolverService;
    }

    @Bean
    public RestTemplate diskApiRestTemplateMock() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.doThrow(new ResourceAccessException("Mocked exceptiom"))
                .when(restTemplate)
                .getForObject(Mockito.any(), Mockito.any());
        return restTemplate;
    }

    @Bean
    public PublicLinkResolverService publicLinkResolverServiceForRetryTest(
            @Autowired RestTemplate diskApiRestTemplateMock
    ) {
        return new PublicLinkResolverService(diskApiRestTemplateMock);
    }
}

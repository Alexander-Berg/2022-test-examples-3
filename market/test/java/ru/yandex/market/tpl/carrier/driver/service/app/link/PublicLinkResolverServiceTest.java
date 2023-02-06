package ru.yandex.market.tpl.carrier.driver.service.app.link;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;

@RequiredArgsConstructor(onConstructor_=@Autowired)
class PublicLinkResolverServiceTest extends BaseDriverApiIntTest {

    private final PublicLinkResolverService publicLinkResolverServiceForRetryTest;
    private final RestTemplate diskApiRestTemplateMock;

    @Test
    @SneakyThrows
    void shouldRetryDiskCall() {
        try {
            publicLinkResolverServiceForRetryTest.getResolvedPublicLink("https://disk.yandex.ru/link");
        } catch (ResourceAccessException ignored) { }

        Mockito.verify(diskApiRestTemplateMock, Mockito.times(5))
                .getForObject(Mockito.any(), Mockito.any());
    }

}

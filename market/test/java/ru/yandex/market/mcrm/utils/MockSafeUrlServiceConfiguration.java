package ru.yandex.market.mcrm.utils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.mcrm.utils.html.SafeUrlService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockSafeUrlServiceConfiguration {

    @Bean
    @Primary
    public SafeUrlService mockSafeUrlService() {
        SafeUrlService mock = mock(SafeUrlService.class);
        when(mock.toSafeUrl(anyString())).then(inv -> inv.getArgument(0));
        return mock;
    }
}

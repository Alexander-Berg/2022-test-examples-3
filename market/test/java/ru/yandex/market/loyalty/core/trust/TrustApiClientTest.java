package ru.yandex.market.loyalty.core.trust;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.monitoring.PushMonitor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TrustApiClientTest extends MarketLoyaltyCoreMockedDbTestBase {

    private TrustApiClient apiClient;
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final TvmTicketProvider tvmTicketProvider = mock(TvmTicketProvider.class);
    private final PushMonitor monitor = mock(PushMonitor.class);
    private final ConfigurationService configurationService = mock(ConfigurationService.class);

    @Before
    public void init() {
        apiClient = new TrustApiClient(restTemplate, tvmTicketProvider, monitor, configurationService);
    }

    @Test
    public void getYandexAccountBalance_enabledLimiter() {
        when(configurationService.isTrustApiClientRateLimiterEnabled()).thenReturn(true);
        TrustApiClient spy = spy(apiClient);
        spy.getYandexAccountBalance(1L, false);

        verify(tvmTicketProvider, times(1)).getServiceTicket();
        verify(spy, times(1)).useRateLimiter(false);
    }

    @Test
    public void getYandexAccountBalance_disabledLimiter() {
        when(configurationService.isTrustApiClientRateLimiterEnabled()).thenReturn(false);
        TrustApiClient spy = spy(apiClient);
        spy.getYandexAccountBalance(1L, false);

        verify(tvmTicketProvider, times(1)).getServiceTicket();
        verify(spy, times(0)).useRateLimiter(false);
    }
}

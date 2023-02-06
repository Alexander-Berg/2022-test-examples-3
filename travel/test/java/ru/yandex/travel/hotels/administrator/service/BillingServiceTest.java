package ru.yandex.travel.hotels.administrator.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.http.apiclient.HttpApiRetryableException;
import ru.yandex.travel.hotels.administrator.configuration.BillingServiceProperties;
import ru.yandex.travel.hotels.administrator.entity.LegalDetails;
import ru.yandex.travel.integration.balance.BillingApiClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class BillingServiceTest {
    private BillingApiClient apiClient;
    private BillingService service;

    @Before
    public void init() {
        BillingServiceProperties properties = BillingServiceProperties.builder()
                .operatorId(7523482694372L)
                .serviceId(836582364)
                .build();
        apiClient = mock(BillingApiClient.class);
        service = new BillingService(properties, apiClient);
    }

    @Test
    public void createClient_retryableException() {
        doThrow(new HttpApiRetryableException("500")).when(apiClient).createClient(anyLong(), any());
        assertThatThrownBy(() -> service.createClient(legalDetails()))
                .isExactlyInstanceOf(RetryableServiceException.class)
                .hasMessageContaining("request has failed with a retryable exception");
    }

    @Test
    public void createPerson_retryableException() {
        doThrow(new HttpApiRetryableException("500")).when(apiClient).createPerson(anyLong(), any());
        assertThatThrownBy(() -> service.createPerson(1L, legalDetails()))
                .isExactlyInstanceOf(RetryableServiceException.class)
                .hasMessageContaining("request has failed with a retryable exception");
    }
    @Test
    public void createAcceptedOffer_retryableException() {
        doThrow(new HttpApiRetryableException("500")).when(apiClient).createContract(anyLong(), any());
        assertThatThrownBy(() -> service.createAcceptedOffer(1L, 2L))
                .isExactlyInstanceOf(RetryableServiceException.class)
                .hasMessageContaining("request has failed with a retryable exception");
    }

    private LegalDetails legalDetails() {
        return LegalDetails.builder()
                .hotelConnections(List.of())
                .build();
    }
}

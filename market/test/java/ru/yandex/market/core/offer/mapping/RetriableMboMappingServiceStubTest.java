package ru.yandex.market.core.offer.mapping;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.market.http.ServiceException;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsServiceStub;

/**
 * Тест на ретраи {@link RetriableMboMappingsServiceStub}.
 */
class RetriableMboMappingServiceStubTest {

    private static RetryTemplate retryTemplate;
    private static MboMappingsServiceStub mboMappingsServiceStub;

    @BeforeAll
    static void init() {
        retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(new ExponentialBackOffPolicy());
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));

        mboMappingsServiceStub = Mockito.mock(MboMappingsServiceStub.class);
    }

    @BeforeEach
    void initMock() {
        Mockito.clearInvocations(mboMappingsServiceStub);
    }

    @Test
    void testRetry() {
        Mockito.when(mboMappingsServiceStub.addProductInfo(Mockito.any(MboMappings.ProviderProductInfoRequest.class)))
                .thenThrow(ServiceException.class)
                .thenReturn(MboMappings.ProviderProductInfoResponse.newBuilder().setMessage("GOOD RESULT").build());

        RetriableMboMappingsServiceStub retriableMboMappingsServiceStub = new RetriableMboMappingsServiceStub(
                retryTemplate, mboMappingsServiceStub);

        MboMappings.ProviderProductInfoResponse response = retriableMboMappingsServiceStub.addProductInfo(MboMappings.ProviderProductInfoRequest.newBuilder().build());
        Assertions.assertEquals("GOOD RESULT", response.getMessage());
        Mockito.verify(mboMappingsServiceStub, Mockito.times(2)).addProductInfo(Mockito.any(MboMappings.ProviderProductInfoRequest.class));
    }

    @Test
    void testRetryLimit() {
        Mockito.when(mboMappingsServiceStub.addProductInfo(Mockito.any(MboMappings.ProviderProductInfoRequest.class)))
                .thenThrow(ServiceException.class)
                .thenThrow(RuntimeException.class)
                .thenThrow(ServiceException.class)
                .thenThrow(RuntimeException.class);

        RetriableMboMappingsServiceStub retriableMboMappingsServiceStub = new RetriableMboMappingsServiceStub(
                retryTemplate, mboMappingsServiceStub);

        Assertions.assertThrows(RuntimeException.class,
                () -> retriableMboMappingsServiceStub.addProductInfo(MboMappings.ProviderProductInfoRequest.newBuilder().build()));
        Mockito.verify(mboMappingsServiceStub, Mockito.times(3)).addProductInfo(Mockito.any(MboMappings.ProviderProductInfoRequest.class));
    }
}

package ru.yandex.market.logistics.lom.service.tvm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.util.client.tvm.client.DetailedTvmClient;
import ru.yandex.market.logistics.util.client.tvm.client.ServiceInfo;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DetailedTvmServiceTest extends AbstractContextualTest {
    @Autowired
    private DetailedTvmService detailedTvmService;

    @Autowired
    private DetailedTvmClient detailedTvmClient;

    @Test
    void getServiceCacheable() {
        ServiceInfo expected = ServiceInfo.builder().abcServiceId(100L).build();
        when(detailedTvmClient.getServiceInfo(1L)).thenReturn(expected);

        ServiceInfo actual;
        for (int i = 0; i < 100; ++i) {
            actual = detailedTvmService.getServiceInfo(1L);
            softly.assertThat(actual).isEqualTo(expected);
        }

        verify(detailedTvmClient).getServiceInfo(1L);
        verifyNoMoreInteractions(detailedTvmClient);
    }

    @Test
    void getServiceCacheableWithNoCachedValue() {
        ServiceInfo expected = ServiceInfo.builder().abcServiceId(100L).build();
        ServiceInfo noCachedExpected = ServiceInfo.builder().abcServiceId(101L).build();
        when(detailedTvmClient.getServiceInfo(1L)).thenReturn(expected);
        when(detailedTvmClient.getServiceInfo(2L)).thenReturn(noCachedExpected);

        ServiceInfo actual;
        actual = detailedTvmService.getServiceInfo(1L);
        softly.assertThat(actual).isEqualTo(expected);

        ServiceInfo noCached = detailedTvmService.getServiceInfo(2L);
        softly.assertThat(noCached).isEqualTo(noCachedExpected);

        actual = detailedTvmService.getServiceInfo(1L);
        softly.assertThat(actual).isEqualTo(expected);

        verify(detailedTvmClient).getServiceInfo(1L);
        verify(detailedTvmClient).getServiceInfo(2L);
        verify(detailedTvmClient).getServiceInfo(1L);
        verifyNoMoreInteractions(detailedTvmClient);
    }

}

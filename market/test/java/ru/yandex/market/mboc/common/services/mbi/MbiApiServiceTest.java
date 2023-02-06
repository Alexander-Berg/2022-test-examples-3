package ru.yandex.market.mboc.common.services.mbi;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.notification.SendNotificationResponse;
import ru.yandex.market.mboc.common.notifications.model.data.suppliers.ProcessedOffersData;
import ru.yandex.market.mboc.common.test.YamlTestUtil;

public class MbiApiServiceTest {

    private MbiApiService mbiApiService;

    private MbiApiClient mbiApiClientMock;
    private ProcessedOffersData processedOffersData;

    @Before
    public void setUp() {
        mbiApiClientMock = Mockito.mock(MbiApiClient.class);
        processedOffersData = YamlTestUtil
            .readFromResources("mbi/supplier-processed-offers-data.yml", ProcessedOffersData.class);

    }

    @Test
    public void sendProcessedOffersNotificationToSupplier() {
        mbiApiService = new MbiApiService(mbiApiClientMock, true);

        Mockito.when(mbiApiClientMock.sendMessageToSupplier(Mockito.anyLong(), Mockito.anyInt(), Mockito.anyString()))
            .thenReturn(new SendNotificationResponse(1L));
        Assertions.assertThat(mbiApiService
            .sendProcessedOffersNotificationToSupplier(1L, 1, processedOffersData)).isTrue();

        Mockito.when(mbiApiClientMock.sendMessageToSupplier(Mockito.anyLong(), Mockito.anyInt(), Mockito.anyString()))
            .thenThrow(new RuntimeException("Something went wrong"));
        Assertions.assertThat(mbiApiService
            .sendProcessedOffersNotificationToSupplier(1L, 1, processedOffersData)).isFalse();
    }

    @Test
    public void testDisabledNotifications() {
        mbiApiService = new MbiApiService(mbiApiClientMock, false);

        Assertions.assertThat(mbiApiService
            .sendProcessedOffersNotificationToSupplier(1L, 1, processedOffersData)).isTrue();

        Mockito.verify(mbiApiClientMock, Mockito.times(0))
            .sendMessageToSupplier(Mockito.anyLong(), Mockito.anyInt(), Mockito.anyString());

    }
}

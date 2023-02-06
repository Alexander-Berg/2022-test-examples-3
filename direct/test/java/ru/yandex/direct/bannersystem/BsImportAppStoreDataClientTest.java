package ru.yandex.direct.bannersystem;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.bannersystem.container.appstoredata.BsImportAppStoreDataResponse;
import ru.yandex.direct.bannersystem.container.appstoredata.BsImportAppStoreDataResponseItem;
import ru.yandex.direct.bannersystem.exception.BsClientException;
import ru.yandex.direct.bannersystem.exception.BsImportAppStoreDataException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

public class BsImportAppStoreDataClientTest {
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(60);

    @Mock
    private BannerSystemClient bannerSystemClient;
    private BsImportAppStoreDataClient client;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        client = new BsImportAppStoreDataClient(bannerSystemClient);
    }

    @Test
    public void testSendAppStoreData() {
        BsImportAppStoreDataResponseItem responseItem = new BsImportAppStoreDataResponseItem();
        responseItem.setError(1);
        responseItem.setMobileAppId(1L);
        responseItem.setErrorMessage("Message");

        doReturn(new BsImportAppStoreDataResponse().withProcessedResults(Collections.singletonList(responseItem)))
                .when(bannerSystemClient).doRequest(any(), any(), any(UUID.class), any(Duration.class));

        List<BsImportAppStoreDataResponseItem>
                responseItemList = client.sendAppStoreData(Collections.emptyList(), UUID.randomUUID(), TEST_TIMEOUT);

        assertThat(responseItemList)
                .containsOnly(responseItem)
                .as("Получили только нужную запись, не смотря на ошибки в ней");
    }

    @Test(expected = BsImportAppStoreDataException.class)
    public void testSendAppStoreDataErrorOnBaseException() {
        doThrow(BsClientException.class)
                .when(bannerSystemClient).doRequest(any(), any(), any(UUID.class), any(Duration.class));

        client.sendAppStoreData(Collections.emptyList(), UUID.randomUUID(), TEST_TIMEOUT);
    }

    @Test(expected = BsImportAppStoreDataException.class)
    public void testSendAppStoreDataErrorInParsedResponse() {
        doReturn(new BsImportAppStoreDataResponse().withError(1).withErrorMessage("Message"))
                .when(bannerSystemClient).doRequest(any(), any(), any(UUID.class), any(Duration.class));

        client.sendAppStoreData(Collections.emptyList(), UUID.randomUUID(), TEST_TIMEOUT);
    }

    @Test(expected = BsImportAppStoreDataException.class)
    public void testSendAppStoreDataUnDoneInParsedResponse() {
        doReturn(new BsImportAppStoreDataResponse().withUnDone(1))
                .when(bannerSystemClient).doRequest(any(), any(), any(UUID.class), any(Duration.class));

        client.sendAppStoreData(Collections.emptyList(), UUID.randomUUID(), TEST_TIMEOUT);
    }

    @Test(expected = BsImportAppStoreDataException.class)
    public void testSendAppStoreDataEmptyParsedResponse() {
        doReturn(new BsImportAppStoreDataResponse())
                .when(bannerSystemClient).doRequest(any(), any(), any(UUID.class), any(Duration.class));

        client.sendAppStoreData(Collections.emptyList(), UUID.randomUUID(), TEST_TIMEOUT);
    }
}

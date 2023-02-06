package ru.yandex.direct.bannersystem;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.bannersystem.container.exporttable.SspInfoRecord;
import ru.yandex.direct.bannersystem.exception.BsClientException;
import ru.yandex.direct.bannersystem.exception.BsExportTableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

public class BsExportTableClientTest {
    @Mock
    private BannerSystemClient bannerSystemClient;
    private BsExportTableClient client;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        client = new BsExportTableClient(bannerSystemClient);
    }

    @Test
    public void testGetSspPlatforms() {
        // Немного бесполезный тест, проверяющий, что ничего не развалилось
        SspInfoRecord record = new SspInfoRecord("Title", "option1,option2", 1000L);
        doReturn(Collections.singletonList(record))
                .when(bannerSystemClient).doRequest(any(), any(), any());

        List<SspInfoRecord> sspInfoRecords = client.getSspInfoTableRowsList();

        assertThat(sspInfoRecords)
                .containsOnly(record)
                .as("Получили только нужную запись");
    }

    @Test(expected = BsExportTableException.class)
    public void testGetSspPlatformsError() {
        doThrow(BsClientException.class)
                .when(bannerSystemClient).doRequest(any(), any(), any());

        client.getSspInfoTableRowsList();
    }
}

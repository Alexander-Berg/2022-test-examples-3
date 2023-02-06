package ru.yandex.market.direct.feed;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.direct.feed.RefreshFeedYtDao;
import ru.yandex.market.core.direct.feed.model.RefreshFeedRecord;
import ru.yandex.market.core.direct.feed.model.YtRefreshFeedRecord;
import ru.yandex.market.yt.binding.BindingTable;
import ru.yandex.market.yt.client.YtClientProxy;
import ru.yandex.market.yt.client.YtClientProxySource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RefreshFeedYtDaoTest {
    private static final BindingTable<YtRefreshFeedRecord> BINDING_TABLE =
            new BindingTable<>("T", YtRefreshFeedRecord.class);
    private final YtClientProxy ytClient = mock(YtClientProxy.class);

    private RefreshFeedYtDao ytDao;

    @BeforeEach
    void setUp() {
        ytDao = new RefreshFeedYtDao(ytClient, YtClientProxySource.singleSource(ytClient), BINDING_TABLE);
    }

    @Test
    void getAllRefreshRecordsCreateRightQuery() {
        ytDao.getAllRefreshRecords();
        verify(ytClient).selectRows("feedId, etag, hash, httpCode, errorMessage, updatedAt from [T]",
                BINDING_TABLE.getBinder());
    }

    @Test
    void saveRefreshRecordSaveCorrectData() {
        RefreshFeedRecord refreshFeedRecord = RefreshFeedRecord.builder()
                .setFeedId(1)
                .setEtag("123")
                .setUpdatedAt(Instant.EPOCH)
                .setHash("321")
                .setHttpCode(200)
                .build();
        ytDao.saveRefreshRecord(refreshFeedRecord);
        verify(ytClient).insertRows("T", BINDING_TABLE.getBinder(), List.of(
                new YtRefreshFeedRecord(refreshFeedRecord)));
    }
}

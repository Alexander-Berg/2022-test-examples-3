package ru.yandex.market.common.report.indexer.yellow;

import java.io.InputStream;

import org.apache.http.entity.InputStreamEntity;
import org.junit.Test;

import ru.yandex.market.common.report.indexer.yellow.model.SessionFeedInfo;

import static org.junit.Assert.assertEquals;

/**
 * Тесты для {@link YellowIdxApiService}.
 */
public class YellowIdxApiServiceTest {

    @Test
    public void testParseFeedsInfo() {
        InputStream content = this.getClass().getResourceAsStream("/files/feed/parsed-feed-info.json");
        SessionFeedInfo actualResult = YellowIdxApiService.parse(new InputStreamEntity(content), SessionFeedInfo.class);
        checkResult(actualResult);
    }

    private void checkResult(SessionFeedInfo sessionFeedInfo) {
        assertEquals(676292, sessionFeedInfo.getFeedId().longValue());
        assertEquals(2670, sessionFeedInfo.getOffersCount().longValue());
        assertEquals(1575860640, sessionFeedInfo.getYmlDate().longValue());
    }
}

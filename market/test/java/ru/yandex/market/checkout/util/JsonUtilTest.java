package ru.yandex.market.checkout.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.report.model.FeedOfferId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author kukabara
 */
public class JsonUtilTest {

    @Test
    public void testNull() {
        assertNull(JsonUtil.convertFromString(null));
        assertNull(JsonUtil.convertFromString(""));

        assertNull(JsonUtil.convertToString(null));
    }

    @Test
    public void testReadWrite() {
        List<FeedOfferId> feedOfferIds = getFeedOfferIds();
        String str = JsonUtil.convertToString(feedOfferIds);
        List<FeedOfferId> readed = JsonUtil.convertFromString(str);
        assertEquals(feedOfferIds, readed);
    }

    private static List<FeedOfferId> getFeedOfferIds() {
        List<FeedOfferId> feedOfferIds = new ArrayList<>();
        feedOfferIds.add(new FeedOfferId("offerId", 1L));
        feedOfferIds.add(new FeedOfferId("offerId2", 2L));
        return feedOfferIds;
    }

    @Test
    public void testReadUnknown() {
        String saved = "[{\"id\":\"offerId\",\"feedId\":1, \"unknownParam\":2},{\"id\":\"offerId2\",\"feedId\":2}]";
        List<FeedOfferId> readed = JsonUtil.convertFromString(saved);
        assertNotNull(readed);
        assertEquals(getFeedOfferIds(), readed);
    }
}

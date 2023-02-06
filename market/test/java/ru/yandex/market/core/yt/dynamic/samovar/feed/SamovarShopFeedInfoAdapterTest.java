package ru.yandex.market.core.yt.dynamic.samovar.feed;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.feed.model.FeedInfo;
import ru.yandex.market.core.misc.resource.RemoteResource;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

import static ru.yandex.market.core.test.utils.SamovarFeedTestUtils.checkSamovarFeedInfo;

/**
 * Тесты для {@link SamovarShopFeedInfoAdapter}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class SamovarShopFeedInfoAdapterTest {

    private static final FeedInfo FEED = buildFeed(true);
    private static final FeedInfo EMPTY_FEED = buildFeed(false);

    @Test
    @DisplayName("Геттеры берут нужные поля")
    void testGetters() {
        final SamovarShopFeedInfoAdapter adapter = new SamovarShopFeedInfoAdapter(FEED, Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of());

        Assertions.assertEquals(FEED.getDatasourceId(), adapter.getPartnerId());
        Assertions.assertEquals(FEED.getReparseIntervalMinutes(), adapter.getPeriod());
        Assertions.assertNull(adapter.getTimeout());
        Assertions.assertEquals(RemoteResource.of("http://test.local"), adapter.getResource());
    }

    @Test
    @DisplayName("Таймаут и период могут быть null")
    void testNullGetters() {
        final SamovarShopFeedInfoAdapter adapter =
                new SamovarShopFeedInfoAdapter(EMPTY_FEED, Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

        Assertions.assertNull(adapter.getPeriod());
        Assertions.assertNull(adapter.getTimeout());
    }

    @Test
    @DisplayName("Построение FeedInfo без дополнительной информации")
    void testFeedInfoWithoutBusiness() {
        final SamovarShopFeedInfoAdapter adapter =
                new SamovarShopFeedInfoAdapter(FEED, Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

        final SamovarContextOuterClass.FeedInfo actual = adapter.toFeedInfo();
        final SamovarContextOuterClass.FeedInfo expected = ProtoTestUtil.getProtoMessageByJson(
                SamovarContextOuterClass.FeedInfo.class, "proto/shop.adapter1.json", getClass()
        );

        checkSamovarFeedInfo(expected, actual);
    }

    @Test
    @DisplayName("Построение FeedInfo с дополнительной информацией")
    void testFeedInfoWithBusiness() {
        final Map<Long, FeedAdditionalInfo> businessIds = Map.of(FEED.getId(), FeedAdditionalInfo.builder()
                .setBusinessId(500L)
                .setCampaignType(CampaignType.SHOP)
                .build()
        );
        final SamovarShopFeedInfoAdapter adapter =
                new SamovarShopFeedInfoAdapter(FEED, businessIds, Map.of(), Map.of(), Map.of(), Map.of());

        final SamovarContextOuterClass.FeedInfo actual = adapter.toFeedInfo();
        final SamovarContextOuterClass.FeedInfo expected = ProtoTestUtil.getProtoMessageByJson(
                SamovarContextOuterClass.FeedInfo.class, "proto/shop.adapter2.json", getClass()
        );

        checkSamovarFeedInfo(expected, actual);
    }

    @Test
    @DisplayName("Построение FeedInfo с использует CamapignType")
    void testFeedInfoUseCamapignType() {
        final Map<Long, FeedAdditionalInfo> businessIds = Map.of(FEED.getId(), FeedAdditionalInfo.builder()
                .setBusinessId(500L)
                .setCampaignType(CampaignType.DIRECT)
                .build()
        );
        final SamovarShopFeedInfoAdapter adapter =
                new SamovarShopFeedInfoAdapter(FEED, businessIds, Map.of(), Map.of(), Map.of(), Map.of());

        final SamovarContextOuterClass.FeedInfo actual = adapter.toFeedInfo();
        final SamovarContextOuterClass.FeedInfo expected = ProtoTestUtil.getProtoMessageByJson(
                SamovarContextOuterClass.FeedInfo.class, "proto/shop.adapter3.json", getClass()
        );

        checkSamovarFeedInfo(expected, actual);
    }

    private static FeedInfo buildFeed(boolean withTime) {
        final FeedInfo feedInfo = new FeedInfo();
        feedInfo.setId(100L);
        feedInfo.setDefault(false);
        feedInfo.setUrl("http://test.local");
        feedInfo.setDatasourceId(400L);

        if (withTime) {
            feedInfo.setReparseIntervalMinutes(200);
        }

        return feedInfo;
    }
}

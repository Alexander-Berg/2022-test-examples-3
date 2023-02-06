package ru.yandex.market.core.yt.dynamic.samovar.feed;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.delivery.model.FeedWarehouseMapping;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.core.feed.supplier.model.SupplierFeed;
import ru.yandex.market.core.misc.resource.RemoteResource;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

/**
 * Тесты для {@link SamovarSupplierFeedInfoAdapter}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class SamovarSupplierFeedInfoAdapterTest {

    private static final SupplierFeed FEED = buildFeed(true);
    private static final SupplierFeed EMPTY_FEED = buildFeed(false);

    @Test
    @DisplayName("Геттеры берут нужные поля")
    void testGetters() {
        final SamovarSupplierFeedInfoAdapter adapter =
                new SamovarSupplierFeedInfoAdapter(FEED, FeedType.ASSORTMENT, Map.of(), Map.of(), Map.of(), Map.of(),
                        Map.of(400L,true));

        Assertions.assertEquals(FEED.getSupplierId(), adapter.getPartnerId());
        Assertions.assertEquals(FEED.getPeriod(), adapter.getPeriod());
        Assertions.assertEquals(FEED.getTimeout(), adapter.getTimeout());
        Assertions.assertEquals(FEED.getResource(), adapter.getResource());
        Assertions.assertTrue(adapter.toFeedInfo().getIsPartnerInterface());
    }

    @Test
    @DisplayName("Таймаут и период могут быть null")
    void testNullGetters() {
        final SamovarSupplierFeedInfoAdapter adapter = new SamovarSupplierFeedInfoAdapter(EMPTY_FEED,
                FeedType.ASSORTMENT, Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

        Assertions.assertNull(adapter.getPeriod());
        Assertions.assertNull(adapter.getTimeout());
    }

    @Test
    @DisplayName("Построение FeedInfo без складов")
    void testFeedInfoWithoutWarehouse() {
        final SamovarSupplierFeedInfoAdapter adapter = new SamovarSupplierFeedInfoAdapter(FEED, FeedType.ASSORTMENT,
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

        final SamovarContextOuterClass.FeedInfo actual = adapter.toFeedInfo();
        final SamovarContextOuterClass.FeedInfo expected =
                ProtoTestUtil.getProtoMessageByJson(SamovarContextOuterClass.FeedInfo.class, "proto/supplier.adapter1" +
                        ".json", getClass());
        ProtoTestUtil.assertThat(actual)
                .ignoringFieldsMatchingRegexes(".*updatedAt.*")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Построение FeedInfo со складами")
    void testFeedInfoWithWarehouse() {
        final var warehouses = Map.of(FEED.getSupplierId(), List.of(new FeedWarehouseMapping(1L, 2,
                DeliveryServiceType.DROPSHIP, "5")));
        final var businessIds = Map.of(FEED.getSupplierId(), 14L);
        final SamovarSupplierFeedInfoAdapter adapter = new SamovarSupplierFeedInfoAdapter(FEED, FeedType.ASSORTMENT,
                businessIds, warehouses, Map.of(), Map.of(), Map.of());

        final SamovarContextOuterClass.FeedInfo actual = adapter.toFeedInfo();
        final SamovarContextOuterClass.FeedInfo expected =
                ProtoTestUtil.getProtoMessageByJson(SamovarContextOuterClass.FeedInfo.class, "proto/supplier.adapter2" +
                        ".json", getClass());
        ProtoTestUtil.assertThat(actual)
                .ignoringFieldsMatchingRegexes(".*updatedAt.*")
                .isEqualTo(expected);
    }

    private static SupplierFeed buildFeed(boolean withTime) {
        final SupplierFeed.Builder builder = new SupplierFeed.Builder()
                .setId(100L)
                .setIsDefault(false)
                .setUpdatedAt(DateTimes.toInstant(2020, 1, 1))
                .setResource(RemoteResource.of("http://test.local"))
                .setSupplierId(400L)
                .setBusinessId(666L);

        if (withTime) {
            builder.setPeriod(200)
                    .setTimeout(300);
        }

        return builder.build();
    }
}

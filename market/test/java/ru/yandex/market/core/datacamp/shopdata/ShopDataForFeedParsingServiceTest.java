package ru.yandex.market.core.datacamp.shopdata;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.API.UpdateTask;
import Market.DataCamp.DataCampOfferMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.datacamp.FeedParsingType;
import ru.yandex.market.core.feed.event.PartnerParsingFeedEvent;
import ru.yandex.market.core.feed.model.FeedType;

/**
 * Проверяем {@link ShopDataForFeedParsingService}.
 */
@DbUnitDataSet(before = {
        "ShopDataForFeedParsingServiceTest.before.csv",
        "ShopDataForFeedParsing.regions.before.csv"
})
class ShopDataForFeedParsingServiceTest extends FunctionalTest {

    @Autowired
    private ShopDataForFeedParsingService shopDataForFeedParsingService;

    @ParameterizedTest(name = "feed: {0}, shop: {1}")
    @MethodSource("args")
    void getShopDataInfoObject(long feedId, long shopId, PartnerParsingFeedEvent event, UpdateTask.ShopsDatParameters expected) {
        checkShopsDat(event, expected);
    }

    @Test
    @DisplayName("Батчевое получение шопсдаты")
    void batchSelect() {
        List<PartnerParsingFeedEvent> events = args()
                .map(e -> (PartnerParsingFeedEvent) e.get()[2])
                .collect(Collectors.toList());
        Map<Long, UpdateTask.ShopsDatParameters> actual = shopDataForFeedParsingService.getDataForParsingFeed(events);

        Map<Long, UpdateTask.ShopsDatParameters> expected = args()
                .filter(e -> e.get()[3] != null)
                .collect(Collectors.toMap(e -> (long) e.get()[0], e -> ((UpdateTask.ShopsDatParameters) e.get()[3]).toBuilder().setUrl(((PartnerParsingFeedEvent) e.get()[2]).getOriginalUrl()).build(), (a, b) -> a));
        Assertions.assertEquals(expected, actual);
    }

    private void checkShopsDat(PartnerParsingFeedEvent event, UpdateTask.ShopsDatParameters expected) {
        Optional<UpdateTask.ShopsDatParameters> actual = shopDataForFeedParsingService.getDataForParsingFeed(event);
        if (actual.isEmpty()) {
            Assertions.assertNull(expected);
            return;
        }

        ProtoTestUtil.assertThat(actual.get())
                .isEqualTo(expected);
    }

    private static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(645L, 1L, PartnerParsingFeedEvent.builder()
                        .withRealFeedId(645L)
                        .withPartnerId(1L)
                        .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                        .withFeedType(FeedType.ASSORTMENT)
                        .build(), null),
                Arguments.of(100L, 10L,
                        PartnerParsingFeedEvent.builder()
                                .withRealFeedId(100L)
                                .withPartnerId(10L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .withFeedType(FeedType.ASSORTMENT)
                                .withIsUpload(false)
                                .withOriginalUrl("http://url-from-parsing-event.local")
                                .build(),
                        UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.WHITE)
                                .setIsSiteMarket(true)
                                .setVat(7)
                                .setIsDiscountsEnabled(true)
                                .setUrl("http://url-from-parsing-event.local")
                                .setIsUpload(false)
                                .setIsMock(false)
                                .setLocalRegionTzOffset(10800)
                                .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_NO)
                                .build()
                ),
                Arguments.of(102L, 11L,
                        PartnerParsingFeedEvent.builder()
                                .withRealFeedId(102L)
                                .withPartnerId(11L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .withFeedType(FeedType.ASSORTMENT)
                                .withIsUpload(true)
                                .withOriginalUrl("http://mds.url")
                                .withUploadUrl("http://mds.url")
                                .build(),
                        UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.WHITE)
                                .setIsSiteMarket(true)
                                .setIsDiscountsEnabled(false)
                                .setUrl("http://mds.url")
                                .setIsUpload(true)
                                .setIsMock(false)
                                .setLocalRegionTzOffset(25200)
                                .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_NO)
                                .build()),
                Arguments.of(103L, 12L,
                        PartnerParsingFeedEvent.builder()
                                .withRealFeedId(103L)
                                .withPartnerId(12L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .withFeedType(FeedType.ASSORTMENT)
                                .withIsUpload(false)
                                .withOriginalUrl("http://feed-url-3.com")
                                .build(),
                        UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.WHITE)
                                .setIsSiteMarket(true)
                                .setIsDiscountsEnabled(true)
                                .setUrl("http://feed-url-3.com")
                                .setIsUpload(false)
                                .setIsMock(true)
                                .setLocalRegionTzOffset(10800)
                                .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_REAL)
                                .build()),
                Arguments.of(104L, 12L,
                        PartnerParsingFeedEvent.builder()
                                .withRealFeedId(104L)
                                .withPartnerId(12L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .withFeedType(FeedType.ASSORTMENT)
                                .withIsUpload(false)
                                .withOriginalUrl("http://feed-url-4.com")
                                .build(),
                        UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.WHITE)
                                .setIsSiteMarket(true)
                                .setIsDiscountsEnabled(true)
                                .setUrl("http://feed-url-4.com")
                                .setIsUpload(false)
                                .setIsMock(true)
                                .setLocalRegionTzOffset(10800)
                                .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_REAL)
                                .build()),
                Arguments.of(104L, 12L,
                        PartnerParsingFeedEvent.builder()
                                .withRealFeedId(104L)
                                .withPartnerId(12L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .withFeedType(FeedType.PRICES)
                                .withIsUpload(false)
                                .withOriginalUrl("http://feed-url-4.com")
                                .build(),
                        UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.WHITE)
                                .setIsSiteMarket(true)
                                .setIsDiscountsEnabled(true)
                                .setUrl("http://feed-url-4.com")
                                .setIsUpload(false)
                                .setIsMock(true)
                                .setLocalRegionTzOffset(10800)
                                .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_REAL)
                                .build())
        );
    }
}

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
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.datacamp.FeedParsingType;
import ru.yandex.market.core.feed.event.PartnerParsingFeedEvent;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.core.supplier.model.SupplierType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Проверяем {@link SupplierDataForFeedParsingService}.
 */
@DbUnitDataSet(before = {
        "SupplierDataInfoForFeedParsingServiceTest.before.csv",
        "ShopDataForFeedParsing.regions.before.csv"
})
class SupplierDataInfoForFeedParsingServiceTest extends FunctionalTest {

    @Autowired
    private SupplierDataForFeedParsingService supplierDataForFeedParsingService;

    @ParameterizedTest(name = "feed: {0}, shop: {1}")
    @MethodSource("args")
    void getSupplierDataInfoObject(long feedId, long supplierId, PartnerParsingFeedEvent event, Optional<UpdateTask.ShopsDatParameters> expected) {
        assertEquals(expected, supplierDataForFeedParsingService.getDataForParsingFeed(event));
    }

    @Test
    @DisplayName("Батчевое получение шопсдаты")
    void batchSelect() {
        List<PartnerParsingFeedEvent> events = args()
                .map(e -> (PartnerParsingFeedEvent) e.get()[2])
                .collect(Collectors.toList());
        Map<Long, UpdateTask.ShopsDatParameters> actual = supplierDataForFeedParsingService.getDataForParsingFeed(events);

        Map<Long, UpdateTask.ShopsDatParameters> expected = args()
                .filter(e -> ((Optional<UpdateTask.ShopsDatParameters>) e.get()[3]).isPresent())
                .collect(Collectors.toMap(e -> (long) e.get()[0], e -> ((Optional<UpdateTask.ShopsDatParameters>) e.get()[3]).get(), (a, b) -> a));
        Assertions.assertEquals(expected, actual);
    }

    private static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(645L, 1L,
                        PartnerParsingFeedEvent.builder()
                                .withRealFeedId(645L)
                                .withPartnerId(1L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .withFeedType(FeedType.ASSORTMENT)
                                .build(),
                        Optional.empty()),
                Arguments.of(30000L, 10L,
                        PartnerParsingFeedEvent.builder()
                                .withRealFeedId(30000L)
                                .withPartnerId(10L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .withFeedType(FeedType.ASSORTMENT)
                                .withIsUpload(false)
                                .withOriginalUrl("https://market-mbi-prod.s3.mds.yandex.net/supplier-feed/suppliers/465984/feeds/497423/data")
                                .build(),
                        Optional.of(UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.BLUE)
                                .setIsSiteMarket(true)
                                .setVat(7)
                                .setIsDiscountsEnabled(true)
                                .setUrl("https://market-mbi-prod.s3.mds.yandex.net/supplier-feed/suppliers/465984/feeds/497423/data")
                                .setIsUpload(false)
                                .setBlueStatus("REAL")
                                .setSupplierType(SupplierType.THIRD_PARTY.getId().toString())
                                .setIsMock(false)
                                .setLocalRegionTzOffset(10800)
                                .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_REAL)
                                .build())
                ),
                Arguments.of(30001L, 11L,
                        PartnerParsingFeedEvent.builder()
                                .withRealFeedId(30001L)
                                .withPartnerId(11L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .withFeedType(FeedType.ASSORTMENT)
                                .withIsUpload(true)
                                .withOriginalUrl("https://market-mbi-prod.s3.mds.yandex.net/supplier-feed/suppliers/465984/feeds/497423/data")
                                .build(),
                        Optional.of(UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.BLUE)
                                .setIsSiteMarket(true)
                                .setIsDiscountsEnabled(true)
                                .setUrl("https://market-mbi-prod.s3.mds.yandex.net/supplier-feed/suppliers/465984/feeds/497423/data")
                                .setIsUpload(true)
                                .setBlueStatus("REAL")
                                .setSupplierType(SupplierType.FIRST_PARTY.getId().toString())
                                .setIsMock(false)
                                .setLocalRegionTzOffset(25200)
                                .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_REAL)
                                .build())),
                Arguments.of(30002L, 13L,
                        PartnerParsingFeedEvent.builder()
                                .withRealFeedId(30002L)
                                .withPartnerId(13L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .withFeedType(FeedType.ASSORTMENT)
                                .withIsUpload(true)
                                .withOriginalUrl("https://market-mbi-prod.s3.mds.yandex.net/supplier-feed/suppliers/465984/feeds/497423/data")
                                .withUploadUrl("https://market-mbi-prod.s3.mds.yandex.net/supplier-feed/suppliers/465984/feeds/497423/data")
                                .build(),
                        Optional.of(UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.BLUE)
                                .setIsSiteMarket(true)
                                .setIsDiscountsEnabled(true)
                                .setUrl("https://market-mbi-prod.s3.mds.yandex.net/supplier-feed/suppliers/465984/feeds/497423/data")
                                .setIsUpload(true)
                                .setBlueStatus("REAL")
                                .setSupplierType(SupplierType.THIRD_PARTY.getId().toString())
                                .setIsMock(false)
                                .setLocalRegionTzOffset(10800)
                                .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_REAL)
                                .build())),
                // не верный partnerID - ничего не должно вернуться
                // feedId -верный
                Arguments.of(30002L, 100500L,
                        PartnerParsingFeedEvent.builder()
                                .withRealFeedId(30002L)
                                .withPartnerId(100500L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .withFeedType(FeedType.ASSORTMENT)
                                .build(),
                        Optional.empty()),
                //фид с фтп ссылкой без авторизации
                Arguments.of(30010L, 14L,
                        PartnerParsingFeedEvent.builder()
                                .withRealFeedId(30010L)
                                .withPartnerId(14L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .withFeedType(FeedType.ASSORTMENT)
                                .withIsUpload(false)
                                .withOriginalUrl("ftp://market-mbi-prod.s3.mds.yandex.net/supplier-feed/suppliers/465984/feeds/497423/data")
                                .build(),
                        Optional.of(UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.BLUE)
                                .setIsSiteMarket(true)
                                .setIsDiscountsEnabled(true)
                                .setUrl("ftp://market-mbi-prod.s3.mds.yandex.net/supplier-feed/suppliers/465984/feeds/497423/data")
                                .setIsUpload(false)
                                .setBlueStatus("REAL")
                                .setSupplierType(SupplierType.THIRD_PARTY.getId().toString())
                                .setIsMock(false)
                                .setLocalRegionTzOffset(10800)
                                .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_REAL)
                                .build())),
                //фид с фтп ссылкой c авторизацией
                Arguments.of(30015L, 15L,
                        PartnerParsingFeedEvent.builder()
                                .withRealFeedId(30015L)
                                .withPartnerId(15L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .withFeedType(FeedType.ASSORTMENT)
                                .withIsUpload(true)
                                .withOriginalUrl("ftp://loginchick:1@market-mbi-prod.s3.mds.yandex.net/supplier-feed/suppliers/465984/feeds/497423/data")
                                .build(),
                        Optional.of(UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.BLUE)
                                .setIsSiteMarket(true)
                                .setIsDiscountsEnabled(true)
                                .setUrl("ftp://loginchick:1@market-mbi-prod.s3.mds.yandex.net/supplier-feed/suppliers/465984/feeds/497423/data")
                                .setIsUpload(true)
                                .setBlueStatus("REAL")
                                .setSupplierType(SupplierType.THIRD_PARTY.getId().toString())
                                .setIsMock(false)
                                .setLocalRegionTzOffset(10800)
                                .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_REAL)
                                .build())),

                Arguments.of(30002L, 13L,
                        PartnerParsingFeedEvent.builder()
                                .withRealFeedId(30002L)
                                .withPartnerId(13L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .withFeedType(FeedType.PRICES)
                                .withIsUpload(true)
                                .withOriginalUrl("https://market-mbi-prod.s3.mds.yandex.net/supplier-feed/suppliers/465984/feeds/497423/data")
                                .withUploadUrl("https://market-mbi-prod.s3.mds.yandex.net/supplier-feed/suppliers/465984/feeds/497423/data")
                                .build(),
                        Optional.of(UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.BLUE)
                                .setIsSiteMarket(true)
                                .setIsDiscountsEnabled(true)
                                .setUrl("https://market-mbi-prod.s3.mds.yandex.net/supplier-feed/suppliers/465984/feeds/497423/data")
                                .setIsUpload(true)
                                .setBlueStatus("REAL")
                                .setSupplierType(SupplierType.THIRD_PARTY.getId().toString())
                                .setIsMock(false)
                                .setLocalRegionTzOffset(10800)
                                .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_REAL)
                                .build()))
        );
    }
}

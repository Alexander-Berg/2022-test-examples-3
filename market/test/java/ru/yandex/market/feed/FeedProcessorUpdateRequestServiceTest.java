package ru.yandex.market.feed;

import java.util.List;

import Market.DataCamp.API.UpdateTask;
import Market.DataCamp.DataCampOfferMeta;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestService;
import ru.yandex.market.core.feed.datacamp.FeedParsingType;
import ru.yandex.market.core.feed.event.PartnerParsingFeedEvent;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.core.misc.resource.ResourceAccessCredentials;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarFeed;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.feed.processor.parsing.FeedUpdateTaskOuterClass;
import ru.yandex.market.shop.FunctionalTest;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

/**
 * Тесты для {@link FeedProcessorUpdateRequestService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class FeedProcessorUpdateRequestServiceTest extends FunctionalTest {

    @Autowired
    private FeedProcessorUpdateRequestService feedProcessorUpdateRequestService;

    @Autowired
    private LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher;

    @AfterEach
    void checkMocks() {
        Mockito.verifyNoMoreInteractions(feedProcessorUpdateLogbrokerEventPublisher);
    }

    @Test
    @DisplayName("Ивент на парсинг фида по ссылке")
    @DbUnitDataSet
    void testEventFromSamovar() {
        SamovarFeed samovarFeed = SamovarFeed.builder()
                .setContext(SamovarContextOuterClass.SamovarContext.newBuilder()
                        .setEnvironment("DEVELOPMENT")
                        .setRequestId("111/qqq")
                        .addFeeds(SamovarContextOuterClass.FeedInfo.newBuilder()
                                .setBusinessId(100)
                                .setShopId(200)
                                .setCampaignType("SHOP")
                                .setFeedId(300)
                                .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                .setUrl("http://url1.ru")
                                .setUpdatedAt(DateTimes.toTimestamp(DateTimes.toInstant(2020, 1, 1)))
                                .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                        .setColor(DataCampOfferMeta.MarketColor.WHITE)
                                        .build())
                                .addPlacementPrograms(SamovarContextOuterClass.FeedInfo.PlacementProgram.newBuilder()
                                        .setType(1)
                                        .build())
                                .addWarehouses(SamovarContextOuterClass.FeedInfo.WarehouseInfo.newBuilder()
                                        .setType("fulfillment")
                                        .setWarehouseId(123)
                                        .setFeedId(500)
                                        .setExternalId("ext_id")
                                        .build())
                                .build())
                        .build())
                .setCredentials(ResourceAccessCredentials.of("log_123", "pass_123"))
                .setEnabled(true)
                .setUrl("http://url1.ru")
                .build();

        feedProcessorUpdateRequestService.sendRequestToUpdate(samovarFeed);

        ArgumentCaptor<FeedProcessorUpdateRequestEvent> captor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        Mockito.verify(feedProcessorUpdateLogbrokerEventPublisher, Mockito.times(1)).publishEvent(captor.capture());

        List<FeedProcessorUpdateRequestEvent> actualEvents = captor.getAllValues();
        Assertions.assertThat(actualEvents).hasSize(1);
        FeedProcessorUpdateRequestEvent actualEvent = actualEvents.get(0);
        ProtoTestUtil.assertThat(actualEvent.getPayload())
                .isEqualTo(FeedUpdateTaskOuterClass.FeedUpdateTask.newBuilder()
                        .setFeed(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedInfo.newBuilder()
                                .setBusinessId(100)
                                .setShopId(200)
                                .setCampaignType("SHOP")
                                .setFeedId(300)
                                .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                .setUrl("http://url1.ru")
                                .setLogin("log_123")
                                .setPassword("pass_123")
                                .setUpload(false)
                                .build())
                        .setRequestedAt(DateTimes.toTimestamp(DateTimes.toInstant(2020, 1, 1)))
                        .setFeedParsingType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.COMPLETE)
                        .setIsRegularParsing(true)
                        .setRequestId("111/qqq")
                        .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.WHITE)
                                .build())
                        .addPlacementPrograms(SamovarContextOuterClass.FeedInfo.PlacementProgram.newBuilder()
                                .setType(1)
                                .build())
                        .addWarehouses(SamovarContextOuterClass.FeedInfo.WarehouseInfo.newBuilder()
                                .setType("fulfillment")
                                .setWarehouseId(123)
                                .setFeedId(500)
                                .setExternalId("ext_id")
                                .build())
                        .setFeedUpdateType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE)
                        .build());
    }

    @Test
    @DisplayName("Отправка на парсинг через листенер обновления фида")
    @DbUnitDataSet(before = {
            "FeedProcessorUpdateRequestServiceTest/testEventToDataCamp.before.csv"
    })
    void testEventToDataCamp() {
        PartnerParsingFeedEvent event = PartnerParsingFeedEvent.builder()
                .withBusinessId(100L)
                .withPartnerId(200)
                .withFeedId(300L)
                .withFeedType(FeedType.ASSORTMENT)
                .withFeedParsingType(FeedParsingType.UPDATE_FEED)
                .withIsUpload(true)
                .withOriginalUrl("http://mds.ru")
                .withUploadUrl("http://mds.ru")
                .withSendTime(DateTimes.toInstant(2020, 1, 1))
                .build();

        feedProcessorUpdateRequestService.sendRequestToUpdate(event);

        ArgumentCaptor<FeedProcessorUpdateRequestEvent> captor =
                ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        Mockito.verify(feedProcessorUpdateLogbrokerEventPublisher, Mockito.times(1)).publishEvent(captor.capture());

        List<FeedProcessorUpdateRequestEvent> actualEvents = captor.getAllValues();
        Assertions.assertThat(actualEvents).hasSize(1);
        FeedProcessorUpdateRequestEvent actualEvent = actualEvents.get(0);
        ProtoTestUtil.assertThat(actualEvent.getPayload())
                .isEqualTo(FeedUpdateTaskOuterClass.FeedUpdateTask.newBuilder()
                        .setFeed(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedInfo.newBuilder()
                                .setBusinessId(100)
                                .setShopId(200)
                                .setCampaignType("SHOP")
                                .setFeedId(300)
                                .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                .setUrl("http://mds.ru")
                                .setUpload(true)
                                .build())
                        .setRequestedAt(DateTimes.toTimestamp(DateTimes.toInstant(2020, 1, 1)))
                        .setFeedParsingType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.UPDATE)
                        .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.WHITE)
                                .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_NO)
                                .setIsDiscountsEnabled(true)
                                .setIsSiteMarket(true)
                                .setLocalRegionTzOffset(10800)
                                .setUrl("http://mds.ru")
                                .setIsUpload(true)
                                .build())
                        .addPlacementPrograms(SamovarContextOuterClass.FeedInfo.PlacementProgram.newBuilder()
                                .setType(6)
                                .build())
                        .addWarehouses(SamovarContextOuterClass.FeedInfo.WarehouseInfo.newBuilder()
                                .setType("dropship_by_seller")
                                .setWarehouseId(123)
                                .setFeedId(500)
                                .setExternalId("ext_id")
                                .build())
                        .setFeedUpdateType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE)
                        .setIsPartnerInterface(true)
                        .build());
    }


    @Test
    @DisplayName("Отправка на парсинг через листенер обновления фида с указанием полей для парсинга")
    @DbUnitDataSet(before = {
            "FeedProcessorUpdateRequestServiceTest/testEventToDataCamp.before.csv"
    })
    void testEventToDataCampWithParsingFields() {
        var parsingFields = List.of("id", "price", "adult");
        PartnerParsingFeedEvent event = PartnerParsingFeedEvent.builder()
                .withBusinessId(100L)
                .withPartnerId(200)
                .withFeedId(300L)
                .withFeedType(FeedType.ASSORTMENT)
                .withFeedParsingType(FeedParsingType.UPDATE_FEED)
                .withParsingFields(parsingFields)
                .withIsUpload(true)
                .withOriginalUrl("http://mds.ru")
                .withUploadUrl("http://mds.ru")
                .withSendTime(DateTimes.toInstant(2020, 1, 1))
                .build();

        feedProcessorUpdateRequestService.sendRequestToUpdate(event);

        ArgumentCaptor<FeedProcessorUpdateRequestEvent> captor =
                ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        Mockito.verify(feedProcessorUpdateLogbrokerEventPublisher, Mockito.times(1)).publishEvent(captor.capture());

        List<FeedProcessorUpdateRequestEvent> actualEvents = captor.getAllValues();
        Assertions.assertThat(actualEvents).hasSize(1);
        FeedProcessorUpdateRequestEvent actualEvent = actualEvents.get(0);
        ProtoTestUtil.assertThat(actualEvent.getPayload())
                .isEqualTo(FeedUpdateTaskOuterClass.FeedUpdateTask.newBuilder()
                        .setFeed(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedInfo.newBuilder()
                                .setBusinessId(100)
                                .setShopId(200)
                                .setCampaignType("SHOP")
                                .setFeedId(300)
                                .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                .setUrl("http://mds.ru")
                                .setUpload(true)
                                .build())
                        .setRequestedAt(DateTimes.toTimestamp(DateTimes.toInstant(2020, 1, 1)))
                        .setFeedParsingType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.UPDATE)
                        .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                .setColor(DataCampOfferMeta.MarketColor.WHITE)
                                .setCpa(UpdateTask.ProgramStatus.PROGRAM_STATUS_NO)
                                .setIsDiscountsEnabled(true)
                                .setIsSiteMarket(true)
                                .setLocalRegionTzOffset(10800)
                                .setUrl("http://mds.ru")
                                .setIsUpload(true)
                                .build())
                        .addPlacementPrograms(SamovarContextOuterClass.FeedInfo.PlacementProgram.newBuilder()
                                .setType(6)
                                .build())
                        .addWarehouses(SamovarContextOuterClass.FeedInfo.WarehouseInfo.newBuilder()
                                .setType("dropship_by_seller")
                                .setWarehouseId(123)
                                .setFeedId(500)
                                .setExternalId("ext_id")
                                .build())
                        .setFeedUpdateType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE)
                        .setIsPartnerInterface(true)
                        .addAllParsingFields(parsingFields)
                        .build());
    }


    @Test
    @DisplayName("Отправка на парсинг через листенер обновления бизнесовго фида с указанием полей для парсинга")
    @DbUnitDataSet(before = {
            "FeedProcessorUpdateRequestServiceTest/testEventToDataCamp.before.csv"
    })
    void testBusinessEventToDataCampWithParsingFields() {
        var parsingFields = List.of("id", "price", "adult");
        PartnerParsingFeedEvent event = PartnerParsingFeedEvent.builder()
                .withBusinessId(100L)
                .withPartnerId(100)
                .withFeedId(301L)
                .withFeedType(FeedType.ASSORTMENT)
                .withFeedParsingType(FeedParsingType.UPDATE_FEED)
                .withParsingFields(parsingFields)
                .withIsUpload(true)
                .withOriginalUrl("http://mds.ru")
                .withUploadUrl("http://mds.ru")
                .withSendTime(DateTimes.toInstant(2020, 1, 1))
                .build();

        feedProcessorUpdateRequestService.sendRequestToUpdate(event);

        ArgumentCaptor<FeedProcessorUpdateRequestEvent> captor =
                ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        Mockito.verify(feedProcessorUpdateLogbrokerEventPublisher, Mockito.times(1)).publishEvent(captor.capture());

        List<FeedProcessorUpdateRequestEvent> actualEvents = captor.getAllValues();
        Assertions.assertThat(actualEvents).hasSize(1);
        FeedProcessorUpdateRequestEvent actualEvent = actualEvents.get(0);
        ProtoTestUtil.assertThat(actualEvent.getPayload())
                .isEqualTo(FeedUpdateTaskOuterClass.FeedUpdateTask.newBuilder()
                        .setFeed(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedInfo.newBuilder()
                                .setBusinessId(100)
                                .setShopId(100)
                                .setCampaignType("BUSINESS")
                                .setFeedId(301)
                                .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                .setUrl("http://mds.ru")
                                .setUpload(true)
                                .build())
                        .setRequestedAt(DateTimes.toTimestamp(DateTimes.toInstant(2020, 1, 1)))
                        .setFeedParsingType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.UPDATE)
                        .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                .setIsUpload(true)
                                .build())
                        .setFeedUpdateType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE)
                        .addAllParsingFields(parsingFields)
                        .build());
    }
}

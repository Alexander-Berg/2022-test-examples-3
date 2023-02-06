package ru.yandex.market.core.feed.event;

import java.time.Instant;

import Market.DataCamp.API.UpdateTask;
import Market.DataCamp.DataCampOfferMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestService;
import ru.yandex.market.core.feed.FeedRefreshService;
import ru.yandex.market.core.feed.datacamp.FeedParsingType;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.feed.processor.parsing.FeedUpdateTaskOuterClass;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Проверка иницииации переключения партнеров {@link DataCampCreateUpdateFeedEventListener}.
 */
@DbUnitDataSet(before = "DataCampCreateUpdateFeedEventListenerTest.before.csv")
class DataCampCreateUpdateFeedEventListenerTest extends FunctionalTest {

    public static final PartnerParsingFeedEvent DIRECT_PARTNER_PARSING_EVENT = new PartnerParsingFeedEvent.Builder()
            .withFeedId(34L)
            .withBusinessId(33L)
            .withPartnerId(31)
            .withOriginalUrl("http://ditrct.url.remote")
            .withUploadUrl("http://ditrct.url.local")
            .withActionId(1L)
            .withIsUpload(true)
            .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
            .withFeedType(FeedType.ASSORTMENT)
            .withSendTime(Instant.now())
            .build();
    public static final long DIRECT_FEED_ID = 26666L;
    public static final long DIRECT_CLIENT_ID = 15555L;

    @Autowired
    private CampaignService campaignService;
    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;
    @Autowired
    private FeedRefreshService feedRefreshService;
    @Autowired
    private FeedProcessorUpdateRequestService feedProcessorUpdateRequestService;

    private DataCampCreateUpdateFeedEventListener dataCampCreateUpdateFeedEventListener;

    @Autowired
    private LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher;

    @BeforeEach
    void init() {
        dataCampCreateUpdateFeedEventListener =
                new DataCampCreateUpdateFeedEventListener(
                        campaignService,
                        feedRefreshService,
                        partnerTypeAwareService,
                        feedProcessorUpdateRequestService
                );
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void checkSendEventToDatacamp() {
        dataCampCreateUpdateFeedEventListener.onApplicationEvent(DIRECT_PARTNER_PARSING_EVENT);

        var argumentCaptor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);

        verify(feedProcessorUpdateLogbrokerEventPublisher, times(1)).publishEvent(argumentCaptor.capture());
        FeedUpdateTaskOuterClass.FeedUpdateTask task = argumentCaptor.getValue().getPayload();
        ProtoTestUtil.assertThat(task)
                .ignoringFieldsMatchingRegexes(".*timestamp_.*", ".*requestedAt.*", ".*requestId.*")
                .isEqualTo(
                        FeedUpdateTaskOuterClass.FeedUpdateTask.newBuilder()
                                .setFeed(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedInfo.newBuilder()
                                        .setCampaignType("DIRECT")
                                        .setUpload(true)
                                        .setFeedId(DIRECT_PARTNER_PARSING_EVENT.getFeedId().intValue())
                                        .setUrl(DIRECT_PARTNER_PARSING_EVENT.getUploadUrl())
                                        .setShopId((int) DIRECT_PARTNER_PARSING_EVENT.getPartnerId())
                                        .setBusinessId(DIRECT_PARTNER_PARSING_EVENT.getBusinessId().intValue())
                                        .setFeedType(SamovarContextOuterClass.FeedInfo.FeedType.ASSORTMENT)
                                        .build())
                                .setShopsDatParameters(UpdateTask.ShopsDatParameters.newBuilder()
                                        .setColor(DataCampOfferMeta.MarketColor.DIRECT)
                                        .setDirectFeedId(DIRECT_FEED_ID)
                                        .setClientId(DIRECT_CLIENT_ID)
                                        .setVerticalShare(true)
                                        .setIsUpload(true)
                                        .setDirectStandby(true)
                                        .setDirectSearchSnippetGallery(true)
                                        .setDirectGoodsAds(true)
                                        .build())
                                .setFeedParsingType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.COMPLETE)
                                .setFeedUpdateType(FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE)
                                .build()
                );
    }
}

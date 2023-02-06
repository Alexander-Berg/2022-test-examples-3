package ru.yandex.direct.logicprocessor.processors.moderation.banner.support;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.model.ModerationableBanner;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.moderation.ModerationOperationMode;
import ru.yandex.direct.core.entity.moderation.ModerationOperationModeProvider;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.BaseBannerModerationData;
import ru.yandex.direct.core.entity.moderation.service.sending.BaseBannerSender;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.ess.logicobjects.moderation.banner.BannerModerationEventsObject;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.utils.FunctionalUtils.flatMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;


class BannerModerationEventsHandlerTest {

    private static final int SHARD = 1;
    private static final Long CAMPAIGN_ID = 10L;
    private static final Long ADGROUP_ID = 21L;
    private static final Long BANNER_ID = 32L;
    private BaseBannerSender<BannerModerationRequest<BannerModerationMeta, BaseBannerModerationData>,
            ModerationableBanner,
            BannerModerationMeta> sender;
    private Consumer<List<BannerModerationRequest<BannerModerationMeta, BaseBannerModerationData>>> requestConsumer;

    private ModerationOperationModeProvider moderationOperationModeProvider;

    @BeforeEach
    void before() {
        sender = Mockito.mock(BaseBannerSender.class);
        requestConsumer = Mockito.mock(Consumer.class);
        moderationOperationModeProvider = Mockito.mock(ModerationOperationModeProvider.class);
        when(moderationOperationModeProvider.getMode(any())).thenReturn(ModerationOperationMode.NORMAL);
    }

    @Test
    @SuppressWarnings("unchecked")
    void makeRequest_bannerTypeNotMatched_SendNoneBanner() {
        BannerModerationEventsHandler handler = new BannerModerationEventsHandler.Builder()
                .addBannerModerationSupport(BannerModerationSupport.forAllEvents(BannersBannerType.cpm_banner,
                        sender, requestConsumer))
                .setModerationOperationModeProvider(moderationOperationModeProvider)
                .build();

        handleObjectsMakeRequestNotCalled(handler,
                Collections.singletonList(textBannerModerationEventsWithInfo()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void makeRequest_noImage_SendNoneBanner() {
        BannerModerationEventsHandler handler = new BannerModerationEventsHandler.Builder()
                .addBannerModerationSupport(new BannerModerationSupport.Builder()
                        .setBannerType(BannersBannerType.cpm_banner)
                        .hasImage(true)
                        .setSender(sender)
                        .setRequestConsumer(requestConsumer)
                        .build()
                )
                .setModerationOperationModeProvider(moderationOperationModeProvider)
                .build();

        handleObjectsMakeRequestNotCalled(handler,
                Collections.singletonList(cpmBannerModerationEventsWithInfo()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void makeRequest_expectedImage_SendOneBanner() {
        BannerModerationEventsHandler handler = new BannerModerationEventsHandler.Builder()
                .addBannerModerationSupport(new BannerModerationSupport.Builder()
                        .setBannerType(BannersBannerType.cpm_banner)
                        .hasImage(true)
                        .setSender(sender)
                        .setRequestConsumer(requestConsumer)
                        .build()
                )
                .setModerationOperationModeProvider(moderationOperationModeProvider)
                .build();

        var actual = handleObjects(handler,
                Collections.singletonList(bannerModerationEventsWithImageWithInfo()));

        assertThat(actual).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void makeRequest_expectedNoImage_SendNoneBanner() {
        BannerModerationEventsHandler handler = new BannerModerationEventsHandler.Builder()
                .addBannerModerationSupport(new BannerModerationSupport.Builder()
                        .setBannerType(BannersBannerType.cpm_banner)
                        .hasImage(false)
                        .setSender(sender)
                        .setRequestConsumer(requestConsumer)
                        .build()
                )
                .setModerationOperationModeProvider(moderationOperationModeProvider)
                .build();

        handleObjectsMakeRequestNotCalled(handler,
                Collections.singletonList(bannerModerationEventsWithImageWithInfo()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void makeRequest_adGroupTypeNotMatched_SendNoneBanner() {
        BannerModerationEventsHandler handler = new BannerModerationEventsHandler.Builder()
                .addBannerModerationSupport(new BannerModerationSupport.Builder<
                        BannerModerationRequest<BannerModerationMeta, BaseBannerModerationData>, ModerationableBanner,
                        BannerModerationMeta>()
                        .setBannerType(BannersBannerType.text)
                        .setAdGroupType(AdGroupType.CPM_BANNER)
                        .setCampaignType(CampaignType.CPM_BANNER)
                        .setSender(sender)
                        .setRequestConsumer(requestConsumer)
                        .build())
                .setModerationOperationModeProvider(moderationOperationModeProvider)
                .build();

        handleObjectsMakeRequestNotCalled(handler,
                Collections.singletonList(textBannerModerationEventsWithInfo()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void makeRequest_campaignTypeNotMatched_SendNoneBanner() {
        BannerModerationEventsHandler handler = new BannerModerationEventsHandler.Builder()
                .addBannerModerationSupport(new BannerModerationSupport.Builder()
                        .setBannerType(BannersBannerType.cpm_banner)
                        .setAdGroupType(AdGroupType.CPM_BANNER)
                        .setCampaignType(CampaignType.TEXT)
                        .setSender(sender)
                        .setRequestConsumer(requestConsumer)
                        .build())
                .setModerationOperationModeProvider(moderationOperationModeProvider)
                .build();

        handleObjectsMakeRequestNotCalled(handler,
                Collections.singletonList(textBannerModerationEventsWithInfo()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void makeRequest_bannerTypeMatched_SendOneBanner() {

        BannerModerationEventsHandler handler = new BannerModerationEventsHandler.Builder()
                .addBannerModerationSupport(BannerModerationSupport.forAllEvents(BannersBannerType.text, sender,
                        requestConsumer))
                .setModerationOperationModeProvider(moderationOperationModeProvider)
                .build();

        List<Long> objects = handleObjects(handler,
                Collections.singletonList(textBannerModerationEventsWithInfo()));

        assertThat(objects).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void makeRequest_allTypesMatched_SendOneBanner() {

        BannerModerationEventsHandler handler = new BannerModerationEventsHandler.Builder()
                .addBannerModerationSupport(new BannerModerationSupport.Builder()
                        .setBannerType(BannersBannerType.text)
                        .setAdGroupType(AdGroupType.BASE)
                        .setCampaignType(CampaignType.TEXT)
                        .setSender(sender)
                        .setRequestConsumer(requestConsumer)
                        .build())
                .setModerationOperationModeProvider(moderationOperationModeProvider)
                .build();

        List<Long> objects = handleObjects(handler,
                Collections.singletonList(textBannerModerationEventsWithInfo()));
        assertThat(objects).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void makeRequest_allTypesMatched_MultipleBannersDifferentTypes() {

        BannerModerationEventsHandler handler = new BannerModerationEventsHandler.Builder()
                .addBannerModerationSupport(new BannerModerationSupport.Builder()
                        .setBannerType(BannersBannerType.image_ad)
                        .setSender(sender)
                        .setRequestConsumer(requestConsumer)
                        .build())
                .addBannerModerationSupport(new BannerModerationSupport.Builder()
                        .setBannerType(BannersBannerType.cpm_banner)
                        .setSender(sender)
                        .setRequestConsumer(requestConsumer)
                        .build())
                .setModerationOperationModeProvider(moderationOperationModeProvider)
                .build();

        BannerModerationEventsWithInfo frontPageBanner1 = cpmFrontPageBannerModerationEventsWithInfo();
        BannerModerationEventsWithInfo imageBanner = imageBannerModerationEventsWithInfo();

        BannerModerationEventsWithInfo cpmCampaignNotMatched = cpmBannerModerationEventsWithInfo();
        BannerModerationEventsWithInfo textBannerNotMatched = textBannerModerationEventsWithInfo();

        ArgumentCaptor<List<Long>> requestsCaptor = ArgumentCaptor.forClass(List.class);

        handler.handleObjects(SHARD, asList(cpmCampaignNotMatched, frontPageBanner1, imageBanner,
                textBannerNotMatched));

        Mockito.verify(sender, Mockito.times(2)).send(anyInt(), requestsCaptor.capture(),
                any(Function.class), any(Function.class), any(Function.class), eq(requestConsumer));

        List<Long> requestsCaptorObjects = flatMap(requestsCaptor.getAllValues(), Function.identity());

        assertThat(requestsCaptorObjects).hasSize(3);

        assertThat(requestsCaptorObjects).containsExactlyInAnyOrder(mapList(asList(frontPageBanner1,
                imageBanner, cpmCampaignNotMatched),
                BannerModerationEventsWithInfo::getBannerId).toArray(new Long[0]));

    }

    @Test
    @SuppressWarnings("unchecked")
    void makeRequest_allTypesMatched_MultipleBanners() {

        BannerModerationEventsHandler handler = new BannerModerationEventsHandler.Builder()
                .addBannerModerationSupport(new BannerModerationSupport.Builder()
                        .setBannerType(BannersBannerType.cpm_banner)
                        .setCampaignType(CampaignType.CPM_DEALS)
                        .setSender(sender)
                        .setRequestConsumer(requestConsumer)
                        .build())
                .addBannerModerationSupport(new BannerModerationSupport.Builder()
                        .setBannerType(BannersBannerType.cpm_banner)
                        .setAdGroupType(AdGroupType.CPM_YNDX_FRONTPAGE)
                        .setSender(sender)
                        .setRequestConsumer(requestConsumer)
                        .build())
                .setModerationOperationModeProvider(moderationOperationModeProvider)
                .build();

        BannerModerationEventsWithInfo frontPageBanner1 = cpmFrontPageBannerModerationEventsWithInfo();
        BannerModerationEventsWithInfo frontPageBanner2 = cpmFrontPageBannerModerationEventsWithInfo();
        BannerModerationEventsWithInfo cpmBannerDealsCamp = cpmBannerDealsCampModerationEventsWithInfo();

        BannerModerationEventsWithInfo cpmCampaignNotMatched = cpmBannerModerationEventsWithInfo();
        BannerModerationEventsWithInfo textBannerNotMatched = textBannerModerationEventsWithInfo();

        ArgumentCaptor<List<Long>> requestsCaptor = ArgumentCaptor.forClass(List.class);

        handler.handleObjects(SHARD, asList(cpmCampaignNotMatched, frontPageBanner1, frontPageBanner2,
                textBannerNotMatched, cpmBannerDealsCamp));

        Mockito.verify(sender, Mockito.times(2)).send(anyInt(), requestsCaptor.capture(),
                any(Function.class), any(Function.class), any(Function.class), eq(requestConsumer));

        List<Long> requestsCaptorObjects = flatMap(requestsCaptor.getAllValues(), Function.identity());

        assertThat(requestsCaptorObjects).hasSize(3);

        assertThat(requestsCaptorObjects).containsExactlyInAnyOrder(mapList(asList(frontPageBanner1,
                frontPageBanner2, cpmBannerDealsCamp),
                BannerModerationEventsWithInfo::getBannerId).toArray(new Long[0]));

    }

    private BannerModerationEventsWithInfo cpmBannerDealsCampModerationEventsWithInfo() {
        return getBannerModerationEventsWithInfo(CampaignType.CPM_DEALS, AdGroupType.CPM_BANNER,
                BannersBannerType.cpm_banner);
    }

    private BannerModerationEventsWithInfo textBannerModerationEventsWithInfo() {
        return getBannerModerationEventsWithInfo(CampaignType.TEXT, AdGroupType.BASE, BannersBannerType.text);
    }

    private BannerModerationEventsWithInfo cpmFrontPageBannerModerationEventsWithInfo() {
        return getBannerModerationEventsWithInfo(CampaignType.CPM_BANNER, AdGroupType.CPM_YNDX_FRONTPAGE,
                BannersBannerType.cpm_banner);
    }

    private BannerModerationEventsWithInfo cpmBannerModerationEventsWithInfo() {
        return getBannerModerationEventsWithInfo(CampaignType.CPM_BANNER, AdGroupType.CPM_BANNER,
                BannersBannerType.cpm_banner);
    }

    private BannerModerationEventsWithInfo imageBannerModerationEventsWithInfo() {
        return getBannerModerationEventsWithInfo(CampaignType.TEXT, AdGroupType.BASE,
                BannersBannerType.image_ad);
    }

    private BannerModerationEventsWithInfo bannerModerationEventsWithImageWithInfo() {
        var info = getBannerModerationEventsWithInfo(CampaignType.CPM_BANNER, AdGroupType.CPM_BANNER,
                BannersBannerType.cpm_banner);
        info.withHasImage(true);

        return info;
    }


    private BannerModerationEventsWithInfo getBannerModerationEventsWithInfo(CampaignType campaignType,
                                                                             AdGroupType adGroupType,
                                                                             BannersBannerType bannerType) {
        return new BannerModerationEventsWithInfo("", 0L, false)
                .withCampaignType(campaignType)
                .withAdGroupType(adGroupType)
                .withHasImage(false)
                .withObject(new BannerModerationEventsObject(null, 0L, CAMPAIGN_ID, ADGROUP_ID, BANNER_ID, bannerType,
                        false, false));
    }

    private List<Long> handleObjects(BannerModerationEventsHandler handler,
                                     List<BannerModerationEventsWithInfo> events) {

        ArgumentCaptor<List<Long>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);

        handler.handleObjects(SHARD, events);

        Mockito.verify(sender, Mockito.only()).send(anyInt(), requestsCaptor.capture(),
                any(Function.class), any(Function.class), any(Function.class), eq(requestConsumer));

        return requestsCaptor.getValue();
    }

    private void handleObjectsMakeRequestNotCalled(BannerModerationEventsHandler handler,
                                                   List<BannerModerationEventsWithInfo> events) {
        handler.handleObjects(SHARD, events);

        Mockito.verify(sender, Mockito.never()).send(anyInt(), anyCollection(),
                any(Function.class), any(Function.class), any(Function.class), eq(requestConsumer));
    }
}

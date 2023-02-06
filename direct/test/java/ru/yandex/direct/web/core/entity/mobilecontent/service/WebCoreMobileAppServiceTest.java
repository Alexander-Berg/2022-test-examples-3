package ru.yandex.direct.web.core.entity.mobilecontent.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignSimple;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.entity.domain.service.DomainService;
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppStoreType;
import ru.yandex.direct.core.entity.mobileapp.model.SkAdNetworkSlot;
import ru.yandex.direct.core.entity.mobileapp.service.IosSkAdNetworkSlotManager;
import ru.yandex.direct.core.entity.mobileapp.service.MobileAppService;
import ru.yandex.direct.core.entity.mobileapp.service.SkAdNetworkSlotsConfig;
import ru.yandex.direct.core.entity.mobileapp.service.SkAdNetworkSlotsConfigProvider;
import ru.yandex.direct.core.entity.mobilecontent.container.MobileAppStoreUrl;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.service.MobileContentService;
import ru.yandex.direct.core.entity.mobilecontent.util.MobileAppStoreUrlParser;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.web.core.entity.mobilecontent.converter.MobileContentConverter;
import ru.yandex.direct.web.core.entity.mobilecontent.converter.TrackerConverter;
import ru.yandex.direct.web.core.entity.mobilecontent.converter.WebCoreMobileAppConverter;
import ru.yandex.direct.web.core.entity.mobilecontent.model.WebMobileApp;
import ru.yandex.direct.web.core.entity.mobilecontent.model.WebMobileContent;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

public class WebCoreMobileAppServiceTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(11L);
    private static final int SHARD_ID = 1;
    private static final String STORE_URL =
            "https://play.google.com/store/apps/details?id=com.stayfprod.awesomeradio.free";
    private static final MobileAppStoreUrl PARSED_STORE_URL = MobileAppStoreUrlParser.parseStrict(STORE_URL);
    private static final long DOMAIN_ID = 2222L;
    private static final String DOMAIN = "ya.ru";
    private static final long MOBILE_CONTENT_ID = 99L;
    private static final MobileContent MOBILE_CONTENT = new MobileContent()
            .withId(MOBILE_CONTENT_ID)
            .withClientId(CLIENT_ID.asLong())
            .withPublisherDomainId(DOMAIN_ID)
            .withPrices(emptyMap());
    private static final int FREE_SLOTS_NUMBER = 10;

    private WebCoreMobileAppService webCoreMobileAppService;

    private DomainService domainService;
    private MobileAppService mobileAppService;
    private CampaignRepository campaignRepository;
    private SkAdNetworkSlotsConfigProvider skAdNetworkSlotsConfigProvider;
    private ShardHelper shardHelper;
    private IosSkAdNetworkSlotManager iosSkAdNetworkSlotManager;

    @Before
    public void before() {
        MobileContentService mobileContentService = mock(MobileContentService.class);
        domainService = mock(DomainService.class);
        mobileAppService = mock(MobileAppService.class);
        campaignRepository = mock(CampaignRepository.class);
        skAdNetworkSlotsConfigProvider = mock(SkAdNetworkSlotsConfigProvider.class);
        shardHelper = mock(ShardHelper.class);
        iosSkAdNetworkSlotManager = mock(IosSkAdNetworkSlotManager.class);

        var mobileContentConverter = new MobileContentConverter();
        var webCoreMobileAppConverter = new WebCoreMobileAppConverter(mock(TrackerConverter.class),
                mobileContentConverter, skAdNetworkSlotsConfigProvider);

        webCoreMobileAppService = new WebCoreMobileAppService(
                iosSkAdNetworkSlotManager, campaignRepository,
                mobileAppService, mobileContentService, mobileContentConverter,
                webCoreMobileAppConverter, domainService,
                shardHelper);

        when(mobileContentService.getMobileContent(CLIENT_ID, STORE_URL, PARSED_STORE_URL, true, true))
                .thenReturn(Optional.of(MOBILE_CONTENT));
        when(domainService.getDomainsByIdsFromDict(Collections.singleton(DOMAIN_ID)))
                .thenReturn(Collections.singletonList(new Domain().withId(DOMAIN_ID).withDomain(DOMAIN)));
        when(skAdNetworkSlotsConfigProvider.getConfig())
                .thenReturn(new SkAdNetworkSlotsConfig(FREE_SLOTS_NUMBER, 3));
        when(shardHelper.getShardByClientIdStrictly(CLIENT_ID)).thenReturn(SHARD_ID);
    }

    @Test
    public void getMobileContent() {
        Optional<WebMobileContent> maybeMobileContent = webCoreMobileAppService.getMobileContent(
                CLIENT_ID, STORE_URL, PARSED_STORE_URL, true);
        verify(domainService).getDomainsByIdsFromDict(Collections.singleton(DOMAIN_ID));

        assertThat(maybeMobileContent.isPresent()).isTrue();
        //noinspection OptionalGetWithoutIsPresent
        WebMobileContent webMobileContent = maybeMobileContent.get();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(webMobileContent.getId()).isEqualTo(MOBILE_CONTENT_ID);
            softly.assertThat(webMobileContent.getPublisherDomain()).isEqualTo(DOMAIN);
        });
    }

    @Test
    public void getAppListWithSkadNetworkSlots() {
        var mobileAppId1 = 1L;
        var mobileAppId2 = 2L;
        var bundleId1 = "xxx";
        var bundleId2 = "yyy";
        var storeContentId1 = "aaa";
        var storeContentId2 = "bbb";
        var mobileApps = List.of(
                new MobileApp()
                        .withId(mobileAppId1)
                        .withMobileContent(
                                new MobileContent()
                                        .withBundleId(bundleId1)
                                        .withStoreContentId(storeContentId1)
                                        .withPublisherDomainId(DOMAIN_ID)
                                        .withPrices(emptyMap())
                                        .withStoreCountry("RU"))
                        .withStoreType(MobileAppStoreType.APPLEAPPSTORE)
                        .withDomainId(DOMAIN_ID),
                new MobileApp()
                        .withId(mobileAppId2)
                        .withMobileContent(
                                new MobileContent()
                                        .withBundleId(bundleId2)
                                        .withStoreContentId(storeContentId2)
                                        .withPublisherDomainId(DOMAIN_ID)
                                        .withPrices(emptyMap())
                                        .withStoreCountry("RU"))
                        .withStoreType(MobileAppStoreType.APPLEAPPSTORE)
        );
        var campaignId1 = 1L;
        var campaignId2 = 2L;
        var campaignIds = List.of(campaignId1, campaignId2);
        var campaignSimpleList =
                List.of((CampaignSimple) (new Campaign().withId(campaignId1).withClientId(CLIENT_ID.asLong())));
        var slots = List.of(
                new SkAdNetworkSlot(bundleId1, campaignId1, 1),
                new SkAdNetworkSlot(bundleId1, campaignId2, 2));

        when(mobileAppService.getMobileApps(CLIENT_ID)).thenReturn(mobileApps);
        when(iosSkAdNetworkSlotManager.getAllocatedSlotsByBundleIds(any())).thenReturn(slots);
        when(campaignRepository.getCampaignsForClient(SHARD_ID, CLIENT_ID, campaignIds)).thenReturn(campaignSimpleList);

        var webMobileApps = listToMap(
                webCoreMobileAppService.getAppList(CLIENT_ID, null, null), WebMobileApp::getId);

        assertThat(webMobileApps.get(mobileAppId1).getBusySkadNetworkSlotsCount()).isEqualTo(2);
        assertThat(webMobileApps.get(mobileAppId1).getCampaignsWithSkadSlots().size()).isEqualTo(1);
        assertThat(webMobileApps.get(mobileAppId1).getSkadNetworkSlotsCount()).isEqualTo(FREE_SLOTS_NUMBER);

        assertThat(webMobileApps.get(mobileAppId2).getBusySkadNetworkSlotsCount()).isEqualTo(0);
        assertThat(webMobileApps.get(mobileAppId2).getSkadNetworkSlotsCount()).isEqualTo(FREE_SLOTS_NUMBER);

        var webMobileAppsWithStoreContentId = webCoreMobileAppService.getAppList(CLIENT_ID, "bbb", null);
        assertThat(webMobileAppsWithStoreContentId.size()).isEqualTo(1);
        assertThat(webMobileAppsWithStoreContentId.get(0).getId()).isEqualTo(mobileAppId2);
    }
}

package ru.yandex.direct.core.entity.banner.service.deleteoperation;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.service.BannerDeleteOperationFactory;
import ru.yandex.direct.core.entity.banner.type.creative.BannerCreativeRepository;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestCreativeRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTree;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.jooq.impl.DSL.count;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.draftTextBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultVideoAddition;
import static ru.yandex.direct.core.testing.data.TestGroups.draftTextAdgroup;
import static ru.yandex.direct.dbschema.ppc.tables.Banners.BANNERS;
import static ru.yandex.direct.dbschema.ppcdict.tables.ShardIncBid.SHARD_INC_BID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerDeleteOperationTest {

    @Autowired
    private BannerDeleteOperationFactory operationFactory;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private BannerCreativeRepository bannerCreativeRepository;

    @Autowired
    private CreativeRepository creativeRepository;

    @Autowired
    private TestCreativeRepository testCreativeRepository;

    @Autowired
    private ClientGeoService clientGeoService;

    private int shard;

    private ClientId clientId;
    private long clientUid;
    private ClientInfo clientInfo;
    private CampaignInfo textCampaignInfo;
    private GeoTree geoTree;

    @Before
    public void beforeClass() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        clientUid = clientInfo.getUid();
        shard = clientInfo.getShard();
        textCampaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        geoTree = clientGeoService.getClientTranslocalGeoTree(clientId);
    }

    @Test
    public void bannerDeletionFromTablesTest() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(textCampaignInfo);
        TextBannerInfo bannerInfo = createDraftAdGroupWithGeoAndDraftBanner(null, 225L);
        var bids = List.of(bannerInfo.getBannerId());

        var mr = operationFactory
                .createBannerDeleteOperation(shard, clientId, clientUid, bids, Applicability.PARTIAL)
                .prepareAndApply();
        assertThat(mr.getValidationResult(), hasNoErrors());

        Integer bannersCnt = dslContextProvider.ppc(shard)
                .selectCount()
                .from(BANNERS)
                .where(BANNERS.BID.in(bids))
                .fetchOne(count());
        assertThat("records count in banners table", bannersCnt, is(0));

        Integer metabaseCnt = dslContextProvider.ppcdict()
                .selectCount()
                .from(SHARD_INC_BID)
                .where(SHARD_INC_BID.BID.in(bids))
                .fetchOne(count());
        assertThat("records count in shard_inc_bid table", metabaseCnt, is(0));
    }

    // deleteBannerWithCreative

    @Test
    public void deleteBannerWithCreative_CreativeIsDraft_GeoChanged() {
        long creativeId = createDraftCreative();
        long bannerRussiaId = createDraftAdGroupWithGeoAndDraftBanner(creativeId, RUSSIA_REGION_ID).getBannerId();
        long bannerUkraineId = createDraftAdGroupWithGeoAndDraftBanner(creativeId, UKRAINE_REGION_ID).getBannerId();
        creativeRepository
                .updateCreativesGeo(shard, bannerCreativeRepository.getJoinedGeo(shard, geoTree,
                        singletonList(creativeId)));
        deleteBannersAndCheckGeo(creativeId, singletonList(bannerUkraineId), String.valueOf(RUSSIA_REGION_ID));
    }

    @Test
    public void deleteBannerWithCreative_CreativeIsNotDraft_GeoNotChanged() {
        long creativeId = createDraftCreative();
        long bannerRussiaId = createDraftAdGroupWithGeoAndDraftBanner(creativeId, RUSSIA_REGION_ID).getBannerId();
        long bannerUkraineId = createDraftAdGroupWithGeoAndDraftBanner(creativeId, UKRAINE_REGION_ID).getBannerId();
        creativeRepository
                .updateCreativesGeo(shard, bannerCreativeRepository.getJoinedGeo(shard, geoTree,
                        singletonList(creativeId)));
        creativeRepository.sendCreativesToModeration(shard, singletonList(creativeId));
        deleteBannersAndCheckGeo(creativeId, singletonList(bannerUkraineId),
                String.valueOf(RUSSIA_REGION_ID), String.valueOf(UKRAINE_REGION_ID));
    }

    @Test
    public void deleteBannerWithCreative_CreativeIsDraft_AllBannersDeleted() {
        long creativeId = createDraftCreative();
        long bannerRussiaId = createDraftAdGroupWithGeoAndDraftBanner(creativeId, RUSSIA_REGION_ID).getBannerId();
        long bannerUkraineId = createDraftAdGroupWithGeoAndDraftBanner(creativeId, UKRAINE_REGION_ID).getBannerId();
        creativeRepository
                .updateCreativesGeo(shard, bannerCreativeRepository.getJoinedGeo(shard, geoTree,
                        singletonList(creativeId)));
        deleteBannersAndCheckGeo(creativeId, Arrays.asList(bannerRussiaId, bannerUkraineId), (String[]) null);
    }

    private void deleteBannersAndCheckGeo(long creativeId, List<Long> bannerIds, String... regions) {
        assertThat(operationFactory
                .createBannerDeleteOperation(shard, clientId, clientUid, bannerIds, Applicability.PARTIAL)
                .prepareAndApply()
                .getValidationResult(), hasNoErrors());

        String newGeo = testCreativeRepository.getCreativesGeo(shard, singletonList(creativeId)).get(creativeId);
        if (regions != null) {
            assertThat(Arrays.asList(newGeo.split(",")), containsInAnyOrder(regions));
        } else {
            assertThat(newGeo, nullValue());
        }
    }

    private long createDraftCreative() {
        Creative creative = defaultVideoAddition(clientId, null).withStatusModerate(StatusModerate.NEW);
        CreativeInfo creativeInfo = new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
        creativeInfo = steps.creativeSteps().createCreative(creativeInfo);
        return creativeInfo.getCreativeId();
    }

    private TextBannerInfo createDraftAdGroupWithGeoAndDraftBanner(Long creativeId, Long... geoRegionId) {
        AdGroupInfo adGroupToDeleteInfo = new AdGroupInfo()
                .withAdGroup(draftTextAdgroup(null).withGeo(Arrays.asList(geoRegionId)))
                .withCampaignInfo(textCampaignInfo);
        steps.adGroupSteps().createAdGroup(adGroupToDeleteInfo);
        OldTextBanner bannerToDelete = draftTextBanner().withCreativeId(creativeId).withBsBannerId(0L);
        return steps.bannerSteps().createBanner(bannerToDelete, adGroupToDeleteInfo);
    }
}

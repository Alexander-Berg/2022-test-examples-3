package ru.yandex.direct.core.entity.banner.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Streams;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusSitelinksModerate;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.model.sitelinks.SitelinksModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.sitelinks.SitelinksModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.receiving.SitelinksModerationReceivingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestAdGroupRepository;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersMinusGeoType;
import ru.yandex.direct.dbschema.ppc.enums.PhrasesStatusbssynced;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.SITELINKS_SET;
import static ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames.DIRECT_SERVICE;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ResyncPhrasesOnReceiveSitelinksModerationResponseTest {

    private static final long DEFAULT_VERSION = 4L;
    private static final List<Long> DEFAULT_REASONS = Arrays.asList(2L, 3L);

    @Autowired
    TestModerationRepository testModerationRepository;

    @Autowired
    Steps steps;

    @Autowired
    AdGroupRepository adGroupRepository;

    @Autowired
    SitelinksModerationReceivingService sitelinksModerationReceivingService;

    @Autowired
    protected TestBannerRepository testBannerRepository;

    @Autowired
    TestAdGroupRepository testAdGroupRepository;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private OldTextBanner banner;
    private SitelinkSetInfo sitelinkSetInfo;
    private AdGroupInfo phrases;
    private TextBannerInfo bannerInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        clientInfo = campaignInfo.getClientInfo();
        shard = clientInfo.getShard();

        sitelinkSetInfo = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo);

        phrases = steps.adGroupSteps().createAdGroup(defaultTextAdGroup(campaignInfo.getCampaignId())
                .withStatusBsSynced(StatusBsSynced.YES), campaignInfo);

        bannerInfo = steps.bannerSteps()
                .createBanner(activeTextBanner(null, phrases.getAdGroupId())
                                .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId())
                                .withStatusSitelinksModerate(StatusSitelinksModerate.SENT)
                                .withStatusModerate(OldBannerStatusModerate.YES),
                        phrases);

        banner = bannerInfo.getBanner();

        testModerationRepository.createSitelinksVersion(shard, banner.getId(), DEFAULT_VERSION);
    }

    @Test
    public void noMinusRegions() {
        Map<Long, PhrasesStatusbssynced> statuses = prepare(List.of(), List.of(), 12516L);

        assertThat(statuses.keySet(), contains(phrases.getAdGroupId()));
        assertThat(statuses.get(phrases.getAdGroupId()), equalTo(PhrasesStatusbssynced.Yes));
    }

    @Test
    public void noMinusRegionsNewBanner() {
        Map<Long, PhrasesStatusbssynced> statuses = prepare(List.of(), List.of(), 0L);

        assertThat(statuses.keySet(), contains(phrases.getAdGroupId()));
        assertThat(statuses.get(phrases.getAdGroupId()), equalTo(PhrasesStatusbssynced.No));
    }

    @Test
    public void minusRegionsSyncedWithBs() {
        Map<Long, PhrasesStatusbssynced> statuses = prepare(List.of(BannersMinusGeoType.bs_synced), List.of("977"),
                12516L);

        assertThat(statuses.keySet(), contains(phrases.getAdGroupId()));
        assertThat(statuses.get(phrases.getAdGroupId()), equalTo(PhrasesStatusbssynced.No));
    }


    @Test
    public void minusRegionsHaveTwoRecordsWithBs() {
        Map<Long, PhrasesStatusbssynced> statuses = prepare(List.of(BannersMinusGeoType.current,
                BannersMinusGeoType.bs_synced), List.of("977", "977"), 12516L);

        assertThat(statuses.keySet(), contains(phrases.getAdGroupId()));
        assertThat(statuses.get(phrases.getAdGroupId()), equalTo(PhrasesStatusbssynced.Yes));
    }

    @Test
    public void minusRegionsHaveDifferentGeos() {
        Map<Long, PhrasesStatusbssynced> statuses = prepare(List.of(BannersMinusGeoType.current,
                BannersMinusGeoType.bs_synced), List.of("977", "12"), 12516L);

        assertThat(statuses.keySet(), contains(phrases.getAdGroupId()));
        assertThat(statuses.get(phrases.getAdGroupId()), equalTo(PhrasesStatusbssynced.No));
    }

    @Test
    public void minusRegionsHaveOnlyCurrentRecord() {
        Map<Long, PhrasesStatusbssynced> statuses = prepare(List.of(BannersMinusGeoType.current), List.of("977"),
                12345L);

        assertThat(statuses.keySet(), contains(phrases.getAdGroupId()));
        assertThat(statuses.get(phrases.getAdGroupId()), equalTo(PhrasesStatusbssynced.No));
    }

    @Test
    public void minusRegionsSyncedWithBsAndNoBannerId() {
        Map<Long, PhrasesStatusbssynced> statuses = prepare(List.of(BannersMinusGeoType.bs_synced), List.of("977"), 0L);

        assertThat(statuses.keySet(), contains(phrases.getAdGroupId()));
        assertThat(statuses.get(phrases.getAdGroupId()), equalTo(PhrasesStatusbssynced.No));
    }

    @Test
    public void minusRegionsSyncedWithBsAndNoCurrent() {
        Map<Long, PhrasesStatusbssynced> statuses = prepare(List.of(BannersMinusGeoType.bs_synced), List.of("977"),
                1234L);

        assertThat(statuses.keySet(), contains(phrases.getAdGroupId()));
        assertThat(statuses.get(phrases.getAdGroupId()), equalTo(PhrasesStatusbssynced.No));
    }

    @Test
    public void minusRegionsHaveOnlyCurrentRecordAndNoBannerId() {
        Map<Long, PhrasesStatusbssynced> statuses = prepare(List.of(BannersMinusGeoType.current), List.of("977"), 0L);

        assertThat(statuses.keySet(), contains(phrases.getAdGroupId()));
        assertThat(statuses.get(phrases.getAdGroupId()), equalTo(PhrasesStatusbssynced.No));
    }

    private Map<Long, PhrasesStatusbssynced> prepare(List<BannersMinusGeoType> geoTypes, List<String> geoCodes,
                                                     long bannerId) {
        SitelinksModerationResponse response = createResponse(Yes);

        Streams.zip(geoTypes.stream(), geoCodes.stream(), Pair::of).forEach(p ->
                testBannerRepository.addMinusGeo(shard, banner.getId(), p.getLeft(), p.getRight())
        );

        testBannerRepository.updateBannerId(shard, bannerInfo, bannerId);

        var unknownVerdictCountAndSuccess =
                sitelinksModerationReceivingService.processModerationResponses(shard, singletonList(response));

        assumeThat(unknownVerdictCountAndSuccess.getLeft(), equalTo(0));
        assumeThat(unknownVerdictCountAndSuccess.getRight().size(), equalTo(1));

        return testAdGroupRepository.getStatusBsSynced(shard,
                List.of(phrases.getAdGroupId()));
    }

    private SitelinksModerationResponse createResponse(ModerationDecision status) {
        return createResponse(status, DEFAULT_VERSION);
    }

    private SitelinksModerationResponse createResponse(ModerationDecision status, long version) {
        SitelinksModerationResponse response = new SitelinksModerationResponse();
        response.setService(DIRECT_SERVICE);
        response.setType(SITELINKS_SET);

        SitelinksModerationMeta meta = new SitelinksModerationMeta();
        meta.setClientId(clientInfo.getClientId().asLong());
        meta.setBannerId(banner.getId());
        meta.setUid(clientInfo.getUid());
        meta.setVersionId(version);
        meta.setSitelinksSetId(sitelinkSetInfo.getSitelinkSetId());

        response.setMeta(meta);

        Verdict v = new Verdict();
        v.setVerdict(status);

        if (status == No) {
            v.setReasons(DEFAULT_REASONS);
        }

        response.setResult(v);

        return response;
    }

}

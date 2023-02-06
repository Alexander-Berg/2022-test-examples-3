package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.model.sitelinks.SitelinksModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.sitelinks.SitelinksModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.SitelinksModerationReceivingService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveSitelinksModerationResponseTest extends AbstractModerationResponseTest<SitelinksModerationMeta,
        Verdict,
        SitelinksModerationResponse> {

    private static final long DEFAULT_VERSION = 40_000_000L;
    protected static final List<ModerationReasonDetailed> DEFAULT_REASONS = Arrays.asList(
            new ModerationReasonDetailed().withId(2L),
            new ModerationReasonDetailed().withId(3L));

    @Autowired
    TestModerationRepository testModerationRepository;

    @Autowired
    Steps steps;

    @Autowired
    OldBannerRepository bannerRepository;

    @Autowired
    SitelinksModerationReceivingService sitelinksModerationReceivingService;

    @Autowired
    protected TestBannerRepository testBannerRepository;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private OldTextBanner banner;
    private SitelinkSetInfo sitelinkSetInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        clientInfo = campaignInfo.getClientInfo();
        shard = clientInfo.getShard();

        sitelinkSetInfo = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo);

        banner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null)
                                .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId())
                                .withStatusSitelinksModerate(StatusSitelinksModerate.SENT)
                                .withStatusModerate(OldBannerStatusModerate.YES),
                        campaignInfo).getBanner();

        testModerationRepository.createSitelinksVersion(shard, banner.getId(), DEFAULT_VERSION);
    }

    private StatusSitelinksModerate getExpectedStatusSitelinksModerate(SitelinksModerationResponse response) {
        return StatusSitelinksModerate.valueOf(response.getResult().getVerdict().getString().toUpperCase());
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected void checkInDbForId(long bid, SitelinksModerationResponse response) {
        List<OldBanner> banners = bannerRepository.getBanners(shard, Collections.singleton(bid));

        assumeThat(banners, not(empty()));

        OldTextBanner dbRecord = (OldTextBanner) banners.get(0);
        StatusSitelinksModerate expectedStatusModerate = getExpectedStatusSitelinksModerate(response);

        assertEquals(dbRecord.getStatusSitelinksModerate(), expectedStatusModerate);
    }

    @Override
    protected ModerationReceivingService<SitelinksModerationResponse> getReceivingService() {
        return sitelinksModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        var banner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null)
                                .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId())
                                .withStatusSitelinksModerate(StatusSitelinksModerate.SENT)
                                .withStatusModerate(OldBannerStatusModerate.YES),
                        campaignInfo).getBanner();

        testModerationRepository.createSitelinksVersion(shard, banner.getId(), version);
        return banner.getId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return ModerationObjectType.SITELINKS_SET;
    }

    @Override
    protected long getDefaultVersion() {
        return DEFAULT_VERSION;
    }

    @Override
    protected SitelinksModerationResponse createResponse(long bid, ModerationDecision status,
                                                         @Nullable String language, long version,
                                                         Map<String, String> flags, List<Long> minusRegions,
                                                         ClientInfo clientInfo,
                                                         List<ModerationReasonDetailed> reasons) {
        SitelinksModerationResponse response = new SitelinksModerationResponse();
        response.setService(ModerationServiceNames.DIRECT_SERVICE);
        response.setType(ModerationObjectType.SITELINKS_SET);

        SitelinksModerationMeta meta = new SitelinksModerationMeta();
        meta.setClientId(clientInfo.getClientId().asLong());
        meta.setBannerId(bid);
        meta.setUid(clientInfo.getUid());
        meta.setVersionId(version);
        meta.setSitelinksSetId(sitelinkSetInfo.getSitelinkSetId());

        response.setMeta(meta);

        Verdict v = new Verdict();
        v.setVerdict(status);

        if (status == ModerationDecision.No) {
            v.setReasons(DEFAULT_REASONS.stream().map(ModerationReasonDetailed::getId).collect(Collectors.toList()));
            v.setDetailedReasons(DEFAULT_REASONS);
        }

        response.setResult(v);

        return response;
    }

    @Override
    protected long getDefaultObjectId() {
        return banner.getId();
    }

    @Override
    protected ClientInfo getDefaultObjectClientInfo() {
        return clientInfo;
    }

    @Override
    protected void deleteDefaultObjectVersion() {
        testModerationRepository.deleteSitelinksVersion(getShard(), getDefaultObjectId());
    }


    @Test
    public void declinedRemovedSitelinks() {
        testBannerRepository.deleteSitelinkSetFromBanner(shard, banner.getId());

        SitelinksModerationResponse response = createResponseForDefaultObject(No);

        sitelinksModerationReceivingService.processModerationResponses(shard, singletonList(response));

        List<OldBanner> banners = bannerRepository.getBanners(shard, List.of(banner.getId()));

        assertThat(banners.size(), greaterThan(0));
        assertThat(((OldTextBanner) banners.get(0)).getStatusSitelinksModerate(), equalTo(StatusSitelinksModerate.NEW));
    }

}

package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusBannerLogoModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerAssetModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerAssetModerationResponse;
import ru.yandex.direct.core.entity.moderation.repository.sending.ModerationDecisionAdapter;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.entity.moderation.service.receiving.BannerLogoModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerLogosRecord;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.BANNER_LOGOS;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmVideoBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveBannerLogoModerationResponseTest extends AbstractModerationResponseTest
        <BannerAssetModerationMeta, Verdict, BannerAssetModerationResponse> {

    private static final long DEFAULT_VERSION = 40000L;
    private static final List<ModerationReasonDetailed> DEFAULT_REASONS = Arrays.asList(
            new ModerationReasonDetailed().withId(2L),
            new ModerationReasonDetailed().withId(3L));

    @Autowired
    TestModerationRepository testModerationRepository;

    @Autowired
    Steps steps;

    @Autowired
    OldBannerRepository bannerRepository;

    @Autowired
    protected TestBannerRepository testBannerRepository;

    @Autowired
    BannerLogoModerationReceivingService bannerLogoModerationReceivingService;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private OldCpmBanner banner;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        clientInfo = campaignInfo.getClientInfo();
        shard = clientInfo.getShard();

        Long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmVideoAdditionCreative(clientInfo, creativeId);

        CpmBannerInfo cpmBannerInfo = steps.bannerSteps()
                .createActiveCpmVideoBanner(activeCpmVideoBanner(null, null, creativeId)
                        .withLogoImageHash("hash")
                        .withLogoStatusModerate(StatusBannerLogoModerate.SENT)
                        .withStatusModerate(OldBannerStatusModerate.YES), campaignInfo);

        this.banner = cpmBannerInfo.getBanner();
        testModerationRepository.createBannerLogosVersion(shard, this.banner.getId(), DEFAULT_VERSION);
    }

    private BannerLogosRecord getExpectedBannerLogos(BannerAssetModerationResponse response) {
        BannerLogosRecord record = new BannerLogosRecord();

        record.setBid(response.getMeta().getBannerId());
        record.setImageHash("hash");
        record.setStatusmoderate(ModerationDecisionAdapter.toBannerLogosStatusmoderate(response.getResult().getVerdict()));

        return record;
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected void checkInDbForId(long bid, BannerAssetModerationResponse response) {
        List<BannerLogosRecord> dbBannerLogos = testModerationRepository.getBannerLogos(shard,
                Collections.singleton(bid));

        assumeThat(dbBannerLogos, not(empty()));

        BannerLogosRecord dbRecord = dbBannerLogos.get(0);
        BannerLogosRecord expectedBannerLogos = getExpectedBannerLogos(response);

        assertEquals(dbRecord.getStatusmoderate(), expectedBannerLogos.getStatusmoderate());
    }

    @Override
    protected ModerationReceivingService<BannerAssetModerationResponse> getReceivingService() {
        return bannerLogoModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        CpmBannerInfo cpmBannerInfo = steps.bannerSteps()
                .createActiveCpmVideoBanner(activeCpmVideoBanner(null, null, banner.getCreativeId())
                        .withLogoImageHash("hash")
                        .withLogoStatusModerate(StatusBannerLogoModerate.SENT)
                        .withStatusModerate(OldBannerStatusModerate.YES), campaignInfo);

        testModerationRepository.createBannerLogosVersion(shard, cpmBannerInfo.getBannerId(), version);
        return cpmBannerInfo.getBannerId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return BANNER_LOGOS;
    }

    @Override
    protected long getDefaultVersion() {
        return DEFAULT_VERSION;
    }

    @Override
    protected BannerAssetModerationResponse createResponse(long bid, ModerationDecision status,
                                                          @Nullable String language, long version, Map<String,
            String> flags, List<Long> minusRegions, ClientInfo clientInfo, List<ModerationReasonDetailed> reasons) {
        BannerAssetModerationResponse response = new BannerAssetModerationResponse();
        response.setService(ModerationServiceNames.DIRECT_SERVICE);
        response.setType(BANNER_LOGOS);

        BannerAssetModerationMeta meta = new BannerAssetModerationMeta();
        meta.setClientId(clientInfo.getClientId().asLong());
        meta.setBannerId(bid);
        meta.setUid(clientInfo.getUid());
        meta.setVersionId(version);

        response.setMeta(meta);

        Verdict v = new Verdict();
        v.setVerdict(status);

        if (status == No) {
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
        testModerationRepository.deleteBannerLogoVersion(getShard(), getDefaultObjectId());
    }
}

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
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.model.turbolandings.TurbolandingModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.turbolandings.TurbolandingsModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.TurbolandingModerationReceivingService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannerTurbolandingsStatusmoderate;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerTurbolandingsRecord;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.TURBOLANDINGS;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveTurbolandingsModerationResponseTest extends AbstractModerationResponseTest<TurbolandingModerationMeta,
        Verdict,
        TurbolandingsModerationResponse> {

    private static final long DEFAULT_VERSION = 1600L;
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
    TurbolandingModerationReceivingService turbolandingModerationReceivingService;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private OldTextBanner banner;
    private TurboLanding turboLanding;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        clientInfo = campaignInfo.getClientInfo();
        shard = clientInfo.getShard();

        turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(clientInfo.getClientId());

        banner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null)
                                .withTurboLandingId(turboLanding.getId())
                                .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.SENT)
                                .withStatusModerate(OldBannerStatusModerate.YES),
                        campaignInfo).getBanner();

        testModerationRepository.createTurbolandingVersion(shard, banner.getId(), DEFAULT_VERSION);
    }

    private BannerTurbolandingsRecord getExpectedTurbolandings(TurbolandingsModerationResponse response) {
        String verdict = capitalize(response.getResult().getVerdict().getString().toLowerCase());

        BannerTurbolandingsRecord record = new BannerTurbolandingsRecord();

        record.setBid(response.getMeta().getBannerId());
        record.setStatusmoderate(BannerTurbolandingsStatusmoderate.valueOf(verdict));

        return record;
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected void checkInDbForId(long bid, TurbolandingsModerationResponse response) {
        List<BannerTurbolandingsRecord> turbolandings = testModerationRepository.getBannerTurbolandings(shard,
                Collections.singleton(bid));

        assumeThat(turbolandings, not(empty()));

        BannerTurbolandingsRecord dbRecord = turbolandings.get(0);
        BannerTurbolandingsRecord expectedTurbolandings = getExpectedTurbolandings(response);

        assertEquals(dbRecord.getStatusmoderate(), expectedTurbolandings.getStatusmoderate());
    }

    @Override
    protected ModerationReceivingService<TurbolandingsModerationResponse> getReceivingService() {
        return turbolandingModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        var turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(clientInfo.getClientId());

        var banner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null)
                                .withTurboLandingId(turboLanding.getId())
                                .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.SENT)
                                .withStatusModerate(OldBannerStatusModerate.YES),
                        campaignInfo).getBanner();

        testModerationRepository.createTurbolandingVersion(shard, banner.getId(), version);
        return banner.getId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return TURBOLANDINGS;
    }

    @Override
    protected long getDefaultVersion() {
        return DEFAULT_VERSION;
    }

    @Override
    protected TurbolandingsModerationResponse createResponse(long bid, ModerationDecision status,
                                                             @Nullable String language, long version, Map<String,
            String> flags, List<Long> minusRegions, ClientInfo clientInfo, List<ModerationReasonDetailed> reasons) {
        TurbolandingsModerationResponse response = new TurbolandingsModerationResponse();
        response.setService(ModerationServiceNames.DIRECT_SERVICE);
        response.setType(TURBOLANDINGS);

        TurbolandingModerationMeta meta = new TurbolandingModerationMeta();
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
        testModerationRepository.deleteTurbolandingsVersion(shard, banner.getId());
    }
}

package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerVideoAdditionModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerVideoAdditionModerationResponse;
import ru.yandex.direct.core.entity.moderation.repository.sending.ModerationDecisionAdapter;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.VideoAdditionModerationReceivingService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersPerformanceStatusmoderate;
import ru.yandex.direct.dbschema.ppc.tables.records.BannersPerformanceRecord;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.BANNER_VIDEO_ADDITION;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveVideoAdditionModerationResponseTest extends AbstractModerationResponseTest
        <BannerVideoAdditionModerationMeta, Verdict, BannerVideoAdditionModerationResponse> {
    private static final long DEFAULT_VERSION = 80000L;
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
    VideoAdditionModerationReceivingService videoAdditionModerationReceivingService;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private OldTextBanner banner;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        clientInfo = campaignInfo.getClientInfo();
        shard = clientInfo.getShard();

        Long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultVideoAdditionCreative(clientInfo, creativeId);
        banner = activeTextBanner()
                .withCreativeId(creativeId)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES);
        TextBannerInfo bannerInfo = steps.bannerSteps()
                .createActiveTextBanner(banner);

        this.banner = bannerInfo.getBanner();
        testModerationRepository.createBannerCreativeVersion(shard, this.banner.getId(), this.banner.getCreativeId(),
                DEFAULT_VERSION);
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected void checkInDbForId(long bid, BannerVideoAdditionModerationResponse response) {
        List<BannersPerformanceRecord> dbRecords = testModerationRepository.getBannersPerformance(shard,
                Collections.singleton(bid));

        assumeThat(dbRecords, not(empty()));

        BannersPerformanceRecord dbRecord = dbRecords.get(0);
        BannersPerformanceRecord expectedRecord = getExpectedRecord(response);

        assertEquals(dbRecord.getStatusmoderate(), expectedRecord.getStatusmoderate());
    }

    private BannersPerformanceRecord getExpectedRecord(BannerVideoAdditionModerationResponse response) {
        BannersPerformanceRecord record = new BannersPerformanceRecord();

        record.setBid(response.getMeta().getBannerId());
        record.setStatusmoderate(ModerationDecisionAdapter.toBannersPerformanceStatusmoderate(response.getResult().getVerdict()));

        return record;
    }

    @Override
    protected ModerationReceivingService<BannerVideoAdditionModerationResponse> getReceivingService() {
        return videoAdditionModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultVideoAdditionCreative(clientInfo, creativeId);
        TextBannerInfo bannerInfo = steps.bannerSteps()
                .createActiveTextBanner(activeTextBanner()
                        .withCreativeId(creativeId)
                        .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES));

        testModerationRepository.createBannerCreativeVersion(shard, bannerInfo.getBannerId(), creativeId,
                version);

        return bannerInfo.getBannerId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return BANNER_VIDEO_ADDITION;
    }

    @Override
    protected long getDefaultVersion() {
        return DEFAULT_VERSION;
    }

    @Override
    protected BannerVideoAdditionModerationResponse createResponse(long bid, ModerationDecision status,
                                                                   @Nullable String language, long version,
                                                                   Map<String, String> flags, List<Long> minusRegions,
                                                                   ClientInfo clientInfo,
                                                                   List<ModerationReasonDetailed> reasons) {
        var banner = (OldTextBanner) bannerRepository.getBanners(shard, singletonList(bid)).get(0);
        return createResponse(bid, banner.getCreativeId(), status, language, version, flags, minusRegions, clientInfo, reasons);
    }

    private BannerVideoAdditionModerationResponse createResponse(long bid, long creativeId, ModerationDecision status,
                                                                 @Nullable String language, long version,
                                                                 Map<String, String> flags, List<Long> minusRegions,
                                                                 ClientInfo clientInfo, List<ModerationReasonDetailed> reasons) {
        BannerVideoAdditionModerationResponse response = new BannerVideoAdditionModerationResponse();
        response.setService(ModerationServiceNames.DIRECT_SERVICE);
        response.setType(BANNER_VIDEO_ADDITION);

        BannerVideoAdditionModerationMeta meta = new BannerVideoAdditionModerationMeta();
        meta.setClientId(clientInfo.getClientId().asLong());
        meta.setBannerId(bid);
        meta.setUid(clientInfo.getUid());
        meta.setVersionId(version);
        meta.setCreativeId(creativeId);

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
        testModerationRepository.deleteBannerCreativeVersion(getShard(), getDefaultObjectId(), banner.getCreativeId());
    }

    @Override
    protected PpcPropertyName<Boolean> getRestrictedModePropertyName() {
        return PpcPropertyNames.RESTRICTED_VIDEO_ADDITION_TRANSPORT_NEW_MODERATION;
    }

    @Override
    protected void checkStatusModerateNotChanged(long id) {
        List<BannersPerformanceRecord> dbRecords = testModerationRepository.getBannersPerformance(shard,
                Collections.singleton(id));
        assertEquals(1, dbRecords.size());
        assertEquals(BannersPerformanceStatusmoderate.Yes, dbRecords.get(0).getStatusmoderate());
    }

    protected BannerVideoAdditionModerationResponse createResponseForDefaultObject(ModerationDecision status) {
        return createResponse(getDefaultObjectId(), banner.getCreativeId(), status, getDefaultVersion());
    }

    private BannerVideoAdditionModerationResponse createResponse(long bid, long creativeId,
                                                                 ModerationDecision status, long version) {
        var response = createResponse(getDefaultObjectId(), status, null, version,
                emptyMap(), DEFAULT_MINUS_REGION, getDefaultObjectClientInfo(), DEFAULT_REASONS);
        response.getMeta().setCreativeId(creativeId);
        return response;
    }

    @Test
    public void differentCreativeId_notSavedInDb() {
        var response = createResponse(banner.getId(), 1 + banner.getCreativeId(),
                ModerationDecision.No, getDefaultVersion());

        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(0, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(createResponseForDefaultObject(ModerationDecision.Yes));
    }

    @Test
    public void sameCreativeId_savedInDb() {
        var response = createResponseForDefaultObject(ModerationDecision.Yes);

        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response);
    }
}

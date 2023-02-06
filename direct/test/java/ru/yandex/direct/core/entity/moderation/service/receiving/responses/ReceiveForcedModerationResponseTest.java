package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.repository.receiving.BannersModerationReceivingRepository;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.entity.moderation.service.receiving.BannerChangesValidator;
import ru.yandex.direct.core.entity.moderation.service.receiving.CpmYndxFrontpageModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.processing_configurations.CpmYndxFrontpageOperations;
import ru.yandex.direct.core.entity.moderation.service.receiving.processor.response_parser.BannerResponseParser;
import ru.yandex.direct.core.entity.moderationdiag.service.ModerationDiagService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestModerationDiag;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.repository.TestBannerCreativeRepository;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.ModerationDiagSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersPerformanceStatusmoderate;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.YNDX_FRONTPAGE_CREATIVE;
import static ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonMapping.reasonsToDbFormat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5;
import static ru.yandex.direct.dbschema.ppc.enums.ModReasonsType.banner;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(Parameterized.class)
public class ReceiveForcedModerationResponseTest {
    private static final List<Long> DEFAULT_MINUS_REGION = Collections.emptyList();

    @Autowired
    private BannerRelationsRepository bannerRelationsRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private CpmYndxFrontpageOperations cpmYndxFrontpageOperations;

    @Autowired
    private BannersModerationReceivingRepository bannersModerationReceivingRepository;

    @Autowired
    private BannerResponseParser bannerResponseParser;

    @Autowired
    private BannerChangesValidator bannerChangesValidator;

    private CpmYndxFrontpageModerationReceivingService cpmYndxFrontpageModerationReceivingService;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private TestBannerCreativeRepository testBannerCreativeRepository;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private TestBannerRepository testBannerRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private ModerationDiagSteps moderationDiagSteps;

    @Autowired
    private ModerationDiagService moderationDiagService;

    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private CpmBannerInfo bannerInfo;
    private AdGroupInfo adGroupInfo;

    private int shard;
    private long adGroupId;
    private long creativeId;

    private static final long CRITICAL_DIAG = TestModerationDiag.DIAG_ID1;
    private static final long ORDINARY_DIAG = TestModerationDiag.DIAG_ID2;

    @Parameterized.Parameter
    public OldBannerStatusModerate directStatus;

    @Parameterized.Parameter(1)
    public boolean responseHaveCriticalReasons;

    @Parameterized.Parameter(2)
    public boolean versionsCoincident;

    @Parameterized.Parameter(3)
    public ModerationDecision responseStatusModerate;

    @Parameterized.Parameter(4)
    public boolean responseSupposedToBeApplied;

    @Parameterized.Parameters(name = "statusModerate {0}, isCrit: {1}, versionsAreTheSame: {2}, verdict: {3} " +
            "expected: {4}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {OldBannerStatusModerate.NO, true, false, No, false},
                {OldBannerStatusModerate.NO, true, true, No, true},
                {OldBannerStatusModerate.NO, false, true, No, true},
                {OldBannerStatusModerate.NO, false, false, No, false},
                {OldBannerStatusModerate.NO, false, true, Yes, true},
                {OldBannerStatusModerate.NO, false, false, Yes, false},

                {OldBannerStatusModerate.YES, true, false, No, false},
                {OldBannerStatusModerate.YES, true, true, No, true},
                {OldBannerStatusModerate.YES, false, true, No, true},
                {OldBannerStatusModerate.YES, false, false, No, false},
                {OldBannerStatusModerate.YES, false, true, Yes, true},
                {OldBannerStatusModerate.YES, false, false, Yes, false},

                {OldBannerStatusModerate.READY, true, false, No, false},
                {OldBannerStatusModerate.READY, true, true, No, false},
                {OldBannerStatusModerate.READY, false, true, No, false},
                {OldBannerStatusModerate.READY, false, false, No, false},
                {OldBannerStatusModerate.READY, false, true, Yes, false},
                {OldBannerStatusModerate.READY, false, false, Yes, false},

                {OldBannerStatusModerate.SENDING, true, false, No, false},
                {OldBannerStatusModerate.SENDING, true, true, No, true},
                {OldBannerStatusModerate.SENDING, false, true, No, true},
                {OldBannerStatusModerate.SENDING, false, false, No, false},
                {OldBannerStatusModerate.SENDING, false, true, Yes, true},
                {OldBannerStatusModerate.SENDING, false, false, Yes, false},

                {OldBannerStatusModerate.SENT, true, false, No, false},
                {OldBannerStatusModerate.SENT, true, true, No, true},
                {OldBannerStatusModerate.SENT, false, true, No, true},
                {OldBannerStatusModerate.SENT, false, false, No, false},
                {OldBannerStatusModerate.SENT, false, true, Yes, true},
                {OldBannerStatusModerate.SENT, false, false, Yes, false},
        });
    }

    @Before
    public void setUp() throws Exception {
        new TestContextManager(this.getClass()).prepareTestInstance(this);

        moderationDiagSteps.insertStandartDiags();
        moderationDiagService.invalidateAll();

        clientInfo = steps.clientSteps().createDefaultClient();
        adGroupInfo = steps.adGroupSteps().createDefaultCpmYndxFrontpageAdGroup(clientInfo);
        Creative creative = defaultHtml5(null, null).withHeight(67L).withWidth(320L);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);

        adGroupId = adGroupInfo.getAdGroupId();
        creativeId = creativeInfo.getCreativeId();

        ppcPropertiesSupport = mock(PpcPropertiesSupport.class);

        cpmYndxFrontpageModerationReceivingService = new CpmYndxFrontpageModerationReceivingService(
                bannerRelationsRepository,
                adGroupRepository,
                dslContextProvider,
                cpmYndxFrontpageOperations,
                bannersModerationReceivingRepository,
                bannerResponseParser,
                bannerChangesValidator,
                ppcPropertiesSupport
        );
    }

    @After
    public void after() {
        moderationDiagSteps.cleanup();
        moderationDiagService.invalidateAll();
    }

    @Test
    public void checkModerationResult() {
        createBanner(directStatus, 1L);

        BannerModerationResponse response = createResponse(bannerInfo,
                responseStatusModerate,
                versionsCoincident ? 1L : 2L,
                responseStatusModerate == Yes ? null :
                        (responseHaveCriticalReasons ? List.of(CRITICAL_DIAG) : List.of(ORDINARY_DIAG)));

        var unknownVerdictCountAndSuccess = cpmYndxFrontpageModerationReceivingService
                .processModerationResponses(shard, singletonList(response));

        if (responseSupposedToBeApplied) {
            assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
            assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
            checkInDb(bannerInfo, response);
        } else {
            assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
            assertEquals(0, unknownVerdictCountAndSuccess.getRight().size());
            checkNoChangesWereInDb(bannerInfo.getBannerId());
        }

    }

    private void createBanner(OldBannerStatusModerate statusModerate, long version) {
        OldCpmBanner banner = activeCpmBanner(adGroupInfo.getCampaignId(), adGroupId, creativeId)
                .withStatusModerate(statusModerate);

        bannerInfo = steps.bannerSteps().createActiveCpmBanner(banner, adGroupInfo);
        shard = bannerInfo.getShard();
        testModerationRepository.createBannerVersion(shard, bannerInfo.getBannerId(), version);
    }

    private BannerModerationResponse createResponse(CpmBannerInfo bannerInfo, ModerationDecision status,
                                                    long version, List<Long> reasons) {
        return createResponse(bannerInfo, status, emptyMap(), version, reasons);
    }

    private BannerModerationResponse createResponse(CpmBannerInfo bannerInfo, ModerationDecision status,
                                                    Map<String, String> flags,
                                                    long version, List<Long> reasons) {
        BannerModerationResponse r = new BannerModerationResponse();

        r.setService(ModerationServiceNames.DIRECT_SERVICE);
        r.setType(YNDX_FRONTPAGE_CREATIVE);

        BannerModerationMeta meta = new BannerModerationMeta();
        meta.setClientId(bannerInfo.getClientId().asLong());
        meta.setBannerId(bannerInfo.getBannerId());
        meta.setUid(bannerInfo.getUid());
        meta.setVersionId(version);

        r.setMeta(meta);

        Verdict v = new Verdict();
        v.setVerdict(status);
        v.setMinusRegions(DEFAULT_MINUS_REGION);

        if (status == No) {
            v.setReasons(reasons);
        }

        v.setFlags(flags);
        v.setLang(null);

        r.setResult(v);

        return r;
    }

    private OldBanner checkInDb(CpmBannerInfo bannerInfo, BannerModerationResponse response) {
        return checkInDb(bannerInfo, response, DEFAULT_MINUS_REGION);
    }

    private void checkNoChangesWereInDb(long bid) {
        List<OldBanner> dbBanners = bannerRepository.getBanners(shard, singleton(bid));
        OldBanner b = dbBanners.get(0);

        assertEquals(b.getStatusModerate(), bannerInfo.getBanner().getStatusModerate());
        assertEquals(b.getStatusPostModerate(), bannerInfo.getBanner().getStatusPostModerate());
    }

    private OldBanner checkInDb(CpmBannerInfo bannerInfo, BannerModerationResponse response,
                                List<Long> expectedMinusRegions) {
        long bid = bannerInfo.getBannerId();

        List<OldBanner> dbBanners = bannerRepository.getBanners(shard, singleton(bid));

        assertNotNull(dbBanners);
        assertEquals(1, dbBanners.size());

        OldBanner b = dbBanners.get(0);
        BannersPerformanceStatusmoderate bannerPerformanceStatus =
                testBannerCreativeRepository.getBannerPerformanceStatus(shard, bid);

        String minusGeo = testBannerRepository.getMinusGeo(shard, bid);

        List<String> modReasons = testBannerRepository.getModReasons(shard, bid, banner);

        assertNotNull(bannerPerformanceStatus);

        assertEquals(StatusBsSynced.NO, b.getStatusBsSynced());

        if (expectedMinusRegions.isEmpty()) {
            assertNull(minusGeo);
        } else {
            assertEquals(expectedMinusRegions.stream().map(Object::toString).collect(joining(",")), minusGeo);
        }

        if (response.getResult().getVerdict() == Yes) {
            assertEquals(OldBannerStatusModerate.YES, b.getStatusModerate());
            assertEquals(OldBannerStatusPostModerate.YES, b.getStatusPostModerate());
            assertEquals(BannersPerformanceStatusmoderate.Yes, bannerPerformanceStatus);
            assertEquals(reasonsToDbFormat(null), modReasons.get(0));
        } else {
            assertEquals(OldBannerStatusModerate.NO, b.getStatusModerate());

            if (responseHaveCriticalReasons || directStatus == OldBannerStatusModerate.YES) {
                assertEquals(OldBannerStatusPostModerate.REJECTED, b.getStatusPostModerate());
            } else {
                assertEquals(OldBannerStatusPostModerate.NO, b.getStatusPostModerate());
            }
            assertEquals(BannersPerformanceStatusmoderate.No, bannerPerformanceStatus);
            assertEquals(1, modReasons.size());
            assertEquals(reasonsToDbFormat(response.getResult().getReasonsWithDetails()), modReasons.get(0));
        }

        return b;
    }

}

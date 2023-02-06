package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.entity.moderationdiag.service.ModerationDiagService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.ModReasonsType;
import ru.yandex.direct.intapi.client.IntApiClient;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.util.Lists.emptyList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonMapping.reasonsToDbFormat;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;


public abstract class OldAbstractBannerModerationResponseTest
        extends AbstractModerationResponseTest<BannerModerationMeta, Verdict, BannerModerationResponse> {


    @Autowired
    private IntApiClient intApiClient;

    @Autowired
    protected TestBannerRepository testBannerRepository;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    protected OldBannerRepository bannerRepository;

    @Autowired
    protected BannerRelationsRepository bannerRelationsRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private ModerationDiagService moderationDiagService;

    @Before
    public void before() {
        moderationDiagService.invalidateAll();
    }

    protected void checkInDb(BannerModerationResponse response,
                             List<Long> expectedMinusRegions) {

        checkInDb(response);

        String minusGeo = testBannerRepository.getMinusGeo(getShard(), response.getMeta().getBannerId());

        if (expectedMinusRegions.isEmpty()) {
            assertNull(minusGeo);
        } else {
            assertEquals(expectedMinusRegions.stream().map(Object::toString).collect(joining(",")), minusGeo);
        }

        OldBanner b = getBanner(response.getMeta().getBannerId());

        if (response.getResult().getVerdict() != Yes) {
            assertEquals(expectedMinusRegions.isEmpty() ? OldBannerStatusPostModerate.NO : OldBannerStatusPostModerate.REJECTED,
                    b.getStatusPostModerate());
        }

    }

    @Override
    protected void deleteDefaultObjectVersion() {
        testModerationRepository.deleteBannerVersion(getShard(), getDefaultObjectId());
    }

    protected OldBanner getBanner(long bid) {
        List<OldBanner> dbBanners = bannerRepository.getBanners(getShard(), singleton(bid));

        assertNotNull(dbBanners);
        assertEquals(1, dbBanners.size());

        return dbBanners.get(0);
    }

    @Test
    public void moderationResponseLanguage_savedInDb() {
        BannerModerationResponse response = createResponseForDefaultObject(Yes, "en");
        var unknownVerdictCountAndSuccess =
                getReceivingService().processModerationResponses(getShard(), singletonList(response));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response);

        OldBanner banner = getBanner(response.getMeta().getBannerId());

        assertThat("Язык из ответа модерации сохранился в базе", banner.getLanguage(),
                is(Language.EN));
    }

    @Test
    public void moderationResponseNoLanguage_savedInDb() {
        BannerModerationResponse response = createResponseForDefaultObject(No, null);

        var unknownVerdictCountAndSuccess =
                getReceivingService().processModerationResponses(getShard(), singletonList(response));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());

        checkInDb(response);

        OldBanner banner = getBanner(response.getMeta().getBannerId());

        assertThat("отсутствие языка не должно влиять на значение в базе", banner.getLanguage(),
                is(Language.RU_));
    }

    protected BannerModerationResponse createResponseForDefaultObject(ModerationDecision status, String language) {
        return createResponse(getDefaultObjectId(), status, language, 1,
                emptyMap(),
                DEFAULT_MINUS_REGION,
                getDefaultObjectClientInfo(),
                DEFAULT_REASONS);
    }

    /**
     * Минус-регионы
     */

    @Test
    public void moderationBannerModerationResponseYesWithMinusRegions_savedInDb() {
        BannerModerationResponse response = createResponseForDefaultObject(Yes);
        response.getResult().setMinusRegions(singletonList(RUSSIA_REGION_ID));

        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response, singletonList(RUSSIA_REGION_ID));
    }

    @Test
    public void moderationBannerModerationResponseNoWithMinusRegions_savedInDb() {
        BannerModerationResponse response = createResponseForDefaultObject(No);
        response.getResult().setMinusRegions(singletonList(RUSSIA_REGION_ID));
        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response, singletonList(RUSSIA_REGION_ID));
    }

    @Test
    public void moderationBannerModerationResponseNoWithoutMinusRegions_savedInDb() {
        BannerModerationResponse bannerModerationResponse = createResponseForDefaultObject(No);
        bannerModerationResponse.getResult().setMinusRegions(List.of());

        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(bannerModerationResponse));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(bannerModerationResponse, emptyList());
    }


    @Test
    public void firstYesThenYesWithMinusRegionsVerdict_minusRegionsUpdated() {
        BannerModerationResponse bannerModerationResponse = createResponseForDefaultObject(Yes);
        bannerModerationResponse.getResult().setMinusRegions(emptyList());
        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(bannerModerationResponse));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(bannerModerationResponse, emptyList());

        bannerModerationResponse.getResult().setMinusRegions(singletonList(RUSSIA_REGION_ID));
        unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(bannerModerationResponse));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(bannerModerationResponse, singletonList(RUSSIA_REGION_ID));
    }

    @Test
    public void firstYesThenYesWithoutMinusRegionsVerdict_minusRegionsUpdated() {
        BannerModerationResponse response = createResponseForDefaultObject(Yes);
        response.getResult().setMinusRegions(singletonList(RUSSIA_REGION_ID));
        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response, singletonList(RUSSIA_REGION_ID));

        response = createResponseForDefaultObject(Yes);
        response.getResult().setMinusRegions(emptyList());
        unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response, emptyList());
    }

    /**
     * Флаги
     */

    @Test
    public void moderationResponseWithFlags_savedInDb() {
        BannerModerationResponse response = createResponseForDefaultObject(Yes,
                new HashMap<>(Map.of("age", "age18", "finance", "1", "people", "", "education", "")),
                1L);

        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response);

        OldBanner bannerInDb = getBanner(getDefaultObjectId());

        assertNotNull(bannerInDb.getFlags());
        assertThat("флаги из ответа модерации должны попадать в таблицу",
                bannerInDb.getFlags().getFlags().keySet(), hasItems(BannerFlags.AGE.getKey(),
                        BannerFlags.PEOPLE.getKey(), BannerFlags.EDUCATION.getKey()));
        assertThat("значение флага age не содержит слово age", bannerInDb.getFlags().getFlags().get("age"), is("18"));
    }

    protected BannerModerationResponse createResponseForDefaultObject(ModerationDecision status,
                                                                      Map<String, String> flags,
                                                                      long version) {
        return createResponse(getDefaultObjectId(), status, null, version,
                flags, DEFAULT_MINUS_REGION, getDefaultObjectClientInfo(), DEFAULT_REASONS);
    }

    @Test
    public void moderationResponseWithFlags_ageAndBabyFoodFlagsNotBroken_flagsNotChanged() {
        BannerModerationResponse response = createResponseForDefaultObject(Yes,
                new HashMap<>(Map.of("age", "16+", "baby_food", "12")), 1L);

        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response);
        OldBanner bannerInDb = getBanner(getDefaultObjectId());
        assertNotNull(bannerInDb.getFlags());
        assertThat("флаги из ответа модерации должны попадать в таблицу",
                bannerInDb.getFlags().getFlags().keySet(), hasItems(BannerFlags.AGE.getKey(),
                        BannerFlags.BABY_FOOD.getKey()));
        assertThat("значение флага age не содержит слово age",
                bannerInDb.getFlags().getFlags().get("age"), is("16+"));
        assertThat("значение флага baby_food не содержит слово baby_food",
                bannerInDb.getFlags().getFlags().get("baby_food"), is("12"));
    }

    @Test
    public void moderationResponseWithFlags_ageAndBabyFoodFlagsBroken_flagsFixed() {
        BannerModerationResponse response = createResponseForDefaultObject(Yes,
                new HashMap<>(Map.of("age", "age16+", "baby_food", "baby_food12")), 1L);

        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        OldBanner bannerInDb = getBanner(getDefaultObjectId());

        checkInDb(response);

        assertNotNull(bannerInDb.getFlags());
        assertThat("флаги из ответа модерации должны попадать в таблицу",
                bannerInDb.getFlags().getFlags().keySet(), hasItems(BannerFlags.AGE.getKey(),
                        BannerFlags.BABY_FOOD.getKey()));
        assertThat("значение флага age не содержит слово age",
                bannerInDb.getFlags().getFlags().get("age"), is("16+"));
        assertThat("значение флага baby_food не содержит слово baby_food",
                bannerInDb.getFlags().getFlags().get("baby_food"), is("12"));
    }

    @Test
    public void moderationResponseWithFlagsUpdated_savedInDb() {
        BannerModerationResponse response1 = createResponseForDefaultObject(Yes,
                new HashMap<>(Map.of("age", "age18")), 1L);
        BannerModerationResponse response2 = createResponseForDefaultObject(Yes,
                new HashMap<>(Map.of("education", "")), 1L);

        getReceivingService()
                .processModerationResponses(getShard(), singletonList(response1));
        getReceivingService()
                .processModerationResponses(getShard(), singletonList(response2));

        checkInDb(response2);

        OldBanner bannerInDb = getBanner(getDefaultObjectId());

        assertNotNull(bannerInDb.getFlags());
        assertThat(bannerInDb.getFlags().getFlags().keySet(), hasItems(BannerFlags.EDUCATION.getKey()));
        assertThat("флаги из ответа модерации должны затирать существующие флаги",
                bannerInDb.getFlags().getFlags().keySet(), not(hasItems(BannerFlags.AGE.getKey())));
    }

    /*
    @Test
    public void processModerationResponses_NoResponsesWithDifferentCampaigns_ModMailCandidatesAdded() {
        RESPONSE response = createResponse(banner, No);

        AdGroupInfo anotherGroup = steps.adGroupSteps().createDefaultCpmYndxFrontpageAdGroup(clientInfo);

        ImageHashBanner newBanner = defaultClientImageHashBanner(adGroupInfo.getCampaignId(),
                anotherGroup.getAdGroupId(), banner.getImage().getImageHash())
                .withStatusModerate(SENT);

        ImageHashBanner anotherBanner =
                steps.bannerSteps().createActiveImageHashBanner(newBanner, anotherGroup).getBanner();


        testModerationRepository.createBannerVersion(getShard(), anotherBanner.getId(), 1L);

        RESPONSE anotherResponse = createResponse(anotherBanner, No);

        getReceivingService()
                .processModerationResponses(getShard(), Arrays.asList(response, anotherResponse));

        List<ModerationMailCandidate> expectedMailCandidates = Arrays.asList(
                new ModerationMailCandidate()
                        .withCampaignId(banner.getCampaignId())
                        .withClientId(campaignInfo.getClientId().asLong())
                        .withStatus(ModerationMailCandidateStatus.NEW),
                new ModerationMailCandidate()
                        .withCampaignId(anotherBanner.getCampaignId())
                        .withClientId(adGroupInfo.getClientId().asLong())
                        .withStatus(ModerationMailCandidateStatus.NEW));

        checkClientMailCandidatesInDb(clientInfo, expectedMailCandidates);
    }
*/

    @Override
    protected BannerModerationResponse createResponse(long bid,
                                                      ModerationDecision status,
                                                      @Nullable String language,
                                                      long version,
                                                      Map<String, String> flags,
                                                      List<Long> minusRegions,
                                                      ClientInfo clientInfo,
                                                      List<ModerationReasonDetailed> reasons) {
        BannerModerationResponse r = new BannerModerationResponse();

        r.setService(ModerationServiceNames.DIRECT_SERVICE);
        r.setType(getObjectType());

        BannerModerationMeta meta = new BannerModerationMeta();
        meta.setClientId(clientInfo.getClientId().asLong());
        meta.setBannerId(bid);
        meta.setUid(clientInfo.getUid());
        meta.setVersionId(version);
        if (getDirectBannerType() != null) {
            meta.setBannerType(getDirectBannerType());
        }

        r.setMeta(meta);

        Verdict v = new Verdict();
        v.setVerdict(status);
        v.setMinusRegions(minusRegions);
        if (status == No) {
            v.setReasons(DEFAULT_REASONS.stream().map(ModerationReasonDetailed::getId).collect(Collectors.toList()));
            v.setDetailedReasons(DEFAULT_REASONS);
        }
        v.setFlags(flags);
        v.setLang(language);

        r.setResult(v);

        return r;
    }

    @Override
    protected void checkInDbForId(long bid, BannerModerationResponse response) {
        int shard = getShard();

        OldBanner b = getBanner(bid);

        List<String> modReasons = testBannerRepository.getModReasons(shard, bid, ModReasonsType.banner);

        assertEquals(StatusBsSynced.NO, b.getStatusBsSynced());

        if (response.getResult().getVerdict() == Yes) {
            assertEquals(OldBannerStatusModerate.YES, b.getStatusModerate());
            assertEquals(OldBannerStatusPostModerate.YES, b.getStatusPostModerate());
            assertEquals(reasonsToDbFormat(null), modReasons.get(0));
        } else {
            assertEquals(OldBannerStatusModerate.NO, b.getStatusModerate());
            assertEquals(1, modReasons.size());
            assertEquals(reasonsToDbFormat(DEFAULT_REASONS), modReasons.get(0));
        }

    }

    protected Language defaultLanguage() {
        return Language.RU_;
    }


}

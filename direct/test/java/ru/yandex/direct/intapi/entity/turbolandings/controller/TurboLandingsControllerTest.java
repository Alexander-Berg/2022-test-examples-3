package ru.yandex.direct.intapi.entity.turbolandings.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.turbolanding.model.StatusModerateForCpa;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLandingMetrikaCounter;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLandingMetrikaGoal;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.TurbolandingsStatusmoderateforcpa;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.turbolandings.TurboLandingsController;
import ru.yandex.direct.intapi.entity.turbolandings.model.IntapiTurboLanding;
import ru.yandex.direct.intapi.entity.turbolandings.model.InterfaceTypeEnum;
import ru.yandex.direct.intapi.entity.turbolandings.model.TurboLandingInfoResponse;
import ru.yandex.direct.intapi.entity.turbolandings.model.TurboPageModerationStatusEnum;
import ru.yandex.direct.intapi.entity.turbolandings.service.TurboLandingConverter;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.utils.JsonUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.UPDATE_COUNTER_GRANTS_JOB;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.textBannerData;
import static ru.yandex.direct.core.testing.data.TestSitelinkSets.sitelinkSet;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink;
import static ru.yandex.direct.dbschema.ppc.enums.TurbolandingsPreset.empty_preset;
import static ru.yandex.direct.intapi.util.IntapiUtils.getObjectFromResource;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class TurboLandingsControllerTest {
    public static final Long TEST_TL_ID = 30000695L;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SAVE_MAPPING = "/turboLandings/save_turbolanding";
    private static final String GET_INFO_MAPPING = "/turboLandings/get_turbolanding_info";
    private static final String SAVE_REQUEST_FILENAME = "/turbolandings/saveTurbolandingRequest.json";
    private static final String SAVE_CPA_TURBOLANDING_REQUEST_ITEM_RESOURCE = "/turbolandings" +
            "/saveCpaTurbolandingRequestItem.json";
    private static final String SAVE_REQUEST_FILENAME_WITH_PRESET
            = "/turbolandings/saveTurbolandingRequestWithPreset.json";
    @Autowired
    private TurboLandingsController controller;

    @Autowired
    private Steps steps;

    private MockMvc mockMvc;

    private ClientInfo clientInfo;
    private ClientId clientId;
    private Long clientUid;
    private List<Long> turbolandingIds;

    public static List<IntapiTurboLanding> getIntapiSaveTurbolandingRequestFromResource(
            String resourcePath) throws IOException {
        return getObjectFromResource(resourcePath, new TypeReference<>() {
        });
    }

    public static IntapiTurboLanding getIntapiSaveTurbolandingRequestItemFromResource(
            String resourcePath) throws IOException {
        return getObjectFromResource(resourcePath, IntapiTurboLanding.class);
    }

    public static void checkResponseIsSuccess(String response) {
        Map<String, Object> responseMap = JsonUtils.fromJson(response, HashMap.class);
        assertThat("Запрос завершился успехом", responseMap.get("success"), is(true));
    }

    public static void checkResponseIsFailed(String response) {
        Map<String, Object> responseMap = JsonUtils.fromJson(response, HashMap.class);
        assertThat("Запрос не завершился успехом", responseMap.get("success"), is(false));
    }

    public TurboLanding checkAndGetTurbolanding(Long turbolandingId) {
        List<TurboLanding> actualTurboLandings = steps.turboLandingSteps()
                .getTurbolandingsById(clientId, singleton(turbolandingId));
        assertThat("В базе есть турбостраница с нужным ID", actualTurboLandings.size(), is(1));
        return actualTurboLandings.get(0);
    }

    public String makeSaveTurbolandingRequest(String content) throws Exception {
        return mockMvc.perform(post(SAVE_MAPPING)
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(UTF_8.name())
                .param("client_id", clientId.toString())
                .param("operator_uid", clientUid.toString()))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Before
    public void before() {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        clientUid = clientInfo.getUid();
        TurboLanding turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(clientId);

        turbolandingIds = singletonList(turboLanding.getId());
    }

    @Test
    public void saveTurbolanding_turbolandingSaved() throws Exception {
        List<IntapiTurboLanding> request = getIntapiSaveTurbolandingRequestFromResource(SAVE_REQUEST_FILENAME);
        IntapiTurboLanding requestItem = request.get(0);
        requestItem.withId(TEST_TL_ID);

        String response = makeSaveTurbolandingRequest(JsonUtils.toJson(request));
        checkResponseIsSuccess(response);

        TurboLanding actualTurbolanding = checkAndGetTurbolanding(TEST_TL_ID);

        Set<TurboLandingMetrikaCounter> actualCounters = getTurbolandingCountersByTlId(TEST_TL_ID);
        Set<TurboLandingMetrikaGoal> actualGoals = getTurbolandingGoalsByTlId(TEST_TL_ID);

        Set<TurboLandingMetrikaCounter> expectedCounters = TurboLandingConverter
                .extractCounters(requestItem.getCounters());
        Set<TurboLandingMetrikaGoal> expectedGoals = TurboLandingConverter
                .extractGoals(requestItem.getCounters());

        assertThat("В базе сохранен нужный ID", actualTurbolanding.getId(), is(TEST_TL_ID));
        assertThat("В базе сохранен нужный preset", actualTurbolanding.getPreset(), is(empty_preset));
        assertThat("В базе сохранен нужный name", actualTurbolanding.getName(), is(requestItem.getName()));
        assertThat("В базе сохранен корректный turboSiteHref",
                actualTurbolanding.getTurboSiteHref(), is(requestItem.getTurboSiteHref()));
        assertThat("В базе сохранен корректный список счетчиков", actualCounters, beanDiffer(expectedCounters));
        assertThat("В базе сохранен корректный список целей", actualGoals, beanDiffer(expectedGoals));
        assertThat("isCpaModerationRequred соответствует ожиданиям", actualTurbolanding.getIsCpaModerationRequired(),
                is(false));
        assertThat("statusModerateForCpa соответствует ожиданиям", actualTurbolanding.getStatusModerateForCpa(),
                nullValue());
    }

    @Test
    public void saveTurbolanding_newCpaTurbolandingSaved() throws Exception {
        steps.featureSteps().addClientFeature(clientId,
                FeatureName.CPA_PAY_FOR_CONVERSIONS_TURBOLANDINGS_MODERATION_REQUIRED, true);

        Long turbolandingId = RandomNumberUtils.nextPositiveLong();
        IntapiTurboLanding requestItem =
                getIntapiSaveTurbolandingRequestItemFromResource(SAVE_CPA_TURBOLANDING_REQUEST_ITEM_RESOURCE)
                        .withId(turbolandingId);

        String responseString = makeSaveTurbolandingRequest(JsonUtils.toJson(List.of(requestItem)));
        checkResponseIsSuccess(responseString);
        var turboLanding = checkAndGetTurbolanding(turbolandingId);

        assertThat("В базе сохранен нужный ID", turboLanding.getId(), is(turbolandingId));
        assertThat("isCpaModerationRequred соответствует ожиданиям", turboLanding.getIsCpaModerationRequired(),
                is(true));
        assertThat("statusModerateForCpa соответствует ожиданиям", turboLanding.getStatusModerateForCpa(),
                is(StatusModerateForCpa.READY));
    }

    @Test
    public void saveTurbolanding_newCpaTurbolandingWithModerationStatusNull_Failed() throws Exception {
        steps.featureSteps().addClientFeature(clientId,
                FeatureName.CPA_PAY_FOR_CONVERSIONS_TURBOLANDINGS_MODERATION_REQUIRED, true);

        Long turbolandingId = RandomNumberUtils.nextPositiveLong();
        IntapiTurboLanding requestItem =
                getIntapiSaveTurbolandingRequestItemFromResource(SAVE_CPA_TURBOLANDING_REQUEST_ITEM_RESOURCE)
                        .withId(turbolandingId)
                        .withTurboPageModerationStatus(null);

        String responseString = makeSaveTurbolandingRequest(JsonUtils.toJson(List.of(requestItem)));
        checkResponseIsFailed(responseString);

        List<TurboLanding> actualTurboLandings = steps.turboLandingSteps()
                .getTurbolandingsById(clientId, singleton(turbolandingId));
        assertThat("Турбостраница не сохранилась", actualTurboLandings.size(), is(0));
    }

    @Test
    public void saveTurbolanding_cpaTurbolandingNewVersion() throws Exception {
        steps.featureSteps().addClientFeature(clientId,
                FeatureName.CPA_PAY_FOR_CONVERSIONS_TURBOLANDINGS_MODERATION_REQUIRED, true);

        final Long turbolandingId = RandomNumberUtils.nextPositiveLong();
        IntapiTurboLanding requestItem =
                getIntapiSaveTurbolandingRequestItemFromResource(SAVE_CPA_TURBOLANDING_REQUEST_ITEM_RESOURCE)
                        .withId(turbolandingId);

        String responseString = makeSaveTurbolandingRequest(JsonUtils.toJson(List.of(requestItem)));
        checkResponseIsSuccess(responseString);
        checkAndGetTurbolanding(turbolandingId);
        //симулируем отправку в модерацию
        steps.turboLandingSteps().setStatusModerateForCpa(clientId, List.of(turbolandingId),
                TurbolandingsStatusmoderateforcpa.Sent);

        //повторно сохраняем с новой версией
        var expectedVersion = requestItem.getVersion() + 1;
        requestItem
                .withTurboPageModerationStatus(TurboPageModerationStatusEnum.READY)
                .withVersion(expectedVersion);
        responseString = makeSaveTurbolandingRequest(JsonUtils.toJson(List.of(requestItem)));
        checkResponseIsSuccess(responseString);
        var turboLanding = checkAndGetTurbolanding(turbolandingId);

        assertThat("В базе сохранен нужный ID", turboLanding.getId(), is(turbolandingId));
        assertThat("В базе сохранилась новая версия", turboLanding.getVersion(), is(expectedVersion));
        assertThat("isCpaModerationRequred соответствует ожиданиям", turboLanding.getIsCpaModerationRequired(),
                is(true));
        assertThat("statusModerateForCpa соответствует ожиданиям", turboLanding.getStatusModerateForCpa(),
                is(StatusModerateForCpa.READY));
    }

    @Test
    public void saveTurbolanding_cpaTurbolandingUpdateModerationStatus() throws Exception {
        steps.featureSteps().addClientFeature(clientId,
                FeatureName.CPA_PAY_FOR_CONVERSIONS_TURBOLANDINGS_MODERATION_REQUIRED, true);

        final Long turbolandingId = RandomNumberUtils.nextPositiveLong();
        IntapiTurboLanding requestItem =
                getIntapiSaveTurbolandingRequestItemFromResource(SAVE_CPA_TURBOLANDING_REQUEST_ITEM_RESOURCE)
                        .withId(turbolandingId);

        String responseString = makeSaveTurbolandingRequest(JsonUtils.toJson(List.of(requestItem)));
        checkResponseIsSuccess(responseString);
        checkAndGetTurbolanding(turbolandingId);
        //симулируем отправку в модерацию
        steps.turboLandingSteps().setStatusModerateForCpa(clientId, List.of(turbolandingId),
                TurbolandingsStatusmoderateforcpa.Sent);

        //повторно сохраняем с новым статусом модерации
        requestItem
                .withTurboPageModerationStatus(TurboPageModerationStatusEnum.YES);
        responseString = makeSaveTurbolandingRequest(JsonUtils.toJson(List.of(requestItem)));
        checkResponseIsSuccess(responseString);
        var turboLanding = checkAndGetTurbolanding(turbolandingId);

        assertThat("В базе сохранен нужный ID", turboLanding.getId(), is(turbolandingId));
        assertThat("В базе сохранилась новая версия", turboLanding.getVersion(), is(requestItem.getVersion()));
        assertThat("isCpaModerationRequred соответствует ожиданиям", turboLanding.getIsCpaModerationRequired(),
                is(true));
        assertThat("statusModerateForCpa соответствует ожиданиям", turboLanding.getStatusModerateForCpa(),
                is(StatusModerateForCpa.YES));
    }

    @Test
    public void saveTurbolanding_updateCpaTurbolandingWithModerationStatusNull() throws Exception {
        //Когда турбостраница переносится пользователем на другой сайт, у нее не меняется версия
        //  и текущий статус модерации нужно оставить без изменений.
        //Для таких случаев разрешаем принимать moderationStatus = null (DIRECT-117772)

        steps.featureSteps().addClientFeature(clientId,
                FeatureName.CPA_PAY_FOR_CONVERSIONS_TURBOLANDINGS_MODERATION_REQUIRED, true);

        final Long turbolandingId = RandomNumberUtils.nextPositiveLong();
        IntapiTurboLanding requestItem =
                getIntapiSaveTurbolandingRequestItemFromResource(SAVE_CPA_TURBOLANDING_REQUEST_ITEM_RESOURCE)
                        .withId(turbolandingId);

        String responseString = makeSaveTurbolandingRequest(JsonUtils.toJson(List.of(requestItem)));
        checkResponseIsSuccess(responseString);
        checkAndGetTurbolanding(turbolandingId);
        //симулируем отправку в модерацию
        steps.turboLandingSteps().setStatusModerateForCpa(clientId, List.of(turbolandingId),
                TurbolandingsStatusmoderateforcpa.Sent);

        //обновляем данные турбостраницы без изменения статуса модерации (null)
        requestItem
                .withTurboPageModerationStatus(null)
                .withUrl("https://newurl.site/turbopage_123456");
        responseString = makeSaveTurbolandingRequest(JsonUtils.toJson(List.of(requestItem)));
        checkResponseIsSuccess(responseString);
        var turboLanding = checkAndGetTurbolanding(turbolandingId);

        assertThat("В базе сохранен нужный ID", turboLanding.getId(), is(turbolandingId));
        assertThat("В базе сохранилась новая версия", turboLanding.getVersion(), is(requestItem.getVersion()));
        assertThat("isCpaModerationRequred соответствует ожиданиям", turboLanding.getIsCpaModerationRequired(),
                is(true));
        assertThat("statusModerateForCpa соответствует ожиданиям", turboLanding.getStatusModerateForCpa(),
                is(StatusModerateForCpa.SENT));
    }

    @Test
    public void saveTurbolanding_updateCpaTurbolandingNewVersionWithModerationStatusNull_Fail() throws Exception {
        //Когда турбостраница переносится пользователем на другой сайт, у нее не меняется версия
        //  и текущий статус модерации нужно оставить без изменений.
        //Для таких случаев разрешаем принимать moderationStatus = null (DIRECT-117772)

        steps.featureSteps().addClientFeature(clientId,
                FeatureName.CPA_PAY_FOR_CONVERSIONS_TURBOLANDINGS_MODERATION_REQUIRED, true);

        final Long turbolandingId = RandomNumberUtils.nextPositiveLong();
        IntapiTurboLanding requestItem =
                getIntapiSaveTurbolandingRequestItemFromResource(SAVE_CPA_TURBOLANDING_REQUEST_ITEM_RESOURCE)
                        .withId(turbolandingId);

        steps.dbQueueSteps().registerJobType(UPDATE_COUNTER_GRANTS_JOB);
        String responseString = makeSaveTurbolandingRequest(JsonUtils.toJson(List.of(requestItem)));
        checkResponseIsSuccess(responseString);
        checkAndGetTurbolanding(turbolandingId);
        //симулируем отправку в модерацию
        steps.turboLandingSteps().setStatusModerateForCpa(clientId, List.of(turbolandingId),
                TurbolandingsStatusmoderateforcpa.Sent);

        //обновляем данные турбостраницы без изменения статуса модерации (null)
        var expectedVersion = requestItem.getVersion() + 1;
        requestItem
                .withTurboPageModerationStatus(null)
                .withUrl("https://newurl.site/turbopage_123456")
                .withVersion(expectedVersion);
        responseString = makeSaveTurbolandingRequest(JsonUtils.toJson(List.of(requestItem)));

        checkResponseIsFailed(responseString);
    }


    @Test
    public void getTurboLandingInfo_notLinkedTurboLanding() throws Exception {
        TurboLandingInfoResponse response = callGetTurboLandingInfo();
        assertThat("Запрос завершился успехом", response.isSuccessful(), is(true));
        assertThat("С турболендингом связано 0 баннеров", response.getBannersCount(), is(0));
    }

    @Test
    public void getTurboLandingInfo_interfaceClassic() throws Exception {
        steps.featureSteps().addClientFeature(clientId, FeatureName.GRID, false);
        TurboLandingInfoResponse response = callGetTurboLandingInfo();
        assertThat("Клиент пользуется классическим интерфейсом",
                response.getInterfaceType(), is(InterfaceTypeEnum.CLASSIC));
    }

    @Test
    public void getTurboLandingInfo_interfaceGrid() throws Exception {
        steps.featureSteps().addClientFeature(clientId, FeatureName.GRID, true);
        TurboLandingInfoResponse response = callGetTurboLandingInfo();
        assertThat("Клиент пользуется новым интерфейсом",
                response.getInterfaceType(), is(InterfaceTypeEnum.GRID));
    }

    @Test
    public void getTurboLandingInfo_linkedTurboLandingDifferentBanners() throws Exception {
        var turboLandingId = turbolandingIds.get(0);
        Sitelink sitelink = defaultSitelink().withTurboLandingId(turboLandingId);
        SitelinkSet sitelinkSet = sitelinkSet(clientId, singletonList(sitelink));
        steps.sitelinkSetSteps().createSitelinkSet(sitelinkSet, clientInfo);
        //Привязка только к основной ссылке баннера
        steps.bannerSteps()
                .createBanner(
                        activeTextBanner(textBannerData()
                                .withTurboLandingId(turboLandingId)
                                .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.YES)),
                        clientInfo);

        //Привязка только к сайтлтинку
        steps.bannerSteps()
                .createBanner(
                        activeTextBanner(textBannerData()
                                .withSitelinksSetId(sitelinkSet.getId())),
                        clientInfo);

        TurboLandingInfoResponse response = callGetTurboLandingInfo();
        assertThat("С турболендингом связано 2 баннера", response.getBannersCount(), is(2));
    }

    @Test
    public void getTurboLandingInfo_linkedTurboLandingOneBanner() throws Exception {
        var turboLandingId = turbolandingIds.get(0);
        Sitelink sitelink = defaultSitelink().withTurboLandingId(turboLandingId);
        SitelinkSet sitelinkSet = sitelinkSet(clientId, singletonList(sitelink));
        steps.sitelinkSetSteps().createSitelinkSet(sitelinkSet, clientInfo);
        //Привязка и к основной ссылке баннера и к сайтлтинку
        steps.bannerSteps()
                .createBanner(
                        activeTextBanner(textBannerData()
                                .withSitelinksSetId(sitelinkSet.getId())
                                .withTurboLandingId(turboLandingId)
                                .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.YES)),
                        clientInfo);

        TurboLandingInfoResponse response = callGetTurboLandingInfo();
        assertThat("С турболендингом связан 1 баннер", response.getBannersCount(), is(1));
    }

    private TurboLandingInfoResponse callGetTurboLandingInfo() throws Exception {
        String r = mockMvc.perform(get(GET_INFO_MAPPING)
                .param("user_id", clientUid.toString())
                .param("client_id", clientId.toString())
                .param("tl_id", turbolandingIds.get(0).toString())
        )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        return MAPPER.readerFor(TurboLandingInfoResponse.class).readValue(r);
    }

    private Set<TurboLandingMetrikaCounter> getTurbolandingCountersByTlId(Long id) {
        var countersFromDb = steps.turboLandingSteps().getTurbolandingCountersByTlId(clientId, singleton(id));
        return StreamEx.of(countersFromDb)
                .nonNull()
                .filter(c -> c.getTurbolandingId().equals(id))
                .map(c -> new TurboLandingMetrikaCounter()
                        .withId(c.getCounterId())
                        .withIsUserCounter(c.getIsUserCounter()))
                .toSet();
    }

    private Set<TurboLandingMetrikaGoal> getTurbolandingGoalsByTlId(Long id) {
        var goalsFromDb = steps.turboLandingSteps().getTurbolandingGoalsByTlId(clientId, singleton(id));
        return StreamEx.of(goalsFromDb)
                .nonNull()
                .filter(c -> c.getTurbolandingId().equals(id))
                .map(c -> new TurboLandingMetrikaGoal()
                        .withId(c.getGoalId())
                        .withIsConversionGoal(c.getIsConversionGoal()))
                .toSet();
    }
}

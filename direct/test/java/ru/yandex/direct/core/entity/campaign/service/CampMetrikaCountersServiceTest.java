package ru.yandex.direct.core.entity.campaign.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.EntryStream;
import one.util.streamex.LongStreamEx;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.campaign.container.DeleteCampMetrikaCountersRequest;
import ru.yandex.direct.core.entity.campaign.container.UpdateCampMetrikaCountersRequest;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMetrikaCounters;
import ru.yandex.direct.core.entity.campaign.model.MetrikaCounter;
import ru.yandex.direct.core.entity.campaign.model.MetrikaCounterSource;
import ru.yandex.direct.core.entity.campaign.repository.CampMetrikaCountersRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.metrika.repository.CalltrackingNumberClicksRepository;
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCampaignRepository;
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCounterByDomainRepository;
import ru.yandex.direct.core.entity.metrikacounter.model.MetrikaCounterWithAdditionalInformation;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbschema.ppc.enums.MetrikaCountersSource;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect;
import ru.yandex.direct.metrika.client.model.response.GetExistentCountersResponse;
import ru.yandex.direct.metrika.client.model.response.GetExistentCountersResponseItem;
import ru.yandex.direct.metrika.client.model.response.UserCounters;
import ru.yandex.direct.metrika.client.model.response.UserCountersExtended;
import ru.yandex.direct.metrika.client.model.response.UserCountersExtendedResponse;
import ru.yandex.direct.metrika.client.model.response.UserCountersResponse;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.function.Predicate.not;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.converter.CampaignConverter.SPRAV;
import static ru.yandex.direct.core.entity.campaign.converter.CampaignConverter.fromMetrikaCounterSource;
import static ru.yandex.direct.core.entity.campaign.service.CampMetrikaCountersService.MAX_METRIKA_COUNTERS_COUNT;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampMetrikaCountersServiceTest {
    private static final List<MetrikaCounter> OLD_TEXT_CAMP_COUNTERS = asList(
            new MetrikaCounter().withId(11L).withSource(MetrikaCounterSource.SPRAV),
            new MetrikaCounter().withId(12L),
            new MetrikaCounter().withId(13L));
    private static final List<Long> OLD_TEXT_CAMP_COUNTERS_IDS = OLD_TEXT_CAMP_COUNTERS.stream()
            .map(MetrikaCounter::getId)
            .collect(Collectors.toList());
    private static final List<MetrikaCounter> OLD_PERFORMANCE_CAMP_COUNTERS = asList(
            new MetrikaCounter().withId(20L));
    private static final List<Long> OLD_PERFORMANCE_CAMP_COUNTERS_IDS = OLD_PERFORMANCE_CAMP_COUNTERS.stream()
            .map(MetrikaCounter::getId)
            .collect(Collectors.toList());
    public static final long NEW_COUNTER_ID_FIRST = 14L;
    public static final long NEW_COUNTER_ID_SECOND = 15L; // Счётчик справочника
    public static final long NEW_COUNTER_ID_THIRD = 16L;

    @Autowired
    private Steps steps;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private BannerCommonRepository bannerCommonRepository;
    @Autowired
    private CalltrackingNumberClicksRepository calltrackingNumberClicksRepository;
    @Autowired
    private CampMetrikaCountersRepository campMetrikaCountersRepository;
    @Autowired
    private RbacService rbacService;
    @Autowired
    private FeatureService featureService;
    @Autowired
    TestCampaignRepository testCampaignRepository;
    @Autowired
    MetrikaCampaignRepository metrikaCampaignRepository;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private MetrikaCounterByDomainRepository metrikaCounterByDomainRepository;
    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

    private MetrikaClient metrikaClient;
    private CampMetrikaCountersService campMetrikaCountersService;

    private int shard;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private Long textCampaignId;
    private Long performanceCampaignId;


    @Before
    public void before() {
        //создаем кампании
        Campaign textCampaignTest =
                TestCampaigns.activeTextCampaign(null, null);
        Campaign performanceCampaignTest =
                TestCampaigns.activePerformanceCampaign(null, null);

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(textCampaignTest);
        textCampaignId = campaignInfo.getCampaignId();
        clientInfo = campaignInfo.getClientInfo();
        performanceCampaignId = steps.campaignSteps().createCampaign(
                performanceCampaignTest, clientInfo).getCampaignId();

        clientId = campaignInfo.getClientId();
        shard = campaignInfo.getShard();

        //добавляем в кампании счетчики
        Map<Long, List<MetrikaCounter>> countersByCids = EntryStream.of(
                        textCampaignId, OLD_TEXT_CAMP_COUNTERS,
                        performanceCampaignId, OLD_PERFORMANCE_CAMP_COUNTERS)
                .toMap();
        campMetrikaCountersRepository.updateMetrikaCounters(shard, countersByCids);
        campMetrikaCountersRepository.updateTableMetrikaCounters(dslContextProvider.ppc(shard), countersByCids);

        //создаем service, который будем тестировать
        metrikaClient = mock(MetrikaClient.class);

        when(metrikaClient.getUsersCountersNumExtended(eq(List.of(campaignInfo.getUid()))))
                .thenReturn(List.of(new UserCountersExtended()
                        .withOwner(campaignInfo.getUid())
                        .withCountersCnt(1)
                        .withCounters(List.of(
                                new CounterInfoDirect()
                                        .withSitePath("https://yandex.ru")
                                        .withCounterPermission("own")
                                        .withId(OLD_TEXT_CAMP_COUNTERS.get(0).getId().intValue())
                                        .withCounterSource(fromMetrikaCounterSource(
                                                OLD_TEXT_CAMP_COUNTERS.get(0).getSource()))
                        ))));

        when(metrikaClient.getUsersCountersNumExtended2(eq(List.of(campaignInfo.getUid())), any()))
                .thenReturn(
                        new UserCountersExtendedResponse()
                                .withUsers(
                                        List.of(new UserCountersExtended()
                                                .withOwner(campaignInfo.getUid())
                                                .withCountersCnt(5)
                                                .withCounters(List.of(
                                                        new CounterInfoDirect()
                                                                .withSitePath(RandomStringUtils.randomAlphabetic(5))
                                                                .withId(OLD_TEXT_CAMP_COUNTERS.get(0).getId().intValue())
                                                                .withCounterSource(fromMetrikaCounterSource(
                                                                        OLD_TEXT_CAMP_COUNTERS.get(0).getSource())),
                                                        new CounterInfoDirect()
                                                                .withSitePath(RandomStringUtils.randomAlphabetic(5))
                                                                .withId(OLD_TEXT_CAMP_COUNTERS.get(1).getId().intValue()),
                                                        new CounterInfoDirect()
                                                                .withSitePath(RandomStringUtils.randomAlphabetic(5))
                                                                .withId(OLD_TEXT_CAMP_COUNTERS.get(2).getId().intValue()),
                                                        new CounterInfoDirect()
                                                                .withSitePath(RandomStringUtils.randomAlphabetic(5))
                                                                .withId((int) NEW_COUNTER_ID_FIRST),
                                                        new CounterInfoDirect()
                                                                .withSitePath(RandomStringUtils.randomAlphabetic(5))
                                                                .withId((int) NEW_COUNTER_ID_SECOND)
                                                                .withCounterSource(SPRAV),
                                                        new CounterInfoDirect()
                                                                .withSitePath(RandomStringUtils.randomAlphabetic(5))
                                                                .withId((int) NEW_COUNTER_ID_THIRD),
                                                        new CounterInfoDirect()
                                                                .withSitePath(RandomStringUtils.randomAlphabetic(5))
                                                                .withId(OLD_PERFORMANCE_CAMP_COUNTERS.get(0).getId().intValue())
                                                )))));

        campMetrikaCountersService = new CampMetrikaCountersService(
                metrikaClient, rbacService, featureService, shardHelper,
                calltrackingNumberClicksRepository, campMetrikaCountersRepository,
                bannerCommonRepository, campaignRepository, metrikaCounterByDomainRepository,
                metrikaCampaignRepository, strategyTypedRepository, ppcPropertiesSupport);
    }

    // добавление счетчиков метрики в кампании (addCampMetrikaCounters)
    @Test
    public void addCampMetrikaCounters_whenTwoCampaigns() {
        List<Long> newMetrikaCounters = asList(NEW_COUNTER_ID_FIRST, NEW_COUNTER_ID_SECOND);
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(asList(textCampaignId, performanceCampaignId))
                .withMetrikaCounters(newMetrikaCounters);

        Result<UpdateCampMetrikaCountersRequest> operationResult =
                campMetrikaCountersService.addCampMetrikaCounters(clientId, request, Applicability.PARTIAL);
        checkOperationResult(operationResult, true);

        Map<Long, CampaignWithMetrikaCounters> campaignsMap =
                EntryStream.of(campaignTypedRepository.getTypedCampaignsMap(shard,
                        List.of(textCampaignId, performanceCampaignId)))
                        .selectValues(CampaignWithMetrikaCounters.class)
                        .toMap();

        SoftAssertions soft = new SoftAssertions();

        List<Long> expectedCounterIds = StreamEx.of(OLD_TEXT_CAMP_COUNTERS_IDS).append(newMetrikaCounters).toList();
        soft.assertThat(campaignsMap.get(textCampaignId).getMetrikaCounters())
                .as("в текстовую кампанию добавились счетчики")
                .isEqualTo(expectedCounterIds);

        Map<Long, MetrikaCountersSource> textCampaignCountersSources =
                testCampaignRepository.getMetrikaCountersSources(shard, textCampaignId, expectedCounterIds);

        soft.assertThat(textCampaignCountersSources)
                .as("источники счетчиков тго корректно сохранились")
                .isEqualTo(Map.of(OLD_TEXT_CAMP_COUNTERS_IDS.get(0), MetrikaCountersSource.sprav,
                        OLD_TEXT_CAMP_COUNTERS_IDS.get(1), MetrikaCountersSource.unknown,
                        OLD_TEXT_CAMP_COUNTERS_IDS.get(2), MetrikaCountersSource.unknown,
                        newMetrikaCounters.get(0), MetrikaCountersSource.unknown,
                        newMetrikaCounters.get(1), MetrikaCountersSource.sprav));

        //в performance кампанию нельзя добавить счетчики
        soft.assertThat(campaignsMap.get(performanceCampaignId).getMetrikaCounters())
                .as("в performance кампании значения счетчиков не изменились")
                .isEqualTo(OLD_PERFORMANCE_CAMP_COUNTERS_IDS);

        Map<Long, MetrikaCountersSource> performanceCampaignCountersSources =
                testCampaignRepository.getMetrikaCountersSources(shard, performanceCampaignId,
                        OLD_PERFORMANCE_CAMP_COUNTERS_IDS);

        soft.assertThat(performanceCampaignCountersSources)
                .as("источники счетчиков performance кампании не изменились")
                .isEqualTo(Map.of(OLD_PERFORMANCE_CAMP_COUNTERS_IDS.get(0), MetrikaCountersSource.unknown));
        soft.assertAll();
    }

    @Test
    public void addCampMetrikaCounters_whenTooManyCounters() {
        List<Long> newMetrikaCounters = LongStreamEx.rangeClosed(100L, 200).boxed().toList();
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(textCampaignId))
                .withMetrikaCounters(newMetrikaCounters);

        //с учетом счетчиков которые уже есть в кампании их стало бы слишком много
        Result<UpdateCampMetrikaCountersRequest> operationResult =
                campMetrikaCountersService.addCampMetrikaCounters(clientId, request, Applicability.PARTIAL);
        checkOperationResult(operationResult, true);

        Map<Long, List<Long>> actualCounters = campMetrikaCountersRepository.getMetrikaCountersByCids(
                shard, singletonList(textCampaignId));

        assertThat("значения счетчиков не изменились",
                actualCounters.get(textCampaignId), equalTo(OLD_TEXT_CAMP_COUNTERS_IDS));
    }

    @Test
    public void addCampMetrikaCounters_whenNotPartialOperation() {
        List<Long> newMetrikaCounters = asList(NEW_COUNTER_ID_FIRST, NEW_COUNTER_ID_SECOND);
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(asList(textCampaignId, performanceCampaignId))
                .withMetrikaCounters(newMetrikaCounters);

        //если partialOperation=false, то счетчики не должны меняться ни в одной кампании если есть ошибки
        Result<UpdateCampMetrikaCountersRequest> operationResult =
                campMetrikaCountersService.addCampMetrikaCounters(clientId, request, Applicability.FULL);
        checkState(!operationResult.isSuccessful(), "результат операции должен быть отрицательный");

        Map<Long, List<Long>> actualCounters = campMetrikaCountersRepository.getMetrikaCountersByCids(
                shard, asList(textCampaignId, performanceCampaignId));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(actualCounters.get(textCampaignId))
                .as("в текстовой кампании значения счетчиков не изменились")
                .isEqualTo(OLD_TEXT_CAMP_COUNTERS_IDS);

        soft.assertThat(actualCounters.get(performanceCampaignId))
                .as("в performance кампании значения счетчиков не изменились")
                .isEqualTo(OLD_PERFORMANCE_CAMP_COUNTERS_IDS);
        soft.assertAll();
    }

    //Проверяем, что получаем те же счетчики, что и создали
    @Test
    public void getCampMetricaCountersWithFilter(){
        Set<Long> campaignIds = Set.of(textCampaignId, performanceCampaignId);
        Set<Long> counterIds = new HashSet<>(OLD_TEXT_CAMP_COUNTERS_IDS);
        counterIds.addAll(OLD_PERFORMANCE_CAMP_COUNTERS_IDS);
        SoftAssertions soft = new SoftAssertions();
        Set<Long> actualCounters = campMetrikaCountersRepository.getMetrikaCountersByCids(
                shard, campaignIds).values().stream().flatMap(List::stream).collect(Collectors.toSet());
        Set<Long> countersByClientAndCampaignId = campMetrikaCountersService
                .getAvailableCountersByClientAndCampaignId(clientId, null, campaignIds)
                .getClientAvailableCounters().stream()
                .map(MetrikaCounterWithAdditionalInformation::getId)
                .collect(Collectors.toSet());
        soft.assertThat(actualCounters)
                .as("счетчики из базы те же")
                .isEqualTo(counterIds);
        counterIds.addAll(asList(NEW_COUNTER_ID_FIRST, NEW_COUNTER_ID_SECOND, NEW_COUNTER_ID_THIRD));
        soft.assertThat(countersByClientAndCampaignId)
                .as("счетчики из ручки те же")
                .isEqualTo(counterIds);
        soft.assertAll();
    }

    // удаление счетчиков метрики в кампании (removeCampMetrikaCounters)
    @Test
    public void removeCampMetrikaCounters_whenOneCampaignAffected() {
        List<Long> deleteMetrikaCounters = StreamEx.of(OLD_TEXT_CAMP_COUNTERS_IDS).skip(1).toList();
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(List.of(textCampaignId, performanceCampaignId))
                .withMetrikaCounters(deleteMetrikaCounters);

        Result<UpdateCampMetrikaCountersRequest> operationResult =
                campMetrikaCountersService.removeCampMetrikaCounters(clientId, request, Applicability.PARTIAL);
        checkOperationResult(operationResult, false);

        Map<Long, List<Long>> actualCounters = campMetrikaCountersRepository.getMetrikaCountersByCids(
                shard, List.of(textCampaignId, performanceCampaignId));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(actualCounters.get(textCampaignId))
                .as("в текстовой кампании удалили счетчики")
                .isEqualTo(StreamEx.of(OLD_TEXT_CAMP_COUNTERS_IDS).remove(deleteMetrikaCounters::contains).toList());

        //в performance кампанию нельзя удалить счетчики
        soft.assertThat(actualCounters.get(performanceCampaignId))
                .as("в performance кампании значения счетчиков не изменились")
                .isEqualTo(OLD_PERFORMANCE_CAMP_COUNTERS_IDS);
        soft.assertAll();
    }

    @Test
    public void removeCampMetrikaCounters_whenTwoCampaigns() {
        List<Long> deleteMetrikaCounters = List.of(OLD_TEXT_CAMP_COUNTERS_IDS.get(1),
                OLD_PERFORMANCE_CAMP_COUNTERS_IDS.get(0));
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(List.of(textCampaignId, performanceCampaignId))
                .withMetrikaCounters(deleteMetrikaCounters);

        Result<UpdateCampMetrikaCountersRequest> operationResult =
                campMetrikaCountersService.removeCampMetrikaCounters(clientId, request, Applicability.PARTIAL);
        checkOperationResult(operationResult, true);

        Map<Long, List<Long>> actualCounters = campMetrikaCountersRepository.getMetrikaCountersByCids(
                shard, List.of(textCampaignId, performanceCampaignId));

        SoftAssertions soft = new SoftAssertions();
        List<Long> expectedCounterIds = filterList(OLD_TEXT_CAMP_COUNTERS_IDS, not(deleteMetrikaCounters::contains));
        soft.assertThat(actualCounters.get(textCampaignId))
                .as("в текстовой кампании удалили счетчики")
                .isEqualTo(expectedCounterIds);

        Map<Long, MetrikaCountersSource> textCampaignCountersSources =
                testCampaignRepository.getMetrikaCountersSources(shard, textCampaignId, expectedCounterIds);

        soft.assertThat(textCampaignCountersSources)
                .as("источники счетчиков тго корректно сохранились")
                .isEqualTo(Map.of(OLD_TEXT_CAMP_COUNTERS_IDS.get(0), MetrikaCountersSource.sprav,
                        OLD_TEXT_CAMP_COUNTERS_IDS.get(2), MetrikaCountersSource.unknown));

        //в performance кампанию нельзя удалить счетчики
        soft.assertThat(actualCounters.get(performanceCampaignId))
                .as("в performance кампании значения счетчиков не изменились")
                .isEqualTo(OLD_PERFORMANCE_CAMP_COUNTERS_IDS);

        Map<Long, MetrikaCountersSource> performanceCampaignCountersSources =
                testCampaignRepository.getMetrikaCountersSources(shard, performanceCampaignId,
                        OLD_PERFORMANCE_CAMP_COUNTERS_IDS);

        soft.assertThat(performanceCampaignCountersSources)
                .as("источники счетчиков performance кампании не изменились")
                .isEqualTo(Map.of(OLD_PERFORMANCE_CAMP_COUNTERS_IDS.get(0), MetrikaCountersSource.unknown));

        soft.assertAll();
    }

    @Test
    public void removeCampMetrikaCounters_whenNotPartialOperation() {
        List<Long> deleteMetrikaCounters = List.of(OLD_TEXT_CAMP_COUNTERS_IDS.get(0),
                OLD_PERFORMANCE_CAMP_COUNTERS_IDS.get(0));
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(List.of(textCampaignId, performanceCampaignId))
                .withMetrikaCounters(deleteMetrikaCounters);

        //если partialOperation=false, то счетчики не должны меняться ни в одной кампании если есть ошибки
        Result<UpdateCampMetrikaCountersRequest> operationResult =
                campMetrikaCountersService.removeCampMetrikaCounters(clientId, request, Applicability.FULL);
        checkState(!operationResult.isSuccessful(), "результат операции должен быть отрицательный");

        Map<Long, List<Long>> actualCounters = campMetrikaCountersRepository.getMetrikaCountersByCids(
                shard, List.of(textCampaignId, performanceCampaignId));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(actualCounters.get(textCampaignId))
                .as("в текстовой кампании значения счетчиков не изменились")
                .isEqualTo(OLD_TEXT_CAMP_COUNTERS_IDS);

        soft.assertThat(actualCounters.get(performanceCampaignId))
                .as("в performance кампании значения счетчиков не изменились")
                .isEqualTo(OLD_PERFORMANCE_CAMP_COUNTERS_IDS);
        soft.assertAll();
    }

    @Test
    public void removeCampMetrikaCounters_whenTooManyCounters() {
        List<Long> deleteMetrikaCounters = LongStreamEx.rangeClosed(30, MAX_METRIKA_COUNTERS_COUNT + 1)
                .boxed()
                .append(OLD_TEXT_CAMP_COUNTERS_IDS.stream().skip(1).collect(Collectors.toList()))
                .toList();
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(textCampaignId))
                .withMetrikaCounters(deleteMetrikaCounters);

        //при удалении не должны проверять кол-во счетчиков из запроса
        Result<UpdateCampMetrikaCountersRequest> operationResult =
                campMetrikaCountersService.removeCampMetrikaCounters(clientId, request, Applicability.PARTIAL);
        checkOperationResult(operationResult, false);

        Map<Long, List<Long>> actualCounters = campMetrikaCountersRepository
                .getMetrikaCountersByCids(shard, List.of(textCampaignId));

        Assertions.assertThat(actualCounters.get(textCampaignId))
                .as("в текстовой кампании удалили вcе счетчики, которые можно удалять (счётчик справочника " +
                        "удалять нельзя).")
                .isEqualTo(List.of(OLD_TEXT_CAMP_COUNTERS_IDS.get(0)));
    }


    // замена счетчиков метрики в кампаниях (replaceCampMetrikaCounters)
    @Test
    public void replaceCampMetrikaCounters_whenCounterIsAvailable() {
        List<Long> newMetrikaCounters = asList(
                NEW_COUNTER_ID_FIRST,
                NEW_COUNTER_ID_SECOND,
                OLD_TEXT_CAMP_COUNTERS_IDS.get(0)); // счётчики справочника удалять нельзя, поэтому включаем их в "new"
        //первый счетчик доступен пользователю
        when(metrikaClient.getUsersCountersNum2(any(), any())).thenReturn(
                new UserCountersResponse()
                        .withUsers(singletonList(new UserCounters().withCounterIds(singletonList(14)))));

        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(asList(textCampaignId, performanceCampaignId))
                .withMetrikaCounters(newMetrikaCounters);

        Result<UpdateCampMetrikaCountersRequest> operationResult =
                campMetrikaCountersService.replaceCampMetrikaCounters(clientId, request, Applicability.PARTIAL);
        checkOperationResult(operationResult, false);

        Map<Long, List<Long>> actualCounters = campMetrikaCountersRepository.getMetrikaCountersByCids(
                shard, asList(textCampaignId, performanceCampaignId));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(actualCounters.get(textCampaignId))
                .as("в текстовой кампании новые значения счетчиков такие же как в запросе")
                .isEqualTo(newMetrikaCounters);

        Map<Long, MetrikaCountersSource> textCampaignCountersSources =
                testCampaignRepository.getMetrikaCountersSources(shard, textCampaignId, newMetrikaCounters);

        soft.assertThat(textCampaignCountersSources)
                .as("источники счетчиков тго корректно сохранились")
                .isEqualTo(Map.of(
                        newMetrikaCounters.get(0), MetrikaCountersSource.unknown,
                        newMetrikaCounters.get(1), MetrikaCountersSource.sprav,
                        OLD_TEXT_CAMP_COUNTERS_IDS.get(0), MetrikaCountersSource.sprav));

        //в performance кампании может быть только один счетчик
        soft.assertThat(actualCounters.get(performanceCampaignId))
                .as("в performance кампании установлен первый счетчик из запроса")
                .isEqualTo(singletonList(newMetrikaCounters.get(0)));

        Map<Long, MetrikaCountersSource> performanceCampaignCountersSources =
                testCampaignRepository.getMetrikaCountersSources(shard, performanceCampaignId,
                        List.of(newMetrikaCounters.get(0)));

        soft.assertThat(performanceCampaignCountersSources)
                .as("источники счетчиков performance кампании не изменились")
                .isEqualTo(Map.of(newMetrikaCounters.get(0), MetrikaCountersSource.unknown));

        soft.assertAll();
    }

    @Test
    public void replaceCampMetrikaCounters_whenCounterIsUnavailable() {
        List<Long> newMetrikaCounters = asList(NEW_COUNTER_ID_FIRST, NEW_COUNTER_ID_SECOND);
        //первый счетчик не доступен пользователю, поэтому его нельзя установить в performance кампании
        when(metrikaClient.getUsersCountersNum2(any(), any()))
                .thenReturn(new UserCountersResponse().withUsers(Collections.emptyList()));

        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(performanceCampaignId))
                .withMetrikaCounters(newMetrikaCounters);

        Result<UpdateCampMetrikaCountersRequest> operationResult =
                campMetrikaCountersService.replaceCampMetrikaCounters(clientId, request, Applicability.PARTIAL);
        checkOperationResult(operationResult, true);

        Map<Long, List<Long>> actualCounters = campMetrikaCountersRepository.getMetrikaCountersByCids(
                shard, singletonList(performanceCampaignId));

        assertThat("в performance кампании значения счетчиков не изменились",
                actualCounters.get(performanceCampaignId), equalTo(OLD_PERFORMANCE_CAMP_COUNTERS_IDS));
    }

    @Test
    public void replaceCampMetrikaCounters_whenRequestWithoutCounters() {
        //в запросе нет счетчиков, поэтому значения не должны меняться
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(textCampaignId))
                .withMetrikaCounters(Collections.emptyList());

        Result<UpdateCampMetrikaCountersRequest> operationResult =
                campMetrikaCountersService.replaceCampMetrikaCounters(clientId, request, Applicability.PARTIAL);
        checkState(!operationResult.isSuccessful(), "результат операции должен быть отрицательный");

        Map<Long, List<Long>> actualCounters = campMetrikaCountersRepository.getMetrikaCountersByCids(
                shard, singletonList(textCampaignId));
        assertThat("значения счетчиков не изменились",
                actualCounters.get(textCampaignId), equalTo(OLD_TEXT_CAMP_COUNTERS_IDS));
    }


    // удаление счетчиков метрики из кампаний (deleteCampMetrikaCounters)
    @Test
    public void deleteCampMetrikaCounters_whenTwoCampaigns() {
        DeleteCampMetrikaCountersRequest request = new DeleteCampMetrikaCountersRequest()
                .withCids(asList(textCampaignId, performanceCampaignId));

        Result<DeleteCampMetrikaCountersRequest> operationResult =
                campMetrikaCountersService.deleteAllCampMetrikaCounters(clientId, request, Applicability.PARTIAL);
        checkOperationResult(operationResult, true);

        Map<Long, List<Long>> actualCounters = campMetrikaCountersRepository.getMetrikaCountersByCids(
                shard, asList(textCampaignId, performanceCampaignId));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(actualCounters.get(textCampaignId))
                .as("из текстовой кампании удалены счетчики")
                .isEqualTo(Collections.emptyList());

        //из performance кампании нельзя удалить счетчики
        soft.assertThat(actualCounters.get(performanceCampaignId))
                .as("в performance кампании значения счетчиков не изменились")
                .isEqualTo(OLD_PERFORMANCE_CAMP_COUNTERS_IDS);
        soft.assertAll();
    }

    @Test
    // проверяем, что флаг ecommerce выставляется и для обычных счетчиков и для счетчиков организаций
    public void getAvailableAndFilterInputCountersInMetrikaForGoals_checkEcommerceNotNull() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.GOALS_FROM_ALL_ORGS_ALLOWED, true);

        long firstCounterId = RandomNumberUtils.nextPositiveInteger();
        long secondCounterId = RandomNumberUtils.nextPositiveInteger();

        long spravCounterId = RandomNumberUtils.nextPositiveInteger();

        // возвращаем обычные счетчики
        when(metrikaClient.getUsersCountersNumExtended2(any(), any())).thenReturn(
                new UserCountersExtendedResponse()
                        .withUsers(List.of(new UserCountersExtended()
                                .withCounters(List.of(
                                        new CounterInfoDirect()
                                                .withId((int) firstCounterId)
                                                .withEcommerce(false),
                                        new CounterInfoDirect()
                                                .withId((int) secondCounterId)
                                                .withEcommerce(true)
                                )))));

        // возвращаем счетчик справочника
        when(metrikaClient.getExistentCounters(any())).thenReturn(new GetExistentCountersResponse()
                .withResponseItems(List.of(
                        new GetExistentCountersResponseItem()
                                .withCounterId(spravCounterId)
                                .withCounterSource(SPRAV))
                )
        );

        Set<MetrikaCounterWithAdditionalInformation> counters =
                campMetrikaCountersService.getAvailableAndFilterInputCountersInMetrikaForGoals(clientId,
                        List.of(spravCounterId));

        List<Long> counterIds = mapList(counters, MetrikaCounterWithAdditionalInformation::getId);
        assertThat(counterIds, containsInAnyOrder(firstCounterId, secondCounterId, spravCounterId));

        List<MetrikaCounterWithAdditionalInformation> countersWithNullEcommerceFlag = filterList(counters,
                c -> c.getHasEcommerce() == null);
        assertThat(countersWithNullEcommerceFlag, empty());
    }

    @Test
    public void hasAvailableCountersReturnFalse_whenNoCounterUrl() {
        assertFalse(campMetrikaCountersService.hasAvailableCounters("https://www.velodrive.ru", clientId));
    }

    @Test
    public void hasAvailableCountersReturnTrueTest() {
        assertTrue(campMetrikaCountersService.hasAvailableCounters("https://yandex.ru", clientId));
    }

    private void checkOperationResult(Result<?> operationResult, boolean expectedHasValidationErrors) {
        boolean actualHasValidationErrors = operationResult.getValidationResult().hasAnyErrors();
        if (expectedHasValidationErrors) {
            checkState(actualHasValidationErrors, "результат операции должен содержать ошибку валидации");
        } else {
            checkState(!actualHasValidationErrors, "результат операции не должен содержать ошибок валидации");
        }
    }
}

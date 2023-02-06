package ru.yandex.direct.core.entity.campaign.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.LongStreamEx;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.campaign.container.DeleteCampMetrikaCountersRequest;
import ru.yandex.direct.core.entity.campaign.container.UpdateCampMetrikaCountersRequest;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignSimple;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampMetrikaCountersRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.metrika.repository.CalltrackingNumberClicksRepository;
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCampaignRepository;
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCounterByDomainRepository;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.metrika.client.model.response.UserCounters;
import ru.yandex.direct.metrika.client.model.response.UserCountersResponse;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.service.CampMetrikaCountersService.MAX_METRIKA_COUNTERS_COUNT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.archivedCampaignModification;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.cantAddOrDeleteMetrikaCountersToPerformanceCampaign;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.maxMetrikaCountersListSize;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.metrikaCounterIsUnavailable;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.metrikaCountersUnsupportedCampType;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.mustContainMetrikaCounters;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;


public class CampMetrikaCountersServiceValidationTest {
    private static final int SHARD = 4;
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);

    private MetrikaClient metrikaClient;
    private CampaignRepository campaignRepository;
    private CampMetrikaCountersService campMetrikaCountersService;
    private FeatureService featureService;

    static ShardHelper constantShardHelper(int shard) {
        ShardHelper shardHelper = mock(ShardHelper.class);
        when(shardHelper.getShardByClientIdStrictly(any())).thenReturn(shard);
        return shardHelper;
    }

    @Before
    public void setup() {
        ShardHelper shardHelper = constantShardHelper(SHARD);
        RbacService rbacService = mock(RbacService.class);
        BannerCommonRepository bannerCommonRepository = mock(BannerCommonRepository.class);
        CampMetrikaCountersRepository campMetrikaCountersRepository = mock(CampMetrikaCountersRepository.class);
        StrategyTypedRepository strategyTypedRepository = mock(StrategyTypedRepository.class);
        CalltrackingNumberClicksRepository calltrackingNumberClicksRepository =
                mock(CalltrackingNumberClicksRepository.class);
        MetrikaCampaignRepository metrikaCampaignRepository = mock(MetrikaCampaignRepository.class);
        featureService = mock(FeatureService.class);

        metrikaClient = mock(MetrikaClient.class);
        campaignRepository = mock(CampaignRepository.class);

        campMetrikaCountersService = new CampMetrikaCountersService(
                metrikaClient, rbacService, featureService, shardHelper,
                calltrackingNumberClicksRepository, campMetrikaCountersRepository,
                bannerCommonRepository, campaignRepository, mock(MetrikaCounterByDomainRepository.class),
                metrikaCampaignRepository, strategyTypedRepository, mock(PpcPropertiesSupport.class));
    }

    private Campaign getDefaultCampaign(Long cid) {
        return new Campaign().withId(cid).withStatusArchived(false).withType(CampaignType.TEXT)
                .withClientId(CLIENT_ID.asLong());
    }


    @Test
    public void validateDelete_whenCampaignIsNotArchivedAndIsNotPerformance() {
        Long cid = 123L;
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(
                listToMap(singletonList(getDefaultCampaign(cid)), CampaignSimple::getId)
        );
        DeleteCampMetrikaCountersRequest request = new DeleteCampMetrikaCountersRequest().withCids(singletonList(cid));

        ValidationResult<DeleteCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateDelete(SHARD, CLIENT_ID, request);
        assertThat("результат валидации не должен содержать ошибок при удалении из обычной кампании",
                validationResult.hasAnyErrors(), equalTo(false));
    }

    @Test
    public void validateDelete_whenCampaignIsPerformance() {
        Long cid = 123L;
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(listToMap(singletonList(
                getDefaultCampaign(cid).withType(CampaignType.PERFORMANCE)), CampaignSimple::getId)
        );
        DeleteCampMetrikaCountersRequest request = new DeleteCampMetrikaCountersRequest().withCids(singletonList(cid));

        ValidationResult<DeleteCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateDelete(SHARD, CLIENT_ID, request);

        checkState(!validationResult.hasErrors(), "результат валидации не должен содержать ошибок уровня операции");
        assertThat("результат валидации должен содержать ошибку при удалении из performance кампании",
                validationResult, hasDefectDefinitionWith(validationError(
                        path(field("cids"), index(0)), cantAddOrDeleteMetrikaCountersToPerformanceCampaign())));
    }

    @Test
    public void validateDelete_whenCampaignIsArchived() {
        Long cid = 123L;
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(listToMap(singletonList(
                getDefaultCampaign(cid).withStatusArchived(true)), CampaignSimple::getId)
        );
        DeleteCampMetrikaCountersRequest request = new DeleteCampMetrikaCountersRequest().withCids(singletonList(cid));

        ValidationResult<DeleteCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateDelete(SHARD, CLIENT_ID, request);

        checkState(!validationResult.hasErrors(), "результат валидации не должен содержать ошибок уровня операции");
        assertThat("результат валидации должен содержать ошибку при удалении из архивной кампании",
                validationResult, hasDefectDefinitionWith(validationError(
                        path(field("cids"), index(0)), archivedCampaignModification())));
    }


    @Test
    public void validateRemove_whenCampaignsIsNotArchivedAndIsNotPerformance() {
        Long cid = RandomNumberUtils.nextPositiveLong();
        Long cid2 = cid + 1;
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(
                listToMap(List.of(getDefaultCampaign(cid), getDefaultCampaign(cid2)), CampaignSimple::getId)
        );

        List<Long> metrikaCountersToDelete = asList(1L, 2L);
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(List.of(cid, cid2)).withMetrikaCounters(metrikaCountersToDelete);

        ValidationResult<UpdateCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateRemove(SHARD,
                        CLIENT_ID, request,
                        emptyMap(),
                        Map.of(cid, emptyList(), cid2, metrikaCountersToDelete),
                        emptyMap());
        assertThat("результат валидации не должен содержать ошибок при удалении счетчиков из обычной кампании",
                validationResult.hasAnyErrors(), equalTo(false));
    }

    @Test
    public void validateRemove_whenCampaignIsPerformance() {
        Long cid = RandomNumberUtils.nextPositiveLong();
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(listToMap(singletonList(
                getDefaultCampaign(cid).withType(CampaignType.PERFORMANCE)), CampaignSimple::getId)
        );

        List<Long> metrikaCountersToDelete = asList(1L, 2L);
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(cid)).withMetrikaCounters(metrikaCountersToDelete);

        ValidationResult<UpdateCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateRemove(SHARD, CLIENT_ID, request,
                        Map.of(cid, emptyList()),
                        Map.of(cid, List.of(metrikaCountersToDelete.get(0))),
                        Map.of());
        checkState(!validationResult.hasErrors(), "результат валидации не должен содержать ошибок уровня операции");
        assertThat("результат валидации должен содержать ошибку при удалении из performance кампании",
                validationResult, hasDefectDefinitionWith(validationError(
                        path(field("cids"), index(0)), cantAddOrDeleteMetrikaCountersToPerformanceCampaign())));
    }

    @Test
    public void validateRemove_whenCampaignIsPerformance_ButNotAffected() {
        Long cid = RandomNumberUtils.nextPositiveLong();
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(listToMap(singletonList(
                getDefaultCampaign(cid).withType(CampaignType.PERFORMANCE)), CampaignSimple::getId)
        );

        List<Long> metrikaCountersToDelete = asList(1L, 2L);
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(cid)).withMetrikaCounters(metrikaCountersToDelete);

        Map<Long, List<Long>> oldCampMetrikaCounters = Map.of(cid, List.of(3L));
        ValidationResult<UpdateCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateRemove(SHARD, CLIENT_ID, request,
                        oldCampMetrikaCounters, oldCampMetrikaCounters, Map.of());
        assertThat("результат валидации не должен содержать ошибок при удалении счетчиков из performance кампании, " +
                        "если таковых у кампании нет",
                validationResult.hasAnyErrors(), equalTo(false));
    }

    @Test
    public void validateRemove_whenCampaignIsArchived() {
        Long cid = RandomNumberUtils.nextPositiveLong();
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(listToMap(singletonList(
                getDefaultCampaign(cid).withStatusArchived(true)), CampaignSimple::getId)
        );

        List<Long> metrikaCountersToDelete = asList(1L, 2L);
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(cid)).withMetrikaCounters(metrikaCountersToDelete);

        ValidationResult<UpdateCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateRemove(SHARD, CLIENT_ID, request,
                        Collections.singletonMap(cid, emptyList()), emptyMap(), emptyMap());

        checkState(!validationResult.hasErrors(), "результат валидации не должен содержать ошибок уровня операции");
        assertThat("результат валидации должен содержать ошибку при удалении из архивной кампании",
                validationResult, hasDefectDefinitionWith(validationError(
                        path(field("cids"), index(0)), archivedCampaignModification())));
    }

    @Test
    public void validateRemove_ifSpravCounterRemoved_ValidationError() {
        Long cid = RandomNumberUtils.nextPositiveLong();
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(
                listToMap(List.of(getDefaultCampaign(cid)), CampaignSimple::getId)
        );

        Long spravMetrikaCounter = 1L;
        Long notSpravMetrikaCounter = 2L;
        List<Long> metrikaCountersToDelete = asList(spravMetrikaCounter, notSpravMetrikaCounter);
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(List.of(cid)).withMetrikaCounters(metrikaCountersToDelete);

        ValidationResult<UpdateCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateRemove(SHARD,
                        CLIENT_ID,
                        request,
                        Map.of(cid, List.of()),
                        Map.of(cid, List.of(spravMetrikaCounter, notSpravMetrikaCounter)),
                        Map.of(cid, Set.of(spravMetrikaCounter)));

        assertThat("результат валидации должен содержать ошибку, если пытаются удалить счётчик справочника",
                validationResult, hasDefectDefinitionWith(validationError(
                        path(field("cids"), index(0)),
                        mustContainMetrikaCounters(Set.of(spravMetrikaCounter)))));
    }

    @Test
    public void validateAdd_whenCampaignIsNotArchivedAndIsNotPerformance() {
        Long cid = 123L;
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(
                listToMap(singletonList(getDefaultCampaign(cid)), CampaignSimple::getId)
        );
        List<Long> metrikaCounters = asList(1L, 2L);
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(cid)).withMetrikaCounters(metrikaCounters);

        ValidationResult<UpdateCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateAdd(SHARD,
                        CLIENT_ID, request, Collections.singletonMap(cid, metrikaCounters));
        assertThat("результат валидации не должен содержать ошибок при добавлении в обычную кампанию",
                validationResult.hasAnyErrors(), equalTo(false));
    }

    @Test
    public void validateAdd_whenCampaignIsPerformance() {
        Long cid = 123L;
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(listToMap(singletonList(
                getDefaultCampaign(cid).withType(CampaignType.PERFORMANCE)), CampaignSimple::getId)
        );
        List<Long> metrikaCounters = asList(1L, 2L);
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(cid)).withMetrikaCounters(metrikaCounters);

        ValidationResult<UpdateCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateAdd(SHARD,
                        CLIENT_ID, request, Collections.singletonMap(cid, metrikaCounters));
        checkState(!validationResult.hasErrors(), "результат валидации не должен содержать ошибок уровня операции");
        assertThat("результат валидации должен содержать ошибку при добавлении в performance кампанию",
                validationResult, hasDefectDefinitionWith(validationError(path(field("cids"), index(0)),
                        cantAddOrDeleteMetrikaCountersToPerformanceCampaign())));
    }

    @Test
    public void validateAdd_whenCampaignIsMobile() {
        Long cid = 123L;
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(listToMap(singletonList(
                getDefaultCampaign(cid).withType(CampaignType.MOBILE_CONTENT)), CampaignSimple::getId)
        );
        List<Long> metrikaCounters = asList(1L, 2L);
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(cid)).withMetrikaCounters(metrikaCounters);

        ValidationResult<UpdateCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateAdd(SHARD,
                        CLIENT_ID, request, Collections.singletonMap(cid, metrikaCounters));
        checkState(!validationResult.hasErrors(), "результат валидации не должен содержать ошибок уровня операции");
        assertThat("результат валидации должен содержать ошибку при добавлении в РМП кампанию",
                validationResult, hasDefectDefinitionWith(validationError(path(field("cids"), index(0)),
                        metrikaCountersUnsupportedCampType())));
    }

    @Test
    public void validateAdd_whenCampaignIsMCB() {
        Long cid = 123L;
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(listToMap(singletonList(
                getDefaultCampaign(cid).withType(CampaignType.MCB)), CampaignSimple::getId)
        );
        List<Long> metrikaCounters = asList(1L, 2L);
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(cid)).withMetrikaCounters(metrikaCounters);

        ValidationResult<UpdateCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateAdd(SHARD,
                        CLIENT_ID, request, Collections.singletonMap(cid, metrikaCounters));
        checkState(!validationResult.hasErrors(), "результат валидации не должен содержать ошибок уровня операции");
        assertThat("результат валидации должен содержать ошибку при добавлении в МКБ кампанию",
                validationResult, hasDefectDefinitionWith(validationError(path(field("cids"), index(0)),
                        metrikaCountersUnsupportedCampType())));
    }

    @Test
    public void validateAdd_whenNotTooManyCounters() {
        Long cid = 123L;
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(
                listToMap(singletonList(getDefaultCampaign(cid)), CampaignSimple::getId)
        );

        List<Long> metrikaCounters = LongStreamEx.rangeClosed(1L, 100L).boxed().toList();
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(cid)).withMetrikaCounters(metrikaCounters);

        ValidationResult<UpdateCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateAdd(SHARD,
                        CLIENT_ID, request, Collections.singletonMap(cid, metrikaCounters));
        checkState(!validationResult.hasErrors(), "результат валидации не должен содержать ошибок уровня операции");
        assertThat("результат валидации должен содержать ошибку, если количество счетчиков больше максимального",
                validationResult.hasAnyErrors(), equalTo(false));
    }

    @Test
    public void validateAdd_whenTooManyCounters() {
        Long cid = 123L;
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(
                listToMap(singletonList(getDefaultCampaign(cid)), CampaignSimple::getId)
        );

        List<Long> metrikaCounters = LongStreamEx.rangeClosed(1L, 101L).boxed().toList();
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(cid)).withMetrikaCounters(metrikaCounters);

        ValidationResult<UpdateCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateAdd(SHARD,
                        CLIENT_ID, request, Collections.singletonMap(cid, metrikaCounters));
        checkState(!validationResult.hasErrors(), "результат валидации не должен содержать ошибок уровня операции");
        assertThat("результат валидации должен содержать ошибку, если количество счетчиков больше максимального",
                validationResult, hasDefectDefinitionWith(validationError(path(field("cids"), index(0)),
                        maxMetrikaCountersListSize(MAX_METRIKA_COUNTERS_COUNT))));
    }

    @Test
    public void validateReplace_whenCampaignIsPerformanceAndCounterIsAvailable() {
        Long cid = 123L;
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(listToMap(singletonList(
                getDefaultCampaign(cid).withType(CampaignType.PERFORMANCE)), CampaignSimple::getId)
        );
        Integer metrikaCounter = 5;
        List<Long> metrikaCounters = singletonList(metrikaCounter.longValue());
        when(metrikaClient.getUsersCountersNum2(any(), any())).thenReturn(
                new UserCountersResponse()
                        .withUsers(singletonList(new UserCounters().withCounterIds(singletonList(metrikaCounter)))));

        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(cid)).withMetrikaCounters(metrikaCounters);

        ValidationResult<UpdateCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateReplace(SHARD,
                        CLIENT_ID,
                        request,
                        Set.of(cid),
                        Map.of(),
                        Collections.singletonMap(cid, metrikaCounters));

        assertThat("результат валидации не должен содержать ошибок при замене счетчика в performance кампании",
                validationResult.hasAnyErrors(), equalTo(false));
    }

    @Test
    public void validateReplace_ifSpravCounterRemoved_ValidationError() {
        Long cid = 123L;
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(listToMap(singletonList(
                getDefaultCampaign(cid)), CampaignSimple::getId)
        );
        Integer metrikaCounter = 5;
        List<Long> newMetrikaCounters = singletonList(metrikaCounter.longValue());
        when(metrikaClient.getUsersCountersNum2(any(), any())).thenReturn(
                new UserCountersResponse()
                        .withUsers(singletonList(new UserCounters().withCounterIds(singletonList(metrikaCounter)))));

        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(cid)).withMetrikaCounters(newMetrikaCounters);

        Long oldSpravMetrikaCounter = 4L;
        ValidationResult<UpdateCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateReplace(SHARD,
                        CLIENT_ID,
                        request,
                        Set.of(),
                        Map.of(cid, Set.of(oldSpravMetrikaCounter)),
                        Collections.singletonMap(cid, newMetrikaCounters));

        assertThat("результат валидации должен содержать ошибку, если пытаются удалить счётчик справочника",
                validationResult, hasDefectDefinitionWith(validationError(
                        path(field("cids"), index(0)),
                        mustContainMetrikaCounters(Set.of(oldSpravMetrikaCounter)))));
    }

    @Test
    public void validateReplace_whenCampaignIsPerformanceAndCounterIsUnavailable() {
        Long cid = 123L;
        when(campaignRepository.getCampaignsSimple(eq(SHARD), any())).thenReturn(listToMap(singletonList(
                getDefaultCampaign(cid).withType(CampaignType.PERFORMANCE)), CampaignSimple::getId)
        );
        List<Long> metrikaCounters = singletonList(5L);
        when(metrikaClient.getUsersCountersNum2(any(), any()))
                .thenReturn(new UserCountersResponse().withUsers(Collections.emptyList()));

        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(cid)).withMetrikaCounters(metrikaCounters);

        ValidationResult<UpdateCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateReplace(SHARD,
                        CLIENT_ID,
                        request,
                        Set.of(cid),
                        Map.of(),
                        Collections.singletonMap(cid, metrikaCounters));

        checkState(!validationResult.hasErrors(), "результат валидации не должен содержать ошибок уровня операции");
        assertThat("результат валидации должен содержать ошибку, если счетчик не принадлежит пользователю",
                validationResult, hasDefectDefinitionWith(validationError(
                        path(field("cids"), index(0)), metrikaCounterIsUnavailable())));
    }

    @Test
    public void validateUpdateCountersRequest_whenTooManyCounters() {
        Long cid = RandomNumberUtils.nextPositiveLong();

        List<Long> metrikaCountersToDelete =
                LongStreamEx.rangeClosed(1L, MAX_METRIKA_COUNTERS_COUNT + 1).boxed().toList();
        UpdateCampMetrikaCountersRequest request = new UpdateCampMetrikaCountersRequest()
                .withCids(singletonList(cid)).withMetrikaCounters(metrikaCountersToDelete);

        ValidationResult<UpdateCampMetrikaCountersRequest, Defect> validationResult =
                campMetrikaCountersService.validateUpdateCountersRequest(CLIENT_ID, request, false);
        assertThat("результат валидации не должен содержать ошибку," +
                        " если количество счетчиков больше максимального, но флаг на проверку отключен",
                validationResult.hasAnyErrors(), equalTo(false));
    }
}

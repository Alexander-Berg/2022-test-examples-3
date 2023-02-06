package ru.yandex.direct.intapi.entity.showconditions.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.common.log.service.LogPriceService;
import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesRepository;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.bids.service.KeywordBidDynamicDataService;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.keyword.service.KeywordModifyOperationFactory;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.entity.mailnotification.service.MailNotificationEventService;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchService;
import ru.yandex.direct.core.entity.relevancematch.valdiation.RelevanceMatchValidationService;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.adqualityexport.AdQualityExportLogger;
import ru.yandex.direct.intapi.entity.showconditions.model.request.RelevanceMatchAddItem;
import ru.yandex.direct.intapi.entity.showconditions.model.request.RelevanceMatchItem;
import ru.yandex.direct.intapi.entity.showconditions.model.request.RelevanceMatchModificationContainer;
import ru.yandex.direct.intapi.entity.showconditions.model.request.ShowConditionsRequest;
import ru.yandex.direct.intapi.entity.showconditions.model.response.ShowConditionsResponse;
import ru.yandex.direct.intapi.entity.showconditions.service.BannerStatusService;
import ru.yandex.direct.intapi.entity.showconditions.service.ShowConditionsService;
import ru.yandex.direct.intapi.entity.showconditions.service.validation.ShowConditionsValidationResultConverter;
import ru.yandex.direct.intapi.entity.showconditions.service.validation.ShowConditionsValidationService;
import ru.yandex.direct.intapi.validation.kernel.ValidationResultConversionService;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.ytcore.entity.statistics.service.RecentStatisticsService;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ShowConditionsControllerModifyRelevanceMatchTest {
    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private RelevanceMatchValidationService relevanceMatchValidationService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private LogPriceService logPriceService;

    @Autowired
    private MailNotificationEventService mailNotificationEventService;

    @Autowired
    private ValidationResultConversionService validationResultConversionService;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private KeywordService keywordService;

    @Autowired
    private RetargetingService retargetingService;

    @Autowired
    private RetargetingService targetInterestsService;

    @Autowired
    private BannerStatusService bannerStatusService;

    @Autowired
    private KeywordModifyOperationFactory keywordModifyOperationFactory;

    @Autowired
    private AdGroupService adGroupService;

    @Autowired
    private BannerRelationsRepository bannerRelationsRepository;

    @Autowired
    private ShowConditionsValidationService validationService;

    @Autowired
    private KeywordBidDynamicDataService keywordBidDynamicDataService;

    @Autowired
    private RecentStatisticsService recentStatisticsService;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Autowired
    private ShowConditionsValidationResultConverter showConditionsValidationResultConverter;

    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private AdQualityExportLogger adQualityExportLogger;

    private RelevanceMatchService relevanceMatchService;
    private RelevanceMatchRepository relevanceMatchRepository;
    private ShowConditionsService showConditionsService;
    private ShowConditionsController showConditionsController;

    private int shard;
    private long uid;
    private long clientId;
    private long adGroupId;

    private Long relevanceMatchId1;

    private Long otherAdGroupId;
    private Long relevanceMatchId2;

    private Long emptyAdGroupId;

    private Boolean isInitialized = false;

    private AdGroupInfo adGroupInfo;

    public boolean init() {
        relevanceMatchRepository =
                new RelevanceMatchRepository(dslContextProvider, shardHelper);
        steps.relevanceMatchSteps().setRelevanceMatchRepository(relevanceMatchRepository);

        relevanceMatchService = new RelevanceMatchService(
                shardHelper, relevanceMatchValidationService, relevanceMatchRepository,
                campaignRepository, adGroupRepository, aggregatedStatusesRepository, clientService, logPriceService,
                mailNotificationEventService, dslContextProvider, keywordService, recentStatisticsService);

        showConditionsService = new ShowConditionsService(
                validationResultConversionService, rbacService, relevanceMatchService,
                retargetingService, recentStatisticsService, clientService, keywordService,
                keywordModifyOperationFactory, keywordBidDynamicDataService, adGroupService,
                bannerRelationsRepository, targetInterestsService,
                bannerStatusService, validationService, showConditionsValidationResultConverter,
                adQualityExportLogger, bidModifierService);

        showConditionsController = new ShowConditionsController(showConditionsService);

        return true;
    }

    @Before
    public void before() {
        if (!isInitialized) {
            isInitialized = init();
        }

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        shard = clientInfo.getShard();
        uid = clientInfo.getUid();
        clientId = clientInfo.getClientId().asLong();
        adGroupId = adGroupInfo.getAdGroupId();

        relevanceMatchId1 = steps.relevanceMatchSteps().addDefaultRelevanceMatchToAdGroup(adGroupInfo);

        AdGroupInfo otherAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(adGroupInfo.getCampaignInfo());
        otherAdGroupId = otherAdGroupInfo.getAdGroupId();
        relevanceMatchId2 = steps.relevanceMatchSteps().addDefaultRelevanceMatchToAdGroup(otherAdGroupInfo);

        AdGroupInfo emptyAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(adGroupInfo.getCampaignInfo());
        emptyAdGroupId = emptyAdGroupInfo.getAdGroupId();
    }

    @Test
    public void update_EmptyRequest_SuccessResponse() {
        ShowConditionsRequest request = new ShowConditionsRequest();

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertTrue(response.isSuccessful());
        assertThat(response.getItems().entrySet(), Matchers.empty());
    }

    @Test
    public void update_AddNewRelevanceMatch_SuccessAdd() {
        BigDecimal price = BigDecimal.valueOf(66.6);
        ShowConditionsRequest request = buildAddWithPriceRequest(emptyAdGroupId, price, price);

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);
        assertThat(response.isSuccessful(), is(true));

        Map<Long, RelevanceMatch> relevanceMatches = relevanceMatchRepository
                .getRelevanceMatchesByAdGroupIds(shard, ClientId.fromLong(clientId), singletonList(emptyAdGroupId));
        assertThat(relevanceMatches.values(), hasSize(1));
        RelevanceMatch relevanceMatch = relevanceMatches.values().iterator().next();

        assertThat(relevanceMatch.getPriceContext(), comparesEqualTo(price));
        assertThat(relevanceMatch.getPrice(), comparesEqualTo(price));
    }

    @Test
    public void update_AddNewExtendedRelevanceMatch_SuccessAdd() {
        BigDecimal price = BigDecimal.valueOf(66.66);
        BigDecimal priceContext = BigDecimal.valueOf(44.44);

        ShowConditionsRequest request =
                buildAddExtendedRelevanceMathRequest(emptyAdGroupId, price, priceContext);

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);
        assertThat(response.isSuccessful(), is(true));

        Map<Long, RelevanceMatch> relevanceMatches = relevanceMatchRepository
                .getRelevanceMatchesByAdGroupIds(shard, ClientId.fromLong(clientId), singletonList(emptyAdGroupId));
        assertThat(relevanceMatches.values(), hasSize(1));
        RelevanceMatch relevanceMatch = relevanceMatches.values().iterator().next();
        RelevanceMatch expectedRelevanceMatch = new RelevanceMatch()
                .withPrice(price)
                .withPriceContext(priceContext);
        assertThat(relevanceMatch, beanDiffer(expectedRelevanceMatch)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    public void update_AddRelevanceMatchToAdGroupWithExistRelevanceMatch_RelevanceMatchNotChanged() {
        @SuppressWarnings("ConstantConditions")
        BigDecimal oldPrice = getRelevanceMatch(relevanceMatchId1).getPrice();
        BigDecimal oldPriceContext = getRelevanceMatch(relevanceMatchId1).getPriceContext();

        BigDecimal price = BigDecimal.valueOf(77.7);
        BigDecimal priceContext = BigDecimal.valueOf(88.8);
        ShowConditionsRequest request = buildAddWithPriceRequest(adGroupId, price, priceContext);

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);
        assertThat(response.isSuccessful(), is(true));

        Map<Long, RelevanceMatch> relevanceMatchMap =
                relevanceMatchRepository
                        .getRelevanceMatchesByAdGroupIds(shard, ClientId.fromLong(clientId), singletonList(adGroupId));
        assumeThat("только 1 бесфразный таргетинг может быть у группы", relevanceMatchMap.size(), is(1));

        RelevanceMatch relevanceMatch = relevanceMatchMap.get(adGroupId);
        assertThat(relevanceMatch.getPriceContext(), comparesEqualTo(oldPriceContext));
        assertThat(relevanceMatch.getPrice(), comparesEqualTo(oldPrice));
    }

    @Test
    public void update_EditPriceRelevanceMatch_SuccessEdit() {
        BigDecimal price = BigDecimal.valueOf(66.6);
        BigDecimal priceContext = BigDecimal.valueOf(77.7);
        ShowConditionsRequest request = buildEditPriceRequest(adGroupId, relevanceMatchId1, price, priceContext);

        ShowConditionsResponse response =
                (ShowConditionsResponse) showConditionsController.update(request, uid, clientId);
        assertThat(response.isSuccessful(), is(true));

        RelevanceMatch relevanceMatch = getRelevanceMatch(relevanceMatchId1);
        assert relevanceMatch != null;
        assertThat(relevanceMatch.getPriceContext(), comparesEqualTo(priceContext));
        assertThat(relevanceMatch.getPrice(), comparesEqualTo(price));
    }

    @Test
    public void update_EditExtendedRelevanceMatch_SuccessEdit() {
        BigDecimal price = BigDecimal.valueOf(66.66);
        BigDecimal priceContext = BigDecimal.valueOf(44.44);
        ShowConditionsRequest request = buildEditRequest(adGroupId, relevanceMatchId1,
                new RelevanceMatchItem()
                        .withPrice(price)
                        .withPriceContext(priceContext)
        );

        RelevanceMatch expectedRelevanceMatch = new RelevanceMatch()
                .withPrice(price)
                .withPriceContext(priceContext);

        ShowConditionsResponse response =
                (ShowConditionsResponse) showConditionsController.update(request, uid, clientId);
        assertThat(response.isSuccessful(), is(true));

        RelevanceMatch relevanceMatch = getRelevanceMatch(relevanceMatchId1);
        assert relevanceMatch != null;
        assertThat(relevanceMatch, beanDiffer(expectedRelevanceMatch)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    public void update_EditPriceOfDeletedRelevanceMatch_RequestFailed() {
        deleteRelevanceMatch(relevanceMatchId1);

        BigDecimal price = BigDecimal.valueOf(66.6);
        ShowConditionsRequest request = buildEditPriceRequest(adGroupId, relevanceMatchId1, price);

        ShowConditionsResponse response =
                (ShowConditionsResponse) showConditionsController.update(request, uid, clientId);
        assertResponseHasErrors(response);
    }

    @Test
    public void update_EditAutobudgetAndSuspendWithoutPrice_SuccessEdit() {
        BigDecimal price = BigDecimal.valueOf(55.5);
        Integer autobudgetPriority = 1;

        ShowConditionsRequest request = buildEditRequest(adGroupId, relevanceMatchId1,
                new RelevanceMatchItem()
                        .withAutobudgetPriority(autobudgetPriority)
                        .withIsSuspended(1));

        ShowConditionsResponse response =
                (ShowConditionsResponse) showConditionsController.update(request, uid, clientId);
        assertThat(response.isSuccessful(), is(true));

        RelevanceMatch relevanceMatch = getRelevanceMatch(relevanceMatchId1);
        assert relevanceMatch != null;

        RelevanceMatch expected = new RelevanceMatch()
                .withId(relevanceMatchId1)
                .withPrice(price)
                .withAutobudgetPriority(autobudgetPriority)
                .withIsSuspended(true);

        CompareStrategy fields = onlyFields(newPath("autobudgetPriority"), newPath("is_suspended"));
        assertThat(relevanceMatch, beanDiffer(expected).useCompareStrategy(fields));
    }

    @Test
    public void update_SuspendRelevanceMatch_SuccessEdit() {
        ShowConditionsRequest request = buildSuspendRequest(adGroupId, relevanceMatchId1, true);

        ShowConditionsResponse response =
                (ShowConditionsResponse) showConditionsController.update(request, uid, clientId);
        assertThat(response.isSuccessful(), is(true));

        RelevanceMatch relevanceMatch = getRelevanceMatch(relevanceMatchId1);
        assert relevanceMatch != null;
        assertThat(relevanceMatch.getIsSuspended(), is(true));
    }

    @Test
    public void update_AddDeletedRelevanceMatch_SuccessAdd() {
        deleteRelevanceMatch(relevanceMatchId1);

        BigDecimal price = BigDecimal.valueOf(66.6);
        BigDecimal priceContext = BigDecimal.valueOf(77.7);
        ShowConditionsRequest request = buildAddWithPriceRequest(adGroupId, price, priceContext);

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);
        assertThat(response.isSuccessful(), is(true));

        RelevanceMatch savedRelevanceMatches = getRelevanceMatch(relevanceMatchId1);
        assert savedRelevanceMatches != null;
        assertThat(savedRelevanceMatches.getPriceContext(), comparesEqualTo(priceContext));
        assertThat(savedRelevanceMatches.getPrice(), comparesEqualTo(price));
    }

    @Test
    public void update_DeleteRelevanceMatch_SuccessDelete() {
        ShowConditionsRequest request = buildDeleteRequest(relevanceMatchId1);
        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertThat(response.isSuccessful(), is(true));
        assertThat(getRelevanceMatch(relevanceMatchId1), nullValue());
    }

    @Test
    public void update_DeleteNonExistRelevanceMatch_RequestFailed() {
        ShowConditionsRequest request = buildDeleteRequest(666L);
        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertResponseHasErrors(response);
    }

    @Test
    public void update_deleteAndUpdateSameRelevanceMatch_RequestFailed() {
        ShowConditionsRequest request = buildEditPriceRequest(adGroupId, relevanceMatchId1, BigDecimal.ONE);
        request.getRelevanceMatches().get(adGroupId).getDeleted().add(relevanceMatchId1);

        ShowConditionsResponse response =
                (ShowConditionsResponse) showConditionsController.update(request, uid, clientId);

        assertResponseHasErrors(response);
    }

    @Test
    public void update_TwoAdGroupsModifyOperation_SuccessUpdate() {
        BigDecimal price1 = BigDecimal.valueOf(33.3);

        ShowConditionsRequest request = buildEditPriceRequest(adGroupId, relevanceMatchId1, price1);
        request.getRelevanceMatches().put(otherAdGroupId,
                new RelevanceMatchModificationContainer().withDeleted(singletonList(relevanceMatchId2)));

        ShowConditionsResponse response =
                (ShowConditionsResponse) showConditionsController.update(request, uid, clientId);
        assertThat(response.isSuccessful(), is(true));

        assertThat(getRelevanceMatch(relevanceMatchId2), nullValue());

        RelevanceMatch relevanceMatch1 = getRelevanceMatch(relevanceMatchId1);
        assert relevanceMatch1 != null;
        assertThat(relevanceMatch1.getPrice(), comparesEqualTo(price1));
    }

    @Test
    public void update_AddRelevanceMatchInArchiveCampaign_RequestFailed() {
        ShowConditionsRequest request = buildAddWithPriceRequest(adGroupId, BigDecimal.TEN);

        archiveCampaign(adGroupInfo.getCampaignId(), shard);

        ShowConditionsResponse response =
                (ShowConditionsResponse) showConditionsController.update(request, uid, clientId);
        assertResponseHasErrors(response);
    }

    @Test
    public void update_EditInArchivedCampaign_RequestFailed() {
        ShowConditionsRequest request = buildEditPriceRequest(adGroupId, relevanceMatchId1, BigDecimal.TEN);

        archiveCampaign(adGroupInfo.getCampaignId(), shard);

        ShowConditionsResponse response =
                (ShowConditionsResponse) showConditionsController.update(request, uid, clientId);

        assertResponseHasErrors(response);
    }

    @Test
    public void update_DeleteInArchivedCampaign_RequestFailed() {
        ShowConditionsRequest request = buildDeleteRequest(relevanceMatchId1);

        archiveCampaign(adGroupInfo.getCampaignId(), shard);

        ShowConditionsResponse response =
                (ShowConditionsResponse) showConditionsController.update(request, uid, clientId);

        assertResponseHasErrors(response);
    }

    private void deleteRelevanceMatch(Long id) {
        List<Long> ids = singletonList(id);
        relevanceMatchService.createFullDeleteOperation(ClientId.fromLong(clientId), uid, ids,
                relevanceMatchRepository.getRelevanceMatchesByIds(shard, ClientId.fromLong(clientId), ids))
                .prepareAndApply();

        assumeThat(getRelevanceMatch(relevanceMatchId1), nullValue());
    }

    private RelevanceMatch getRelevanceMatch(Long id) {
        List<RelevanceMatch> list =
                EntryStream.of(relevanceMatchRepository
                        .getRelevanceMatchesByIds(shard, ClientId.fromLong(clientId), singletonList(id)))
                        .values()
                        .toList();

        return list.isEmpty() ? null : list.get(0);
    }

    private ShowConditionsRequest buildAddWithPriceRequest(Long adGroupId, BigDecimal price) {
        return buildAddWithPriceRequest(adGroupId, price, null);
    }

    private ShowConditionsRequest buildAddWithPriceRequest(Long adGroupId, BigDecimal price, BigDecimal priceContext) {
        Map<Long, RelevanceMatchModificationContainer> relevanceMatches = EntryStream.of(
                adGroupId,
                new RelevanceMatchModificationContainer()
                        .withAdded(singletonList(
                                new RelevanceMatchAddItem().withPrice(price).withPriceContext(priceContext))))
                .toMap();

        return new ShowConditionsRequest().withRelevanceMatches(relevanceMatches);
    }

    private ShowConditionsRequest buildAddExtendedRelevanceMathRequest(
            Long adGroupId,
            BigDecimal price,
            BigDecimal priceContext
    ) {
        Map<Long, RelevanceMatchModificationContainer> relevanceMatches = EntryStream.of(
                adGroupId,
                new RelevanceMatchModificationContainer()
                        .withAdded(singletonList(
                                new RelevanceMatchAddItem()
                                        .withPrice(price)
                                        .withPriceContext(priceContext)
                        )))
                .toMap();

        return new ShowConditionsRequest().withRelevanceMatches(relevanceMatches);
    }

    private ShowConditionsRequest buildEditPriceRequest(Long adGroupId, Long id, BigDecimal price,
                                                        BigDecimal priceContext) {
        return buildEditRequest(adGroupId, id,
                new RelevanceMatchItem().withPrice(price).withPriceContext(priceContext));
    }

    private ShowConditionsRequest buildEditPriceRequest(Long adGroupId, Long id, BigDecimal price) {
        return buildEditRequest(adGroupId, id, new RelevanceMatchItem().withPrice(price));
    }

    private ShowConditionsRequest buildSuspendRequest(Long adGroupId, Long id, boolean suspend) {
        return buildEditRequest(adGroupId, id, new RelevanceMatchItem()
                .withIsSuspended(suspend ? 1 : 0));
    }

    private ShowConditionsRequest buildEditRequest(Long adGroupId, Long id, RelevanceMatchItem item) {
        Map<Long, RelevanceMatchModificationContainer> relevanceMatches = EntryStream.of(
                adGroupId,
                new RelevanceMatchModificationContainer()
                        .withEdited(singletonMap(id, item)))
                .toMap();

        return new ShowConditionsRequest().withRelevanceMatches(relevanceMatches);
    }

    private ShowConditionsRequest buildDeleteRequest(Long relevanceMatchId) {
        Map<Long, RelevanceMatchModificationContainer> relevanceMatches = EntryStream.of(
                adGroupId,
                new RelevanceMatchModificationContainer()
                        .withDeleted(singletonList(relevanceMatchId)))
                .toMap();

        return new ShowConditionsRequest().withRelevanceMatches(relevanceMatches);
    }

    private void assertResponseHasErrors(ShowConditionsResponse response) {
        boolean hasErrors = !response.getErrors().isEmpty()
                || response.getItems().values().stream().anyMatch(i -> !i.getErrors().isEmpty());

        assertThat(hasErrors, is(true));
    }

    private void archiveCampaign(long campaignId, int shard) {
        testCampaignRepository.archiveCampaign(shard, campaignId);
    }
}


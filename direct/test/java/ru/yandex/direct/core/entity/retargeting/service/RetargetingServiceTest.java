package ru.yandex.direct.core.entity.retargeting.service;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.log.service.LogPriceService;
import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesRepository;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.currency.service.CpmYndxFrontpageCurrencyService;
import ru.yandex.direct.core.entity.mailnotification.service.MailNotificationEventService;
import ru.yandex.direct.core.entity.markupcondition.repository.MarkupConditionRepository;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageGeoTree;
import ru.yandex.direct.core.entity.retargeting.container.RetargetingSelection;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.InterestLink;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.entity.retargeting.repository.TargetingCategoriesCache;
import ru.yandex.direct.core.entity.retargeting.service.validation2.AddRetargetingValidationService;
import ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingSetBidsValidationService;
import ru.yandex.direct.core.entity.retargeting.service.validation2.UpdateRetargetingValidationService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacService;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.multitype.entity.LimitOffset.limited;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class RetargetingServiceTest {
    private static final ClientId clientId = ClientId.fromLong(1L);
    private static final Long operatorUid = 3L;
    private static final int shard = 1;
    private static final Integer limit = 1000;
    private static final Long campaignId = 10L;

    private static final Long interest1 = 1L;
    private static final Long interestLink1 = 1001L;
    private static final Long interestLinkGoalId1 = 100001L;
    private static final Long retargeting1 = 1001L;

    private static final Long interest2 = 2L;
    private static final Long interestLink2 = 1002L;
    private static final Long interestLinkGoalId2 = 100002L;

    private static final Long interest3 = 3L;
    private static final Long interestLink3 = 1003L;
    private static final Long interestLinkGoalId3 = 100003L;

    private static final Long retCond5 = 5L;

    @Autowired
    private DeleteRetargetingValidationService deleteRetargetingValidationService;

    private RetargetingRepository retargetingRepository;

    private RetargetingService service;

    private List<InterestLink> existingInterests;

    @Autowired
    private RetargetingSetBidsValidationService retargetingSetBidsValidationService;

    @Autowired
    private LogPriceService logPriceService;

    @Autowired
    private AddTargetInterestService targetInterestService;

    @Autowired
    private MailNotificationEventService mailNotificationEventService;

    @Autowired
    private RetargetingDeleteService retargetingDeleteService;

    @Autowired
    private MarkupConditionRepository markupConditionRepository;

    @Autowired
    private PricePackageGeoTree pricePackageGeoTree;

    @Autowired
    private ClientGeoService clientGeoService;

    private InterestLink interest(InterestLinkFactory interestLinkFactory, Long retCondId, Long interestId) {
        Goal goal = new Goal();
        goal.withId(interestId);
        Rule rule = new Rule();
        rule.withGoals(asList(goal));
        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition.withId(retCondId)
                .withInterest(true)
                .withRules(asList(rule));
        return interestLinkFactory.from(retargetingCondition);
    }

    private static RetargetingSelection selection(Long... interestIds) {
        return new RetargetingSelection()
                .withInterestIds(asList(interestIds));
    }

    private static Retargeting retargetingOfRetCond(Long retargetingConditionId) {
        return new Retargeting().withRetargetingConditionId(retargetingConditionId);
    }

    @Before
    public void setup() {
        TargetingCategoriesCache targetingCategoriesCache = mock(TargetingCategoriesCache.class);
        when(targetingCategoriesCache.getCategoryByImportId(BigInteger.valueOf(interestLinkGoalId1)))
                .thenReturn(interest1);
        when(targetingCategoriesCache.getCategoryByImportId(BigInteger.valueOf(interestLinkGoalId2)))
                .thenReturn(interest2);
        when(targetingCategoriesCache.getCategoryByImportId(BigInteger.valueOf(interestLinkGoalId3)))
                .thenReturn(interest3);
        InterestLinkFactory interestLinkFactory = new InterestLinkFactory(targetingCategoriesCache);

        existingInterests = StreamEx.of(interestLink1, interestLink2, interestLink3)
                .zipWith(Stream.of(interestLinkGoalId1, interestLinkGoalId2, interestLinkGoalId3))
                .mapKeyValue((retCondId, interestId) -> interest(interestLinkFactory, retCondId, interestId))
                .toList();

        ShardHelper shardHelper = mock(ShardHelper.class);
        when(shardHelper.getShardByClientIdStrictly(clientId)).thenReturn(shard);

        retargetingRepository = mock(RetargetingRepository.class);

        RetargetingConditionRepository retargetingConditionRepository = mock(RetargetingConditionRepository.class);
        when(retargetingConditionRepository.getExistingInterest(shard, clientId))
                .thenReturn(existingInterests);

        AdGroupRepository adGroupRepository = mock(AdGroupRepository.class);
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        CampaignTypedRepository campaignTypedRepository = mock(CampaignTypedRepository.class);
        PricePackageRepository pricePackageRepository = mock(PricePackageRepository.class);
        AggregatedStatusesRepository aggregatedStatusesRepository = mock(AggregatedStatusesRepository.class);
        AddRetargetingValidationService addValidation = mock(AddRetargetingValidationService.class);
        ClientService clientService = mock(ClientService.class);

        RbacService rbacService = mock(RbacService.class);
        when(rbacService.getVisibleCampaigns(anyLong(), any()))
                .thenReturn(listToSet(asList(campaignId), id -> id));

        UpdateRetargetingValidationService updateRetargetingValidationService =
                mock(UpdateRetargetingValidationService.class);

        //noinspection ConstantConditions
        service = new RetargetingService(shardHelper,
                retargetingRepository,
                retargetingConditionRepository,
                adGroupRepository,
                campaignRepository,
                campaignTypedRepository,
                pricePackageRepository,
                pricePackageGeoTree,
                markupConditionRepository,
                aggregatedStatusesRepository,
                addValidation,
                deleteRetargetingValidationService,
                clientService,
                clientGeoService,
                rbacService,
                logPriceService,
                mailNotificationEventService, null,
                updateRetargetingValidationService,
                retargetingSetBidsValidationService,
                mock(AdGroupService.class),
                mock(CpmYndxFrontpageCurrencyService.class),
                targetInterestService, retargetingDeleteService);
    }

    @Test
    public void get_queryByInterests_retargetingsAreFilteredByInterests() {
        RetargetingSelection selection = selection(interest1, interest2, interest3);

        service.getRetargetings(selection, clientId, operatorUid, limited(limit));

        verify(retargetingRepository, times(1))
                .getRetIdWithCidWithoutLimit(
                        anyInt(), eq(selection), eq(existingInterests));
    }

    @Test
    public void get_queryByInterests_invokeRetargetingRepository() {
        RetargetingSelection selection = selection(interest1, interest3);

        service.getRetargetings(selection, clientId, operatorUid, limited(limit));

        verify(retargetingRepository, times(1))
                .getRetIdWithCidWithoutLimit(anyInt(), eq(selection), eq(existingInterests));
    }

    @Test
    public void get_queryByInterests_correctInterestIsReturned() {
        RetargetingSelection selection = selection(interest1);
        when(retargetingRepository.getRetIdWithCidWithoutLimit(anyInt(), eq(selection), eq(existingInterests)))
                .thenReturn(ImmutableMap.of(retargeting1, campaignId));
        when(retargetingRepository.getRetargetingsByIds(anyInt(), eq(asList(retargeting1)), any()))
                .thenReturn(asList(retargetingOfRetCond(interestLink1)));

        List<TargetInterest> result = service.getRetargetings(selection, clientId, operatorUid, limited(limit));

        assertThat(result, hasItem(hasProperty("interestId", equalTo(interest1))));
    }

    @Test
    public void get_queryByInterests_retargetingConditionIsNotConvertedToInterest() {
        RetargetingSelection selection = selection(interest1);
        when(retargetingRepository.getRetargetingsByIds(anyInt(), any(), any()))
                .thenReturn(asList(retargetingOfRetCond(interestLink1), retargetingOfRetCond(retCond5)));

        List<TargetInterest> result = service.getRetargetings(selection, clientId, operatorUid, limited(limit));

        assertThat(result, hasItem(hasProperty("retargetingConditionId", equalTo(retCond5))));
    }

    @Test
    public void get_queryInterests_interestsAreRequestedWhenQueriedByOtherParameters() {
        RetargetingSelection selection = new RetargetingSelection()
                .withCampaignIds(asList(campaignId));
        when(retargetingRepository.getRetIdWithCidWithoutLimit(shard, selection, existingInterests))
                .thenReturn(ImmutableMap.of(retargeting1, campaignId));
        when(retargetingRepository.getRetargetingsByIds(anyInt(), eq(asList(retargeting1)), any()))
                .thenReturn(asList(retargetingOfRetCond(interest1)));

        service.getRetargetings(selection, clientId, operatorUid, limited(limit));

        verify(retargetingRepository).getRetargetingsByIds(anyInt(), eq(asList(retargeting1)), any());
    }

    @Test
    public void get_retargetingIdsAreQueriedBySelection() {
        RetargetingSelection selection = new RetargetingSelection()
                .withCampaignIds(asList(campaignId));

        service.getRetargetings(selection, clientId, operatorUid, limited(limit));

        verify(retargetingRepository).getRetIdWithCidWithoutLimit(anyInt(), eq(selection), anyList());
    }
}

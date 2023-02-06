package ru.yandex.direct.core.entity.campaign.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.metrika.client.model.request.GetExistentCountersRequest;
import ru.yandex.direct.metrika.client.model.request.GetGoalsRequest;
import ru.yandex.direct.metrika.client.model.request.GoalType;
import ru.yandex.direct.metrika.client.model.request.UserCountersExtendedFilter;
import ru.yandex.direct.metrika.client.model.response.CounterGoal;
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect;
import ru.yandex.direct.metrika.client.model.response.GetExistentCountersResponse;
import ru.yandex.direct.metrika.client.model.response.GetExistentCountersResponseItem;
import ru.yandex.direct.metrika.client.model.response.GetGoalsResponse;
import ru.yandex.direct.metrika.client.model.response.RetargetingCondition;
import ru.yandex.direct.metrika.client.model.response.UserCounters;
import ru.yandex.direct.metrika.client.model.response.UserCountersExtended;
import ru.yandex.direct.metrika.client.model.response.UserCountersExtendedResponse;
import ru.yandex.direct.metrika.client.model.response.UserCountersResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RequestBasedMetrikaClientAdapterTest {

    private static final Long AB_SEGEMENT_GOAL_ID1 = 2_500_000_001L;
    private static final Long AB_SEGEMENT_GOAL_ID2 = 2_500_000_002L;
    private static final Long AB_SEGEMENT_GOAL_ID3 = 2_500_000_003L;
    private static final Long GOAL_ID1 = 1L;
    private static final Long GOAL_ID2 = 2L;

    private RequestBasedMetrikaClientAdapter metrikaClientAdapter;

    private MetrikaClient metrikaClient;

    private Collection<Long> representativesUids;

    @Before
    public void setUp() throws Exception {
        metrikaClient = mock(MetrikaClient.class);
        representativesUids = List.of(1L, 2L);
        metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClient, representativesUids,
                Set.of(FeatureName.GOALS_FROM_ALL_ORGS_ALLOWED.getName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGetGoalsFromMetrikaOnce() {
        //given
        var metrikaData1 = Map.of(1L, List.of(new RetargetingCondition()));
        var metrikaData2 = Map.of(2L, List.of(new RetargetingCondition()));
        when(metrikaClient.getGoals(representativesUids, GoalType.AB_SEGMENT))
                .thenReturn(new GetGoalsResponse()
                        .withUidToConditions(metrikaData1), new GetGoalsResponse()
                        .withUidToConditions(metrikaData2));

        //when
        var result1 = metrikaClientAdapter.getAbSegmentGoals();
        var result2 = metrikaClientAdapter.getAbSegmentGoals();

        //then
        verify(metrikaClient, times(1)).getGoals(representativesUids, GoalType.AB_SEGMENT);
        assertEquals(metrikaData1, result1);
        assertEquals(result1, result2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGetUserCountersNumOnce() {
        //given
        var metrikaData1 = List.of(new UserCounters().withOwner(1L));
        var metrikaData2 = List.of(new UserCounters().withOwner(2L));
        var uidsArg = new ArrayList<>(representativesUids);

        when(metrikaClient.getUsersCountersNum2(eq(uidsArg), any()))
                .thenReturn(new UserCountersResponse()
                        .withUsers(metrikaData1), new UserCountersResponse()
                        .withUsers(metrikaData2));
        var campaigns = List.of(
                new TextCampaign().withId(1L).withMetrikaCounters(List.of(1L))
        );
        metrikaClientAdapter.setCampaignsCounterIds(campaigns);
        //when
        var result1 = metrikaClientAdapter.getUsersCountersNumByCampaignCounterIds();
        var result2 = metrikaClientAdapter.getUsersCountersNumByCampaignCounterIds();

        //then
        verify(metrikaClient, times(1)).getUsersCountersNum2(eq(uidsArg), any());
        assertEquals(metrikaData1, result1);
        assertEquals(result1, result2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGetUserCountersNumExtendedOnce() {
        //given
        var metrikaData1 = List.of(new UserCountersExtended().withOwner(1L));
        var metrikaData2 = List.of(new UserCountersExtended().withOwner(2L));
        var uidsArg = new ArrayList<>(representativesUids);
        var campaigns = List.of(
                new TextCampaign().withId(1L).withMetrikaCounters(List.of(1L))
        );
        metrikaClientAdapter.setCampaignsCounterIds(campaigns);
        when(metrikaClient.getUsersCountersNumExtended2(eq(uidsArg), any()))
                .thenReturn(new UserCountersExtendedResponse()
                        .withHasMoreCounters(false)
                        .withUsers(metrikaData1), new UserCountersExtendedResponse()
                        .withHasMoreCounters(false)
                        .withUsers(metrikaData2));

        //when
        var result1 = metrikaClientAdapter.getUsersCountersNumExtendedByCampaignCounterIds();
        var result2 = metrikaClientAdapter.getUsersCountersNumExtendedByCampaignCounterIds();

        //then
        verify(metrikaClient, times(1)).getUsersCountersNumExtended2(eq(uidsArg), any());
        assertEquals(metrikaData1, result1);
        assertEquals(result1, result2);
    }

    @Test
    public void shouldGetCampaignCountersOnce() {
        //given
        var campaigns = List.of(
                new TextCampaign().withId(1L).withMetrikaCounters(List.of(1L))
        );
        var metrikaResponse = new GetExistentCountersResponse()
                .withResponseItems(List.of(mock(GetExistentCountersResponseItem.class)));
        metrikaClientAdapter.setCampaignsCounterIds(campaigns);

        when(metrikaClient.getExistentCounters(any()))
                .thenReturn(metrikaResponse);

        //when
        var result1 = metrikaClientAdapter.getCampaignCountersIfNeeded();
        var result2 = metrikaClientAdapter.getCampaignCountersIfNeeded();

        //then
        var requestArgumentCaptor = ArgumentCaptor.forClass(GetExistentCountersRequest.class);
        verify(metrikaClient, times(1)).getExistentCounters(requestArgumentCaptor.capture());
        assertEquals(result1, result2);
        assertThat(requestArgumentCaptor.getValue().getCounterIds()).containsOnly(1L);
    }

    @Test
    public void shouldGetCampaignCounters_ifUnavailableGoalsAllowed() {
        //given
        var campaigns = List.of(
                new TextCampaign().withId(1L).withMetrikaCounters(List.of(1L))
        );
        metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClient, representativesUids,
                Set.of(), campaigns, true);

        var metrikaResponse = new GetExistentCountersResponse()
                .withResponseItems(List.of(mock(GetExistentCountersResponseItem.class)));
        when(metrikaClient.getExistentCounters(any()))
                .thenReturn(metrikaResponse);

        //when
        var result1 = metrikaClientAdapter.getCampaignCountersIfNeeded();
        var result2 = metrikaClientAdapter.getCampaignCountersIfNeeded();

        //then
        var requestArgumentCaptor = ArgumentCaptor.forClass(GetExistentCountersRequest.class);
        verify(metrikaClient, times(1)).getExistentCounters(requestArgumentCaptor.capture());
        assertEquals(result1, result2);
        assertThat(requestArgumentCaptor.getValue().getCounterIds()).containsOnly(1L);
    }

    @Test
    public void shouldReturnEmptyCampaignCounters_ifCampaignListIsEmpty() {
        var result = metrikaClientAdapter.getCampaignCountersIfNeeded();
        assertThat(result).isEmpty();
        verify(metrikaClient, never()).getExistentCounters(any());
    }

    @Test
    public void shouldReturnEmptyCampaignCounters_ifFeatureGoalsFromAllOrgsAllowedDisabled() {
        var campaigns = List.of(
                new TextCampaign().withId(1L).withMetrikaCounters(List.of(1L))
        );
        metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClient, representativesUids,
                Set.of(), campaigns, false);
        var result = metrikaClientAdapter.getCampaignCountersIfNeeded();
        assertThat(result).isEmpty();
        verify(metrikaClient, never()).getExistentCounters(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGetMassCountersGoalsOnce() {
        var uidsArg = new ArrayList<>(representativesUids);
        var campaigns = List.of(
                new TextCampaign().withId(1L).withMetrikaCounters(List.of(1L, 3L))
        );
        var countersExtendeds = List.of(new UserCountersExtended()
                .withCounters(List.of(new CounterInfoDirect().withId(1)))
        );
        var existentCountersResponse = new GetExistentCountersResponse()
                .withResponseItems(List.of(new GetExistentCountersResponseItem().withCounterId(3L)));
        var metrikaMassCountersGoals = Map.of(1, List.of(mock(CounterGoal.class)));
        metrikaClientAdapter.setCampaignsCounterIds(campaigns);
        var filter = new UserCountersExtendedFilter()
                .withCounterIds(List.of(1L, 3L));
        when(metrikaClient.getUsersCountersNumExtended2(uidsArg, filter))
                .thenReturn(new UserCountersExtendedResponse()
                        .withHasMoreCounters(false)
                        .withUsers(countersExtendeds));
        when(metrikaClient.getExistentCounters(new GetExistentCountersRequest()
                .withCounterIds(List.of(1L, 3L))))
                .thenReturn(existentCountersResponse);
        when(metrikaClient.getMassCountersGoalsFromMetrika(any()))
                .thenReturn(metrikaMassCountersGoals);

        var result1 = metrikaClientAdapter.getCountersGoals();
        var result2 = metrikaClientAdapter.getCountersGoals();

        verify(metrikaClient, times(1)).getUsersCountersNumExtended2(uidsArg, filter);
        verify(metrikaClient, times(1)).getExistentCounters(any());
        var requestCaptor = ArgumentCaptor.forClass(Set.class);
        verify(metrikaClient, times(1)).getMassCountersGoalsFromMetrika(requestCaptor.capture());
        assertThat(requestCaptor.getValue()).containsOnly(1, 3);
        assertThat(result1)
                .isEqualTo(result2)
                .isEqualTo(metrikaMassCountersGoals);
    }

    @Test
    public void shouldReturnEmptyMassCountersGoals_ifCounterIdsIsEmpty() {
        var result = metrikaClientAdapter.getCountersGoals();
        assertThat(result).isEmpty();
        verify(metrikaClient, never()).getMassCountersGoalsFromMetrika(any());
    }

    @Test
    public void check_getGoals_SameGoalIds_ExpectedOnce() {
        List<Long> goalIds = List.of(AB_SEGEMENT_GOAL_ID1, AB_SEGEMENT_GOAL_ID2, AB_SEGEMENT_GOAL_ID3, GOAL_ID1,
                GOAL_ID2);
        Map<Long, List<RetargetingCondition>> getAbSegments = Map.of(1L, List.of(new RetargetingCondition()
                .withId(AB_SEGEMENT_GOAL_ID1)), 2L, List.of(new RetargetingCondition()
                .withId(AB_SEGEMENT_GOAL_ID2), new RetargetingCondition()
                .withId(AB_SEGEMENT_GOAL_ID3)));
        Map<Long, List<RetargetingCondition>> getGoals = Map.of(2L, List.of(new RetargetingCondition()
                .withId(GOAL_ID1), new RetargetingCondition()
                .withId(GOAL_ID2)));
        var expectedRequest1 = new GetGoalsRequest()
                .withUids(representativesUids)
                .withGoalType(GoalType.AB_SEGMENT)
                .withIds(List.of(AB_SEGEMENT_GOAL_ID1, AB_SEGEMENT_GOAL_ID2, AB_SEGEMENT_GOAL_ID3));
        when(metrikaClient.getGoals(expectedRequest1))
                .thenReturn(new GetGoalsResponse()
                        .withUidToConditions(getAbSegments));

        var expectedRequest2 = new GetGoalsRequest()
                .withUids(representativesUids)
                .withGoalType(GoalType.GOAL)
                .withIds(List.of(GOAL_ID1, GOAL_ID2));
        when(metrikaClient.getGoals(expectedRequest2))
                .thenReturn(new GetGoalsResponse()
                        .withUidToConditions(getGoals));
        var result = metrikaClientAdapter.getGoalsByUid(goalIds);
        var result2 = metrikaClientAdapter.getGoalsByUid(goalIds);

        assertThat(result).isEqualTo(result2);
        verify(metrikaClient, times(1)).getGoals(expectedRequest1);
        verify(metrikaClient, times(1)).getGoals(expectedRequest2);
    }

    @Test
    public void check_getGoals_DifferentGoalIds_ExpectedTwice() {
        List<Long> goalIds1 = List.of(AB_SEGEMENT_GOAL_ID1, AB_SEGEMENT_GOAL_ID2, GOAL_ID1);
        List<Long> goalIds2 = List.of(AB_SEGEMENT_GOAL_ID1, AB_SEGEMENT_GOAL_ID2, GOAL_ID1, AB_SEGEMENT_GOAL_ID3,
                GOAL_ID2);

        var expectedRequest11 = new GetGoalsRequest()
                .withUids(representativesUids)
                .withGoalType(GoalType.AB_SEGMENT)
                .withIds(List.of(AB_SEGEMENT_GOAL_ID1, AB_SEGEMENT_GOAL_ID2));

        var expectedRequest12 = new GetGoalsRequest()
                .withUids(representativesUids)
                .withGoalType(GoalType.AB_SEGMENT)
                .withIds(List.of(AB_SEGEMENT_GOAL_ID3));
        when(metrikaClient.getGoals(expectedRequest11))
                .thenReturn(new GetGoalsResponse()
                        .withUidToConditions(Map.of(1L, List.of(new RetargetingCondition()
                                .withId(AB_SEGEMENT_GOAL_ID1)), 2L, List.of(new RetargetingCondition()
                                .withId(AB_SEGEMENT_GOAL_ID2)))));
        when(metrikaClient.getGoals(expectedRequest12))
                .thenReturn(new GetGoalsResponse()
                        .withUidToConditions(Map.of(2L, List.of(new RetargetingCondition()
                                .withId(AB_SEGEMENT_GOAL_ID3)))));

        var expectedRequest21 = new GetGoalsRequest()
                .withUids(representativesUids)
                .withGoalType(GoalType.GOAL)
                .withIds(List.of(GOAL_ID1));
        var expectedRequest22 = new GetGoalsRequest()
                .withUids(representativesUids)
                .withGoalType(GoalType.GOAL)
                .withIds(List.of(GOAL_ID2));
        when(metrikaClient.getGoals(expectedRequest21))
                .thenReturn(new GetGoalsResponse()
                        .withUidToConditions(Map.of(2L, List.of(new RetargetingCondition()
                                .withId(GOAL_ID1)))));
        when(metrikaClient.getGoals(expectedRequest22))
                .thenReturn(new GetGoalsResponse()
                        .withUidToConditions(Map.of(1L, List.of(new RetargetingCondition()
                                .withId(GOAL_ID2)))));
        metrikaClientAdapter.getGoalsByUid(goalIds1);
        metrikaClientAdapter.getGoalsByUid(goalIds2);

        verify(metrikaClient, times(1)).getGoals(expectedRequest11);
        verify(metrikaClient, times(1)).getGoals(expectedRequest12);
        verify(metrikaClient, times(1)).getGoals(expectedRequest21);
        verify(metrikaClient, times(1)).getGoals(expectedRequest22);
    }
}

package ru.yandex.direct.core.entity.metrika.service.campaigngoals;

import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.metrika.container.CampaignTypeWithCounterIds;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.metrika.client.model.response.CounterGoal;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.converter.CampaignConverter.SPRAV;
import static ru.yandex.direct.core.entity.campaign.converter.CampaignConverter.TURBODIRECT;
import static ru.yandex.direct.metrika.client.model.response.CounterGoal.Source.AUTO;
import static ru.yandex.direct.metrika.client.model.response.CounterGoal.Source.USER;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveInteger;

public abstract class CampaignGoalsServiceTest {
    private static int counter1, counter2;
    private static int goal1, goal21, goal22;

    @Autowired
    private Steps steps;
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    private Long uid;
    private ClientId clientId;
    private List<TextCampaign> campaigns;

    @Before
    public void before() {
        counter1 = nextPositiveInteger();
        counter2 = nextPositiveInteger();
        goal1 = nextPositiveInteger();
        goal21 = nextPositiveInteger();
        goal22 = nextPositiveInteger();

        var clientInfo = steps.clientSteps().createDefaultClient();
        uid = clientInfo.getUid();
        clientId = clientInfo.getClientId();
        campaigns = List.of(new TextCampaign()
                .withId(1L)
                .withMetrikaCounters(List.of((long) counter1, (long) counter2)));
    }

    @After
    public void after() {
        metrikaClientStub.clearUnavailableCounters();
    }

    @Test
    public void getGoals_AllCountersAvailable() {
        metrikaClientStub.addUserCounterIds(uid, List.of(counter1, counter2));
        metrikaClientStub.addCounterGoal(counter1, goal1);
        metrikaClientStub.addCounterGoal(counter2, goal21);

        Set<Long> availableGoals = getAvailableGoals();

        assertThat(availableGoals).containsExactlyInAnyOrder((long) goal1, (long) goal21);
    }

    @Test
    public void getGoals_OneCounterAvailable() {
        metrikaClientStub.addUserCounterIds(uid, List.of(counter1));
        metrikaClientStub.addUnavailableCounter(counter2);
        metrikaClientStub.addCounterGoal(counter1, goal1);
        metrikaClientStub.addCounterGoal(counter2, goal21);

        Set<Long> availableGoals = getAvailableGoals();

        assertThat(availableGoals).containsExactlyInAnyOrder((long) goal1);
    }

    @Test
    public void getGoals_NoCountersAvailable() {
        metrikaClientStub.addUserCounterIds(uid, List.of());
        metrikaClientStub.addUnavailableCounters(List.of(counter1, counter2));
        metrikaClientStub.addCounterGoal(counter1, goal1);
        metrikaClientStub.addCounterGoal(counter2, goal21);

        Set<Long> availableGoals = getAvailableGoals();

        assertThat(availableGoals).isEmpty();
    }

    @Test
    public void getGoals_UnavailableAutoGoalAllowed() {
        metrikaClientStub.addUserCounterIds(uid, List.of(counter1));
        metrikaClientStub.addUnavailableCounter(counter2);
        metrikaClientStub.addCounterGoal(counter1, goal1);
        metrikaClientStub.addCounterGoal(counter2, new CounterGoal().withId(goal21).withSource(AUTO));
        metrikaClientStub.addCounterGoal(counter2, new CounterGoal().withId(goal22).withSource(USER));

        Set<Long> availableGoals = getGoalsWithUnavailableAutoGoals();

        assertThat(availableGoals).containsExactlyInAnyOrder((long) goal1, (long) goal21);
    }

    @Test
    public void getGoals_UnavailableGoalsAllowed() {
        metrikaClientStub.addUserCounterIds(uid, List.of(counter1));
        metrikaClientStub.addUnavailableCounter(counter2);
        metrikaClientStub.addCounterGoal(counter1, goal1);
        metrikaClientStub.addCounterGoal(counter2, new CounterGoal().withId(goal21).withSource(AUTO));
        metrikaClientStub.addCounterGoal(counter2, new CounterGoal().withId(goal22).withSource(USER));

        Set<Long> availableGoals = getGoalsWithUnavailableGoals();

        assertThat(availableGoals).containsExactlyInAnyOrder((long) goal1, (long) goal21, (long) goal22);
    }

    @Test
    public void getGoals_UnavailableGoalsNotAllowed() {
        metrikaClientStub.addUserCounterIds(uid, List.of(counter1));
        metrikaClientStub.addUnavailableCounter(counter2);
        metrikaClientStub.addCounterGoal(counter1, goal1);
        metrikaClientStub.addCounterGoal(counter2, new CounterGoal().withId(goal21).withSource(AUTO));
        metrikaClientStub.addCounterGoal(counter2, new CounterGoal().withId(goal22).withSource(USER));

        Set<Long> availableGoals = getAvailableGoals();

        assertThat(availableGoals).containsExactlyInAnyOrder((long) goal1);
    }

    @Test
    public void getGoals_UnavailableGoalUnderTechCounterAllowed() {
        metrikaClientStub.addUserCounterIds(uid, List.of(counter1));
        metrikaClientStub.addUnavailableCounter(counter2, TURBODIRECT);
        metrikaClientStub.addCounterGoal(counter1, goal1);
        metrikaClientStub.addCounterGoal(counter2, new CounterGoal().withId(goal22).withSource(USER));

        Set<Long> availableGoals = getGoalsWithUnavailableAutoGoals();

        assertThat(availableGoals).containsExactlyInAnyOrder((long) goal1, (long) goal22);
    }

    @Test
    public void getGoals_UnavailableGoalUnderTechCounterNotAllowed() {
        metrikaClientStub.addUserCounterIds(uid, List.of(counter1));
        metrikaClientStub.addUnavailableCounter(counter2, TURBODIRECT);
        metrikaClientStub.addCounterGoal(counter1, goal1);
        metrikaClientStub.addCounterGoal(counter2, new CounterGoal().withId(goal22).withSource(USER));

        Set<Long> availableGoals = getAvailableGoals();

        assertThat(availableGoals).containsExactlyInAnyOrder((long) goal1);
    }

    @Test
    public void getGoals_UnavailableGoalUnderOrgCounterAllowed() {
        metrikaClientStub.addUserCounterIds(uid, List.of(counter1));
        metrikaClientStub.addUnavailableCounter(counter2, SPRAV);
        metrikaClientStub.addCounterGoal(counter1, goal1);
        metrikaClientStub.addCounterGoal(counter2, goal21);

        Set<Long> availableGoals = getGoalsWithUnavailableOrgGoals();

        assertThat(availableGoals).containsExactlyInAnyOrder((long) goal1, (long) goal21);
    }

    @Test
    public void getGoals_UnavailableGoalUnderOrgCounterNotAllowed() {
        metrikaClientStub.addUserCounterIds(uid, List.of(counter1));
        metrikaClientStub.addUnavailableCounter(counter2, SPRAV);
        metrikaClientStub.addCounterGoal(counter1, goal1);
        metrikaClientStub.addCounterGoal(counter2, goal21);

        Set<Long> availableGoals = getAvailableGoals();

        assertThat(availableGoals).containsExactlyInAnyOrder((long) goal1);
    }

    private Set<Long> getAvailableGoals() {
        return getAvailableGoals(false, false, false);
    }

    private Set<Long> getGoalsWithUnavailableAutoGoals() {
        return getAvailableGoals(true, false, false);
    }

    private Set<Long> getGoalsWithUnavailableGoals() {
        return getAvailableGoals(false, true, false);
    }

    private Set<Long> getGoalsWithUnavailableOrgGoals() {
        return getAvailableGoals(false, false, true);
    }

    Set<Long> getAvailableGoals(boolean isUnavailableAutoGoalsAllowed,
                                boolean isUnavailableGoalsAllowed,
                                boolean isGoalsFromAllOrgsAllowed) {
        var campaignTypeWithCounterIds = new CampaignTypeWithCounterIds()
                .withCampaignType(CampaignType.TEXT)
                .withCounterIds(Set.of((long) counter1, (long) counter2))
                .withUnavailableAutoGoalsAllowed(isUnavailableAutoGoalsAllowed)
                .withUnavailableGoalsAllowed(isUnavailableGoalsAllowed);
        Set<String> enabledFeatures = isGoalsFromAllOrgsAllowed ?
                Set.of(FeatureName.GOALS_FROM_ALL_ORGS_ALLOWED.getName()) : Set.of();
        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub, Set.of(uid),
                enabledFeatures, campaigns, isUnavailableAutoGoalsAllowed || isUnavailableGoalsAllowed);
        return getAvailableGoals(uid, clientId, campaignTypeWithCounterIds, metrikaClientAdapter);
    }

    abstract Set<Long> getAvailableGoals(Long uid, ClientId clientId,
                                         CampaignTypeWithCounterIds campaignTypeWithCounterIds,
                                         RequestBasedMetrikaClientAdapter metrikaClientAdapter);

}

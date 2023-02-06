package ru.yandex.direct.grid.processing.service.inventori;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.asynchttpclient.Param;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.inventori.GdReachMultiBudgetRequest;
import ru.yandex.direct.grid.processing.model.retargeting.GdGoalMinimal;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleItemReq;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleType;
import ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemAgePoint;
import ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemGender;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.inventori.InventoriClient;
import ru.yandex.direct.inventori.model.request.CampaignParametersCorrections;
import ru.yandex.direct.inventori.model.request.CampaignPredictionRequest;
import ru.yandex.direct.inventori.model.request.CryptaGroup;
import ru.yandex.direct.inventori.model.request.GroupType;
import ru.yandex.direct.inventori.model.request.Target;
import ru.yandex.direct.inventori.model.request.TrafficTypeCorrections;
import ru.yandex.direct.inventori.model.response.MultiBudgetsPredictionResponse;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.inventori.service.InventoriServiceCore.ALLOWED_BLOCK_SIZES;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemAgePoint.AGE_0;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemAgePoint.AGE_25;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemAgePoint.AGE_35;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemAgePoint.AGE_55;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemAgePoint.AGE_INF;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemGender.FEMALE;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemGender.MALE;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.web.core.model.retargeting.CryptaInterestTypeWeb.short_term;

@GridProcessingTest
@RunWith(Parameterized.class)
public class InventoriGraphQlServiceReachMultiBudgetsRequestsTest {

    private static final String REACH_MULTI_BUDGETS_QUERY = "reachTouchMultiBudgets";
    private static final String QUERY_TEMPLATE = ""
            + "query {\n"
            + "  %s (input: %s) {\n"
            + "    totalReach\n"
            + "    reaches {\n"
            + "      budget\n"
            + "      reach\n"
            + "      leftIntervalReach\n"
            + "      rightIntervalReach\n"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final long GOAL_ID = 2499001372L;
    private static final String KEYWORD = "601";
    private static final String KEYWORD_SHORT = "602";
    private static final String KEYWORD_VALUE = "241";
    private static final Goal GOAL = (Goal) new Goal()
            .withId(GOAL_ID)
            .withParentId(0L)
            .withName("Подарки и цветы")
            .withInterestType(CryptaInterestType.all)
            .withKeyword(KEYWORD)
            .withKeywordValue(KEYWORD_VALUE)
            .withKeywordShort(KEYWORD_SHORT)
            .withKeywordValueShort(KEYWORD_VALUE);
    private static final TrafficTypeCorrections DEFAULT_TRAFFIC_TYPE_CORRECTIONS =
            new TrafficTypeCorrections(null, null, null, null, null, null);

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    private InventoriClient inventoriClient;

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public GdReachMultiBudgetRequest request;

    @Parameterized.Parameter(2)
    public List<Set<String>> socialDemoSegments;

    private User operator;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"Без ограничений на пол и возраст",
                        getRequest(asList(MALE, FEMALE), AGE_0, AGE_INF),
                        emptyList()},
                {"Один пол", getRequest(singletonList(FEMALE), AGE_0, AGE_INF),
                        singletonList(singleton("616:1"))},
                {"Один возраст", getRequest(asList(MALE, FEMALE), AGE_25, AGE_35),
                        singletonList(singleton("617:2"))},
                {"Несколько возрастов", getRequest(asList(MALE, FEMALE), AGE_25, AGE_55),
                        singletonList(asSet("617:2", "617:3", "617:4"))},
                {"Один пол и несколько возрастов", getRequest(singletonList(FEMALE), AGE_25, AGE_55),
                        asList(singleton("616:1"), asSet("617:2", "617:3", "617:4"))},
        };
        return Arrays.asList(data);
    }

    @Before
    public void initTestData() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);
        Mockito.reset(inventoriClient);
        when(inventoriClient.getParametrisedCampaignPrediction(any(), any(), any(), any(), any()))
                .thenReturn(new MultiBudgetsPredictionResponse(null, 0L, emptyList()));

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        steps.cryptaGoalsSteps().addAllSocialDemoGoals();
        testCryptaSegmentRepository.addAll(singleton(GOAL));
    }

    @Test
    public void test() throws JsonProcessingException {
        String query = String.format(QUERY_TEMPLATE, REACH_MULTI_BUDGETS_QUERY, graphQlSerialize(request));

        processor.processQuery(null, query, null, buildContext(operator));

        ArgumentCaptor<List<Param>> pathsArgument = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<CampaignPredictionRequest> requestArgument
                = ArgumentCaptor.forClass(CampaignPredictionRequest.class);
        verify(inventoriClient).getParametrisedCampaignPrediction(any(), any(), any(), requestArgument.capture(),
                pathsArgument.capture());

        List<Param> params = pathsArgument.getValue();
        Assert.assertThat(params, beanDiffer(singletonList(new Param("budgets", "1000000000,2000000000"))));

        CampaignPredictionRequest predictionRequest = requestArgument.getValue();
        Target target = predictionRequest.getTargets().get(0);

        List<Set<String>> segments = new ArrayList<>(socialDemoSegments);
        segments.add(0, singleton(KEYWORD_SHORT + ":" + KEYWORD_VALUE));
        List<CryptaGroup> cryptaGroups = mapList(segments, s -> new CryptaGroup().withSegments(s));

        Target expectedTarget = new Target()
                .withGroupType(GroupType.BANNER)
                .withBlockSizes(new ArrayList<>(ALLOWED_BLOCK_SIZES))
                .withCryptaGroups(cryptaGroups)
                .withRegions(singleton(225))
                .withTargetTags(emptyList())
                .withOrderTags(emptyList())
                .withCorrections(new CampaignParametersCorrections(DEFAULT_TRAFFIC_TYPE_CORRECTIONS));

        Assert.assertThat(target, beanDiffer(expectedTarget));
    }

    private static GdReachMultiBudgetRequest getRequest(List<GdTouchSocdemGender> genders,
                                                        GdTouchSocdemAgePoint ageLower,
                                                        GdTouchSocdemAgePoint ageUpper) {
        return new GdReachMultiBudgetRequest()
                .withAgeLower(ageLower)
                .withAgeUpper(ageUpper)
                .withGenders(genders)
                .withBudgets(asList(1000L, 2000L))
                .withGeo(singleton(225))
                .withConditions(singletonList(
                        new GdRetargetingConditionRuleItemReq()
                                .withType(GdRetargetingConditionRuleType.OR)
                                .withInterestType(short_term)
                                .withGoals(singletonList(new GdGoalMinimal().withId(GOAL_ID))
                                )));
    }
}

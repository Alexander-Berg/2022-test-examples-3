package ru.yandex.direct.grid.processing.service.inventori;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.inventori.GdReachBudgetInfo;
import ru.yandex.direct.grid.processing.model.inventori.GdReachMultiBudgetRequest;
import ru.yandex.direct.grid.processing.model.inventori.GdReachMultiBudgetsResult;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.inventori.InventoriClient;
import ru.yandex.direct.inventori.model.response.CampaignPredictionAvailableResponse;
import ru.yandex.direct.inventori.model.response.CampaignPredictionLowReachResponse;
import ru.yandex.direct.inventori.model.response.CampaignPredictionResponse;
import ru.yandex.direct.inventori.model.response.MultiBudgetsPredictionResponse;
import ru.yandex.direct.inventori.model.response.MultiBudgetsPredictionResponse.IntervalPredictionResult;
import ru.yandex.direct.inventori.model.response.error.PredictionResponseUnknownSegmentsError;
import ru.yandex.direct.inventori.model.response.error.PredictionResponseUnsupportedError;
import ru.yandex.direct.validation.defect.params.StringDefectParams;
import ru.yandex.direct.web.core.entity.inventori.validation.InventoriDefectIds;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemAgePoint.AGE_0;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemAgePoint.AGE_INF;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemGender.FEMALE;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemGender.MALE;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.inventori.model.response.error.ErrorType.UNKNOWN_SEGMENTS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(Parameterized.class)
public class InventoriGraphQlServiceReachMultiBudgetsResponsesTest {

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
    private InventoriClient inventoriClient;

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public CampaignPredictionResponse inventoryResponse;

    @Parameterized.Parameter(2)
    public GdReachMultiBudgetsResult expectedPayload;

    @Parameterized.Parameter(3)
    public GdDefect expectedDefect;

    private User operator;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"Пустой ответ",
                        new MultiBudgetsPredictionResponse(null, 0L, emptyList()),
                        new GdReachMultiBudgetsResult().withTotalReach(0L).withReaches(emptyList()),
                        null
                },
                {"Стандартный ответ",
                        new MultiBudgetsPredictionResponse(null, 5000L, singletonList(
                                new IntervalPredictionResult(1000_000_000L, 1000L, 900L, 1100L))),
                        new GdReachMultiBudgetsResult().withTotalReach(5000L).withReaches(singletonList(
                                new GdReachBudgetInfo().withBudget(1000L)
                                        .withReach(1000L)
                                        .withLeftIntervalReach(900L)
                                        .withRightIntervalReach(1100L))),
                        null
                },
                {"Охват меньше 1000",
                        new MultiBudgetsPredictionResponse(null, 5000L, singletonList(
                                new IntervalPredictionResult(1000_000_000L, 999L, 900L, 1100L))),
                        new GdReachMultiBudgetsResult().withTotalReach(5000L).withReaches(singletonList(
                                new GdReachBudgetInfo().withBudget(1000L)
                                        // Булевские значения в тесты не попадают
                                        //.withShowReachLessThan(true)
                                        .withReach(1000L)
                                        .withLeftIntervalReach(0L)
                                        .withRightIntervalReach(0L))),
                        null
                },
                {"Совпадающие с округлением до 1000 охваты для всех бюджетов, требующие уменьшения",
                        new MultiBudgetsPredictionResponse(null, 5000L, asList(
                                new IntervalPredictionResult(1000_000_000L, 1500L, 1300L, 1700L),
                                new IntervalPredictionResult(2000_000_000L, 2000L, 1800L, 2200L)
                        )),
                        new GdReachMultiBudgetsResult().withTotalReach(5000L).withReaches(asList(
                                new GdReachBudgetInfo().withBudget(1000L)
                                        .withReach(1000L)
                                        .withLeftIntervalReach(900L)
                                        .withRightIntervalReach(1100L),
                                new GdReachBudgetInfo().withBudget(2000L)
                                        .withReach(2000L)
                                        .withLeftIntervalReach(1800L)
                                        .withRightIntervalReach(2200L)
                        )),
                        null
                },
                {"Если один из охватов меньше 1000, корректировки не требуется",
                        new MultiBudgetsPredictionResponse(null, 5000L, asList(
                                new IntervalPredictionResult(1000_000_000L, 999L, 900L, 1100L),
                                new IntervalPredictionResult(2000_000_000L, 1000L, 900L, 1100L)
                        )),
                        new GdReachMultiBudgetsResult().withTotalReach(5000L).withReaches(asList(
                                new GdReachBudgetInfo().withBudget(1000L)
                                        .withReach(1000L)
                                        .withLeftIntervalReach(0L)
                                        .withRightIntervalReach(0L),
                                new GdReachBudgetInfo().withBudget(2000L)
                                        .withReach(1000L)
                                        .withLeftIntervalReach(900L)
                                        .withRightIntervalReach(1100L)
                        )),
                        null
                },
                {"Совпадающие охваты для всех бюджетов, с суммой меньше 1000 после корректировки",
                        new MultiBudgetsPredictionResponse(null, 5000L, asList(
                                new IntervalPredictionResult(1000_000_000L, 1500L, 1400L, 1600L),
                                new IntervalPredictionResult(2000_000_000L, 1998L, 1800L, 2200L)
                        )),
                        new GdReachMultiBudgetsResult().withTotalReach(5000L).withReaches(asList(
                                new GdReachBudgetInfo().withBudget(1000L)
                                        .withReach(1000L)
                                        .withLeftIntervalReach(0L)
                                        .withRightIntervalReach(0L),
                                new GdReachBudgetInfo().withBudget(2000L)
                                        .withReach(1998L)
                                        .withLeftIntervalReach(1800L)
                                        .withRightIntervalReach(2200L)
                        )),
                        null
                },
                {"Недостаточный охват для прогноза",
                        new CampaignPredictionLowReachResponse(null, 5000L),
                        new GdReachMultiBudgetsResult().withTotalReach(0L).withReaches(emptyList()),
                        null
                },
                {"Ошибка со стороны прогнозатора - неизвестный сегмент",
                        new CampaignPredictionAvailableResponse(singletonList(
                                new PredictionResponseUnknownSegmentsError(UNKNOWN_SEGMENTS, singletonList("111"))),
                                null, null, null, null, null, null, null, null, null, null),
                        null,
                        new GdDefect()
                                .withCode(InventoriDefectIds.String.UNKNOWN_SEGMENTS.name())
                                .withParams(new StringDefectParams().withInvalidSubstrings(singletonList("111")))

                },
                {"Неизвестная ошибка со стороны прогнозатора",
                        new CampaignPredictionAvailableResponse(singletonList(new PredictionResponseUnsupportedError()),
                                null, null, null, null, null, null, null, null, null, null),
                        null,
                        new GdDefect()
                                .withCode(InventoriDefectIds.Gen.UNSUPPORTED_ERROR.name())
                                .withParams(new LinkedHashMap<>())

                },
        };
        return asList(data);
    }

    @Before
    public void initTestData() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);
        when(inventoriClient.getParametrisedCampaignPrediction(any(), any(), any(), any(), any()))
                .thenReturn(inventoryResponse);

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @After
    public void after() {
        reset(inventoriClient);
    }

    @Test
    public void test() {
        GdReachMultiBudgetRequest request = new GdReachMultiBudgetRequest()
                .withAgeLower(AGE_0)
                .withAgeUpper(AGE_INF)
                .withGenders(asList(MALE, FEMALE))
                .withGeo(singleton(225))
                .withConditions(emptyList());

        String query = String.format(QUERY_TEMPLATE, REACH_MULTI_BUDGETS_QUERY, graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));

        if (expectedDefect == null) {
            assertThat(result.getErrors()).isEmpty();

            Map<String, Object> data = result.getData();
            assertThat(data).containsOnlyKeys(REACH_MULTI_BUDGETS_QUERY);

            TypeReference<GdReachMultiBudgetsResult> typeReference =
                    new TypeReference<>() {
                    };

            GdReachMultiBudgetsResult payload = GraphQlJsonUtils
                    .convertValue(data.get(REACH_MULTI_BUDGETS_QUERY), typeReference);

            assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
        } else {
            ExceptionWhileDataFetching error = (ExceptionWhileDataFetching) result.getErrors().get(0);
            GridValidationException exception = (GridValidationException) error.getException();
            assertThat(exception.getValidationResult().getErrors().get(0)).is(matchedBy(beanDiffer(expectedDefect)));
        }
    }
}

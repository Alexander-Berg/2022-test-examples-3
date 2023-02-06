package ru.yandex.direct.grid.processing.service.showcondition;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.SoftAssertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionType;
import ru.yandex.direct.grid.core.entity.showcondition.service.GridShowConditionConstants;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.GdEntityStatsFilter;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionBaseStatus;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionFilter;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionOrderBy;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionOrderByField;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.utils.CommonUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.thymeleaf.util.SetUtils.singletonSet;
import static ru.yandex.direct.common.util.RepositoryUtils.booleanToLong;
import static ru.yandex.direct.feature.FeatureName.ADD_WITH_TOTALS_TO_SHOW_CONDITION_QUERY;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.ShowConditionTestDataUtils.getDefaultGdShowConditionsContainer;
import static ru.yandex.direct.grid.schema.yt.Tables.BIDSTABLE_DIRECT;

/**
 * Тест на сервис, проверям получение итоговой статистики из БД
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class ShowConditionGraphQlServiceGetTotalStatsTest {
    private final static Integer MAX_ROWS_FOR_TEST = 2;
    private final static Long CLICKS = 157L;
    private final static Long SHOWS = 1L;
    private final static Long TOTAL_CLICKS = 54678L;
    public static final GdShowConditionOrderBy ORDER_BY_ID = new GdShowConditionOrderBy()
            .withField(GdShowConditionOrderByField.ID)
            .withOrder(Order.ASC);
    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    showConditions(input: %s) {\n"
            + "      totalStats {\n"
            + "        clicks\n"
            + "      },\n"
            + "      totalStatsWithoutFiltersWarn,\n"
            + "      totalCount\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    private UserSteps userSteps;
    @Autowired
    private Steps steps;
    @Autowired
    private YtDynamicSupport gridYtSupport;

    private static int maxShowConditionRowsActual;
    private UserInfo userInfo;
    private GridGraphQLContext context;
    private AdGroupInfo adGroupInfo;

    @BeforeClass
    public static void beforeClass() throws Exception {
        maxShowConditionRowsActual = GridShowConditionConstants.getMaxConditionRows();
        GridShowConditionConstants.setMaxConditionRows(MAX_ROWS_FOR_TEST);
    }

    @Before
    public void before() {
        userInfo = userSteps.createDefaultUser();

        steps.featureSteps().addClientFeature(userInfo.getClientId(), ADD_WITH_TOTALS_TO_SHOW_CONDITION_QUERY, true);

        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(userInfo.getClientInfo());

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @AfterClass
    public static void afterClass() {
        GridShowConditionConstants.setMaxConditionRows(maxShowConditionRowsActual);
    }

    /**
     * При запросе в БД получаем количество условий показа меньше лимита и без общей статистики
     * -> получаем итоговую статистику посчитанную в коде и без флага показа предупреждения
     */
    @Test
    public void getKeywordsUnderLimitWithoutTotalStats_DoNotGetTotalStatsAndWarnFlag() {
        KeywordInfo keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo);

        doReturnFromYt(List.of(Pair.of(keywordInfo, CLICKS)));

        GdShowConditionsContainer container = getShowConditionsContainer(singletonSet(keywordInfo.getId()), emptySet());

        Map<String, Object> data = sendRequestAndGetTotalStats(container);

        checkResult(data, CLICKS, false, 1);
    }

    /**
     * При запросе в БД получаем количество условий показа равное лимиту и общую статистику, а список условий
     * не фильтруется в коде -> получаем итоговую статистику из БД и без флага показа предупреждения
     */
    @Test
    public void getKeywordsOverLimitWithTotalStats_WithoutCodeFilter_GetDbTotalStatsAndWithoutWarnFlag() {
        List<Pair<KeywordInfo, Long>> keywordAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> steps.keywordSteps().createKeyword(adGroupInfo))
                .map(keywordInfo -> Pair.of(keywordInfo, CLICKS))
                .toList();
        Set<Long> keywordIds = StreamEx.of(keywordAndClicks).map(p -> p.getLeft().getId()).toSet();

        // Add total row
        keywordAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(keywordAndClicks);

        GdShowConditionsContainer container = getShowConditionsContainer(keywordIds, emptySet());
        Map<String, Object> data = sendRequestAndGetTotalStats(container);

        checkResult(data, TOTAL_CLICKS, false, MAX_ROWS_FOR_TEST);
    }

    /**
     * При запросе в БД получаем количество условий показа равное лимиту и общую статистику, при этом список условий
     * не фильтруется в коде но фильтр (по коду) присутствует
     * -> получаем итоговую статистику из БД и с флагом показа предупреждения
     */
    @Test
    public void getKeywordsOverLimitWithTotalStats_WithCodeFilter_GetDbTotalStatsAndWithWarnFlag() {
        List<Pair<KeywordInfo, Long>> keywordAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> steps.keywordSteps().createKeyword(adGroupInfo))
                .map(keywordInfo -> Pair.of(keywordInfo, CLICKS))
                .toList();
        Set<Long> keywordIds = StreamEx.of(keywordAndClicks).map(p -> p.getLeft().getId()).toSet();

        // Add total row
        keywordAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(keywordAndClicks);

        GdShowConditionsContainer container =
                getShowConditionsContainer(keywordIds, Set.of(GdShowConditionBaseStatus.ACTIVE));
        Map<String, Object> data = sendRequestAndGetTotalStats(container);

        checkResult(data, TOTAL_CLICKS, true, MAX_ROWS_FOR_TEST);
    }

    /**
     * При запросе в БД получаем количество условий показа равное лимиту и общую статистику, но после фильтров
     * возвращается пустой список -> получаем итоговую статистику из БД и с флагом показа предупреждения
     */
    @Test
    public void getKeywordsOverLimitWithTotalStats_FilterKeywordsToEmpty_GetDbTotalStatsAndWithWarnFlag() {
        List<Pair<KeywordInfo, Long>> keywordAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> steps.keywordSteps().createDefaultKeyword())
                .map(keywordInfo -> Pair.of(keywordInfo, CLICKS))
                .toList();
        Set<Long> keywordIds = StreamEx.of(keywordAndClicks).map(p -> p.getLeft().getId()).toSet();

        // Add total row
        keywordAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(keywordAndClicks);

        GdShowConditionsContainer container =
                getShowConditionsContainer(keywordIds, Set.of(GdShowConditionBaseStatus.SUSPENDED));
        Map<String, Object> data = sendRequestAndGetTotalStats(container);

        checkResult(data, TOTAL_CLICKS, true, 0);
    }

    /**
     * При запросе в БД получаем количество условий показа равное лимиту и общую статистику, но затем список условий
     * фильтруется в коде -> получаем итоговую статистику из БД и с флагом показа предупреждения
     */
    @Test
    public void getKeywordsOverLimitWithTotalStats_WithCodeFilter_GetDbTotalStatsAndWithoutWarnFlag() {
        List<Pair<KeywordInfo, Long>> keywordAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST - 1)
                .mapToObj(i -> steps.keywordSteps().createKeyword(adGroupInfo))
                .map(keywordInfo -> Pair.of(keywordInfo, CLICKS))
                .toList();

        // Add keyword from differ campaign
        KeywordInfo keywordInfoFromDifferCampaign = steps.keywordSteps().createDefaultKeyword();
        keywordAndClicks.add(Pair.of(keywordInfoFromDifferCampaign, CLICKS));
        Set<Long> keywordIds = StreamEx.of(keywordAndClicks).map(p -> p.getLeft().getId()).toSet();

        // Add total row
        keywordAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(keywordAndClicks);

        GdShowConditionsContainer container = getShowConditionsContainer(keywordIds, emptySet());
        Map<String, Object> data = sendRequestAndGetTotalStats(container);

        checkResult(data, TOTAL_CLICKS, true, MAX_ROWS_FOR_TEST - 1);
    }

    /**
     * При запросе в БД получаем количество условий показа равное лимиту, но без общей статистики
     * -> получаем итоговую статистику, посчитанную в коде, без флага показа предупреждения
     */
    @Test
    public void getKeywords_OverLimitWithFilter_WithoutTotalStats() {
        List<Pair<KeywordInfo, Long>> keywordAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> steps.keywordSteps().createKeyword(adGroupInfo))
                .map(keywordInfo -> Pair.of(keywordInfo, CLICKS))
                .toList();
        Set<Long> keywordIds = StreamEx.of(keywordAndClicks).map(p -> p.getLeft().getId()).toSet();

        doReturnFromYt(keywordAndClicks);

        GdShowConditionsContainer container = getShowConditionsContainer(keywordIds, emptySet());
        Map<String, Object> data = sendRequestAndGetTotalStats(container);

        checkResult(data, CLICKS * MAX_ROWS_FOR_TEST, false, MAX_ROWS_FOR_TEST);
    }

    private void checkResult(Map<String, Object> data,
                             Long expectTotalClicks,
                             Boolean expectTotalStatsWarn,
                             Integer expectTotalCount) {
        Long totalClicks = getDataValue(data, "totalStats/clicks");
        Boolean totalStatsWithoutFiltersWarn = getDataValue(data, "totalStatsWithoutFiltersWarn");
        Integer totalCount = getDataValue(data, "totalCount");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(totalClicks)
                .as("количество кликов из БД")
                .isEqualTo(expectTotalClicks);
        soft.assertThat(totalStatsWithoutFiltersWarn)
                .as("предупреждение, что итоговая статистика без учета части фильтров")
                .isEqualTo(expectTotalStatsWarn);
        soft.assertThat(totalCount)
                .as("количество условий показа")
                .isEqualTo(expectTotalCount);
        soft.assertAll();
    }

    private GdShowConditionsContainer getShowConditionsContainer(Set<Long> keywordIds,
                                                                 Set<GdShowConditionBaseStatus> statusIn) {
        return getDefaultGdShowConditionsContainer()
                .withFilter(new GdShowConditionFilter()
                        .withShowConditionIdIn(keywordIds)
                        .withStatusIn(CommonUtils.nvl(statusIn, emptySet()))
                        .withCampaignIdIn(singletonSet(adGroupInfo.getCampaignId()))
                        .withStats(new GdEntityStatsFilter().withMinClicks(1L))
                )
                .withOrderBy(Collections.singletonList(ORDER_BY_ID));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendRequestAndGetTotalStats(GdShowConditionsContainer conteiner) {
        String query = String.format(QUERY_TEMPLATE, context.getSubjectUser().getLogin(),
                graphQlSerialize(conteiner));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());

        Map<String, Object> clientData = (Map<String, Object>) ((Map<String, Object>) result.getData()).get("client");
        return (Map<String, Object>) clientData.get("showConditions");
    }

    private void doReturnFromYt(Collection<Pair<KeywordInfo, Long>> keywordAndClicks) {
        doReturn(wrapInRowset(
                StreamEx.of(keywordAndClicks)
                        .map(keywordAndClick -> {
                                    KeywordInfo keywordInfo = keywordAndClick.getLeft();
                                    YTreeBuilder yTreeBuilder = YTree.mapBuilder()
                                            .key(GdiEntityStats.CLICKS.name()).value(keywordAndClick.getRight())
                                            .key(GdiEntityStats.SHOWS.name()).value(SHOWS);
                                    if (keywordInfo != null) {
                                        yTreeBuilder
                                                .key(BIDSTABLE_DIRECT.CID.getName()).value(keywordInfo.getCampaignId())
                                                .key(BIDSTABLE_DIRECT.PID.getName()).value(keywordInfo.getAdGroupId())
                                                .key(BIDSTABLE_DIRECT.ID.getName()).value(keywordInfo.getId())
                                                .key(BIDSTABLE_DIRECT.PHRASE_ID.getName()).value(keywordInfo.getId())
                                                .key(BIDSTABLE_DIRECT.PHRASE.getName())
                                                .value(keywordInfo.getKeyword().getPhrase())
                                                .key(BIDSTABLE_DIRECT.IS_SUSPENDED.getName())
                                                .value(keywordInfo.getKeyword().getIsSuspended())
                                                .key(BIDSTABLE_DIRECT.IS_DELETED.getName()).value(booleanToLong(false))
                                                .key(BIDSTABLE_DIRECT.BID_TYPE.getName())
                                                .value(GdiShowConditionType.KEYWORD.name().toLowerCase())
                                                .key(BIDSTABLE_DIRECT.IS_ARCHIVED.getName()).value(booleanToLong(false));
                                    }
                                    return yTreeBuilder.endMap().build();
                                }
                        ).toList()))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(), anyBoolean());
    }

    private UnversionedRowset wrapInRowset(List<YTreeNode> nodes) {
        UnversionedRowset rowset = mock(UnversionedRowset.class);
        doReturn(nodes).when(rowset).getYTreeRows();
        return rowset;
    }
}

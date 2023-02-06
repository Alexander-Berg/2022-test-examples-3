package ru.yandex.direct.grid.processing.service.showcondition.retargeting;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.SoftAssertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.entity.showcondition.service.GridShowConditionConstants;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.GdEntityStatsFilter;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingBaseStatus;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingFilter;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingOrderBy;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingOrderByField;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.feature.FeatureName.ADD_WITH_TOTALS_TO_BIDS_RETARGETING_QUERY;
import static ru.yandex.direct.feature.FeatureName.SHOW_DNA_BY_DEFAULT;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.BIDS_RETARGETINGTABLE_DIRECT;
import static ru.yandex.direct.grid.schema.yt.Tables.RETARGETING_CONDITIONSTABLE_DIRECT;


/**
 * Тест на сервис, проверям получение итоговой статистики из БД
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetRetargetingsGraphQlGetTotalStatsTest {
    private final static Integer MAX_ROWS_FOR_TEST = 2;
    private final static Long CLICKS = 553L;
    private final static Long SHOWS = 1L;
    private final static Long TOTAL_CLICKS = 55557L;
    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    retargetings(input: %s) {\n"
            + "      totalStats {\n"
            + "        clicks\n"
            + "      },\n"
            + "      totalStatsWithoutFiltersWarn,\n"
            + "      totalCount\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

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

    private static int maxRowsActual;
    private UserInfo userInfo;
    private GridGraphQLContext context;
    private AdGroupInfo adGroupInfo;

    @BeforeClass
    public static void beforeClass() throws Exception {
        maxRowsActual = GridShowConditionConstants.getMaxConditionRows();
        GridShowConditionConstants.setMaxConditionRows(MAX_ROWS_FOR_TEST);
    }

    @Before
    public void before() {
        userInfo = userSteps.createDefaultUser();

        steps.featureSteps().addClientFeature(userInfo.getClientId(), SHOW_DNA_BY_DEFAULT, true);
        steps.featureSteps().addClientFeature(userInfo.getClientId(), ADD_WITH_TOTALS_TO_BIDS_RETARGETING_QUERY, true);

        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(userInfo.getClientInfo());

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @AfterClass
    public static void afterClass() {
        GridShowConditionConstants.setMaxConditionRows(maxRowsActual);
    }

    /**
     * При запросе в БД получаем количество ретаргетинга меньше лимита и без общей статистики
     * -> получаем итоговую статистику посчитанную в коде и без флага показа предупреждения
     */
    @Test
    public void getRetargetingsUnderLimitWithoutTotalStats_DoNotGetTotalStatsAndWarnFlag() {
        var retargetingInfo = steps.retargetingSteps().createDefaultRetargeting(adGroupInfo);

        doReturnFromYt(List.of(Pair.of(retargetingInfo, CLICKS)));

        GdRetargetingsContainer container = getRetargetingsContainer(emptySet());
        Map<String, Object> data = sendRequestAndGetTotalStats(container);

        checkResult(data, CLICKS, false, 1);
    }

    /**
     * При запросе в БД получаем количество ретаргетинга равное лимиту и общую статистику, а список ретаргетинга не
     * фильтруется в коде -> получаем итоговую статистику из БД и без флага показа предупреждения
     */
    @Test
    public void getRetargetingsOverLimitWithTotalStats_WithoutCodeFilter_GetDbTotalStatsAndWithoutWarnFlag() {
        List<Pair<RetargetingInfo, Long>> retargetingAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> steps.retargetingSteps().createDefaultRetargeting(adGroupInfo))
                .map(retargetingInfo -> Pair.of(retargetingInfo, CLICKS))
                .toList();

        // Add total row
        retargetingAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(retargetingAndClicks);

        GdRetargetingsContainer container = getRetargetingsContainer(emptySet());
        Map<String, Object> data = sendRequestAndGetTotalStats(container);

        checkResult(data, TOTAL_CLICKS, false, MAX_ROWS_FOR_TEST);
    }

    /**
     * При запросе в БД получаем количество ретаргетинга равное лимиту и общую статистику, при этом список ретаргетинга
     * не фильтруется в коде но фильтр (по коду) присутствует
     * -> получаем итоговую статистику из БД и с флагом показа предупреждения
     */
    @Test
    public void getRetargetingsOverLimitWithTotalStats_WithCodeFilter_GetDbTotalStatsAndWithWarnFlag() {
        List<Pair<RetargetingInfo, Long>> retargetingAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> steps.retargetingSteps().createDefaultRetargeting(adGroupInfo))
                .map(retargetingInfo -> Pair.of(retargetingInfo, CLICKS))
                .toList();

        // Add total row
        retargetingAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(retargetingAndClicks);

        GdRetargetingsContainer container = getRetargetingsContainer(
                Set.of(GdRetargetingBaseStatus.ACTIVE, GdRetargetingBaseStatus.SUSPENDED));
        Map<String, Object> data = sendRequestAndGetTotalStats(container);

        checkResult(data, TOTAL_CLICKS, true, MAX_ROWS_FOR_TEST);
    }

    /**
     * При запросе в БД получаем количество ретаргетинга равное лимиту и общую статистику, но после фильтров
     * возвращается пустой список -> получаем итоговую статистику из БД и с флагом показа предупреждения
     */
    @Test
    public void getRetargetingsOverLimitWithTotalStats_FilterRetargetingsToEmpty_GetDbTotalStatsAndWithWarnFlag() {
        List<Pair<RetargetingInfo, Long>> retargetingAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> steps.retargetingSteps().createDefaultRetargeting(adGroupInfo))
                .map(retargetingInfo -> Pair.of(retargetingInfo, CLICKS))
                .toList();

        // Add total row
        retargetingAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(retargetingAndClicks);

        GdRetargetingsContainer container = getRetargetingsContainer(Set.of(GdRetargetingBaseStatus.SUSPENDED));
        Map<String, Object> data = sendRequestAndGetTotalStats(container);

        checkResult(data, TOTAL_CLICKS, true, 0);
    }

    /**
     * При запросе в БД получаем количество ретаргетинга равное лимиту и общую статистику, но затем список ретаргетинга
     * фильтруется в коде -> получаем итоговую статистику из БД и с флагом показа предупреждения
     */
    @Test
    public void getRetargetingsOverLimitWithTotalStats_WithCodeFilter_GetDbTotalStatsAndWithoutWarnFlag() {
        List<Pair<RetargetingInfo, Long>> retargetingAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST - 1)
                .mapToObj(i -> steps.retargetingSteps().createDefaultRetargeting(adGroupInfo))
                .map(retargetingInfo -> Pair.of(retargetingInfo, CLICKS))
                .toList();

        // Add retargeting from differ campaign
        RetargetingInfo retargetingInfoFromDifferCampaign = steps.retargetingSteps().createDefaultRetargeting();
        retargetingAndClicks.add(Pair.of(retargetingInfoFromDifferCampaign, CLICKS));

        // Add total row
        retargetingAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(retargetingAndClicks);

        GdRetargetingsContainer container = getRetargetingsContainer(emptySet());
        Map<String, Object> data = sendRequestAndGetTotalStats(container);

        checkResult(data, TOTAL_CLICKS, true, MAX_ROWS_FOR_TEST - 1);
    }

    /**
     * При запросе в БД получаем количество ретаргетинга равное лимиту, но без общей статистики
     * -> получаем итоговую статистику, посчитанную в коде, без флага показа предупреждения
     */
    @Test
    public void getRetargetings_OverLimitWithFilter_WithoutTotalStats() {
        List<Pair<RetargetingInfo, Long>> retargetingAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> steps.retargetingSteps().createDefaultRetargeting(adGroupInfo))
                .map(retargetingInfo -> Pair.of(retargetingInfo, CLICKS))
                .toList();

        doReturnFromYt(retargetingAndClicks);

        GdRetargetingsContainer container = getRetargetingsContainer(emptySet());
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
                .as("количество ретаргетингов")
                .isEqualTo(expectTotalCount);
        soft.assertAll();
    }

    private GdRetargetingsContainer getRetargetingsContainer(Set<GdRetargetingBaseStatus> statusIn) {
        GdRetargetingFilter filter = new GdRetargetingFilter()
                .withCampaignIdIn(Collections.singleton(adGroupInfo.getCampaignId()))
                .withAdGroupIdIn(Collections.singleton(adGroupInfo.getAdGroupId()))
                .withStatusIn(statusIn)
                .withStats(new GdEntityStatsFilter()
                        .withMinClicks(1L));

        LocalDate now = LocalDate.now();
        return new GdRetargetingsContainer()
                .withOrderBy(singletonList(new GdRetargetingOrderBy()
                        .withField(GdRetargetingOrderByField.CAMPAIGN_ID)
                        .withOrder(Order.ASC)))
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(now.minusDays(1))
                        .withTo(now))
                .withLimitOffset(new GdLimitOffset().withOffset(0).withLimit(MAX_ROWS_FOR_TEST))
                .withFilter(filter);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendRequestAndGetTotalStats(GdRetargetingsContainer container) {
        String query = String.format(QUERY_TEMPLATE, context.getSubjectUser().getLogin(),
                graphQlSerialize(container));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());

        Map<String, Object> clientData = (Map<String, Object>) ((Map<String, Object>) result.getData()).get("client");
        return (Map<String, Object>) clientData.get("retargetings");
    }

    private void doReturnFromYt(Collection<Pair<RetargetingInfo, Long>> retargetingAndClicks) {
        doReturn(wrapInRowset(
                StreamEx.of(retargetingAndClicks)
                        .map(retargetingAndClick -> {
                                    RetargetingInfo retargetingInfo = retargetingAndClick.getLeft();
                                    YTreeBuilder yTreeBuilder = YTree.mapBuilder()
                                            .key(GdiEntityStats.CLICKS.name()).value(retargetingAndClick.getRight())
                                            .key(GdiEntityStats.SHOWS.name()).value(SHOWS);
                                    if (retargetingInfo != null) {
                                        yTreeBuilder
                                                .key(BIDS_RETARGETINGTABLE_DIRECT.RET_ID.getName())
                                                .value(retargetingInfo.getRetargetingId())
                                                .key(BIDS_RETARGETINGTABLE_DIRECT.CID.getName())
                                                .value(retargetingInfo.getCampaignId())
                                                .key(BIDS_RETARGETINGTABLE_DIRECT.PID.getName())
                                                .value(retargetingInfo.getAdGroupId())
                                                .key(RETARGETING_CONDITIONSTABLE_DIRECT.CLIENT_ID.getName())
                                                .value(retargetingInfo.getClientId().asLong())
                                                .key(RETARGETING_CONDITIONSTABLE_DIRECT.RET_COND_ID.getName())
                                                .value(retargetingInfo.getRetConditionId());
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


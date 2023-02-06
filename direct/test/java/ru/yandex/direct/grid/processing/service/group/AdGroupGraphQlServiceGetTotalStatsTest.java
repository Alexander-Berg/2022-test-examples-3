package ru.yandex.direct.grid.processing.service.group;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
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

import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.entity.group.service.GridAdGroupConstants;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.GdEntityStatsFilter;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderBy;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderByField;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.thymeleaf.util.SetUtils.singletonSet;
import static ru.yandex.direct.feature.FeatureName.ADD_WITH_TOTALS_TO_GROUP_QUERY;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;

/**
 * Тест на сервис, проверям получение итоговой статистики из БД
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceGetTotalStatsTest {
    private final static Integer MAX_ROWS_FOR_TEST = 2;
    private final static Long CLICKS = 553L;
    private final static Long SHOWS = 1L;
    private final static Long TOTAL_CLICKS = 55557L;
    private static final GdAdGroupOrderBy ORDER_BY_ID = new GdAdGroupOrderBy()
            .withField(GdAdGroupOrderByField.ID)
            .withOrder(Order.ASC);
    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
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
    private Steps steps;
    @Autowired
    private CampaignSteps campaignSteps;
    @Autowired
    private AdGroupSteps groupSteps;
    @Autowired
    private YtDynamicSupport gridYtSupport;

    private static int maxGroupRowsActual;
    private UserInfo userInfo;
    private GridGraphQLContext context;
    private CampaignInfo campaignInfo;

    @BeforeClass
    public static void beforeClass() throws Exception {
        maxGroupRowsActual = GridAdGroupConstants.getMaxGroupRows();
        GridAdGroupConstants.setMaxGroupRows(MAX_ROWS_FOR_TEST);
    }

    @Before
    public void before() {
        userInfo = steps.userSteps().createDefaultUser();

        steps.featureSteps().addClientFeature(userInfo.getClientId(), ADD_WITH_TOTALS_TO_GROUP_QUERY, true);

        campaignInfo = campaignSteps.createActiveCampaign(userInfo.getClientInfo());

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @AfterClass
    public static void afterClass() {
        GridAdGroupConstants.setMaxGroupRows(maxGroupRowsActual);
    }

    /**
     * При запросе в БД получаем количество групп меньше лимита и без общей статистики
     * -> получаем итоговую статистику посчитанную в коде и без флага показа предупреждения
     */
    @Test
    public void getAdGroupsUnderLimitWithoutTotalStats_DoNotGetTotalStatsAndWarnFlag() {
        AdGroupInfo groupInfo = groupSteps.createAdGroup(TestGroups.activeTextAdGroup(), campaignInfo);

        doReturnFromYt(List.of(Pair.of(groupInfo, CLICKS)));

        GdAdGroupsContainer adGroupsContainer = getAdGroupsContainer(singletonSet(groupInfo.getAdGroupId()), null);
        Map<String, Object> data = sendRequestAndGetTotalStats(adGroupsContainer);

        checkResult(data, CLICKS, false, 1);
    }

    /**
     * При запросе в БД получаем количество групп равное лимиту и общую статистику, а список груп не фильтруется в коде
     * -> получаем итоговую статистику из БД и без флага показа предупреждения
     */
    @Test
    public void getAdGroupsOverLimitWithTotalStats_WithoutCodeFilter_GetDbTotalStatsAndWithoutWarnFlag() {
        List<Pair<AdGroupInfo, Long>> groupAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> groupSteps.createAdGroup(TestGroups.activeTextAdGroup(), campaignInfo))
                .map(groupInfo -> Pair.of(groupInfo, CLICKS))
                .toList();
        Set<Long> groupIds = StreamEx.of(groupAndClicks).map(p -> p.getLeft().getAdGroupId()).toSet();

        // Add total row
        groupAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(groupAndClicks);

        GdAdGroupsContainer adGroupsContainer = getAdGroupsContainer(groupIds, null);
        Map<String, Object> data = sendRequestAndGetTotalStats(adGroupsContainer);

        checkResult(data, TOTAL_CLICKS, false, MAX_ROWS_FOR_TEST);
    }

    /**
     * При запросе в БД получаем количество групп равное лимиту и общую статистику, при этом список груп
     * не фильтруется в коде но фильтр (по коду) присутствует
     * -> получаем итоговую статистику из БД и с флагом показа предупреждения
     */
    @Test
    public void getAdGroupsOverLimitWithTotalStats_WithCodeFilter_GetDbTotalStatsAndWithWarnFlag() {
        List<Pair<AdGroupInfo, Long>> groupAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> groupSteps.createAdGroup(TestGroups.activeTextAdGroup(), campaignInfo))
                .map(groupInfo -> Pair.of(groupInfo, CLICKS))
                .toList();
        Set<Long> groupIds = StreamEx.of(groupAndClicks).map(p -> p.getLeft().getAdGroupId()).toSet();

        // Add total row
        groupAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(groupAndClicks);

        GdAdGroupsContainer adGroupsContainer = getAdGroupsContainer(groupIds, false);
        Map<String, Object> data = sendRequestAndGetTotalStats(adGroupsContainer);

        checkResult(data, TOTAL_CLICKS, true, MAX_ROWS_FOR_TEST);
    }

    /**
     * При запросе в БД получаем количество групп равное лимиту и общую статистику, но после фильтров
     * возвращается пустой список -> получаем итоговую статистику из БД и с флагом показа предупреждения
     */
    @Test
    public void getAdGroupsOverLimitWithTotalStats_FilterGroupsToEmpty_GetDbTotalStatsAndWithWarnFlag() {
        List<Pair<AdGroupInfo, Long>> groupAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> groupSteps.createAdGroup(TestGroups.activeTextAdGroup(), campaignInfo))
                .map(groupInfo -> Pair.of(groupInfo, CLICKS))
                .toList();
        Set<Long> groupIds = StreamEx.of(groupAndClicks).map(p -> p.getLeft().getAdGroupId()).toSet();

        // Add total row
        groupAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(groupAndClicks);

        GdAdGroupsContainer adGroupsContainer = getAdGroupsContainer(groupIds, true);
        Map<String, Object> data = sendRequestAndGetTotalStats(adGroupsContainer);

        checkResult(data, TOTAL_CLICKS, true, 0);
    }

    /**
     * При запросе в БД получаем количество групп равное лимиту и общую статистику, но затем список груп
     * фильтруется в коде -> получаем итоговую статистику из БД и с флагом показа предупреждения
     */
    @Test
    public void getAdGroupsOverLimitWithTotalStats_WithCodeFilter_GetDbTotalStatsAndWithoutWarnFlag() {
        List<Pair<AdGroupInfo, Long>> groupAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST - 1)
                .mapToObj(i -> groupSteps.createAdGroup(TestGroups.activeTextAdGroup(), campaignInfo))
                .map(groupInfo -> Pair.of(groupInfo, CLICKS))
                .toList();

        // Add group from differ campaign
        AdGroupInfo adGroupInfoFromDifferCampaign = groupSteps.createAdGroup(TestGroups.activeTextAdGroup());
        groupAndClicks.add(Pair.of(adGroupInfoFromDifferCampaign, CLICKS));
        Set<Long> groupIds = StreamEx.of(groupAndClicks).map(p -> p.getLeft().getAdGroupId()).toSet();

        // Add total row
        groupAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(groupAndClicks);

        GdAdGroupsContainer adGroupsContainer = getAdGroupsContainer(groupIds, null);
        Map<String, Object> data = sendRequestAndGetTotalStats(adGroupsContainer);

        checkResult(data, TOTAL_CLICKS, true, MAX_ROWS_FOR_TEST - 1);
    }

    /**
     * При запросе в БД получаем количество групп равное лимиту, но без общей статистики
     * -> получаем итоговую статистику, посчитанную в коде, без флага показа предупреждения
     */
    @Test
    public void getAdGroup_OverLimitWithFilter_WithoutTotalStats() {
        List<Pair<AdGroupInfo, Long>> groupAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> groupSteps.createAdGroup(TestGroups.activeTextAdGroup(), campaignInfo))
                .map(groupInfo -> Pair.of(groupInfo, CLICKS))
                .toList();
        Set<Long> groupIds = StreamEx.of(groupAndClicks).map(p -> p.getLeft().getAdGroupId()).toSet();

        doReturnFromYt(groupAndClicks);

        GdAdGroupsContainer adGroupsContainer = getAdGroupsContainer(groupIds, null);
        Map<String, Object> data = sendRequestAndGetTotalStats(adGroupsContainer);

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
                .as("количество групп")
                .isEqualTo(expectTotalCount);
        soft.assertAll();
    }

    private GdAdGroupsContainer getAdGroupsContainer(Set<Long> groupIds, Boolean withArchived) {
        return getDefaultGdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withAdGroupIdIn(groupIds)
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId()))
                        .withArchived(withArchived)
                        .withStats(new GdEntityStatsFilter()
                                .withMinClicks(1L)))
                .withOrderBy(Collections.singletonList(ORDER_BY_ID));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendRequestAndGetTotalStats(GdAdGroupsContainer adGroupsContainer) {
        String query = String.format(QUERY_TEMPLATE, context.getSubjectUser().getLogin(),
                graphQlSerialize(adGroupsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());

        Map<String, Object> clientData = (Map<String, Object>) ((Map<String, Object>) result.getData()).get("client");
        return (Map<String, Object>) clientData.get("adGroups");
    }

    private void doReturnFromYt(Collection<Pair<AdGroupInfo, Long>> groupAndClicks) {
        doReturn(wrapInRowset(
                StreamEx.of(groupAndClicks)
                        .map(groupAndClick -> {
                                    AdGroupInfo groupInfo = groupAndClick.getLeft();
                                    YTreeBuilder yTreeBuilder = YTree.mapBuilder()
                                            .key(GdiEntityStats.CLICKS.name()).value(groupAndClick.getRight())
                                            .key(GdiEntityStats.SHOWS.name()).value(SHOWS);
                                    if (groupInfo != null) {
                                        yTreeBuilder
                                                .key(PHRASESTABLE_DIRECT.CID.getName()).value(groupInfo.getCampaignId())
                                                .key(PHRASESTABLE_DIRECT.PID.getName()).value(groupInfo.getAdGroupId())
                                                .key(PHRASESTABLE_DIRECT.ADGROUP_TYPE.getName())
                                                .value(groupInfo.getAdGroupType().name().toLowerCase());
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

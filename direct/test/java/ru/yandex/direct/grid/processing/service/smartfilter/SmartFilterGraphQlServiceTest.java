package ru.yandex.direct.grid.processing.service.smartfilter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.jooq.Select;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.tables.records.BidsPerformanceRecord;
import ru.yandex.direct.grid.core.entity.smartfilter.model.GdiSmartFilter;
import ru.yandex.direct.grid.core.frontdb.steps.FilterShortcutsSteps;
import ru.yandex.direct.grid.core.util.stats.GridStatNew;
import ru.yandex.direct.grid.core.util.stats.completestat.DirectPhraseStatData;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterConditionOperator;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterFilter;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterNowOptimizingBy;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterOrderBy;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterOrderByField;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterPrimaryStatus;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterTab;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterTargetFunnel;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFiltersContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.SmartFilterTestDataUtils;
import ru.yandex.direct.grid.schema.yt.tables.DirectphrasegoalsstatBs;
import ru.yandex.direct.grid.schema.yt.tables.Directphrasestatv2Bs;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class SmartFilterGraphQlServiceTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final String FILTERS_QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    smartFilters(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        adGroupId\n"
            + "        campaignId\n"
            + "        name\n"
            + "        priceCpc\n"
            + "        priceCpa\n"
            + "        autobudgetPriority\n"
            + "        targetFunnel\n"
            + "        nowOptimizingBy\n"
            + "        isSuspended\n"
            + "        tab\n"
            + "        stats {\n"
            + "          cost\n"
            + "        }\n"
            + "        goalStats {\n"
            + "          costPerAction\n"
            + "        }\n"
            + "        conditions {\n"
            + "          field\n"
            + "          operator\n"
            + "          stringValue\n"
            + "        }\n"
            + "      }\n"
            + "      filter {\n"
            + "        campaignIdIn\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;

    @Autowired
    private FilterShortcutsSteps filterShortcutsSteps;

    @Autowired
    private YtDynamicSupport ytSupport;

    private PerformanceAdGroupInfo adGroupInfo;
    private Long perfFilterId;
    private BidsPerformanceRecord bidsPerformanceRecord;
    private GridGraphQLContext context;

    private final Long goalId = 1L;
    private final Integer defaultYtValue = 10000000;

    // Такая точность устанавливается при получении данных из YT
    private final BigDecimal convertedDefaultYtValue = BigDecimal.TEN.setScale(2, RoundingMode.HALF_UP);

    private final GridStatNew<Directphrasestatv2Bs, DirectphrasegoalsstatBs> gridStat
            = new GridStatNew<>(DirectPhraseStatData.INSTANCE);

    @Before
    public void initTestData() {
        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        bidsPerformanceRecord = steps.performanceFilterSteps().addDefaultBidsPerformance(adGroupInfo);
        perfFilterId = bidsPerformanceRecord.getPerfFilterId();
        String options = "{\""
                + perfFilterId
                + "\": \"tree\""
                + "}";
        String key = "perf_filter:from_tab";
        steps.campaignSteps().createCampSecondaryOptions(adGroupInfo.getShard(), adGroupInfo.getCampaignId(),
                key, options);

        User user = userService.getUser(adGroupInfo.getUid());
        context = ContextHelper.buildContext(user);
        gridContextProvider.setGridContext(context);

        List<GdiSmartFilter> smartFilters = singletonList(new GdiSmartFilter()
                .withCampaignId(adGroupInfo.getCampaignId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withSmartFilterId(perfFilterId));

        doAnswer(getAnswer(smartFilters))
                .when(ytSupport).selectRows(any(Select.class));
    }

    public static Object[] parameters() {
        return new Object[][]{
                {true},
                {false},
        };
    }

    @Test
    @TestCaseName("use filterKey instead filter: {0}")
    @Parameters(method = "parameters")
    public void testGetSmartFilters(boolean replaceFilterToFilterKey) {
        LocalDate statDateTo = LocalDate.now();
        LocalDate statDateFrom = statDateTo.minusDays(2);

        GdSmartFiltersContainer gdSmartFiltersContainer =
                SmartFilterTestDataUtils.getDefaultGdSmartFiltersContainer()
                        .withFilter(new GdSmartFilterFilter()
                                .withCampaignIdIn(ImmutableSet.of(adGroupInfo.getCampaignId())))
                        .withStatRequirements(new GdStatRequirements()
                                .withFrom(statDateFrom)
                                .withTo(statDateTo)
                                .withGoalIds(ImmutableSet.of(goalId)));

        if (replaceFilterToFilterKey) {
            String jsonFilter = JsonUtils.toJson(gdSmartFiltersContainer.getFilter());
            String key = filterShortcutsSteps.saveFilter(adGroupInfo.getClientId(), jsonFilter);
            gdSmartFiltersContainer.setFilter(null);
            gdSmartFiltersContainer.setFilterKey(key);
        }

        ExecutionResult result = processFiltersQuery(gdSmartFiltersContainer);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client", ImmutableMap.of(
                        "smartFilters", ImmutableMap.of(
                                "filter", ImmutableMap.<String, Object>builder()
                                        .put("campaignIdIn", List.of(adGroupInfo.getCampaignId()))
                                        .build(),
                                "rowset", singletonList(map(
                                        "id", perfFilterId,
                                        "adGroupId", adGroupInfo.getAdGroupId(),
                                        "campaignId", adGroupInfo.getCampaignId(),
                                        "name", bidsPerformanceRecord.getName(),
                                        "priceCpc", null,
                                        "priceCpa", null,
                                        "autobudgetPriority", null,
                                        "targetFunnel", GdSmartFilterTargetFunnel.SAME_PRODUCTS.name(),
                                        "nowOptimizingBy", GdSmartFilterNowOptimizingBy.CPC.name(),
                                        "isSuspended", false,
                                        "tab", GdSmartFilterTab.CONDITION.name(),
                                        "stats", ImmutableMap.of("cost", convertedDefaultYtValue),
                                        "goalStats", singletonList(ImmutableMap.of("costPerAction", convertedDefaultYtValue)),
                                        "conditions", singletonList(ImmutableMap.of(
                                                "field", "price",
                                                "operator", GdSmartFilterConditionOperator.RANGE.name(),
                                                "stringValue", "[\"0.00-3000.00\"]"))))
                        )
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(allFields())));
    }

    @Test
    public void getAdGroup_success() {
        String templateWithAdGroup = ""
                + "{\n"
                + "  client(searchBy: {login: \"%s\"}) {\n"
                + "    smartFilters(input: %s) {\n"
                + "      rowset {\n"
                + "        id\n"
                + "        adGroupId\n"
                + "        adGroup {\n"
                + "          id\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        GdSmartFiltersContainer gdSmartFiltersContainer =
                SmartFilterTestDataUtils.getDefaultGdSmartFiltersContainer()
                        .withFilter(new GdSmartFilterFilter()
                                .withCampaignIdIn(singleton(adGroupInfo.getCampaignId()))
                                .withAdGroupIdIn(singleton(adGroupInfo.getAdGroupId())));

        ExecutionResult result = processCustomFiltersQuery(templateWithAdGroup, gdSmartFiltersContainer);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client", map(
                        "smartFilters", map(
                                "rowset", ImmutableList.of(map(
                                        "id", perfFilterId,
                                        "adGroupId", adGroupInfo.getAdGroupId(),
                                        "adGroup", map(
                                                "id", adGroupInfo.getAdGroupId())
                                ))
                        )
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(allFields())));
    }

    @Test
    public void getStatus_success() {
        PerformanceFilterInfo filterInfo = new PerformanceFilterInfo()
                .withAdGroupInfo(adGroupInfo)
                .withFilter(defaultPerformanceFilter(adGroupInfo.getAdGroupId(), adGroupInfo.getFeedId())
                        .withIsSuspended(true));
        Long suspendedFilterId = steps.performanceFilterSteps().addPerformanceFilter(filterInfo).getFilterId();

        String templateWithStatus = ""
                + "{\n"
                + "  client(searchBy: {login: \"%s\"}) {\n"
                + "    smartFilters(input: %s) {\n"
                + "      rowset {\n"
                + "        id\n"
                + "        status {\n"
                + "          suspended\n"
                + "          readOnly\n"
                + "          primaryStatus\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        GdSmartFiltersContainer gdSmartFiltersContainer =
                SmartFilterTestDataUtils.getDefaultGdSmartFiltersContainer()
                        .withFilter(new GdSmartFilterFilter()
                                .withCampaignIdIn(singleton(adGroupInfo.getCampaignId())))
                        .withOrderBy(singletonList(new GdSmartFilterOrderBy()
                                .withField(GdSmartFilterOrderByField.ID)
                                .withOrder(Order.ASC)));

        ExecutionResult result = processCustomFiltersQuery(templateWithStatus, gdSmartFiltersContainer);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client", map(
                        "smartFilters", map(
                                "rowset", ImmutableList.of(
                                        map("id", perfFilterId,
                                                "status", map(
                                                        "primaryStatus", GdSmartFilterPrimaryStatus.ACTIVE.name(),
                                                        "suspended", false,
                                                        "readOnly", false)),
                                        map("id", suspendedFilterId,
                                                "status", map(
                                                        "primaryStatus", GdSmartFilterPrimaryStatus.STOPPED.name(),
                                                        "suspended", true,
                                                        "readOnly", false))
                                )
                        )
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(allFields())));
    }

    @Test
    public void getSmartFilterSchema_noErrors() {
        String query = ""
                + "query SmartFeedSchemaQuery {\n"
                + "  reqId: getReqId\n"
                + "  constants {\n"
                + "    smartFilterSchemas {\n"
                + "      fields {\n"
                + "        name\n"
                + "        operators {\n"
                + "          maxItems\n"
                + "          type\n"
                + "        }\n"
                + "        ... on GdSmartFilterNumberField {\n"
                + "          max\n"
                + "          min\n"
                + "          precision\n"
                + "        }\n"
                + "        ... on GdSmartFilterStringField {\n"
                + "          maxLength\n"
                + "          minLength\n"
                + "          pattern\n"
                + "        }\n"
                + "        ... on GdSmartFilterEnumField {\n"
                + "          values\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        ExecutionResult result = processor.processQuery(null, query, null, context);
        assertThat(result.getErrors()).isEmpty();
    }

    private ExecutionResult processCustomFiltersQuery(String template,
                                                      GdSmartFiltersContainer gdSmartFiltersContainer) {
        String query = String.format(template, context.getOperator().getLogin(),
                graphQlSerialize(gdSmartFiltersContainer));
        return processor.processQuery(null, query, null, context);
    }

    private ExecutionResult processFiltersQuery(GdSmartFiltersContainer gdSmartFiltersContainer) {
        String query = String.format(FILTERS_QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(gdSmartFiltersContainer));
        return processor.processQuery(null, query, null, context);
    }


    private Answer<UnversionedRowset> getAnswer(List<GdiSmartFilter> smartFilters) {
        return invocation -> convertToSmartFilterRowset(smartFilters);
    }

    private UnversionedRowset convertToSmartFilterRowset(List<GdiSmartFilter> smartFilters) {
        RowsetBuilder builder = rowsetBuilder();
        smartFilters.forEach(filter -> builder.add(
                rowBuilder()
                        .withColValue(gridStat.getTableData().table().EXPORT_ID.getName(), filter.getCampaignId())
                        .withColValue(gridStat.getTableData().table().PHRASE_EXPORT_ID.getName(), filter.getSmartFilterId())
                        .withColValue(gridStat.getTableData().table().GROUP_EXPORT_ID.getName(), filter.getAdGroupId())
                        .withColValue("cost", defaultYtValue)
                        .withColValue("costPerAction" + goalId, defaultYtValue)
                )
        );

        return builder.build();
    }
}

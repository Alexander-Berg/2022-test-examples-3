package ru.yandex.direct.grid.processing.service.banner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.jooq.Select;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesRepository;
import ru.yandex.direct.core.testing.data.TestBanners;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.banner.GdAdFilter;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderBy;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderByField;
import ru.yandex.direct.grid.processing.model.banner.GdAdsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.dbschema.ppc.Tables.AGGR_STATUSES_BANNERS;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultGdAdsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.BANNERSTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

/**
 * Тест на отдачу aggr_statuses_banners.is_obsolete параметра
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class AdGraphQlServiceGetAggrStatusIsObsoleteTest {
    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    ads(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        aggregatedStatusInfo {\n"
            + "          isObsolete\n"
            + "        }"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    private static final String ACTIVE_BANNER_AGGR_DATA = ""
            + "{\"r\": [\"ACTIVE\"], "
            + "\"s\": \"RUN_OK\", "
            + "\"sts\": [\"PREACCEPTED\"]}";

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
    private Steps steps;
    @Autowired
    private YtDynamicSupport gridYtSupport;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;

    private ClientInfo clientInfo;
    private GridGraphQLContext context;
    private long campaignId;
    private TextBannerInfo bannerInfo;
    private long bannerId;

    public static Object[] parameters() {
        return new Object[][]{
                {true, ImmutableMap.of("isObsolete", true)},
                {false, ImmutableMap.of("isObsolete", false)},
        };
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        context = ContextHelper.buildContext(clientInfo.getChiefUserInfo().getUser());
        gridContextProvider.setGridContext(context);

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();

        AdGroupInfo groupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        var banner = TestBanners.draftTextBanner(campaignId, groupInfo.getAdGroupId());

        bannerInfo = steps.bannerSteps().createBanner(banner, clientInfo);
        bannerId = bannerInfo.getBannerId();
    }

    @After
    public void after() {
        aggregatedStatusesRepository.deleteAdStatuses(clientInfo.getShard(), singleton(bannerInfo.getBannerId()));
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("is obsolete: {0}, expect: {1}")
    public void testGetIsObsolete(boolean isObsolete,
                                  Map<String, Object> mapIsObsoleteExpected) {
        mockYtResult(bannerInfo);
        createBannerAggrStatus(bannerId, isObsolete);

        ExecutionResult result = processQueryGetBanner();
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expect = expectResult(bannerId, mapIsObsoleteExpected);
        assertThat(data).is(matchedBy(beanDiffer(expect)));
    }

    private ExecutionResult processQueryGetBanner() {
        GdAdsContainer adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(singleton(bannerId))
                        .withCampaignIdIn(ImmutableSet.of(campaignId)))
                .withOrderBy(singletonList(new GdAdOrderBy()
                        .withField(GdAdOrderByField.ID)
                        .withOrder(Order.ASC)));
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(adsContainer));
        return processor.processQuery(null, query, null, context);
    }

    private void mockYtResult(TextBannerInfo adInfo) {
        RowsetBuilder builder = rowsetBuilder();
        builder.add(rowBuilder()
                .withColValue(BANNERSTABLE_DIRECT.BID.getName(), adInfo.getBannerId())
                .withColValue(BANNERSTABLE_DIRECT.PID.getName(), adInfo.getAdGroupId())
                .withColValue(BANNERSTABLE_DIRECT.CID.getName(), adInfo.getCampaignId())
                .withColValue(BANNERSTABLE_DIRECT.BANNER_TYPE.getName(), adInfo.getBanner().getBannerType().name())
                .withColValue(BANNERSTABLE_DIRECT.STATUS_SHOW.getName(), "Yes")
                .withColValue(BANNERSTABLE_DIRECT.STATUS_ACTIVE.getName(), "Yes")
                .withColValue(BANNERSTABLE_DIRECT.STATUS_ARCH.getName(), "No")
                .withColValue(BANNERSTABLE_DIRECT.STATUS_BS_SYNCED.getName(), "Yes")
        );
        doReturn(builder.build())
                .when(gridYtSupport).selectRows(eq(adInfo.getShard()), any(Select.class), anyBoolean());
    }

    private void createBannerAggrStatus(Long pid, boolean isObsolete) {
        dslContextProvider.ppc(clientInfo.getShard())
                .insertInto(AGGR_STATUSES_BANNERS, AGGR_STATUSES_BANNERS.BID, AGGR_STATUSES_BANNERS.AGGR_DATA,
                        AGGR_STATUSES_BANNERS.UPDATED, AGGR_STATUSES_BANNERS.IS_OBSOLETE)
                .values(pid, ACTIVE_BANNER_AGGR_DATA, LocalDateTime.of(LocalDate.now(), LocalTime.MIN),
                        isObsolete ? 1L : 0L)
                .execute();
    }

    private Map<String, Object> expectResult(Long pid, Map<String, Object> mapToObsolete) {
        Map<String, Object> mapOfRowset = new HashMap<>();
        mapOfRowset.put("id", pid);
        mapOfRowset.put("aggregatedStatusInfo", mapToObsolete);
        return ImmutableMap.of(
                "client",
                ImmutableMap.of(
                        "ads", ImmutableMap.of(
                                "rowset", singletonList(mapOfRowset)
                        )
                ));
    }
}

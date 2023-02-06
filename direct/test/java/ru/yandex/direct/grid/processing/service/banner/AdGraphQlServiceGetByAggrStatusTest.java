package ru.yandex.direct.grid.processing.service.banner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import junitparams.naming.TestCaseName;
import one.util.streamex.EntryStream;
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
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.banner.GdAdFilter;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderBy;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderByField;
import ru.yandex.direct.grid.processing.model.banner.GdAdPrimaryStatus;
import ru.yandex.direct.grid.processing.model.banner.GdAdsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.dbschema.ppc.Tables.AGGR_STATUSES_ADGROUPS;
import static ru.yandex.direct.dbschema.ppc.Tables.AGGR_STATUSES_BANNERS;
import static ru.yandex.direct.feature.FeatureName.HIDE_OLD_SHOW_CAMPS_FOR_DNA;
import static ru.yandex.direct.feature.FeatureName.SHOW_AGGREGATED_STATUS_OPEN_BETA;
import static ru.yandex.direct.feature.FeatureName.SHOW_DNA_BY_DEFAULT;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultGdAdsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.BANNERSTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class AdGraphQlServiceGetByAggrStatusTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    ads(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    private static final String MODERATE_ADGROUP_AGGR_DATA = ""
            + "{\"r\": [\"ADGROUP_SHOW_CONDITIONS_ON_MODERATION\"], " +
            "\"s\": \"STOP_WARN\", " +
            "\"sts\": [\"MODERATION\"], " +
            "\"cnts\": {\"ads\": 1, " +
            "   \"b_s\": {\"RUN_OK\": 1}, " +
            "   \"kws\": 1, " +
            "   \"kw_s\": {\"DRAFT\": 1}, " +
            "   \"rets\": 0, " +
            "   \"b_sts\": {\"PREACCEPTED\": 1}}" +
            "}";
    private static final String REJECTED_ADGROUP_AGGR_DATA = ""
            + "{\"r\": [\"ADGROUP_REJECTED_ON_MODERATION\"], " +
            "\"s\": \"STOP_CRIT\", " +
            "\"sts\": [\"MODERATION\"], " +
            "\"cnts\": {\"ads\": 1, " +
            "   \"b_s\": {\"RUN_OK\": 1}, " +
            "   \"kws\": 1, " +
            "   \"kw_s\": {\"DRAFT\": 1}, " +
            "   \"rets\": 0, " +
            "   \"b_sts\": {\"PREACCEPTED\": 1}}" +
            "}";
    private static final String SUSPENDED_ADGROUP_AGGR_DATA = ""
            + "{\"r\": [\"ADGROUP_SHOW_CONDITIONS_SUSPENDED_BY_USER\"], " +
            "\"s\": \"STOP_OK\", " +
            "\"cnts\": {\"ads\": 1, " +
            "   \"b_s\": {\"RUN_OK\": 1}, " +
            "   \"kws\": 1, " +
            "   \"kw_s\": {\"STOP_OK\": 1}, " +
            "   \"rets\": 0, " +
            "   \"b_sts\": {\"PREACCEPTED\": 1}}" +
            "}";
    private static final String DRAFT_BANNER_AGGR_DATA = ""
            + "{\"r\": [\"DRAFT\"], "
            + "\"s\": \"DRAFT\", "
            + "\"sts\": [\"DRAFT\"]}";
    private static final String ACTIVE_BANNER_AGGR_DATA = ""
            + "{\"r\": [\"ACTIVE\"], "
            + "\"s\": \"RUN_OK\", "
            + "\"sts\": [\"PREACCEPTED\"]}";
    private static final String ARCHIVE_BANNER_AGGR_DATA = ""
            + "{\"r\": [\"NOTHING\"], "
            + "\"s\": \"ARCHIVED\", "
            + "\"sts\": [\"ARCHIVED\", \"SUSPENDED\", \"PREACCEPTED\"]}";

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
    private AdGroupInfo groupInfo;
    private GridGraphQLContext context;
    private long campaignId;
    private TextBannerInfo bannerInfo;
    private long bannerId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        context = ContextHelper.buildContext(clientInfo.getChiefUserInfo().getUser());
        gridContextProvider.setGridContext(context);

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();

        groupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        var banner = TestBanners.draftTextBanner(campaignId, groupInfo.getAdGroupId());

        bannerInfo = steps.bannerSteps().createBanner(banner, groupInfo);
        bannerId = bannerInfo.getBannerId();
    }

    @After
    public void after() {
        aggregatedStatusesRepository.deleteAdGroupStatuses(clientInfo.getShard(), singleton(groupInfo.getAdGroupId()));
        aggregatedStatusesRepository.deleteAdStatuses(clientInfo.getShard(), singleton(bannerInfo.getBannerId()));
    }

    public static Object[] parametersForGetByStatus() {
        return new Object[][]{
                // Все
                {"у баннера ACTIVE статус, запрос без фильтра по статусам -> получаем bannerId",
                        null, null, ACTIVE_BANNER_AGGR_DATA, true},
                {"у баннера DRAFT статус, запрос без фильтра по статусам -> получаем bannerId",
                        null, null, DRAFT_BANNER_AGGR_DATA, true},
                {"у баннера ARCHIVE статус, запрос без фильтра по статусам -> получаем bannerId",
                        null, null, ARCHIVE_BANNER_AGGR_DATA, true},
                {"у баннера нет статуса, запрос без фильтра по статусам -> получаем bannerId",
                        null, null, null, true},
                // Все, кроме архивных
                {"у баннера ACTIVE статус, запрос без фильтра по статусам и без archive -> получаем bannerId",
                        null, false, ACTIVE_BANNER_AGGR_DATA, true},
                {"у баннера DRAFT статус, запрос без фильтра по статусам и без archive -> получаем bannerId",
                        null, false, DRAFT_BANNER_AGGR_DATA, true},
                {"у баннера ARCHIVE статус, запрос без фильтра по статусам и без archive -> bannerId не получаем",
                        null, false, ARCHIVE_BANNER_AGGR_DATA, false},
                {"у баннера без статуса, запрос без фильтра по статусам и без archive -> получаем bannerId",
                        null, false, null, true},
                // Только Архивные
                {"у баннера ACTIVE статус, запрос без фильтра по статусам и с archive -> bannerId не получаем",
                        null, true, ACTIVE_BANNER_AGGR_DATA, false},
                {"у баннера DRAFT статус, запрос без фильтра по статусам и с archive -> bannerId не получаем",
                        null, true, DRAFT_BANNER_AGGR_DATA, false},
                {"у баннера ARCHIVE статус, запрос без фильтра по статусам и с archive -> получаем bannerId",
                        null, true, ARCHIVE_BANNER_AGGR_DATA, true},
                {"у баннера без статуса, запрос без фильтра по статусам и с archive -> bannerId не получаем",
                        null, true, null, false},
                // По статусам
                {"у баннера ACTIVE статус, запрос с фильтром по ACTIVE статусам -> получаем bannerId",
                        List.of(GdAdPrimaryStatus.ACTIVE), null, ACTIVE_BANNER_AGGR_DATA, true},
                {"у баннера DRAFT статус, запрос с фильтром по ACTIVE статусам -> bannerId не получаем",
                        List.of(GdAdPrimaryStatus.ACTIVE), null, DRAFT_BANNER_AGGR_DATA, false},
                {"у баннера ARCHIVE статус, запрос с фильтром по ACTIVE статусам -> bannerId не получаем",
                        List.of(GdAdPrimaryStatus.ACTIVE), null, ARCHIVE_BANNER_AGGR_DATA, false},
                {"у баннера без статуса, запрос с фильтром по ACTIVE статусам -> bannerId не получаем",
                        List.of(GdAdPrimaryStatus.ACTIVE), null, null, false},
                {"у баннера без статуса, запрос с фильтром по DRAFT статусам -> получаем bannerId",
                        List.of(GdAdPrimaryStatus.DRAFT), null, null, true},
                // По статусам, без архивных
                {"у баннера ACTIVE статус, запрос с фильтром по ACTIVE статусам и без archive -> получаем bannerId",
                        List.of(GdAdPrimaryStatus.ACTIVE), false, ACTIVE_BANNER_AGGR_DATA, true},
                {"у баннера DRAFT статус, запрос с фильтром по ACTIVE статусам и без archive -> bannerId не получаем",
                        List.of(GdAdPrimaryStatus.ACTIVE), false, DRAFT_BANNER_AGGR_DATA, false},
                {"у баннера ARCHIVE статус, запрос с фильтром по ACTIVE статусам и без archive -> bannerId не получаем",
                        List.of(GdAdPrimaryStatus.ACTIVE), false, ARCHIVE_BANNER_AGGR_DATA, false},
                {"у баннера без статуса, запрос с фильтром по ACTIVE статусам и без archive -> bannerId не получаем",
                        List.of(GdAdPrimaryStatus.ACTIVE), false, null, false},
                // По статусам, с архивными
                {"у баннера ACTIVE статус, запрос с фильтром по ACTIVE статусам и с archive -> получаем bannerId",
                        List.of(GdAdPrimaryStatus.ACTIVE), true, ACTIVE_BANNER_AGGR_DATA, true},
                {"у баннера DRAFT статус, запрос с фильтром по ACTIVE статусам и с archive -> bannerId не получаем",
                        List.of(GdAdPrimaryStatus.ACTIVE), true, DRAFT_BANNER_AGGR_DATA, false},
                {"у баннера ARCHIVE статус, запрос с фильтром по ACTIVE статусам и с archive -> получаем bannerId",
                        List.of(GdAdPrimaryStatus.ACTIVE), true, ARCHIVE_BANNER_AGGR_DATA, true},
                {"у баннера без статуса, запрос с фильтром по ACTIVE статусам и с archive -> bannerId не получаем",
                        List.of(GdAdPrimaryStatus.ACTIVE), true, null, false},
                // Несколько статусов в фильтре
                {"у баннера ACTIVE статус, запрос с фильтром по ACTIVE,DRAFT статусам -> получаем bannerId",
                        List.of(GdAdPrimaryStatus.ACTIVE, GdAdPrimaryStatus.DRAFT), null,
                        ACTIVE_BANNER_AGGR_DATA, true},
                {"у баннера DRAFT статус, запрос с фильтром по ACTIVE,DRAFT статусам -> получаем bannerId",
                        List.of(GdAdPrimaryStatus.ACTIVE, GdAdPrimaryStatus.DRAFT), null,
                        DRAFT_BANNER_AGGR_DATA, true},
                {"у баннера ARCHIVE статус, запрос с фильтром по ACTIVE,DRAFT статусам -> bannerId не получаем",
                        List.of(GdAdPrimaryStatus.ACTIVE, GdAdPrimaryStatus.DRAFT), null,
                        ARCHIVE_BANNER_AGGR_DATA, false},
                {"у баннера нет статуса, запрос с фильтром по ACTIVE,DRAFT статусам -> получаем bannerId",
                        List.of(GdAdPrimaryStatus.ACTIVE, GdAdPrimaryStatus.DRAFT), null,
                        null, true},
                // Без статусов в фильтре
                {"у баннера ACTIVE статус, запрос с empty фильтром по статусам -> получаем bannerId",
                        emptySet(), null, ACTIVE_BANNER_AGGR_DATA, true},
        };
    }

    @Test
    @Parameters(method = "parametersForGetByStatus")
    @TestCaseName("{0}")
    public void getAdGroups(@SuppressWarnings("unused") String testDescription,
                            Collection<GdAdPrimaryStatus> filterStatuses,
                            Boolean withArchive,
                            String aggrData,
                            boolean getBannerIdInResult) {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), SHOW_DNA_BY_DEFAULT, true);

        Map<TextBannerInfo, String> bannerInfoToAggrData = new HashMap<>();
        bannerInfoToAggrData.put(bannerInfo, aggrData);
        mockYtResult(bannerInfoToAggrData);

        Set<GdAdPrimaryStatus> filterStatusesSet =
                filterStatuses == null ? null : new HashSet<>(filterStatuses);
        ExecutionResult result = processQueryGetBanner(filterStatusesSet, withArchive);
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(getBannerIdInResult ? bannerId : null);
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    public static Object[] parametersForFeature() {
        return new Object[][]{
                {SHOW_DNA_BY_DEFAULT},
                {HIDE_OLD_SHOW_CAMPS_FOR_DNA},
                {SHOW_AGGREGATED_STATUS_OPEN_BETA},
        };
    }

    /**
     * Когда в запросе все статусы, кроме нашего
     */
    @Test
    @Parameters(method = "parametersForFeature")
    @TestCaseName("{0}")
    public void getAds_WhichDoesNotExistWithSentAggregateStatus(FeatureName featureName) {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), featureName, true);

        mockYtResult(Map.of(bannerInfo, DRAFT_BANNER_AGGR_DATA));

        Set<GdAdPrimaryStatus> filterByAggrStatus = Arrays.stream(GdAdPrimaryStatus.values())
                .filter(status -> status != GdAdPrimaryStatus.DRAFT)
                .collect(Collectors.toSet());

        ExecutionResult result = processQueryGetBanner(filterByAggrStatus, null);
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(null);
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    /**
     * Когда есть два баннера с разными статусами и запрашиваем только по одному статусу
     */
    @Test
    @Parameters(method = "parametersForFeature")
    @TestCaseName("{0}")
    public void getAds_From2BannersGetOnlyOneByStatus(FeatureName featureName) {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), featureName, true);

        TextBannerInfo activeBannerInfo = steps.bannerSteps().createActiveTextBanner(groupInfo);
        mockYtResult(Map.of(bannerInfo, DRAFT_BANNER_AGGR_DATA, activeBannerInfo, ACTIVE_BANNER_AGGR_DATA));

        ExecutionResult result = processQueryGetBanner(Set.of(GdAdPrimaryStatus.ACTIVE), null);
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(activeBannerInfo.getBannerId());
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    /**
     * Когда у группы баннера статус в модерации -> баннер тоже в модерации
     */
    @Test
    @Parameters(method = "parametersForFeature")
    @TestCaseName("{0}")
    public void getAds_GetActiveBannerWithGroupOnModerateByFilter(FeatureName featureName) {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), featureName, true);

        mockYtResult(Map.of(bannerInfo, ACTIVE_BANNER_AGGR_DATA));

        addAggrStatusesAdGroup(groupInfo.getAdGroupId(), MODERATE_ADGROUP_AGGR_DATA);

        ExecutionResult result = processQueryGetBanner(Set.of(GdAdPrimaryStatus.MODERATION), null);
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(bannerId);
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    /**
     * Когда у группы баннера статус отклонен в модерации -> баннер тоже отклонен в модерации
     */
    @Test
    @Parameters(method = "parametersForFeature")
    @TestCaseName("{0}")
    public void getAds_GetActiveBannerWithRejectedGroupByFilter(FeatureName featureName) {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), featureName, true);

        mockYtResult(Map.of(bannerInfo, ACTIVE_BANNER_AGGR_DATA));

        addAggrStatusesAdGroup(groupInfo.getAdGroupId(), REJECTED_ADGROUP_AGGR_DATA);

        ExecutionResult result = processQueryGetBanner(Set.of(GdAdPrimaryStatus.MODERATION_REJECTED), null);
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(bannerId);
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    /**
     * Когда группа баннера остановлена -> баннер тоже остановлен
     */
    @Test
    @Parameters(method = "parametersForFeature")
    @TestCaseName("{0}")
    public void getAds_GetActiveBannerWithSuspendedGroupByFilter(FeatureName featureName) {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), featureName, true);

        mockYtResult(Map.of(bannerInfo, ACTIVE_BANNER_AGGR_DATA));

        addAggrStatusesAdGroup(groupInfo.getAdGroupId(), SUSPENDED_ADGROUP_AGGR_DATA);

        ExecutionResult result = processQueryGetBanner(Set.of(GdAdPrimaryStatus.MANUALLY_SUSPENDED), null);
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectedData(bannerId);
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    private ExecutionResult processQueryGetBanner(Set<GdAdPrimaryStatus> filterStatuses,
                                                  @Nullable Boolean withArchived) {
        GdAdsContainer adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(singleton(bannerId))
                        .withCampaignIdIn(ImmutableSet.of(campaignId))
                        .withPrimaryStatusContains(filterStatuses)
                        .withArchived(withArchived))
                .withOrderBy(singletonList(new GdAdOrderBy()
                        .withField(GdAdOrderByField.ID)
                        .withOrder(Order.ASC)));
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(adsContainer));
        return processor.processQuery(null, query, null, context);
    }

    private void mockYtResult(Map<TextBannerInfo, String> bannerInfoToAggrData) {
        doAnswer(invocation -> ytBannerRowset(bannerInfoToAggrData))
                .when(gridYtSupport).selectRows(anyInt(), any(Select.class), anyBoolean());
    }

    private UnversionedRowset ytBannerRowset(Map<TextBannerInfo, String> bannerInfoToAggrData) {
        RowsetBuilder builder = rowsetBuilder();
        EntryStream.of(bannerInfoToAggrData)
                .forKeyValue((bannerInfo, aggrData) -> {
                    if (aggrData != null) {
                        addBannerAggrStatus(bannerInfo.getBannerId(), aggrData);
                    }
                    builder.add(rowBuilder()
                            .withColValue(BANNERSTABLE_DIRECT.BID.getName(), bannerInfo.getBannerId())
                            .withColValue(BANNERSTABLE_DIRECT.PID.getName(), bannerInfo.getAdGroupId())
                            .withColValue(BANNERSTABLE_DIRECT.CID.getName(), bannerInfo.getCampaignId())
                            .withColValue(BANNERSTABLE_DIRECT.BANNER_TYPE.getName(),
                                    BannersBannerType.text.name().toUpperCase())
                            .withColValue(BANNERSTABLE_DIRECT.STATUS_SHOW.getName(), "Yes")
                            .withColValue(BANNERSTABLE_DIRECT.STATUS_ACTIVE.getName(), "Yes")
                            .withColValue(BANNERSTABLE_DIRECT.STATUS_ARCH.getName(), "No")
                            .withColValue(BANNERSTABLE_DIRECT.STATUS_BS_SYNCED.getName(), "Yes"));
                });
        return builder.build();
    }

    private void addBannerAggrStatus(Long bid, String aggrData) {
        dslContextProvider.ppc(clientInfo.getShard())
                .insertInto(AGGR_STATUSES_BANNERS, AGGR_STATUSES_BANNERS.BID, AGGR_STATUSES_BANNERS.AGGR_DATA,
                        AGGR_STATUSES_BANNERS.UPDATED, AGGR_STATUSES_BANNERS.IS_OBSOLETE)
                .values(bid, aggrData, LocalDateTime.of(LocalDate.now(), LocalTime.MIN), 0L)
                .execute();
    }

    private Map<String, Object> expectedData(Long bannerId) {
        return Map.of(
                "client",
                Map.of("ads",
                        Map.of("rowset", expectedRowset(bannerId))
                )
        );
    }

    private List<Object> expectedRowset(@Nullable Long bannerId) {
        return bannerId == null ? emptyList() : singletonList(Map.of("id", bannerId));
    }

    private void addAggrStatusesAdGroup(Long pid, String aggrData) {
        dslContextProvider.ppc(groupInfo.getShard())
                .insertInto(AGGR_STATUSES_ADGROUPS, AGGR_STATUSES_ADGROUPS.PID, AGGR_STATUSES_ADGROUPS.AGGR_DATA,
                        AGGR_STATUSES_ADGROUPS.UPDATED, AGGR_STATUSES_ADGROUPS.IS_OBSOLETE)
                .values(pid, aggrData, LocalDateTime.of(LocalDate.now(), LocalTime.MIN), 0L)
                .execute();
    }
}

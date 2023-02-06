package ru.yandex.direct.grid.processing.service.banner;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
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

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.entity.banner.service.GridBannerConstants;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.GdEntityStatsFilter;
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
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.thymeleaf.util.SetUtils.singletonSet;
import static ru.yandex.direct.feature.FeatureName.ADD_WITH_TOTALS_TO_BANNER_QUERY;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultGdAdsContainer;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.TEXT;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.BANNERSTABLE_DIRECT;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class AdGraphQlServiceGetAdsTotalStatsTest {
    private final static Integer MAX_ROWS_FOR_TEST = 2;
    private final static Long SHOWS = 1L;
    private final static Long CLICKS = 353L;
    private final static Long TOTAL_CLICKS = 75557L;
    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    ads(input: %s) {\n"
            + "      totalCount,\n"
            + "      totalStatsWithoutFiltersWarn,\n"
            + "      totalStats {\n"
            + "         clicks\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    private static final GdAdOrderBy ORDER_BY_ID = new GdAdOrderBy()
            .withField(GdAdOrderByField.ID)
            .withOrder(Order.ASC);

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private YtDynamicSupport gridYtSupport;
    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    private BannerSteps bannerSteps;

    private static int maxAdRowsActual;
    private UserInfo userInfo;
    private CampaignInfo campaignInfo;
    private AdGroupInfo adGroupInfo;
    private GridGraphQLContext context;

    @BeforeClass
    public static void beforeClass() throws Exception {
        maxAdRowsActual = GridBannerConstants.getMaxBannerRows();
        GridBannerConstants.setMaxBannerRows(MAX_ROWS_FOR_TEST);
    }

    @Before
    public void before() {
        userInfo = steps.userSteps().createDefaultUser();

        steps.featureSteps().addClientFeature(userInfo.getClientId(), ADD_WITH_TOTALS_TO_BANNER_QUERY, true);

        User operator = userRepository.fetchByUids(userInfo.getShard(), singletonList(userInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        campaignInfo = steps.campaignSteps().createActiveCampaign(userInfo.getClientInfo());
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        User user = userRepository.fetchByUids(adGroupInfo.getShard(), singletonList(userInfo.getUid())).get(0);

        context = ContextHelper.buildContext(user)
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @AfterClass
    public static void afterClass() {
        GridBannerConstants.setMaxBannerRows(maxAdRowsActual);
    }

    /**
     * При запросе в БД получаем количество баннеров меньше лимита и без общей статистики
     * -> получаем итоговую статистику посчитанную в коде и без флага показа предупреждения
     */
    @Test
    public void getAdsUnderLimitWithoutTotalStats_DoNotGetTotalStatsAndWarnFlag() {
        TextBannerInfo adInfo = bannerSteps.createActiveTextBanner(adGroupInfo);

        doReturnFromYt(List.of(Pair.of(adInfo, CLICKS)));

        GdAdsContainer adsContainer = getAdsContainer(singletonSet(adInfo.getBannerId()), null);
        Map<String, Object> data = sendRequestAndGetTotalStats(adsContainer);

        checkResult(data, CLICKS, false, 1);
    }

    /**
     * При запросе в БД получаем количество баннеров равное лимиту и общую статистику, при этом список баннров
     * не фильтруется в коде -> получаем итоговую статистику из БД и без флага показа предупреждения
     */
    @Test
    public void getAdsOverLimitWithTotalStats_WithoutCodeFilter_GetDbTotalStatsAndWithoutWarnFlag() {
        List<Pair<TextBannerInfo, Long>> adAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> bannerSteps.createActiveTextBanner(adGroupInfo))
                .map(adInfo -> Pair.of(adInfo, CLICKS))
                .toList();
        Set<Long> adIds = StreamEx.of(adAndClicks).map(p -> p.getLeft().getBannerId()).toSet();

        // Add total row
        adAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(adAndClicks);

        GdAdsContainer adsContainer = getAdsContainer(adIds, null);
        Map<String, Object> data = sendRequestAndGetTotalStats(adsContainer);

        checkResult(data, TOTAL_CLICKS, false, MAX_ROWS_FOR_TEST);
    }

    /**
     * При запросе в БД получаем количество баннеров равное лимиту и общую статистику, при этом список баннеров
     * не фильтруется в коде но фильтр (по коду) присутствует
     * -> получаем итоговую статистику из БД и с флагом показа предупреждения
     */
    @Test
    public void getAdsOverLimitWithTotalStats_WithCodeFilter_GetDbTotalStatsAndWithWarnFlag() {
        List<Pair<TextBannerInfo, Long>> adAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> bannerSteps.createActiveTextBanner(adGroupInfo))
                .map(adInfo -> Pair.of(adInfo, CLICKS))
                .toList();
        Set<Long> adIds = StreamEx.of(adAndClicks).map(p -> p.getLeft().getBannerId()).toSet();

        // Add total row
        adAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(adAndClicks);

        GdAdsContainer adsContainer = getAdsContainer(adIds, false);
        Map<String, Object> data = sendRequestAndGetTotalStats(adsContainer);

        checkResult(data, TOTAL_CLICKS, true, MAX_ROWS_FOR_TEST);
    }

    /**
     * При запросе в БД получаем количество баннеров равное лимиту и общую статистику, но после фильтров
     * возвращается пустой список -> получаем итоговую статистику из БД и с флагом показа предупреждения
     */
    @Test
    public void getAdsOverLimitWithTotalStats_FilterAdsToEmpty_GetDbTotalStatsAndWithWarnFlag() {
        // create ads from different campaigns
        List<Pair<TextBannerInfo, Long>> adAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> bannerSteps.createActiveTextBanner())
                .map(adInfo -> Pair.of(adInfo, CLICKS))
                .toList();
        Set<Long> adIds = StreamEx.of(adAndClicks).map(p -> p.getLeft().getBannerId()).toSet();

        // Add total row
        adAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(adAndClicks);

        GdAdsContainer adsContainer = getAdsContainer(adIds, true);
        Map<String, Object> data = sendRequestAndGetTotalStats(adsContainer);

        checkResult(data, TOTAL_CLICKS, true, 0);
    }

    /**
     * При запросе в БД получаем количество баннеров равное лимиту и общую статистику, но затем список баннеров
     * фильтруется в коде -> получаем итоговую статистику из БД и с флагом показа предупреждения
     */
    @Test
    public void getAdsOverLimitWithTotalStats_WithCodeFilter_GetDbTotalStatsAndWithoutWarnFlag() {
        List<Pair<TextBannerInfo, Long>> adAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST - 1)
                .mapToObj(i -> bannerSteps.createActiveTextBanner(adGroupInfo))
                .map(adInfo -> Pair.of(adInfo, CLICKS))
                .toList();

        // Add banner from differ campaign
        TextBannerInfo adInfoFromDifferCampaign = bannerSteps.createActiveTextBanner();
        adAndClicks.add(Pair.of(adInfoFromDifferCampaign, CLICKS));
        Set<Long> adIds = StreamEx.of(adAndClicks).map(p -> p.getLeft().getBannerId()).toSet();

        // Add total row
        adAndClicks.add(Pair.of(null, TOTAL_CLICKS));
        doReturnFromYt(adAndClicks);

        GdAdsContainer adsContainer = getAdsContainer(adIds, null);
        Map<String, Object> data = sendRequestAndGetTotalStats(adsContainer);

        checkResult(data, TOTAL_CLICKS, true, MAX_ROWS_FOR_TEST - 1);
    }

    /**
     * При запросе в БД получаем количество баннеров равное лимиту, но без общей статистики
     * -> получаем итоговую статистику, посчитанную в коде, без флага показа предупреждения
     */
    @Test
    public void getAd_OverLimitWithFilter_WithoutTotalStats() {
        List<Pair<TextBannerInfo, Long>> adAndClicks = IntStreamEx.range(0, MAX_ROWS_FOR_TEST)
                .mapToObj(i -> bannerSteps.createActiveTextBanner(adGroupInfo))
                .map(adInfo -> Pair.of(adInfo, CLICKS))
                .toList();
        Set<Long> adIds = StreamEx.of(adAndClicks).map(p -> p.getLeft().getBannerId()).toSet();

        doReturnFromYt(adAndClicks);

        GdAdsContainer adsContainer = getAdsContainer(adIds, null);
        Map<String, Object> data = sendRequestAndGetTotalStats(adsContainer);

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
                .as("количество баннеров")
                .isEqualTo(expectTotalCount);
        soft.assertAll();
    }

    private GdAdsContainer getAdsContainer(Set<Long> adIds, Boolean withArchived) {
        return getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(adIds)
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId()))
                        .withArchived(withArchived)
                        .withStats(new GdEntityStatsFilter()
                                .withMinClicks(1L)))
                .withOrderBy(Collections.singletonList(ORDER_BY_ID));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendRequestAndGetTotalStats(GdAdsContainer adsContainer) {
        String query = String.format(QUERY_TEMPLATE, context.getSubjectUser().getLogin(),
                graphQlSerialize(adsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());

        Map<String, Object> clientData = (Map<String, Object>) ((Map<String, Object>) result.getData()).get("client");
        return (Map<String, Object>) clientData.get("ads");
    }

    private void doReturnFromYt(Collection<Pair<TextBannerInfo, Long>> adAndClicks) {
        doReturn(wrapInRowset(
                StreamEx.of(adAndClicks)
                        .map(adAndClick -> {
                                    TextBannerInfo adInfo = adAndClick.getLeft();
                                    YTreeBuilder yTreeBuilder = YTree.mapBuilder()
                                            .key(GdiEntityStats.CLICKS.name()).value(adAndClick.getRight())
                                            .key(GdiEntityStats.SHOWS.name()).value(SHOWS);
                                    if (adInfo != null) {
                                        yTreeBuilder
                                                .key(BANNERSTABLE_DIRECT.BID.getName()).value(adInfo.getBannerId())
                                                .key(BANNERSTABLE_DIRECT.CID.getName()).value(adInfo.getCampaignId())
                                                .key(BANNERSTABLE_DIRECT.PID.getName()).value(adInfo.getAdGroupId())
                                                .key(BANNERSTABLE_DIRECT.BANNER_TYPE.getName()).value(TEXT.name())
                                                .key(BANNERSTABLE_DIRECT.STATUS_SHOW.getName()).value("Yes")
                                                .key(BANNERSTABLE_DIRECT.STATUS_ACTIVE.getName()).value("Yes")
                                                .key(BANNERSTABLE_DIRECT.STATUS_ARCH.getName()).value("No")
                                                .key(BANNERSTABLE_DIRECT.STATUS_BS_SYNCED.getName()).value("Yes");
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

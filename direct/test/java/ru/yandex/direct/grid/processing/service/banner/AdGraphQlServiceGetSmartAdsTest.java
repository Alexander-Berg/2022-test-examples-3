package ru.yandex.direct.grid.processing.service.banner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerCreativeInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
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

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultGdAdsContainer;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlServiceTest.convertToGroupsRowset;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.BANNERSTABLE_DIRECT;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

/**
 * Интеграционный тест. Проверяет логику влияния статуса креатива смарт-банера на итоговый статус самого смарт-банера.
 */
@GridProcessingTest
@RunWith(Parameterized.class)
public class AdGraphQlServiceGetSmartAdsTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    ads(input: %s) {\n"
            + "      totalCount\n"
            + "      adIds\n"
            + "      cacheKey\n"
            + "      rowset {\n"
            + "        index\n"
            + "        id\n"
            + "        status{\n"
            + "          hasInactiveResources\n"
            + "          primaryStatus\n"
            + "          previousVersionShown\n"
            + "          rejectedOnModeration\n"
            + "        }  \n"
            + "        __typename\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    private static final GdAdOrderBy ORDER_BY_ID = new GdAdOrderBy()
            .withField(GdAdOrderByField.ID)
            .withOrder(Order.ASC);

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
    UserRepository userRepository;

    private BannerCreativeInfo<OldPerformanceBanner> bannerCreativeInfo;
    private GdAdsContainer adsContainer;
    private GridGraphQLContext context;

    @Parameterized.Parameter(0)
    public StatusModerate statusModerate;

    @Parameterized.Parameter(1)
    public GdAdPrimaryStatus primaryStatus;

    @Parameterized.Parameters(name = "CreativeStatusModerate = {0}")
    public static Collection testData() {
        // PrimaryStatus рассчитывается в GridBannerAggregationFieldsUtils.primaryStatus
        Object[][] data = new Object[][]{
                {StatusModerate.NEW, GdAdPrimaryStatus.DRAFT},
                {StatusModerate.YES, GdAdPrimaryStatus.ACTIVE},
                {StatusModerate.ERROR, GdAdPrimaryStatus.DRAFT},
                {StatusModerate.NO, GdAdPrimaryStatus.MODERATION_REJECTED},
                {StatusModerate.READY, GdAdPrimaryStatus.ACTIVE},
                {StatusModerate.SENDING, GdAdPrimaryStatus.ACTIVE},
                {StatusModerate.SENT, GdAdPrimaryStatus.ACTIVE},
        };
        return Arrays.asList(data);
    }


    @Before
    public void initTestData() throws Exception {
        // Manual Spring integration (because we're using Parametrized runner)
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);


        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        CampaignInfo campaignInfo = adGroupInfo.getCampaignInfo();
        bannerCreativeInfo = steps.bannerCreativeSteps().createPerformanceBannerCreative(adGroupInfo);
        Long bannerId = bannerCreativeInfo.getBannerId();

        OldBanner banner = bannerCreativeInfo.getBanner();
        doAnswer(getAnswer(singletonList(adGroupInfo), singletonList(banner)))
                .when(gridYtSupport).selectRows(anyInt(), any(Select.class), anyBoolean());

        adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(singleton(bannerId))
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId())))
                .withOrderBy(Collections.singletonList(ORDER_BY_ID));

        Long uid = adGroupInfo.getClientInfo().getUid();
        User user = userRepository.fetchByUids(adGroupInfo.getShard(), singletonList(uid)).get(0);
        context = ContextHelper.buildContext(user)
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void testService() {
        //Подготавливаем состояние
        CreativeInfo creativeInfo = bannerCreativeInfo.getCreativeInfo();
        steps.creativeSteps().setCreativeProperty(creativeInfo, Creative.STATUS_MODERATE, statusModerate);

        //Выполняем запрос
        ExecutionResult result = processQuery();
        checkErrors(result.getErrors());

        //Проверяем результат
        Object data = result.getData();
        String primaryStatus = getDataValue(data, "client/ads/rowset/0/status/primaryStatus");
        assertThat(primaryStatus).isEqualTo(this.primaryStatus.name());
    }

    private ExecutionResult processQuery() {
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(adsContainer));
        return processor.processQuery(null, query, null, context);
    }

    private static UnversionedRowset convertToBannerRowset(List<OldBanner> banners) {
        RowsetBuilder builder = rowsetBuilder();
        banners.forEach(banner -> builder.add(
                rowBuilder()
                        .withColValue(BANNERSTABLE_DIRECT.BID.getName(), banner.getId())
                        .withColValue(BANNERSTABLE_DIRECT.PID.getName(), banner.getAdGroupId())
                        .withColValue(BANNERSTABLE_DIRECT.CID.getName(), banner.getCampaignId())
                        .withColValue(BANNERSTABLE_DIRECT.BANNER_TYPE.getName(), banner.getBannerType().name())
                        .withColValue(BANNERSTABLE_DIRECT.STATUS_SHOW.getName(), "Yes")
                        .withColValue(BANNERSTABLE_DIRECT.STATUS_ACTIVE.getName(), "Yes")
                        .withColValue(BANNERSTABLE_DIRECT.STATUS_ARCH.getName(), "No")
                        .withColValue(BANNERSTABLE_DIRECT.STATUS_BS_SYNCED.getName(), "Yes")
        ));
        return builder.build();
    }

    private Answer<UnversionedRowset> getAnswer(List<AdGroupInfo> groups, List<OldBanner> banners) {
        return invocation -> {
            Select query = invocation.getArgument(1);
            if (query.toString().contains(BANNERSTABLE_DIRECT.getName())) {
                return convertToBannerRowset(banners);
            }
            return convertToGroupsRowset(groups);
        };
    }

}

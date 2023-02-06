package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;

import graphql.ExecutionResult;
import org.assertj.core.api.SoftAssertions;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerCreativeInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;
import ru.yandex.direct.grid.processing.model.banner.GdAdFilter;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderBy;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderByField;
import ru.yandex.direct.grid.processing.model.banner.GdAdsContainer;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderBy;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderByField;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultGdAdsContainer;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.schema.yt.Tables.BANNERSTABLE_DIRECT;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

/**
 * Интеграционный тест. Проверяет что в client/adGroups/rowset и client/ads/rowset/adGroup одна и та же группа
 * приходит с одинаковыми значениями.
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceCompareGroupFieldsTest {

    // при добавлении поля надо убедиться, что оно мокается для yt в #convertToGroupsRowset
    private static final String[] CHECKING_FIELDS = new String[]{"id", "__typename", "name", "status/__typename",
            "status/blGenerationStatus", "status/bsEverSynced", "status/bsEverSynced", "status/moderationStatus",
            "status/primaryStatus", "status/primaryStatusDesc"};

    private static final String AD_GROUP_FIELDS = "" +
            "        __typename\n" +
            "        id\n" +
            "        name\n" +
            "          status {\n" +
            "            __typename\n" +
            "            blGenerationStatus\n" +
            "            bsEverSynced\n" +
            "            moderationStatus\n" +
            "            primaryStatus\n" +
            "            primaryStatusDesc\n" +
            "          }\n";

    private static final String QUERY_TEMPLATE = ""
            + "{\n" +
            "  client(searchBy: {login: \"%s\"}) {\n" +
            "    adGroups(input: %s) {\n" +
            "      rowset {\n" +
            AD_GROUP_FIELDS +
            "      }\n" +
            "    }\n" +
            "    ads(input: %s){\n" +
            "      rowset {\n" +
            "        adGroup {\n" +
            AD_GROUP_FIELDS +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

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
    @Autowired
    AdGroupRepository adGroupRepository;

    private Long adGroupId;
    private Long campaignId;
    private GridGraphQLContext context;
    private String userLogin;
    private PerformanceAdGroupInfo adGroupInfo;

    @Before
    public void initTestData() {
        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        adGroupId = adGroupInfo.getAdGroupId();
        CampaignInfo campaignInfo = adGroupInfo.getCampaignInfo();
        BannerCreativeInfo<OldPerformanceBanner> bannerCreativeInfo =
                steps.bannerCreativeSteps().createPerformanceBannerCreative(adGroupInfo);
        campaignId = campaignInfo.getCampaignId();

        OldBanner banner = bannerCreativeInfo.getBanner();
        doAnswer(getAnswer(singletonList(adGroupInfo), singletonList(banner)))
                .when(gridYtSupport).selectRows(anyInt(), any(Select.class), anyBoolean());
        doAnswer(getAnswer(singletonList(adGroupInfo), singletonList(banner)))
                .when(gridYtSupport).selectRows(anyInt(), any(Select.class));

        Long uid = adGroupInfo.getClientInfo().getUid();
        User user = userRepository.fetchByUids(adGroupInfo.getShard(), singletonList(uid)).get(0);
        context = ContextHelper.buildContext(user)
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
        userLogin = user.getLogin();
    }

    @Test
    public void checkAdGroupFields() {
        //Выполняем запрос
        ExecutionResult result = sendRequest();

        //Проверяем результат
        Object data = result.getData();
        SoftAssertions.assertSoftly(soft -> {
            for (String fieldName : CHECKING_FIELDS) {
                Object adGroupFieldValue = getDataValue(data, "client/adGroups/rowset/0/" + fieldName);
                Object absFieldValue = getDataValue(data, "client/ads/rowset/0/adGroup/" + fieldName);
                soft.assertThat(adGroupFieldValue)
                        .as("Field name = \"" + fieldName + "\"")
                        .isEqualTo(absFieldValue);
            }
        });
    }

    private ExecutionResult sendRequest() {
        String query = getQuery();
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());
        return result;
    }

    private String getQuery() {
        GdLimitOffset gdLimitOffset = new GdLimitOffset()
                .withLimit(1)
                .withOffset(0);
        GdAdGroupsContainer adGroupsContainer = getDefaultGdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withCampaignIdIn(singleton(campaignId))
                        .withAdGroupIdIn(singleton(adGroupId)))
                .withLimitOffset(gdLimitOffset)
                .withOrderBy(singletonList(new GdAdGroupOrderBy()
                        .withField(GdAdGroupOrderByField.ID)
                        .withOrder(Order.ASC)));
        GdAdsContainer gdAdsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withCampaignIdIn(singleton(campaignId))
                        .withAdGroupIdIn(singleton(adGroupId)))
                .withLimitOffset(gdLimitOffset)
                .withOrderBy(singletonList(new GdAdOrderBy()
                        .withField(GdAdOrderByField.ID)
                        .withOrder(Order.ASC)));
        return String.format(QUERY_TEMPLATE, userLogin,
                GraphQlJsonUtils.graphQlSerialize(adGroupsContainer),
                GraphQlJsonUtils.graphQlSerialize(gdAdsContainer));
    }

    private UnversionedRowset convertToBannerRowset(List<OldBanner> banners) {
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

    public UnversionedRowset convertToGroupsRowset(List<AdGroupInfo> infos) {
        RowsetBuilder builder = rowsetBuilder();
        infos.forEach(info -> builder.add(
                rowBuilder()
                        .withColValue(PHRASESTABLE_DIRECT.PID.getName(), info.getAdGroupId())
                        .withColValue(PHRASESTABLE_DIRECT.CID.getName(), info.getCampaignId())
                        .withColValue(PHRASESTABLE_DIRECT.ADGROUP_TYPE.getName(),
                                info.getAdGroupType().name().toLowerCase())
                        .withColValue(PHRASESTABLE_DIRECT.GROUP_NAME.getName(),
                                info.getAdGroup().getName())
                        .withColValue(PHRASESTABLE_DIRECT.STATUS_MODERATE.getName(),
                                info.getAdGroup().getStatusModerate().name())
                        .withColValue(PHRASESTABLE_DIRECT.STATUS_POST_MODERATE.getName(),
                                info.getAdGroup().getStatusPostModerate().name())
                        .withColValue(PHRASESTABLE_DIRECT.STATUS_BS_SYNCED.getName(),
                                info.getAdGroup().getStatusBsSynced().name())
                        .withColValue(PHRASESTABLE_DIRECT.STATUS_AUTOBUDGET_SHOW.getName(),
                                info.getAdGroup().getStatusAutobudgetShow().toString().toLowerCase())
                        .withColValue(PHRASESTABLE_DIRECT.STATUS_SHOWS_FORECAST.getName(),
                                info.getAdGroup().getStatusShowsForecast().name())
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

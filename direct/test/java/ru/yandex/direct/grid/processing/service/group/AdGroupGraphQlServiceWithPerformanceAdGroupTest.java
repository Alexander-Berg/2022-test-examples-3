package ru.yandex.direct.grid.processing.service.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import io.leangen.graphql.annotations.GraphQLNonNull;
import one.util.streamex.StreamEx;
import org.jooq.Select;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerLogoStatusModerate;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.NewPerformanceMainBannerInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
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
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceMainBanners.fullPerformanceMainBanner;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.list;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

/**
 * Тест на сервис, проверяем в основном то, что базовый функционал работает.
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceWithPerformanceAdGroupTest {

    private static final String QUERY_TEMPLATE = "{\n"
            + "  client(searchBy:{id: %1$d}) {\n"
            + "    adGroups(input: %2$s) {\n"
            + "      totalCount\n"
            + "      adGroupIds\n"
            + "      rowset {\n"
            + "        id\n"
            + "        ... on GdSmartAdGroup {\n"
            + "          feedId,\n"
            + "          fieldToUseAsName,\n"
            + "          fieldToUseAsBody,\n"
            + "          logoImage {\n"
            + "            imageHash,\n"
            + "            imageSize {\n"
            + "              width,\n"
            + "              height\n"
            + "            }\n"
            + "          },\n"
            + "          trackingParams\n"
            + "        }\n"
            + "        campaign {\n"
            + "          id\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private static final String QUERY_TEMPLATE_DEFAULT_REGION_IDS = "{\n"
            + "  client(searchBy:{id: %1$d}) {\n"
            + "    adGroups(input: %2$s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        campaign {\n"
            + "          id\n"
            + "          defaultRegionIds\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private static final String QUERY_TEMPLATE_MAIN_AD = "{\n"
            + "  client(searchBy: {id: %1$d}) {\n"
            + "    adGroups(input: %2$s) {\n"
            + "      rowset {\n"
            + "        mainAd {\n"
            + "          id\n"
            + "        }\n"
            + "      }\n"
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
    private UserService userService;
    @Autowired
    private YtDynamicSupport gridYtSupport;

    @Test
    public void getGdSmartAdGroup_success() {
        //Создаём группу
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        PerformanceAdGroup adGroup = adGroupInfo.getPerformanceAdGroup();
        checkNotNull(adGroup.getFeedId(), "Feed field has to be not null.");
        checkNotNull(adGroup.getFieldToUseAsName(), "FieldToUseAsName field has to be not null.");
        checkNotNull(adGroup.getFieldToUseAsBody(), "FieldToUseAsBody field has to be not null.");
        checkNotNull(adGroup.getTrackingParams(), "TrackingParams field has to be not null.");

        BannerImageFormat bannerImageFormat = steps.bannerSteps().createLogoImageFormat(adGroupInfo.getClientInfo());
        steps.performanceMainBannerSteps().createPerformanceMainBanner(new NewPerformanceMainBannerInfo()
            .withAdGroupInfo(adGroupInfo)
            .withBanner(fullPerformanceMainBanner()
                .withLogoImageHash(bannerImageFormat.getImageHash())
                .withLogoStatusModerate(BannerLogoStatusModerate.SENT)));

        //Ожидаемый результат
        Map<String, Object> expected = map(
                "client", map(
                        "adGroups", map(
                                "totalCount", 1,
                                "adGroupIds", list(adGroupInfo.getAdGroupId()),
                                "rowset", list(
                                        map(
                                                "id", adGroupInfo.getAdGroupId(),
                                                "feedId", adGroup.getFeedId(),
                                                "fieldToUseAsName", adGroup.getFieldToUseAsName(),
                                                "fieldToUseAsBody", adGroup.getFieldToUseAsBody(),
                                                "logoImage", map(
                                                        "imageHash", bannerImageFormat.getImageHash(),
                                                        "imageSize", map(
                                                                "width", bannerImageFormat.getWidth().intValue(),
                                                                "height", bannerImageFormat.getHeight().intValue()
                                                        )
                                                ),
                                                "trackingParams", adGroup.getTrackingParams(),
                                                "campaign", map(
                                                        "id", adGroupInfo.getCampaignId()
                                                )
                                        )
                                )
                        )
                ));

        //Выполняем запрос
        Map<String, Object> data = sendRequest(adGroupInfo, QUERY_TEMPLATE);

        //Сверяем ожидания и реальность
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getDefaultRegionIds_success() {
        //Создаём группу
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        TestUtils.assumeThat(adGroupInfo.getAdGroup().getGeo(), is(TestGroups.DEFAULT_GEO));
        TestUtils.assumeThat(adGroupInfo.getCampaignInfo().getCampaign().getGeo(), nullValue());
        TestUtils.assumeThat(adGroupInfo.getClientInfo().getClient().getCountryRegionId(), is(Region.RUSSIA_REGION_ID));

        //Выполняем запрос
        Map<String, Object> data = sendRequest(adGroupInfo, QUERY_TEMPLATE_DEFAULT_REGION_IDS);

        //Сверяем ожидания и реальность
        ArrayList<Long> defaultRegionIds = getDataValue(data, "client/adGroups/rowset/0/campaign/defaultRegionIds");
        assertThat(defaultRegionIds).as("defaultRegionIds").isEqualTo(TestGroups.DEFAULT_GEO);
    }

    @Test
    public void getMainAd_isNull() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        steps.performanceMainBannerSteps().createPerformanceMainBanner(adGroupInfo);

        Map<String, Object> data = sendRequest(adGroupInfo, QUERY_TEMPLATE_MAIN_AD);

        Object mainAd = getDataValue(data, "client/adGroups/rowset/0/mainAd");
        assertThat(mainAd).isNull();
    }

    private Map<String, Object> sendRequest(PerformanceAdGroupInfo adGroupInfo, String queryTemplate) {
        String filter = getFilter(singletonList(adGroupInfo));
        String query = String.format(queryTemplate, adGroupInfo.getClientInfo().getClientId().asLong(), filter);
        GridGraphQLContext context = getGridGraphQLContext(adGroupInfo.getClientInfo().getUid());
        gridContextProvider.setGridContext(context);
        UnversionedRowset ytRowset = convertToGroupsRowset(singletonList(adGroupInfo));
        doReturn(ytRowset)
                .when(gridYtSupport).selectRows(eq(adGroupInfo.getShard()), any(Select.class), anyBoolean());
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());
        return result.getData();
    }

    private UnversionedRowset convertToGroupsRowset(List<AdGroupInfo> infos) {
        RowsetBuilder builder = rowsetBuilder();
        infos.forEach(info -> builder.add(
                rowBuilder()
                        .withColValue(PHRASESTABLE_DIRECT.PID.getName(), info.getAdGroupId())
                        .withColValue(PHRASESTABLE_DIRECT.CID.getName(), info.getCampaignId())
                        .withColValue(PHRASESTABLE_DIRECT.ADGROUP_TYPE.getName(),
                                info.getAdGroupType().name().toLowerCase())
        ));

        return builder.build();
    }

    private GridGraphQLContext getGridGraphQLContext(Long uid) {
        User user = userService.getUser(uid);
        return ContextHelper.buildContext(user)
                .withFetchedFieldsReslover(null);
    }

    private String getFilter(List<PerformanceAdGroupInfo> adGroupInfos) {
        Set<@GraphQLNonNull Long> adGroupIds = StreamEx.of(adGroupInfos)
                .map(PerformanceAdGroupInfo::getAdGroupId)
                .toSet();
        Set<@GraphQLNonNull Long> campaignIds = StreamEx.of(adGroupInfos)
                .map(PerformanceAdGroupInfo::getCampaignInfo)
                .map(CampaignInfo::getCampaignId)
                .toSet();
        GdAdGroupOrderBy orderById = new GdAdGroupOrderBy()
                .withField(GdAdGroupOrderByField.ID)
                .withOrder(Order.ASC);
        GdAdGroupsContainer adGroupsContainer = getDefaultGdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withAdGroupIdIn(adGroupIds)
                        .withCampaignIdIn(campaignIds)
                )
                .withOrderBy(singletonList(orderById));
        return graphQlSerialize(adGroupsContainer);
    }

}

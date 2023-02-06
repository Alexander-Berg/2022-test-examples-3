package ru.yandex.direct.grid.processing.service.banner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.clientphone.ClientPhoneService;
import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.ClientPhoneSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.frontdb.steps.FilterShortcutsSteps;
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
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultGdAdsContainer;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlServiceTest.convertToGroupsRowset;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.BANNERSTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

/**
 * Тест на сервис, проверяем в основном то, что базовый функционал работает.
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class AdGraphQlServiceTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    ads(input: %s) {\n"
            + "      totalCount\n"
            + "      adIds\n"
            + "      cacheKey\n"
            + "      filter {\n"
            + "        campaignIdIn\n"
            + "      }\n"
            + "      rowset {\n"
            + "        index\n"
            + "        id\n"
            + "        access {\n"
            + "          canBeDeleted\n"
            + "          canEditOrganizationPhone\n"
            + "          canBeSelfRemoderated\n"
            + "        }\n"
            + "        ... on GdTextAd {\n"
            + "          permalinkId\n"
            + "          permalinkAssignType\n"
            + "          phoneId\n"
            + "          phone {\n"
            + "            phoneId\n"
            + "            phoneType\n"
            + "            phone\n"
            + "            comment\n"
            + "            extension\n"
            + "            redirectPhone\n"
            + "          }\n"
            + "        }\n"
            + "        adGroup {\n"
            + "             name\n"
            + "             ... on GdTextAdGroupTruncated {\n"
            + "                 minusKeywords\n"
            + "             }\n"
            + "             ... on GdDynamicAdGroupTruncated {\n"
            + "                 mainDomain\n"
            + "             }\n"
            + "             campaign {\n"
            + "                 id\n"
            + "                 isRecommendationsManagementEnabled\n"
            + "                 ... on GdTextCampaignTruncated {\n"
            + "                     name\n"
            + "                 }\n"
            + "             }\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    private static final GdAdOrderBy ORDER_BY_ID = new GdAdOrderBy()
            .withField(GdAdOrderByField.ID)
            .withOrder(Order.ASC);

    private GdAdsContainer adsContainer;
    private GridGraphQLContext context;

    private CampaignInfo campaignInfo;
    private AdGroupInfo groupInfoOne;
    private TextBannerInfo bannerInfoOne;
    private AdGroupInfo groupInfoTwo;
    private TextBannerInfo bannerInfoTwo;
    private ClientPhone clientPhone;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private AdGroupSteps groupSteps;

    @Autowired
    private BannerSteps bannerSteps;

    @Autowired
    private ClientPhoneSteps clientPhoneSteps;

    @Autowired
    private YtDynamicSupport gridYtSupport;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private FilterShortcutsSteps filterShortcutsSteps;

    @Autowired
    private ClientPhoneService clientPhoneService;

    private static final Long PERMALINK_ID = nextLong();
    private static final Long ANOTHER_PERMALINK_ID = nextLong();

    @Before
    public void initTestData() {
        UserInfo userInfo = userSteps.createUser(generateNewUser());

        campaignInfo = campaignSteps.createActiveCampaign(userInfo.getClientInfo());
        groupInfoOne = groupSteps.createDefaultAdGroup(campaignInfo);
        groupInfoTwo = groupSteps.createDefaultAdGroup(campaignInfo);

        bannerInfoOne = bannerSteps.createDefaultBanner(groupInfoOne);
        bannerInfoTwo = bannerSteps.createDefaultBanner(groupInfoTwo);

        organizationRepository.addOrUpdateAndLinkOrganizations(bannerInfoOne.getShard(),
                ImmutableMap.of(
                        bannerInfoOne.getBannerId(),
                        new Organization()
                                .withClientId(bannerInfoOne.getClientId())
                                .withPermalinkId(PERMALINK_ID),
                        bannerInfoTwo.getBannerId(),
                        new Organization()
                                .withClientId(bannerInfoTwo.getClientId())
                                .withPermalinkId(ANOTHER_PERMALINK_ID)));
        clientPhone = addPhoneToBanner(userInfo.getShard(), userInfo.getClientId(), bannerInfoOne.getBannerId());

        doAnswer(getAnswer(asList(groupInfoOne, groupInfoTwo),
                asList(bannerInfoOne, bannerInfoTwo)))
                .when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        adsContainer = getDefaultGdAdsContainer()
                .withFilter(new GdAdFilter()
                        .withAdIdIn(ImmutableSet.of(bannerInfoOne.getBannerId(), bannerInfoTwo.getBannerId()))
                        .withCampaignIdIn(ImmutableSet.of(campaignInfo.getCampaignId()))
                )
                .withOrderBy(Collections.singletonList(ORDER_BY_ID));

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
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
    public void testService(boolean replaceFilterToFilterKey) {
        if (replaceFilterToFilterKey) {
            String jsonFilter = JsonUtils.toJson(adsContainer.getFilter());
            String key = filterShortcutsSteps.saveFilter(bannerInfoOne.getClientId(), jsonFilter);

            adsContainer.setFilter(null);
            adsContainer.setFilterKey(key);
        }

        ExecutionResult result = processQuery();

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();

        Map<String, Object> expected = map(
                "client",
                map("ads", map(
                        "totalCount", 2,
                        "filter", ImmutableMap.<String, Object>builder()
                                .put("campaignIdIn", List.of(campaignInfo.getCampaignId()))
                                .build(),
                        "rowset", asList(
                                map(
                                        "index", 0,
                                        "id", bannerInfoOne.getBannerId(),
                                        "access", map("canBeDeleted", false,
                                                "canEditOrganizationPhone", false,
                                                "canBeSelfRemoderated", false),
                                        "permalinkId", PERMALINK_ID,
                                        "permalinkAssignType", "MANUAL",
                                        "adGroup", getAdGroupExpectedData(groupInfoOne.getAdGroup()),
                                        "phoneId", clientPhone.getId(),
                                        "phone", map(
                                                "phoneId", clientPhone.getId(),
                                                "phoneType", clientPhone.getPhoneType().toString(),
                                                "phone", clientPhone.getPhoneNumber().getPhone(),
                                                "comment", clientPhone.getComment(),
                                                "extension", clientPhone.getPhoneNumber().getExtension(),
                                                "redirectPhone", null
                                        )
                                ),
                                map(
                                        "index", 1,
                                        "id", bannerInfoTwo.getBannerId(),
                                        "access", map("canBeDeleted", false,
                                                "canEditOrganizationPhone", false,
                                                "canBeSelfRemoderated", false),
                                        "permalinkId", ANOTHER_PERMALINK_ID,
                                        "permalinkAssignType", "MANUAL",
                                        "adGroup", getAdGroupExpectedData(groupInfoTwo.getAdGroup()),
                                        "phoneId", null,
                                        "phone", null
                                )
                        )
                        )
                )
        );

        BeanFieldPath prefix = newPath("client", "ads");
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(prefix.join("cacheKey"))
                .useMatcher(notNullValue())
                .forFields(prefix.join("adIds"))
                .useMatcher(containsInAnyOrder(bannerInfoOne.getBannerId(), bannerInfoTwo.getBannerId()));
        assertThat(data)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    private ClientPhone addPhoneToBanner(int shard, ClientId clientId, Long bannerId) {
        ClientPhone phone = clientPhoneSteps.addDefaultClientManualPhone(clientId);
        clientPhoneSteps.linkPhoneIdToBanner(shard, bannerId, phone.getId());
        return clientPhoneService.getByPhoneIds(clientId, List.of(phone.getId())).get(0);
    }

    private ExecutionResult processQuery() {
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(adsContainer));
        return processor.processQuery(null, query, null, context);
    }

    private Map<String, ?> getAdGroupExpectedData(AdGroup adGroup) {
        return ImmutableMap.<String, Object>builder()
                .put("name", adGroup.getName())
                .put("minusKeywords", adGroup.getMinusKeywords())
                .put("campaign", ImmutableMap.<String, Object>builder()
                        .put("id", adGroup.getCampaignId())
                        .put("isRecommendationsManagementEnabled", false)
                        .put("name", campaignInfo.getCampaign().getName())
                        .build())
                .build();
    }

    private static UnversionedRowset convertToBannerRowset(List<TextBannerInfo> infos) {
        RowsetBuilder builder = rowsetBuilder();
        infos.forEach(info -> builder.add(
                rowBuilder()
                        .withColValue(BANNERSTABLE_DIRECT.BID.getName(), info.getBannerId())
                        .withColValue(BANNERSTABLE_DIRECT.PID.getName(), info.getAdGroupId())
                        .withColValue(BANNERSTABLE_DIRECT.CID.getName(), info.getCampaignId())
                        .withColValue(BANNERSTABLE_DIRECT.BANNER_TYPE.getName(),
                                info.getBanner().getBannerType().name())
                        .withColValue(BANNERSTABLE_DIRECT.STATUS_SHOW.getName(), "Yes")
                        .withColValue(BANNERSTABLE_DIRECT.STATUS_ACTIVE.getName(), "Yes")
                        .withColValue(BANNERSTABLE_DIRECT.STATUS_ARCH.getName(), "No")
                        .withColValue(BANNERSTABLE_DIRECT.STATUS_BS_SYNCED.getName(), "Yes")
        ));

        return builder.build();
    }

    private Answer<UnversionedRowset> getAnswer(List<AdGroupInfo> groups, List<TextBannerInfo> banners) {
        return invocation -> {
            Select query = invocation.getArgument(1);
            if (query.toString().contains(BANNERSTABLE_DIRECT.getName())) {
                return convertToBannerRowset(banners);
            }
            return convertToGroupsRowset(groups);
        };
    }

}

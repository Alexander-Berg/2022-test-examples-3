package ru.yandex.direct.grid.processing.service.group;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import org.jooq.Select;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusCorrect;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderBy;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderByField;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.interestsRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.feature.FeatureName.SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.client.ClientGraphQlService.CLIENT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.AD_GROUPS_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_ACCEPT_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_ACCEPT_MODERATION_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_BE_SENT_TO_BS_AD_GROUPS_COUNT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_BE_SENT_TO_BS_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_BE_SENT_TO_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_BE_SENT_TO_REMODERATION_AD_GROUPS_COUNT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_COUNTRY;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(Parameterized.class)
public class AdGroupGraphQlServiceCpmPriceModerationTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @org.junit.Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final String CAN_SENT_TO_BS_AND_TO_MODERATION_AD_GROUPS_QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      features {\n"
            + "         " + CAN_BE_SENT_TO_BS_AD_GROUPS_COUNT_RESOLVER_NAME + "\n"
            + "         " + CAN_BE_SENT_TO_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME + "\n"
            + "         " + CAN_BE_SENT_TO_REMODERATION_AD_GROUPS_COUNT_RESOLVER_NAME + "\n"
            + "         " + CAN_ACCEPT_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME + "\n"
            + "      }\n"
            + "      rowset {\n"
            + "        access {\n"
            + "          " + CAN_BE_SENT_TO_BS_RESOLVER_NAME + "\n"
            + "          " + CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME + "\n"
            + "          " + CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME + "\n"
            + "          " + CAN_ACCEPT_MODERATION_RESOLVER_NAME + "\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @Parameterized.Parameter
    public Boolean campaignStatusShow;

    @Parameterized.Parameter(1)
    public PriceFlightStatusApprove campaignStatusApprove;

    @Parameterized.Parameter(2)
    public PriceFlightStatusCorrect campaignStatusCorrect;

    @Parameterized.Parameter(3)
    public OldBannerStatusModerate bannersStatusModerate;

    @Parameterized.Parameter(4)
    public Map<String, Boolean> expectedAccessForDefaultAdGroup;

    @Parameterized.Parameter(5)
    public Map<String, Boolean> expectedAccessForSpecificAdGroup;

    @Parameterized.Parameter(6)
    public Map<String, Integer> expectedFeaturesMap;

    // Отправлка в bs такая же, как и для других типов групп.
    //
    // Дефолтную группу можно отправить/переотрпавить в модерацию если кампания:
    // !statusApprove || !statusCorrect || !statusShow.
    // Специфичную группу всегда можно отправить/переотрпавить в модерацию.
    //
    // Принятие модерации запрещаем.
    @Parameterized.Parameters()
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // В группах нет баннеров - ничего отправлять на модерацию/перемодерацию нельзя.
                {Boolean.FALSE, PriceFlightStatusApprove.YES, PriceFlightStatusCorrect.YES, null,
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, false)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, false)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Integer>builder()
                                .put(CAN_BE_SENT_TO_BS_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_BE_SENT_TO_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_BE_SENT_TO_REMODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_ACCEPT_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .build()
                },

                // Баннер StatusModerate.YES - поэтому проверяем перемодерацию, а не модерацию.
                // Кампания находится в statusApprove && statusCorrect && statusShow, поэтому default группу нельзя
                // отправлять на перемодерацию.
                // Перемодерация специфической группы работает так же, как для других типов групп.
                // Отправлка в bs такая же, как и для других типов групп.
                // Принятие модерации запрещенно для всех cpm_yndx_frontpage.
                {Boolean.TRUE, PriceFlightStatusApprove.YES, PriceFlightStatusCorrect.YES, OldBannerStatusModerate.YES,
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, true)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, false)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, true)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, true)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Integer>builder()
                                .put(CAN_BE_SENT_TO_BS_AD_GROUPS_COUNT_RESOLVER_NAME, 2)
                                .put(CAN_BE_SENT_TO_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_BE_SENT_TO_REMODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 1)
                                .put(CAN_ACCEPT_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .build()
                },

                // Кампания находится в !statusShow. Перемодерация работает как для других типов групп.
                {Boolean.FALSE, PriceFlightStatusApprove.YES, PriceFlightStatusCorrect.YES, OldBannerStatusModerate.YES,
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, true)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, true)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, true)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, true)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Integer>builder()
                                .put(CAN_BE_SENT_TO_BS_AD_GROUPS_COUNT_RESOLVER_NAME, 2)
                                .put(CAN_BE_SENT_TO_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_BE_SENT_TO_REMODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 2)
                                .put(CAN_ACCEPT_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .build()
                },

                // Кампания находится в !statusApprove. Перемодерация работает как для других типов групп.
                {Boolean.TRUE, PriceFlightStatusApprove.NEW, PriceFlightStatusCorrect.YES, OldBannerStatusModerate.YES,
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, true)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, true)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, true)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, true)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Integer>builder()
                                .put(CAN_BE_SENT_TO_BS_AD_GROUPS_COUNT_RESOLVER_NAME, 2)
                                .put(CAN_BE_SENT_TO_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_BE_SENT_TO_REMODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 2)
                                .put(CAN_ACCEPT_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .build()
                },

                // Кампания находится в !statusCorrect. Перемодерация работает как для других типов групп.
                {Boolean.TRUE, PriceFlightStatusApprove.YES, PriceFlightStatusCorrect.NO, OldBannerStatusModerate.YES,
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, true)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, true)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, true)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, true)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Integer>builder()
                                .put(CAN_BE_SENT_TO_BS_AD_GROUPS_COUNT_RESOLVER_NAME, 2)
                                .put(CAN_BE_SENT_TO_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_BE_SENT_TO_REMODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 2)
                                .put(CAN_ACCEPT_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .build()
                },

                // Баннер находится в StatusModerate.NEW - поэтому проверяем модерацию, а не перемодерацию.
                // Кампания находится в statusApprove && statusCorrect && statusShow, поэтому default группу нельзя
                // отправлять на модерацию.
                // Модерация специфической группы работает так же, как для других типов групп.
                // Отправлка в bs такая же, как и для других типов групп.
                // Принятие модерации запрещенно для всех cpm_yndx_frontpage.
                {Boolean.TRUE, PriceFlightStatusApprove.YES, PriceFlightStatusCorrect.YES, OldBannerStatusModerate.NEW,
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, false)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, true)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, false)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Integer>builder()
                                .put(CAN_BE_SENT_TO_BS_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_BE_SENT_TO_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 1)
                                .put(CAN_BE_SENT_TO_REMODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_ACCEPT_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .build()
                },

                // Кампания находится в !statusShow. Модерация работает как для других типов групп.
                {Boolean.FALSE, PriceFlightStatusApprove.YES, PriceFlightStatusCorrect.YES, OldBannerStatusModerate.NEW,
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, true)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, false)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, true)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, false)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Integer>builder()
                                .put(CAN_BE_SENT_TO_BS_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_BE_SENT_TO_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 2)
                                .put(CAN_BE_SENT_TO_REMODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_ACCEPT_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .build()
                },

                // Кампания находится в !statusApprove. Модерация работает как для других типов групп.
                {Boolean.TRUE, PriceFlightStatusApprove.NO, PriceFlightStatusCorrect.YES, OldBannerStatusModerate.NEW,
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, true)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, false)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, true)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, false)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Integer>builder()
                                .put(CAN_BE_SENT_TO_BS_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_BE_SENT_TO_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 2)
                                .put(CAN_BE_SENT_TO_REMODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_ACCEPT_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .build()
                },

                // Кампания находится в !statusCorrect. Модерация работает как для других типов групп.
                {Boolean.TRUE, PriceFlightStatusApprove.YES, PriceFlightStatusCorrect.NEW, OldBannerStatusModerate.NEW,
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, true)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, false)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Boolean>builder()
                                .put(CAN_BE_SENT_TO_BS_RESOLVER_NAME, false)
                                .put(CAN_BE_SENT_TO_MODERATION_RESOLVER_NAME, true)
                                .put(CAN_BE_SENT_TO_REMODERATION_RESOLVER_NAME, false)
                                .put(CAN_ACCEPT_MODERATION_RESOLVER_NAME, false)
                                .build(),
                        ImmutableMap.<String, Integer>builder()
                                .put(CAN_BE_SENT_TO_BS_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_BE_SENT_TO_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 2)
                                .put(CAN_BE_SENT_TO_REMODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .put(CAN_ACCEPT_MODERATION_AD_GROUPS_COUNT_RESOLVER_NAME, 0)
                                .build()
                },
        });
    }

    private GdAdGroupsContainer adGroupsContainer;
    private GridGraphQLContext context;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private YtDynamicSupport gridYtSupport;

    private UserInfo userInfo;
    private ClientInfo clientInfo;
    private PricePackage pricePackage;
    private CpmPriceCampaign campaign;
    private CpmYndxFrontpageAdGroup defaultAdGroup;
    private CpmYndxFrontpageAdGroup specificAdGroup;
    private Integer shard;
    private Goal behaviorGoalForPriceSales;

    @Before
    public void initTestData() {
        clientInfo = steps.clientSteps().createDefaultClient();
        userInfo = clientInfo.getChiefUserInfo();
        shard = clientInfo.getShard();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID, true);

        behaviorGoalForPriceSales = defaultGoalByType(GoalType.BEHAVIORS);
        pricePackage = approvedPricePackage()
                .withTargetingsCustom(emptyTargetingsCustom())
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA))
                .withGeoType(REGION_TYPE_COUNTRY);
        steps.pricePackageSteps().createPricePackage(pricePackage);
    }

    @Test
    public void testCanSentToBSAndToModerationAdGroupsResolvers() {
        var cpmPriceCampaign = TestCampaigns.defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage)
                .withStatusShow(campaignStatusShow)
                .withFlightStatusApprove(campaignStatusApprove)
                .withFlightStatusCorrect(campaignStatusCorrect);
        campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, cpmPriceCampaign);
        createAdGroups();
        if (bannersStatusModerate != null) {
            createBanners(bannersStatusModerate);
        }
        userInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPPORT).getChiefUserInfo();
        context = ContextHelper.buildContext(userInfo.getUser(), clientInfo.getChiefUserInfo().getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);

        ExecutionResult result = processQuery(CAN_SENT_TO_BS_AND_TO_MODERATION_AD_GROUPS_QUERY_TEMPLATE);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();

        Map<String, Object> expected = singletonMap(CLIENT_RESOLVER_NAME,
                ImmutableMap.of(AD_GROUPS_RESOLVER_NAME, ImmutableMap.builder()
                        .put(GdAdGroupsContext.FEATURES.name(), expectedFeaturesMap)
                        .put(GdAdGroupsContext.ROWSET.name(),
                                asList(
                                        singletonMap(GdAdGroup.ACCESS.name(), expectedAccessForDefaultAdGroup),
                                        singletonMap(GdAdGroup.ACCESS.name(), expectedAccessForSpecificAdGroup)))
                        .build()
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    private void createAdGroups() {
        defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        specificAdGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign, clientInfo);

        doAnswer((Answer<UnversionedRowset>) invocation -> {
            RowsetBuilder rowsetBuilder = rowsetBuilder();
            addRowBuilder(rowsetBuilder, campaign, defaultAdGroup);
            addRowBuilder(rowsetBuilder, campaign, specificAdGroup);
            return rowsetBuilder.build();
        }).when(gridYtSupport).selectRows(eq(userInfo.getShard()), any(Select.class), anyBoolean());

        adGroupsContainer = getDefaultGdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withCampaignIdIn(ImmutableSet.of(campaign.getId()))
                        .withAdGroupIdIn(ImmutableSet.of(defaultAdGroup.getId(), specificAdGroup.getId()))
                )
                .withOrderBy(Collections.singletonList(new GdAdGroupOrderBy()
                        .withField(GdAdGroupOrderByField.ID)
                        .withOrder(Order.ASC)));
    }

    private void createBanners(OldBannerStatusModerate statusModerate) {
        CreativeInfo html5Creative = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo,
                campaign);
        OldCpmBanner defaultAdGroupBanner = activeCpmBanner(campaign.getId(), defaultAdGroup.getId(),
                html5Creative.getCreativeId())
                .withStatusModerate(statusModerate);
        OldCpmBanner specificAdGroupBanner = activeCpmBanner(campaign.getId(), specificAdGroup.getId(),
                html5Creative.getCreativeId())
                .withStatusModerate(statusModerate);
        steps.bannerSteps().createActiveCpmBannerRaw(shard, defaultAdGroupBanner, defaultAdGroup);
        steps.bannerSteps().createActiveCpmBannerRaw(shard, specificAdGroupBanner, specificAdGroup);

        createRetargeting(defaultAdGroup);
        createRetargeting(specificAdGroup);
    }

    private void createRetargeting(CpmYndxFrontpageAdGroup adGroup) {
        RetConditionInfo retConditionInfo = steps.retConditionSteps().createRetCondition(
                interestsRetCondition(clientInfo.getClientId(), List.of(behaviorGoalForPriceSales)), clientInfo);
        Retargeting retargeting =
                defaultRetargeting(campaign.getId(), adGroup.getId(), retConditionInfo.getRetConditionId())
                        .withAutobudgetPriority(null)
                        .withPriceContext(pricePackage.getPrice());
        steps.retargetingSteps().createRetargetingRaw(shard, retargeting, retConditionInfo);
    }

    private static void addRowBuilder(RowsetBuilder builder, CpmPriceCampaign cpmPriceCampaign,
                                      CpmYndxFrontpageAdGroup cpmPriceAdGroup) {
        builder.add(
                rowBuilder()
                        .withColValue(PHRASESTABLE_DIRECT.PID.getName(), cpmPriceAdGroup.getId())
                        .withColValue(PHRASESTABLE_DIRECT.CID.getName(), cpmPriceCampaign.getId())
                        .withColValue(PHRASESTABLE_DIRECT.ADGROUP_TYPE.getName(),
                                cpmPriceAdGroup.getType().name().toLowerCase()));
    }

    private ExecutionResult processQuery(String queryTemplate) {
        String query = String.format(queryTemplate, clientInfo.getLogin(),
                graphQlSerialize(adGroupsContainer));
        return processor.processQuery(null, query, null, context);
    }

}

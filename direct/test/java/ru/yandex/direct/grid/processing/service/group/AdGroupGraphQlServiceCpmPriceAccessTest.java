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
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupAccess;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderBy;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderByField;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
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
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.feature.FeatureName.SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.client.ClientGraphQlService.CLIENT_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.AD_GROUPS_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_COUNTRY;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(Parameterized.class)
public class AdGroupGraphQlServiceCpmPriceAccessTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @org.junit.Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final String CAN_SENT_TO_BS_AND_TO_MODERATION_AD_GROUPS_QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      cacheKey\n"
            + "      rowset {\n"
            + "        access {\n"
            + "          " + GdAdGroupAccess.CAN_COPY.name() + "\n"
            + "          " + GdAdGroupAccess.CAN_EDIT_REGIONS.name() + "\n"
            + "          " + GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name() + "\n"
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

    // canCopy: запрещаем.
    //
    // canEditRegions: запрещаем для дефолтных групп; разрешаем для специфичных
    //
    // canBeSentToModerationByClient: разрешаем для дефолтных групп по условию:
    //     !statusApprove || !statusCorrect || !statusShow;
    //     разрешаем для всех специфичных
    //     (и при условии что выполненны все другие условия, в частности hasDraftAds = true, т.е. баннер
    //      StatusModerate != YES)
    @Parameterized.Parameters()
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{

                // В группах нет баннеров.
                {Boolean.FALSE, PriceFlightStatusApprove.YES, PriceFlightStatusCorrect.YES, null,
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), false,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), false),
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), true,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), false)
                },

                // Кампания находится в statusApprove && statusCorrect && statusShow.
                {Boolean.TRUE, PriceFlightStatusApprove.YES, PriceFlightStatusCorrect.YES, OldBannerStatusModerate.YES,
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), false,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), false),
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), true,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), false)
                },

                // Кампания находится в !statusShow.
                {Boolean.FALSE, PriceFlightStatusApprove.YES, PriceFlightStatusCorrect.YES, OldBannerStatusModerate.YES,
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), false,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), false),
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), true,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), false)
                },

                // Кампания находится в !statusApprove.
                {Boolean.TRUE, PriceFlightStatusApprove.NEW, PriceFlightStatusCorrect.YES, OldBannerStatusModerate.YES,
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), false,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), false),
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), true,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), false)

                },

                // Кампания находится в !statusCorrect.
                {Boolean.TRUE, PriceFlightStatusApprove.YES, PriceFlightStatusCorrect.NO, OldBannerStatusModerate.YES,
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), false,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), false),
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), true,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), false)
                },

                // Баннер находится в StatusModerate.NEW
                // Кампания находится в statusApprove && statusCorrect && statusShow.
                {Boolean.TRUE, PriceFlightStatusApprove.YES, PriceFlightStatusCorrect.YES, OldBannerStatusModerate.NEW,
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), false,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), false),
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), true,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), true)
                },

                // Кампания находится в !statusShow.
                {Boolean.FALSE, PriceFlightStatusApprove.YES, PriceFlightStatusCorrect.YES, OldBannerStatusModerate.NEW,
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), false,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), true),
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), true,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), true)
                },

                // Кампания находится в !statusApprove.
                {Boolean.TRUE, PriceFlightStatusApprove.NO, PriceFlightStatusCorrect.YES, OldBannerStatusModerate.NEW,
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), false,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), true),
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), true,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), true)
                },

                // Кампания находится в !statusCorrect.
                {Boolean.TRUE, PriceFlightStatusApprove.YES, PriceFlightStatusCorrect.NEW, OldBannerStatusModerate.NEW,
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), false,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), true),
                        Map.of(GdAdGroupAccess.CAN_COPY.name(), false,
                                GdAdGroupAccess.CAN_EDIT_REGIONS.name(), true,
                                GdAdGroupAccess.CAN_BE_SENT_TO_MODERATION_BY_CLIENT.name(), true)
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

    @Before
    public void initTestData() {
        userInfo = userSteps.createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();
        shard = clientInfo.getShard();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID, true);

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
    public void testCanCopyCanEditCanBeSentToModerationByClientResolvers() {
        var cpmPriceCampaign = TestCampaigns.defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage)
                .withStatusShow(campaignStatusShow)
                .withFlightStatusApprove(campaignStatusApprove)
                .withFlightStatusCorrect(campaignStatusCorrect);
        campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, cpmPriceCampaign);
        createAdGroups();
        if (bannersStatusModerate != null) {
            createBanners(bannersStatusModerate);
        }

        // два раза, чтобы проверить, как работает на втором прогоне с закэшированной версией
        for (int i = 0; i < 2; i++) {
            context = ContextHelper.buildContext(userInfo.getUser());
            gridContextProvider.setGridContext(context);

            ExecutionResult result = processQuery(CAN_SENT_TO_BS_AND_TO_MODERATION_AD_GROUPS_QUERY_TEMPLATE);

            GraphQLUtils.logErrors(result.getErrors());

            assertThat(result.getErrors())
                    .isEmpty();
            Map<String, Object> data = result.getData();

            Map<String, Object> expected = singletonMap(CLIENT_RESOLVER_NAME,
                    ImmutableMap.of(AD_GROUPS_RESOLVER_NAME, ImmutableMap.builder()
                            .put(GdAdGroupsContext.ROWSET.name(),
                                    asList(
                                            singletonMap(GdAdGroup.ACCESS.name(), expectedAccessForDefaultAdGroup),
                                            singletonMap(GdAdGroup.ACCESS.name(), expectedAccessForSpecificAdGroup)))
                            .build()
                    )
            );

            assertThat(data)
                    .is(matchedBy(beanDiffer(expected).useCompareStrategy(allFieldsExcept(newPath(".*", "cacheKey")))));

            String cacheKey = extractCacheKey(data);
            adGroupsContainer.setCacheKey(cacheKey);
        }
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
        String query = String.format(queryTemplate, context.getOperator().getLogin(),
                graphQlSerialize(adGroupsContainer));
        return processor.processQuery(null, query, null, context);
    }

    @SuppressWarnings("unchecked")
    private static String extractCacheKey(Object data) {
        var clientData = ((Map<String, Object>) data).get("client");
        var adGroupsData = ((Map<String, Object>) clientData).get("adGroups");
        return (String) ((Map<String, Object>) adGroupsData).get("cacheKey");
    }

}

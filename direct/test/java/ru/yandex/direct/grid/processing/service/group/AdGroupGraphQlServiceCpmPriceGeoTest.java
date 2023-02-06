package ru.yandex.direct.grid.processing.service.group;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import org.jooq.Select;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDefaultAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestRegions.CRIMEA_PROVINCE;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRegions.UKRAINE;
import static ru.yandex.direct.feature.FeatureName.CPM_YNDX_FRONTPAGE_ON_GRID;
import static ru.yandex.direct.feature.FeatureName.SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.getDefaultGdAdGroupsContainer;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_COUNTRY;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceCpmPriceGeoTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    adGroups(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        regionsInfo {\n"
            + "          regionIds\n"
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
    private YtDynamicSupport gridYtSupport;

    private GridGraphQLContext context;

    @Test
    public void russianGeoTreeUsedForRussianClient() {
        var client = steps.clientSteps().createClient(defaultClient().withCountryRegionId(RUSSIA));
        useClientForRequest(client);

        var pricePackage = createPricePackageWithRussiaGeoForClient(client);
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackage);
        var adGroup = activeDefaultAdGroupForPriceSales(campaign)
                .withGeo(List.of(RUSSIA, CRIMEA_PROVINCE));
        steps.adGroupSteps().createAdGroupRaw(adGroup, client);

        var graphQlResponse = getAdGroupGraphQl(adGroup);
        var expectedResponse = Map.of(
                "client", Map.of(
                        "adGroups", Map.of(
                                "rowset", List.of(
                                        Map.of(
                                                "id", adGroup.getId(),
                                                "regionsInfo", Map.of(
                                                        "regionIds", List.of((int) RUSSIA)
                                                )
                                        )
                                )
                        )
                )
        );
        assertThat(graphQlResponse).is(matchedBy(beanDiffer(expectedResponse)));
    }

    @Test
    public void russianGeoTreeUsedForNonRussianClient() {
        var client = steps.clientSteps().createClient(defaultClient().withCountryRegionId(UKRAINE));
        useClientForRequest(client);

        var pricePackage = createPricePackageWithRussiaGeoForClient(client);
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackage);
        var adGroup = activeDefaultAdGroupForPriceSales(campaign)
                .withGeo(List.of(RUSSIA, CRIMEA_PROVINCE));
        steps.adGroupSteps().createAdGroupRaw(adGroup, client);

        var graphQlResponse = getAdGroupGraphQl(adGroup);
        var expectedResponse = Map.of(
                "client", Map.of(
                        "adGroups", Map.of(
                                "rowset", List.of(
                                        Map.of(
                                                "id", adGroup.getId(),
                                                "regionsInfo", Map.of(
                                                        "regionIds", List.of((int) RUSSIA)
                                                )
                                        )
                                )
                        )
                )
        );
        assertThat(graphQlResponse).is(matchedBy(beanDiffer(expectedResponse)));
    }

    private void useClientForRequest(ClientInfo client) {
        var operator = client.getChiefUserInfo().getUser();
        context = ContextHelper.buildContext(operator);
        gridContextProvider.setGridContext(context);
        steps.featureSteps().addClientFeature(client.getClientId(), CPM_YNDX_FRONTPAGE_ON_GRID, true);
        steps.featureSteps().addClientFeature(client.getClientId(), SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID, true);
    }

    private PricePackage createPricePackageWithRussiaGeoForClient(ClientInfo client) {
        var pricePackage = approvedPricePackage()
                .withTargetingsCustom(emptyTargetingsCustom())
                .withClients(List.of(allowedPricePackageClient(client)));
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA))
                .withGeoType(REGION_TYPE_COUNTRY);
        steps.pricePackageSteps().createPricePackage(pricePackage);
        return pricePackage;
    }

    private Map<String, Object> getAdGroupGraphQl(AdGroup adGroup) {
        doAnswer((Answer<UnversionedRowset>) invocation -> {
            RowsetBuilder builder = rowsetBuilder();
            builder.add(
                    rowBuilder()
                            .withColValue(PHRASESTABLE_DIRECT.PID.getName(), adGroup.getId())
                            .withColValue(PHRASESTABLE_DIRECT.CID.getName(), adGroup.getCampaignId())
                            .withColValue(PHRASESTABLE_DIRECT.ADGROUP_TYPE.getName(),
                                    adGroup.getType().name().toLowerCase())
            );
            return builder.build();
        }).when(gridYtSupport).selectRows(anyInt(), any(Select.class), anyBoolean());

        var adGroupsContainer = getDefaultGdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withAdGroupIdIn(ImmutableSet.of(adGroup.getId()))
                        .withCampaignIdIn(ImmutableSet.of(adGroup.getCampaignId()))
                );

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(adGroupsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        assertThat(result.getErrors()).isEmpty();
        return result.getData();
    }
}

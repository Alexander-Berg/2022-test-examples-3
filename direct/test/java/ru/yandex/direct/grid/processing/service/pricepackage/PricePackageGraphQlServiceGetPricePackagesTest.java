package ru.yandex.direct.grid.processing.service.pricepackage;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventory;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageCampaignOptions;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PricePackageInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.pricepackage.GdGetPricePackages;
import ru.yandex.direct.grid.processing.model.pricepackage.GdGetPricePackagesFilterInternal;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class PricePackageGraphQlServiceGetPricePackagesTest {

    private static final String QUERY_HANDLE = "getPricePackages";

    private static final String QUERY_TEMPLATE = "query {\n" +
            "  %s(input: %s) {\n" +
            "    rowset {\n" +
            "      id\n" +
            "      title\n" +
            "      trackerUrl\n" +
            "      price\n" +
            "      currency\n" +
            "      auctionPriority\n" +
            "      orderVolumeMin\n" +
            "      orderVolumeMax\n" +
            "      targetingsFixed { \n" +
            "        geo\n" +
            "        geoType\n" +
            "        geoExpanded\n" +
            "        viewTypes\n" +
            "        allowExpandedDesktopCreative\n" +
            "      }\n" +
            "      targetingsCustom {\n" +
            "        geo\n" +
            "        geoType\n" +
            "        geoExpanded\n" +
            "      }\n" +
            "      statusApprove\n" +
            "      lastUpdateTime\n" +
            "      dateStart\n" +
            "      dateEnd\n" +
            "      isPublic\n" +
            "      isSpecial\n" +
            "      isArchived\n" +
            "      eshow\n" +
            "      campaignAutoApprove\n" +
            "      allowBrandSafety\n" +
            "      allowDisabledPlaces\n" +
            "      allowDisabledVideoPlaces\n" +
            "      bidModifiers {\n" +
            "        bidModifierInventoryFixed\n" +
            "        bidModifierInventoryAll\n" +
            "        bidModifierPlatformFixed\n" +
            "        bidModifierPlatformAll\n" +
            "      }\n" +
            "      clients {\n" +
            "        clientId\n" +
            "        login\n" +
            "        isAllowed\n" +
            "      }\n" +
            "    }\n" +
            "    totalCount\n" +
            "  }\n" +
            "}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private User operator;

    @Before
    public void initTestData() {
        createAndAuthenticateClient(defaultClient(RbacRole.SUPERREADER));
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void getPricePackages_CheckClients() {
        ClientInfo client = steps.clientSteps().createDefaultClient();
        PricePackageInfo pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withClients(List.of(allowedPricePackageClient(client))));

        var input = new GdGetPricePackages()
                .withFilter(new GdGetPricePackagesFilterInternal()
                        .withPackageIdIn(Set.of(pricePackage.getPricePackageId())))
                .withOrderBy(List.of());
        Map<String, Object> data = (Map<String, Object>) getPricePackagesGraphQlRawData(input);
        List<Map<String, Object>> rowset = (List<Map<String, Object>>) data.get("rowset");
        List<Map<String, Object>> clients = (List<Map<String, Object>>) rowset.get(0).get("clients");

        Map<String, Object> actualClient = clients.get(0);
        Map<String, Object> expectedClient = Map.of(
                "clientId", client.getClientId().asLong(),
                "login", client.getLogin(),
                "isAllowed", true
        );
        assertThat(actualClient).isEqualTo(expectedClient);
    }

    private void createAndAuthenticateClient(Client client) {
        clientInfo = steps.clientSteps().createClient(client);
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    private Object getPricePackagesGraphQlRawData(GdGetPricePackages input) {
        String query = String.format(QUERY_TEMPLATE, QUERY_HANDLE, graphQlSerialize(input));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(QUERY_HANDLE);
        return data.get(QUERY_HANDLE);
    }

    @Test
    public void getPricePackagesCampaignOptions() {
        PricePackage pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withIsArchived(false)
                .withCampaignOptions(new PricePackageCampaignOptions().withAllowBrandSafety(true))
        ).getPricePackage();
        Long pricePackageId = pricePackage.getId();

        var input = new GdGetPricePackages()
                .withFilter(new GdGetPricePackagesFilterInternal()
                        .withPackageIdIn(Set.of(pricePackageId)))
                .withOrderBy(List.of());
        Map<String, Object> data = (Map<String, Object>) getPricePackagesGraphQlRawData(input);
        List<Map<String, Object>> rowset = (List<Map<String, Object>>) data.get("rowset");
        Boolean allowBrandSafety = (Boolean) rowset.get(0).get("allowBrandSafety");
        assertThat(allowBrandSafety).isEqualTo(defaultPricePackage().getCampaignOptions().getAllowBrandSafety());
    }

    @Test
    public void getPricePackagesAuctionPriority() {
        PricePackage pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withIsArchived(false)
                .withAuctionPriority(12345L)).getPricePackage();
        Long pricePackageId = pricePackage.getId();

        var input = new GdGetPricePackages()
                .withFilter(new GdGetPricePackagesFilterInternal()
                        .withPackageIdIn(Set.of(pricePackageId)))
                .withOrderBy(List.of());
        Map<String, Object> data = (Map<String, Object>) getPricePackagesGraphQlRawData(input);
        List<Map<String, Object>> rowset = (List<Map<String, Object>>) data.get("rowset");
        var auctionPriority = (Long) rowset.get(0).get("auctionPriority");
        assertThat(auctionPriority).isEqualTo(12345L);
    }

    @Test
    public void testBidModifiers_AddInventoryAndPlatform() {
        PricePackageInfo pricePackage = steps.pricePackageSteps().createPricePackage(
                defaultPricePackage().withBidModifiers(List.of(new BidModifierMobile()
                                .withType(BidModifierType.MOBILE_MULTIPLIER)
                                .withMobileAdjustment(new BidModifierMobileAdjustment().withOsType(OsType.ANDROID)),
                        new BidModifierDesktop()
                                .withType(BidModifierType.DESKTOP_MULTIPLIER)
                                .withDesktopAdjustment(new BidModifierDesktopAdjustment()),
                        new BidModifierInventory()
                                .withType(BidModifierType.INVENTORY_MULTIPLIER)
                                .withInventoryAdjustments(
                                        List.of(new BidModifierInventoryAdjustment()
                                                .withInventoryType(InventoryType.REWARDED)))
                )));

        var input = new GdGetPricePackages()
                .withFilter(new GdGetPricePackagesFilterInternal()
                        .withPackageIdIn(Set.of(pricePackage.getPricePackageId())))
                .withOrderBy(List.of());
        Map<String, Object> data = (Map<String, Object>) getPricePackagesGraphQlRawData(input);
        List<Map<String, Object>> rowset = (List<Map<String, Object>>) data.get("rowset");
        Map<String, Object> bidModifiers = (Map<String, Object>) rowset.get(0).get("bidModifiers");
        List bidModifierInventory = (List) bidModifiers.get("bidModifierInventoryFixed");
        assertThat(bidModifiers.get("bidModifierInventoryFixed").equals(InventoryType.REWARDED.toString()));
        assertThat(bidModifierInventory.size()).isLessThanOrEqualTo(2);
        assertThat(bidModifierInventory.containsAll(Set.of(OsType.ANDROID, OsType.IOS)));
    }

}

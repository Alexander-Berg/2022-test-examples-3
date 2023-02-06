package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.ExecutionResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.campaign.model.CampaignMeasurer;
import ru.yandex.direct.core.entity.campaign.model.CampaignMeasurerSystem;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightReasonIncorrect;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusCorrect;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightTargetingsSnapshot;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmPriceCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.feature.FeatureName.SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignsGraphQlServiceCpmPriceTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    campaigns(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "        name\n"
            + "        availableAdGroupTypes\n"
            + "        isAimingAllowed\n"
            + "        ... on GdPriceCampaign {\n"
            + "          pricePackageId\n"
            + "          pricePackage{\n"
            + "            id\n"
            + "            title\n"
            + "            price\n"
            + "            currency\n"
            + "            orderVolumeMin\n"
            + "            orderVolumeMax\n"
            + "            targetingsFixed { \n"
            + "              geo\n"
            + "              geoType\n"
            + "              geoExpanded\n"
            + "              viewTypes\n"
            + "            }\n"
            + "            targetingsCustom {\n"
            + "              geo\n"
            + "              geoType\n"
            + "              geoExpanded\n"
            + "            }\n"
            + "            dateStart\n"
            + "            dateEnd\n"
            + "            campaignAutoApprove\n"
            + "            allowDisabledPlaces\n"
            + "            allowDisabledVideoPlaces\n"
            + "          }\n"
            + "          flightTargetingsSnapshot{\n"
            + "            geoType\n"
            + "            geoExpanded\n"
            + "            viewTypes\n"
            + "            allowExpandedDesktopCreative\n"
            + "          }\n"
            + "          flightOrderVolume\n"
            + "          flightStatusApprove\n"
            + "          flightStatusCorrect\n"
            + "          flightReasonIncorrect\n"
            + "          measurers {"
            + "            measurerSystem\n"
            + "            params\n"
            + "          }\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private ClientInfo client;
    private GdCampaignsContainer campaignsContainer;
    private GridGraphQLContext context;
    private PricePackage pricePackage;
    private CpmPriceCampaign campaign;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private CampaignModifyRepository campaignModifyRepository;

    public void initTestData(Set<AdGroupType> availableAdGroupTypes) {
        client = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB));
        var operator = client.getChiefUserInfo().getUser();
        steps.featureSteps().addClientFeature(client.getClientId(), SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID, true);
        var measures = List.of(
                new CampaignMeasurer()
                        .withMeasurerSystem(CampaignMeasurerSystem.MOAT)
                        .withParams("{}"),
                new CampaignMeasurer()
                        .withMeasurerSystem(CampaignMeasurerSystem.IAS)
                        .withParams("{\"advid\":82444,\"pubid\":58348904}")
        );
        pricePackage = steps.pricePackageSteps().createPricePackage(approvedPricePackage()
                .withTitle("Price Package")
                .withAvailableAdGroupTypes(availableAdGroupTypes)
                .withIsFrontpage(availableAdGroupTypes.contains(AdGroupType.CPM_YNDX_FRONTPAGE))
                .withPrice(BigDecimal.valueOf(100.22))
                .withCurrency(CurrencyCode.RUB)
                .withTargetingsFixed(new TargetingsFixed()
                        .withGeo(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withViewTypes(List.of(ViewType.MOBILE, ViewType.DESKTOP, ViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true))
                .withTargetingsCustom(emptyTargetingsCustom())
                .withOrderVolumeMin(100L)
                .withOrderVolumeMax(1000L)
                .withClients(List.of(allowedPricePackageClient(client)))
                .withDateStart(LocalDate.of(2010, 1, 2))
                .withDateEnd(LocalDate.of(2030, 3, 4)))
                .getPricePackage();

        campaign = defaultCpmPriceCampaignWithSystemFields(client, pricePackage)
                .withName("Cpm Price Campaign")
                .withCurrency(CurrencyCode.RUB)
                .withFlightTargetingsSnapshot(new PriceFlightTargetingsSnapshot()
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withViewTypes(List.of(ViewType.MOBILE, ViewType.DESKTOP, ViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true))
                .withFlightOrderVolume(500L)
                .withFlightStatusApprove(PriceFlightStatusApprove.YES)
                .withFlightStatusCorrect(PriceFlightStatusCorrect.NO)
                .withFlightReasonIncorrect(PriceFlightReasonIncorrect.NOT_FULL)
                .withMeasurers(measures);
        var addCampaignParametersContainer = RestrictedCampaignsAddOperationContainer.create(client.getShard(),
                client.getUid(),
                client.getClientId(), client.getUid(), client.getUid());
        campaignModifyRepository.addCampaigns(dslContextProvider.ppc(client.getShard()),
                addCampaignParametersContainer, List.of(campaign));

        campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        context = ContextHelper.buildContext(operator)
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void testService() {
        initTestData(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE));
        campaignsContainer.getFilter().setCampaignIdIn(Set.of(campaign.getId()));
        campaignsContainer.getLimitOffset().withLimit(1).withOffset(0);

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        var opts = approvedPricePackage().getCampaignOptions();
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Map.of(
                "client",
                Map.of(
                        "campaigns", Map.of(
                                "rowset", Collections.singletonList(Map.ofEntries(
                                        entry("id", campaign.getId()),
                                        entry("name", "Cpm Price Campaign"),
                                        entry("availableAdGroupTypes", List.of("CPM_PRICE")),
                                        entry("isAimingAllowed", true),
                                        entry("pricePackageId", pricePackage.getId()),
                                        entry("pricePackage", Map.ofEntries(
                                                entry("id", pricePackage.getId()),
                                                entry("title", "Price Package"),
                                                entry("price", BigDecimal.valueOf(100.22)),
                                                entry("currency", "RUB"),
                                                entry("orderVolumeMin", 100L),
                                                entry("orderVolumeMax", 1000L),
                                                entry("targetingsFixed", Map.of(
                                                        "geo", List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT),
                                                        "geoType", REGION_TYPE_DISTRICT,
                                                        "geoExpanded", List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT),
                                                        "viewTypes", List.of("MOBILE", "DESKTOP", "NEW_TAB")
                                                )),
                                                entry("targetingsCustom", new HashMap<String, Object>() {{
                                                    put("geo", null);
                                                    put("geoType", null);
                                                    put("geoExpanded", null);
                                                }}),
                                                entry("dateStart", "2010-01-02"),
                                                entry("dateEnd", "2030-03-04"),
                                                entry("campaignAutoApprove",
                                                        approvedPricePackage().getCampaignAutoApprove()),
                                                entry("allowDisabledPlaces", opts.getAllowDisabledPlaces()),
                                                entry("allowDisabledVideoPlaces", opts.getAllowDisabledVideoPlaces())
                                        )),
                                        entry("flightTargetingsSnapshot", Map.of(
                                                "geoType", REGION_TYPE_DISTRICT,
                                                "geoExpanded", List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT),
                                                "viewTypes", List.of("MOBILE", "DESKTOP", "NEW_TAB"),
                                                "allowExpandedDesktopCreative", true
                                        )),
                                        entry("flightOrderVolume", 500L),
                                        entry("flightStatusApprove", "YES"),
                                        entry("flightStatusCorrect", "NO"),
                                        entry("flightReasonIncorrect", "NOT_FULL"),
                                        entry("measurers", List.of(
                                                Map.of(
                                                        "measurerSystem", "MOAT",
                                                        "params", "{}"
                                                ),
                                                Map.of(
                                                        "measurerSystem", "IAS",
                                                        "params",
                                                        "https://pixel.adsafeprotected" +
                                                                ".com/rfw/st/82444/58348904/skeleton.gif"
                                                )
                                        ))
                                )))
                )
        );
        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testCpmVideoTargetings() {
        //Если выбран только тип группы CPM_VIDEO, на фронте ожидаю
        //targetingsFixed: {viewTypes: []}
        initTestData(Set.of(AdGroupType.CPM_VIDEO));
        campaignsContainer.getFilter().setCampaignIdIn(Set.of(campaign.getId()));
        campaignsContainer.getLimitOffset().withLimit(1).withOffset(0);

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        var clientData = (Map<String, Object>) data.get("client");
        var campaignsData = (Map<String, Object>) clientData.get("campaigns");
        var rowset = (List<Map<String, Object>>) campaignsData.get("rowset");
        var pricePackage = (Map<String, Object>) rowset.get(0).get("pricePackage");
        var targetingsFixed = (Map<String, Object>) pricePackage.get("targetingsFixed");
        var targetingsCustom = (Map<String, Object>) pricePackage.get("targetingsCustom");
        List<ViewType> viewTypes = (List<ViewType>) targetingsFixed.get("viewTypes");

        assertThat(viewTypes).isEmpty();
    }
}

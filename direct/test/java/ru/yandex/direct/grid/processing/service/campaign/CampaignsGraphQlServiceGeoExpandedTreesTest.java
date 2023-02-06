package ru.yandex.direct.grid.processing.service.campaign;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_ALLOWED_CREATIVE_TYPES;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.core.testing.data.TestRegions.ASTRAKHAN_PROVINCE;
import static ru.yandex.direct.core.testing.data.TestRegions.KRASNODAR_KRAI;
import static ru.yandex.direct.core.testing.data.TestRegions.REPUBLIC_OF_ADYGEA;
import static ru.yandex.direct.core.testing.data.TestRegions.REPUBLIC_OF_KALMIKIA;
import static ru.yandex.direct.core.testing.data.TestRegions.ROSTOV_PROVINCE;
import static ru.yandex.direct.core.testing.data.TestRegions.SOUTH_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGOGRAD_PROVINCE;
import static ru.yandex.direct.feature.FeatureName.SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.regions.Region.AFRICA_REGION_ID;
import static ru.yandex.direct.regions.Region.ASIA_REGION_ID;
import static ru.yandex.direct.regions.Region.AUSTRALIA_AND_OCEANIA_REGION_ID;
import static ru.yandex.direct.regions.Region.CENTRAL_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.EUROPE_REGION_ID;
import static ru.yandex.direct.regions.Region.FAR_EASTERN_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.KIROV_OBLAST_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.NORTHWESTERN_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.NORTH_AMERICA_REGION_ID;
import static ru.yandex.direct.regions.Region.REGION_TYPE_CONTINENT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_COUNTRY;
import static ru.yandex.direct.regions.Region.REGION_TYPE_PROVINCE;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.regions.Region.SIBERIAN_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.SOUTH_AMERICA_REGION_ID;
import static ru.yandex.direct.regions.Region.SOUTH_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.VOLGA_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignsGraphQlServiceGeoExpandedTreesTest {

    private static final String QUERY_TEMPLATE =
            "{\n" +
                    "    client(searchBy: {login: \"%s\"}) {\n" +
                    "    campaigns(input: %s) {\n" +
                    "            rowset {\n" +
                    "                ... on GdPriceCampaign {\n" +
                    "                    customGeoExpanded {\n" +
                    "                      geoExpandedTrees {\n" +
                    "                        id\n" +
                    "                        inner {\n" +
                    "                          id\n" +
                    "                          inner {\n" +
                    "                            id\n" +
                    "                            inner {\n" +
                    "                              id\n" +
                    "                              name\n" +
                    "                            }\n" +
                    "                            name\n" +
                    "                          }\n" +
                    "                          name\n" +
                    "                        }\n" +
                    "                        name\n" +
                    "                      }\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";


    private static final List<Long> DEFAULT_GEO = List.of(SOUTH_DISTRICT, -KRASNODAR_KRAI, -REPUBLIC_OF_ADYGEA,
            -REPUBLIC_OF_KALMIKIA, -ROSTOV_PROVINCE);
    private static final List<Long> DEFAULT_GEO_EXPANDED = List.of(ASTRAKHAN_PROVINCE, VOLGOGRAD_PROVINCE);
    private static final Integer DEFAULT_GEO_TYPE = REGION_TYPE_PROVINCE;

    private GdCampaignsContainer campaignsContainer;
    private GridGraphQLContext context;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private PricePackageService pricePackageService;

    private CpmPriceCampaign cpmPriceCampaign;
    private UserInfo userInfo;
    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        userInfo = steps.userSteps().createDefaultUser();
        clientInfo = userInfo.getClientInfo();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), SHOW_CPM_PRICE_CAMPAIGNS_IN_GRID, true);
    }

    private void initTestData(List<Long> testGeo, Integer testGeoType, List<Long> testGeoExpanded) {
        var targetingsFixed = new TargetingsFixed()
                .withGeo(testGeo)
                .withGeoType(testGeoType)
                .withGeoExpanded(testGeoExpanded)
                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                .withAllowExpandedDesktopCreative(true);
        var pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withTargetingsFixed(targetingsFixed))
                .getPricePackage();
        cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        context = ContextHelper.buildContext(userInfo.getUser());
        gridContextProvider.setGridContext(context);
    }

    private void initVideoTestData(List<Long> fixedGeo,List<Long> customGeo) {
        var targetingsFixed = new TargetingsFixed()
                .withGeo(fixedGeo)
                .withGeoType(10)
                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                .withAllowExpandedDesktopCreative(false);
        var targetingsCustom = new TargetingsCustom()
                .withGeo(customGeo)
                .withGeoType(10);
        var pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withAllowedCreativeTemplates(DEFAULT_ALLOWED_CREATIVE_TYPES)
                .withTargetingsCustom(targetingsCustom)
                .withTargetingsFixed(targetingsFixed))
                .getPricePackage();
        cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        context = ContextHelper.buildContext(userInfo.getUser());
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void testGeoExpandedTrees_En() {
        initTestData(DEFAULT_GEO, DEFAULT_GEO_TYPE, DEFAULT_GEO_EXPANDED);
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        ExecutionResult result = testServiceAndGetResult();

        Map<String, Object> data = result.getData();
        assertThat(result.getErrors())
                .isEmpty();
        assertThat(data)
                .is(matchedBy(beanDiffer(getExpectedPayloadEn())));
    }

    @Test
    public void testGeoExpandedTrees_Ru() {
        initTestData(DEFAULT_GEO, DEFAULT_GEO_TYPE, DEFAULT_GEO_EXPANDED);
        LocaleContextHolder.setLocale(Locale.forLanguageTag("ru"));

        ExecutionResult result = testServiceAndGetResult();

        Map<String, Object> data = result.getData();
        assertThat(result.getErrors())
                .isEmpty();
        assertThat(data)
                .is(matchedBy(beanDiffer(getExpectedPayloadRu())));
    }

    @Test
    public void testGeoType10() {
        // Для прайсового видео эта ручка должна строить дерево на основе ids из geo.
        // Дерево должно быть без дочерних элементов и состоять только из тех регионов, которые есть в geo
        initVideoTestData(List.of(SAINT_PETERSBURG_REGION_ID),
                List.of(SOUTH_FEDERAL_DISTRICT_REGION_ID, SAINT_PETERSBURG_REGION_ID, KRASNODAR_KRAI));
        LocaleContextHolder.setLocale(Locale.forLanguageTag("ru"));

        ExecutionResult result = testServiceAndGetResult();

        Map<String, Object> data = result.getData();
        assertThat(result.getErrors()).isEmpty();
        assertThat(data).is(matchedBy(beanDiffer(getExpectedPayloadVideo())));
    }

    @Test
    public void testGeoGap() {
        //если в дереве разрыв, то дочерние элементы должны быть потомками ближайшего родителя
        var geo = List.of(KIROV_OBLAST_REGION_ID, RUSSIA_REGION_ID);
        initVideoTestData(geo, geo);
        LocaleContextHolder.setLocale(Locale.forLanguageTag("ru"));
        Map<String, Object> rowExpected = ImmutableMap.of(CampaignsGraphQlService.CUSTOM_GEO_EXPANDED_RESOLVER_NAME,
                Map.of("geoExpandedTrees", List.of(
                        entry(225L, "Россия", List.of(
                                entry(11070L, "Кировская область", null)
                        ))
                )));

        ExecutionResult result = testServiceAndGetResult();

        Map<String, Object> data = result.getData();
        assertThat(result.getErrors()).isEmpty();
        assertThat(data).is(matchedBy(beanDiffer(getExpectedPayload(rowExpected))));
    }

    @Test
    public void testGeoExpandedTrees_SortOrder_Continents() {
        List<Long> geo = List.of(GLOBAL_REGION_ID);
        Integer geoType = REGION_TYPE_CONTINENT;
        List<Long> geoExpanded = pricePackageService.getGeoTreeConverter().expandGeo(geo, geoType);
        initTestData(geo, geoType, geoExpanded);

        LocaleContextHolder.setLocale(Locale.forLanguageTag("ru"));
        ExecutionResult result = testServiceAndGetResult();
        assertThat(result.getErrors())
                .isEmpty();

        List<Map<String, Object>> globalTrees = getGeoExpandedTrees(result);

        checkInnerOrder(globalTrees, EUROPE_REGION_ID, ASIA_REGION_ID, AFRICA_REGION_ID,
                NORTH_AMERICA_REGION_ID, SOUTH_AMERICA_REGION_ID, AUSTRALIA_AND_OCEANIA_REGION_ID);
    }

    @Test
    public void testGeoExpandedTrees_SortOrder_RegionType2() {
        List<Long> geo = List.of(GLOBAL_REGION_ID);
        Integer geoType = 2;
        List<Long> geoExpanded = pricePackageService.getGeoTreeConverter().expandGeo(geo, geoType);
        initTestData(geo, geoType, geoExpanded);

        LocaleContextHolder.setLocale(Locale.forLanguageTag("ru"));
        ExecutionResult result = testServiceAndGetResult();
        assertThat(result.getErrors())
                .isEmpty();

        List<Map<String, Object>> regionType2Trees = getGeoExpandedTrees(result);

        checkInnerOrder(regionType2Trees,  166L);
    }

    @Test
    public void testGeoExpandedTrees_SortOrder_CountriesAndBelow() {
        List<Long> geo = List.of(GLOBAL_REGION_ID);
        Integer geoType = REGION_TYPE_COUNTRY;
        List<Long> geoExpanded = pricePackageService.getGeoTreeConverter().expandGeo(geo, geoType);
        initTestData(geo, geoType, geoExpanded);

        LocaleContextHolder.setLocale(Locale.forLanguageTag("ru"));
        ExecutionResult result = testServiceAndGetResult();
        assertThat(result.getErrors())
                .isEmpty();

        List<Map<String, Object>> contryTrees = getGeoExpandedTrees(result);
        Map<String, Object> russiaTree = findSubTree(contryTrees, RUSSIA_REGION_ID);
        Map<String, Object> centerTree = findSubTree(russiaTree, CENTRAL_FEDERAL_DISTRICT_REGION_ID);
        Map<String, Object> moscowTree = findSubTree(centerTree, MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID);
        Map<String, Object> northWesternTree = findSubTree(russiaTree, NORTHWESTERN_FEDERAL_DISTRICT_REGION_ID);

        checkInnerOrder(contryTrees, RUSSIA_REGION_ID);
        checkInnerOrder(russiaTree, CENTRAL_FEDERAL_DISTRICT_REGION_ID,
                NORTHWESTERN_FEDERAL_DISTRICT_REGION_ID,
                SOUTH_FEDERAL_DISTRICT_REGION_ID,
                VOLGA_FEDERAL_DISTRICT_REGION_ID,
                SIBERIAN_FEDERAL_DISTRICT_REGION_ID,
                FAR_EASTERN_FEDERAL_DISTRICT_REGION_ID);
        checkInnerOrder(centerTree, MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID);
        checkInnerOrder(moscowTree, MOSCOW_REGION_ID);
        checkInnerOrder(northWesternTree, SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID);
    }

    private ExecutionResult testServiceAndGetResult() {
        campaignsContainer.getFilter().setCampaignIdIn(ImmutableSet.of(cpmPriceCampaign.getId()));
        campaignsContainer.getLimitOffset().withLimit(1).withOffset(0);

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getGeoExpandedTrees(ExecutionResult result) {
        Map<String, Object> data = result.getData();
        Map<String, Object> data2 = (Map<String, Object>) data.get("client");
        Map<String, Object> data3 = (Map<String, Object>) data2.get("campaigns");
        List<Object> rowset = (List<Object>) data3.get("rowset");
        Map<String, Object> data4 = (Map<String, Object>) rowset.get(0);
        Map<String, Object> data5 = (Map<String, Object>) data4.get("customGeoExpanded");
        return (List<Map<String, Object>>) data5.get("geoExpandedTrees");
    }

    /**
     * Проверяет что для переданного дерева его дети идут в следующем порядке:
     * - сначала идут firstElements
     * - оставшиеся элементы отсортированы лексиграфически
     */
    @SuppressWarnings("unchecked")
    private static void checkInnerOrder(Map<String, Object> tree, Long... firstElements) {
        var inner = (List<Map<String, Object>>) tree.get("inner");
        checkInnerOrder(inner, firstElements);
    }

    private static void checkInnerOrder(List<Map<String, Object>> inner, Long... firstElements) {
        var iterator = inner.iterator();
        for (var expectedElementId : firstElements) {
            assertThat(expectedElementId).isEqualTo(iterator.next().get("id"));
        }

        if (!iterator.hasNext()) {
            return;
        }
        String prevousName = (String) iterator.next().get("name");
        while (iterator.hasNext()) {
            String nextName = (String) iterator.next().get("name");
            assertThat(nextName.compareTo(prevousName)).isGreaterThan(0);
            prevousName = nextName;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> findSubTree(Map<String, Object> parentTree, Long regionId) {
        return findSubTree((List<Map<String, Object>>) parentTree.get("inner"), regionId);
    }

    private static Map<String, Object> findSubTree(List<Map<String, Object>> treeList, Long regionId) {
        return treeList.stream()
                .filter(tree -> tree.get("id").equals(regionId))
                .findAny()
                .orElseThrow(
                        () -> new IllegalStateException(String.format("Tree with regionId %s not found", regionId)));
    }

    private static Map<String, Object> getExpectedPayloadEn() {
        Map<String, Object> row = ImmutableMap.of(CampaignsGraphQlService.CUSTOM_GEO_EXPANDED_RESOLVER_NAME,
                Map.of("geoExpandedTrees", List.of(
                        entry(10946L, "Astrakhan Oblast", List.of(
                                entry(99221L, "Akhtubinsk District", List.of(
                                        entry(20167L, "Ahtubinsk", null))
                                ),
                                entry(37L, "Astrahan", null)
                                )),
                        entry(10950L, "Volgograd Oblast", List.of(
                                entry(10959L, "Kamishin", null),
                                entry(10965L, "Mihaylovka", null),
                                entry(10981L, "Uryupinsk", null),
                                entry(38L, "Volgograd", null),
                                entry(10951L, "Volzhskiy", null)
                        ))
                )));

        return getExpectedPayload(row);

    }

    private static Map<String, Object> getExpectedPayloadRu() {
        Map<String, Object> row = ImmutableMap.of(CampaignsGraphQlService.CUSTOM_GEO_EXPANDED_RESOLVER_NAME,
                Map.of("geoExpandedTrees", List.of(
                        entry(10946L, "Астраханская область", List.of(
                                entry(37L, "Астрахань", null),
                                entry(99221L, "Ахтубинский район", List.of(
                                        entry(20167L, "Ахтубинск", null))
                                ))
                        ),
                        entry(10950L, "Волгоградская область", List.of(
                                entry(38L, "Волгоград", null),
                                entry(10951L, "Волжский", null),
                                entry(10959L, "Камышин", null),
                                entry(10965L, "Михайловка", null),
                                entry(10981L, "Урюпинск", null)
                        ))
                )));

        return getExpectedPayload(row);
    }

    private static Map<String, Object> getExpectedPayloadVideo() {
        Map<String, Object> row = ImmutableMap.of(CampaignsGraphQlService.CUSTOM_GEO_EXPANDED_RESOLVER_NAME,
                Map.of("geoExpandedTrees", List.of(
                        entry(26L, "Юг", List.of(
                                entry(10995L, "Краснодарский край", null)
                        )),
                        entry(2L, "Санкт-Петербург", null)
                )));

        return getExpectedPayload(row);
    }

    private static Map<String, Object> getExpectedPayload(Object row) {
        return ImmutableMap.of(
                "client", ImmutableMap.of(
                        "campaigns", ImmutableMap.of(
                                "rowset", List.of(
                                        row
                                )
                        )
                )
        );
    }

    private static Map<String, Object> entry(Long id, String name, List<Map<String, Object>> inner) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("name", name);
        result.put("inner", inner);
        return result;
    }

}

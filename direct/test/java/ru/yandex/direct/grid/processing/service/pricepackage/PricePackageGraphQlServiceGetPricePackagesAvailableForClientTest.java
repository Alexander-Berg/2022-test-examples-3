package ru.yandex.direct.grid.processing.service.pricepackage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import jdk.jfr.Description;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageClient;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PricePackageInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.pricepackage.GdGetPricePackagesAvailableForClientPayload;
import ru.yandex.direct.grid.processing.model.pricepackage.GdPricePackage;
import ru.yandex.direct.grid.processing.model.pricepackage.GdPricePackageForClient;
import ru.yandex.direct.grid.processing.model.pricepackage.GdTargetingsCustom;
import ru.yandex.direct.grid.processing.model.pricepackage.GdTargetingsFixed;
import ru.yandex.direct.grid.processing.model.pricepackage.GdViewType;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.disallowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestRegions.CENTRAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.FAR_EASTERN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.NORTHWESTERN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.NORTH_CAUCASIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.SOUTH_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.URAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class PricePackageGraphQlServiceGetPricePackagesAvailableForClientTest {

    private static final String QUERY_HANDLE = "getPricePackagesAvailableForClient";

    private static final String QUERY_TEMPLATE = "query {\n" +
            "  %s {\n" +
            "    rowset {\n" +
            "      id\n" +
            "      title\n" +
            "      price\n" +
            "      currency\n" +
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
            "      dateStart\n" +
            "      dateEnd\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final CurrencyCode defaultCurrency = CurrencyCode.RUB;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private User operator;

    @Before
    public void initTestData() {
        clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
        steps.pricePackageSteps().clearPricePackages();
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void getPricePackagesAvailableForClient() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CPM_PRICE_CAMPAIGN, true);
        var anotherClient = steps.clientSteps().createDefaultClient();

        var privatePackage = createPricePackage(defaultCurrency, false, false,
                List.of(allowedPricePackageClient(clientInfo)), StatusApprove.YES);
        var privatePackageAnotherClient = createPricePackage(defaultCurrency, false, false,
                List.of(allowedPricePackageClient(anotherClient)), StatusApprove.YES);
        var publicPackageAllowed = createPricePackage(defaultCurrency, true, false,
                List.of(disallowedPricePackageClient(anotherClient)), StatusApprove.YES);
        var publicPackageAllowedAndNoMinusClients = createPricePackage(defaultCurrency, true, false,
                List.of(), StatusApprove.YES);
        var publicPackageDisallowed = createPricePackage(defaultCurrency, true, false,
                List.of(disallowedPricePackageClient(clientInfo)), StatusApprove.YES);
        var publicPackageAnotherCurrency = createPricePackage(CurrencyCode.EUR, true, false,
                List.of(allowedPricePackageClient(clientInfo)), StatusApprove.YES);
        var publicPackageStatusApproveNew = createPricePackage(defaultCurrency, true, false,
                List.of(), StatusApprove.NEW);
        var publicPackageStatusApproveWaiting = createPricePackage(defaultCurrency, true, false,
                List.of(), StatusApprove.WAITING);
        var publicPackageStatusApproveNo = createPricePackage(defaultCurrency, true, false,
                List.of(), StatusApprove.NO);
        var privatePackageArchived = createPricePackage(defaultCurrency, false, true,
                List.of(allowedPricePackageClient(clientInfo)), StatusApprove.YES);

        var payload = getPricePackagesGraphQl();

        assertThat(payload.getRowset().size()).isEqualTo(3);
        Assert.assertThat(payload.getRowset(), containsInAnyOrder(
                getExpectedPricePackage(privatePackage),
                getExpectedPricePackage(publicPackageAllowedAndNoMinusClients),
                getExpectedPricePackage(publicPackageAllowed)));
    }

    @Test
    @Description("Если фича выключена - пакеты не возвращаются")
    public void getPricePackagesAvailableForClient_NoResultIfFeatureOff() {
        var pricePackageForCurrentClient = createPricePackage(defaultCurrency, false, false,
                List.of(allowedPricePackageClient(clientInfo)), StatusApprove.YES);
        var publicPackageAllowed = createPricePackage(defaultCurrency, true, false, List.of(), StatusApprove.YES);
        var payload = getPricePackagesGraphQl();
        assertThat(payload.getRowset().size()).isEqualTo(0);
    }

    private GdPricePackage getExpectedPricePackage(PricePackageInfo pricePackage) {
        return new GdPricePackage()
                .withId(pricePackage.getPricePackageId())
                .withTitle("Current Client Package")
                .withPrice(BigDecimal.valueOf(12.34))
                .withCurrency(defaultCurrency)
                .withOrderVolumeMin(22L)
                .withOrderVolumeMax(1000L)
                .withTargetingsFixed(new GdTargetingsFixed()
                        .withViewTypes(List.of(GdViewType.DESKTOP, GdViewType.MOBILE, GdViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true))
                .withTargetingsCustom(new GdTargetingsCustom()
                        .withGeo(List.of(RUSSIA, -VOLGA_DISTRICT, -SIBERIAN_DISTRICT, -FAR_EASTERN_DISTRICT))
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(List.of(CENTRAL_DISTRICT, NORTHWESTERN_DISTRICT, SOUTH_DISTRICT,
                                NORTH_CAUCASIAN_DISTRICT, URAL_DISTRICT)))
                .withDateStart(LocalDate.of(2030, 1, 1))
                .withDateEnd(LocalDate.of(2030, 1, 1));
    }

    private PricePackageInfo createPricePackage(CurrencyCode currency,
                                                Boolean isPublic,
                                                Boolean isArchived,
                                                List<PricePackageClient> clients,
                                                StatusApprove statusApprove) {
        return steps.pricePackageSteps().createPricePackage(new PricePackage()
                .withTitle("Current Client Package")
                .withTrackerUrl("http://ya.ru")
                .withPrice(BigDecimal.valueOf(12.34))
                .withCurrency(currency)
                .withOrderVolumeMin(22L)
                .withOrderVolumeMax(1000L)
                .withTargetingsFixed(new TargetingsFixed()
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true))
                .withTargetingsCustom(new TargetingsCustom()
                        .withGeo(List.of(RUSSIA, -VOLGA_DISTRICT, -SIBERIAN_DISTRICT, -FAR_EASTERN_DISTRICT))
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(List.of(CENTRAL_DISTRICT, NORTHWESTERN_DISTRICT, SOUTH_DISTRICT,
                                NORTH_CAUCASIAN_DISTRICT, URAL_DISTRICT)))
                .withStatusApprove(statusApprove)
                .withLastUpdateTime(LocalDateTime.parse("2019-08-09T00:11:05"))
                .withDateStart(LocalDate.of(2030, 1, 1))
                .withDateEnd(LocalDate.of(2030, 1, 1))
                .withIsPublic(isPublic)
                .withIsSpecial(false)
                .withIsCpd(false)
                .withIsFrontpage(false)
                .withIsArchived(isArchived)
                .withCampaignAutoApprove(false)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE))
                .withClients(clients)
                .withIsDraftApproveAllowed(false)
                .withCategoryId(1L)
        );
    }

    private void assertPackagesMatch(GdPricePackageForClient actual, GdPricePackageForClient expected) {
        var compareStrategy = DefaultCompareStrategies
                .allFields()
                .forFields(newPath("price")).useDiffer(new BigDecimalDiffer());
        assertThat(actual).is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }

    private GdGetPricePackagesAvailableForClientPayload getPricePackagesGraphQl() {
        String query = String.format(QUERY_TEMPLATE, QUERY_HANDLE);
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(QUERY_HANDLE);

        return GraphQlJsonUtils.convertValue(data.get(QUERY_HANDLE),
                GdGetPricePackagesAvailableForClientPayload.class);
    }

}

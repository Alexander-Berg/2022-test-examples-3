package ru.yandex.direct.grid.processing.service.pricepackage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import jdk.jfr.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageCampaignOptions;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.pricepackage.GdAvailableAdGroupType;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdAddPricePackageItem;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdAddPricePackagePayloadItem;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdAddPricePackages;
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdAddPricePackagesPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.pricepackage.service.validation.defects.PricePackageDefects.geoExpandedIsEmpty;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_RETARGETING_CONDITION;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
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
import static ru.yandex.direct.grid.processing.service.pricepackage.converter.PricePackageDataConverter.toGdAllowedCreativeTemplates;
import static ru.yandex.direct.grid.processing.service.pricepackage.converter.PricePackageDataConverter.toGdAllowedDomains;
import static ru.yandex.direct.grid.processing.service.pricepackage.converter.PricePackageDataConverter.toGdAvailableAdGroupTypes;
import static ru.yandex.direct.grid.processing.service.pricepackage.converter.PricePackageDataConverter.toGdBidModifiers;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.regions.Region.SNG_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.filterAndMapList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThan;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class PricePackageGraphQlServiceAddPricePackagesTest {

    private static final String MUTATION_HANDLE = "addPricePackages";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    addedItems {\n"
            + "      id\n"
            + "    }\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        params\n"
            + "        path\n"
            + "      }\n"
            + "      warnings {\n"
            + "        code\n"
            + "        params\n"
            + "        path\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private PricePackageRepository repository;

    @Autowired
    PricePackageDataService pricePackageDataService;

    @Autowired
    private Steps steps;

    private User operator;

    @Before
    public void initTestData() {
        ClientInfo clientInfo = steps.clientSteps().createClient(defaultClient(RbacRole.SUPER));
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
        steps.sspPlatformsSteps().addSspPlatforms(defaultPricePackage().getAllowedSsp());
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }


    @Test
    public void addPricePackages_OneValidAndOneInvalidPackages() {
        PricePackage defaultPricePackage = clientPricePackage();
        defaultPricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA, -VOLGA_DISTRICT, -SIBERIAN_DISTRICT, -FAR_EASTERN_DISTRICT))
                .withGeoType(REGION_TYPE_DISTRICT);

        GdAddPricePackageItem validPricePackage = toGdAddPricePackage(defaultPricePackage);
        GdAddPricePackageItem invalidPricePackage = toGdAddPricePackage(defaultPricePackage);
        invalidPricePackage.setOrderVolumeMax(-1L);

        List<GdAddPricePackageItem> items = asList(validPricePackage, invalidPricePackage);
        GdAddPricePackagesPayload payload = addPricePackagesGraphQl(items);

        List<Long> expectedGeoExpanded = List.of(NORTHWESTERN_DISTRICT, CENTRAL_DISTRICT, URAL_DISTRICT, SOUTH_DISTRICT,
                NORTH_CAUCASIAN_DISTRICT);
        PricePackage expectedDefaultPricePackage = clientPricePackage()
                .withStatusApprove(StatusApprove.NEW);
        expectedDefaultPricePackage.getTargetingsFixed()
                .withGeoExpanded(expectedGeoExpanded);
        List<PricePackage> expectedPricePackages = singletonList(expectedDefaultPricePackage);

        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath("0", PricePackage.LAST_UPDATE_TIME.name()))
                .useMatcher(notNullValue())
                .forFields(newPath("0", PricePackage.PRICE.name()))
                .useDiffer(new BigDecimalDiffer());


        List<Long> pricePackagesIds =
                filterAndMapList(payload.getAddedItems(), Objects::nonNull, GdAddPricePackagePayloadItem::getId);

        Collection<PricePackage> actualPricePackages = repository
                .getPricePackages(pricePackagesIds)
                .values();

        assertThat(actualPricePackages)
                .is(matchedBy(beanDiffer(expectedPricePackages).useCompareStrategy(compareStrategy)));

        GdAddPricePackagesPayload expectedPayload = new GdAddPricePackagesPayload()
                .withAddedItems(asList(
                        new GdAddPricePackagePayloadItem().withId(pricePackagesIds.get(0)),
                        null))
                .withValidationResult(toGdValidationResult(
                        toGdDefect(path(field(GdAddPricePackages.ADD_ITEMS), index(1),
                                        field(GdAddPricePackageItem.ORDER_VOLUME_MAX)),
                                greaterThan(0))));
        DefaultCompareStrategy validationCompareStrategy = DefaultCompareStrategies
                .allFieldsExcept(newPath("validationResult", "errors", "0", "params"));
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload).useCompareStrategy(validationCompareStrategy)));
    }

    @Test
    @Description("После разворачивания geo пустой geoExpanded в targetingsFixed - ошибка")
    public void addPricePackages_TargetingsFixedGeoExpandedNull_Error() {
        var pricePackage = defaultPricePackage()
                .withTargetingsFixed(new TargetingsFixed()
                        // В нашем geo деревере СНГ не имеет округов, поэтому geoExpanded будет пустым
                        .withGeo(List.of(SNG_REGION_ID))
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(null)
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true)
                        .withCryptaSegments(emptyList()))
                .withTargetingsCustom(new TargetingsCustom()
                        .withRetargetingCondition(DEFAULT_RETARGETING_CONDITION));
        GdAddPricePackageItem gdAddPricePackage = toGdAddPricePackage(pricePackage);

        GdAddPricePackagesPayload payload = addPricePackagesGraphQl(List.of(gdAddPricePackage));

        GdAddPricePackagesPayload expectedPayload = new GdAddPricePackagesPayload()
                .withAddedItems(singletonList(null))
                .withValidationResult(toGdValidationResult(
                        toGdDefect(path(field(GdAddPricePackages.ADD_ITEMS), index(0),
                                        field(PricePackage.TARGETINGS_FIXED.name()),
                                        field(TargetingsFixed.GEO.name())),
                                geoExpandedIsEmpty())));
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    @Description("После разворачивания geo пустой geoExpanded в targetingsCustom - ошибка")
    public void addPricePackages_TargetingsCustomGeoExpandedNull_Error() {
        var pricePackage = defaultPricePackage()
                .withTargetingsFixed(new TargetingsFixed()
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true)
                        .withCryptaSegments(emptyList()))
                .withTargetingsCustom(new TargetingsCustom()
                        // В нашем geo деревере СНГ не имеет округов, поэтому geoExpanded будет пустым
                        .withGeo(List.of(SNG_REGION_ID))
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(null)
                        .withRetargetingCondition(DEFAULT_RETARGETING_CONDITION));
        GdAddPricePackageItem gdAddPricePackage = toGdAddPricePackage(pricePackage);

        GdAddPricePackagesPayload payload = addPricePackagesGraphQl(List.of(gdAddPricePackage));

        GdAddPricePackagesPayload expectedPayload = new GdAddPricePackagesPayload()
                .withAddedItems(singletonList(null))
                .withValidationResult(toGdValidationResult(
                        toGdDefect(path(field(GdAddPricePackages.ADD_ITEMS), index(0),
                                        field(PricePackage.TARGETINGS_CUSTOM.name()),
                                        field(TargetingsCustom.GEO.name())),
                                geoExpandedIsEmpty())));
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void cmp_video_frontpage_field() {
        //Добавляем пакет видео на Главной с доступностью картинки и галереи. Все три поля проставились
        var pricePackage = defaultPricePackage();
        pricePackage.getTargetingsFixed().withAllowExpandedDesktopCreative(false);
        GdAddPricePackageItem gdAddPricePackage = toGdAddPricePackage(pricePackage);
        gdAddPricePackage
                .withAvailableAdGroupTypes(Set.of(GdAvailableAdGroupType.CPM_PRICE_FRONTPAGE_VIDEO))
                .withAllowGallery(true)
                .withAllowImage(true);

        GdAddPricePackagesPayload payload = addPricePackagesGraphQl(List.of(gdAddPricePackage));

        PricePackage actualPricePackage = repository
                .getPricePackages(mapList(payload.getAddedItems(), GdAddPricePackagePayloadItem::getId))
                .values().stream().findAny().get();

        assertThat(actualPricePackage.getIsFrontpage()).isTrue();
        assertThat(actualPricePackage.getAvailableAdGroupTypes()).contains(AdGroupType.CPM_VIDEO);
        assertThat(actualPricePackage.getCampaignOptions().getAllowGallery()).isTrue();
        assertThat(actualPricePackage.getCampaignOptions().getAllowImage()).isTrue();
    }

    private PricePackage clientPricePackage() {
        return defaultPricePackage()
                .withTitle("Client Package")
                .withOrderVolumeMax(1000L)
                .withAuctionPriority(12345L)
                .withCampaignOptions(new PricePackageCampaignOptions())
                .withDateStart(LocalDate.of(2020, 1, 1))
                .withDateEnd(LocalDate.of(2020, 11, 1));
    }

    public GdAddPricePackageItem toGdAddPricePackage(PricePackage pricePackage) {
        TargetingsFixed targetingsFixed = pricePackage.getTargetingsFixed();
        TargetingsCustom targetingsCustom = pricePackage.getTargetingsCustom();

        return new GdAddPricePackageItem()
                .withTitle(pricePackage.getTitle())
                .withAuctionPriority(pricePackage.getAuctionPriority())
                .withTrackerUrl(pricePackage.getTrackerUrl())
                .withPrice(pricePackage.getPrice())
                .withCurrency(pricePackage.getCurrency())
                .withOrderVolumeMin(pricePackage.getOrderVolumeMin())
                .withOrderVolumeMax(pricePackage.getOrderVolumeMax())
                .withTargetingsFixed(pricePackageDataService.toGdMutationTargetingsFixed(targetingsFixed))
                .withTargetingsCustom(pricePackageDataService.toGdMutationTargetingsCustom(targetingsCustom))
                .withDateStart(pricePackage.getDateStart())
                .withDateEnd(pricePackage.getDateEnd())
                .withIsPublic(pricePackage.getIsPublic())
                .withIsSpecial(pricePackage.getIsSpecial())
                .withIsCpd(pricePackage.getIsCpd())
                .withAvailableAdGroupTypes(toGdAvailableAdGroupTypes(pricePackage))
                .withAllowedPageIds(pricePackage.getAllowedPageIds())
                .withAllowedDomains(toGdAllowedDomains(pricePackage.getAllowedSsp(), pricePackage.getAllowedDomains()))
                .withCampaignAutoApprove(pricePackage.getCampaignAutoApprove())
                .withBidModifiers(toGdBidModifiers(pricePackage.getBidModifiers()))
                .withAllowedCreativeTemplates(toGdAllowedCreativeTemplates(pricePackage.getAllowedCreativeTemplates(),
                        pricePackageDataService.creativeTemplatesSupplier()))
                .withCategoryId(pricePackage.getCategoryId());
    }

    private GdAddPricePackagesPayload addPricePackagesGraphQl(List<GdAddPricePackageItem> items) {
        GdAddPricePackages request = new GdAddPricePackages().withAddItems(items);
        String query = String.format(MUTATION_TEMPLATE, MUTATION_HANDLE, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_HANDLE);
        return GraphQlJsonUtils.convertValue(data.get(MUTATION_HANDLE), GdAddPricePackagesPayload.class);
    }
}

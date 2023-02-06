package ru.yandex.direct.grid.processing.service.pricepackage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.SortOrder;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageClient;
import ru.yandex.direct.core.entity.pricepackage.model.PriceRetargetingCondition;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestPlacements;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.GdDateRange;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;
import ru.yandex.direct.grid.processing.model.pricepackage.GdGetPricePackages;
import ru.yandex.direct.grid.processing.model.pricepackage.GdGetPricePackagesFilter;
import ru.yandex.direct.grid.processing.model.pricepackage.GdGetPricePackagesFilterInternal;
import ru.yandex.direct.grid.processing.model.pricepackage.GdGetPricePackagesPayload;
import ru.yandex.direct.grid.processing.model.pricepackage.GdPricePackage;
import ru.yandex.direct.grid.processing.model.pricepackage.GdPricePackageOrderBy;
import ru.yandex.direct.grid.processing.model.pricepackage.GdPricePackageOrderByField;
import ru.yandex.direct.grid.processing.model.pricepackage.GdStatusApprove;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.FEMALE_CRYPTA_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.MALE_CRYPTA_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.MID_INCOME_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.testPackagesGoals;
import static ru.yandex.direct.core.testing.data.TestRegions.CENTRAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.FAR_EASTERN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.KIROV_PROVINCE;
import static ru.yandex.direct.core.testing.data.TestRegions.NORTHWESTERN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.NORTH_CAUCASIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.SOUTH_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.URAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_PROVINCE;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class PricePackageGraphQlServiceGetPricePackagesFilterSortTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final String QUERY_HANDLE = "getPricePackages";

    private static final String QUERY_TEMPLATE = "query {\n" +
            "  %s(input: %s) {\n" +
            "    rowset {\n" +
            "      id\n" +
            "      title\n" +
            "      trackerUrl\n" +
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
            "        allowPremiumDesktopCreative\n" +
            "        hideIncomeSegment\n" +
            "        cryptaSegments {\n" +
            "          id\n" +
            "          type\n" +
            "        }\n" +
            "      }\n" +
            "      targetingsCustom {\n" +
            "        geo\n" +
            "        geoType\n" +
            "        geoExpanded\n" +
            "        retargetingCondition {\n" +
            "          lowerCryptaTypesCount\n" +
            "          upperCryptaTypesCount\n" +
            "          allowMetrikaSegments\n" +
            "          allowAudienceSegments\n" +
            "          cryptaSegments {\n" +
            "            id\n" +
            "            type\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "      statusApprove\n" +
            "      lastUpdateTime\n" +
            "      dateStart\n" +
            "      dateEnd\n" +
            "      isPublic\n" +
            "      isSpecial\n" +
            "      isArchived\n" +
            "      isCpd\n" +
            "      eshow\n" +
            "      campaignAutoApprove\n" +
            "      allowBrandSafety\n" +
            "      allowImage\n" +
            "      allowGallery\n" +
            "      allowBrandLift\n" +
            "      allowDisabledPlaces\n" +
            "      allowDisabledVideoPlaces\n" +
            "      showsFrequencyLimit {\n" +
            "        frequencyLimit\n" +
            "        frequencyLimitDays\n" +
            "        frequencyLimitIsForCampaignTime\n" +
            "      }\n" +
            "      allowedPageIds\n" +
            "      allowedDomains\n" +
            "      placements {\n" +
            "           domain\n" +
            "           id\n" +
            "           caption\n" +
            "           isYandexPage\n" +
            "           isDeleted\n" +
            "           isTesting\n" +
            "           mirrors\n" +
            "      }\n" +
            "      availableAdGroupTypes\n" +
            "      allowedCreativeTemplates {\n" +
            "        creativeTemplateIds\n" +
            "        creativeTemplates {\n" +
            "          id\n" +
            "          name\n" +
            "          creativeType\n" +
            "        }\n" +
            "      }\n" +
            "      bidModifiers {\n" +
            "        bidModifierInventoryFixed\n" +
            "        bidModifierInventoryAll\n" +
            "        bidModifierPlatformFixed\n" +
            "        bidModifierPlatformAll\n" +
            "      }\n" +
            "      clients {\n" +
            "        clientId\n" +
            "        isAllowed\n" +
            "      }\n" +
            "      productId\n" +
            "      priceMarkups {\n" +
            "        dateStart\n" +
            "        dateEnd\n" +
            "        percent\n" +
            "      }\n" +
            "      targetingMarkups {\n" +
            "        conditionId\n" +
            "        percent\n" +
            "      }\n" +
            "      isDraftApproveAllowed\n" +
            "      categoryId\n" +
            "    }\n" +
            "    totalCount\n" +
            "  }\n" +
            "}";

    private static final CompareStrategy PRICE_PACKAGES_COMPARE_STRATEGY = DefaultCompareStrategies
            .allFields()
            .forFields(newPath("eshow")).useDiffer(new BigDecimalDiffer())
            .forFields(newPath("price")).useDiffer(new BigDecimalDiffer());

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private PricePackageDataService pricePackageDataService;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    private UserSteps userSteps;

    private User operator;

    private List<GdPricePackage> expectedPackages;

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public GdGetPricePackagesFilter filter;

    @Parameterized.Parameter(2)
    public List<GdPricePackageOrderBy> orderBy;

    @Parameterized.Parameter(3)
    public GdLimitOffset limitOffset;

    @Parameterized.Parameter(4)
    public List<Integer> expectedPackagesIndexes;

    @Parameterized.Parameter(5)
    public Integer totalCount;

    private static final String TITLE1 = "1 Hello World, Hello everyone";
    private static final String TITLE2 = "3 Helloworld";
    private static final String TITLE3 = "2 Public package";
    private static final String TITLE4 = "4 Title";
    private static final String TITLE5 = "5 A";
    private static final String TITLE6 = "6 B";
    private static final String TITLE7 = "7 C";

    private static final BigDecimal PRICE1 = BigDecimal.valueOf(400L);
    private static final BigDecimal PRICE2 = BigDecimal.valueOf(300L);
    private static final BigDecimal PRICE3 = BigDecimal.valueOf(200L);
    private static final BigDecimal PRICE4 = BigDecimal.valueOf(100L);
    private static final BigDecimal PRICE5 = BigDecimal.valueOf(99L);
    private static final BigDecimal PRICE6 = BigDecimal.valueOf(98L);
    private static final BigDecimal PRICE7 = BigDecimal.valueOf(97L);

    private static final List<Long> DEFAULT_GEO = List.of(RUSSIA, -VOLGA_DISTRICT, -SIBERIAN_DISTRICT,
            -FAR_EASTERN_DISTRICT);
    private static final List<Long> DEFAULT_GEO_EXPANDED = List.of(NORTHWESTERN_DISTRICT, CENTRAL_DISTRICT,
            URAL_DISTRICT, SOUTH_DISTRICT, NORTH_CAUCASIAN_DISTRICT);
    private static final Integer DEFAULT_GEO_TYPE = REGION_TYPE_DISTRICT;

    private static final String TEST_LOGIN = "login7778899";
    private static final String TEST_LOGIN_2 = "login2325363";
    private static final String TEST_LOGIN_3 = "login67676";
    private static Long testClientId = null;
    private static Long testClientId2 = null;
    private static Long testClientId3 = null;

    private static final LocalDate today = LocalDate.now();

    @Before
    public void initTestData() {
        steps.placementSteps().clearPlacements();
        testCryptaSegmentRepository.clean();
        testCryptaSegmentRepository.addAll(testPackagesGoals());

        createAndAuthenticateClient(defaultClient(RbacRole.SUPERREADER));

        if (testClientId == null) {
            long uid = userSteps.generateNewUserUidSafely(TEST_LOGIN);
            var userInfo = userSteps.createUser(generateNewUser()
                    .withUid(uid)
                    .withLogin(TEST_LOGIN));
            testClientId = userInfo.getClientId().asLong();
        }

        if (testClientId2 == null) {
            long uid = userSteps.generateNewUserUidSafely(TEST_LOGIN_2);
            var userInfo = userSteps.createUser(generateNewUser()
                    .withUid(uid)
                    .withLogin(TEST_LOGIN_2));
            testClientId2 = userInfo.getClientId().asLong();
        }

        if (testClientId3 == null) {
            long uid = userSteps.generateNewUserUidSafely(TEST_LOGIN_3);
            var userInfo = userSteps.createUser(generateNewUser()
                    .withUid(uid)
                    .withLogin(TEST_LOGIN_3));
            testClientId3 = userInfo.getClientId().asLong();
        }

        steps.pricePackageSteps().clearPricePackages();
        PricePackage newPackage = steps.pricePackageSteps().createPricePackage(
                defaultPricePackage()
                        .withTargetingsFixed(new TargetingsFixed()
                                .withGeo(DEFAULT_GEO)
                                .withGeoType(DEFAULT_GEO_TYPE)
                                .withGeoExpanded(DEFAULT_GEO_EXPANDED)
                                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                                .withCryptaSegments(List.of(FEMALE_CRYPTA_GOAL_ID))
                                .withAllowExpandedDesktopCreative(true)
                                .withAllowPremiumDesktopCreative(false)
                                .withHideIncomeSegment(false))
                        .withTitle(TITLE1)
                        .withPrice(PRICE1)
                        .withStatusApprove(StatusApprove.NEW)
                        .withIsPublic(false)
                        .withIsArchived(false)
                        .withDateStart(today.plusYears(0).plusMonths(1))
                        .withDateEnd(today.plusYears(1).plusMonths(1))
                        .withCurrency(CurrencyCode.RUB)
        ).getPricePackage();

        PricePackage waitingPackage = steps.pricePackageSteps().createPricePackage(
                defaultPricePackage()
                        .withTargetingsFixed(new TargetingsFixed()
                                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                                .withCryptaSegments(List.of(FEMALE_CRYPTA_GOAL_ID))
                                .withAllowExpandedDesktopCreative(true)
                                .withAllowPremiumDesktopCreative(false)
                                .withHideIncomeSegment(false))
                        .withTargetingsCustom(new TargetingsCustom()
                                .withGeo(List.of(SAINT_PETERSBURG_REGION_ID))
                                .withGeoType(REGION_TYPE_DISTRICT)
                                .withGeoExpanded(List.of(SAINT_PETERSBURG_REGION_ID))
                                .withRetargetingCondition(
                                        new PriceRetargetingCondition()
                                                .withAllowAudienceSegments(true)
                                                .withAllowMetrikaSegments(false)
                                                .withLowerCryptaTypesCount(1)
                                                .withUpperCryptaTypesCount(3)
                                                .withCryptaSegments(List.of(MALE_CRYPTA_GOAL_ID, MID_INCOME_GOAL_ID))
                                ))
                        .withTitle(TITLE2)
                        .withPrice(PRICE2)
                        .withStatusApprove(StatusApprove.WAITING)
                        .withIsPublic(false)
                        .withIsArchived(false)
                        .withDateStart(today.plusYears(1).plusMonths(1))
                        .withDateEnd(today.plusYears(2).plusMonths(1))
                        .withCurrency(CurrencyCode.RUB)
        ).getPricePackage();

        // Публичный пакет с заминусованным клиентом
        PricePackage noPackage = steps.pricePackageSteps().createPricePackage(
                defaultPricePackage()
                        .withTargetingsFixed(new TargetingsFixed()
                                .withGeo(List.of(KIROV_PROVINCE))
                                .withGeoType(REGION_TYPE_PROVINCE)
                                .withGeoExpanded(List.of(KIROV_PROVINCE))
                                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                                .withCryptaSegments(List.of(FEMALE_CRYPTA_GOAL_ID))
                                .withAllowExpandedDesktopCreative(true)
                                .withAllowPremiumDesktopCreative(false)
                                .withHideIncomeSegment(false))
                        .withTitle(TITLE3)
                        .withPrice(PRICE3)
                        .withStatusApprove(StatusApprove.NO)
                        .withIsPublic(true)
                        .withIsArchived(false)
                        .withClients(List.of(
                                new PricePackageClient()
                                        .withClientId(testClientId)
                                        .withIsAllowed(false),
                                new PricePackageClient()
                                        .withClientId(testClientId2)
                                        .withIsAllowed(false)))
                        .withDateStart(today.plusYears(2).plusMonths(1))
                        .withDateEnd(today.plusYears(3).plusMonths(1))
                        .withCurrency(CurrencyCode.RUB)
        ).getPricePackage();

        // В фиксированных таргетингах заминусовали VOLGA_DISTRICT
        // А в кастомных добавили VOLGA_DISTRICT
        // Публичный пакет с валютой отличной от валюты клиента
        PricePackage yesPackageAnotherCurrency = steps.pricePackageSteps().createPricePackage(
                defaultPricePackage()
                        .withTargetingsFixed(new TargetingsFixed()
                                .withGeo(DEFAULT_GEO)
                                .withGeoType(DEFAULT_GEO_TYPE)
                                .withGeoExpanded(DEFAULT_GEO_EXPANDED)
                                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                                .withCryptaSegments(List.of(FEMALE_CRYPTA_GOAL_ID))
                                .withAllowExpandedDesktopCreative(true)
                                .withAllowPremiumDesktopCreative(false)
                                .withHideIncomeSegment(false))
                        .withTargetingsCustom(new TargetingsCustom()
                                .withGeo(List.of(VOLGA_DISTRICT))
                                .withGeoType(DEFAULT_GEO_TYPE)
                                .withGeoExpanded(List.of(VOLGA_DISTRICT))
                                .withRetargetingCondition(
                                        new PriceRetargetingCondition()
                                                .withAllowAudienceSegments(true)
                                                .withAllowMetrikaSegments(false)
                                                .withLowerCryptaTypesCount(1)
                                                .withUpperCryptaTypesCount(3)
                                                .withCryptaSegments(List.of(MALE_CRYPTA_GOAL_ID, MID_INCOME_GOAL_ID))
                                ))
                        .withTitle(TITLE4)
                        .withPrice(PRICE4)
                        .withStatusApprove(StatusApprove.YES)
                        .withIsPublic(true)
                        .withIsArchived(false)
                        .withDateStart(today.plusYears(3).plusMonths(1))
                        .withDateEnd(today.plusYears(4).plusMonths(1))
                        .withCurrency(CurrencyCode.EUR)
        ).getPricePackage();

        // Публичный пакет с валютой клиента и не заминусованный, должен быть выбран по фильтру на клиента
        PricePackage yesPackage = steps.pricePackageSteps().createPricePackage(
                defaultPricePackage()
                        .withTargetingsFixed(new TargetingsFixed()
                                .withGeo(DEFAULT_GEO)
                                .withGeoType(DEFAULT_GEO_TYPE)
                                .withGeoExpanded(DEFAULT_GEO_EXPANDED)
                                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                                .withCryptaSegments(List.of(FEMALE_CRYPTA_GOAL_ID))
                                .withAllowExpandedDesktopCreative(true)
                                .withAllowPremiumDesktopCreative(false)
                                .withHideIncomeSegment(false))
                        .withTitle(TITLE5)
                        .withPrice(PRICE5)
                        .withStatusApprove(StatusApprove.YES)
                        .withIsPublic(true)
                        .withIsArchived(false)
                        .withDateStart(today.plusYears(4).plusMonths(1))
                        .withDateEnd(today.plusYears(5).plusMonths(1))
                        .withCurrency(CurrencyCode.RUB)
        ).getPricePackage();

        PricePackage archivedPackage = steps.pricePackageSteps().createPricePackage(
                defaultPricePackage()
                        .withTargetingsFixed(new TargetingsFixed()
                                .withGeo(DEFAULT_GEO)
                                .withGeoType(DEFAULT_GEO_TYPE)
                                .withGeoExpanded(DEFAULT_GEO_EXPANDED)
                                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                                .withCryptaSegments(List.of(FEMALE_CRYPTA_GOAL_ID))
                                .withAllowExpandedDesktopCreative(true)
                                .withAllowPremiumDesktopCreative(false)
                                .withHideIncomeSegment(false))
                        .withTitle(TITLE6)
                        .withPrice(PRICE6)
                        .withStatusApprove(StatusApprove.NEW)
                        .withIsPublic(false)
                        .withIsArchived(true)
                        .withDateStart(today.plusYears(5).plusMonths(1))
                        .withDateEnd(today.plusYears(6).plusMonths(1))
                        .withCurrency(CurrencyCode.RUB)
        ).getPricePackage();

        PricePackage specialPackage = steps.pricePackageSteps().createPricePackage(
                defaultPricePackage()
                        .withTargetingsFixed(new TargetingsFixed()
                                .withGeo(DEFAULT_GEO)
                                .withGeoType(DEFAULT_GEO_TYPE)
                                .withGeoExpanded(DEFAULT_GEO_EXPANDED)
                                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                                .withCryptaSegments(List.of(FEMALE_CRYPTA_GOAL_ID))
                                .withAllowExpandedDesktopCreative(true)
                                .withAllowPremiumDesktopCreative(false)
                                .withHideIncomeSegment(false))
                        .withTitle(TITLE7)
                        .withPrice(PRICE7)
                        .withStatusApprove(StatusApprove.NEW)
                        .withIsPublic(false)
                        .withIsSpecial(true)
                        .withIsArchived(false)
                        .withDateStart(today.plusYears(5).plusMonths(1))
                        .withDateEnd(today.plusYears(6).plusMonths(1))
                        .withCurrency(CurrencyCode.RUB)
        ).getPricePackage();

        Map<Long, Placement> pagesToPlacements =
                Stream
                        .of(newPackage, waitingPackage, noPackage, yesPackageAnotherCurrency,
                                yesPackage, archivedPackage, specialPackage)
                        .flatMap(e -> e.getAllowedPageIds().stream()).distinct()
                        .collect(toMap(
                                e -> e, TestPlacements::gradusnikPlacement
                        ));

        steps.placementSteps().addPlacements(pagesToPlacements.values().toArray(new Placement[0]));

        expectedPackages = pricePackageDataService.toGdPricePackages(
                List.of(newPackage, waitingPackage, noPackage, yesPackageAnotherCurrency,
                        yesPackage, archivedPackage, specialPackage),
                pagesToPlacements
        );
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {
                        "Дефолтный запрос. Сортировка по умолчанию по id по возрастанию.",
                        new GdGetPricePackagesFilterInternal(),
                        List.of(),
                        null,
                        List.of(0, 1, 2, 3, 4, 5, 6),
                        7
                },
                {
                        "Запрос с limitOffset",
                        new GdGetPricePackagesFilterInternal(),
                        List.of(),
                        new GdLimitOffset()
                                .withLimit(1)
                                .withOffset(1),
                        List.of(1),
                        7
                },
                {
                        "Запрос с limitOffset и фильтрацией по id",
                        new GdGetPricePackagesFilterInternal()
                                // это виртуальные id - подменяются в тест кейсе на настоящие из базы
                                .withPackageIdIn(Set.of(0L, 2L)),
                        List.of(),
                        new GdLimitOffset()
                                .withLimit(1)
                                .withOffset(1),
                        List.of(2),
                        2
                },
                {
                        "Запрос с фильтром по имени",
                        new GdGetPricePackagesFilterInternal()
                                .withTitleContains("hello"),
                        List.of(),
                        null,
                        List.of(0, 1),
                        2
                },
                {
                        "Запрос с фильтром по имени, вхождение в середине",
                        new GdGetPricePackagesFilterInternal()
                                .withTitleContains(", Hello"),
                        List.of(),
                        null,
                        List.of(0),
                        1
                },
                {
                        "Запрос с пустым фильтром по id",
                        new GdGetPricePackagesFilterInternal()
                                .withPackageIdIn(Set.of()),
                        List.of(),
                        null,
                        List.of(),
                        0
                },
                {
                        "Запрос только публичных пакетов",
                        new GdGetPricePackagesFilterInternal()
                                .withIsPublic(true),
                        List.of(),
                        null,
                        List.of(2, 3, 4),
                        3
                },
                {
                        "Запрос только непубличных пакетов",
                        new GdGetPricePackagesFilterInternal()
                                .withIsPublic(false),
                        List.of(),
                        null,
                        List.of(0, 1, 5, 6),
                        4
                },
                {
                        "Запрос только специальных пакетов",
                        new GdGetPricePackagesFilterInternal()
                                .withIsSpecial(true),
                        List.of(),
                        null,
                        List.of(6),
                        1
                },
                {
                        "Запрос только не специальных пакетов",
                        new GdGetPricePackagesFilterInternal()
                                .withIsSpecial(false),
                        List.of(),
                        null,
                        List.of(0, 1, 2, 3, 4, 5),
                        6
                },
                {
                        "Запрос только неархивных пакетов",
                        new GdGetPricePackagesFilterInternal()
                                .withIsArchived(false),
                        List.of(),
                        null,
                        List.of(0, 1, 2, 3, 4, 6),
                        6
                },
                {
                        "Запрос только архивных пакетов",
                        new GdGetPricePackagesFilterInternal()
                                .withIsArchived(true),
                        List.of(),
                        null,
                        List.of(5),
                        1
                },
                {
                        "Запрос пакетов, которые действуют в Москве",
                        new GdGetPricePackagesFilterInternal()
                                .withRegionIds(Set.of(MOSCOW_REGION_ID)),
                        List.of(),
                        null,
                        List.of(0, 3, 4, 5, 6),
                        5
                },
                {
                        "Запрос пакетов, которые действуют в России",
                        new GdGetPricePackagesFilterInternal()
                                .withRegionIds(Set.of(RUSSIA_REGION_ID)),
                        List.of(),
                        null,
                        List.of(0, 1, 2, 3, 4, 5, 6),
                        7
                },
                {
                        "Запрос пакетов, которые действуют в Мире",
                        new GdGetPricePackagesFilterInternal()
                                .withRegionIds(Set.of(GLOBAL_REGION_ID)),
                        List.of(),
                        null,
                        List.of(0, 1, 2, 3, 4, 5, 6),
                        7
                },
                {
                        "Запрос пакетов, которые действуют в Кировской области",
                        new GdGetPricePackagesFilterInternal()
                                .withRegionIds(Set.of(KIROV_PROVINCE)),
                        List.of(),
                        null,
                        List.of(2, 3),
                        2
                },
                {
                        "Запрос пакетов, которые действуют в VOLGA_DISTRICT",
                        new GdGetPricePackagesFilterInternal()
                                .withRegionIds(Set.of(VOLGA_DISTRICT)),
                        List.of(),
                        null,
                        List.of(2, 3),
                        2
                },
                {
                        "Запрос пакетов, привязанных к клиенту",
                        new GdGetPricePackagesFilterInternal()
                                .withClientIn(Set.of(TEST_LOGIN)),
                        List.of(),
                        null,
                        List.of(4),
                        1
                },
                {
                        "Запрос пакетов, привязанных к клиенту, доступен публичный пакет в котором есть другие минус " +
                                "клиенты",
                        new GdGetPricePackagesFilterInternal()
                                .withClientIn(Set.of(TEST_LOGIN_3)),
                        List.of(),
                        null,
                        List.of(2, 4),
                        2
                },
                {
                        "Запрос пакетов с фильтром по датам",
                        new GdGetPricePackagesFilterInternal()
                                .withActivityIntervals(List.of(
                                new GdDateRange()
                                        .withMin(today)
                                        .withMax(today.plusYears(1).plusMonths(1)),
                                new GdDateRange()
                                        .withMin(today.plusYears(4).plusMonths(1))
                                        .withMax(today.plusYears(4).plusMonths(1).plusDays(2))
                        )),
                        List.of(),
                        null,
                        List.of(0, 1, 3, 4),
                        4
                },
                {
                        "Запрос пакетов с фильтром по датам 2",
                        new GdGetPricePackagesFilterInternal()
                                .withActivityIntervals(List.of(
                                new GdDateRange()
                                        .withMin(today))),
                        List.of(),
                        null,
                        List.of(0, 1, 2, 3, 4, 5, 6),
                        7
                },
                {
                        "Запрос пакетов с фильтром по датам 3",
                        new GdGetPricePackagesFilterInternal()
                                .withActivityIntervals(List.of(
                                new GdDateRange()
                                        .withMax(today.plusYears(1).plusMonths(1)))),
                        List.of(),
                        null,
                        List.of(0, 1),
                        2
                },
                {
                        "Запрос пакетов с фильтром по цене",
                        new GdGetPricePackagesFilterInternal()
                                .withMinPrice(BigDecimal.valueOf(200)),
                        List.of(),
                        null,
                        List.of(0, 1, 2),
                        3
                },
                {
                        "Запрос пакетов с фильтром по цене 2",
                        new GdGetPricePackagesFilterInternal()
                                .withMaxPrice(BigDecimal.valueOf(301))
                                .withMinPrice(BigDecimal.valueOf(299)),
                        List.of(),
                        null,
                        List.of(1),
                        1
                },
                {
                        "Запрос пакетов с фильтром по валюте",
                        new GdGetPricePackagesFilterInternal()
                                .withCurrencyIn(Set.of(CurrencyCode.RUB)),
                        List.of(),
                        null,
                        List.of(0, 1, 2, 4, 5, 6),
                        6
                },
                {
                        "Запрос пакетов с фильтром по statusApprove",
                        new GdGetPricePackagesFilterInternal()
                                .withStatusApproveIn(Set.of(GdStatusApprove.YES)),
                        List.of(),
                        null,
                        List.of(3, 4),
                        2
                },
                {
                        "Сортировка по имени по возрастанию",
                        new GdGetPricePackagesFilterInternal(),
                        List.of(new GdPricePackageOrderBy()
                                .withField(GdPricePackageOrderByField.TITLE)
                                .withOrder(SortOrder.ASC)),
                        null,
                        List.of(0, 2, 1, 3, 4, 5, 6),
                        7
                },
                {
                        "Сортировка по имени по убыванию",
                        new GdGetPricePackagesFilterInternal(),
                        List.of(new GdPricePackageOrderBy()
                                .withField(GdPricePackageOrderByField.TITLE)
                                .withOrder(SortOrder.DESC)),
                        null,
                        List.of(6, 5, 4, 3, 1, 2, 0),
                        7
                },
                {
                        "Сортировка по statusApprove по возрастанию",
                        new GdGetPricePackagesFilterInternal(),
                        List.of(new GdPricePackageOrderBy()
                                .withField(GdPricePackageOrderByField.STATUS_APPROVE)
                                .withOrder(SortOrder.ASC)),
                        null,
                        List.of(0, 5, 6, 1, 2, 3, 4),
                        7
                },
                {
                        "Сортировка по statusApprove по убыванию",
                        new GdGetPricePackagesFilterInternal(),
                        List.of(new GdPricePackageOrderBy()
                                .withField(GdPricePackageOrderByField.STATUS_APPROVE)
                                .withOrder(SortOrder.DESC)),
                        null,
                        List.of(3, 4, 2, 1, 0, 5, 6),
                        7
                },
                {
                        "Сортировка по цене по возрастанию",
                        new GdGetPricePackagesFilterInternal(),
                        List.of(new GdPricePackageOrderBy()
                                .withField(GdPricePackageOrderByField.PRICE)
                                .withOrder(SortOrder.ASC)),
                        null,
                        List.of(6, 5, 4, 3, 2, 1, 0),
                        7
                },
                {
                        "Сортировка по цене по убыванию",
                        new GdGetPricePackagesFilterInternal(),
                        List.of(new GdPricePackageOrderBy()
                                .withField(GdPricePackageOrderByField.PRICE)
                                .withOrder(SortOrder.DESC)),
                        null,
                        List.of(0, 1, 2, 3, 4, 5, 6),
                        7
                },
                {
                        "Сразу все фильтры",
                        new GdGetPricePackagesFilterInternal()
                                .withTitleContains("world"),
                        List.of(new GdPricePackageOrderBy()
                                .withField(GdPricePackageOrderByField.PRICE)
                                .withOrder(SortOrder.ASC)),
                        new GdLimitOffset()
                                .withLimit(1)
                                .withOffset(1),
                        List.of(0),
                        2
                },
        });
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void getPricePackages() {
        insertActualPackageIds(filter);
        GdGetPricePackagesPayload payload = getPricePackagesGraphQl(new GdGetPricePackages()
                .withFilter(filter)
                .withOrderBy(orderBy)
                .withLimitOffset(limitOffset));
        for (int i = 0; i < expectedPackagesIndexes.size(); i++) {
            assertPackagesMatch(payload.getRowset().get(i),
                    expectedPackages.get(expectedPackagesIndexes.get(i)));
        }
        assertThat(payload.getRowset().size()).isEqualTo(expectedPackagesIndexes.size());
        assertThat(payload.getTotalCount()).isEqualTo(totalCount);
    }

    private void insertActualPackageIds(GdGetPricePackagesFilter filter) {
        if (filter.getPackageIdIn() != null) {
            Set<Long> dbPricePackageIds = filter.getPackageIdIn().stream()
                    .map(index -> expectedPackages.get(index.intValue()))
                    .map(GdPricePackage::getId)
                    .collect(Collectors.toSet());
            filter.setPackageIdIn(dbPricePackageIds);
        }
    }

    private void createAndAuthenticateClient(Client client) {
        ClientInfo clientInfo = steps.clientSteps().createClient(client);
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    private void assertPackagesMatch(GdPricePackage actual, GdPricePackage expected) {
        assertThat(actual).is(matchedBy(beanDiffer(expected).useCompareStrategy(PRICE_PACKAGES_COMPARE_STRATEGY)));
    }

    private GdGetPricePackagesPayload getPricePackagesGraphQl(GdGetPricePackages input) {
        String query = String.format(QUERY_TEMPLATE, QUERY_HANDLE, graphQlSerialize(input));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));

        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(QUERY_HANDLE);
        GdGetPricePackagesPayload graphQlPackages = GraphQlJsonUtils.convertValue(data.get(QUERY_HANDLE),
                GdGetPricePackagesPayload.class);
        return graphQlPackages;
    }

}

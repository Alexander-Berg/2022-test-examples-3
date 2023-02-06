package ru.yandex.direct.core.entity.pricepackage.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jdk.jfr.Description;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventory;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageCampaignOptions;
import ru.yandex.direct.core.entity.pricepackage.model.PriceRetargetingCondition;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestFullGoals;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.validation.result.DefectInfo;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.creative.model.CreativeType.BANNERSTORAGE;
import static ru.yandex.direct.core.entity.creative.model.CreativeType.CPM_VIDEO_CREATIVE;
import static ru.yandex.direct.core.entity.pricepackage.service.validation.PricePackageValidator.REGION_TYPE_REGION;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_ALLOWED_CREATIVE_TYPES;
import static ru.yandex.direct.core.testing.data.TestRegions.CENTRAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.CRIMEA_PROVINCE;
import static ru.yandex.direct.core.testing.data.TestRegions.FAR_EASTERN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.KRASNODAR_KRAI;
import static ru.yandex.direct.core.testing.data.TestRegions.NORTHWESTERN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.NORTH_CAUCASIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.SOUTH_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.URAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_PROVINCE;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.regions.Region.SOUTH_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringRunner.class)
public class PricePackageAddOperationTest {
    private static final Logger logger = LoggerFactory.getLogger(PricePackageAddOperationTest.class);
    private static final CompareStrategy PRICE_PACKAGES_COMPARE_STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(newPath("id"))
            .forFields(newPath("price")).useDiffer(new BigDecimalDiffer())
            .forFields(newPath("lastUpdateTime")).useMatcher(approximatelyNow(ZoneOffset.UTC));
    private static final Map<CreativeType, List<Long>> ALLOWED_CREATIVE_TYPES = Map.of(
            BANNERSTORAGE, List.of(),
            CPM_VIDEO_CREATIVE, List.of()
    );

    @Autowired
    private PricePackageRepository repository;

    @Autowired
    private PricePackageAddOperationFactory addOperationFactory;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    private PricePackage clientPricePackage;

    @Before
    public void before() {
        var allGoals = TestFullGoals.defaultCryptaGoals();

        // не заполняем lastUpdateTime и statusApprove - их должна заполнить операция
        clientPricePackage = new PricePackage()
                .withTitle("Title_1")
                .withTrackerUrl("http://ya.ru")
                .withPrice(BigDecimal.valueOf(2999))
                .withCurrency(CurrencyCode.RUB)
                .withOrderVolumeMin(1L)
                .withOrderVolumeMax(1L)
                .withTargetingsFixed(new TargetingsFixed()
                        .withGeo(List.of(RUSSIA, -URAL_DISTRICT))
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true)
                        .withCryptaSegments(emptyList())
                        .withAllowPremiumDesktopCreative(false)
                        .withHideIncomeSegment(false))
                .withTargetingsCustom(new TargetingsCustom()
                        .withRetargetingCondition(
                                new PriceRetargetingCondition()
                                        .withAllowAudienceSegments(true)
                                        .withAllowMetrikaSegments(false)
                                        .withLowerCryptaTypesCount(0)
                                        .withUpperCryptaTypesCount(0)
                                        .withCryptaSegments(emptyList())
                        ))
                .withTargetingMarkups(emptyList())
                .withDateStart(LocalDate.of(2020, 1, 1))
                .withDateEnd(LocalDate.of(2020, 1, 1))
                .withIsPublic(false)
                .withIsSpecial(false)
                .withIsCpd(false)
                .withIsFrontpage(false)
                .withCampaignAutoApprove(false)
                .withIsDraftApproveAllowed(true)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE))
                .withClients(emptyList())
                .withCampaignOptions(new PricePackageCampaignOptions()
                    .withAllowBrandLift(false)
                    .withAllowBrandSafety(false)
                    .withAllowGallery(false)
                    .withAllowImage(false)
                    .withAllowDisabledPlaces(false)
                    .withAllowDisabledVideoPlaces(false))
                .withAllowedCreativeTemplates(ALLOWED_CREATIVE_TYPES)
                .withAllowedPageIds(emptyList())
                .withBidModifiers(emptyList())
                .withPriceMarkups(emptyList())
                .withProductId(0L)
                .withCategoryId(1L);

        testCryptaSegmentRepository.addAll(allGoals);
    }

    @Test
    public void prepareAndApply_OnePricePackage_Successful() {
        MassResult<Long> result = prepareAndApplyOperation(clientPricePackage);

        assertThat(result, isFullySuccessful());
        List<Result<Long>> operationResult = result.getResult();
        Long packageId = operationResult.get(0).getResult();
        PricePackage fromDb = repository.getPricePackages(List.of(packageId))
                .get(packageId);
        Assertions.assertThat(fromDb).is(matchedBy(
                beanDiffer(
                        clientPricePackage
                                .withLastUpdateTime(LocalDateTime.now(ZoneOffset.UTC))
                                .withStatusApprove(StatusApprove.NEW)
                ).useCompareStrategy(PRICE_PACKAGES_COMPARE_STRATEGY)
        ));
    }

    @Test
    @Description("Особый геотип для прайсового видео. Разрешены сразу кастомные и фиксированные гео")
    public void geoType10_Successful() {
        clientPricePackage.withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withAllowedCreativeTemplates(DEFAULT_ALLOWED_CREATIVE_TYPES);
        clientPricePackage.getTargetingsFixed()
                .withGeo(List.of(SAINT_PETERSBURG_REGION_ID))
                .withGeoType(REGION_TYPE_REGION)
                .withAllowExpandedDesktopCreative(false);
        clientPricePackage.getTargetingsCustom()
                .withGeo(List.of(SOUTH_FEDERAL_DISTRICT_REGION_ID, SAINT_PETERSBURG_REGION_ID, KRASNODAR_KRAI))
                .withGeoType(REGION_TYPE_REGION);

        MassResult<Long> result = prepareAndApplyOperation(clientPricePackage);
        for (DefectInfo info : result.getValidationResult().flattenErrors()) {
            logger.info(info.toString());
        }
        assertThat(result, isFullySuccessful());
    }

    @Test
    @Description("Особый геотип для пакета на главной. Разрешены сразу кастомные и фиксированные гео")
    public void geoType10yndx_Successful() {
        clientPricePackage.withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE));
        clientPricePackage.getTargetingsFixed()
                .withGeo(List.of(SAINT_PETERSBURG_REGION_ID))
                .withGeoType(REGION_TYPE_REGION)
                .withAllowExpandedDesktopCreative(false);
        clientPricePackage.getTargetingsCustom()
                .withGeo(List.of(SOUTH_FEDERAL_DISTRICT_REGION_ID, SAINT_PETERSBURG_REGION_ID, KRASNODAR_KRAI))
                .withGeoType(REGION_TYPE_REGION);

        MassResult<Long> result = prepareAndApplyOperation(clientPricePackage);
        for (DefectInfo info : result.getValidationResult().flattenErrors()) {
            logger.info(info.toString());
        }
        assertThat(result, isFullySuccessful());
    }

    @Test
    @Description("Валидация что положительные значения targetingsFixed " +
            "- это подмножество положительных значений targetingsCustom")
    public void geoType10_fixedSubCustom() {
        clientPricePackage.withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withAllowedCreativeTemplates(DEFAULT_ALLOWED_CREATIVE_TYPES);
        clientPricePackage.getTargetingsFixed()
                .withGeo(List.of(SAINT_PETERSBURG_REGION_ID, MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID))
                .withGeoType(REGION_TYPE_REGION)
                .withAllowExpandedDesktopCreative(false);
        clientPricePackage.getTargetingsCustom()
                .withGeo(List.of(SOUTH_FEDERAL_DISTRICT_REGION_ID, SAINT_PETERSBURG_REGION_ID, KRASNODAR_KRAI))
                .withGeoType(REGION_TYPE_REGION);

        MassResult<Long> result = prepareAndApplyOperation(clientPricePackage);
        assertThat(result, Matchers.not(isFullySuccessful()));
    }

    @Test
    @Description("выдавать ошибку, если пользователь не выбрал вообще регионы")
    public void geoType10_emptyRegion() {
        clientPricePackage.withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withAllowedCreativeTemplates(DEFAULT_ALLOWED_CREATIVE_TYPES);
        clientPricePackage.getTargetingsFixed()
                .withGeo(emptyList())
                .withGeoType(REGION_TYPE_REGION)
                .withAllowExpandedDesktopCreative(false);
        clientPricePackage.getTargetingsCustom()
                .withGeo(emptyList())
                .withGeoType(REGION_TYPE_REGION);

        MassResult<Long> result = prepareAndApplyOperation(clientPricePackage);
        assertThat(result, Matchers.not(isFullySuccessful()));
    }

    @Test
    public void prepareAndApply_Geo() {
        clientPricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA, -URAL_DISTRICT))
                .withGeoType(REGION_TYPE_DISTRICT);
        clientPricePackage.getTargetingsCustom()
                .withGeo(null)
                .withGeoType(null);

        MassResult<Long> result = prepareAndApplyOperation(clientPricePackage);
        assertThat(result, isFullySuccessful());

        List<Result<Long>> operationResult = result.getResult();
        Long packageId = operationResult.get(0).getResult();
        PricePackage fromDb = repository.getPricePackages(List.of(packageId))
                .get(packageId);

        Assertions.assertThat(fromDb.getTargetingsFixed().getGeo()).containsExactlyInAnyOrder(RUSSIA, -URAL_DISTRICT);
        Assertions.assertThat(fromDb.getTargetingsFixed().getGeoExpanded()).containsExactlyInAnyOrder(
                NORTHWESTERN_DISTRICT, CENTRAL_DISTRICT, SOUTH_DISTRICT,
                SIBERIAN_DISTRICT, FAR_EASTERN_DISTRICT, VOLGA_DISTRICT, NORTH_CAUCASIAN_DISTRICT);
        Assertions.assertThat(fromDb.getTargetingsCustom().getGeo()).isNull();
        Assertions.assertThat(fromDb.getTargetingsCustom().getGeoExpanded()).isNull();
    }

    @Test
    public void prepareAndApply_RussianGeoTreeUsed() {
        clientPricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoType(REGION_TYPE_PROVINCE);
        clientPricePackage.getTargetingsCustom()
                .withGeo(null)
                .withGeoType(null);

        MassResult<Long> result = prepareAndApplyOperation(clientPricePackage);
        assertThat(result, isFullySuccessful());

        List<Result<Long>> operationResult = result.getResult();
        Long packageId = operationResult.get(0).getResult();
        PricePackage fromDb = repository.getPricePackages(List.of(packageId))
                .get(packageId);

        Assertions.assertThat(fromDb.getTargetingsFixed().getGeoExpanded()).contains(CRIMEA_PROVINCE);
    }

    @Test
    public void prepareAndApply_OnePricePackage_Fail() {
        clientPricePackage.withPrice(null);

        MassResult<Long> result = prepareAndApplyOperation(clientPricePackage);
        assertThat(result, isSuccessful(false));
    }

    private MassResult<Long> prepareAndApplyOperation(PricePackage pricePackage) {
        return addOperationFactory.newInstance(Applicability.FULL, List.of(pricePackage), new User().withUid(1L))
                .prepareAndApply();
    }

    @Test
    public void modileAdjustments_validation() {
        clientPricePackage
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE))
                .withBidModifiers(List.of(
                        new BidModifierInventory()
                                .withType(BidModifierType.INVENTORY_MULTIPLIER)
                                .withInventoryAdjustments(List.of(
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.REWARDED))),
                        new BidModifierMobile()
                                .withType(BidModifierType.MOBILE_MULTIPLIER)
                                .withMobileAdjustment(new BidModifierMobileAdjustment().withOsType(OsType.ANDROID)),
                        new BidModifierMobile()
                                .withType(BidModifierType.MOBILE_MULTIPLIER)
                                .withMobileAdjustment(new BidModifierMobileAdjustment().withOsType(OsType.IOS)),
                        new BidModifierDesktop()
                                .withType(BidModifierType.DESKTOP_MULTIPLIER)
                                .withDesktopAdjustment(new BidModifierDesktopAdjustment())));

        MassResult<Long> result = prepareAndApplyOperation(clientPricePackage);

        assertThat(result, Matchers.not(isFullySuccessful()));
    }

    @Test
    public void mobileAdjustment_serialize() {
        clientPricePackage
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE))
                .withBidModifiers(List.of(
                        new BidModifierInventory()
                                .withType(BidModifierType.INVENTORY_MULTIPLIER)
                                .withInventoryAdjustments(List.of(
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.REWARDED))),
                        new BidModifierMobile()
                                .withType(BidModifierType.MOBILE_MULTIPLIER)
                                .withMobileAdjustment(new BidModifierMobileAdjustment().withOsType(OsType.ANDROID)),
                        new BidModifierDesktop()
                                .withType(BidModifierType.DESKTOP_MULTIPLIER)
                                .withDesktopAdjustment(new BidModifierDesktopAdjustment())));

        MassResult<Long> result = prepareAndApplyOperation(clientPricePackage);

        assertThat(result, isFullySuccessful());
        Long packageId = result.getResult().get(0).getResult();
        PricePackage fromDb = repository.getPricePackages(List.of(packageId)).get(packageId);

        assertThat(fromDb.getBidModifiers(), hasItems(new BidModifierInventory()
                .withType(BidModifierType.INVENTORY_MULTIPLIER)
                .withInventoryAdjustments(List.of(
                        new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.REWARDED)
                                .withIsRequiredInPricePackage(true)))));
        assertThat(fromDb.getBidModifiers(), hasItems(new BidModifierMobile()
                .withType(BidModifierType.MOBILE_MULTIPLIER)
                .withMobileAdjustment(new BidModifierMobileAdjustment()
                        .withIsRequiredInPricePackage(true)
                        .withOsType(OsType.ANDROID))));
        assertThat(fromDb.getBidModifiers(), hasItems(new BidModifierDesktop()
                .withType(BidModifierType.DESKTOP_MULTIPLIER)
                .withDesktopAdjustment(new BidModifierDesktopAdjustment()
                        .withIsRequiredInPricePackage(true))));
    }

    @Test
    public void cmp_video_field() {
        clientPricePackage
                .withAllowedPageIds(List.of(1L, 2L))
                .withCampaignAutoApprove(true)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE))
                .withCampaignOptions(new PricePackageCampaignOptions().withAllowBrandSafety(true));

        MassResult<Long> result = prepareAndApplyOperation(clientPricePackage);

        assertThat(result, isFullySuccessful());
        Long packageId = result.getResult().get(0).getResult();
        PricePackage fromDb = repository.getPricePackages(List.of(packageId)).get(packageId);

        assertThat(fromDb.getCampaignAutoApprove(), is(true));
        assertThat(fromDb.getAllowedPageIds(), hasItems(1L, 2L));
        assertThat(fromDb.getAvailableAdGroupTypes(), is(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE)));
        assertThat(fromDb.getCampaignOptions().getAllowBrandSafety(), is(true));
    }

    @Test
    public void cmp_video_frontpage_field() {
        //Добавляем пакет видео на Главной с доступностью картинки и галереи. Все три поля проставились
        clientPricePackage
                .withIsFrontpage(true)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withAllowedCreativeTemplates(null)
                .withAllowedOrderTags(List.of("mytag"))
                .withCampaignOptions(new PricePackageCampaignOptions()
                        .withAllowGallery(true)
                        .withAllowImage(true));
        clientPricePackage.getTargetingsFixed().withAllowExpandedDesktopCreative(false);

        MassResult<Long> result = prepareAndApplyOperation(clientPricePackage);

        assertThat(result, isFullySuccessful());
        Long packageId = result.getResult().get(0).getResult();
        PricePackage fromDb = repository.getPricePackages(List.of(packageId)).get(packageId);

        assertThat(fromDb.getIsFrontpage(), is(true));
        assertThat(fromDb.getAvailableAdGroupTypes(), is(Set.of(AdGroupType.CPM_VIDEO)));
        assertThat(fromDb.getCampaignOptions().getAllowGallery(), is(true));
        assertThat(fromDb.getCampaignOptions().getAllowImage(), is(true));
        //Теги таргет и ордер на превалидации проставляются принудительно в portal-trusted
        Assertions.assertThat(fromDb.getAllowedTargetTags()).contains("portal-trusted");
        Assertions.assertThat(fromDb.getAllowedOrderTags()).contains("portal-trusted");
        //разрешённые шаблоны принудительно выставляются в новый шаблон 406
        Assertions.assertThat(fromDb.getAllowedCreativeTemplates().get(CPM_VIDEO_CREATIVE)).contains(406L);
    }
}

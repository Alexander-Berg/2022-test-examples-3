package ru.yandex.direct.core.entity.campaign.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jdk.jfr.Description;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventory;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.campaign.model.CampaignMeasurer;
import ru.yandex.direct.core.entity.campaign.model.CampaignMeasurerSystem;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBrandSafety;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPricePackage;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightReasonIncorrect;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusCorrect;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightTargetingsSnapshot;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageCampaignOptions;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.libs.timetarget.TimeTarget;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.defect.ids.NumberDefectIds;
import ru.yandex.direct.validation.result.DefectIds;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmVideoBanner;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.feature.FeatureName.IAS_MEASURER;
import static ru.yandex.direct.feature.FeatureName.MOAT_MEASURER_CAMP;
import static ru.yandex.direct.feature.FeatureName.MOAT_USE_UNSTABLE_SCRIPT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.mustBeEmpty;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RestrictedCampaignsUpdateOperationCpmPriceTest extends RestrictedCampaignsUpdateOperationCpmPriceTestBase {
    private static final TimeTarget TEST_TIME_TARGET = TimeTarget.parseRawString("1ABCDEFGHIJKLMNOPQRSTUVWX");
    private static final List<String> DISABLED_VIDEO_PLACEMENTS = List.of("vk.com", "music.yandex.ru");
    private static final String DOMAIN = "domain.com";
    private static final String ANOTHER_DOMAIN = "anotherdomain.com";
    private static final String WWW = "www.";

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Test
    public void changeImmutableFieldsProhibited() {
        setupOperator(RbacRole.CLIENT);
        var campaign = defaultCampaign();
        createPriceCampaign(campaign);

        ModelChanges<CpmPriceCampaign> modelChanges = new ModelChanges<>(campaign.getId(), CpmPriceCampaign.class);
        modelChanges.process(TEST_TIME_TARGET, CpmPriceCampaign.TIME_TARGET);
        modelChanges.process(new DbStrategy(), CpmPriceCampaign.STRATEGY);
        modelChanges.process(100L, CpmPriceCampaign.PRICE_PACKAGE_ID);
        modelChanges.process(new PriceFlightTargetingsSnapshot()
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withViewTypes(emptyList())
                        .withAllowExpandedDesktopCreative(true),
                CpmPriceCampaign.FLIGHT_TARGETINGS_SNAPSHOT);
        modelChanges.process(PriceFlightStatusCorrect.NO, CpmPriceCampaign.FLIGHT_STATUS_CORRECT);
        modelChanges.process(PriceFlightReasonIncorrect.MORE_THAN_ONE_DEFAULT_GROUP,
                CpmPriceCampaign.FLIGHT_REASON_INCORRECT);
        modelChanges.process(false, CpmPriceCampaign.HAS_EXTENDED_GEO_TARGETING);
        modelChanges.process(false, CpmPriceCampaign.HAS_TITLE_SUBSTITUTION);
        modelChanges.process(false, CpmPriceCampaign.ENABLE_COMPANY_INFO);
        modelChanges.process(false, CpmPriceCampaign.ENABLE_CPC_HOLD);
        modelChanges.process(false, CpmPriceCampaign.IS_DRAFT_APPROVE_ALLOWED);

        MassResult<Long> result = apply(modelChanges);

        Assert.assertThat(result.getValidationResult(), allOf(
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.STRATEGY)), DefectIds.FORBIDDEN_TO_CHANGE)),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.TIME_TARGET)), DefectIds.FORBIDDEN_TO_CHANGE)),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.PRICE_PACKAGE_ID)), DefectIds.FORBIDDEN_TO_CHANGE)),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.FLIGHT_TARGETINGS_SNAPSHOT)), DefectIds.FORBIDDEN_TO_CHANGE)),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.FLIGHT_STATUS_CORRECT)), DefectIds.FORBIDDEN_TO_CHANGE)),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.FLIGHT_REASON_INCORRECT)), DefectIds.FORBIDDEN_TO_CHANGE)),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.HAS_EXTENDED_GEO_TARGETING)), DefectIds.FORBIDDEN_TO_CHANGE)),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.HAS_TITLE_SUBSTITUTION)), DefectIds.FORBIDDEN_TO_CHANGE)),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.ENABLE_COMPANY_INFO)), DefectIds.FORBIDDEN_TO_CHANGE)),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.ENABLE_CPC_HOLD)), DefectIds.FORBIDDEN_TO_CHANGE)),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.IS_DRAFT_APPROVE_ALLOWED)), DefectIds.FORBIDDEN_TO_CHANGE))
        ));
    }

    @Test
    @Description("при обновлении кампании прайсовый пакет может быть отвязан от клиента. это ок, у клиента должна " +
            "сохранится возможность доступа к кампании")
    public void packageNotAvailableForClient_OkForUpdate() {
        setupOperator(RbacRole.CLIENT);
        var anotherClientPricePackage = steps.pricePackageSteps().createPricePackage(
                defaultPricePackage()
                        .withPrice(BigDecimal.valueOf(20L))
                        .withClients(emptyList()))
                .getPricePackage();
        var campaign = defaultCampaign()
                .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                .withPricePackageId(anotherClientPricePackage.getId());
        createPriceCampaign(campaign);

        // проверяем, что у клиента остался доступ к кампании редактируя name, но можно было бы поредактировать
        // любое другое поле
        String newName = campaign.getName() + " updated";
        ModelChanges<CampaignWithPricePackage> modelChanges =
                ModelChanges.build(campaign, CampaignWithPricePackage.NAME, newName);

        MassResult<Long> result = apply(modelChanges);

        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign)
                .is(matchedBy(beanDiffer(updateOriginCampaignToExpected(campaign)
                        .withName(newName)
                        .withStatusBsSynced(CampaignStatusBsSynced.NO)
                ).useCompareStrategy(cpmPriceCampaignCompareStrategy())));
    }

    @Test
    @Description("Проверяем, что вызывается валидация кампании. Клиент у незапрувленной кампании до момента старта " +
            "может поменять ставку, но если она будет за рамками пакета - должна прийти ошибка валидации ")
    public void invalidCampaignWithPricePackageProhibited() {
        setupOperator(RbacRole.CLIENT);
        var campaign = defaultCampaign()
                .withFlightStatusApprove(PriceFlightStatusApprove.NEW);
        createPriceCampaign(campaign);

        long newFlightOrderVolume = 3000; // не укладывает в ограничение на пакет
        ModelChanges<CampaignWithPricePackage> modelChanges =
                ModelChanges.build(campaign, CampaignWithPricePackage.FLIGHT_ORDER_VOLUME, newFlightOrderVolume);

        MassResult<Long> result = apply(modelChanges);

        Assert.assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.FLIGHT_ORDER_VOLUME)),
                        NumberDefectIds.MUST_BE_IN_THE_INTERVAL_INCLUSIVE))
        );
    }

    @Test
    public void superChangeStatusShow_Success() {
        setupOperator(RbacRole.SUPER);
        var campaign = defaultCampaign();
        createPriceCampaign(campaign);

        boolean newStatusShow = !campaign.getStatusShow();
        ModelChanges<CampaignWithPricePackage> modelChanges =
                ModelChanges.build(campaign, CampaignWithPricePackage.STATUS_SHOW, newStatusShow);

        MassResult<Long> result = apply(modelChanges);

        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign)
                .is(matchedBy(beanDiffer(updateOriginCampaignToExpected(campaign)
                        .withStatusShow(newStatusShow)
                        .withStatusBsSynced(CampaignStatusBsSynced.NO)
                ).useCompareStrategy(cpmPriceCampaignCompareStrategy())));
    }

    @Test
    public void clientChangeStatusShow_Error() {
        setupOperator(RbacRole.CLIENT);
        var campaign = defaultCampaign();
        createPriceCampaign(campaign);

        boolean newStatusShow = !campaign.getStatusShow();
        ModelChanges<CampaignWithPricePackage> modelChanges =
                ModelChanges.build(campaign, CampaignWithPricePackage.STATUS_SHOW, newStatusShow);

        MassResult<Long> result = apply(modelChanges);

        Assert.assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.STATUS_SHOW)),
                        DefectIds.FORBIDDEN_TO_CHANGE))
        );
    }

    @Test
    public void preValidationAndValidationBeforeApplyFailed_PreValidationError() {
        setupOperator(RbacRole.CLIENT);
        var campaign = defaultCampaign();
        createPriceCampaign(campaign);

        ModelChanges<CampaignWithPricePackage> modelChanges = ModelChanges.build(campaign,
                        CampaignWithPricePackage.PRICE_PACKAGE_ID, -1L);
        modelChanges.process(!campaign.getStatusShow(), CampaignWithPricePackage.STATUS_SHOW);

        MassResult<Long> result = apply(modelChanges);

        Assert.assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0), field(CpmPriceCampaign.PRICE_PACKAGE_ID)),
                DefectIds.FORBIDDEN_TO_CHANGE))
        );
    }

    @Test
    public void changeFlightStatusApprove_CampaignAggregatedStatusMarkedObsolete() {
        setupOperator(RbacRole.SUPER);
        var campaign = defaultCampaign()
                .withShows(0L)
                .withFlightStatusCorrect(PriceFlightStatusCorrect.NO)
                .withFlightStatusApprove(PriceFlightStatusApprove.YES);
        createPriceCampaign(campaign);
        addCampaignAggregatedStatus(campaign);

        ModelChanges<CampaignWithPricePackage> modelChanges = ModelChanges.build(campaign,
                CampaignWithPricePackage.FLIGHT_STATUS_APPROVE, PriceFlightStatusApprove.NEW);
        MassResult<Long> result = apply(modelChanges);
        assumeThat(result, isFullySuccessful());

        Boolean campaignAggregatedStatusIsObsolete = aggregatedStatusesRepository
                .getCampaignStatusesIsObsolete(defaultClient.getShard(), singletonList(campaign.getId()))
                .get(campaign.getId());
        assertThat(campaignAggregatedStatusIsObsolete).isTrue();
    }


    @Test
    @Description("Если на пакете разрешены brandsafety, то сохраняется")
    public void brandsafety_enable() {
        setupOperator(RbacRole.CLIENT);
        List<Long> categories = asList(4294967299L, 4294967297L, 4294967298L);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withCampaignOptions(new PricePackageCampaignOptions().withAllowBrandSafety(true))
                ).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        ModelChanges<CampaignWithPricePackage> modelChanges = ModelChanges.build(campaign,
                CampaignWithPricePackage.BRAND_SAFETY_CATEGORIES, categories);

        MassResult<Long> result = apply(modelChanges);

        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getBrandSafetyCategories()).isEqualTo(categories);
    }

    @Test
    @Description("Если на пакете запрещены brandsafety, то ошибка валидации")
    public void brandsafety_disable() {
        setupOperator(RbacRole.CLIENT);
        List<Long> categories = asList(4294967299L, 4294967297L, 4294967298L);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withCampaignOptions(new PricePackageCampaignOptions().withAllowBrandSafety(false))
                ).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        ModelChanges<CampaignWithPricePackage> modelChanges = ModelChanges.build(campaign,
                CampaignWithPricePackage.BRAND_SAFETY_CATEGORIES, categories);

        MassResult<Long> result = apply(modelChanges);

        Assert.assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithBrandSafety.BRAND_SAFETY_CATEGORIES)),
                mustBeEmpty())));
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getBrandSafetyCategories()).isEmpty();
    }

    @Test
    @Description("Если на пакете флаг brandsafety = null, то считает что разрешены")
    public void brandsafety_null() {
        setupOperator(RbacRole.CLIENT);
        List<Long> categories = asList(4294967299L, 4294967297L, 4294967298L);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withCampaignOptions(null)
                                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                ).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        ModelChanges<CampaignWithPricePackage> modelChanges = ModelChanges.build(campaign,
                CampaignWithPricePackage.BRAND_SAFETY_CATEGORIES, categories);

        MassResult<Long> result = apply(modelChanges);

        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getBrandSafetyCategories()).isEqualTo(categories);
    }

    @Test
    @Description("На пакете разрешено ограничение видео инвентаря - сохраняется")
    public void disabledVideoPlacements_enable() {
        setupOperator(RbacRole.CLIENT);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withCampaignOptions(new PricePackageCampaignOptions()
                                        .withAllowDisabledVideoPlaces(true))
                ).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        ModelChanges<CampaignWithPricePackage> modelChanges = ModelChanges.build(campaign,
                CampaignWithPricePackage.DISABLED_VIDEO_PLACEMENTS, DISABLED_VIDEO_PLACEMENTS);

        MassResult<Long> result = apply(modelChanges);

        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getDisabledVideoPlacements()).containsOnlyElementsOf(DISABLED_VIDEO_PLACEMENTS);
    }

    @Test
    @Description("На пакете запрещено ограничение видео инвентаря - ошибка валидации")
    public void disabledVideoPlacements_forbidden() {
        setupOperator(RbacRole.CLIENT);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withCampaignOptions(new PricePackageCampaignOptions()
                                        .withAllowDisabledVideoPlaces(false))
                ).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        ModelChanges<CampaignWithPricePackage> modelChanges = ModelChanges.build(campaign,
                CampaignWithPricePackage.DISABLED_VIDEO_PLACEMENTS, DISABLED_VIDEO_PLACEMENTS);

        MassResult<Long> result = apply(modelChanges);

        Assert.assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithPricePackage.DISABLED_VIDEO_PLACEMENTS)),
                mustBeEmpty())));
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getDisabledVideoPlacements()).isNull();
    }

    @Test
    @Description("На пакете запрещено ограничение видео инвентаря - можно пустой список")
    public void disabledVideoPlacements_forbidden_empty() {
        setupOperator(RbacRole.CLIENT);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withCampaignOptions(new PricePackageCampaignOptions()
                                        .withAllowDisabledVideoPlaces(false))
                ).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        ModelChanges<CampaignWithPricePackage> modelChanges = ModelChanges.build(campaign,
                CampaignWithPricePackage.DISABLED_VIDEO_PLACEMENTS, emptyList());

        MassResult<Long> result = apply(modelChanges);

        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getDisabledVideoPlacements()).isEmpty();
    }

    @Test
    @Description("На пакете разрешено ограничение площадок - сохраняется")
    public void disabledPlaces_enable() {
        setupOperator(RbacRole.CLIENT);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withCampaignOptions(new PricePackageCampaignOptions()
                                        .withAllowDisabledPlaces(true))
                ).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        ModelChanges<CampaignWithPricePackage> modelChanges = ModelChanges.build(campaign,
                CampaignWithPricePackage.DISABLED_DOMAINS, List.of(DOMAIN, WWW + ANOTHER_DOMAIN));

        MassResult<Long> result = apply(modelChanges);

        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        Assertions.assertThat(actualCampaign.getDisabledDomains()).containsExactlyInAnyOrder(DOMAIN, ANOTHER_DOMAIN);
    }

    @Test
    @Description("На пакете запрещено ограничение площадок - ошибка валидации")
    public void disabledPlaces_forbidden() {
        setupOperator(RbacRole.CLIENT);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withCampaignOptions(new PricePackageCampaignOptions()
                                        .withAllowDisabledPlaces(false))
                ).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        ModelChanges<CampaignWithPricePackage> modelChanges = ModelChanges.build(campaign,
                CampaignWithPricePackage.DISABLED_DOMAINS, List.of(DOMAIN, WWW + ANOTHER_DOMAIN));

        MassResult<Long> result = apply(modelChanges);

        Assert.assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0), field(CpmPriceCampaign.DISABLED_DOMAINS)),
                mustBeEmpty())));
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getDisabledDomains()).isNull();
    }

    @Test
    @Description("На пакете запрещено ограничение площадок - можно пустой список")
    public void disabledPlaces_forbidden_empty() {
        setupOperator(RbacRole.CLIENT);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withCampaignOptions(new PricePackageCampaignOptions()
                                        .withAllowDisabledPlaces(false))
                ).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        ModelChanges<CampaignWithPricePackage> modelChanges = ModelChanges.build(campaign,
                CampaignWithPricePackage.DISABLED_DOMAINS, emptyList());

        MassResult<Long> result = apply(modelChanges);

        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getDisabledDomains()).isNull();
    }

    @Test
    @Description("На пакете заданы корректировки по типу инвентаря. На обновлении не теряются")
    public void bidModifiers() {
        setupOperator(RbacRole.CLIENT);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withBidModifiers(List.of(
                                        new BidModifierInventory()
                                                .withType(BidModifierType.INVENTORY_MULTIPLIER)
                                                .withInventoryAdjustments(List.of(
                                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.INPAGE),
                                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.INSTREAM_WEB)
                                                ))))
                ).getPricePackageId();
        var campaign = defaultCampaign()
                .withPricePackageId(pricePackageId)
                .withBidModifiers(List.of(
                        new BidModifierInventory()
                                .withType(BidModifierType.INVENTORY_MULTIPLIER)
                                .withInventoryAdjustments(List.of(
                                        new BidModifierInventoryAdjustment()
                                                .withInventoryType(InventoryType.INAPP).withPercent(0),
                                        new BidModifierInventoryAdjustment()
                                                .withInventoryType(InventoryType.INBANNER).withPercent(0),
                                        new BidModifierInventoryAdjustment()
                                                .withInventoryType(InventoryType.REWARDED).withPercent(0)
                                ))));
        createPriceCampaign(campaign);
        ModelChanges<CampaignWithPricePackage> modelChanges = ModelChanges.build(campaign,
                CampaignWithPricePackage.BID_MODIFIERS, null);

        MassResult<Long> result = apply(modelChanges);

        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        BidModifierInventory bidModifierInventory = (BidModifierInventory) actualCampaign.getBidModifiers().stream()
                .filter(it -> it.getType() == BidModifierType.INVENTORY_MULTIPLIER)
                .findAny().get();
        var inappAdjustment = bidModifierInventory.getInventoryAdjustments().stream()
                .filter(it -> it.getInventoryType() == InventoryType.INAPP)
                .findAny().get();
        assertThat(inappAdjustment.getPercent()).isEqualTo(0);
    }

    @Test
    @Description("для заапрувленного видео можем поменять дату окончания и объем")
    public void video_canChangeEndDate() {
        setupOperator(RbacRole.CLIENT);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                                .withCampaignAutoApprove(true)
                ).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        LocalDate newEndDate = campaign.getEndDate().plusDays(90);
        ModelChanges<CpmPriceCampaign> modelChanges = new ModelChanges<>(campaign.getId(), CpmPriceCampaign.class);
        modelChanges.process(newEndDate, CampaignWithPricePackage.END_DATE);
        modelChanges.process(900L, CampaignWithPricePackage.FLIGHT_ORDER_VOLUME);

        MassResult<Long> result = apply(modelChanges);

        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getEndDate()).isEqualTo(newEndDate);
        assertThat(actualCampaign.getFlightOrderVolume()).isEqualTo(900L);
    }

    @Test
    @Description("для заапрувленного видео не можем поменять дату начала")
    public void video_changeStartDate_forbidden() {
        setupOperator(RbacRole.CLIENT);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                                .withCampaignAutoApprove(true)
                ).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        ModelChanges<CpmPriceCampaign> modelChanges = new ModelChanges<>(campaign.getId(), CpmPriceCampaign.class);
        modelChanges.process(LocalDate.now().minusDays(90), CampaignWithPricePackage.START_DATE);

        MassResult<Long> result = apply(modelChanges);
        Assert.assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0), field(CpmPriceCampaign.START_DATE)),
                DefectIds.FORBIDDEN_TO_CHANGE)));
    }

    @Test
    @Description("для заапрувленной кампании на главной не можем поменять дату окончания")
    public void yndx_changeEndDate_forbidden() {
        setupOperator(RbacRole.CLIENT);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withCampaignAutoApprove(true)
                ).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        ModelChanges<CpmPriceCampaign> modelChanges = new ModelChanges<>(campaign.getId(), CpmPriceCampaign.class);
        modelChanges.process(LocalDate.now().minusDays(90), CampaignWithPricePackage.END_DATE);

        MassResult<Long> result = apply(modelChanges);
        Assert.assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0), field(CpmPriceCampaign.END_DATE)),
                DefectIds.FORBIDDEN_TO_CHANGE)));
    }

    @Test
    @Description("для заапрувленного видео после изменения статус апрува сбросился без автоапрува")
    public void video_canChangeEndDate_statusApproveNew() {
        setupOperator(RbacRole.CLIENT);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                                .withCampaignAutoApprove(false)
                ).getPricePackageId();
        var campaign = defaultCampaign()
                .withFlightStatusApprove(PriceFlightStatusApprove.YES)
                .withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        LocalDate newEndDate = campaign.getEndDate().plusDays(90);
        ModelChanges<CpmPriceCampaign> modelChanges = new ModelChanges<>(campaign.getId(), CpmPriceCampaign.class);
        modelChanges.process(newEndDate, CampaignWithPricePackage.END_DATE);

        MassResult<Long> result = apply(modelChanges);

        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getFlightStatusApprove()).isEqualTo(PriceFlightStatusApprove.NEW);
    }

    @Test
    @Description("для заапрувленного видео после изменения статус апрува остался с автоапрувом на пакете")
    public void video_canChangeEndDate_statusApproveYes() {
        setupOperator(RbacRole.CLIENT);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                                .withCampaignAutoApprove(true)
                ).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        LocalDate newEndDate = campaign.getEndDate().plusDays(90);
        ModelChanges<CpmPriceCampaign> modelChanges = new ModelChanges<>(campaign.getId(), CpmPriceCampaign.class);
        modelChanges.process(newEndDate, CampaignWithPricePackage.END_DATE);
        modelChanges.process(900L, CampaignWithPricePackage.FLIGHT_ORDER_VOLUME);

        MassResult<Long> result = apply(modelChanges);

        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getFlightStatusApprove()).isEqualTo(PriceFlightStatusApprove.YES);
    }

    @Test
    @Description("Сохраняется верификатор moat на прайсовой кампании без фичи не пройдём валидацию")
    public void save_moat_invalid() {
        setupOperator(RbacRole.CLIENT);
        steps.featureSteps().addClientFeature(defaultClient.getClientId(), MOAT_MEASURER_CAMP, false);
        var pricePackageId = steps.pricePackageSteps().createPricePackage(defaultPricePackage()).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        ModelChanges<CpmPriceCampaign> modelChanges = new ModelChanges<>(campaign.getId(), CpmPriceCampaign.class);
        modelChanges.process(List.of(new CampaignMeasurer()
                .withMeasurerSystem(CampaignMeasurerSystem.MOAT)
                .withParams("{}")
        ), CpmPriceCampaign.MEASURERS);

        MassResult<Long> result = apply(modelChanges);
        Assert.assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0), field(CpmPriceCampaign.MEASURERS), index(0)),
                DefectIds.INVALID_VALUE)));
    }

    @Test
    @Description("Сохраняется верификатор moat на прайсовой кампании с фичей")
    public void save_moat_valid() {
        setupOperator(RbacRole.CLIENT);
        steps.featureSteps().addClientFeature(defaultClient.getClientId(), MOAT_MEASURER_CAMP, true);
        steps.featureSteps().addClientFeature(defaultClient.getClientId(), MOAT_USE_UNSTABLE_SCRIPT, true);
        var pricePackageId = steps.pricePackageSteps().createPricePackage(defaultPricePackage()).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        ModelChanges<CpmPriceCampaign> modelChanges = new ModelChanges<>(campaign.getId(), CpmPriceCampaign.class);
        modelChanges.process(List.of(new CampaignMeasurer()
                .withMeasurerSystem(CampaignMeasurerSystem.MOAT)
                .withParams("{}")
        ), CpmPriceCampaign.MEASURERS);

        MassResult<Long> result = apply(modelChanges);
        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getMeasurers()).isNotEmpty();
        var params = actualCampaign.getMeasurers().get(0).getParams();
        //MOAT_USE_UNSTABLE_SCRIPT учитывается
        assertThat(params).containsIgnoringCase("use_unstable_script");
    }

    @Test
    @Description("Сохраняется верификатор ias на прайсовой кампании без фичи не пройдём валидацию")
    public void save_ias_invalid() {
        setupOperator(RbacRole.CLIENT);
        steps.featureSteps().addClientFeature(defaultClient.getClientId(), IAS_MEASURER, false);
        var pricePackageId = steps.pricePackageSteps().createPricePackage(defaultPricePackage()).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        ModelChanges<CpmPriceCampaign> modelChanges = new ModelChanges<>(campaign.getId(), CpmPriceCampaign.class);
        modelChanges.process(List.of(new CampaignMeasurer()
                .withMeasurerSystem(CampaignMeasurerSystem.IAS)
                .withParams("{\"advid\":123,\"pubid\":123}")
        ), CpmPriceCampaign.MEASURERS);

        MassResult<Long> result = apply(modelChanges);
        Assert.assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0), field(CpmPriceCampaign.MEASURERS), index(0)),
                DefectIds.INVALID_VALUE)));
    }

    @Test
    @Description("Сохраняется верификатор ias на прайсовой кампании с фичей")
    public void save_ias_valid() {
        setupOperator(RbacRole.CLIENT);
        steps.featureSteps().addClientFeature(defaultClient.getClientId(), IAS_MEASURER, true);
        var pricePackageId = steps.pricePackageSteps().createPricePackage(defaultPricePackage()).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId);
        createPriceCampaign(campaign);
        ModelChanges<CpmPriceCampaign> modelChanges = new ModelChanges<>(campaign.getId(), CpmPriceCampaign.class);
        modelChanges.process(List.of(new CampaignMeasurer()
                .withMeasurerSystem(CampaignMeasurerSystem.IAS)
                .withParams("{\"advid\":123,\"pubid\":123}")
        ), CpmPriceCampaign.MEASURERS);

        MassResult<Long> result = apply(modelChanges);
        assumeThat(result, isFullySuccessful());
        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getMeasurers()).isNotEmpty();
    }

    @Test
    @Description("Включение-выключение измерителя на кампании должно сбрасывать флаг statusBsSynced на всех баннерах " +
            "кампании")
    public void save_moat_statusBsSynced() {
        setupOperator(RbacRole.CLIENT);
        steps.featureSteps().addClientFeature(defaultClient.getClientId(), MOAT_MEASURER_CAMP, true);
        PricePackage pricePackage = defaultPricePackage().withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO));
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign().withPricePackageId(pricePackageId)
                .withMeasurers(List.of(new CampaignMeasurer()
                        .withMeasurerSystem(CampaignMeasurerSystem.MOAT)
                        .withParams("{}")
                ));
        createPriceCampaign(campaign);
        var adGroup = steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(campaign, defaultClient);
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmVideoAdditionCreative(defaultClient, creativeId);
        OldCpmBanner activeBanner = activeCpmVideoBanner(adGroup.getCampaignId(), adGroup.getId(), creativeId)
                .withStatusBsSynced(StatusBsSynced.YES);
        steps.bannerSteps().createActiveCpmBannerRaw(defaultClient.getShard(), activeBanner, adGroup);

        ModelChanges<CpmPriceCampaign> modelChanges = new ModelChanges<>(campaign.getId(), CpmPriceCampaign.class);
        modelChanges.process(emptyList(), CpmPriceCampaign.MEASURERS);
        MassResult<Long> result = apply(modelChanges);
        assumeThat(result, isFullySuccessful());

        CpmPriceCampaign actualCampaign = getCampaignFromRepository(campaign.getId());
        assertThat(actualCampaign.getMeasurers()).isEmpty();
        //statusBsSynced на всех баннерах кампании сбросился
        CpmBanner banner = (CpmBanner) bannerTypedRepository.getTyped(defaultClient.getShard(), List.of(activeBanner.getId())).get(0);
        assertThat(banner.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
    }
}

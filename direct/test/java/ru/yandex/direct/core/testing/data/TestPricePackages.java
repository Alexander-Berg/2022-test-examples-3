package ru.yandex.direct.core.testing.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventory;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.entity.crypta.AudienceType;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageCampaignOptions;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageClient;
import ru.yandex.direct.core.entity.pricepackage.model.PriceRetargetingCondition;
import ru.yandex.direct.core.entity.pricepackage.model.ShowsFrequencyLimit;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.emptyList;
import static ru.yandex.direct.core.entity.creative.model.CreativeType.BANNERSTORAGE;
import static ru.yandex.direct.core.entity.creative.model.CreativeType.CPM_VIDEO_CREATIVE;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalWithId;
import static ru.yandex.direct.core.testing.data.TestRegions.CENTRAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.FAR_EASTERN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.NORTHWESTERN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.NORTH_CAUCASIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.SOUTH_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.URAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.regions.Region.REGION_TYPE_COUNTRY;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;

@ParametersAreNonnullByDefault
public class TestPricePackages {

    public static final List<Long> DEFAULT_GEO = List.of(RUSSIA, -VOLGA_DISTRICT, -SIBERIAN_DISTRICT,
            -FAR_EASTERN_DISTRICT);
    public static final List<Long> DEFAULT_GEO_EXPANDED = List.of(NORTHWESTERN_DISTRICT, CENTRAL_DISTRICT,
            URAL_DISTRICT, SOUTH_DISTRICT, NORTH_CAUCASIAN_DISTRICT);
    public static final Integer DEFAULT_GEO_TYPE = REGION_TYPE_DISTRICT;
    public static final Map<CreativeType, List<Long>>
            DEFAULT_ALLOWED_CREATIVE_TYPES = Map.of(BANNERSTORAGE, List.of(1401L, 1500L),
            CPM_VIDEO_CREATIVE, List.of(7L, 51L));
    public static final PriceRetargetingCondition DEFAULT_RETARGETING_CONDITION = new PriceRetargetingCondition()
            .withAllowAudienceSegments(true)
            .withAllowMetrikaSegments(true)
            .withCryptaSegments(emptyList())
            .withLowerCryptaTypesCount(0)
            .withUpperCryptaTypesCount(0);

    //Идентификатор цели из таблицы crypta_goals, соответствующий "мужщина"
    public static final Long MALE_CRYPTA_GOAL_ID = 2499000001L;
    public static final Long FEMALE_CRYPTA_GOAL_ID = 2499000002L;
    public static final Long AGE_18_24 = 2499000004L;
    public static final Long AGE_25_34 = 2499000005L;
    public static final Long AGE_35_44 = 2499000006L;
    //Идентификатор цели из таблицы crypta_goals, соответствующий "средний доход"
    public static final Long MID_INCOME_GOAL_ID = 2499000010L;
    public static final Long C1_INCOME_GOAL_ID = 2499000012L;
    public static final Long LTV = 2499000201L;
    public static final Long SPORT_GOAL_ID = 4294968335L;
    public static final Long BOOKS_GOAL_ID = 4294968319L;
    public static final Long SCIENCE_GOAL_ID = 4294968329L;

    private TestPricePackages() {
    }

    public static PricePackage defaultPricePackage() {
        return new PricePackage()
                .withTitle("Default Package")
                .withTrackerUrl("http://ya.ru")
                .withPrice(BigDecimal.valueOf(2999, 2))
                .withCurrency(CurrencyCode.RUB)
                .withOrderVolumeMin(1L)
                .withOrderVolumeMax(1L)
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
                        .withRetargetingCondition(
                                new PriceRetargetingCondition()
                                        .withAllowAudienceSegments(true)
                                        .withAllowMetrikaSegments(false)
                                        .withLowerCryptaTypesCount(1)
                                        .withUpperCryptaTypesCount(3)
                                        .withCryptaSegments(List.of(MALE_CRYPTA_GOAL_ID, MID_INCOME_GOAL_ID))
                        ))
                .withTargetingMarkups(emptyList())
                .withStatusApprove(StatusApprove.NEW)
                .withLastUpdateTime(LocalDateTime.parse("2019-08-09T00:11:04"))
                .withDateStart(LocalDate.now().plusMonths(1))
                .withDateEnd(LocalDate.now().plusYears(1).plusMonths(1))
                .withIsPublic(false)
                .withIsSpecial(false)
                .withIsCpd(false)
                .withIsFrontpage(true)
                .withIsArchived(false)
                .withClients(emptyList())
                .withAllowedPageIds(List.of(1L, 2L))
                .withAllowedDomains(ImmutableList.sortedCopyOf(ImmutableList.of("vk.ru", "mail.ru","*.rbc.ru")))
                .withAllowedSsp(List.of("Smaato"))
                .withCampaignAutoApprove(true)
                .withIsDraftApproveAllowed(false)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE))
                .withBidModifiers(List.of(new BidModifierInventory()
                                .withType(BidModifierType.INVENTORY_MULTIPLIER)
                                .withInventoryAdjustments(
                                        List.of(new BidModifierInventoryAdjustment()
                                                .withInventoryType(InventoryType.REWARDED)
                                                .withIsRequiredInPricePackage(true))),
                        new BidModifierMobile()
                                .withType(BidModifierType.MOBILE_MULTIPLIER)
                                .withMobileAdjustment(new BidModifierMobileAdjustment()
                                        .withOsType(OsType.ANDROID)
                                        .withIsRequiredInPricePackage(true))
                ))
                .withAllowedCreativeTemplates(DEFAULT_ALLOWED_CREATIVE_TYPES)
                .withProductId(0L)
                .withPriceMarkups(emptyList())
                .withCampaignOptions(new PricePackageCampaignOptions()
                        .withAllowBrandSafety(true)
                        .withAllowBrandLift(false)
                        .withAllowDisabledPlaces(false)
                        .withAllowDisabledVideoPlaces(false)
                        .withAllowImage(false)
                        .withAllowGallery(false)
                        .withShowsFrequencyLimit(
                                new ShowsFrequencyLimit()
                                        .withFrequencyLimit(5)
                                        .withFrequencyLimitIsForCampaignTime(true))
                )
                .withCategoryId(1L);
    }

    public static PricePackage anotherPricePackage() {
        return new PricePackage()
                // packageId не указываем, будет назначен базой
                .withTitle("Another Package")
                .withTrackerUrl("http://yandex.ru")
                .withPrice(BigDecimal.valueOf(0.99))
                .withCurrency(CurrencyCode.USD)
                .withOrderVolumeMin(111L)
                .withOrderVolumeMax(111L)
                .withTargetingsFixed(new TargetingsFixed()
                        .withViewTypes(List.of(ViewType.MOBILE))
                        .withAllowExpandedDesktopCreative(false)
                        .withCryptaSegments(emptyList())
                        .withAllowPremiumDesktopCreative(false)
                        .withHideIncomeSegment(false))
                .withTargetingsCustom(new TargetingsCustom()
                        .withRetargetingCondition(
                                new PriceRetargetingCondition()
                                        .withAllowAudienceSegments(true)
                                        .withAllowMetrikaSegments(false)
                                        .withLowerCryptaTypesCount(1)
                                        .withUpperCryptaTypesCount(3)
                                        .withCryptaSegments(emptyList())
                        )
                        .withGeo(DEFAULT_GEO)
                        .withGeoType(DEFAULT_GEO_TYPE)
                        .withGeoExpanded(DEFAULT_GEO_EXPANDED))
                .withTargetingMarkups(emptyList())
                .withStatusApprove(StatusApprove.WAITING)
                .withLastUpdateTime(LocalDateTime.parse("2029-08-09T01:23:45"))
                .withDateStart(LocalDate.of(2030, 1, 1))
                .withDateEnd(LocalDate.of(2030, 1, 1))
                .withIsPublic(false)
                .withIsSpecial(false)
                .withIsCpd(false)
                .withIsFrontpage(false)
                .withIsArchived(false)
                .withClients(emptyList())
                .withAllowedPageIds(List.of(3L))
                .withAllowedDomains(ImmutableList.of("vk.ru"))
                .withAllowedSsp(List.of("MoPub"))
                .withCampaignAutoApprove(false)
                .withIsDraftApproveAllowed(false)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withBidModifiers(List.of(new BidModifierInventory()
                        .withType(BidModifierType.INVENTORY_MULTIPLIER)
                        .withInventoryAdjustments(List.of(
                                new BidModifierInventoryAdjustment()
                                        .withInventoryType(InventoryType.INBANNER)
                                        .withIsRequiredInPricePackage(true)
                        ))))
                .withCampaignOptions(new PricePackageCampaignOptions()
                        .withAllowBrandLift(false)
                        .withAllowBrandSafety(false)
                        .withAllowDisabledPlaces(false)
                        .withAllowImage(false)
                        .withAllowGallery(false)
                        .withAllowDisabledVideoPlaces(true))
                .withAllowedCreativeTemplates(DEFAULT_ALLOWED_CREATIVE_TYPES)
                .withProductId(0L)
                .withPriceMarkups(emptyList())
                .withCategoryId(1L);
    }

    public static PricePackage approvedPricePackage() {
        return new PricePackage()
                .withTitle("Approved Package")
                .withTrackerUrl("http://yayaya.ru")
                .withPrice(BigDecimal.valueOf(12.34))
                .withCurrency(CurrencyCode.EUR)
                .withOrderVolumeMin(22L)
                .withOrderVolumeMax(1000L)
                .withTargetingsFixed(new TargetingsFixed()
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE))
                        .withAllowExpandedDesktopCreative(true)
                        .withCryptaSegments(emptyList())
                        .withAllowPremiumDesktopCreative(false)
                        .withHideIncomeSegment(false))
                .withTargetingsCustom(new TargetingsCustom()
                        .withGeo(DEFAULT_GEO)
                        .withGeoType(DEFAULT_GEO_TYPE)
                        .withGeoExpanded(DEFAULT_GEO_EXPANDED)
                        .withRetargetingCondition(DEFAULT_RETARGETING_CONDITION))
                .withTargetingMarkups(emptyList())
                .withStatusApprove(StatusApprove.YES)
                .withLastUpdateTime(LocalDateTime.parse("2019-08-09T00:11:05"))
                .withDateStart(LocalDate.of(2030, 1, 1))
                .withDateEnd(LocalDate.of(2030, 1, 1))
                .withIsPublic(false)
                .withIsSpecial(false)
                .withIsCpd(false)
                .withIsFrontpage(true)
                .withIsArchived(false)
                .withAllowedCreativeTemplates(DEFAULT_ALLOWED_CREATIVE_TYPES)
                .withProductId(0L)
                .withPriceMarkups(emptyList())
                .withBidModifiers(emptyList())
                .withAllowedPageIds(emptyList())
                .withCampaignAutoApprove(false)
                .withIsDraftApproveAllowed(false)
                .withCampaignOptions(new PricePackageCampaignOptions()
                        .withAllowBrandLift(false)
                        .withAllowBrandSafety(false)
                        .withAllowImage(false)
                        .withAllowGallery(false)
                        .withAllowDisabledPlaces(true)
                        .withAllowDisabledVideoPlaces(true))
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE))
                .withClients(emptyList())
                .withCategoryId(1L);
    }

    public static PricePackage pricePackageForFrontendTestSteps(@Nullable Long clientId,
                                                                Boolean allowExpandedDesktopCreative,
                                                                StatusApprove statusApprove,
                                                                Set<AdGroupType> availableAdGroupTypes,
                                                                Boolean isDraftApproveAllowed) {

        return defaultPricePackage()
                .withTitle("Created by PricePackageStepsService")
                .withPrice(BigDecimal.valueOf(2999, 2))
                .withCurrency(CurrencyCode.RUB)
                .withOrderVolumeMin(1L)
                .withOrderVolumeMax(2000L)
                .withTargetingsFixed(new TargetingsFixed()
                        .withGeo(List.of(Region.RUSSIA_REGION_ID))
                        .withGeoType(REGION_TYPE_COUNTRY)
                        .withGeoExpanded(List.of(Region.RUSSIA_REGION_ID))
                        .withViewTypes(Arrays.asList(ViewType.values()))
                        .withAllowExpandedDesktopCreative(allowExpandedDesktopCreative)
                        .withCryptaSegments(emptyList()))
                .withTargetingsCustom(new TargetingsCustom()
                      .withRetargetingCondition(DEFAULT_RETARGETING_CONDITION
                                .withCryptaSegments(List.of(AGE_18_24, AGE_25_34, AGE_35_44, MALE_CRYPTA_GOAL_ID,
                                        MID_INCOME_GOAL_ID, LTV))))
                .withStatusApprove(statusApprove)
                .withLastUpdateTime(LocalDateTime.now(ZoneOffset.UTC))
                .withDateStart(LocalDate.now())
                .withDateEnd(LocalDate.now().plusYears(2))
                .withIsPublic(false)
                .withIsSpecial(false)
                .withIsArchived(false)
                .withAvailableAdGroupTypes(availableAdGroupTypes)
                .withCampaignAutoApprove(false)
                .withIsDraftApproveAllowed(isDraftApproveAllowed)
                .withAllowedPageIds(emptyList())
                .withCampaignOptions(null)
                .withBidModifiers(emptyList())
                .withProductId(0L)
                .withClients(clientId == null ?
                        List.of() :
                        List.of(new PricePackageClient()
                                .withClientId(clientId)
                                .withIsAllowed(true)))
                .withCategoryId(1L);
    }

    public static TargetingsCustom emptyTargetingsCustom() {
        return new TargetingsCustom();
    }

    public static PricePackageClient allowedPricePackageClient(ClientInfo client) {
        return allowedPricePackageClient(client.getClientId().asLong());
    }

    public static PricePackageClient allowedPricePackageClient(Long clientId) {
        return new PricePackageClient()
                .withClientId(clientId)
                .withIsAllowed(true);
    }

    public static PricePackageClient disallowedPricePackageClient(ClientInfo client) {
        return disallowedPricePackageClient(client.getClientId().asLong());
    }

    public static PricePackageClient disallowedPricePackageClient(Long clientId) {
        return new PricePackageClient()
                .withClientId(clientId)
                .withIsAllowed(false);
    }

    public static List<Goal> testPackagesGoals() {
        List<Goal> goalsToAdd = new ArrayList<>();
        Goal gender = defaultGoalWithId(AudienceType.GENDER.getTypedValue(), GoalType.SOCIAL_DEMO);
        gender.setName("gender");
        gender.setTankerNameKey("gender");
        goalsToAdd.add(gender);
        for (Long id : List.of(MALE_CRYPTA_GOAL_ID, FEMALE_CRYPTA_GOAL_ID, MID_INCOME_GOAL_ID)) {
            Goal goal = defaultGoalWithId(id, GoalType.SOCIAL_DEMO);
            goal.setParentId(AudienceType.GENDER.getTypedValue());
            goal.setName("name" + id);
            goal.setTankerNameKey("name" + id);
            goalsToAdd.add(goal);
        }
        return goalsToAdd;
    }

    public static PricePackage frontpageVideoPackage(ClientInfo clientInfo) {
        PricePackage pricePackage = defaultPricePackage()
                .withIsFrontpage(true)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        pricePackage.getTargetingsFixed().withViewTypes(List.of(ViewType.DESKTOP));
        pricePackage.getCampaignOptions().setAllowImage(true);
        pricePackage.setAllowedCreativeTemplates(Map.of(CPM_VIDEO_CREATIVE, List.of(406L)));
        return pricePackage;
    }
}

package ru.yandex.direct.core.testing.data;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import ru.yandex.direct.core.entity.bidmodifier.AbstractBidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BannerType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegmentAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierBannerType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierBannerTypeAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopOnly;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopOnlyAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierExpressionAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventory;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgoAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPrismaIncomeGrade;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPrismaIncomeGradeAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingFilter;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingFilterAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTV;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTVAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTablet;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPosition;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPositionAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTraffic;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafficAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideoAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeather;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherLiteral;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.bidmodifier.OperationType;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.entity.bidmodifier.TrafaretPosition;
import ru.yandex.direct.core.entity.bidmodifier.WeatherType;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionLiteral;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionParameter;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.utils.FunctionalUtils;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class TestBidModifiers {
    public static final Integer DEFAULT_PERCENT = 110;

    public static final int PERCENT_MAX = 1300;
    public static final int PERCENT_MIN = 0;
    public static final int PERCENT_MOBILE_VIDEO_MIN = 50;
    public static final int PERCENT_GEO_MIN = 10;

    public static BidModifierMobile createEmptyMobileModifier() {
        return new BidModifierMobile()
                .withType(BidModifierType.MOBILE_MULTIPLIER)
                .withEnabled(true)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierTablet createEmptyTabletModifier() {
        return new BidModifierTablet()
                .withType(BidModifierType.TABLET_MULTIPLIER)
                .withEnabled(false)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierMobile createEmptyClientMobileModifier() {
        return new BidModifierMobile()
                .withType(BidModifierType.MOBILE_MULTIPLIER);
    }

    public static BidModifierDesktop createEmptyDesktopModifier() {
        return new BidModifierDesktop()
                .withType(BidModifierType.DESKTOP_MULTIPLIER)
                .withEnabled(true)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierDesktopOnly createEmptyDesktopOnlyModifier() {
        return new BidModifierDesktopOnly()
                .withType(BidModifierType.DESKTOP_ONLY_MULTIPLIER)
                .withEnabled(false)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierDesktop createEmptyClientDesktopModifier() {
        return new BidModifierDesktop()
                .withType(BidModifierType.DESKTOP_MULTIPLIER);
    }

    public static BidModifierSmartTV createEmptySmartTVModifier() {
        return new BidModifierSmartTV()
                .withType(BidModifierType.SMARTTV_MULTIPLIER)
                .withEnabled(true)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierSmartTV createEmptyClientSmartTVModifier() {
        return new BidModifierSmartTV()
                .withType(BidModifierType.SMARTTV_MULTIPLIER);
    }

    public static BidModifierVideo createEmptyVideoModifier() {
        return new BidModifierVideo()
                .withType(BidModifierType.VIDEO_MULTIPLIER)
                .withEnabled(true)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierVideo createEmptyClientVideoModifier() {
        return new BidModifierVideo()
                .withType(BidModifierType.VIDEO_MULTIPLIER);
    }

    public static BidModifierPerformanceTgo createEmptyPerformanceTgoModifier() {
        return new BidModifierPerformanceTgo()
                .withType(BidModifierType.PERFORMANCE_TGO_MULTIPLIER)
                .withEnabled(true)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierPerformanceTgo createEmptyClientPerformanceTgoModifier() {
        return new BidModifierPerformanceTgo()
                .withType(BidModifierType.PERFORMANCE_TGO_MULTIPLIER);
    }

    public static BidModifierRetargeting createEmptyRetargetingModifier() {
        return new BidModifierRetargeting()
                .withType(BidModifierType.RETARGETING_MULTIPLIER)
                .withEnabled(true)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierRetargeting createEmptyClientRetargetingModifier() {
        return new BidModifierRetargeting()
                .withType(BidModifierType.RETARGETING_MULTIPLIER);
    }

    public static BidModifierRetargetingFilter createEmptyRetargetingFilterModifier() {
        return new BidModifierRetargetingFilter()
                .withType(BidModifierType.RETARGETING_FILTER)
                .withEnabled(true)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierGeo createEmptyGeoModifier() {
        return new BidModifierGeo()
                .withType(BidModifierType.GEO_MULTIPLIER)
                .withEnabled(true)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierGeo createEmptyClientGeoModifier() {
        return new BidModifierGeo()
                .withType(BidModifierType.GEO_MULTIPLIER);
    }

    public static BidModifierWeather createEmptyWeatherModifier() {
        return new BidModifierWeather()
                .withType(BidModifierType.WEATHER_MULTIPLIER)
                .withEnabled(true)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierWeather createEmptyClientWeatherModifier() {
        return new BidModifierWeather()
                .withType(BidModifierType.WEATHER_MULTIPLIER);
    }

    public static BidModifierDemographics createEmptyDemographicsModifier() {
        return new BidModifierDemographics()
                .withType(BidModifierType.DEMOGRAPHY_MULTIPLIER)
                .withEnabled(true)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierDemographics createEmptyClientDemographicsModifier() {
        return new BidModifierDemographics()
                .withType(BidModifierType.DEMOGRAPHY_MULTIPLIER);
    }

    public static BidModifierABSegment createEmptyABSegmentModifier() {
        return new BidModifierABSegment()
                .withType(BidModifierType.AB_SEGMENT_MULTIPLIER)
                .withEnabled(true)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierABSegment createEmptyClientABSegmentModifier() {
        return new BidModifierABSegment()
                .withType(BidModifierType.AB_SEGMENT_MULTIPLIER);
    }

    public static BidModifierBannerType createEmptyBannerTypeModifier() {
        return new BidModifierBannerType()
                .withType(BidModifierType.BANNER_TYPE_MULTIPLIER)
                .withEnabled(true)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierBannerType createEmptyClientBannerTypeModifier() {
        return new BidModifierBannerType()
                .withType(BidModifierType.BANNER_TYPE_MULTIPLIER);
    }

    public static BidModifierInventory createEmptyInventoryModifier() {
        return new BidModifierInventory()
                .withType(BidModifierType.INVENTORY_MULTIPLIER)
                .withEnabled(true)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierInventory createEmptyClientInventoryModifier() {
        return new BidModifierInventory()
                .withType(BidModifierType.INVENTORY_MULTIPLIER);
    }

    public static BidModifierTrafaretPosition createEmptyTrafaretPositionModifier() {
        return new BidModifierTrafaretPosition()
                .withType(BidModifierType.TRAFARET_POSITION_MULTIPLIER)
                .withEnabled(true)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierTrafaretPosition createDefaultTrafaretPositionModifier() {
        return createEmptyTrafaretPositionModifier().withTrafaretPositionAdjustments(List.of(createTrafaretPositionAdjustment(TrafaretPosition.ALONE)));
    }

    public static BidModifierTrafaretPosition createEmptyClientTrafaretPositionModifier() {
        return new BidModifierTrafaretPosition()
                .withType(BidModifierType.TRAFARET_POSITION_MULTIPLIER);
    }

    public static BidModifierMobileAdjustment createDefaultMobileAdjustment() {
        return new BidModifierMobileAdjustment().withPercent(DEFAULT_PERCENT).withLastChange(LocalDateTime.now());
    }

    public static BidModifierMobileAdjustment createDefaultClientMobileAdjustment() {
        return new BidModifierMobileAdjustment().withPercent(DEFAULT_PERCENT);
    }

    public static BidModifierDesktopAdjustment createDefaultDesktopAdjustment() {
        return new BidModifierDesktopAdjustment().withPercent(DEFAULT_PERCENT);
    }

    public static BidModifierDesktopOnlyAdjustment createDefaultDesktopOnlyAdjustment() {
        return new BidModifierDesktopOnlyAdjustment().withPercent(DEFAULT_PERCENT);
    }

    public static BidModifierSmartTVAdjustment createDefaultSmartTVAdjustment() {
        return new BidModifierSmartTVAdjustment().withPercent(DEFAULT_PERCENT);
    }

    public static BidModifierVideoAdjustment createDefaultVideoAdjustment() {
        return new BidModifierVideoAdjustment().withPercent(110);
    }

    public static BidModifierPerformanceTgoAdjustment createDefaultPerformanceTgoAdjustment() {
        return new BidModifierPerformanceTgoAdjustment().withPercent(150);
    }

    public static List<BidModifierRegionalAdjustment> createDefaultGeoAdjustments() {
        return Collections.singletonList(createDefaultGeoAdjustment());
    }

    public static List<BidModifierRegionalAdjustment> createDefaultClientGeoAdjustments() {
        return Collections.singletonList(createDefaultClientGeoAdjustment());
    }

    public static BidModifierRegionalAdjustment createDefaultGeoAdjustment() {
        return new BidModifierRegionalAdjustment().withRegionId(Region.RUSSIA_REGION_ID)
                .withHidden(false)
                .withPercent(TestBidModifiers.DEFAULT_PERCENT)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierRegionalAdjustment createDefaultClientGeoAdjustment() {
        return new BidModifierRegionalAdjustment().withRegionId(Region.RUSSIA_REGION_ID)
                .withHidden(false)
                .withPercent(TestBidModifiers.DEFAULT_PERCENT);
    }

    public static List<AbstractBidModifierRetargetingAdjustment> createDefaultRetargetingAdjustments(long retCondId) {
        return Collections.singletonList(createDefaultRetargetingAdjustment(retCondId));
    }

    public static List<AbstractBidModifierRetargetingAdjustment> createDefaultRetargetingAdjustments(
            List<Long> retCondIds) {
        return FunctionalUtils.mapList(retCondIds, TestBidModifiers::createDefaultRetargetingAdjustment);
    }

    public static List<AbstractBidModifierRetargetingAdjustment> createDefaultClientRetargetingAdjustments(long retCondId) {
        return Collections.singletonList(createDefaultClientRetargetingAdjustment(retCondId));
    }

    public static BidModifierRetargetingAdjustment createDefaultRetargetingAdjustment(long retCondId) {
        return (BidModifierRetargetingAdjustment)
                addCommonAttributesToRetargetingAdjustment(retCondId, new BidModifierRetargetingAdjustment());
    }

    public static List<AbstractBidModifierRetargetingAdjustment> createDefaultRetargetingFilterAdjustments(long retCondId) {
        return Collections.singletonList(createDefaultRetargetingFilterAdjustment(retCondId));
    }

    public static List<AbstractBidModifierRetargetingAdjustment> createDefaultRetargetingFilterAdjustments(List<Long> retCondIds) {
        return mapList(retCondIds, TestBidModifiers::createDefaultRetargetingFilterAdjustment);
    }

    public static BidModifierRetargetingFilterAdjustment createDefaultRetargetingFilterAdjustment(long retCondId) {
        return (BidModifierRetargetingFilterAdjustment)
                addCommonAttributesToRetargetingAdjustment(retCondId, new BidModifierRetargetingFilterAdjustment())
                        .withPercent(0);
    }

    private static AbstractBidModifierRetargetingAdjustment addCommonAttributesToRetargetingAdjustment(
            long retCondId,
            AbstractBidModifierRetargetingAdjustment adjustment
    ) {
        return adjustment
                .withAccessible(true)
                .withRetargetingConditionId(retCondId)
                .withPercent(110)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierRetargetingAdjustment createDefaultClientRetargetingAdjustment(long retCondId) {
        return new BidModifierRetargetingAdjustment()
                .withAccessible(true)
                .withRetargetingConditionId(retCondId)
                .withPercent(110);
    }

    public static List<BidModifierBannerTypeAdjustment> createDefaultBannerTypeAdjustments() {
        return Collections.singletonList(createDefaultBannerTypeAdjustment());
    }

    public static List<BidModifierBannerTypeAdjustment> createDefaultClientBannerTypeAdjustments() {
        return Collections.singletonList(createDefaultClientBannerTypeAdjustment());
    }

    public static BidModifierBannerTypeAdjustment createDefaultBannerTypeAdjustment() {
        return new BidModifierBannerTypeAdjustment()
                .withPercent(TestBidModifiers.DEFAULT_PERCENT)
                .withBannerType(BannerType.CPM_BANNER)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierBannerTypeAdjustment createDefaultClientBannerTypeAdjustment() {
        return new BidModifierBannerTypeAdjustment()
                .withPercent(TestBidModifiers.DEFAULT_PERCENT)
                .withBannerType(BannerType.CPM_BANNER);
    }

    public static BidModifierInventoryAdjustment createDefaultInventoryAdjustment() {
        return createInventoryAdjustment(
                InventoryType.values()[RandomNumberUtils.nextPositiveInteger() % InventoryType.values().length]
        );

    }

    public static BidModifierInventoryAdjustment createInventoryAdjustment(InventoryType inventoryType) {
        return new BidModifierInventoryAdjustment()
                .withPercent(DEFAULT_PERCENT)
                .withInventoryType(inventoryType)
                .withLastChange(LocalDateTime.now());
    }


    public static BidModifierInventoryAdjustment createDefaultClientInventoryAdjustment() {
        return new BidModifierInventoryAdjustment()
                .withPercent(DEFAULT_PERCENT)
                .withInventoryType(
                        InventoryType.values()[RandomNumberUtils.nextPositiveInteger() % InventoryType
                                .values().length]);
    }

    public static BidModifierTrafaretPositionAdjustment createDefaultTrafaretPositionAdjustment() {
        return new BidModifierTrafaretPositionAdjustment()
                .withPercent(DEFAULT_PERCENT)
                .withTrafaretPosition(
                        TrafaretPosition.values()[RandomNumberUtils.nextPositiveInteger() % TrafaretPosition.values().length])
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierTrafaretPositionAdjustment createTrafaretPositionAdjustment(TrafaretPosition trafaretPosition) {
        return new BidModifierTrafaretPositionAdjustment()
                .withPercent(DEFAULT_PERCENT)
                .withTrafaretPosition(trafaretPosition)
                .withLastChange(LocalDateTime.now());
    }

    public static List<BidModifierABSegmentAdjustment> createDefaultABSegmentsAdjustments(
            RetConditionInfo... retConditionInfos) {
        return mapList(Arrays.asList(retConditionInfos), TestBidModifiers::createDefaultABSegmentsAdjustment);
    }

    public static List<BidModifierABSegmentAdjustment> createDefaultClientABSegmentsAdjustments(
            RetConditionInfo... retConditionInfos) {
        return mapList(Arrays.asList(retConditionInfos), TestBidModifiers::createDefaultClientABSegmentsAdjustment);
    }

    public static BidModifierABSegmentAdjustment createDefaultABSegmentsAdjustment(RetConditionInfo retConditionInfo) {
        return new BidModifierABSegmentAdjustment()
                .withAccessible(true)
                .withAbSegmentRetargetingConditionId(retConditionInfo.getRetConditionId())
                .withPercent(110)
                .withSectionId(retConditionInfo.getRetCondition().getRules().get(0).getSectionId())
                .withSegmentId(retConditionInfo.getRetCondition().getRules().get(0).getGoals().get(0).getId())
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierABSegmentAdjustment createDefaultClientABSegmentsAdjustment(RetConditionInfo retConditionInfo) {
        return new BidModifierABSegmentAdjustment()
                .withAccessible(true)
                .withAbSegmentRetargetingConditionId(retConditionInfo.getRetConditionId())
                .withPercent(110)
                .withSectionId(retConditionInfo.getRetCondition().getRules().get(0).getSectionId())
                .withSegmentId(retConditionInfo.getRetCondition().getRules().get(0).getGoals().get(0).getId());
    }

    public static BidModifierTraffic createEmptyTrafficModifier() {
        return new BidModifierTraffic()
                .withType(BidModifierType.EXPRESS_TRAFFIC_MULTIPLIER)
                .withEnabled(true);
    }

    public static BidModifierTraffic createDefaultTrafficModifier() {
        return createEmptyTrafficModifier()
                .withExpressionAdjustments(createDefaultTrafficAdjustments());
    }

    public static List<BidModifierExpressionAdjustment> createDefaultTrafficAdjustments() {
        return Collections.singletonList(createDefaultTrafficAdjustment());
    }

    public static BidModifierExpressionAdjustment createDefaultTrafficAdjustment() {
        return new BidModifierTrafficAdjustment()
                .withCondition(createDefaultTrafficExpressions())
                .withPercent(110)
                .withLastChange(LocalDateTime.now());
    }

    private static List<List<BidModifierExpressionLiteral>> createDefaultTrafficExpressions() {
        return Collections.singletonList(Collections.singletonList(
                new BidModifierExpressionLiteral()
                        .withParameter(BidModifierExpressionParameter.TRAFFIC_JAM)
                        .withOperation(BidModifierExpressionOperator.EQ)
                        .withValueString("1")
        ));
    }

    public static BidModifierPrismaIncomeGrade createEmptyIncomeGradeModifier() {
        return new BidModifierPrismaIncomeGrade()
                .withType(BidModifierType.PRISMA_INCOME_GRADE_MULTIPLIER)
                .withEnabled(true);
    }

    public static BidModifierPrismaIncomeGrade createDefaultIncomeGradeModifier() {
        return createEmptyIncomeGradeModifier()
                .withExpressionAdjustments(createDefaultIncomeGradeAdjustments());
    }

    public static List<BidModifierExpressionAdjustment> createDefaultIncomeGradeAdjustments() {
        return Collections.singletonList(createDefaultIncomeGradeAdjustment());
    }

    public static BidModifierExpressionAdjustment createDefaultIncomeGradeAdjustment() {
        return new BidModifierPrismaIncomeGradeAdjustment()
                .withCondition(createDefaultIncomeGradeExpressions())
                .withPercent(110)
                .withLastChange(LocalDateTime.now());
    }

    private static List<List<BidModifierExpressionLiteral>> createDefaultIncomeGradeExpressions() {
        return Collections.singletonList(Collections.singletonList(
                new BidModifierExpressionLiteral()
                        .withParameter(BidModifierExpressionParameter.PRISMA_INCOME_GRADE)
                        .withOperation(BidModifierExpressionOperator.EQ)
                        .withValueString("1")
        ));
    }

    public static List<BidModifierWeatherAdjustment> createDefaultWeatherAdjustments() {
        return Collections.singletonList(createDefaultWeatherAdjustment());
    }

    public static List<BidModifierWeatherAdjustment> createDefaultClientWeatherAdjustments() {
        return Collections.singletonList(createDefaultClientWeatherAdjustment());
    }

    public static BidModifierWeatherAdjustment createDefaultWeatherAdjustment() {
        return new BidModifierWeatherAdjustment()
                .withExpression(createDefaultWeatherExpressions())
                .withPercent(110)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierWeatherAdjustment createDefaultClientWeatherAdjustment() {
        return new BidModifierWeatherAdjustment()
                .withExpression(createDefaultWeatherExpressions())
                .withPercent(110);
    }

    public static List<List<BidModifierWeatherLiteral>> createDefaultWeatherExpressions() {
        return Collections.singletonList(Collections.singletonList(createDefaultWeatherExpression()));
    }

    public static BidModifierWeatherLiteral createDefaultWeatherExpression() {
        return new BidModifierWeatherLiteral()
                .withParameter(WeatherType.CLOUDNESS)
                .withOperation(OperationType.EQ)
                .withValue(50);
    }

    public static List<BidModifierDemographicsAdjustment> createDefaultDemographicsAdjustments() {
        return Collections.singletonList(createDefaultDemographicsAdjustment());
    }

    public static List<BidModifierDemographicsAdjustment> createMultipleDemographicsAdjustments() {
        return List.of(createDefaultDemographicsAdjustment(), createAnotherDemographicsAdjustment());
    }

    public static List<BidModifierDemographicsAdjustment> createAnotherDemographicsAdjustments() {
        return Collections.singletonList(createAnotherDemographicsAdjustment());
    }

    public static List<BidModifierDemographicsAdjustment> createDefaultClientDemographicsAdjustments() {
        return Collections.singletonList(createDefaultClientDemographicsAdjustment());
    }

    public static BidModifierDemographicsAdjustment createDefaultDemographicsAdjustment() {
        return new BidModifierDemographicsAdjustment()
                .withAge(AgeType._25_34)
                .withPercent(110)
                .withGender(GenderType.MALE)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierDemographicsAdjustment createAnotherDemographicsAdjustment() {
        return new BidModifierDemographicsAdjustment()
                .withAge(AgeType._18_24)
                .withPercent(120)
                .withGender(GenderType.FEMALE)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierDemographicsAdjustment createDemographicsAdjustmentWithUnsupportedAgeType() {
        return new BidModifierDemographicsAdjustment()
                .withAge(AgeType._45_)
                .withPercent(130)
                .withGender(GenderType.FEMALE)
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierDemographicsAdjustment createDefaultClientDemographicsAdjustment() {
        return new BidModifierDemographicsAdjustment()
                .withAge(AgeType._25_34)
                .withPercent(DEFAULT_PERCENT)
                .withGender(GenderType.MALE);
    }

    public static BidModifierMobile createDefaultBidModifierMobile(Long campaignId) {
        return createEmptyMobileModifier().withCampaignId(campaignId)
                .withMobileAdjustment(createDefaultMobileAdjustment());
    }

    public static BidModifierMobile createDefaultClientBidModifierMobile(Long campaignId) {
        return createEmptyClientMobileModifier().withCampaignId(campaignId)
                .withMobileAdjustment(createDefaultClientMobileAdjustment());
    }

    public static BidModifierMobile createDefaultIosBidModifierMobile(Long campaignId) {
        return createEmptyMobileModifier().withCampaignId(campaignId)
                .withMobileAdjustment(createDefaultMobileAdjustment().withOsType(OsType.IOS));
    }

    public static BidModifierMobile createDefaultClientIosBidModifierMobile(Long campaignId) {
        return createEmptyClientMobileModifier().withCampaignId(campaignId)
                .withMobileAdjustment(createDefaultClientMobileAdjustment().withOsType(OsType.IOS));
    }

    public static BidModifierDesktop createDefaultBidModifierDesktop(Long campaignId) {
        return createEmptyDesktopModifier().withCampaignId(campaignId)
                .withDesktopAdjustment(createDefaultDesktopAdjustment());
    }

    public static BidModifierPerformanceTgo createDefaultBidModifierPerformanceTgo(Long campaignId) {
        return createEmptyPerformanceTgoModifier().withCampaignId(campaignId)
                .withPerformanceTgoAdjustment(createDefaultPerformanceTgoAdjustment());
    }

    public static BidModifierSmartTV createDefaultBidModifierSmartTV(Long campaignId) {
        return createEmptySmartTVModifier().withCampaignId(campaignId)
                .withSmartTVAdjustment(createDefaultSmartTVAdjustment());
    }

    public static BidModifierDesktopOnly createDefaultBidModifierDesktopOnly(Long campaignId) {
        return createEmptyDesktopOnlyModifier().withCampaignId(campaignId)
                .withDesktopOnlyAdjustment(createDefaultDesktopOnlyAdjustment());
    }

    public static BidModifierDesktop createDefaultClientBidModifierDesktop(Long campaignId) {
        return createEmptyClientDesktopModifier().withCampaignId(campaignId)
                .withDesktopAdjustment(createDefaultDesktopAdjustment());
    }

    public static BidModifierSmartTV createDefaultClientBidModifierSmartTV(Long campaignId) {
        return createEmptyClientSmartTVModifier().withCampaignId(campaignId)
                .withSmartTVAdjustment(createDefaultSmartTVAdjustment());
    }

    public static BidModifierGeo createDefaultBidModifierGeo(Long campaignId) {
        return createEmptyGeoModifier().withCampaignId(campaignId)
                .withRegionalAdjustments(createDefaultGeoAdjustments());
    }

    public static BidModifierGeo createDefaultClientBidModifierGeo(Long campaignId) {
        return createEmptyClientGeoModifier().withCampaignId(campaignId)
                .withRegionalAdjustments(createDefaultClientGeoAdjustments());
    }

    public static BidModifierWeather createDefaultBidModifierWeather(Long campaignId) {
        return createEmptyWeatherModifier().withCampaignId(campaignId)
                .withWeatherAdjustments(createDefaultWeatherAdjustments());
    }

    public static BidModifierWeather createDefaultClientBidModifierWeather(Long campaignId) {
        return createEmptyClientWeatherModifier().withCampaignId(campaignId)
                .withWeatherAdjustments(createDefaultClientWeatherAdjustments());
    }

    public static BidModifierDemographics createDefaultBidModifierDemographics(Long campaignId) {
        return createEmptyDemographicsModifier().withCampaignId(campaignId)
                .withDemographicsAdjustments(createDefaultDemographicsAdjustments());
    }

    public static BidModifierDemographics createBidModifierDemographicsWithTwoAdjustments(Long campaignId) {
        return createEmptyDemographicsModifier().withCampaignId(campaignId)
                .withDemographicsAdjustments(createMultipleDemographicsAdjustments());
    }

    public static BidModifierDemographics createAnotherBidModifierDemographics(Long campaignId) {
        return createEmptyDemographicsModifier().withCampaignId(campaignId)
                .withDemographicsAdjustments(createAnotherDemographicsAdjustments());
    }

    public static BidModifierDemographics createBidModifierDemographicsWithUnsupportedAgeType(Long campaignId) {
        return createEmptyDemographicsModifier().withCampaignId(campaignId)
                .withDemographicsAdjustments(List.of(createDemographicsAdjustmentWithUnsupportedAgeType()));
    }

    public static BidModifierDemographics createDefaultClientBidModifierDemographics(Long campaignId) {
        return createEmptyClientDemographicsModifier().withCampaignId(campaignId)
                .withDemographicsAdjustments(createDefaultClientDemographicsAdjustments());
    }

    public static BidModifierRetargeting createDefaultBidModifierRetargeting(Long campaignId,
                                                                             Long adGroupId, Long retCondId) {
        return createEmptyRetargetingModifier()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withRetargetingAdjustments(createDefaultRetargetingAdjustments(retCondId));
    }

    public static BidModifierRetargeting createDefaultBidModifierRetargeting(Long campaignId,
                                                                             Long adGroupId,
                                                                             List<Long> retCondIds) {
        return createEmptyRetargetingModifier()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withRetargetingAdjustments(createDefaultRetargetingAdjustments(retCondIds));
    }

    public static BidModifierRetargetingFilter createDefaultBidModifierRetargetingFilter(Long campaignId,
                                                                                         Long adGroupId,
                                                                                         List<Long> retCondIds) {
        return createEmptyRetargetingFilterModifier()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withRetargetingAdjustments(createDefaultRetargetingFilterAdjustments(retCondIds));
    }

    public static BidModifierRetargeting createDefaultClientBidModifierRetargeting(Long campaignId,
                                                                                   Long adGroupId, Long retCondId) {
        return createEmptyClientRetargetingModifier()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withRetargetingAdjustments(createDefaultClientRetargetingAdjustments(retCondId));
    }

    public static BidModifierABSegment createDefaultBidModifierAbSegment(Long campaignId,
                                                                         Long adGroupId, RetConditionInfo retCondInfo) {
        return createEmptyABSegmentModifier()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withAbSegmentAdjustments(createDefaultABSegmentsAdjustments(retCondInfo));
    }

    public static BidModifierABSegment createDefaultClientBidModifierAbSegment(Long campaignId,
                                                                               Long adGroupId,
                                                                               RetConditionInfo retCondInfo) {
        return createEmptyClientABSegmentModifier()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withAbSegmentAdjustments(createDefaultClientABSegmentsAdjustments(retCondInfo));
    }

    public static BidModifierVideo createDefaultBidModifierVideo(Long campaignId) {
        return new BidModifierVideo()
                .withEnabled(true)
                .withCampaignId(campaignId)
                .withType(BidModifierType.VIDEO_MULTIPLIER)
                .withVideoAdjustment(createDefaultVideoAdjustment())
                .withLastChange(LocalDateTime.now());
    }

    public static BidModifierVideo createDefaultClientBidModifierVideo(Long campaignId) {
        return new BidModifierVideo()
                .withCampaignId(campaignId)
                .withType(BidModifierType.VIDEO_MULTIPLIER)
                .withVideoAdjustment(createDefaultVideoAdjustment());
    }

    public static BidModifierPerformanceTgo createDefaultPerformanceTgo(Long campaignId, Long adGroupId) {
        return new BidModifierPerformanceTgo()
                .withEnabled(true)
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withType(BidModifierType.PERFORMANCE_TGO_MULTIPLIER)
                .withPerformanceTgoAdjustment(createDefaultPerformanceTgoAdjustment());
    }

    public static BidModifierBannerType createDefaultBannerType(Long campaignId, Long adGroupId) {
        return new BidModifierBannerType()
                .withEnabled(true)
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withType(BidModifierType.BANNER_TYPE_MULTIPLIER)
                .withBannerTypeAdjustments(createDefaultBannerTypeAdjustments());
    }

    public static BidModifierBannerType createDefaultClientBannerType(Long campaignId, Long adGroupId) {
        return new BidModifierBannerType()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withType(BidModifierType.BANNER_TYPE_MULTIPLIER)
                .withBannerTypeAdjustments(createDefaultClientBannerTypeAdjustments());
    }

    public static ModelChanges<BidModifierAdjustment> getModelChangesForUpdate(
            Class<? extends BidModifierAdjustment> clazz, Long bmId, Integer newPercent) {
        ModelChanges<BidModifierAdjustment> modelChanges = new ModelChanges<>(bmId, clazz)
                .castModelUp(BidModifierAdjustment.class);
        modelChanges.process(newPercent, BidModifierAdjustment.PERCENT);
        return modelChanges;
    }

    public static ComplexBidModifier createComplexBidModifier(@Nullable Integer mobilePercent,
                                                              @Nullable Long retCondId,
                                                              @Nullable Integer retargetingPercent,
                                                              @Nullable Boolean retargetingEnabled,
                                                              @Nullable Integer performanceTgoPercent,
                                                              @Nullable Integer desktopPercent,
                                                              boolean isMobileWithOsType) {
        return createComplexBidModifier(mobilePercent, retCondId, retargetingPercent, retargetingEnabled,
                performanceTgoPercent, desktopPercent, isMobileWithOsType, null);
    }

    /**
     * Создаёт тестовую модель ComplexBidModifier с указанными значениями коэффициентов.
     * Если коэффициент = null, то корректировка соответствующего типа не добавляется.
     */
    public static ComplexBidModifier createComplexBidModifier(@Nullable Integer mobilePercent,
                                                              @Nullable Long retCondId,
                                                              @Nullable Integer retargetingPercent,
                                                              @Nullable Boolean retargetingEnabled,
                                                              @Nullable Integer performanceTgoPercent,
                                                              @Nullable Integer desktopPercent,
                                                              boolean isMobileWithOsType,
                                                              @Nullable Integer videoPercent) {
        ComplexBidModifier bidModifier = new ComplexBidModifier();
        if (mobilePercent != null) {
            bidModifier.withMobileModifier(
                    createEmptyClientMobileModifier()
                            .withMobileAdjustment(
                                    new BidModifierMobileAdjustment()
                                            .withPercent(mobilePercent)));
            if (isMobileWithOsType) {
                bidModifier.getMobileModifier().getMobileAdjustment().setOsType(OsType.IOS);
            }
        }
        if (retCondId != null && retargetingPercent != null && retargetingEnabled != null) {
            bidModifier.withRetargetingModifier(
                    new BidModifierRetargeting()
                            .withType(BidModifierType.RETARGETING_MULTIPLIER)
                            .withEnabled(retargetingEnabled)
                            .withRetargetingAdjustments(
                                    singletonList(
                                            new BidModifierRetargetingAdjustment()
                                                    .withRetargetingConditionId(retCondId)
                                                    .withPercent(retargetingPercent))));
        }
        if (performanceTgoPercent != null) {
            bidModifier.withPerformanceTgoModifier(
                    new BidModifierPerformanceTgo()
                            .withType(BidModifierType.PERFORMANCE_TGO_MULTIPLIER)
                            .withEnabled(true)
                            .withPerformanceTgoAdjustment(
                                    new BidModifierPerformanceTgoAdjustment()
                                            .withPercent(performanceTgoPercent)));
        }
        if (desktopPercent != null) {
            bidModifier.withDesktopModifier(
                    new BidModifierDesktop()
                            .withType(BidModifierType.DESKTOP_MULTIPLIER)
                            .withEnabled(true)
                            .withDesktopAdjustment(
                                    new BidModifierDesktopAdjustment().withPercent(desktopPercent)));
        }
        if (videoPercent != null) {
            bidModifier.withVideoModifier(
                    new BidModifierVideo()
                            .withType(BidModifierType.VIDEO_MULTIPLIER)
                            .withEnabled(true)
                            .withVideoAdjustment(
                                    new BidModifierVideoAdjustment().withPercent(videoPercent)));
        }
        return bidModifier;
    }
}

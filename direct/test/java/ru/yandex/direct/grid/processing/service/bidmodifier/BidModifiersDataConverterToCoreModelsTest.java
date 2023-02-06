package ru.yandex.direct.grid.processing.service.bidmodifier;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BannerType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegmentAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierBannerTypeAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgoAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTVAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPositionAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTraffic;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafficAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideoAdjustment;
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
import ru.yandex.direct.grid.processing.model.bidmodifier.GdAdjustmentAdType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdAgeType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdGenderType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdInventoryType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdOperationType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdOsType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdTrafaretPosition;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdWeatherType;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierABSegment;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierABSegmentAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierAdType;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierAdTypeAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDemographics;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDemographicsAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDesktop;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDesktopAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierExpress;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierExpressAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierExpressLiteral;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierGeo;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierInventory;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierInventoryAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierMobile;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierMobileAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierRegionalAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierRetargeting;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierRetargetingAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierSmartTV;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierSmartTVAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierSmartTgo;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierSmartTgoAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierTrafaretPosition;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierTrafaretPositionAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierVideo;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierVideoAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierWeather;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierWeatherAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierWeatherExpression;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientABSegmentModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientBannerTypeModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientDesktopModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientGeoModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientInventoryModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientMobileModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientPerformanceTgoModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientRetargetingModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientSmartTVModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientTrafaretPositionModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientVideoModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientWeatherModifier;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toComplexBidModifier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class BidModifiersDataConverterToCoreModelsTest {
    private static final long CAMPAIGN_ID = RandomNumberUtils.nextPositiveLong();
    private static final long AD_GROUP_ID = RandomNumberUtils.nextPositiveLong();
    private static final long RET_COND_ID = RandomNumberUtils.nextPositiveLong();
    private static final long REGION_ID = Region.RUSSIA_REGION_ID;
    private static final boolean ENABLED_1 = false;
    private static final boolean ENABLED_2 = true;
    private static final int PERCENT_1 = RandomNumberUtils.nextPositiveInteger() % 1000 + 50;
    private static final int PERCENT_2 = RandomNumberUtils.nextPositiveInteger() % 1000 + 50;
    private GdUpdateBidModifiers updateBidModifiers;
    private ComplexBidModifier expected;

    @Before
    public void init() {
        updateBidModifiers = new GdUpdateBidModifiers()
                .withBidModifierABSegment(new GdUpdateBidModifierABSegment()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(GdBidModifierType.AB_SEGMENT_MULTIPLIER)
                        .withEnabled(ENABLED_1)
                        .withAdjustments(Collections.singletonList(new GdUpdateBidModifierABSegmentAdjustmentItem()
                                .withAbSegmentRetargetingConditionId(RET_COND_ID)
                                .withPercent(PERCENT_1)))
                )
                .withBidModifierAdType(new GdUpdateBidModifierAdType()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(GdBidModifierType.AD_TYPE_MULTIPLIER)
                        .withEnabled(ENABLED_2)
                        .withAdjustments(Collections.singletonList(new GdUpdateBidModifierAdTypeAdjustmentItem()
                                .withAdType(GdAdjustmentAdType.CPM_BANNER)
                                .withPercent(PERCENT_2)))
                )
                .withBidModifierDemographics(new GdUpdateBidModifierDemographics()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(GdBidModifierType.DEMOGRAPHY_MULTIPLIER)
                        .withEnabled(ENABLED_1)
                        .withAdjustments(
                                Collections.singletonList(new GdUpdateBidModifierDemographicsAdjustmentItem()
                                        .withAge(GdAgeType._0_17)
                                        .withGender(GdGenderType.FEMALE)
                                        .withPercent(PERCENT_1)))
                )
                .withBidModifierDesktop(new GdUpdateBidModifierDesktop()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(GdBidModifierType.DESKTOP_MULTIPLIER)
                        .withEnabled(ENABLED_2)
                        .withAdjustment(new GdUpdateBidModifierDesktopAdjustmentItem()
                                .withPercent(PERCENT_2))
                )
                .withBidModifierSmartTV(new GdUpdateBidModifierSmartTV()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(GdBidModifierType.SMARTTV_MULTIPLIER)
                        .withEnabled(ENABLED_2)
                        .withAdjustment(new GdUpdateBidModifierSmartTVAdjustmentItem()
                                .withPercent(PERCENT_2))
                )
                .withBidModifierGeo(new GdUpdateBidModifierGeo()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(GdBidModifierType.GEO_MULTIPLIER)
                        .withEnabled(ENABLED_1)
                        .withAdjustments(Collections.singletonList(new GdUpdateBidModifierRegionalAdjustmentItem()
                                .withHidden(false)
                                .withRegionId((int) REGION_ID)
                                .withPercent(PERCENT_1)))
                )
                .withBidModifierInventory(new GdUpdateBidModifierInventory()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(GdBidModifierType.INVENTORY_MULTIPLIER)
                        .withEnabled(ENABLED_2)
                        .withAdjustments(Collections.singletonList(new GdUpdateBidModifierInventoryAdjustmentItem()
                                .withInventoryType(GdInventoryType.INSTREAM_WEB)
                                .withPercent(PERCENT_2)))
                )
                .withBidModifierMobile(new GdUpdateBidModifierMobile()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_1)
                        .withType(GdBidModifierType.MOBILE_MULTIPLIER)
                        .withAdjustment(new GdUpdateBidModifierMobileAdjustmentItem()
                                .withOsType(GdOsType.IOS)
                                .withPercent(PERCENT_1))
                )
                .withBidModifierRetargeting(new GdUpdateBidModifierRetargeting()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(GdBidModifierType.RETARGETING_MULTIPLIER)
                        .withEnabled(ENABLED_2)
                        .withAdjustments(Collections.singletonList(new GdUpdateBidModifierRetargetingAdjustmentItem()
                                .withRetargetingConditionId(RET_COND_ID)
                                .withPercent(PERCENT_2)))
                )
                .withBidModifierSmartTgo(new GdUpdateBidModifierSmartTgo()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(GdBidModifierType.SMART_TGO_MULTIPLIER)
                        .withEnabled(ENABLED_1)
                        .withAdjustment(new GdUpdateBidModifierSmartTgoAdjustmentItem()
                                .withPercent(PERCENT_1))
                )
                .withBidModifierVideo(new GdUpdateBidModifierVideo()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_2)
                        .withType(GdBidModifierType.VIDEO_MULTIPLIER)
                        .withAdjustment(new GdUpdateBidModifierVideoAdjustmentItem()
                                .withPercent(PERCENT_2))
                )
                .withBidModifierWeather(new GdUpdateBidModifierWeather()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_1)
                        .withType(GdBidModifierType.WEATHER_MULTIPLIER)
                        .withAdjustments(Collections.singletonList(new GdUpdateBidModifierWeatherAdjustmentItem()
                                .withExpression(Collections.singletonList(
                                        Collections.singletonList(new GdUpdateBidModifierWeatherExpression()
                                                .withOperation(GdOperationType.EQ)
                                                .withParameter(GdWeatherType.CLOUDNESS)
                                                .withValue(PERCENT_2))))
                                .withPercent(PERCENT_1)))
                )
                .withBidModifierExpress(List.of(new GdUpdateBidModifierExpress()
                                .withType(GdBidModifierType.EXPRESS_TRAFFIC_MULTIPLIER)
                                .withCampaignId(CAMPAIGN_ID)
                                .withAdGroupId(AD_GROUP_ID)
                                .withEnabled(ENABLED_1)
                                .withAdjustments(List.of(new GdUpdateBidModifierExpressAdjustmentItem()
                                        .withPercent(PERCENT_1)
                                        .withCondition(List.of(List.of(new GdUpdateBidModifierExpressLiteral()
                                                .withParameter(BidModifierExpressionParameter.TRAFFIC_JAM)
                                                .withOperation(BidModifierExpressionOperator.EQ)
                                                .withValue("2")
                                        ))))),
                        new GdUpdateBidModifierExpress()
                                .withType(GdBidModifierType.PRISMA_INCOME_GRADE_MULTIPLIER)
                                .withCampaignId(CAMPAIGN_ID)
                                .withAdGroupId(AD_GROUP_ID)
                                .withEnabled(ENABLED_1)
                                .withAdjustments(List.of(new GdUpdateBidModifierExpressAdjustmentItem()
                                        .withPercent(PERCENT_1)
                                        .withCondition(List.of(List.of(new GdUpdateBidModifierExpressLiteral()
                                                .withParameter(BidModifierExpressionParameter.PRISMA_INCOME_GRADE)
                                                .withOperation(BidModifierExpressionOperator.EQ)
                                                .withValue("0")
                                        )))
                                ))
                        )
                )
                .withBidModifierTrafaretPosition(new GdUpdateBidModifierTrafaretPosition()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(GdBidModifierType.TRAFARET_POSITION_MULTIPLIER)
                        .withEnabled(ENABLED_2)
                        .withAdjustments(Collections.singletonList(new GdUpdateBidModifierTrafaretPositionAdjustmentItem()
                                .withTrafaretPosition(GdTrafaretPosition.ALONE)
                                .withPercent(PERCENT_2)))
                );

        expected = new ComplexBidModifier()
                .withAbSegmentModifier(createEmptyClientABSegmentModifier()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_1)
                        .withType(BidModifierType.AB_SEGMENT_MULTIPLIER)
                        .withAbSegmentAdjustments(Collections.singletonList(new BidModifierABSegmentAdjustment()
                                .withAbSegmentRetargetingConditionId(RET_COND_ID)
                                .withPercent(PERCENT_1)))
                )
                .withBannerTypeModifier(createEmptyClientBannerTypeModifier()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_2)
                        .withType(BidModifierType.BANNER_TYPE_MULTIPLIER)
                        .withBannerTypeAdjustments(Collections.singletonList(new BidModifierBannerTypeAdjustment()
                                .withBannerType(BannerType.CPM_BANNER)
                                .withPercent(PERCENT_2)))
                )
                .withDemographyModifier(createEmptyClientDemographicsModifier()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_1)
                        .withType(BidModifierType.DEMOGRAPHY_MULTIPLIER)
                        .withDemographicsAdjustments(Collections.singletonList(new BidModifierDemographicsAdjustment()
                                .withAge(AgeType._0_17)
                                .withGender(GenderType.FEMALE)
                                .withPercent(PERCENT_1)))
                )
                .withDesktopModifier(createEmptyClientDesktopModifier()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_2)
                        .withType(BidModifierType.DESKTOP_MULTIPLIER)
                        .withDesktopAdjustment(new BidModifierDesktopAdjustment()
                                .withPercent(PERCENT_2))
                )
                .withSmartTVModifier(createEmptyClientSmartTVModifier()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_2)
                        .withType(BidModifierType.SMARTTV_MULTIPLIER)
                        .withSmartTVAdjustment(new BidModifierSmartTVAdjustment()
                                .withPercent(PERCENT_2))
                )
                .withGeoModifier(createEmptyClientGeoModifier()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_1)
                        .withType(BidModifierType.GEO_MULTIPLIER)
                        .withRegionalAdjustments(Collections.singletonList(new BidModifierRegionalAdjustment()
                                .withRegionId(REGION_ID)
                                .withHidden(false)
                                .withPercent(PERCENT_1)))
                )
                .withInventoryModifier(createEmptyClientInventoryModifier()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_2)
                        .withType(BidModifierType.INVENTORY_MULTIPLIER)
                        .withInventoryAdjustments(Collections.singletonList(new BidModifierInventoryAdjustment()
                                .withInventoryType(InventoryType.INSTREAM_WEB)
                                .withPercent(PERCENT_2)))
                )
                .withMobileModifier(createEmptyClientMobileModifier()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_1)
                        .withType(BidModifierType.MOBILE_MULTIPLIER)
                        .withMobileAdjustment(new BidModifierMobileAdjustment()
                                .withOsType(OsType.IOS)
                                .withPercent(PERCENT_1))
                )
                .withRetargetingModifier(createEmptyClientRetargetingModifier()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_2)
                        .withType(BidModifierType.RETARGETING_MULTIPLIER)
                        .withRetargetingAdjustments(Collections.singletonList(new BidModifierRetargetingAdjustment()
                                .withRetargetingConditionId(RET_COND_ID)
                                .withPercent(PERCENT_2)))
                )
                .withPerformanceTgoModifier(createEmptyClientPerformanceTgoModifier()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_1)
                        .withType(BidModifierType.PERFORMANCE_TGO_MULTIPLIER)
                        .withPerformanceTgoAdjustment(new BidModifierPerformanceTgoAdjustment()
                                .withPercent(PERCENT_1))
                )
                .withVideoModifier(createEmptyClientVideoModifier()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_2)
                        .withType(BidModifierType.VIDEO_MULTIPLIER)
                        .withVideoAdjustment(new BidModifierVideoAdjustment()
                                .withPercent(PERCENT_2))
                )
                .withWeatherModifier(createEmptyClientWeatherModifier()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_1)
                        .withType(BidModifierType.WEATHER_MULTIPLIER)
                        .withWeatherAdjustments(
                                Collections.singletonList(new BidModifierWeatherAdjustment()
                                        .withPercent(PERCENT_1)
                                        .withExpression(Collections.singletonList(
                                                Collections.singletonList(new BidModifierWeatherLiteral()
                                                        .withOperation(OperationType.EQ)
                                                        .withParameter(WeatherType.CLOUDNESS)
                                                        .withValue(PERCENT_2))))))
                )
                .withExpressionModifiers(List.of(new BidModifierTraffic()
                                .withType(BidModifierType.EXPRESS_TRAFFIC_MULTIPLIER)
                                .withCampaignId(CAMPAIGN_ID)
                                .withAdGroupId(AD_GROUP_ID)
                                .withEnabled(ENABLED_1)
                                .withExpressionAdjustments(List.of(new BidModifierTrafficAdjustment()
                                        .withPercent(PERCENT_1)
                                        .withCondition(List.of(List.of(new BidModifierExpressionLiteral()
                                                .withParameter(BidModifierExpressionParameter.TRAFFIC_JAM)
                                                .withOperation(BidModifierExpressionOperator.EQ)
                                                .withValueString("2")
                                        )))
                                )),
                        new BidModifierTraffic()
                                .withType(BidModifierType.PRISMA_INCOME_GRADE_MULTIPLIER)
                                .withCampaignId(CAMPAIGN_ID)
                                .withAdGroupId(AD_GROUP_ID)
                                .withEnabled(ENABLED_1)
                                .withExpressionAdjustments(List.of(new BidModifierTrafficAdjustment()
                                        .withPercent(PERCENT_1)
                                        .withCondition(List.of(List.of(new BidModifierExpressionLiteral()
                                                .withParameter(BidModifierExpressionParameter.PRISMA_INCOME_GRADE)
                                                .withOperation(BidModifierExpressionOperator.EQ)
                                                .withValueString("0")
                                        )))
                                ))))
                .withTrafaretPositionModifier(createEmptyClientTrafaretPositionModifier()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withEnabled(ENABLED_2)
                        .withType(BidModifierType.TRAFARET_POSITION_MULTIPLIER)
                        .withTrafaretPositionAdjustments(Collections.singletonList(new BidModifierTrafaretPositionAdjustment()
                                .withTrafaretPosition(TrafaretPosition.ALONE)
                                .withPercent(PERCENT_2)))
                );
    }

    @Test
    public void checkToComplexBidModifier() {
        assertThat(toComplexBidModifier(updateBidModifiers))
                .is(matchedBy(beanDiffer(expected)));
    }
}

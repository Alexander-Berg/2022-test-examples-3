package ru.yandex.direct.grid.processing.service.bidmodifier;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierBannerType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventory;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPosition;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTraffic;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafficAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeather;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionLiteral;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionParameter;
import ru.yandex.direct.core.testing.data.TestBidModifiers;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifier;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierABSegmentAdjustment;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierAdType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierAdTypeAdjustment;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierDemographics;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierDemographicsAdjustment;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierDesktop;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierDesktopAdjustment;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierExpress;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierExpressAdjustment;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierExpressLiteral;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierGeo;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierInventoryAdjustment;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierMobile;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierMobileAdjustment;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierRegionalAdjustment;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierRetargeting;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierRetargetingAdjustment;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierSmartTgo;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierSmartTgoAdjustment;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierTrafaretPosition;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierTrafaretPositionAdjustment;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierVideo;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierVideoAdjustment;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierWeather;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierWeatherAdjustment;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierWeatherExpression;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBannerType;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierAbSegment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDemographics;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDesktop;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierGeo;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierRetargeting;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierVideo;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierWeather;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultPerformanceTgo;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultTrafaretPositionAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyInventoryModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyTrafaretPositionModifier;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toGdAdjustmentAdType;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toGdAge;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toGdBidModifierImplementation;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toGdBidModifierType;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toGdGender;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toGdInventoryType;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toGdOperationType;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toGdOsType;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toGdTrafaretPosition;
import static ru.yandex.direct.grid.processing.service.bidmodifier.BidModifierDataConverter.toGdWeatherType;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BidModifiersDataConverterToGdModelsTest {
    private static final long CAMPAIGN_ID = RandomNumberUtils.nextPositiveLong();
    private static final long AD_GROUP_ID = RandomNumberUtils.nextPositiveLong();
    private static final long RET_COND_ID = RandomNumberUtils.nextPositiveLong();

    @Autowired
    private Steps steps;

    @Test
    public void convertBidModifierMobile() {
        BidModifierMobile actualBidModifier = createDefaultBidModifierMobile(CAMPAIGN_ID);
        GdBidModifierMobile expected = new GdBidModifierMobile()
                .withCampaignId(actualBidModifier.getCampaignId())
                .withEnabled(actualBidModifier.getEnabled())
                .withId(actualBidModifier.getId())
                .withAdGroupId(actualBidModifier.getAdGroupId())
                .withType(toGdBidModifierType(actualBidModifier.getType()))
                .withAdjustments(Collections.singletonList(new GdBidModifierMobileAdjustment()
                        .withId(actualBidModifier.getMobileAdjustment().getId())
                        .withPercent(actualBidModifier.getMobileAdjustment().getPercent())
                        .withOsType(toGdOsType(actualBidModifier.getMobileAdjustment().getOsType()))));

        check(actualBidModifier, expected);
    }

    @Test
    public void convertBidModifierDesktop() {
        BidModifierDesktop actualBidModifier = createDefaultBidModifierDesktop(CAMPAIGN_ID);
        GdBidModifierDesktop expected = new GdBidModifierDesktop()
                .withCampaignId(actualBidModifier.getCampaignId())
                .withEnabled(actualBidModifier.getEnabled())
                .withId(actualBidModifier.getId())
                .withAdGroupId(actualBidModifier.getAdGroupId())
                .withType(toGdBidModifierType(actualBidModifier.getType()))
                .withAdjustments(Collections.singletonList(new GdBidModifierDesktopAdjustment()
                        .withId(actualBidModifier.getDesktopAdjustment().getId())
                        .withPercent(actualBidModifier.getDesktopAdjustment().getPercent())));

        check(actualBidModifier, expected);
    }

    @Test
    public void convertBidModifierDemographics() {
        BidModifierDemographics actualBidModifier = createDefaultBidModifierDemographics(CAMPAIGN_ID);
        GdBidModifierDemographics expected = new GdBidModifierDemographics()
                .withCampaignId(actualBidModifier.getCampaignId())
                .withEnabled(actualBidModifier.getEnabled())
                .withId(actualBidModifier.getId())
                .withAdGroupId(actualBidModifier.getAdGroupId())
                .withType(toGdBidModifierType(actualBidModifier.getType()))
                .withAdjustments(mapList(actualBidModifier.getDemographicsAdjustments(),
                        bidModifierDemographicsAdjustment -> new GdBidModifierDemographicsAdjustment()
                                .withAge(toGdAge(bidModifierDemographicsAdjustment.getAge()))
                                .withGender(toGdGender(bidModifierDemographicsAdjustment.getGender()))
                                .withId(bidModifierDemographicsAdjustment.getId())
                                .withPercent(bidModifierDemographicsAdjustment.getPercent())));

        check(actualBidModifier, expected);
    }

    @Test
    public void convertBidModifierGeo() {
        BidModifierGeo actualBidModifier = createDefaultBidModifierGeo(CAMPAIGN_ID);
        GdBidModifierGeo expected = new GdBidModifierGeo()
                .withCampaignId(actualBidModifier.getCampaignId())
                .withEnabled(actualBidModifier.getEnabled())
                .withId(actualBidModifier.getId())
                .withAdGroupId(actualBidModifier.getAdGroupId())
                .withType(toGdBidModifierType(actualBidModifier.getType()))
                .withAdjustments(mapList(actualBidModifier.getRegionalAdjustments(),
                        bidModifierRegionalAdjustment -> new GdBidModifierRegionalAdjustment()
                                .withHidden(bidModifierRegionalAdjustment.getHidden())
                                .withRegionId(bidModifierRegionalAdjustment.getRegionId().intValue())
                                .withId(bidModifierRegionalAdjustment.getId())
                                .withPercent(bidModifierRegionalAdjustment.getPercent())));

        check(actualBidModifier, expected);
    }

    @Test
    public void convertBidModifierVideo() {
        BidModifierVideo actualBidModifier = createDefaultBidModifierVideo(CAMPAIGN_ID);
        GdBidModifierVideo expected = new GdBidModifierVideo()
                .withCampaignId(actualBidModifier.getCampaignId())
                .withEnabled(actualBidModifier.getEnabled())
                .withId(actualBidModifier.getId())
                .withAdGroupId(actualBidModifier.getAdGroupId())
                .withType(toGdBidModifierType(actualBidModifier.getType()))
                .withAdjustments(Collections.singletonList(new GdBidModifierVideoAdjustment()
                        .withId(actualBidModifier.getVideoAdjustment().getId())
                        .withPercent(actualBidModifier.getVideoAdjustment().getPercent())));

        check(actualBidModifier, expected);
    }

    @Test
    public void convertBidModifierAbSegment() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        RetConditionInfo retConditionInfo = steps.retConditionSteps().createDefaultRetCondition(clientInfo);
        BidModifierABSegment actualBidModifier =
                createDefaultBidModifierAbSegment(CAMPAIGN_ID, AD_GROUP_ID, retConditionInfo);
        GdBidModifierMobile expected = new GdBidModifierMobile()
                .withCampaignId(actualBidModifier.getCampaignId())
                .withEnabled(actualBidModifier.getEnabled())
                .withId(actualBidModifier.getId())
                .withAdGroupId(actualBidModifier.getAdGroupId())
                .withType(toGdBidModifierType(actualBidModifier.getType()))
                .withAdjustments(mapList(actualBidModifier.getAbSegmentAdjustments(),
                        bidModifierABSegmentAdjustment -> new GdBidModifierABSegmentAdjustment()
                                .withAccessible(bidModifierABSegmentAdjustment.getAccessible())
                                .withSectionId(bidModifierABSegmentAdjustment.getSectionId())
                                .withSegmentId(bidModifierABSegmentAdjustment.getSegmentId())
                                .withAbSegmentRetargetingConditionId(
                                        bidModifierABSegmentAdjustment.getAbSegmentRetargetingConditionId())
                                .withId(bidModifierABSegmentAdjustment.getId())
                                .withPercent(bidModifierABSegmentAdjustment.getPercent())));

        check(actualBidModifier, expected);
    }


    @Test
    public void convertBidModifierRetargeting() {
        BidModifierRetargeting
                actualBidModifier = createDefaultBidModifierRetargeting(CAMPAIGN_ID, AD_GROUP_ID, RET_COND_ID);
        GdBidModifierRetargeting expected = new GdBidModifierRetargeting()
                .withCampaignId(actualBidModifier.getCampaignId())
                .withEnabled(actualBidModifier.getEnabled())
                .withId(actualBidModifier.getId())
                .withAdGroupId(actualBidModifier.getAdGroupId())
                .withType(toGdBidModifierType(actualBidModifier.getType()))
                .withAdjustments(mapList(actualBidModifier.getRetargetingAdjustments(),
                        bidModifierRetargetingAdjustment -> new GdBidModifierRetargetingAdjustment()
                                .withAccessible(bidModifierRetargetingAdjustment.getAccessible())
                                .withRetargetingConditionId(
                                        bidModifierRetargetingAdjustment.getRetargetingConditionId())
                                .withId(bidModifierRetargetingAdjustment.getId())
                                .withPercent(bidModifierRetargetingAdjustment.getPercent())));

        check(actualBidModifier, expected);
    }

    @Test
    public void convertBidModifierSmartTgo() {
        BidModifierPerformanceTgo actualBidModifier = createDefaultPerformanceTgo(CAMPAIGN_ID, AD_GROUP_ID);
        GdBidModifierSmartTgo expected = new GdBidModifierSmartTgo()
                .withCampaignId(actualBidModifier.getCampaignId())
                .withEnabled(actualBidModifier.getEnabled())
                .withId(actualBidModifier.getId())
                .withAdGroupId(actualBidModifier.getAdGroupId())
                .withType(toGdBidModifierType(actualBidModifier.getType()))
                .withAdjustments(Collections.singletonList(new GdBidModifierSmartTgoAdjustment()
                        .withId(actualBidModifier.getPerformanceTgoAdjustment().getId())
                        .withPercent(actualBidModifier.getPerformanceTgoAdjustment().getPercent())));

        check(actualBidModifier, expected);
    }

    @Test
    public void convertBidModifierAdType() {
        BidModifierBannerType actualBidModifier = createDefaultBannerType(CAMPAIGN_ID, AD_GROUP_ID);
        GdBidModifierAdType expected = new GdBidModifierAdType()
                .withCampaignId(actualBidModifier.getCampaignId())
                .withEnabled(actualBidModifier.getEnabled())
                .withId(actualBidModifier.getId())
                .withAdGroupId(actualBidModifier.getAdGroupId())
                .withType(toGdBidModifierType(actualBidModifier.getType()))
                .withAdjustments(mapList(actualBidModifier.getBannerTypeAdjustments(),
                        bidModifierBannerTypeAdjustment -> new GdBidModifierAdTypeAdjustment()
                                .withAdType(toGdAdjustmentAdType(bidModifierBannerTypeAdjustment.getBannerType()))
                                .withId(bidModifierBannerTypeAdjustment.getId())
                                .withPercent(bidModifierBannerTypeAdjustment.getPercent())));

        check(actualBidModifier, expected);
    }

    @Test
    public void convertBidModifierInventory() {
        BidModifierInventory actualBidModifier = createEmptyInventoryModifier()
                .withCampaignId(CAMPAIGN_ID)
                .withAdGroupId(AD_GROUP_ID)
                .withInventoryAdjustments(Collections.singletonList(new BidModifierInventoryAdjustment()
                        .withPercent(RandomNumberUtils.nextPositiveInteger() % TestBidModifiers.PERCENT_MAX)
                        .withInventoryType(
                                InventoryType.values()[RandomNumberUtils.nextPositiveInteger() % InventoryType
                                        .values().length])
                        .withId(RandomNumberUtils.nextPositiveLong())));
        GdBidModifierMobile expected = new GdBidModifierMobile()
                .withCampaignId(actualBidModifier.getCampaignId())
                .withEnabled(actualBidModifier.getEnabled())
                .withId(actualBidModifier.getId())
                .withAdGroupId(actualBidModifier.getAdGroupId())
                .withType(toGdBidModifierType(actualBidModifier.getType()))
                .withAdjustments(mapList(actualBidModifier.getInventoryAdjustments(),
                        bidModifierInventoryAdjustment -> new GdBidModifierInventoryAdjustment()
                                .withInventoryType(toGdInventoryType(bidModifierInventoryAdjustment.getInventoryType()))
                                .withId(bidModifierInventoryAdjustment.getId())
                                .withPercent(bidModifierInventoryAdjustment.getPercent())));

        check(actualBidModifier, expected);
    }

    @Test
    public void convertBidModifierWeather() {
        BidModifierWeather actualBidModifier = createDefaultBidModifierWeather(CAMPAIGN_ID).withAdGroupId(AD_GROUP_ID);
        GdBidModifierWeather expected = new GdBidModifierWeather()
                .withCampaignId(actualBidModifier.getCampaignId())
                .withEnabled(actualBidModifier.getEnabled())
                .withId(actualBidModifier.getId())
                .withAdGroupId(actualBidModifier.getAdGroupId())
                .withType(toGdBidModifierType(actualBidModifier.getType()))
                .withAdjustments(mapList(actualBidModifier.getWeatherAdjustments(),
                        bidModifierWeatherAdjustment -> new GdBidModifierWeatherAdjustment()
                                .withExpression(mapList(bidModifierWeatherAdjustment.getExpression(),
                                        bidModifierWeatherLiterals -> mapList(bidModifierWeatherLiterals,
                                                bidModifierWeatherLiteral -> new GdBidModifierWeatherExpression()
                                                        .withValue(bidModifierWeatherLiteral.getValue())
                                                        .withParameter(toGdWeatherType(
                                                                bidModifierWeatherLiteral.getParameter()))
                                                        .withOperation(toGdOperationType(
                                                                bidModifierWeatherLiteral.getOperation())))))
                                .withId(bidModifierWeatherAdjustment.getId())
                                .withPercent(bidModifierWeatherAdjustment.getPercent())));

        check(actualBidModifier, expected);
    }

    @Test
    public void convertBidModifierTraffic() {
        BidModifierTraffic coreModel = new BidModifierTraffic()
                .withId(1231412L)
                .withType(BidModifierType.EXPRESS_TRAFFIC_MULTIPLIER)
                .withCampaignId(3333222L)
                .withAdGroupId(1211251L)
                .withEnabled(true)
                .withLastChange(LocalDateTime.now())
                .withExpressionAdjustments(List.of(new BidModifierTrafficAdjustment()
                        .withId(987685L)
                        .withPercent(123)
                        .withLastChange(LocalDateTime.now())
                        .withCondition(List.of(List.of(new BidModifierExpressionLiteral()
                                .withParameter(BidModifierExpressionParameter.TRAFFIC_JAM)
                                .withOperation(BidModifierExpressionOperator.EQ)
                                .withValueString("2")
                        )))
                ));
        GdBidModifierExpress gdModel = new GdBidModifierExpress()
                .withId(1231412L)
                .withType(GdBidModifierType.EXPRESS_TRAFFIC_MULTIPLIER)
                .withCampaignId(3333222L)
                .withAdGroupId(1211251L)
                .withEnabled(true)
                .withAdjustments(List.of(new GdBidModifierExpressAdjustment()
                        .withId(987685L)
                        .withPercent(123)
                        .withCondition(List.of(List.of(new GdBidModifierExpressLiteral()
                                .withParameter(BidModifierExpressionParameter.TRAFFIC_JAM)
                                .withOperation(BidModifierExpressionOperator.EQ)
                                .withValue("2")
                        )))
                ));

        check(coreModel, gdModel);
    }

    @Test
    public void convertBidModifierTrafaretPosition() {
        BidModifierTrafaretPosition actualBidModifier = createEmptyTrafaretPositionModifier()
                .withCampaignId(CAMPAIGN_ID)
                .withTrafaretPositionAdjustments(Collections.singletonList(createDefaultTrafaretPositionAdjustment()));
        GdBidModifierTrafaretPosition expected = new GdBidModifierTrafaretPosition()
                .withCampaignId(actualBidModifier.getCampaignId())
                .withEnabled(actualBidModifier.getEnabled())
                .withId(actualBidModifier.getId())
                .withAdGroupId(actualBidModifier.getAdGroupId())
                .withType(toGdBidModifierType(actualBidModifier.getType()))
                .withAdjustments(mapList(actualBidModifier.getTrafaretPositionAdjustments(),
                        bidModifierTrafaretPositionAdjustment -> new GdBidModifierTrafaretPositionAdjustment()
                                .withTrafaretPosition(toGdTrafaretPosition(bidModifierTrafaretPositionAdjustment.getTrafaretPosition()))
                                .withId(bidModifierTrafaretPositionAdjustment.getId())
                                .withPercent(bidModifierTrafaretPositionAdjustment.getPercent())));

        check(actualBidModifier, expected);
    }


    private static void check(BidModifier actualBidModifier, GdBidModifier expected) {
        assertThat(toGdBidModifierImplementation(actualBidModifier))
                .is(matchedBy(beanDiffer(expected)));

    }
}

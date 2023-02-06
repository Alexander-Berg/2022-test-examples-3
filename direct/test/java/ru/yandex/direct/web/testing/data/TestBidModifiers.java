package ru.yandex.direct.web.testing.data;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomUtils;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierTraffic;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafficAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionLiteral;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionParameter;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupBidModifiers;
import ru.yandex.direct.web.entity.bidmodifier.model.WebAdjustment;
import ru.yandex.direct.web.entity.bidmodifier.model.WebDemographicsAdjustment;
import ru.yandex.direct.web.entity.bidmodifier.model.WebDemographicsBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebDesktopBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebExpressionBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebExpressionBidModifierAdjustment;
import ru.yandex.direct.web.entity.bidmodifier.model.WebExpressionBidModifierLiteral;
import ru.yandex.direct.web.entity.bidmodifier.model.WebMobileBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebRetargetingBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebVideoBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebWeatherAdjustment;
import ru.yandex.direct.web.entity.bidmodifier.model.WebWeatherBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebWeatherExpression;

import static java.util.Collections.singletonList;

public class TestBidModifiers {

    public static final WebExpressionBidModifier SAMPLE_WEB_EXPRESS_MODIFIER = new WebExpressionBidModifier()
            .withType("express_traffic_multiplier")
            .withEnabled(1)
            .withAdjustments(List.of(new WebExpressionBidModifierAdjustment()
                    .withPercent(123)
                    .withCondition(List.of(List.of(new WebExpressionBidModifierLiteral()
                            .withParameter(BidModifierExpressionParameter.TRAFFIC_JAM)
                            .withOperation(BidModifierExpressionOperator.EQ)
                            .withValue("2")
                    )))
            ));
    public static final BidModifierTraffic SAMPLE_EXPRESS_MODIFIER = new BidModifierTraffic()
            .withType(BidModifierType.EXPRESS_TRAFFIC_MULTIPLIER)
            .withEnabled(true)
            .withExpressionAdjustments(List.of(new BidModifierTrafficAdjustment()
                    .withPercent(123)
                    .withCondition(List.of(List.of(new BidModifierExpressionLiteral()
                            .withParameter(BidModifierExpressionParameter.TRAFFIC_JAM)
                            .withOperation(BidModifierExpressionOperator.EQ)
                            .withValueString("2")
                    )))
            ));

    public static WebAdGroupBidModifiers fullWebAdGroupBidModifiers(long retCondId) {
        return new WebAdGroupBidModifiers()
                .withRetargetingBidModifier(singleRandomPercentRetargetingBidModifier(retCondId))
                .withDemographicsBidModifier(singleRandomPercentDemographicsBidModifier())
                .withMobileBidModifier(randomPercentMobileBidModifier())
                .withVideoBidModifier(randomPercentVideoBidModifier());
    }

    public static WebAdGroupBidModifiers fullWebAdGroupBidModifiersWithDevice(long retCondId) {
        return new WebAdGroupBidModifiers()
                .withRetargetingBidModifier(singleRandomPercentRetargetingBidModifier(retCondId))
                .withDemographicsBidModifier(singleRandomPercentDemographicsBidModifier())
                .withMobileBidModifier(randomPercentIosMobileBidModifier())
                .withDesktopBidModifier(randomPercentDesktopBidModifier())
                .withVideoBidModifier(randomPercentVideoBidModifier());
    }

    public static WebAdGroupBidModifiers fullCpmAdGroupBidModifiers(long retCondId) {
        return new WebAdGroupBidModifiers()
                .withRetargetingBidModifier(singleRandomPercentRetargetingBidModifier(retCondId))
                .withDemographicsBidModifier(singleRandomPercentDemographicsBidModifier())
                .withMobileBidModifier(randomPercentMobileBidModifier())
                .withDesktopBidModifier(randomPercentDesktopBidModifier())
                .withWeatherBidModifier(singleRandomPercentWeatherBidModifier());
    }

    public static WebAdGroupBidModifiers fullContentPromotionAdGroupBidModifiers(
            long retCondId) {
        return new WebAdGroupBidModifiers()
                .withDemographicsBidModifier(singleRandomPercentDemographicsBidModifier())
                .withRetargetingBidModifier(singleRandomPercentRetargetingBidModifier(retCondId))
                .withMobileBidModifier(randomPercentMobileBidModifier());
    }

    public static WebAdGroupBidModifiers cpmAdGroupDeviceBidModifiers() {
        return new WebAdGroupBidModifiers()
                .withMobileBidModifier(randomPercentIosMobileBidModifier())
                .withDesktopBidModifier(randomPercentDesktopBidModifier());
    }

    public static WebAdGroupBidModifiers cpmAdGroupWeatherBidModifiers() {
        return new WebAdGroupBidModifiers()
                .withWeatherBidModifier(singleRandomPercentWeatherBidModifier());
    }

    public static WebRetargetingBidModifier singleRandomPercentRetargetingBidModifier(long retCondId) {
        return new WebRetargetingBidModifier()
                .withAdjustments(singleRandomPercentRetargetingAdjustments(retCondId))
                .withEnabled(1);
    }

    public static Map<String, WebAdjustment> singleRandomPercentRetargetingAdjustments(long retCondId) {
        return ImmutableMap.of(String.valueOf(retCondId), randomPercentAdjustment());
    }

    public static WebAdjustment randomPercentAdjustment() {
        return new WebAdjustment()
                .withPercent(RandomUtils.nextInt(100, 1200));
    }

    public static WebWeatherBidModifier singleRandomPercentWeatherBidModifier() {
        return new WebWeatherBidModifier()
                .withEnabled(1)
                .withAdjustments(singleRandomPercentWeatherAdjustment());
    }

    public static List<WebWeatherAdjustment> singleRandomPercentWeatherAdjustment() {
        return singletonList(randomPercentWeatherAdjustment());
    }

    public static WebWeatherAdjustment randomPercentWeatherAdjustment() {
        return (WebWeatherAdjustment) new WebWeatherAdjustment()
                .withExpression(singleWeatherExpression())
                .withPercent(RandomUtils.nextInt(100, 1200));
    }

    public static List<List<WebWeatherExpression>> singleWeatherExpression() {
        return singletonList(singletonList(new WebWeatherExpression()
                .withParameter("cloudness")
                .withOperation("eq")
                .withValue(50)
        ));
    }

    public static WebDemographicsBidModifier singleRandomPercentDemographicsBidModifier() {
        return new WebDemographicsBidModifier()
                .withEnabled(1)
                .withAdjustments(singleRandomPercentDemographicsAdjustment());
    }

    public static List<WebDemographicsAdjustment> singleRandomPercentDemographicsAdjustment() {
        return singletonList(randomPercentDemographicsAdjustment());
    }

    public static WebDemographicsAdjustment randomPercentDemographicsAdjustment() {
        return (WebDemographicsAdjustment) new WebDemographicsAdjustment()
                .withAge("25-34")
                .withPercent(RandomUtils.nextInt(100, 1200));
    }

    public static WebMobileBidModifier randomPercentMobileBidModifier() {
        return new WebMobileBidModifier()
                .withPercent(RandomUtils.nextInt(100, 1200));
    }

    public static WebMobileBidModifier randomPercentIosMobileBidModifier() {
        return new WebMobileBidModifier()
                .withOsType("ios")
                .withPercent(RandomUtils.nextInt(100, 1200));
    }

    public static WebDesktopBidModifier randomPercentDesktopBidModifier() {
        return new WebDesktopBidModifier()
                .withPercent(RandomUtils.nextInt(100, 1200));
    }

    public static WebVideoBidModifier randomPercentVideoBidModifier() {
        return new WebVideoBidModifier()
                .withPercent(RandomUtils.nextInt(100, 1200));
    }
}

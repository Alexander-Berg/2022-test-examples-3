package ru.yandex.direct.web.entity.bidmodifier.converter;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierTraffic;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafficAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionLiteral;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionParameter;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.web.entity.adgroup.converter.AdGroupBidModifiersConverter;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupBidModifiers;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Тест на то, что JSON объект переданный фронтендом правильно конвертится в ядровую модель
 */

public class BidModifierExpressionFromJsonTest {
    @Test
    public void expressTrafficModifierTest() {
        String complexBidModifierJson =
                LiveResourceFactory.get("classpath:///bidmodifiers/express-traffic-complex-bidmodifier.json")
                        .getContent();
        WebAdGroupBidModifiers webComplexBidModifier =
                JsonUtils.fromJson(complexBidModifierJson, WebAdGroupBidModifiers.class);
        ComplexBidModifier complexBidModifier =
                AdGroupBidModifiersConverter.webAdGroupBidModifiersToCore(webComplexBidModifier);

        ComplexBidModifier expected = getExpectedTrafficModifier();

        assertThat(complexBidModifier, beanDiffer(expected));
    }

    private ComplexBidModifier getExpectedTrafficModifier() {
        return new ComplexBidModifier()
                .withExpressionModifiers(List.of(new BidModifierTraffic()
                        .withType(BidModifierType.EXPRESS_TRAFFIC_MULTIPLIER)
                        .withEnabled(true)
                        .withExpressionAdjustments(List.of(
                                new BidModifierTrafficAdjustment()
                                        .withPercent(0)
                                        .withCondition(List.of(List.of(new BidModifierExpressionLiteral()
                                                .withParameter(BidModifierExpressionParameter.TRAFFIC_JAM)
                                                .withOperation(BidModifierExpressionOperator.EQ)
                                                .withValueString("0")
                                        ))),
                                new BidModifierTrafficAdjustment()
                                        .withPercent(101)
                                        .withCondition(List.of(List.of(new BidModifierExpressionLiteral()
                                                .withParameter(BidModifierExpressionParameter.TRAFFIC_JAM)
                                                .withOperation(BidModifierExpressionOperator.EQ)
                                                .withValueString("1")
                                        )))
                        ))
                ));
    }
}

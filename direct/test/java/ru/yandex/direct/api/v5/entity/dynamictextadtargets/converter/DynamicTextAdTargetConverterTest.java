package ru.yandex.direct.api.v5.entity.dynamictextadtargets.converter;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.yandex.direct.api.v5.dynamictextadtargets.StringConditionOperatorEnum;
import com.yandex.direct.api.v5.dynamictextadtargets.WebpageCondition;
import com.yandex.direct.api.v5.dynamictextadtargets.WebpageConditionOperandEnum;
import com.yandex.direct.api.v5.dynamictextadtargets.WebpageTypeEnum;
import com.yandex.direct.api.v5.general.PriorityEnum;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleKind;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleType;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.currencies.CurrencyRub;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@ParametersAreNonnullByDefault
public class DynamicTextAdTargetConverterTest {

    public GetResponseConverter responseConverter;

    @Autowired
    private TranslationService translationService;

    @Before
    public void prepare() {
        responseConverter = new GetResponseConverter(translationService);
    }

    @Test
    public void convertStrategyPriority() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(responseConverter.convertStrategyPriority(1).getValue()).isEqualTo(PriorityEnum.LOW);
            softly.assertThat(responseConverter.convertStrategyPriority(3).getValue()).isEqualTo(PriorityEnum.NORMAL);
            softly.assertThat(responseConverter.convertStrategyPriority(5).getValue()).isEqualTo(PriorityEnum.HIGH);
            softly.assertThat(responseConverter.convertStrategyPriority(123).getValue()).isEqualTo(PriorityEnum.NORMAL);
            softly.assertThat(responseConverter.convertStrategyPriority(null).getValue()).isEqualTo(null);
        });
    }

    @Test
    public void conditionTypeIsAllPage() {
        List<WebpageRule> condition = singletonList(new WebpageRule().withType(WebpageRuleType.ANY));
        assertThat(responseConverter.convertConditionType(condition)).isEqualTo(WebpageTypeEnum.PAGES_ALL);
    }

    @Test
    public void conditionTypeIsPageSubset() {
        List<WebpageRule> condition = singletonList(new WebpageRule()
                .withKind(WebpageRuleKind.EQUALS)
                .withType(WebpageRuleType.URL_PRODLIST)
                .withValue(ImmutableList.of("http://ya.ru/contact/", "http://ya.ru/")));
        assertThat(responseConverter.convertConditionType(condition)).isEqualTo(WebpageTypeEnum.PAGES_SUBSET);
    }

    @Test
    public void convertAllPageCondition() {
        List<WebpageRule> condition = singletonList(new WebpageRule().withType(WebpageRuleType.ANY));
        assertNull(responseConverter.convertCondition(condition));
    }

    @Test
    public void convertPageSubsetCondition() {
        List<WebpageRule> condition = singletonList(new WebpageRule()
                .withKind(WebpageRuleKind.EQUALS)
                .withType(WebpageRuleType.URL_PRODLIST)
                .withValue(ImmutableList.of("http://ya.ru/contact/", "http://ya.ru/")));

        WebpageCondition expected = new WebpageCondition().withArguments("http://ya.ru/contact/", "http://ya.ru/")
                .withOperand(WebpageConditionOperandEnum.OFFERS_LIST_URL)
                .withOperator(StringConditionOperatorEnum.EQUALS_ANY);
        assertThat(responseConverter.convertCondition(condition))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expected);
    }

    @Test
    public void convertZeroPrice() {
        Currency currency = CurrencyRub.getInstance();
        long price = responseConverter.convertPrice(BigDecimal.ZERO, currency);
        assertEquals(price, currency.getMinPrice().multiply(Money.MICRO_MULTIPLIER).longValue());
    }

    @Test
    public void convertMinBidPrice() {
        Currency currency = CurrencyRub.getInstance();
        BigDecimal minBid = currency.getMinPrice();
        long price = responseConverter.convertPrice(minBid, currency);
        assertEquals(price, minBid.multiply(Money.MICRO_MULTIPLIER).longValue());
    }

    @Test
    public void convertBigPrice() {
        Currency currency = CurrencyRub.getInstance();
        BigDecimal bigPrice = BigDecimal.valueOf(123L);
        long price = responseConverter.convertPrice(bigPrice, currency);
        assertEquals(price, bigPrice.multiply(Money.MICRO_MULTIPLIER).longValue());
    }
}

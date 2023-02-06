package ru.yandex.chemodan.app.psbilling.core.promocodes.rule.impl;

import lombok.val;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.promocodes.model.PromoCodeData;
import ru.yandex.chemodan.app.psbilling.core.promocodes.rule.PromoCodeRuleContext;
import ru.yandex.misc.test.Assert;

public class AvailablePromoCodeRuleCheckerTest {

    @Test
    public void isAvailableIsTrue() {
        val rule = AvailablePromoCodeRuleChecker.INSTANCE;
        val result = rule.check(Mockito.mock(PromoCodeData.class), Option.empty(), Option.empty(), new PromoCodeRuleContext());

        Assert.isTrue(result.isSuccess());
        Assert.none(result.getError());
    }
}

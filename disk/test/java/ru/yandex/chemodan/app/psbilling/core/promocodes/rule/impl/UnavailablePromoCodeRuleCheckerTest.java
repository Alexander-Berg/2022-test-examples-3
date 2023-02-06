package ru.yandex.chemodan.app.psbilling.core.promocodes.rule.impl;

import lombok.val;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.promocodes.model.PromoCodeData;
import ru.yandex.chemodan.app.psbilling.core.promocodes.rule.PromoCodeRuleContext;
import ru.yandex.misc.test.Assert;

public class UnavailablePromoCodeRuleCheckerTest {

    @Test
    public void isAvailable() {
        val rule = UnavailablePromoCodeRuleChecker.INSTANCE;
        val result = rule.check(Mockito.mock(PromoCodeData.class), Option.empty(), Option.empty(), new PromoCodeRuleContext());

        Assert.isFalse(result.isSuccess());
        Assert.some(result.getError());
    }
}

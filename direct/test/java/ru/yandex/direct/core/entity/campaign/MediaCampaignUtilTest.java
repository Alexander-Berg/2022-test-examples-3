package ru.yandex.direct.core.entity.campaign;

import java.math.BigDecimal;

import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.MediaCampaignUtil.calcMediaSumSpent;

public class MediaCampaignUtilTest {

    private static Money sum;

    @BeforeClass
    public static void initSum() {
        sum = Money.valueOf(100, CurrencyCode.RUB);
    }

    @Test
    public void testGetSum() {
        assertThat("Если количество купленных и истраченных показов совпадает, возвращаем сумму",
                calcMediaSumSpent(sum, 5, 5).compareTo(BigDecimal.valueOf(100)), equalTo(0));
    }

    @Test
    public void testGetZero() {
        assertThat("Если количество купленных показов равно нулю, возвращаем ноль",
                calcMediaSumSpent(sum, 5, 0).compareTo(BigDecimal.ZERO), equalTo(0));
    }

    @Test
    public void testGetSumProportional() {
        assertThat(
                "Если количество купленных и истраченных показов развное, возвращаем пропорциональное значение сумму",
                calcMediaSumSpent(sum, 5, 10).compareTo(BigDecimal.valueOf(50)), equalTo(0));
    }
}

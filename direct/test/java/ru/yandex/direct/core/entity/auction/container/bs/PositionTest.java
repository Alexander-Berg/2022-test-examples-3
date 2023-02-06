package ru.yandex.direct.core.entity.auction.container.bs;

import java.util.List;

import com.google.common.collect.Ordering;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;

import static java.util.Arrays.asList;

public class PositionTest {

    @Test
    public void naturalComparisonOrder_isDesc() throws Exception {
        List<Position> positions =
                asList(positionWithPrice(1.0),
                        positionWithPrice(0.0));

        Assertions.assertThat(Ordering.natural().isOrdered(positions)).isTrue();
    }

    private Position positionWithPrice(double val) {
        return new Position(Money.valueOf(val, CurrencyCode.YND_FIXED), Money.valueOf(val, CurrencyCode.YND_FIXED));
    }
}

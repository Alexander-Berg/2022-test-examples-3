package ru.yandex.direct.core.entity.auction.container.bs;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class BlockTest {

    @Test
    public void constructorThrowsException_whenEmptyPositions() throws Exception {
        Assertions.assertThatThrownBy(() -> new Block(emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Block can't be empty");
    }

    @Test
    public void constructorThrowsException_whenUnorderedPositions() throws Exception {
        Assertions.assertThatThrownBy(
                () -> new Block(
                        asList(positionWithPrice(0.),
                                positionWithPrice(1.)
                        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Positions in block should be ordered");
    }

    @Test
    public void first_returnsFirst() throws Exception {
        Block block = blockWithPrices(3., 2., 1.);
        Position actual = block.first();
        assertThat(actual.getBidPrice().bigDecimalValue())
                .isEqualByComparingTo(BigDecimal.valueOf(3.));
    }

    @Test
    public void last_returnsLast() throws Exception {
        Block block = blockWithPrices(3., 2., 1.);
        Position actual = block.last();
        assertThat(actual.getBidPrice().bigDecimalValue())
                .isEqualByComparingTo(BigDecimal.valueOf(1.));
    }

    @Test
    public void get_byMinusOne_returnsLast() throws Exception {
        Block block = blockWithPrices(3., 2., 1.);
        Position actual = block.get(-1);
        assertThat(actual.getBidPrice().bigDecimalValue())
                .isEqualByComparingTo(BigDecimal.valueOf(1.));
    }

    private Block blockWithPrices(double... prices) {
        List<Position> positions = Arrays.stream(prices)
                .mapToObj(this::positionWithPrice)
                .collect(toList());
        return new Block(positions);
    }

    private Position positionWithPrice(double val) {
        return new Position(Money.valueOf(val, CurrencyCode.YND_FIXED), Money.valueOf(val, CurrencyCode.YND_FIXED));
    }

}

package ru.yandex.market.logistics.management.domain.helper;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.dto.front.MoneyHelper;

public class MoneyHelperTest extends AbstractTest {

    @Test
    void withRemainder() {
        var result = MoneyHelper.fromKopecks(10034L);
        softly.assertThat(result).isEqualTo(new BigDecimal("100.34"));
    }

    @Test
    void withoutRemainder() {
        var result = MoneyHelper.fromKopecks(10000L);
        softly.assertThat(result).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void reverseWithRemainder() {
        var result = MoneyHelper.toKopecks(BigDecimal.valueOf(100.31d));
        softly.assertThat(result).isEqualTo(10031);
    }

    @Test
    void reverseWithoutRemainder() {
        var result = MoneyHelper.toKopecks(BigDecimal.valueOf(100.00d));
        softly.assertThat(result).isEqualTo(10000);
    }
}

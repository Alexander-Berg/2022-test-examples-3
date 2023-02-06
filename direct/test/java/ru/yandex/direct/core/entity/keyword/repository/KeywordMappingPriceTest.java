package ru.yandex.direct.core.entity.keyword.repository;

import java.math.BigDecimal;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class KeywordMappingPriceTest {

    @Parameterized.Parameter(0)
    public BigDecimal modelPrice;

    @Parameterized.Parameter(1)
    public BigDecimal dbPrice;

    @Parameterized.Parameters(name = "model format: {0}, db format: {1}")
    public static Collection<Object[]> params() {
        return asList(
                new Object[]{null, BigDecimal.ZERO},
                new Object[]{BigDecimal.ONE, BigDecimal.ONE}
        );
    }

    @Test
    public void testToDbFormat() {
        assertThat("Конвертация типа в формат базы должна быть однозначной",
                KeywordMapping.priceToDbFormat(modelPrice),
                is(dbPrice));
    }

    @Test
    public void testFromDbFormat() {
        assertThat("Конвертация типа в формат модели должна быть однозначной",
                KeywordMapping.priceFromDbFormat(dbPrice),
                is(modelPrice));
    }
}

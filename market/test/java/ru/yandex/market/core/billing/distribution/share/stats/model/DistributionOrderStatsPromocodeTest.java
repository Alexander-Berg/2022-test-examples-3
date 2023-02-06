package ru.yandex.market.core.billing.distribution.share.stats.model;

import java.util.List;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

public class DistributionOrderStatsPromocodeTest {

    @Test
    public void testParse() {
        var result = DistributionOrderStatsPromocode.fromStringToList("1234:abc,333:");
        assertThat(result, iterableWithSize(2));
        assertThat(result.get(0).getClid(), is(1234L));
        assertThat(result.get(0).getPromocode(), is("abc"));
        assertThat(result.get(1).getClid(), is(333L));
        assertThat(result.get(1).getPromocode(), is(""));
    }

    @Test
    public void testSerialize() {
        List<DistributionOrderStatsPromocode> input = List.of(
                new DistributionOrderStatsPromocode(1234L, "promo"),
                new DistributionOrderStatsPromocode(567L, "promo2"));
        assertThat(DistributionOrderStatsPromocode.toStringList(input), is("1234:promo,567:promo2"));
    }

}
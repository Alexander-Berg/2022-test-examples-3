package ru.yandex.market.loyalty.core.service.bundle.strategy.condition;

import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.SerializingTranscoder;
import org.junit.Test;

import ru.yandex.market.loyalty.core.service.bundle.calculation.OfferDiscountCondition;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class GiftWithPurchaseConditionTest {
    private final SerializingTranscoder serializingTranscoder = new SerializingTranscoder();

    @Test
    public void shouldSerializeConditionForCache() {
        CachedData cachedData = serializingTranscoder.encode(GiftWithPurchaseCondition.builder()
                .withFeedId(123)
                .withOfferDiscountDescription(OfferDiscountCondition.builder()
                        .withSsku("test")
                        .withRequiredSsku("relation")
                        .withProportion(BigDecimal.TEN)
                        .withFixedPrice(BigDecimal.ONE)
                        .build())
                .build());

        assertThat(cachedData.getData().length, greaterThan(0));
    }

    @Test
    public void shouldDeserializeConditionForCache() {
        CachedData cachedData = serializingTranscoder.encode(GiftWithPurchaseCondition.builder()
                .withFeedId(123)
                .withOfferDiscountDescription(OfferDiscountCondition.builder()
                        .withSsku("test")
                        .withRequiredSsku("relation")
                        .withProportion(BigDecimal.TEN)
                        .withFixedPrice(BigDecimal.ONE)
                        .build())
                .build());

        GiftWithPurchaseCondition condition = (GiftWithPurchaseCondition) serializingTranscoder.decode(cachedData);

        assertThat(condition.getId(), notNullValue());
        assertThat(condition.getFeedId(), comparesEqualTo(123L));
        assertThat(condition.getMappings(), hasItem(allOf(
                hasProperty("ssku", is("test")),
                hasProperty("requiredSskus", hasItem("relation")),
                hasProperty("proportion", comparesEqualTo(BigDecimal.TEN)),
                hasProperty("fixedPrice", comparesEqualTo(BigDecimal.ONE))
        )));
    }
}

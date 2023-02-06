package ru.yandex.market.antifraud.orders.storage.entity.stat;

import org.assertj.core.api.Assertions;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import ru.yandex.market.antifraud.orders.entity.Msku;

@RunWith(Theories.class)
public class ItemParamsTest {

    @DataPoint("item")
    public static ItemParams itemParams = ItemParams.builder()
        .categoryId(123)
        .modelId(13345L)
        .msku(new Msku(145L))
        .build();

    @DataPoints("matchingRule")
    public static ItemParams[] matchingRules = new ItemParams[] {
        ItemParams.builder()
            .categoryId(123)
            .build(),
        ItemParams.builder()
            .modelId(13345L)
            .build(),
        ItemParams.builder()
            .msku(new Msku(145L))
            .build(),
    };

    @DataPoints("nonmatchingRule")
    public static ItemParams[] nonmatchingRules = new ItemParams[] {
        ItemParams.builder()
            .categoryId(124)
            .build(),
        ItemParams.builder()
            .modelId(13346L)
            .build(),
        ItemParams.builder()
            .msku(new Msku(146L))
            .build(),
    };

    @Theory
    public void matches(@FromDataPoints("matchingRule") ItemParams ruleParams) {
        Assertions.assertThat(ruleParams.matches(itemParams))
            .isTrue();
    }

    @Theory
    public void notMatches(@FromDataPoints("nonmatchingRule") ItemParams ruleParams) {
        Assertions.assertThat(ruleParams.matches(itemParams))
            .isFalse();
    }
}
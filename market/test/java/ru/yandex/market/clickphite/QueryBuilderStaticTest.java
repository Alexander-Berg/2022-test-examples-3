package ru.yandex.market.clickphite;

import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.health.configs.clickphite.QueryBuilder;

public class QueryBuilderStaticTest {

    @Test
    public void testPlaceSplitWhitelistConditionToQueryEmptyMap() {
        final String query = QueryBuilder.placeSplitWhitelistConditionToQuery(QueryBuilder.SPLIT_WHITELIST_VARIABLE,
            Collections.emptyMap());
        Assert.assertEquals(QueryBuilder.DEFAULT_SPLIT_WHITELIST_VALUE, query);
    }

    @Test
    public void testPlaceSplitWhitelistConditionToQueryGeneralCase() {
        final String query = QueryBuilder.placeSplitWhitelistConditionToQuery(QueryBuilder.SPLIT_WHITELIST_VARIABLE,
            ImmutableMap.of(
                "key1", ImmutableSet.of("val11", "val12"),
                "key2", ImmutableSet.of("val21", "val22", "val23")
            ));
        Assert.assertEquals("(key1 IN (val11, val12) AND key2 IN (val21, val22, val23))", query);
    }

}

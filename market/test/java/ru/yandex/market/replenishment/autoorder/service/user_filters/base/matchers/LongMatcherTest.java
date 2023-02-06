package ru.yandex.market.replenishment.autoorder.service.user_filters.base.matchers;


import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.EQUAL;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.GREATER;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.GREATER_OR_EQUALS;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.LESS;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.LESS_OR_EQUALS;

@RunWith(Parameterized.class)
public class LongMatcherTest {

    private LongMatcher matcher = new LongMatcher();
    private Long filterableValue;
    private UserFilterFieldPredicate predicate;
    private String value;
    private boolean expected;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { null, LESS, "10", true},
                { 10L, LESS, "10", false},
                { 11L, LESS, "10", false},
                { 9L, LESS, "10", true},

                { null, LESS_OR_EQUALS, "10", true},
                { 10L, LESS_OR_EQUALS, "10", true},
                { 11L, LESS_OR_EQUALS, "10", false},
                { 9L, LESS_OR_EQUALS, "10", true},

                { null, GREATER, "10", false},
                { 10L, GREATER, "10", false},
                { 11L, GREATER, "10", true},
                { 9L, GREATER, "10", false},

                { null, GREATER_OR_EQUALS, "10", false},
                { 10L, GREATER_OR_EQUALS, "10", true},
                { 11L, GREATER_OR_EQUALS, "10", true},
                { 9L, GREATER_OR_EQUALS, "10", false},

                { null, EQUAL, "10", false},
                { 9L, EQUAL, "10", false},
                { 10L, EQUAL, "10", true},
        });
    }


    public LongMatcherTest(Long filterableValue, UserFilterFieldPredicate predicate, String value, boolean expected) {
        this.filterableValue = filterableValue;
        this.predicate = predicate;
        this.value = value;
        this.expected = expected;
    }

    @Test
    public void shouldReturnCorrectSum() {
        boolean actual = matcher.matches(filterableValue, predicate, value);
        if (expected)
            assertTrue(actual);
        else
            assertFalse(actual);
    }

}

package ru.yandex.market.replenishment.autoorder.service.user_filters.base.matchers;


import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.CONTAINS;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.EQUAL;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.STARTS_WITH;

@RunWith(Parameterized.class)
public class StringMatcherTest {

    private StringMatcher matcher = new StringMatcher();
    private String filterableValue;
    private UserFilterFieldPredicate predicate;
    private String value;
    private boolean expected;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {null, EQUAL, "10", false},
                {"10", EQUAL, "10", true},
                {"101", EQUAL, "10", false},
                {"110", EQUAL, "10", false},

                {null, STARTS_WITH, "10", false},
                {"10", STARTS_WITH, "10", true},
                {"101", STARTS_WITH, "10", true},
                {"110", STARTS_WITH, "10", false},

                {null, CONTAINS, "10", false},
                {"10", CONTAINS, "10", true},
                {"101", CONTAINS, "10", true},
                {"110", CONTAINS, "10", true},
        });
    }


    public StringMatcherTest(String filterableValue, UserFilterFieldPredicate predicate, String value,
                             boolean expected) {
        this.filterableValue = filterableValue;
        this.predicate = predicate;
        this.value = value;
        this.expected = expected;
    }

    @Test
    public void shouldReturnCorrectSum() {
        boolean actual = matcher.matches(filterableValue, predicate, value);
        if (expected) {
            assertTrue(actual);
        } else {
            assertFalse(actual);
        }
    }

}

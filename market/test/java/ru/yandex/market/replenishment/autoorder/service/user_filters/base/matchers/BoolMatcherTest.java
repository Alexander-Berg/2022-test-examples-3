package ru.yandex.market.replenishment.autoorder.service.user_filters.base.matchers;


import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.FALSE;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.TRUE;

@RunWith(Parameterized.class)
public class BoolMatcherTest {

    private BoolMatcher matcher = new BoolMatcher();
    private Boolean filterableValue;
    private UserFilterFieldPredicate predicate;
    private boolean expected;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { null, TRUE, false},
                { true, TRUE, true},
                { null, FALSE, true},
                { true, FALSE, false},
        });
    }


    public BoolMatcherTest(Boolean filterableValue, UserFilterFieldPredicate predicate, boolean expected) {
        this.filterableValue = filterableValue;
        this.predicate = predicate;
        this.expected = expected;
    }

    @Test
    public void shouldReturnCorrectSum() {
        boolean actual = matcher.matches(filterableValue, predicate, "");
        if (expected)
            assertTrue(actual);
        else
            assertFalse(actual);
    }

}

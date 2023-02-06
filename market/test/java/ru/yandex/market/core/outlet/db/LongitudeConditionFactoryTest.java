package ru.yandex.market.core.outlet.db;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import ru.yandex.common.framework.filter.QueryCondition;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.core.outlet.db.condition.LongitudeConditionFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit тесты для {@link LongitudeConditionFactory}.
 *
 * @author avetokhin 19/12/17.
 */
class LongitudeConditionFactoryTest {

    private static final String COL_NAME = "longitude";
    private static final double LEFT_1 = 10.5;
    private static final double RIGHT_1 = 50.6;
    private static final double LEFT_2 = 30;
    private static final double RIGHT_2 = -10;

    @Test
    void testNormal() {
        final QueryCondition condition = new LongitudeConditionFactory(COL_NAME)
                .getCondition(new Pair<>(LEFT_1, RIGHT_1));

        assertThat(condition.getCondition(), equalTo("(longitude > ? and longitude <= ?)"));
        assertThat(condition.getParams(), equalTo(Arrays.asList(LEFT_1, RIGHT_1)));
    }

    @Test
    void testInverted() {
        final QueryCondition condition = new LongitudeConditionFactory(COL_NAME)
                .getCondition(new Pair<>(LEFT_2, RIGHT_2));

        assertThat(condition.getCondition(), equalTo(" not ((longitude <= ? and longitude > ?))"));
        assertThat(condition.getParams(), equalTo(Arrays.asList(LEFT_2, RIGHT_2)));
    }

}

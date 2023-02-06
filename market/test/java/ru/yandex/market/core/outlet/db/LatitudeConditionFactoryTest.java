package ru.yandex.market.core.outlet.db;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import ru.yandex.common.framework.filter.QueryCondition;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.core.outlet.db.condition.LatitudeConditionFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit тесты для {@link LatitudeConditionFactory}.
 *
 * @author avetokhin 19/12/17.
 */
class LatitudeConditionFactoryTest {

    private static final String COL_NAME = "latitude";
    private static final double LEFT_1 = 10.5;
    private static final double RIGHT_1 = 50.6;

    @Test
    void test() {
        final QueryCondition condition = new LatitudeConditionFactory(COL_NAME)
                .getCondition(new Pair<>(LEFT_1, RIGHT_1));

        assertThat(condition.getCondition(), equalTo("(latitude > ? and latitude <= ?)"));
        assertThat(condition.getParams(), equalTo(Arrays.asList(LEFT_1, RIGHT_1)));
    }

}

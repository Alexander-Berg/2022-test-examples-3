package ru.yandex.market.sqb.model.conf;

import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.service.builder.QueryBuilderService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit-тесты для {@link OrderType}.
 *
 * @author Vladislav Bauer
 */
class OrderTypeTest {

    private static final int TYPES_COUNT = 2;


    @Test
    void testContract() {
        final OrderType[] types = OrderType.values();

        assertThat(
                String.format(
                        "%s enum was changed. Probably, you need to update %s",
                        OrderType.class, QueryBuilderService.class
                ),
                types.length, equalTo(TYPES_COUNT)
        );
    }

}

package ru.yandex.market.sqb.model.render;

import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.service.builder.QueryBuilderService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit-тесты для {@link RenderingType}.
 *
 * @author Vladislav Bauer
 */
class RenderingTypeTest {

    private static final int TYPES_COUNT = 3;


    @Test
    void testContract() {
        final RenderingType[] types = RenderingType.values();

        assertThat(
                String.format(
                        "%s enum was changed. Probably, you need to update %s",
                        RenderingType.class, QueryBuilderService.class
                ),
                types.length, equalTo(TYPES_COUNT)
        );
    }

}

package ru.yandex.market.api.util;

import org.junit.Test;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.integration.UnitTestBase;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class PagingTest extends UnitTestBase {

    @Test
    public void testPage() {
        assertEquals(
                Arrays.asList(4, 5, 6),
                Paging.page(
                        Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                        new PageInfo(2, 3)
                )
        );

        assertEquals(
                Arrays.asList(10),
                Paging.page(
                        Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                        new PageInfo(4, 3)
                )
        );

        assertEquals(
                Collections.emptyList(),
                Paging.page(
                        Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                        new PageInfo(5, 3)
                )
        );

        assertEquals(
                Collections.emptyList(),
                Paging.page(
                        Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                        new PageInfo(0, 3)
                )
        );
    }
}

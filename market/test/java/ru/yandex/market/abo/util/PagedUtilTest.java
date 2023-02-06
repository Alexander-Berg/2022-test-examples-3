package ru.yandex.market.abo.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author imelnikov
 */
public class PagedUtilTest {

    @Test
    public void iterat() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        int n = 0;
        for (Collection<Integer> s : PagedUtil.iterateOver(i -> list.subList(i, Math.min(i+1, list.size())))) {
            n++;
        }
        assertEquals(list.size(), n);
    }
}

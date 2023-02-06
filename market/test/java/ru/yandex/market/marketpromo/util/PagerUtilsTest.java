package ru.yandex.market.marketpromo.util;

import org.junit.jupiter.api.Test;

import ru.yandex.market.marketpromo.web.model.response.Pages;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PagerUtilsTest {

    @Test
    void shouldCalcPagesCorrectly() {
        Pages firstPage = PagerUtils.pages(0, 100, 234);
        assertEquals(1, firstPage.getCurrent(), "wrong current page");
        assertEquals(3, firstPage.getTotal(), "wrong total number of pages");
        assertEquals(234, firstPage.getTotalItems(), "wrong total number of items");
        Pages lastPage = PagerUtils.pages(200, 100, 234);
        assertEquals(3, lastPage.getCurrent(), "wrong current page");
        assertEquals(3, lastPage.getTotal(), "wrong total number of pages");
        assertEquals(234, lastPage.getTotalItems(), "wrong total number of items");
    }
}

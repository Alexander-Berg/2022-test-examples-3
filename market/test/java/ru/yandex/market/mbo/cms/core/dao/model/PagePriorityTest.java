package ru.yandex.market.mbo.cms.core.dao.model;

import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Commince
 * @date 28.11.2018
 */
public class PagePriorityTest {
    private static final int MIN_PRIORITY = 10;
    private static final int MAX_PRIORITY = 1;

    private static final Date NOW = new Date();
    private static final Date LONG_TIME_AGO = new Date(0);

    private static final String PAGE_TYPE = "type";

    @Test
    public void compareTo() throws Exception {
        PagePriority p1;
        PagePriority p2;

        p1 = new PagePriority(0, NOW, MAX_PRIORITY, PAGE_TYPE);
        p2 = new PagePriority(0, NOW, MIN_PRIORITY, PAGE_TYPE);
        assertTrue(p1.compareTo(p2) > 0);

        p1 = new PagePriority(0, NOW, MIN_PRIORITY, PAGE_TYPE);
        p2 = new PagePriority(0, LONG_TIME_AGO, MIN_PRIORITY, PAGE_TYPE);
        assertTrue(p1.compareTo(p2) > 0);

        p1 = new PagePriority(0, LONG_TIME_AGO, MAX_PRIORITY, PAGE_TYPE);
        p2 = new PagePriority(0, NOW, MIN_PRIORITY, PAGE_TYPE);
        assertTrue(p1.compareTo(p2) > 0);
    }

}

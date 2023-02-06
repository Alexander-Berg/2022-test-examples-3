package ru.yandex.market.books.diff.dao;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;

/**
 * todo описать предназначение
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class DiffDaoUtilsTest extends TestCase {
    public void testGetIsbnsListAsString() {
        assertEquals(
                "",
                DiffDaoUtils.getIsbnsListAsString(Collections.<Long>emptyList())
        );
        assertEquals(
                "9780000000019",
                DiffDaoUtils.getIsbnsListAsString(Arrays.asList(1L))
        );
        assertEquals(
                "9780000000019\n9780000000026\n9780000000033\n9780000000040",
                DiffDaoUtils.getIsbnsListAsString(Arrays.asList(1L, 2L, 3L, 4L))
        );
    }

    public void testGetYearsListAsString() {
        assertEquals(
                "",
                DiffDaoUtils.getYearsListAsString(Collections.<Integer>emptyList())
        );
        assertEquals(
                "2005",
                DiffDaoUtils.getYearsListAsString(Arrays.asList(2005))
        );
        assertEquals(
                "2003\n2004\n2005\n2006\n2007",
                DiffDaoUtils.getYearsListAsString(Arrays.asList(2003, 2004, 2005, 2006, 2007))
        );
    }
}

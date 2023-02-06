package ru.yandex.vendor.util;

import org.junit.Test;
import ru.yandex.market.vendor.util.Pager;
import ru.yandex.vendor.exception.BadParamException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Created by zaharov-i on 29.09.16.
 */
public class PagerTest {

    @Test
    public void sunnyCase() throws Exception {
        Integer expectedTotal = 10;
        Integer expectedPageSize = 2;
        Integer expectedCurrentPage = 3;
        Integer expectedFrom = 5;
        Integer expectedTo = 6;
        Integer expectedPagesCount = 5;

        Pager pager = new Pager(expectedTotal, expectedPageSize, expectedCurrentPage);

        assertThat("Wrong total items count. ", pager.getTotal(), is(expectedTotal));
        assertThat("Wrong current page number. ", pager.getCurrentPage(), is(expectedCurrentPage));
        assertThat("Wrong FROM value. ", pager.getFrom(), is(expectedFrom));
        assertThat("Wrong TO value. ", pager.getTo(), is(expectedTo));
        assertThat("Wrong pages count. ", pager.getPagesCount(), is(expectedPagesCount));
    }

    @Test
    public void shouldReturnCorrectFromToWhenCurrentPageExceedsTotal() throws Exception {
        Integer expectedTotal = 10;
        Integer expectedPageSize = 2;
        Integer expectedCurrentPage = 3000;
        Integer expectedFrom = 5999;
        Integer expectedTo = 6000;
        Integer expectedPagesCount = 5;

        Pager pager = new Pager(expectedTotal, expectedPageSize, expectedCurrentPage);

        assertThat("Wrong total items count. ", pager.getTotal(), is(expectedTotal));
        assertThat("Wrong current page number. ", pager.getCurrentPage(), is(expectedCurrentPage));
        assertThat("Wrong FROM value. ", pager.getFrom(), is(expectedFrom));
        assertThat("Wrong TO value. ", pager.getTo(), is(expectedTo));
        assertThat("Wrong pages count. ", pager.getPagesCount(), is(expectedPagesCount));
    }

    @Test
    public void shouldNotThrowExceptionWhenTotalIsZero() throws Exception {
        Integer expectedTotal = 0;
        Integer expectedPageSize = 2;
        Integer expectedCurrentPage = 1;
        Integer expectedPagesCount = 0;

        Pager pager = new Pager(expectedTotal, expectedPageSize, expectedCurrentPage);

        assertThat("Wrong total items count. ", pager.getTotal(), is(expectedTotal));
        assertThat("Wrong current page number. ", pager.getCurrentPage(), is(expectedCurrentPage));
        assertThat("Wrong pages count. ", pager.getPagesCount(), is(expectedPagesCount));
    }

    @Test
    public void shouldThrowExceptionIfTotalIsNegative() throws Exception {
        Integer expectedTotal = -10;
        Integer expectedPageSize = 2;
        Integer expectedCurrentPage = 3;

        try {
            new Pager(expectedTotal, expectedPageSize, expectedCurrentPage);
            fail("No BadParamException thrown ");
        } catch (BadParamException e) {
            // it's ok
        }
    }

    @Test
    public void shouldThrowExceptionWhenTotalIsNull() throws Exception {
        Integer expectedTotal = null;
        Integer expectedPageSize = 2;
        Integer expectedCurrentPage = 1;

        try {
            new Pager(expectedTotal, expectedPageSize, expectedCurrentPage);
            fail("No BadParamException thrown ");
        } catch (BadParamException e) {
            // it's ok
        }
    }

    @Test
    public void shouldReturnPagesCountZeroIfPageSizeIsZero() throws Exception {
        Integer expectedTotal = 10;
        Integer expectedPageSize = 0;
        Integer expectedCurrentPage = 3;

        Pager pager = new Pager(expectedTotal, expectedPageSize, expectedCurrentPage);

        assertThat("Wrong total items count. ", pager.getTotal(), is(expectedTotal));
        assertThat("Wrong current page number. ", pager.getCurrentPage(), is(expectedCurrentPage));
        assertThat("Wrong FROM value. ", pager.getFrom(), is(1));
        assertThat("Wrong TO value. ", pager.getTo(), is(expectedTotal));
        assertThat("Wrong pages count. ", pager.getPagesCount(), is(0));
    }

    @Test
    public void shouldThrowExceptionIfPageSizeIsNegative() throws Exception {
        Integer expectedTotal = 10;
        Integer expectedPageSize = -2;
        Integer expectedCurrentPage = 3;

        try {
            new Pager(expectedTotal, expectedPageSize, expectedCurrentPage);
            fail("No BadParamException thrown ");
        } catch (BadParamException e) {
            // it's ok
        }
    }

    @Test
    public void shouldThrowExceptionIfPageSizeIsNull() throws Exception {
        Integer expectedTotal = 10;
        Integer expectedPageSize = null;
        Integer expectedCurrentPage = 3;

        try {
            new Pager(expectedTotal, expectedPageSize, expectedCurrentPage);
            fail("No BadParamException thrown ");
        } catch (BadParamException e) {
            // it's ok
        }
    }

    @Test
    public void shouldThrowExceptionIfCurrentPageIsZero() throws Exception {
        Integer expectedTotal = 10;
        Integer expectedPageSize = 2;
        Integer expectedCurrentPage = 0;

        try {
            new Pager(expectedTotal, expectedPageSize, expectedCurrentPage);
            fail("No BadParamException thrown ");
        } catch (BadParamException e) {
            // it's ok
        }
    }

    @Test
    public void shouldThrowExceptionIfCurrentPageIsNegative() throws Exception {
        Integer expectedTotal = 10;
        Integer expectedPageSize = 2;
        Integer expectedCurrentPage = -3;

        try {
            new Pager(expectedTotal, expectedPageSize, expectedCurrentPage);
            fail("No BadParamException thrown ");
        } catch (BadParamException e) {
            // it's ok
        }
    }

    @Test
    public void shouldReturnCurrentPageOneIfItWasNotSet() throws Exception {
        Integer expectedTotal = 10;
        Integer expectedPageSize = 2;
        Integer expectedCurrentPage = null;

        Pager pager = new Pager(expectedTotal, expectedPageSize, expectedCurrentPage);

        assertThat("Wrong current page number. ", pager.getCurrentPage(), is(1));
    }
}
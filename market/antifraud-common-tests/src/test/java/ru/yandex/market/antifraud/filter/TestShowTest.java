package ru.yandex.market.antifraud.filter;

import org.junit.Test;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class TestShowTest {
    @Test
    public void testUpperToUnderscore() {
        assertThat(TestShow.upperToUnderscore("categId"), is("categ_id"));
        assertThat(TestShow.upperToUnderscore("rowid"), is("rowid"));
        assertThat(TestShow.upperToUnderscore("a_b"), is("a_b"));
        assertThat(TestShow.upperToUnderscore("Row"), is("Row"));
        assertThat(TestShow.upperToUnderscore("ipGeoId"), is("ip_geo_id"));
    }

    @Test
    public void testGetFieldValue() {
        TestShow show = new TestShow();
        show.setBsBlockId("bsblockid1");
        show.setBid(18);
        show.setFilter(FilterConstants.FILTER_12);
        show.setIpGeoId(17);

        assertThat(TestShow.getFieldValueByCol(show, "bs_block_id"),
                is("bsblockid1"));
        assertThat(TestShow.getFieldValueByCol(show, "bid"),
                is(18));
        assertThat(TestShow.getFieldValueByCol(show, "ip_geo_id"),
                is(17));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSystemFieldValue() {
        TestShow show = new TestShow();
        show.setFilter(FilterConstants.FILTER_12);
        TestShow.getFieldValueByCol(show, "filter");
    }

    @Test
    public void testGetUnsetFieldValue() {
        TestShow show = new TestShow();
        assertThat(TestShow.getFieldValueByCol(show, "bs_block_id"),
                nullValue());
    }
}

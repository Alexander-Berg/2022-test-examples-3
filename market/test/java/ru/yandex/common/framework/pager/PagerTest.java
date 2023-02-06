/**
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: Yandex</p>
 * <p>Date: 07.08.2006</p>
 * <p>Time: 16:24:21</p>
 */
package ru.yandex.common.framework.pager;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class PagerTest {
    @Test
    public void testEmptyPager() {
        Pager pager = new Pager(3, 10);
        StringBuilder result = new StringBuilder();
        pager.toXml(result);
        assertThat(result).isEmpty();
    }

    @Test
    public void testBigPager() {
        Pager pager = new Pager(33, 10);
        pager.setCount(1000);
        StringBuilder result = new StringBuilder();
        pager.toXml(result);
        assertThat(result).hasToString(
                "<pager count=\"1000\" page-size=\"10\" show-all=\"\" total-page-count=\"100\">" +
                        "<page num=\"26\"/><page num=\"27\"/><page num=\"28\"/>" +
                        "<page num=\"29\"/><page num=\"30\"/><page num=\"31\"/>" +
                        "<page num=\"32\"/><page num=\"33\" current=\"true\"/>" +
                        "<page num=\"34\"/><page num=\"35\"/><page num=\"36\"/>" +
                        "<page num=\"37\"/><page num=\"38\"/><page num=\"39\"/><page num=\"40\"/>" +
                        "</pager>"
        );
    }

    @Test
    public void testTooBigPager() {
        Pager pager = new Pager(500, 10);
        pager.setCount(400);
        StringBuilder result = new StringBuilder();
        pager.toXml(result);
        assertThat(result).hasToString(
                "<pager count=\"400\" page-size=\"10\" show-all=\"\" total-page-count=\"40\">" +
                        "<page num=\"32\"/>" + "<page num=\"33\"/>" +
                        "<page num=\"34\"/><page num=\"35\"/><page num=\"36\"/>" +
                        "<page num=\"37\"/><page num=\"38\"/><page num=\"39\" current=\"true\"/>" +
                        "</pager>"
        );
    }

    @Test
    public void testBetween() {
        Pager pager = new Pager(0, 2);
        pager.setCount(8);
        assertThat(pager.getPageSize()).isEqualTo(2);
        assertThat(pager.getFrom()).isEqualTo(1);
        assertThat(pager.getTo()).isEqualTo(2);

        pager = new Pager(3, 2);
        pager.setCount(8);
        assertThat(pager.getFrom()).isEqualTo(7);
        assertThat(pager.getTo()).isEqualTo(8);

        pager = new Pager(4, 2);
        pager.setCount(8);
        assertThat(pager.getFrom()).isEqualTo(7);
        assertThat(pager.getTo()).isEqualTo(8);

        pager = new Pager(4, 2);
        pager.setCount(9);
        assertThat(pager.getFrom()).isEqualTo(9);
        assertThat(pager.getTo()).isEqualTo(10);
    }

}

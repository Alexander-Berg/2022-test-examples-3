package ru.yandex.market.loyalty.core.dao;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.spring.utils.DaoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.lightweight.CommonUtils.getNonNullValue;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class DaoUtilsTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testReadArrayOfString() {
        Object a = new String[]{"1"};
        assertThat(DaoUtils.readArray(a, String.class), contains("1"));
    }

    @Test
    public void testReadArrayOfLong() {
        Object a = new Long[]{1L};
        assertThat(DaoUtils.readArray(a, Long.class), contains(1L));
    }

    @Test
    public void testReadPrimitiveArrayOfLong() {
        Object a = new long[]{1L};
        assertThat(DaoUtils.readArray(a, Long.class), contains(1L));
    }

    @Test
    public void testReadArrayOfInt() {
        Object a = new Integer[]{1};
        assertThat(DaoUtils.readArray(a, Integer.class), contains(1));
    }

    @Test
    public void testReadPrimitiveArrayOfInt() {
        Object a = new int[]{1};
        assertThat(DaoUtils.readArray(a, Integer.class), contains(1));
    }

    @Test
    public void testReadArrayOfDouble() {
        Object a = new Double[]{1.0};
        assertThat(DaoUtils.readArray(a, Double.class), contains(1.0));
    }

    @Test
    public void testReadPrimitiveArrayOfDouble() {
        Object a = new double[]{1.0};
        assertThat(DaoUtils.readArray(a, Double.class), contains(1.0));
    }

    @Test
    public void testExtractLongArray() {
        assertArrayEquals(
                new Long[]{0L, 1L},
                jdbcTemplate.queryForObject("SELECT ARRAY[0::BIGINT, 1::BIGINT] AS arr", (rs, i) ->
                        DaoUtils.extractLongArray(rs, "ARR")
                )
        );
    }

    @Test
    public void testExtractIntegerArray() {
        assertArrayEquals(
                new Integer[]{0, 1},
                jdbcTemplate.queryForObject("SELECT ARRAY[0, 1] AS arr", (rs, i) ->
                        DaoUtils.extractIntegerArray(rs, "ARR")
                )
        );
    }

    @Test
    public void testExecuteWithArrays() {
        class Holder {
            private Integer[] arr1;
            private String[] arr2;
        }
        Holder actual = getNonNullValue(
                jdbcTemplate.queryForObject("SELECT ARRAY[0, 1] AS arr1, ARRAY['val1', 'val2'] AS arr2", (rs, i) ->
                        DaoUtils.executeWithArrays(arrays -> {
                            Holder ret = new Holder();
                            ret.arr1 = DaoUtils.toIntegerArray(arrays.get("ARR1"));
                            ret.arr2 = DaoUtils.toStringArray(arrays.get("ARR2"));
                            return ret;
                        }, rs, "ARR1", "ARR2")
                )
        );
        assertArrayEquals(new Integer[]{0, 1}, actual.arr1);
        assertArrayEquals(new String[]{"val1", "val2"}, actual.arr2);
    }

    @Test
    public void testExecuteWithEmptyArrays() {
        Object touch = new Object();
        assertEquals(
                touch,
                jdbcTemplate.queryForObject("SELECT ARRAY[0::BIGINT, 1::BIGINT] AS arr", (rs, i) ->
                        DaoUtils.executeWithArrays(arrays -> {
                            assertThat(arrays.entrySet(), empty());
                            return touch;
                        }, rs)
                )
        );
    }
}

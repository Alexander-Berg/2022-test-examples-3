package ru.yandex.market.mbo.db;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.common.util.db.StringRowMapper;

/**
 * @author apluhin
 * @created 7/19/21
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class CachedKeyValueMapServiceTest {

    private static final String TEST_KEY = "test";
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private CachedKeyValueMapService keyValueMapService;

    @Before
    public void setUp() throws Exception {
        keyValueMapService = new CachedKeyValueMapService();
        namedParameterJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        ReflectionTestUtils.setField(keyValueMapService, "namedScatJdbcTemplate", namedParameterJdbcTemplate);
    }

    @Test
    public void testReadNullableFromCache() {
        List<Integer> test = keyValueMapService.getList(TEST_KEY, Integer.class);
        keyValueMapService.getList(TEST_KEY, Integer.class);

        verifyCount(1);
        verifyQuery(0);
    }

    @Test
    public void testReadFromCache() throws SQLException {
        Mockito.when(namedParameterJdbcTemplate.queryForObject(
            Mockito.anyString(),
            Mockito.any(MapSqlParameterSource.class),
            Mockito.any(RowMapper.class))).thenReturn("[1,2,3]");
        Mockito.when(namedParameterJdbcTemplate.queryForObject(
            Mockito.anyString(),
            Mockito.any(MapSqlParameterSource.class),
            Mockito.eq(Integer.class))).thenReturn(1);

        List<Integer> test = keyValueMapService.getList(TEST_KEY, Integer.class);
        List<Integer> secondCall = keyValueMapService.getList(TEST_KEY, Integer.class);

        Assert.assertEquals(test, secondCall);
        Assert.assertEquals(Arrays.asList(1, 2, 3), test);

        verifyCount(1);
        verifyQuery(1);
    }

    private void verifyCount(int count) {
        Mockito.verify(namedParameterJdbcTemplate, Mockito.times(count)).queryForObject(
            Mockito.anyString(),
            Mockito.any(MapSqlParameterSource.class),
            Mockito.eq(Integer.class));
    }

    private void verifyQuery(int count) {
        Mockito.verify(namedParameterJdbcTemplate, Mockito.times(count)).queryForObject(
            Mockito.anyString(),
            Mockito.any(MapSqlParameterSource.class),
            Mockito.any(StringRowMapper.class));
    }
}

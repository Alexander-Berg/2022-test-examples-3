package ru.yandex.market.notification.safe.dao.db;

import java.util.Map;

import org.junit.Test;

import ru.yandex.market.notification.safe.dao.db.NamedParameterSqlParser.NamedParameterInfo;
import ru.yandex.market.notification.test.util.ClassUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Утилитный класс для {@link NamedParameterSqlParser}.
 *
 * @author Vladislav Bauer
 */
public class NamedParameterSqlParserTest {

    @Test
    public void testConstructor() {
        ClassUtils.checkConstructor(NamedParameterSqlParser.class);
    }

    @Test
    public void testParse() {
        final String namedSql = "select * from test where id = :id and param = :value";
        final String sql = "select * from test where id = ? and param = ?";

        final NamedParameterInfo info = NamedParameterSqlParser.parse(namedSql);
        final String query = info.getQuery();
        final Map<String, int[]> paramMap = info.getParamMap();

        assertThat(query, equalTo(sql));
        assertThat(paramMap.size(), equalTo(2));
        assertThat(paramMap.get("id"), equalTo(new int[] { 1 }));
        assertThat(paramMap.get("value"), equalTo(new int[] { 2 }));
    }

}

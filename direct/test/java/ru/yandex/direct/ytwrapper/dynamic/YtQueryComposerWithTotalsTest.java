package ru.yandex.direct.ytwrapper.dynamic;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.ytwrapper.dynamic.YtQueryComposerTest.TestTable.TEST_TABLE;

/**
 * Проверяем добавление оператора WITH TOTALS в запросе
 */
@RunWith(Parameterized.class)
public class YtQueryComposerWithTotalsTest {

    private YtQueryComposer queryComposer;

    @Parameterized.Parameter()
    public String description;

    @Parameterized.Parameter(1)
    public String query;

    @Parameterized.Parameter(2)
    public String expectQuery;

    @Parameterized.Parameters(name = "description: {0}, queryValue: {1}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{
                        "Запрос с операторами после GROUP BY -> WITH TOTALS ставится после GROUP BY",
                        "SELECT\n" +
                                "P.pid AS pid\n" +
                                "FROM [//home/phrases] AS P\n" +
                                "WHERE P.cid IN (47442542)\n" +
                                "GROUP BY \n" +
                                "  P.__hash__, \n" +
                                "  P.__shard__\n" +
                                "ORDER BY \n" +
                                "  cid DESC \n" +
                                "LIMIT 100",

                        "SELECT\n" +
                                "P.pid AS pid\n" +
                                "FROM [//home/phrases] AS P\n" +
                                "WHERE P.cid IN (47442542)\n" +
                                "GROUP BY \n" +
                                "  P.__hash__, \n" +
                                "  P.__shard__ \n" +
                                "WITH TOTALS \n" +
                                "ORDER BY \n" +
                                "  cid DESC \n" +
                                "LIMIT 100"},

                new Object[]{
                        "Запрос без перехода на новую строку -> WITH TOTALS ставится после GROUP BY",
                        "SELECT P.pid AS pid FROM [//home/phrases] AS P GROUP BY P.__shard__ LIMIT 100",
                        "SELECT P.pid AS pid FROM [//home/phrases] AS P GROUP BY P.__shard__ \n" +
                                "WITH TOTALS  LIMIT 100"},

                new Object[]{
                        "Запрос с табуляцией -> WITH TOTALS ставится после GROUP BY",
                        "SELECT P.pid AS pid\tFROM [//home/phrases] AS P\tGROUP BY P.__shard__\tLIMIT 100",
                        "SELECT P.pid AS pid\tFROM [//home/phrases] AS P\tGROUP BY P.__shard__ \n" +
                                "WITH TOTALS \tLIMIT 100"},

                new Object[]{
                        "Запрос с большим количеством space -> WITH TOTALS ставится после GROUP BY",
                        "SELECT   P.pid   AS   pid   FROM   [//home/phrases]   AS   P   " +
                                "GROUP BY   P.__shard__   LIMIT 100",
                        "SELECT   P.pid   AS   pid   FROM   [//home/phrases]   AS   P   " +
                                "GROUP BY   P.__shard__ \n" +
                                "WITH TOTALS    LIMIT 100"},

                new Object[]{
                        "Запрос с переходом на новую строку вместо пробелов -> WITH TOTALS ставится после GROUP BY",
                        "SELECT\n" +
                                "P.pid AS pid\n" +
                                "FROM [//home/phrases] AS P\n" +
                                "GROUP BY\n" +
                                "P.__shard__\n" +
                                "ORDER BY\n" +
                                "cid DESC\n" +
                                "LIMIT 100",
                        "SELECT\n" +
                                "P.pid AS pid\n" +
                                "FROM [//home/phrases] AS P\n" +
                                "GROUP BY\n" +
                                "P.__shard__ \n" +
                                "WITH TOTALS \n" +
                                "ORDER BY\n" +
                                "cid DESC\n" +
                                "LIMIT 100"},

                new Object[]{
                        "Запрос без операторов после GROUP BY -> WITH TOTALS ставится в конце запроса",
                        "SELECT\n" +
                                "P.pid AS pid\n" +
                                "FROM [//home/phrases] AS P\n" +
                                "GROUP BY \n" +
                                "  P.__hash__, \n" +
                                "  P.__shard__\n",

                        "SELECT\n" +
                                "P.pid AS pid\n" +
                                "FROM [//home/phrases] AS P\n" +
                                "GROUP BY \n" +
                                "  P.__hash__, \n" +
                                "  P.__shard__ \n" +
                                "WITH TOTALS \n"},

                new Object[]{
                        "Запрос без GROUP BY -> запрос не изменяется",
                        "SELECT\n" +
                                "P.pid AS pid\n" +
                                "FROM [//home/phrases] AS P\n" +
                                "LIMIT 100",

                        "SELECT\n" +
                                "P.pid AS pid\n" +
                                "FROM [//home/phrases] AS P\n" +
                                "LIMIT 100"}
        );
    }

    @Before
    public void setUp() {
        TableMappings tableMappings = () -> ImmutableMap.of(
                TEST_TABLE, "/tmp/test_table"
        );
        queryComposer = new YtQueryComposer(tableMappings);
    }

    @Test
    public void addWithTotalsToQuery() {
        String finalQuery = queryComposer.addWithTotalsToQuery(query);
        assertThat(finalQuery).isEqualTo(expectQuery);
    }
}

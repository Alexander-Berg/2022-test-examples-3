package ru.yandex.market.common.test.jdbc;

import java.util.Arrays;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@RunWith(Parameterized.class)
public class H2SqlTransformerTest {

    private H2SqlTransformer transformer = new H2SqlTransformer();

    @Parameterized.Parameter(0)
    public String source;
    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters
    public static Iterable<Object[]> parameters() {
        return Arrays.asList(
                new String[]{
                        "select 1 from dual",
                        "select 1 from dual"
                },
                new String[]{
                        "select 1 from dual where x in ?",
                        "select 1 from dual where x in ?"
                },
                new String[]{
                        "select 1 from dual where x in (select value(t) from table (CAST(? as shops_web.t_number_tbl)) t)",
                        "select 1 from dual where x in (select t from table(t bigint = ?))"
                },
                new String[]{
                        "select 1 from dual where x in (select value(t) from table (CAST(? as t_number_tbl)) t)",
                        "select 1 from dual where x in (select t from table(t bigint = ?))"
                },
                new String[]{
                        "select 1 from dual where x in (select /*+ cardinality(t 1) */ value(t) from table (CAST(? as shops_web.t_number_tbl)) t)",
                        "select 1 from dual where x in (select t from table(t bigint = ?))"
                },
                new String[]{
                        "select 1 from dual where x in (select /*+ cardinality(t 1) */ value(t) from table (CAST(? as vendors.t_number_tbl)) t)",
                        "select 1 from dual where x in (select t from table(t bigint = ?))"
                },
                new String[]{
                        "select 1 from dual where x in (select /*+ cardinality(t 1) */ value(t) from table (CAST(? as t_number_tbl)) t)",
                        "select 1 from dual where x in (select t from table(t bigint = ?))"
                },
                new String[] {
                        "select 1 from dual where x in (select value(t) from table (CAST(? as shops_web.NTT_VARCHAR2)) t)",
                        "select 1 from dual where x in (select t from table(t varchar = ?))"
                },
                new String[]{
                        "select listagg(distinct reason,',') within group (order by reason) as reasons, " +
                                "listagg(nvl(subreason, ' '), ',') within group (order by subreason) subreasons, " +
                                "listagg(coalesce(problem_type_id, :fakeProblem), ',') within group (order by problem_type_id), " +
                                "listagg(coalesce(to_char(id), ''), ',') within group (order by id), " +
                                "listagg(fake_problem,',') within group (order by problem_type) " +
                                "row_number() over (order by datasource_id ASC) " +
                                "from table where id = :Id",
                        "select group_concat(distinct reason order by reason separator ',') as reasons, " +
                                "group_concat(nvl(subreason, ' ') order by subreason separator ',') subreasons, " +
                                "group_concat(coalesce(problem_type_id, :fakeProblem) order by problem_type_id separator ','), " +
                                "group_concat(coalesce(to_char(id), '') order by id separator ','), " +
                                "group_concat(fake_problem order by problem_type separator ',') " +
                                "rownum() " +
                                "from table where id = :Id"
                },
                new String[]{
                        "select listagg(distinct role, ';') within group (order by role) as role from t group by x",
                        "select group_concat(distinct role order by role separator ';') as role from t group by x",
                },
                new String[] {
                        "INTERVAL '90' DAY",
                        "90"
                }
        );
    }


    @Test
    public void doTest() {
        String result = transformer.transform(source);
        // убрать задублированные пробелы
        result = result.replaceAll(" {2,}", " ");
        assertThat(result, equalTo(expected));
    }

    @Test
    public void testReturnInputQueryIfQueryEmpty() {
        String empty = "";
        Assertions.assertEquals(empty, transformer.transform(empty));
    }

    @Test
    public void testThrowExceptionIfQueryNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> transformer.transform(null));
    }

}

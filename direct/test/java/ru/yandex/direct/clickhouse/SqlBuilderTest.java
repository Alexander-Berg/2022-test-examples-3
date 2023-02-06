package ru.yandex.direct.clickhouse;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.clickhouse.SqlBuilder.column;

public class SqlBuilderTest {
    private SqlBuilder sql;

    @Before
    public void setUp() throws Exception {
        sql = new SqlBuilder();
    }

    @Test
    public void expressionWhenSelectOneColumnAsColumn() {
        sql.select(column("foo"));
        assertThat(sql.toString(), equalTo("SELECT `foo`"));
    }

    @Test
    public void selectedExpressionDoesNotQuotedButAliasQuoted() throws Exception {
        assertThat(sql.selectExpression("count(*)", "cnt").toString(), equalTo("SELECT count(*) as `cnt`"));
    }

    @Test
    public void expressionWhenSelectOneColumnAsString() {
        sql.select("foo");
        assertThat(sql.toString(), equalTo("SELECT `foo`"));
    }

    @Test
    public void expressionWhenSelectManyColumnsAsArrayOfColumns() {
        sql.select(column("foo"), column("bar"));
        assertThat(sql.toString(), equalTo("SELECT `foo`, `bar`"));
    }

    @Test
    public void expressionWhenSelectManyColumnsAsArrayOfStrings() {
        sql.select("foo", "bar");
        assertThat(sql.toString(), equalTo("SELECT `foo`, `bar`"));
    }

    @Test
    public void expressionWhenSelectManyColumnsAsListOfStrings() {
        sql.select(asList("foo", "bar"));
        assertThat(sql.toString(), equalTo("SELECT `foo`, `bar`"));
    }

    @Test
    public void expressionWhenSelectManyColumnsMixedTypes() {
        sql.select("foo").
                select(column("foo-col"), column("bar-col")).
                select("bar", "baz").
                select(asList("barr", "bazz"));
        assertThat(sql.toString(), equalTo("SELECT `foo`, `foo-col`, `bar-col`, `bar`, `baz`, `barr`, `bazz`"));
    }

    @Test
    public void expressionWhenSelectManyColumnsFrom() {
        sql.select(column("foo")).
                select(column("bar"), column("baz")).
                from("universe");
        assertThat(sql.toString(), equalTo("SELECT `foo`, `bar`, `baz` FROM `universe`"));
    }

    @Test
    public void expressionWhenSetWhereByOneColumn() {
        sql.select(column("foo")).
                from("universe").
                where(column("foo"), ">", 10);
        assertThat(sql.toString(), equalTo("SELECT `foo` FROM `universe` WHERE `foo` > ?"));
    }

    @Test
    public void expressionWhenSetWhereByManyColumns() {
        sql.select(column("foo")).
                from("universe").
                where(column("foo"), ">", 10).
                where(column("bar"), "<", 20);
        assertThat(sql.toString(), equalTo("SELECT `foo` FROM `universe` WHERE `foo` > ? AND `bar` < ?"));
    }

    @Test
    public void expressionWhenSetWhereByOneExpression() {
        sql.select(column("foo")).
                from("universe").
                where("`foo` > ?", 10);
        assertThat(sql.toString(), equalTo("SELECT `foo` FROM `universe` WHERE `foo` > ?"));
    }

    @Test
    public void expressionWhenSetWhereByManyExpressions() {
        sql.select(column("foo")).
                from("universe").
                where("(`foo` BETWEEN ? AND ?)", 10, 20).
                where("`bar` < ?", 20);
        assertThat(sql.toString(),
                equalTo("SELECT `foo` FROM `universe` WHERE (`foo` BETWEEN ? AND ?) AND `bar` < ?"));
    }

    @Test
    public void expressionWhenSetWhereByManyExpressionsAndColumns() {
        sql.select(column("foo")).
                from("universe").
                where(column("foo"), ">", 7).
                where("(`bar` BETWEEN ? AND ?)", 18, 24).
                where(column("baz"), "<", 12).
                where("`some_id` = ?", 300);
        assertThat(sql.toString(), equalTo("SELECT `foo` FROM `universe` WHERE `foo` > ? AND " +
                "(`bar` BETWEEN ? AND ?) AND `baz` < ? AND `some_id` = ?"));
    }

    @Test
    public void expressionWhenSetPrewhereByOneColumn() {
        sql.select(column("foo")).
                from("universe").
                prewhere(column("foo"), ">", 10);
        assertThat(sql.toString(), equalTo("SELECT `foo` FROM `universe` PREWHERE `foo` > ?"));
    }

    @Test
    public void expressionWhenSetPrewhereByManyColumns() {
        sql.select(column("foo")).
                from("universe").
                prewhere(column("foo"), ">", 10).
                prewhere(column("bar"), "<", 20);
        assertThat(sql.toString(), equalTo("SELECT `foo` FROM `universe` PREWHERE `foo` > ? AND `bar` < ?"));
    }

    @Test
    public void expressionWhenSetPrewhereByOneExpression() {
        sql.select(column("foo")).
                from("universe").
                prewhere("`foo` > ?", 10);
        assertThat(sql.toString(), equalTo("SELECT `foo` FROM `universe` PREWHERE `foo` > ?"));
    }

    @Test
    public void expressionWhenSetPrewhereByManyExpressions() {
        sql.select(column("foo")).
                from("universe").
                prewhere("(`foo` BETWEEN ? AND ?)", 10, 20).
                prewhere("`bar` < ?", 20);
        assertThat(sql.toString(),
                equalTo("SELECT `foo` FROM `universe` PREWHERE (`foo` BETWEEN ? AND ?) AND `bar` < ?"));
    }

    @Test
    public void expressionWhenSetPrewhereByManyExpressionsAndColumns() {
        sql.select(column("foo")).
                from("universe").
                prewhere(column("foo"), ">", 7).
                prewhere("(`bar` BETWEEN ? AND ?)", 18, 24).
                prewhere(column("baz"), "<", 12).
                prewhere("`some_id` = ?", 300);
        assertThat(sql.toString(), equalTo("SELECT `foo` FROM `universe` PREWHERE `foo` > ? AND " +
                "(`bar` BETWEEN ? AND ?) AND `baz` < ? AND `some_id` = ?"));
    }

    @Test
    public void expressionWhenSetWhereAndPrewhereByExpressionsAndColumns() {
        sql.select(column("foo")).
                from("universe").
                prewhere(column("foo"), ">", 7).
                where("(`bar` BETWEEN ? AND ?)", 18, 24).
                where(column("baz"), "<", 12).
                prewhere("`some_id` = ?", 300);
        assertThat(sql.toString(), equalTo("SELECT `foo` FROM `universe` " +
                "PREWHERE `foo` > ? AND `some_id` = ? WHERE (`bar` BETWEEN ? AND ?) AND `baz` < ?"));
    }

    @Test
    public void expressionWithLimit() {
        sql.select(column("foo")).
                from("universe").
                where(column("foo"), ">", 10).
                prewhere(column("bar"), "<", 20).
                limit(140);
        assertThat(sql.toString(), equalTo("SELECT `foo` FROM `universe` " +
                "PREWHERE `bar` < ? WHERE `foo` > ? LIMIT ?"));
    }

    @Test
    public void expressionWithOffsetAndLimit() {
        sql.select(column("foo")).
                from("universe").
                where(column("foo"), ">", 10).
                prewhere(column("bar"), "<", 20).
                limit(30, 140);
        assertThat(sql.toString(), equalTo("SELECT `foo` FROM `universe` " +
                "PREWHERE `bar` < ? WHERE `foo` > ? LIMIT ?, ?"));
    }

    // order by

    @Test
    public void orderBy() {
        sql.select(column("foo")).
                from("universe").
                where(column("foo"), ">", 10).
                orderBy(column("foo"), SqlBuilder.Order.ASC);
        assertThat(sql.toString(), equalTo("SELECT `foo` FROM `universe` WHERE `foo` > ? ORDER BY `foo` ASC"));
    }

    // bindings

    @Test
    public void bindingsWhenSetWhereByOneColumn() {
        sql.select(column("foo")).
                from("universe").
                where(column("foo"), ">", 4);
        assertThat(sql.getBindings(), beanDiffer(new Object[]{4}));
    }

    @Test
    public void bindingsWhenSetWhereByManyColumns() {
        sql.select(column("foo")).
                from("universe").
                where(column("foo"), ">", 7).
                where(column("bar"), "<", 15);
        assertThat(sql.getBindings(), beanDiffer(new Object[]{7, 15}));
    }

    @Test
    public void bindingsWhenSetWhereByOneExpressionWithOneValue() {
        sql.select(column("foo")).
                from("universe").
                where("`foo` > ?", 10);
        assertThat(sql.getBindings(), beanDiffer(new Object[]{10}));
    }

    @Test
    public void bindingsWhenSetWhereByOneExpressionWithTwoValues() {
        sql.select(column("foo")).
                from("universe").
                where("(`foo` BETWEEN ? AND ?)", 12, 20);
        assertThat(sql.getBindings(), beanDiffer(new Object[]{12, 20}));
    }

    @Test
    public void bindingsWhenSetWhereByManyExpressionWithManyValuesWithMixedOrder() {
        sql.select(column("foo")).
                where("(`foo` BETWEEN ? AND ?)", 18, 24).
                from("universe").
                where("`bar` < ?", 20);
        assertThat(sql.getBindings(), beanDiffer(new Object[]{18, 24, 20}));
    }

    @Test
    public void bindingsWhenSetWhereByManyColumnsAndExpressionsWithMixedOrder() {
        sql.select(column("foo")).
                where(column("foo"), ">", 7).
                where("(`bar` BETWEEN ? AND ?)", 18, 24).
                from("universe").
                where(column("baz"), "<", 12).
                where("`some_id` = ?", 300);
        assertThat(sql.getBindings(), beanDiffer(new Object[]{7, 18, 24, 12, 300}));
    }

    @Test
    public void bindingsWhenSetPrewhereByOneColumn() {
        sql.select(column("foo")).
                from("universe").
                prewhere(column("foo"), ">", 4);
        assertThat(sql.getBindings(), beanDiffer(new Object[]{4}));
    }

    @Test
    public void bindingsWhenSetPrewhereByManyColumns() {
        sql.select(column("foo")).
                from("universe").
                prewhere(column("foo"), ">", 7).
                prewhere(column("bar"), "<", 15);
        assertThat(sql.getBindings(), beanDiffer(new Object[]{7, 15}));
    }

    @Test
    public void bindingsWhenSetPrewhereByOneExpressionWithOneValue() {
        sql.select(column("foo")).
                from("universe").
                prewhere("`foo` > ?", 10);
        assertThat(sql.getBindings(), beanDiffer(new Object[]{10}));
    }

    @Test
    public void bindingsWhenSetPrewhereByOneExpressionWithTwoValues() {
        sql.select(column("foo")).
                from("universe").
                prewhere("(`foo` BETWEEN ? AND ?)", 12, 20);
        assertThat(sql.getBindings(), beanDiffer(new Object[]{12, 20}));
    }

    @Test
    public void bindingsWhenSetPrewhereByManyExpressionWithManyValuesWithMixedOrder() {
        sql.select(column("foo")).
                prewhere("(`foo` BETWEEN ? AND ?)", 18, 24).
                from("universe").
                prewhere("`bar` < ?", 20);
        assertThat(sql.getBindings(), beanDiffer(new Object[]{18, 24, 20}));
    }

    @Test
    public void bindingsWhenSetPrewhereByManyColumnsAndExpressionsWithMixedOrder() {
        sql.select(column("foo")).
                prewhere(column("foo"), ">", 7).
                prewhere("(`bar` BETWEEN ? AND ?)", 18, 24).
                from("universe").
                prewhere(column("baz"), "<", 12).
                prewhere("`some_id` = ?", 300);
        assertThat(sql.getBindings(), beanDiffer(new Object[]{7, 18, 24, 12, 300}));
    }

    @Test
    public void bindingsWhenSetPrewhereAndWhereByColumnsAndExpressionsWithMixedOrder() {
        sql.select(column("foo")).
                where(column("foo"), ">", 7).
                prewhere("(`bar` BETWEEN ? AND ?)", 18, 24).
                from("universe").
                prewhere(column("baz"), "<", 12).
                where("`some_id` = ?", 300);
        assertThat(sql.getBindings(), beanDiffer(new Object[]{18, 24, 12, 7, 300}));
    }

    @Test
    public void bindingsOfDifferentTypes() {
        sql.select(column("foo")).
                from("universe").
                where(column("foo"), ">", 7).
                where("(`bar` BETWEEN ? AND ?)", 18L, 24L).
                where(column("baz"), "<", 12.0).
                where("`some_id` = ?", 300.0d);
        assertThat(sql.getBindings(), beanDiffer(new Object[]{7, 18L, 24L, 12.0, 300.0d}));
    }

    @Test
    public void whereInWithMultipleValues() {
        sql.select(column("foo")).
                from("universe").
                whereIn(column("foo"), asList(2, 3, 4));
        assertThat(sql.toString(), equalTo("SELECT `foo` FROM `universe` WHERE `foo` in (?, ?, ?)"));
        assertThat(sql.getBindings(), beanDiffer(new Object[]{2, 3, 4}));
    }

    @Test
    public void whereInWithOneValue() {
        sql.select("foo")
                .from("universe")
                .whereIn(column("foo"), asList(1234));
        assertThat(sql.toString(), equalTo("SELECT `foo` FROM `universe` WHERE `foo` = ?"));
        assertThat(sql.getBindings(), beanDiffer(new Object[]{1234}));
    }

    @Test
    public void whereNot() {
        sql.select("foo")
                .from("universe")
                .whereNot(new SqlBuilder.ExpressionWithBinds("`foo` = ?", 17));
        Assertions.assertThat(sql.toString()).isEqualTo("SELECT `foo` FROM `universe` WHERE NOT (`foo` = ?)");
        Assertions.assertThat(sql.getBindings()).isEqualTo(new Object[] {17});
    }

    @Test
    public void selectFinal() {
        sql.select("foo")
                .from("universe")
                .selectFinal()
                .where("2 + 2 = 4");
        assertThat(sql.toString(), equalTo("SELECT `foo` FROM `universe` FINAL WHERE 2 + 2 = 4"));
    }
}

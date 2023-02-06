package ru.yandex.market.mbo.db.modelstorage.yt;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

/**
 * @author moskovkin@yandex-team.ru
 * @since 16.03.18
 */
@RunWith(MockitoJUnitRunner.class)
public class YtBatchQueryBuilderTest {

    private YtBatchQueryBuilder builder = new YtBatchQueryBuilder();

    @Test
    public void testFirstWhere() {
        String query = builder.withSelect("a, b, c, d")
            .withLongKeyColumns(Arrays.asList("a", "b", "c"))
            .withBatchSize(100)
            .withTable("//test/table")
            .withWhere("d = 0")
            .buildQuery();
        Assert.assertEquals("a, b, c, d " +
                "FROM [//test/table] " +
                "WHERE (1=1) " +
                "AND (d = 0) " +
                "ORDER BY a, b, c LIMIT 100",
            query);
    }

    @Test
    public void testFirstNoWhere() {
        String query = builder.withSelect("a, b, c, d")
            .withLongKeyColumns(Arrays.asList("a", "b", "c"))
            .withBatchSize(100)
            .withTable("//test/table")
            .buildQuery();
        Assert.assertEquals("a, b, c, d " +
                "FROM [//test/table] " +
                "WHERE (1=1) " +
                "ORDER BY a, b, c LIMIT 100",
            query);
    }

    @Test
    public void testNextWhere() {
        String query = builder.withSelect("a, b, c, d")
            .withLongKeyColumns(Arrays.asList("a", "b", "c"))
            .withFromKeyValues(Arrays.asList("A", "B", "C"))
            .withBatchSize(100)
            .withTable("//test/table")
            .withWhere("d = 0")
            .buildQuery();
        Assert.assertEquals("a, b, c, d FROM [//test/table] " +
                "WHERE (1=1) " +
                "AND ((a > A) " +
                "OR (a = A AND b > B) " +
                "OR (a = A AND b = B AND c > C)) " +
                "AND (d = 0) " +
                "ORDER BY a, b, c LIMIT 100",
            query
        );
    }

    @Test
    public void testNextNoWhere() {
        String query = builder.withSelect("a, b, c, d")
            .withLongKeyColumns(Arrays.asList("a", "b", "c"))
            .withFromKeyValues(Arrays.asList("A", "B", "C"))
            .withBatchSize(100)
            .withTable("//test/table")
            .buildQuery();
        Assert.assertEquals("a, b, c, d FROM [//test/table] " +
                "WHERE (1=1) " +
                "AND ((a > A) " +
                "OR (a = A AND b > B) " +
                "OR (a = A AND b = B AND c > C)) " +
                "ORDER BY a, b, c LIMIT 100",
            query
        );
    }
}

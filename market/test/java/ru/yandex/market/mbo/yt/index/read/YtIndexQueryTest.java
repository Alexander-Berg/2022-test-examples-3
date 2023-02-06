package ru.yandex.market.mbo.yt.index.read;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;

import ru.yandex.market.mbo.yt.index.read.data.TestFilter;
import ru.yandex.market.mbo.yt.index.read.data.TestIndexQuery;

/**
 * @author apluhin
 * @created 7/9/21
 */
public class YtIndexQueryTest extends TestCase {

    public void testConvertQuery() {
        TestFilter testFilter = new TestFilter();
        testFilter.searchByIds(Collections.singletonList(new TestFilter.TestModel(1L)));
        testFilter.searchFrom(100L);
        testFilter.searchTo(500L);
        testFilter.setOffset(1000L);
        testFilter.setLimit(50L);

        Assert.assertTrue(TestIndexQuery.isSupportFilter(testFilter));

        TestIndexQuery testIndex = new TestIndexQuery(testFilter);
        String query = testIndex.query(true);

        Assert.assertEquals(
                "offer_id in (1) AND timestamp > 100 AND timestamp < 500 OFFSET 1000 LIMIT 50",
                query
        );
    }

    public void testCompositeQuery() {
        TestFilter testFilter = new TestFilter();
        testFilter.searchCompositeIn(Collections.singletonList(Pair.of(new TestFilter.TestModel(1L), 500L)));

        TestIndexQuery testIndex = new TestIndexQuery(testFilter);
        String query = testIndex.query(true);

        Assert.assertEquals(
                "(offer_id,timestamp) in ((1,500))",
                query
        );
    }

    public void testQuotedQuery() {
        TestFilter testFilter = new TestFilter();
        testFilter.searchByBusinessId(Arrays.asList("test1", "test2"));

        TestIndexQuery testIndex = new TestIndexQuery(testFilter);
        String query = testIndex.query(true);

        Assert.assertEquals(
                "business_id in ('test1','test2')",
                query
        );
    }
}

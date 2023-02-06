package ru.yandex.market.yt.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.market.yt.client.YTProxy;
import ru.yandex.market.yt.client.YtClientProxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YTFetchTaskTest {

    private static final YTBinder<SampleArg> BINDER = YTBinder.getBinder(SampleArg.class);
    private static final YTBinder<UniqueKey> UNIQUE_KEY_BINDER = YTBinder.getBinder(UniqueKey.class);

    @Mock(lenient = true)
    private YtClientProxy ytClient;

    @Captor
    private ArgumentCaptor<String> queries;

    @Captor
    private ArgumentCaptor<String> keyQueries;

    private List<SampleArg> rows;

    @BeforeEach
    void init() {
        rows = List.of(new SampleArg(1, "4"), new SampleArg(2, "5"), new SampleArg(3, "6"));
        buildAnswer(ytClient, queries, BINDER, rows);
    }


    @Test
    void testSimpleFetch() {
        var fetch = new YTSimpleFetchTask<>(1, "* from [table] where 1 = 2 group by x order by y",
                ytClient, BINDER, List.of(new KeyField<>("a", SampleArg::getA)));
        var allQueries = testFetch(fetch);
        compare(List.of(
                "* from [table] where 1 = 2 group by x order by y limit 1",
                "* from [table] where 1 = 2 and a > 1 group by x order by y limit 1",
                "* from [table] where 1 = 2 and a > 2 group by x order by y limit 1",
                "* from [table] where 1 = 2 and a > 3 group by x order by y limit 1"
        ), allQueries);
    }

    @Test
    void testSimpleFetch1() {
        var fetch = new YTSimpleFetchTask<>(1, "* from [table] where 1 = 2 order by y",
                ytClient, BINDER, List.of(new KeyField<>("a", SampleArg::getA)));
        var allQueries = testFetch(fetch);
        compare(List.of(
                "* from [table] where 1 = 2 order by y limit 1",
                "* from [table] where 1 = 2 and a > 1 order by y limit 1",
                "* from [table] where 1 = 2 and a > 2 order by y limit 1",
                "* from [table] where 1 = 2 and a > 3 order by y limit 1"
        ), allQueries);
    }

    @Test
    void testSimpleFetch2() {
        var fetch = new YTSimpleFetchTask<>(1, "* from [table] order by y", ytClient, BINDER,
                List.of(new KeyField<>("a", SampleArg::getA)));
        var allQueries = testFetch(fetch);
        compare(List.of(
                "* from [table] order by y limit 1",
                "* from [table] where a > 1 order by y limit 1",
                "* from [table] where a > 2 order by y limit 1",
                "* from [table] where a > 3 order by y limit 1"
        ), allQueries);
    }

    @Test
    void testSimpleFetch3() {
        var fetch = new YTSimpleFetchTask<>(1, "* from [table] group by x order by y", ytClient, BINDER,
                List.of(new KeyField<>("a", SampleArg::getA)));
        var allQueries = testFetch(fetch);
        compare(List.of(
                "* from [table] group by x order by y limit 1",
                "* from [table] where a > 1 group by x order by y limit 1",
                "* from [table] where a > 2 group by x order by y limit 1",
                "* from [table] where a > 3 group by x order by y limit 1"
        ), allQueries);
    }

    @Test
    void testSimpleFetchCondition() {
        var fetch = new YTSimpleFetchTask<>(1, "* from [table] where 1 = 2 group by x order by y",
                ytClient, BINDER, List.of(new KeyField<>("a", SampleArg::getA, ">=")));
        var allQueries = testFetch(fetch);
        compare(List.of(
                "* from [table] where 1 = 2 group by x order by y limit 1",
                "* from [table] where 1 = 2 and a >= 1 group by x order by y limit 1",
                "* from [table] where 1 = 2 and a >= 2 group by x order by y limit 1",
                "* from [table] where 1 = 2 and a >= 3 group by x order by y limit 1"
        ), allQueries);
    }

    @Test
    void testSimpleFetchEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                new YTSimpleFetchTask<>(1, "* from [table] where 1 = 2 group by x order by y",
                        ytClient, BINDER, List.of()));
    }

    @Test
    void testSimpleFetchSameKeys() {
        assertThrows(IllegalArgumentException.class, () ->
                new YTSimpleFetchTask<>(1, "* from [table] where 1 = 2 group by x order by y",
                        ytClient, BINDER,
                        List.of(new KeyField<>("a", SampleArg::getA),
                                new KeyField<>("a", SampleArg::getA))));
    }

    @Test
    void testSimpleFetchDifferentConditions() {
        assertThrows(IllegalArgumentException.class, () ->
                new YTSimpleFetchTask<>(1, "* from [table] where 1 = 2 group by x order by y",
                        ytClient, BINDER,
                        List.of(new KeyField<>("a", SampleArg::getA),
                                new KeyField<>("b", SampleArg::getA, ">="))));
    }

    @Test
    void testSimpleFetchTuple() {
        var fetch = new YTSimpleFetchTask<>(1, "* from [table] group by x order by y",
                ytClient, BINDER,
                List.of(new KeyField<>("a", SampleArg::getA),
                        new KeyField<>("b", SampleArg::getB)));
        var allQueries = testFetch(fetch);
        compare(List.of(
                "* from [table] group by x order by y limit 1",
                "* from [table] where (a,b) > (1,\"4\") group by x order by y limit 1",
                "* from [table] where (a,b) > (2,\"5\") group by x order by y limit 1",
                "* from [table] where (a,b) > (3,\"6\") group by x order by y limit 1"
        ), allQueries);
    }

    @Test
    void testOffsetFetchTask() {
        var fetch = new YTOffsetFetchTask<>(1, "* from [table] where 1 = 2 group by x order by y",
                ytClient, BINDER);
        var allQueries = testFetch(fetch);
        compare(List.of(
                "* from [table] where 1 = 2 group by x order by y limit 1",
                "* from [table] where 1 = 2 group by x order by y offset 1 limit 1",
                "* from [table] where 1 = 2 group by x order by y offset 2 limit 1",
                "* from [table] where 1 = 2 group by x order by y offset 3 limit 1"
        ), allQueries);
    }

    @Test
    void testOffsetFetchTask1() {
        var fetch = new YTOffsetFetchTask<>(1, "* from [table] where 1 = 2 order by y", ytClient, BINDER);
        var allQueries = testFetch(fetch);
        compare(List.of(
                "* from [table] where 1 = 2 order by y limit 1",
                "* from [table] where 1 = 2 order by y offset 1 limit 1",
                "* from [table] where 1 = 2 order by y offset 2 limit 1",
                "* from [table] where 1 = 2 order by y offset 3 limit 1"
        ), allQueries);
    }


    @Test
    void testOffsetFetchTask2() {
        var fetch = new YTOffsetFetchTask<>(1, "* from [table] order by y", ytClient, BINDER);
        var allQueries = testFetch(fetch);
        compare(List.of(
                "* from [table] order by y limit 1",
                "* from [table] order by y offset 1 limit 1",
                "* from [table] order by y offset 2 limit 1",
                "* from [table] order by y offset 3 limit 1"
        ), allQueries);
    }


    @Test
    void testOffsetFetchTask3() {
        var fetch = new YTOffsetFetchTask<>(1, "* from [table] group by x order by y", ytClient, BINDER);
        var allQueries = testFetch(fetch);
        compare(List.of(
                "* from [table] group by x order by y limit 1",
                "* from [table] group by x order by y offset 1 limit 1",
                "* from [table] group by x order by y offset 2 limit 1",
                "* from [table] group by x order by y offset 3 limit 1"
        ), allQueries);
    }


    @Test
    void testOptimizedFetchTaskIN() {
        var keyRows = List.of(new UniqueKey(1), new UniqueKey(2), new UniqueKey(3));
        buildAnswer(ytClient, keyQueries, UNIQUE_KEY_BINDER, keyRows);

        var fetch = new YTOptimizedFetchTask<>(1,
                "* from [table] where 1 = 2 group by x order by y",
                "* from [table] where 1 = 2 order by z",
                ytClient, ytClient, BINDER,
                new KeyField<>("a", SampleArg::getA), "b",
                ResolvedFetchType.IN);

        var allQueries = testFetch(fetch);

        compare(List.of(
                "b as key from [table] where 1 = 2 group by x order by y limit 1",
                "b as key from [table] where 1 = 2 and a > 1 group by x order by y limit 1",
                "b as key from [table] where 1 = 2 and a > 2 group by x order by y limit 1",
                "b as key from [table] where 1 = 2 and a > 3 group by x order by y limit 1"
        ), captureQueries(keyQueries));

        compare(List.of(
                "* from [table] where 1 = 2 and b in (1) order by z limit 1",
                "* from [table] where 1 = 2 and b in (2) order by z limit 1",
                "* from [table] where 1 = 2 and b in (3) order by z limit 1"
        ), allQueries);
    }

    @Test
    void testOptimizedFetchTaskRANGED() {
        var keyRows = List.of(new UniqueKey(1), new UniqueKey(2), new UniqueKey(3));
        buildAnswer(ytClient, keyQueries, UNIQUE_KEY_BINDER, keyRows);

        var fetch = new YTOptimizedFetchTask<>(1,
                "* from [table] where 1 = 2 group by x order by y",
                "* from [table] where 1 = 2 order by z",
                ytClient, ytClient, BINDER,
                new KeyField<>("a", SampleArg::getA), "b",
                ResolvedFetchType.RANGED);

        var allQueries = testFetch(fetch);

        compare(List.of(
                "b as key from [table] where 1 = 2 group by x order by y limit 1",
                "b as key from [table] where 1 = 2 and a > 1 group by x order by y limit 1",
                "b as key from [table] where 1 = 2 and a > 2 group by x order by y limit 1",
                "b as key from [table] where 1 = 2 and a > 3 group by x order by y limit 1"
        ), captureQueries(keyQueries));

        compare(List.of(
                "* from [table] where 1 = 2 and b between (1 and 1) order by z limit 1",
                "* from [table] where 1 = 2 and b between (2 and 2) order by z limit 1",
                "* from [table] where 1 = 2 and b between (3 and 3) order by z limit 1"
        ), allQueries);
    }

    private List<String> testFetch(YTFetchTask<SampleArg> fetch) {
        List<SampleArg> target = new ArrayList<>();
        fetch.fetchInto(target::add);

        assertEquals(rows, target);

        return captureQueries(queries);
    }

    private static List<String> captureQueries(ArgumentCaptor<String> captor) {
        return captor.getAllValues().stream()
                .map(YTFetchTaskTest::normalize)
                .collect(Collectors.toList());
    }

    private static <T> void buildAnswer(YTProxy yt, ArgumentCaptor<String> captor, YTBinder<T> binder, List<T> rows) {
        when(yt.selectRows(captor.capture(), same(binder))).then(new Answer<List<T>>() {
            private int i = 0;

            @Override
            public List<T> answer(InvocationOnMock invocation) {
                if (i < rows.size()) {
                    return List.of(rows.get(i++));
                } else {
                    return List.of();
                }
            }
        });
    }

    static String normalize(String text) {
        while (text.contains("  ")) { // Только для тестов
            text = text.replace("  ", " ");
        }
        return text;
    }

    static void compare(List<?> expect, List<?> actual) {
        assertEquals(expect, actual);
    }

    @YTreeObject
    static class SampleArg {
        private int a;
        private String b;

        SampleArg() {
            //
        }

        SampleArg(int a, String b) {
            this.a = a;
            this.b = b;
        }

        public int getA() {
            return a;
        }

        public String getB() {
            return b;
        }
    }
}

package ru.yandex.market.archive.etl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.Multiset;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.archive.JobConfiguration;
import ru.yandex.market.archive.quality.QualityCheckerSupport;
import ru.yandex.market.archive.quality.VerificationKey;
import ru.yandex.market.archive.schema.SourceColumn;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author snoop
 */
public class SampleTest {

    private static final String KEY1 = "id1";
    private static final String KEY2 = "id2";
    private static final String KEY3 = "id3";
    private static final String TS = "ts";

    private JobConfiguration configuration;
    private Sample sample;

    private Map<String, SourceColumn> columns = new HashMap<>();

    private static Map<String, Object> row(Pair<String, Object>... pairs) {
        return Arrays.stream(pairs).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    private static Map<String, Object> add(Map<String, Object> row, Pair<String, Object> pair) {
        row.put(pair.getKey(), pair.getValue());
        return row;
    }

    private static List<Map<String, Object>> generate(String column, int count, Pair<String, Object>... keyColumnList) {
        return IntStream.range(0, count).boxed().
                map(value -> add(row(keyColumnList), Pair.of(column, value))).collect(Collectors.toList());
    }

    private static void repeat(Sample sample, Map<String, Object> row, int count) {
        while (count-- > 0) {
            sample.accept(row);
        }
    }

    @BeforeEach
    public void setUp() {
        configuration = mock(JobConfiguration.class);
        QualityCheckerSupport checker = new QualityCheckerSupport(20, DateUtil.ISO_FORMAT_PATTERN);
        sample = new NonUniqueKeySample(configuration, checker, 5);

        columns.put(KEY1, getColumn(KEY1));
        columns.put(KEY2, getColumn(KEY2));
        columns.put(KEY3, getColumn(KEY3));
        columns.put(TS, getColumn(TS));
    }

    @Test
    public void digests_split_column_only() {
        defineKey(KEY1);

        //get missed due to limit on sample's set
        generate(TS, 10, Pair.of(KEY1, "123")).forEach(sample);

        repeat(sample, row(Pair.of(KEY1, "124"), Pair.of(TS, 12345)), 4);
        sample.accept(row(Pair.of(KEY1, "124"), Pair.of(TS, 52345)));

        //effectively skip other rows because of sample completeness
        repeat(sample, row(Pair.of(KEY1, "125"), Pair.of(TS, 12345)), 2);

        final Map<VerificationKey, Multiset<String>> digests = sample.digests();
        Assertions.assertEquals(1, digests.size(), digests.toString());
        final VerificationKey actualKey = digests.entrySet().iterator().next().getKey();
        Assertions.assertEquals(1, actualKey.length(), digests.toString());
        final Pair<SourceColumn, String> partial = actualKey.getPartial(configuration, 0);
        Assertions.assertEquals(KEY1, partial.getKey().getName(),  partial.toString());
        Assertions.assertEquals( "124", partial.getValue(), partial.toString());
        final Multiset<String> strings = digests.get(actualKey);
        Assertions.assertEquals(5, strings.size(), strings.toString());
        Assertions.assertEquals(2, strings.elementSet().size(), strings.elementSet().toString());
    }

    @Test
    public void digests_composite_key() {
        defineKey(KEY1, KEY2, KEY3);

        //get missed due to limit on sample's set
        generate(TS, 10,
                Pair.of(KEY1, "123"),
                Pair.of(KEY2, 123),
                Pair.of(KEY3, LocalDateTime.now()))
                .forEach(sample);

        final LocalDateTime ts = LocalDateTime.now();
        repeat(sample, row(Pair.of(KEY1, "123"), Pair.of(KEY2, 124), Pair.of(KEY3, ts),
                Pair.of(TS, 12345)), 4);
        sample.accept(row(Pair.of(KEY1, "123"), Pair.of(KEY2, 124), Pair.of(KEY3, ts),
                Pair.of(TS, 52345)));

        //effectively skip other rows because of sample completeness
        repeat(sample, row(Pair.of(KEY1, "123"), Pair.of(KEY2, 124), Pair.of(KEY3, ts.plusHours(1)),
                Pair.of(TS, 12345)), 2);

        final Map<VerificationKey, Multiset<String>> digests = sample.digests();
        Assertions.assertEquals(1, digests.size(), digests.toString());

        final VerificationKey actualKey = digests.entrySet().iterator().next().getKey();
        Assertions.assertEquals(3, actualKey.length(),digests.toString());
        Pair<SourceColumn, String> partial = actualKey.getPartial(configuration, 0);
        Assertions.assertEquals(KEY1, partial.getKey().getName(), partial.toString());
        Assertions.assertEquals("123", partial.getValue(), partial.toString());

        final Multiset<String> strings = digests.get(actualKey);
        Assertions.assertEquals(5, strings.size(), strings.toString());
        Assertions.assertEquals(2, strings.elementSet().size(), strings.elementSet().toString());

        partial = actualKey.getPartial(configuration, 1);
        Assertions.assertEquals(KEY2, partial.getKey().getName(), partial.toString());
        Assertions.assertEquals("124", partial.getValue(), partial.toString());

        partial = actualKey.getPartial(configuration, 2);
        Assertions.assertEquals(KEY3, partial.getKey().getName(), partial.toString());
        Assertions.assertEquals(ts.format(DateTimeFormatter.ofPattern(DateUtil.ISO_FORMAT_PATTERN)),
                partial.getValue(), partial.toString());
    }

    private SourceColumn getColumn(String name) {
        SourceColumn column = mock(SourceColumn.class);
        when(column.getName()).thenReturn(name);
        return column;
    }

    private void defineKey(String... columns) {
        List<SourceColumn> key = Arrays.stream(columns).map(this::column).collect(Collectors.toList());
        when(configuration.getVerificationColumns()).thenReturn(key);
    }

    private SourceColumn column(String name) {
        return columns.get(name);
    }
}

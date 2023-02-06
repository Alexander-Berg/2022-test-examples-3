package ru.yandex.direct.useractionlog.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Streams;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.test.utils.CustomAssumptionException;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class SplittingIteratorWindowTest {
    private static SplittingIteratorWindow<TestRecord, String, Offset> plainMapperTarget;
    private static SplittingIteratorWindow<TestRecord, String, Offset> joining2MapperTarget;
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private static SplittingIteratorWindow.Result<String, Offset> processPlain(List<TestRecord> records, int limit,
                                                                               @Nullable Offset offset) {
        return plainMapperTarget.process(skipOffset(records, offset), limit, offset);
    }

    private static SplittingIteratorWindow.Result<String, Offset> processJoining2(List<TestRecord> records, int limit,
                                                                                  @Nullable Offset offset) {
        return joining2MapperTarget.process(skipOffset(records, offset), limit, offset);
    }

    private static Function<PeekingIterator<TestRecord>, List<String>> joiningMapper(int joinRecords) {
        return iterator -> {
            List<String> result = new ArrayList<>();
            for (int i = 0; i < joinRecords && iterator.hasNext(); ++i) {
                result.addAll(Arrays.asList(iterator.next().values));
            }
            return result;
        };
    }

    private static PeekingIterator<TestRecord> skipOffset(List<TestRecord> records, @Nullable Offset offset) {
        PeekingIterator<TestRecord> result = Iterators.peekingIterator(records.iterator());
        if (offset != null) {
            while (result.hasNext()) {
                TestRecord record = result.peek();
                if (Offset.fromRecord(record, offset.recordOffset).equals(offset)) {
                    break;
                }
                result.next();
            }
        }
        return result;
    }

    @BeforeClass
    public static void checkTestUtilFunctions() {
        try {
            SoftAssertions softly = new SoftAssertions();
            List<TestRecord> source = Arrays.asList(
                    new TestRecord(0, "0"),
                    new TestRecord(1, "1"),
                    new TestRecord(2, "2"));

            softly.assertThat(Streams.stream(skipOffset(source, null)).collect(Collectors.toList()))
                    .describedAs("skipOffset: No offset")
                    .usingRecursiveFieldByFieldElementComparator()
                    .isEqualTo(source);

            softly.assertThat(Streams.stream(skipOffset(source, new Offset(0, 0))).collect(Collectors.toList()))
                    .describedAs("skipOffset: Skip until record 0 to get the whole list")
                    .usingRecursiveFieldByFieldElementComparator()
                    .isEqualTo(source);

            softly.assertThat(Streams.stream(skipOffset(source, new Offset(1, 0))).collect(Collectors.toList()))
                    .describedAs("skipOffset: Skip until record 1")
                    .usingRecursiveFieldByFieldElementComparator()
                    .isEqualTo(source.subList(1, source.size()));

            softly.assertThat(Streams.stream(skipOffset(source, new Offset(1, 100))).collect(Collectors.toList()))
                    .describedAs("skipOffset: Skip until record 1, ignore in-record offset")
                    .usingRecursiveFieldByFieldElementComparator()
                    .isEqualTo(source.subList(1, source.size()));

            softly.assertThat(Streams.stream(skipOffset(source, new Offset(2, 0))).collect(Collectors.toList()))
                    .describedAs("skipOffset: Skip until record 2")
                    .usingRecursiveFieldByFieldElementComparator()
                    .isEqualTo(source.subList(2, source.size()));

            softly.assertThat(Streams.stream(skipOffset(source, new Offset(3, 0))).collect(Collectors.toList()))
                    .describedAs("skipOffset: Skip all records")
                    .usingRecursiveFieldByFieldElementComparator()
                    .isEqualTo(Collections.emptyList());

            softly.assertThat(Streams.stream(skipOffset(source, new Offset(100, 0))).collect(Collectors.toList()))
                    .describedAs("skipOffset: Skip all records with unknown event id")
                    .usingRecursiveFieldByFieldElementComparator()
                    .isEqualTo(Collections.emptyList());

            source = Arrays.asList(
                    new TestRecord(0, "0"),
                    new TestRecord(100, "100"),
                    new TestRecord(200, "200"));
            softly.assertThat(joiningMapper(1).apply(skipOffset(source, null)))
                    .describedAs("plainMapper: output only contents of first record")
                    .containsExactly("0");

            source = Arrays.asList(
                    new TestRecord(0, "0", "1", "2"),
                    new TestRecord(100, "100"),
                    new TestRecord(200, "200"));
            softly.assertThat(joiningMapper(1).apply(skipOffset(source, null)))
                    .describedAs("plainMapper: output only contents of first record and joins all values of it")
                    .containsExactly("0", "1", "2");

            source = Arrays.asList(
                    new TestRecord(0, "0", "1", "2"),
                    new TestRecord(100, "100"),
                    new TestRecord(200, "200", "201"),
                    new TestRecord(300, "300"));
            softly.assertThat(joiningMapper(3).apply(skipOffset(source, null)))
                    .describedAs(
                            "joiningMapper(3): output only contents of first 3 records and joins all values of them")
                    .containsExactly("0", "1", "2", "100", "200", "201");

            softly.assertAll();
        } catch (AssertionError e) {
            throw new CustomAssumptionException(e);
        }
    }

    @BeforeClass
    public static void makeTarget() {
        plainMapperTarget = new SplittingIteratorWindow<>(joiningMapper(1), Offset::fromRecord);
        joining2MapperTarget = new SplittingIteratorWindow<>(joiningMapper(2), Offset::fromRecord);
    }

    /**
     * Если количество результирующих объектов больше либо равно запрошенному лимиту, должн быть выданы все.
     * <p>
     * Версия для обработчика, который берёт из итератора один объект.
     *
     * @param addToLimit Сколько добавить к количеству результирующих элементов.
     */
    @Parameters({"0", "100"})
    @Test
    public void testProcessPlain(int addToLimit) {
        List<TestRecord> records = Arrays.asList(
                new TestRecord(0, "0"),
                new TestRecord(1, "1"),
                new TestRecord(2, "2"));
        SplittingIteratorWindow.Result<String, Offset> result =
                processPlain(records, records.size() + addToLimit, null);
        softly.assertThat(result.getObjects())
                .containsExactly("0", "1", "2");
        softly.assertThat(result.getNextPageOffset())
                .isNull();
    }

    /**
     * Если количество результирующих объектов меньше либо равно запрошенному лимиту, должны быть выданы все.
     * <p>
     * Версия для обработчика, который берёт из итератора два объекта.
     *
     * @param addToLimit Сколько добавить к количеству результирующих элементов.
     */
    @Parameters({"0", "100"})
    @Test
    public void testProcessJoining2(int addToLimit) {
        List<TestRecord> records = Arrays.asList(
                new TestRecord(0, "0"),
                new TestRecord(1, "1"),
                new TestRecord(2, "2"),
                new TestRecord(3, "3"),
                new TestRecord(4, "4"));
        SplittingIteratorWindow.Result<String, Offset> result =
                processPlain(records, records.size() + addToLimit, null);
        softly.assertThat(result.getObjects())
                .containsExactly("0", "1", "2", "3", "4");
        softly.assertThat(result.getNextPageOffset())
                .isNull();
    }

    /**
     * Если количество результирующих объектов больше запрошенного лимита, должно быть выдано не больше объектов,
     * чем указано в лимите. Также должен быть выдан специальный объект, который позволит продолжить чтение с того
     * места, где остановились.
     * <p>
     * Версия для обработчика, который берёт из итератора один объект.
     */
    @Test
    public void testPlainMakeOffset() {
        List<TestRecord> records = Arrays.asList(
                new TestRecord(0, "0"),
                new TestRecord(1, "1"),
                new TestRecord(2, "2"),
                new TestRecord(3, "3"));

        // Чтение с начала до определённого лимита. Чтение не до конца.
        SplittingIteratorWindow.Result<String, Offset> resultToFirstOffset =
                processPlain(records, 1, null);
        softly.assertThat(resultToFirstOffset.getObjects())
                .describedAs("Get from start to first offset: values")
                .containsExactly("0");
        softly.assertThat(resultToFirstOffset.getNextPageOffset())
                .describedAs("Get from start to first offset: next offset")
                .isEqualTo(new Offset(1, 0));

        // Чтение с того места, на котором остановились в первый раз. Чтение не до конца.
        SplittingIteratorWindow.Result<String, Offset> resultFromFirstToSecondOffset =
                processPlain(records, 2, new Offset(1, 0));
        softly.assertThat(resultFromFirstToSecondOffset.getObjects())
                .describedAs("Get from first offset to second offset: values")
                .containsExactly("1", "2");
        softly.assertThat(resultFromFirstToSecondOffset.getNextPageOffset())
                .describedAs("Get from first offset to second offset: next offset")
                .isEqualTo(new Offset(3, 0));

        // Чтение с того места, на котором остановились во второй раз. Чтение до конца.
        SplittingIteratorWindow.Result<String, Offset> resultFromSecondOffsetToEnd =
                processPlain(records, 1, new Offset(3, 0));
        softly.assertThat(resultFromSecondOffsetToEnd.getObjects())
                .describedAs("Get from second offset to end: values")
                .containsExactly("3");
        softly.assertThat(resultFromSecondOffsetToEnd.getNextPageOffset())
                .describedAs("Get from second offset to end: next offset")
                .isNull();
    }

    /**
     * Если количество результирующих объектов больше запрошенного лимита, должно быть выдано не больше объектов,
     * чем указано в лимите. Также должен быть выдан специальный объект, который позволит продолжить чтение с того
     * места, где остановились.
     * <p>
     * Версия для обработчика, который берёт из итератора два объекта.
     */
    @Test
    public void testJoining2MakeOffset() {
        List<TestRecord> records = Arrays.asList(
                new TestRecord(0, "0 (0)"),
                new TestRecord(1, "1 (1)"),
                new TestRecord(2, "2 (0)"),
                new TestRecord(3, "3 (1)"),
                new TestRecord(4, "4 (0)"),
                new TestRecord(5, "5 (1)"),
                new TestRecord(6, "6 (0)"));

        // Чтение с начала до определённого лимита. Чтение не до конца.
        SplittingIteratorWindow.Result<String, Offset> resultToFirstOffset =
                processJoining2(records, 2, null);
        softly.assertThat(resultToFirstOffset.getObjects())
                .describedAs("Get from start to first offset: values")
                .containsExactly("0 (0)", "1 (1)");
        softly.assertThat(resultToFirstOffset.getNextPageOffset())
                .describedAs("Get from start to first offset: next offset")
                .isEqualTo(new Offset(2, 0));

        // Чтение с того места, на котором остановились в первый раз. Чтение не до конца.
        SplittingIteratorWindow.Result<String, Offset> resultFromFirstToSecondOffset =
                processJoining2(records, 4, new Offset(2, 0));
        softly.assertThat(resultFromFirstToSecondOffset.getObjects())
                .describedAs("Get from first offset to second offset: values")
                .containsExactly("2 (0)", "3 (1)", "4 (0)", "5 (1)");
        softly.assertThat(resultFromFirstToSecondOffset.getNextPageOffset())
                .describedAs("Get from first offset to second offset: next offset")
                .isEqualTo(new Offset(6, 0));

        // Чтение с того места, на котором остановились во второй раз. Чтение до конца.
        SplittingIteratorWindow.Result<String, Offset> resultFromSecondOffsetToEnd =
                processJoining2(records, 1, new Offset(6, 0));
        softly.assertThat(resultFromSecondOffsetToEnd.getObjects())
                .describedAs("Get from second offset to end: values")
                .containsExactly("6 (0)");
        softly.assertThat(resultFromSecondOffsetToEnd.getNextPageOffset())
                .describedAs("Get from second offset to end: next offset")
                .isNull();
    }

    /**
     * Если количество результирующих объектов больше либо равно запрошенному лимиту, должн быть выданы все.
     * <p>
     * Версия для обработчика, который берёт из итератора один объект и возвращает несколько объектов (≥1).
     *
     * @param addToLimit Сколько добавить к количеству результирующих элементов.
     */
    @Parameters({"0", "100"})
    @Test
    public void processPlainManyEvents(int addToLimit) {
        List<TestRecord> records = Arrays.asList(
                new TestRecord(0, "0", "0.5"),
                new TestRecord(1, "1", "1.5", "1.7"),
                new TestRecord(2, "2", "2.5", "2.7", "2.9"));
        SplittingIteratorWindow.Result<String, Offset> result = processPlain(
                records,
                (int) records.stream().flatMap(r -> Stream.of(r.values)).count() + addToLimit,
                null);
        softly.assertThat(result.getObjects())
                .containsExactly("0", "0.5", "1", "1.5", "1.7", "2", "2.5", "2.7", "2.9");
        softly.assertThat(result.getNextPageOffset())
                .isNull();
    }

    /**
     * Если количество результирующих объектов больше либо равно запрошенному лимиту, должн быть выданы все.
     * <p>
     * Версия для обработчика, который берёт из итератора два объекта и возвращает несколько объектов (≥2).
     *
     * @param addToLimit Сколько добавить к количеству результирующих элементов.
     */
    @Parameters({"0", "100"})
    @Test
    public void processJoining2ManyEvents(int addToLimit) {
        List<TestRecord> records = Arrays.asList(
                new TestRecord(0, "0", "0.5"),
                new TestRecord(1, "1", "1.5", "1.7"),
                new TestRecord(2, "2", "2.5", "2.7", "2.9"));
        SplittingIteratorWindow.Result<String, Offset> result = processJoining2(
                records,
                (int) records.stream().flatMap(r -> Stream.of(r.values)).count() + addToLimit,
                null);
        softly.assertThat(result.getObjects())
                .containsExactly("0", "0.5", "1", "1.5", "1.7", "2", "2.5", "2.7", "2.9");
        softly.assertThat(result.getNextPageOffset())
                .isNull();
    }

    /**
     * Обработчик из одной записи может извлечь несколько результирующих объектов. Может получиться так, что лимит
     * отрежет половину объектов, которые были получены на последней итерации. Должна быть возможность продолжить чтение
     * с той самой половины. Не должен быть ни упущен, ни повторён ни один результирующий объект.
     * <p>
     * В тесте для небольшого набора данных перебираются все возможные лимиты и смещения со старта.
     * <p>
     * Версия для обработчика, который берёт из итератора один объект и возвращает несколько объектов.
     *
     * @param requestLimit           Сколько объектов прочесть
     * @param recordIndex            Параметр смещения со старта (идентификатор объекта)
     * @param inRecordOffset         Параметр смещения со старта (смещение в результате обработки объекта)
     * @param expectedRecordIndex    Параметр смещения, с которого можно было бы прочесть следующий пласт данных
     * @param expectedInRecordOffset Параметр смещения, с которого можно было бы прочесть следующий пласт данных
     */
    @Parameters({
            "1, 0,0, 0,1",
            "2, 0,0, 1,0",
            "3, 0,0, 1,1",
            "4, 0,0, 1,2",
            "5, 0,0, 2,0",
            "6, 0,0, 2,1",
            "7, 0,0, 2,2",
            "8, 0,0, 2,3",
            "9, 0,0, -1,-1",

            "1, 0,1, 1,0",
            "2, 0,1, 1,1",
            "3, 0,1, 1,2",
            "4, 0,1, 2,0",
            "5, 0,1, 2,1",
            "6, 0,1, 2,2",
            "7, 0,1, 2,3",
            "8, 0,1, -1,-1",

            "1, 1,0, 1,1",
            "2, 1,0, 1,2",
            "3, 1,0, 2,0",
            "4, 1,0, 2,1",
            "5, 1,0, 2,2",
            "6, 1,0, 2,3",
            "7, 1,0, -1,-1",

            "1, 1,1, 1,2",
            "2, 1,1, 2,0",
            "3, 1,1, 2,1",
            "4, 1,1, 2,2",
            "5, 1,1, 2,3",
            "6, 1,1, -1,-1",

            "1, 1,2, 2,0",
            "2, 1,2, 2,1",
            "3, 1,2, 2,2",
            "4, 1,2, 2,3",
            "5, 1,2, -1,-1",

            "1, 2,0, 2,1",
            "2, 2,0, 2,2",
            "3, 2,0, 2,3",
            "4, 2,0, -1,-1",

            "1, 2,1, 2,2",
            "2, 2,1, 2,3",
            "3, 2,1, -1,-1",

            "1, 2,2, 2,3",
            "2, 2,2, -1,-1",

            "1, 2,3, -1,-1",
    })
    @Test
    public void processPlainManyEventsMakeOffsetFromOffset(int requestLimit, int recordIndex, int inRecordOffset,
                                                           int expectedRecordIndex, int expectedInRecordOffset) {
        List<TestRecord> records = Arrays.asList(
                new TestRecord(0, "0", "0.5"),
                new TestRecord(1, "1", "1.5", "1.7"),
                new TestRecord(2, "2", "2.5", "2.7", "2.9"));
        List<String> totalResult = records.stream().flatMap(r -> Stream.of(r.values)).collect(Collectors.toList());

        int expectedTotalStart = inRecordOffset;
        for (TestRecord record : records) {
            if (record.index == recordIndex) {
                break;
            }
            expectedTotalStart += record.values.length;
        }

        SplittingIteratorWindow.Result<String, Offset> result =
                processPlain(records, requestLimit, new Offset(recordIndex, inRecordOffset));
        softly.assertThat(result.getObjects())
                .containsExactly(totalResult
                        .subList(expectedTotalStart, expectedTotalStart + requestLimit)
                        .toArray(new String[0]));
        if (expectedRecordIndex == -1) {
            softly.assertThat(result.getNextPageOffset())
                    .isNull();
        } else {
            softly.assertThat(result.getNextPageOffset())
                    .isEqualTo(new Offset(expectedRecordIndex, expectedInRecordOffset));
        }
    }

    /**
     * Обработчик из одной записи может извлечь несколько результирующих объектов. Может получиться так, что лимит
     * отрежет половину объектов, которые были получены на последней итерации. Должна быть возможность продолжить чтение
     * с той самой половины. Не должен быть ни упущен, ни повторён ни один результирующий объект.
     * <p>
     * В тесте для небольшого набора данных перебираются все возможные лимиты и смещения со старта.
     * <p>
     * Версия для обработчика, который берёт из итератора два объекта и возвращает несколько объектов (≥2).
     *
     * @param requestLimit           Сколько объектов прочесть
     * @param recordIndex            Параметр смещения со старта (идентификатор объекта)
     * @param inRecordOffset         Параметр смещения со старта (смещение в результате обработки объекта)
     * @param expectedRecordIndex    Параметр смещения, с которого можно было бы прочесть следующий пласт данных
     * @param expectedInRecordOffset Параметр смещения, с которого можно было бы прочесть следующий пласт данных
     */
    @Parameters({
            "1, 0,0, 0,1",
            "2, 0,0, 0,2",
            "3, 0,0, 0,3",
            "4, 0,0, 0,4",
            "5, 0,0, 2,0",
            "6, 0,0, 2,1",
            "7, 0,0, 2,2",
            "8, 0,0, 2,3",
            "9, 0,0, -1,-1",

            "1, 0,1, 0,2",
            "2, 0,1, 0,3",
            "3, 0,1, 0,4",
            "4, 0,1, 2,0",
            "5, 0,1, 2,1",
            "6, 0,1, 2,2",
            "7, 0,1, 2,3",
            "8, 0,1, -1,-1",

            "1, 0,2, 0,3",
            "2, 0,2, 0,4",
            "3, 0,2, 2,0",
            "4, 0,2, 2,1",
            "5, 0,2, 2,2",
            "6, 0,2, 2,3",
            "7, 0,2, -1,-1",

            "1, 0,3, 0,4",
            "2, 0,3, 2,0",
            "3, 0,3, 2,1",
            "4, 0,3, 2,2",
            "5, 0,3, 2,3",
            "6, 0,3, -1,-1",

            "1, 0,4, 2,0",
            "2, 0,4, 2,1",
            "3, 0,4, 2,2",
            "4, 0,4, 2,3",
            "5, 0,4, -1,-1",

            "1, 2,0, 2,1",
            "2, 2,0, 2,2",
            "3, 2,0, 2,3",
            "4, 2,0, -1,-1",

            "1, 2,1, 2,2",
            "2, 2,1, 2,3",
            "3, 2,1, -1,-1",

            "1, 2,2, 2,3",
            "2, 2,2, -1,-1",

            "1, 2,3, -1,-1",
    })
    @Test
    public void processJoining2ManyEventsMakeOffsetFromOffset(int requestLimit, int recordIndex, int inRecordOffset,
                                                              int expectedRecordIndex, int expectedInRecordOffset) {
        List<TestRecord> records = Arrays.asList(
                new TestRecord(0, "0", "0.5"),
                new TestRecord(1, "1", "1.5", "1.7"),
                new TestRecord(2, "2", "2.5", "2.7", "2.9"));
        List<String> totalResult = records.stream().flatMap(r -> Stream.of(r.values)).collect(Collectors.toList());

        int expectedTotalStart = inRecordOffset;
        for (TestRecord record : records) {
            if (record.index == recordIndex) {
                break;
            }
            expectedTotalStart += record.values.length;
        }

        SplittingIteratorWindow.Result<String, Offset> result =
                processJoining2(records, requestLimit, new Offset(recordIndex, inRecordOffset));
        softly.assertThat(result.getObjects())
                .containsExactly(totalResult
                        .subList(expectedTotalStart, expectedTotalStart + requestLimit)
                        .toArray(new String[0]));
        if (expectedRecordIndex == -1) {
            softly.assertThat(result.getNextPageOffset())
                    .isNull();
        } else {
            softly.assertThat(result.getNextPageOffset())
                    .isEqualTo(new Offset(expectedRecordIndex, expectedInRecordOffset));
        }
    }

    private static class TestRecord {
        final int index;
        final String[] values;

        TestRecord(int index, String... values) {
            this.index = index;
            this.values = values;
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(index);
            result = 31 * result + Arrays.hashCode(values);
            return result;
        }

        @Override
        public String toString() {
            return "TestRecord{" +
                    "index=" + index +
                    ", values=" + Arrays.toString(values) +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TestRecord that = (TestRecord) o;
            return index == that.index &&
                    Arrays.equals(values, that.values);
        }
    }

    private static class Offset implements SplittingIteratorWindow.Offset {
        final int fromEventId;
        final int recordOffset;

        Offset(int fromEventId, int recordOffset) {
            this.fromEventId = fromEventId;
            this.recordOffset = recordOffset;
        }

        static Offset fromRecord(TestRecord record, int recordOffset) {
            return new Offset(record.index, recordOffset);
        }

        @Override
        public int inRecordOffset() {
            return recordOffset;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fromEventId, recordOffset);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Offset offset = (Offset) o;
            return fromEventId == offset.fromEventId &&
                    recordOffset == offset.recordOffset;
        }

        @Override
        public String toString() {
            return "Offset{" +
                    "fromEventId=" + fromEventId +
                    ", recordOffset=" + recordOffset +
                    '}';
        }
    }
}

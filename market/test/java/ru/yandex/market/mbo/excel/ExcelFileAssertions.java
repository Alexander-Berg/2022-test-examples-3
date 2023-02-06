package ru.yandex.market.mbo.excel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.StringAssert;
import org.assertj.core.internal.Iterables;

import ru.yandex.market.mbo.excel.formatting.ConditionFormattingsWithRange;

/**
 * @author s-ermakov
 */
public class ExcelFileAssertions extends AbstractAssert<ExcelFileAssertions, ExcelFile> {
    protected Iterables iterables = Iterables.instance();

    public ExcelFileAssertions(ExcelFile actual) {
        super(actual, ExcelFileAssertions.class);
    }

    public static ExcelFileAssertions assertThat(byte[] actual) {
        return assertThat(new ByteArrayInputStream(actual));
    }

    public static ExcelFileAssertions assertThat(InputStream actual) {
        try {
            ExcelFile file = ExcelFileConverter.convert(actual, ExcelIgnoresConfigImpl.empty());
            return assertThat(file);
        } catch (ExcelFileConverterException e) {
            throw new AssertionError("Expected valid excel file, but got error during parsing", e);
        }
    }

    public static ExcelFileAssertions assertThat(ExcelFile actual) {
        return new ExcelFileAssertions(actual);
    }

    private static boolean isEmptyValue(Object valueRaw) {
        if (valueRaw instanceof CharSequence) {
            return StringUtils.isEmpty((CharSequence) valueRaw);
        } else {
            return valueRaw == null;
        }
    }

    private static String escapeTextJoin(Collection<?> texts) {
        return texts.stream().map(ExcelFileAssertions::escapeText).collect(Collectors.joining("\n"));
    }

    private static String escapeText(Object text) {
        return text.toString().replace("\n", "\\n");
    }

    private static String lineStr(int line, ExcelFile actual) {
        return ExcelFile.Builder.withHeaders(actual.getHeaders())
                .addLine(actual.getValuesList(line))
                .toString();
    }

    private String headerOrColumn(int column) {
        String header = actual.getHeader(column);
        return header == null ? (column + "") : header;
    }

    public ExcelFileAssertions isEmpty() {
        isNotNull();

        info.description("File:\n%s\nExpected file to be empty but has <%s> rows.",
                actual, actual.getLastLine());
        objects.assertEqual(info, actual.getLastLine(), 0);
        return this;
    }

    public ExcelFileAssertions hasSize(int size) {
        return hasLastLine(size);
    }

    public ExcelFileAssertions hasLastLine(int linesNumber) {
        isNotNull();

        info.description("File:\n%s\nExpected lastLine to be <%s> but was <%s>. File\n%s",
                actual, linesNumber, actual.getLastLine());
        objects.assertEqual(info, actual.getLastLine(), linesNumber);
        return this;
    }

    public ExcelFileAssertions hasHeaderSize(int expectedSize) {
        isNotNull();

        info.description("File\n%s\nExpected headers size to be <%d>, but was <%d>",
                actual, expectedSize, actual.getHeadersSize());
        objects.assertEqual(info, actual.getHeadersSize(), expectedSize);
        return this;
    }

    public ExcelFileAssertions containsHeader(String headerName) {
        isNotNull();

        info.description("File\n%s\nExpected to contain column '%s' but actual columns are:\n%s.",
                actual, escapeText(headerName), escapeTextJoin(actual.getHeaders()));
        iterables.assertContains(info, actual.getHeaders(), new Object[]{headerName});
        return this;
    }

    public ExcelFileAssertions containsHeaders(String... headers) {
        return containsHeaders(Arrays.asList(headers));
    }

    public ExcelFileAssertions containsHeaders(Collection<String> headers) {
        isNotNull();

        info.description("File\n%s\nExpected to contain headers\n%s\nbut actual columns are:\n%s",
                actual, escapeTextJoin(headers), escapeTextJoin(actual.getHeaders()));
        iterables.assertContains(info, actual.getHeaders(), headers.toArray());

        return this;
    }

    public ExcelFileAssertions containsHeadersExactly(String... headers) {
        return containsHeadersExactly(Arrays.asList(headers));
    }

    public ExcelFileAssertions containsHeadersExactly(Collection<String> headers) {
        isNotNull();

        info.description("File:\n%s\nExpected to contain headers exactly in the same order \n%s\nbut actual columns " +
                "are:\n%s.", actual, escapeTextJoin(headers), escapeTextJoin(actual.getHeaders()));
        iterables.assertContainsExactly(info, actual.getHeaders(), headers.toArray());
        return this;
    }

    /**
     * Проверяет, что не существует ни одного заголовка, в котором содержится одна из последовательность из строк.
     */
    public ExcelFileAssertions doesntContainHeadersContaining(CharSequence strSubsequence) {
        isNotNull();

        List<String> headersWithSubsequence = actual.getHeaders().stream()
                .filter(header -> header.contains(strSubsequence))
                .collect(Collectors.toList());
        if (!headersWithSubsequence.isEmpty()) {
            failWithMessage(
                    "File:\n%s\nExpected not to contain subsequence '%s' in headers:\n%s\nbut some headers " +
                            "contain:\n%s",
                    actual, strSubsequence, escapeTextJoin(actual.getHeaders()),
                    escapeTextJoin(headersWithSubsequence));
        }
        return this;
    }

    public ExcelFileAssertions containsValuesExactly(int row, Collection<?> values) {
        isNotNull();

        List<String> actualValues = actual.getValuesList(row);
        List<?> expectedValues = new ArrayList<>(values);
        for (int column = 0; column < Math.max(actualValues.size(), values.size()); column++) {
            containsValue(row, column, column < expectedValues.size() ? expectedValues.get(column) : "");
        }
        return this;
    }

    public ExcelFileAssertions containsValue(int row, int column, Object expectedValue) {
        isNotNull();

        String actualValueStr = actual.getValue(row, column);
        String expectedValueStr = expectedValue != null ? expectedValue.toString() : null;

        if (isEmptyValue(actualValueStr) && isEmptyValue(expectedValue)) {
            return this;
        }

        info.description("Total line:\n%s\nExpected value on line %d, column %s to be <%s> but was <%s>",
                lineStr(row, actual), row, headerOrColumn(column), expectedValueStr, actualValueStr);
        objects.assertEqual(info, actualValueStr, expectedValueStr);
        return this;
    }

    public ExcelFileAssertions containsValue(int row, Header header, Object expectedValue) {
        return containsValue(row, header.getTitle(), expectedValue);
    }

    public ExcelFileAssertions containsValue(int row, String header, Object expectedValue) {
        isNotNull();
        containsHeader(header);

        String actualValueStr = actual.getValue(row, header);
        String expectedValueStr = expectedValue != null ? expectedValue.toString() : null;

        if (isEmptyValue(actualValueStr) && isEmptyValue(expectedValue)) {
            return this;
        }

        if (expectedValue instanceof Boolean &&
                (Boolean.parseBoolean(actualValueStr) == (Boolean) expectedValue)) {
            return this;
        }

        info.description("Total line:\n%s\nExpected value on row %d, column '%s' to be <%s> but was <%s>.",
                lineStr(row, actual), row, escapeText(header), expectedValueStr, actualValueStr);
        objects.assertEqual(info, actualValueStr, expectedValueStr);
        return this;
    }

    public ExcelFileAssertions containsValue(int line, int column) {
        isNotNull();

        String actualValue = actual.getValue(line, column);
        if (isEmptyValue(actualValue)) {
            failWithMessage("Total line:\n%s\nExpected to has value on line %d, column %d but actually is null or " +
                            "empty.", lineStr(line, actual), line, headerOrColumn(column));
        }
        return this;
    }

    public ExcelFileAssertions containsValue(int line, String header) {
        isNotNull();
        containsHeader(header);

        String actualValue = actual.getValue(line, header);
        if (isEmptyValue(actualValue)) {
            failWithMessage("Total line:\n%s\nExpected to has value on line %d, column %s but actually is null or " +
                            "empty", lineStr(line, actual), line, escapeText(header));
        }
        return this;
    }

    public ExcelFileAssertions doesntContainValue(int line, int column) {
        isNotNull();

        String actualValue = actual.getValue(line, column);
        if (!isEmptyValue(actualValue)) {
            failWithMessage("Total line:\n%s\nDoesn't expect to contain value on line %d, column %d, but was <%s>.",
                    lineStr(line, actual), line, headerOrColumn(column), actualValue);
        }
        return this;
    }

    public ExcelFileAssertions doesntContainValue(int line, Header header) {
        return doesntContainValue(line, header.getTitle());
    }

    public ExcelFileAssertions doesntContainValue(int line, String header) {
        isNotNull();
        containsHeader(header);

        String actualValue = actual.getValue(line, header);
        if (!isEmptyValue(actualValue)) {
            failWithMessage("Total line:\n%s\nDoesn't expect to contain value on line %d, column %d, but was <%s>.",
                    lineStr(line, actual), line, escapeText(header), actualValue);
        }
        return this;
    }

    public ExcelFileAssertions doesntContainValuesOnLine(int line) {
        isNotNull();

        Map<Boolean, List<String>> map = actual.getHeaders().stream()
                .collect(Collectors.partitioningBy(header -> !StringUtils.isEmpty(actual.getValue(line, header))));

        List<String> headersWithValues = map.get(true);
        List<String> headersWithoutValues = map.get(false);

        if (!headersWithValues.isEmpty()) {
            String generalMessage = String.format("Expected file doesn't contain values on %d line.", line);
            String headersWithValuesMessage = "But some headers has values:\n" + escapeTextJoin(headersWithValues);

            failWithMessage(generalMessage + "\n" + headersWithValuesMessage);
        }

        return this;
    }

    public ExcelFileAssertions doesntContainValuesOnLineExcept(int line, String... exceptHeaders) {
        isNotNull();
        containsHeaders(exceptHeaders);

        Map<Boolean, List<String>> map = actual.getHeaders().stream()
                .collect(Collectors.partitioningBy(header -> !StringUtils.isEmpty(actual.getValue(line, header))));

        Set<String> headersWithValues = new HashSet<>(map.get(true));
        Set<String> headersWithoutValues = new HashSet<>(map.get(false));

        Set<String> exceptHeadersSet = new HashSet<>(Arrays.asList(exceptHeaders));
        if (!headersWithValues.equals(exceptHeadersSet)) {
            String generalMessage = String.format("Expected file doesn't contain values on %d line" +
                    " except values on %s headers.", line, String.join(", ", exceptHeaders));
            String headersWithValuesMessage = "Headers with values: " + String.join(", ", headersWithValues);
            String headersWithoutValuesMessage = "Headers without values: " + String.join(", ", headersWithoutValues);

            failWithMessage(generalMessage + "\n" + headersWithValuesMessage + "\n" + headersWithoutValuesMessage);
        }

        return this;
    }

    public ExcelFileAssertions containsFormattingForRange(List<String> range) {
        isNotNull();

        Set<String> ranges = actual.getConditionFormattings()
                .stream()
                .map(ConditionFormattingsWithRange::getRange)
                .collect(Collectors.toSet());
        if (!ranges.containsAll(range)) {
            failWithMessage("Expected to contain formatting for range '%s' but actual formattings are:\n%s",
                    escapeTextJoin(range), escapeTextJoin(ranges));
        }

        return this;
    }

    public StringAssert getValue(int line, int column) {
        String value = actual.getValue(line, column);
        return new StringAssert(value);
    }

    public StringAssert getValue(int line, String header) {
        containsHeader(header);
        String value = actual.getValue(line, header);
        return new StringAssert(value);
    }
}

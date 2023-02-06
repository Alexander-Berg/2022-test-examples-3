package ru.yandex.test.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;

import ru.yandex.io.StringBuilderWriter;
import ru.yandex.json.dom.ValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.writer.JsonType;
import ru.yandex.json.writer.JsonValue;
import ru.yandex.json.writer.JsonWriterBase;

public class JsonChecker implements Checker {
    public static final double DEFAULT_PRECISION = 8192d;
    public static final double DEFAULT_EPSILON = 0d;

    public static final Object ANY_VALUE = new Object() {
        @Override
        public boolean equals(final Object o) {
            return o != null;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "<any value>";
        }
    };

    private final Object expected;
    private final double precision;
    private final double epsilon;
    private final JsonSchema schema;

    public JsonChecker(final String expected) {
        this(expected, null);
    }

    public JsonChecker(final String expected, final JsonSchema schema) {
        this(expected, DEFAULT_PRECISION, DEFAULT_EPSILON, schema);
    }

    public JsonChecker(
        final String expected,
        final double precision,
        final double epsilon)
    {
        this(expected, precision, epsilon, null);
    }

    public JsonChecker(
        final String expected,
        final double precision,
        final double epsilon,
        final JsonSchema schema)
    {
        this(parse(expected), precision, epsilon, schema);
    }

    public JsonChecker(final Object expected) {
        this(expected, DEFAULT_PRECISION, DEFAULT_EPSILON);
    }

    public JsonChecker(
        final Object expected,
        final double precision,
        final double epsilon)
    {
        this(expected, precision, epsilon, null);
    }

    public JsonChecker(
        final Object expected,
        final double precision,
        final double epsilon,
        final JsonSchema schema)
    {
        this.precision = precision;
        this.epsilon = epsilon;
        this.expected = normalize(expected);
        this.schema = schema;
    }

    @SuppressWarnings("unchecked")
    public Object normalize(final Object json) {
        Object result;
        if (json instanceof Map) {
            TreeMap<String, Object> map = new TreeMap<>();
            result = map;
            for (Map.Entry<String, Object> entry
                : ((Map<String, Object>) json).entrySet())
            {
                map.put(entry.getKey(), normalize(entry.getValue()));
            }
        } else if (json instanceof List) {
            List<Object> list = new ArrayList<>();
            result = list;
            for (Object obj: (List<Object>) json) {
                list.add(normalize(obj));
            }
        } else if (json == null) {
            result = null;
        } else if (json instanceof Boolean) {
            result = (Boolean) json;
        } else if (json instanceof Double) {
            Double value = (Double) json;
            long longValue = value.longValue();
            if (value.doubleValue() == longValue) {
                result = longValue;
            } else {
                result = new DoubleWrapper(value, precision, epsilon);
            }
        } else if (json instanceof Long) {
            result =
                new LongWrapper(((Long) json).longValue(), precision, epsilon);
        } else if (json instanceof String) {
            String value = (String) json;
            if (value.equals(ANY_VALUE.toString())) {
                result = ANY_VALUE;
            } else if (value.startsWith(StringDoubleWrapper.PREFIX)) {
                result = new StringDoubleWrapper(value, precision, epsilon);
            } else if (value.startsWith(StringJsonWrapper.PREFIX)) {
                result = new StringJsonWrapper(value);
            } else {
                result = value;
            }
        } else if (json instanceof DoubleWrapper) {
            result = json;
        } else if (json instanceof StringDoubleWrapper) {
            result = json;
        } else if (json instanceof StringJsonWrapper) {
            result = json;
        } else if (json == ANY_VALUE) {
            result = ANY_VALUE;
        } else {
            result = new ObjectWrapper(json);
        }
        return result;
    }

    private static Object parse(final String json) {
        try {
            return ValueContentHandler.parse(json);
        } catch (JsonException e) {
            throw new AssertionError("Failed to parse json <" + json + '>', e);
        }
    }

    public Object expected() {
        return expected;
    }

    @Override
    public String toString() {
        return JsonType.NORMAL.toString(expected);
    }

    @Override
    public int hashCode() {
        return expected.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof String && check((String) o) == null;
    }

    protected void filter(final Object expected, final Object actual) {
    }

    @Override
    public String check(final String value) {
        String result = null;
        try {
            if (schema != null) {
                ProcessingReport report =
                    schema.validate(JsonLoader.fromString(value));
                Iterator<ProcessingMessage> iter = report.iterator();
                if (iter.hasNext()) {
                    throw iter.next().asException();
                }
            }
            Object actual = normalize(ValueContentHandler.parse(value));
            filter(expected, actual);
            if (!Objects.equals(expected, actual)) {
                normalizeValues(actual, expected);
                result = StringChecker.compare(
                    JsonType.HUMAN_READABLE.toString(expected),
                    JsonType.HUMAN_READABLE.toString(actual));
            }
        } catch (IOException | JsonException | ProcessingException e) {
            StringBuilder sb = new StringBuilder("failed to ");
            if (e instanceof ProcessingException) {
                sb.append("validate");
            } else {
                sb.append("parse");
            }
            sb.append(' ');
            sb.append('<');
            sb.append(value);
            sb.append(">: ");
            e.printStackTrace(new StringBuilderWriter(sb));
            result = new String(sb);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static void normalizeValues(
        final Object actual,
        final Object expected)
    {
        if (actual instanceof Map && expected instanceof Map) {
            Map<String, Object> actualMap = (Map<String, Object>) actual;
            Map<String, Object> expectedMap = (Map<String, Object>) expected;
            for (Map.Entry<String, Object> entry: actualMap.entrySet()) {
                Object actualValue = entry.getValue();
                Object expectedValue = expectedMap.get(entry.getKey());
                if (expectedValue != null) {
                    if (expectedValue.equals(actualValue)) {
                        entry.setValue(expectedValue);
                    } else {
                        normalizeValues(actualValue, expectedValue);
                    }
                }
            }
        } else if (actual instanceof List && expected instanceof List) {
            List<Object> actualList = (List<Object>) actual;
            List<Object> expectedList = (List<Object>) expected;
            int size = Math.min(expectedList.size(), actualList.size());
            for (int i = 0; i < size; ++i) {
                Object actualValue = actualList.get(i);
                Object expectedValue = expectedList.get(i);
                if (Objects.equals(expectedValue, actualValue)) {
                    actualList.set(i, expectedValue);
                } else {
                    normalizeValues(actualValue, expectedValue);
                }
            }
        }
    }

    private static class ObjectWrapper {
        protected final Object value;

        ObjectWrapper(final Object value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value.toString() + '(' + value.getClass().getName() + ')';
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof ObjectWrapper) {
                return value.equals(((ObjectWrapper) o).value);
            }
            return false;
        }
    }

    public static class DoubleWrapper implements JsonValue {
        private final double value;
        private final double minValue;
        private final double maxValue;

        public DoubleWrapper(
            final double value,
            final double precision,
            final double epsilon)
        {
            this.value = value;
            double gap = Math.max(epsilon, Math.abs(value / precision));
            minValue = value - gap;
            maxValue = value + gap;
        }

        @Override
        public boolean equals(final Object o) {
            double value;
            if (o instanceof Number) {
                value = ((Number) o).doubleValue();
            } else if (o instanceof DoubleWrapper) {
                value = ((DoubleWrapper) o).value;
            } else {
                return false;
            }
            // ERROR_PRONE: without using this.value, will generate warning
            return value == this.value
                || (value >= minValue && value <= maxValue);
        }

        @Override
        public int hashCode() {
            return Double.hashCode(value);
        }

        @Override
        public String toString() {
            return Double.toString(value);
        }

        @Override
        public void writeValue(final JsonWriterBase writer)
            throws IOException
        {
            writer.value(value);
        }
    }

    public static class LongWrapper implements JsonValue {
        private final long value;
        private final double precision;
        private final double epsilon;

        public LongWrapper(
            final long value,
            final double precision,
            final double epsilon)
        {
            this.value = value;
            this.precision = precision;
            this.epsilon = epsilon;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof Long) {
                return value == ((Long) o).longValue();
            } else if (o instanceof LongWrapper) {
                return value == ((LongWrapper) o).value;
            } else {
                return new DoubleWrapper(value, precision, epsilon).equals(o);
            }
        }

        @Override
        public int hashCode() {
            return Long.hashCode(value);
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }

        @Override
        public void writeValue(final JsonWriterBase writer)
            throws IOException
        {
            writer.value(value);
        }
    }

    public static class StringDoubleWrapper {
        public static final String PREFIX = "<double value>";

        private final DoubleWrapper value;

        public StringDoubleWrapper(
            final String value,
            final double precision,
            final double epsilon)
        {
            this.value =
                new DoubleWrapper(
                    Double.parseDouble(value.substring(PREFIX.length())),
                    precision,
                    epsilon);
        }

        @SuppressWarnings("EqualsIncompatibleType")
        @Override
        public boolean equals(final Object o) {
            return o instanceof String
                && value.equals(Double.valueOf((String) o));
        }

        @Override
        public int hashCode() {
            return value.toString().hashCode();
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    public class StringJsonWrapper {
        public static final String PREFIX = "<json value>";

        private final Object value;
        private final int hashCode;

        public StringJsonWrapper(final String value) {
            this.value = normalize(parse(value.substring(PREFIX.length())));
            hashCode = value.hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            return o instanceof String
                && value.equals(normalize(parse((String) o)));
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return JsonType.HUMAN_READABLE.toString(value);
        }
    }
}


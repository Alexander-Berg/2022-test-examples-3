package ru.yandex.tikaite.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import ru.yandex.http.util.server.HttpServerConfigBuilder;
import ru.yandex.json.dom.ValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.search.document.mail.MailMetaInfo;
import ru.yandex.test.util.YandexAssert;

public class Json {
    public static final Object ANY_VALUE = new Object() {
        @Override
        public boolean equals(final Object o) {
            return o != null;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public String toString() {
            return "ANY_VALUE";
        }
    };

    private static final String LF = "\n";

    public interface Assertable {
        void assertEquals(String str, Object o);
    }

    public static class AnyOf {
        private final Object[] options;

        public AnyOf(final Object... options) {
            this.options = options;
        }

        @Override
        public boolean equals(final Object o) {
            for (Object option: options) {
                if ((option == null && o == null)
                    || (option != null && option.equals(o)))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            for (Object option: options) {
                hashCode ^= option.hashCode();
            }
            return hashCode;
        }

        @Override
        public String toString() {
            return "AnyOf(" + Arrays.toString(options) + ')';
        }
    }

    // null patterns not allowed
    public static class AllOf implements Assertable {
        private final Object[] patterns;

        public AllOf(final Object... patterns) {
            this.patterns = patterns;
        }

        @Override
        public boolean equals(final Object o) {
            for (Object pattern: patterns) {
                if (!pattern.equals(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            for (Object pattern: patterns) {
                hashCode ^= pattern.hashCode();
            }
            return hashCode;
        }

        @Override
        public String toString() {
            return "AllOf(" + Arrays.toString(patterns) + ')';
        }

        @Override
        public void assertEquals(final String prefix, final Object o) {
            for (Object pattern: patterns) {
                if (!pattern.equals(o)) {
                    Assert.assertEquals(prefix, pattern, o);
                }
            }
        }
    }

    public static class Headers implements Assertable {
        private final String expected;

        public Headers(final String expected) {
            this.expected = sortHeaders(expected);
        }

        private static String sortHeaders(final String headers) {
            String[] components = headers.split(LF);
            Arrays.sort(components);
            StringBuilder sb = new StringBuilder(components[0]);
            for (int i = 1; i < components.length; ++i) {
                sb.append('\n');
                sb.append(components[i]);
            }
            return sb.toString();
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof String) {
                return expected.equals(sortHeaders((String) o));
            }
            return false;
        }

        @Override
        public int hashCode() {
            return expected.hashCode();
        }

        @Override
        public String toString() {
            return "Headers(" + expected + ')';
        }

        @Override
        public void assertEquals(final String prefix, final Object o) {
            if (o instanceof String) {
                Assert.assertEquals(prefix, expected, sortHeaders((String) o));
            } else {
                Assert.fail(prefix + ". Expected to be String: " + o);
            }
        }
    }

    public static class StartsWith {
        private final String prefix;

        public StartsWith(final String prefix) {
            this.prefix = prefix;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof String) {
                return ((String) o).startsWith(prefix);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return prefix.hashCode();
        }

        @Override
        public String toString() {
            return "StartsWith(" + prefix + ')';
        }
    }

    public static class Contains {
        private final String pattern;

        public Contains(final String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof String) {
                return ((String) o).indexOf(pattern) != -1;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return pattern.hashCode();
        }

        @Override
        public String toString() {
            return "Contains(" + pattern + ')';
        }
    }

    public static class Not {
        private final Object obj;

        public Not(final Object obj) {
            this.obj = obj;
        }

        @Override
        public boolean equals(final Object o) {
            return !obj.equals(o);
        }

        @Override
        public int hashCode() {
            return ~obj.hashCode();
        }

        @Override
        public String toString() {
            return "Not(" + obj + ')';
        }
    }

    private static final String ELEMENT = "Element '";
    private static final String EQUAL = "' = '";

    private final String headers;
    private final String mid;
    private final String prefixField;
    private final String prefix;
    private final List<Object> docs;
    private final Map<String, Object> root;

    // CSOFF: ParameterNumber
    public Json(
        final Object mimetype,
        final Object bodyText,
        final Object parsed,
        final Object error,
        final Object meta)
    {
        headers = null;
        mid = null;
        prefixField = null;
        prefix = null;
        docs = null;
        root = new HashMap<>();
        root.put(CommonFields.BUILT_DATE, HttpServerConfigBuilder.BUILT_DATE);
        if (meta != null) {
            root.put(CommonFields.META, meta);
        }
        root.put(CommonFields.MIMETYPE, mimetype);
        if (bodyText != null) {
            root.put(CommonFields.BODY_TEXT, bodyText);
        }
        root.put(CommonFields.PARSED, parsed);
        if (error != null) {
            root.put(CommonFields.ERROR, error);
        }
    }
    // CSON: ParameterNumber

    public Json(final String mid, final Long suid) {
        this(null, mid, suid);
    }

    public Json(final String headers, final String mid, final Long suid) {
        this(headers, mid, MailMetaInfo.SUID, suid);
    }

    // CSOFF: ParameterNumber
    public Json(
        final String headers,
        final String mid,
        final String prefixField,
        final Long prefix)
    {
        this(headers, mid, prefixField, prefix, null);
    }

    public Json(
        final String headers,
        final String mid,
        final Long prefix,
        final String mdb)
    {
        this(headers, mid, MailMetaInfo.SUID, prefix, mdb);
    }

    public Json(
        final String headers,
        final String mid,
        final String prefixField,
        final Long prefix,
        final String mdb)
    {
        this.headers = headers;
        this.mid = mid;
        this.prefixField = prefixField;
        root = new HashMap<>();
        if (prefix == null) {
            this.prefix = null;
        } else {
            this.prefix = prefix.toString();
            root.put(MailMetaInfo.PREFIX, this.prefix);
        }
        docs = new ArrayList<>();
        if (mdb != null) {
            root.put(MailMetaInfo.MDB, mdb);
        }
        root.put(MailMetaInfo.DOCS, docs);
    }
    // CSON: ParameterNumber

    public Map<String, Object> root() {
        return root;
    }

    public Map<String, Object> createDoc(final String hid) {
        return createDoc(hid, headers);
    }

    public Map<String, Object> createDoc(
        final String hid,
        final String headers)
    {
        Object headersObject;
        if (headers == null) {
            headersObject = null;
        } else {
            headersObject = new Headers(headers);
        }
        return createDoc(hid, headersObject);
    }

    public Map<String, Object> createDoc(
        final String hid,
        final Object headers)
    {
        Map<String, Object> doc = new HashMap<>();
        if (headers != null) {
            doc.put(MailMetaInfo.HEADERS, headers);
        }
        if (mid != null) {
            doc.put(MailMetaInfo.MID, mid);
            doc.put(MailMetaInfo.URL, mid + '/' + hid);
        }
        if (prefix != null) {
            doc.put(prefixField, prefix);
        }
        doc.put(MailMetaInfo.HID, hid);
        doc.put(CommonFields.BUILT_DATE, HttpServerConfigBuilder.BUILT_DATE);
        docs.add(doc);
        return doc;
    }

    public static void assertEquals(
        final Map<String, Object> expected,
        final Map<String, Object> actual)
    {
        assertEquals("", expected, actual);
    }

    @SuppressWarnings("unchecked")
    public static void assertEquals(
        final String prefix,
        final Map<String, Object> expected,
        final Map<String, Object> actual)
    {
        for (Map.Entry<String, Object> entry: expected.entrySet()) {
            Object value = actual.get(entry.getKey());
            if (value == null) {
                Assert.fail(
                    prefix + ELEMENT + entry.getKey() + EQUAL
                    + entry.getValue() + "' expected, but wasn't found");
            } else {
                if (value instanceof List) {
                    List<Object> actArray = (List<Object>) value;
                    List<Object> expArray = (List<Object>) entry.getValue();
                    YandexAssert.assertSize(expArray, actArray);
                    for (int i = 0; i < expArray.size(); ++i) {
                        assertEquals(
                            "At element #" + i + ": ",
                            (Map<String, Object>) expArray.get(i),
                            (Map<String, Object>) actArray.get(i));
                    }
                } else if (entry.getValue() instanceof Assertable) {
                    ((Assertable) entry.getValue()).assertEquals(
                        prefix + ELEMENT + entry.getKey() + '\'', value);
                } else {
                    Assert.assertEquals(
                        prefix + ELEMENT + entry.getKey() + '\'',
                        entry.getValue(),
                        value);
                }
            }
        }
        for (Map.Entry<String, Object> entry: actual.entrySet()) {
            Object value = expected.get(entry.getKey());
            if (value == null) {
                Assert.fail(
                    prefix + "Unexpected entry found: '"
                    + entry.getKey() + EQUAL + entry.getValue() + '\'');
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void assertEquals(final String json) {
        try {
            assertEquals(
                root,
                (Map<String, Object>) ValueContentHandler.parse(json));
        } catch (JsonException e) {
            Assert.fail("Failed to parse string '" + json + "': " + e);
        }
    }
}


package ru.yandex.xavier;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import ru.yandex.json.dom.BasicContainerFactory;
import ru.yandex.json.dom.JsonLong;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonNull;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.test.util.Checker;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;

public class CountersChecker implements Checker {
    public enum CATEGORY {
        PEOPLE,
        SOCIAL,
        ESHOPS,
        TRIPS
    }

    private static final String UNREAD = "unread";

    private final Object expected;

    public CountersChecker(final Map<String, Integer> expected) {
        this.expected = convert(expected);
    }

    public CountersChecker(final int... counters) {
        assert counters.length == CATEGORY.values().length;

        Map<String, Integer> expected = new LinkedHashMap<>();
        for (CATEGORY c: CATEGORY.values()) {
            expected.put(
                c.name().toLowerCase(Locale.ROOT),
                counters[c.ordinal()]);
        }

        this.expected = convert(expected);
    }

    @Override
    public String check(final String value) {
        String result = null;

        try {
            JsonObject root = TypesafeValueContentHandler.parse(value);
            JsonMap counters =
                root.asMap().getMap("payload").getMap("message")
                    .getMap("counters");

            Object actual = JsonChecker.normalize(counters);
            if (!expected.equals(actual)) {
                result = StringChecker.compare(
                    expected.toString(),
                    actual.toString());
            }
        } catch (JsonException je) {
            result = new JsonChecker(JsonNull.INSTANCE).check(value);
        }

        return result;
    }

    private static Object convert(final Map<String, Integer> map) {
        JsonMap json = new JsonMap(BasicContainerFactory.INSTANCE);
        for (Map.Entry<String, Integer> entry: map.entrySet()) {
            JsonMap unread = new JsonMap(BasicContainerFactory.INSTANCE);
            unread.put(UNREAD, new JsonLong(entry.getValue()));
            json.put(entry.getKey(), unread);
        }

        return JsonChecker.normalize(json);
    }
}

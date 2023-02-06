package ru.yandex.market.stat.dicts.utils;

import com.google.common.collect.Lists;
import ru.yandex.market.stat.dicts.loaders.DictionaryLoadIterator;
import ru.yandex.market.stat.dicts.parsers.DictionaryParser;
import ru.yandex.market.stat.dicts.records.DictionaryRecord;
import ru.yandex.market.stats.test.data.TestDataResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
public abstract class ParserTestUtil {

    public static <T extends DictionaryRecord> List<T> loadRecords(DictionaryParser<T> parser, String resource) throws IOException {
        try (InputStream is = getResourceAsStream(resource)) {
            return Lists.newArrayList(parser.createLoadIterator(is));
        }
    }

    public static List<String> readAsText(String resource) throws IOException {
        try (InputStream is = getResourceAsStream(resource); BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.toList());
        }
    }

    public static <T extends DictionaryRecord> DictionaryLoadIterator<T> makeIterator(DictionaryParser<T> parser, String resource) throws IOException {
        return parser.createLoadIterator(getResourceAsStream(resource));
    }

    public static InputStream getResourceAsStream(String resource) throws IOException {
        InputStream resourceAsStream = TestDataResolver.getResource(resource);
        if (resource.endsWith(".gz")) {
            resourceAsStream = new GZIPInputStream(resourceAsStream);
        }
        return resourceAsStream;
    }
}

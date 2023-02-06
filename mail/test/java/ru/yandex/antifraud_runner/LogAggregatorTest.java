package ru.yandex.antifraud_runner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.stream.Stream;

import org.junit.Test;

import ru.yandex.io.StringBuilderWriter;
import ru.yandex.json.dom.BasicContainerFactory;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.writer.JsonType;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class LogAggregatorTest extends TestBase {
    @Test
    public void test() throws Throwable {
        try (final Cluster cluster = new Cluster(this);
             final Stream<String> inputStream = Runner.getInputStream(
                     URI.create("file://" + resource("yt_log.json.txt").toString())
             )) {

            final StringBuilderWriter sb = new StringBuilderWriter();

            {
                final Runner runner = cluster.getRunner();
                final var consumer = runner.makeConsumer(sb);

                runner.processSafe(inputStream, consumer);
                consumer.finish();
            }

            final JsonMap got;
            try (BufferedReader r = new BufferedReader(new StringReader(sb.toString()))) {
                got = linesToMap(r);
            }

            final JsonMap expected;
            try (BufferedReader r = new BufferedReader(new FileReader(resource("batch_save_1.json.txt").toFile(),
                    Charset.defaultCharset()))) {
                expected = linesToMap(r);
            }

            YandexAssert.check(new JsonChecker(JsonType.NORMAL.toString(expected)),
                    JsonType.NORMAL.toString(got));
        }
    }

    JsonMap linesToMap(BufferedReader reader) throws JsonException, IOException {
        final JsonMap map = new JsonMap(BasicContainerFactory.INSTANCE);
        while (true) {
            final String line = reader.readLine();
            if (line == null) {
                break;
            }
            final JsonMap parsed = TypesafeValueContentHandler.parse(line).asMap();
            map.put(parsed.getString("id"), parsed);
        }
        return map;
    }
}

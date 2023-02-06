package ru.yandex.antifraud;

import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

import org.junit.Test;

import ru.yandex.antifraud.util.QueryHelpers;
import ru.yandex.io.StringBuilderWriter;
import ru.yandex.json.writer.HumanReadableJsonWriter;
import ru.yandex.json.writer.JsonWriter;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class UtilTest extends TestBase {
    public UtilTest() {
        super(false, 0L);
    }
    @Test
    public void test() throws Exception {
        final StringBuilderWriter sb = new StringBuilderWriter();

        try (JsonWriter w = new HumanReadableJsonWriter(sb)) {
            w.startObject();
            QueryHelpers.INSTANCE.smoothIncrement(w, "count", "txn_timestamp",
                    Instant.ofEpochMilli(1655386467624L),
                    Duration.ofSeconds(1000), 5);
            w.endObject();
        }


        YandexAssert.check(new JsonChecker(Files.readString(resource("smooth-increment.json"))),
                sb.toString());
    }
}

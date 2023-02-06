package ru.yandex.jni.hypocrisy;

import java.nio.file.Files;
import java.util.Base64;

import org.junit.Test;

import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.writer.JsonType;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class JniHypocrisyTest extends TestBase {
    @Test
    public void test() throws Exception {
        final JsonObject decoded =
            JniHypocrisy.decode(
                Base64.getDecoder().decode(
                    "bY8PcLuZQsG+ZAwW2+pJjjkIP7YJ+vwumnpHLtnOrVw="),
                loadResourceAsString(
                    "antirobot/lib/hypocrisy/ut/valid_fingerprint.json"));
        YandexAssert.check(
            new JsonChecker(JsonType.DOLLAR.toString(decoded)),
            Files.readString(resource("decoded_fingerprint.json")));
    }
}


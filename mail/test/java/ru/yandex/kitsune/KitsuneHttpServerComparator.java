package ru.yandex.kitsune;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.apache.http.HttpResponse;

import ru.yandex.http.proxy.ProxySession;
import ru.yandex.http.util.nio.BasicAsyncResponseProducerGenerator;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.writer.JsonType;
import ru.yandex.kitsune.config.ImmutableKitsuneConfig;

public class KitsuneHttpServerComparator extends KitsuneHttpServer {
    public KitsuneHttpServerComparator(@Nonnull ImmutableKitsuneConfig kitsuneConfig) throws IOException {
        super(kitsuneConfig);
    }

    @Override
    public void compare(@Nonnull ProxySession session,
                        @Nonnull BasicAsyncResponseProducerGenerator head,
                        @Nonnull List<Map.Entry<String, HttpResponse>> tails) {

        final JsonObject headObject;
        try (Reader reader = new InputStreamReader(head.get().generateResponse().getEntity().getContent(),
                Charset.defaultCharset())) {
            headObject = TypesafeValueContentHandler.parse(reader);
        } catch (IOException | JsonException e) {
            logger.log(Level.WARNING, "fail to parse head data", e);
            return;
        }

        logger.fine(JsonType.NORMAL.toString(headObject));
    }
}

package ru.yandex.market.checkout.pushapi.client.util.test;

import java.io.ByteArrayOutputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONParser;

import ru.yandex.common.util.string.SimpleStringConverter;
import ru.yandex.market.checkout.common.json.jackson.Reader;
import ru.yandex.market.checkout.common.json.jackson.Writer;

public class JsonTestUtil {

    private static String trueJson(String json) {
        return JSONParser.parseJSON(json).toString();
    }

    private static JsonParser createParser(String json) throws Exception {
        final JsonFactory jsonFactory = new JsonFactory();

        final String trueJson = trueJson(json);
        return jsonFactory.createParser(trueJson.getBytes());
    }

    public static <T> void assertJsonSerialize(ru.yandex.market.checkout.common.json.JsonSerializer<T> serializer,
                                               T obj, String expectedJson) throws Exception {
        final JsonFactory jsonFactory = new JsonFactory();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final JsonGenerator generator = jsonFactory.createGenerator(baos);
        final Writer writer = new Writer(new ObjectMapper(), generator, createSimpleStringConverter(), serializer);
        serializer.serialize(obj, writer);
        generator.flush();

        final String acutalJson = new String(baos.toByteArray());
        JSONAssert.assertEquals(
                expectedJson,
                acutalJson,
                JSONCompareMode.NON_EXTENSIBLE // LENIENT нельзя! Слишком нестрогая проверка!
        );
    }

    private static SimpleStringConverter createSimpleStringConverter() {
        final SimpleStringConverter stringConverter = new SimpleStringConverter();
        stringConverter.setEmptyNulls(false);
        stringConverter.setDateFormatPattern("dd-MM-yyyy");
        return stringConverter;
    }

    public static <T> T deserialize(ru.yandex.market.checkout.common.json.JsonDeserializer<T> deserializer,
                                    String json) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        final Reader reader = new Reader(objectMapper, objectMapper.readTree(trueJson(json)),
                createSimpleStringConverter(), null);
        return deserializer.deserialize(reader);
    }
}

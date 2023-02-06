package ru.yandex.market.mbi.util;

import java.io.File;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.util.Functional.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для {@link JsonUtils}.
 *
 * @author Vladislav Bauer
 */
class JsonUtilsTest {

    @Test
    void testCreateJsonFile() throws Exception {
        checkCreateJsonFile(
                jsonGen -> {
                },
                "{}"
        );

        checkCreateJsonFile(
                jsonGen -> jsonGen.writeObjectField("half-truth", 21),
                "{\"half-truth\":21}"
        );
    }


    private void checkCreateJsonFile(
            ThrowingConsumer<JsonGenerator> generator, String expectedJson
    ) throws Exception {
        File jsonFile = JsonUtils.createJsonObjectFile(generator);
        String generatedJson = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);

        assertThat(generatedJson).isEqualTo(expectedJson);
    }

}

package ru.yandex.market.tpl.internal.service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.assertj.core.api.Assertions.assertThat;

public class YandexFormsParserTest {

    @Test
    public void testParsing() {
        MultiValueMap<String, String> formMap = new LinkedMultiValueMap<>();
        formMap.add("field_1", getFileContent("yaforms/firstreq/field_1.json"));
        formMap.add("field_2", getFileContent("yaforms/firstreq/field_2.json"));
        formMap.add("field_3", getFileContent("yaforms/firstreq/field_3.json"));
        formMap.add("field_4", getFileContent("yaforms/firstreq/field_4.json"));
        formMap.add("field_5", getFileContent("yaforms/firstreq/field_5.json"));
        formMap.add("field_6", getFileContent("yaforms/firstreq/field_6.json"));

        var result = YandexFormsParser.parseRequest(formMap, Map.of());
        assertThat(result.getFirst("user_id")).isEqualTo("123");
        assertThat(result.getFirst("pvz_id")).isEqualTo("456");
        assertThat(result.getFirst("В пвз чисто?")).isEqualTo("Да");
        assertThat(result.getFirst("Несколько вариантов")).isEqualTo("Вариант 1, Вариант 3");
        assertThat(result.get("Что улучшить?")).containsExactlyInAnyOrder("111", "000");
        assertThat(result).isNotNull();
    }

    @SneakyThrows
    private String getFileContent(String filename) {
        return IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(filename)), StandardCharsets.UTF_8);
    }
}

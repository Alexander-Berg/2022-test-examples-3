package ru.yandex.cms.client.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cms.client.model.CmsTemplateEntry;

class DeserializationTest extends AbstractCmsTest {

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("Десериализатор работает корректно при представление объекта как справочника.")
    @Test
    public void deserialize_templateEntry_objectAsMap() throws IOException {
        CmsTemplateEntry templateEntry = objectMapper.readValue(
                loadFile("json/deserialize_templateEntry.json"),
                CmsTemplateEntry.class
        );

        Assertions.assertThat(templateEntry.getFields().get("PAGE_LINK"))
                .isInstanceOf(ArrayList.class);
        Assertions.assertThat(templateEntry.getFields().get("TOP_MENU"))
                .isInstanceOf(LinkedHashMap.class);
        Assertions.assertThat(templateEntry.getFields().get("bottom"))
                .isInstanceOf(String.class);

        String jsonOutput = objectMapper.writeValueAsString(templateEntry);
        Assertions.assertThat(jsonOutput)
                .isNotBlank();
    }
}

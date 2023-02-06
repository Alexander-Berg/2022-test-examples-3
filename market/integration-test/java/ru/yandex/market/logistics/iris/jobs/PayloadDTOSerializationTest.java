package ru.yandex.market.logistics.iris.jobs;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.configuration.queue.DbQueueConfiguration;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.core.domain.target.TargetType;
import ru.yandex.market.logistics.iris.model.ItemIdentifierDTO;
import ru.yandex.market.logistics.iris.model.ItemNaturalKeyDTO;
import ru.yandex.market.logistics.iris.model.SourceDTO;
import ru.yandex.market.logistics.iris.model.TargetDTO;

public class PayloadDTOSerializationTest extends AbstractContextualTest {

    @Autowired
    @Qualifier(DbQueueConfiguration.DB_QUEUE_OBJECT_MAPPER)
    private ObjectMapper objectMapper;

    /**
     * Проверяем корректность сериализации объекта натурального ключа.
     */
    @Test
    public void itemNaturalKeySerialization() throws JsonProcessingException {
        ItemNaturalKeyDTO dto = new ItemNaturalKeyDTO(
            new ItemIdentifierDTO("pid", "psku"),
            new SourceDTO("145", SourceType.WAREHOUSE),
            new TargetDTO("147", TargetType.WAREHOUSE)
        );

        assertSerialization(dto, "fixtures/data/payload/natural_key.json");
    }

    /**
     * Проверяем корректность десериализации объекта натурального ключа.
     */
    @Test
    public void itemNaturalKeyDeserialization() throws IOException {
        ItemNaturalKeyDTO expected = new ItemNaturalKeyDTO(
            new ItemIdentifierDTO("pid", "psku"),
            new SourceDTO("145", SourceType.WAREHOUSE),
            new TargetDTO("147", TargetType.WAREHOUSE)
        );

        assertDeserialization("fixtures/data/payload/natural_key.json", expected, ItemNaturalKeyDTO.class);
    }

    /**
     * Проверяем корректность сериализации объекта натурального ключа без Target.
     */
    @Test
    public void itemNaturalKeyWithoutTargetSerialization() throws JsonProcessingException {
        ItemNaturalKeyDTO dto = new ItemNaturalKeyDTO(
            new ItemIdentifierDTO("pid", "psku"),
            new SourceDTO("145", SourceType.WAREHOUSE)
        );

        assertSerialization(dto, "fixtures/data/payload/natural_key_without_target.json");
    }

    /**
     * Проверяем корректность десериализации объекта натурального ключа без Target.
     */
    @Test
    public void itemNaturalKeyWithoutTargetDeserialization() throws IOException {
        ItemNaturalKeyDTO expected = new ItemNaturalKeyDTO(
            new ItemIdentifierDTO("pid", "psku"),
            new SourceDTO("145", SourceType.WAREHOUSE)
        );

        assertDeserialization("fixtures/data/payload/natural_key_without_target.json", expected, ItemNaturalKeyDTO.class);
    }

    private <T> void assertSerialization(T value, String expectedJsonPath) throws JsonProcessingException {
        String actualJson = objectMapper.writeValueAsString(value);
        String expectedJson = extractFileContent(expectedJsonPath);

        assertions().assertThat(actualJson).is(jsonMatch(expectedJson));
    }

    private <T> void assertDeserialization(String jsonPath,
                                           T expectedValue,
                                           Class<T> expectedValueClass) throws IOException {
        T actualValue = objectMapper.readValue(
            extractFileContent(jsonPath),
            expectedValueClass
        );

        assertions().assertThat(actualValue).isEqualTo(expectedValue);
    }
}

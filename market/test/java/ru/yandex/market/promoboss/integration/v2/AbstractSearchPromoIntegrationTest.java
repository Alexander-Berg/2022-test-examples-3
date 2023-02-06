package ru.yandex.market.promoboss.integration.v2;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.AccessLevel;
import lombok.Getter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.promoboss.integration.AbstractIntegrationTest;
import ru.yandex.mj.generated.client.self_client.model.PromoSearchRequestDtoV2Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Deprecated(since = "Используется только для кастомного ObjectMapper. Убрать класс, когда ошибка будет устранена")
public abstract class AbstractSearchPromoIntegrationTest extends AbstractIntegrationTest {
    @Getter(AccessLevel.PROTECTED)
    private final ObjectMapper testObjectMapper;

    protected AbstractSearchPromoIntegrationTest() {
        this.testObjectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(PromoSearchRequestDtoV2Sort.FieldEnum.class, new PromoSearchRequestDtoV2SortFieldEnumSerializer());
        testObjectMapper.registerModule(module);
    }

    static class PromoSearchRequestDtoV2SortFieldEnumSerializer extends StdSerializer<PromoSearchRequestDtoV2Sort.FieldEnum> {
        protected PromoSearchRequestDtoV2SortFieldEnumSerializer() {
            super(PromoSearchRequestDtoV2Sort.FieldEnum.class);
        }

        @Override
        public void serialize(PromoSearchRequestDtoV2Sort.FieldEnum value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toString());
        }
    }

    @ParameterizedTest
    @EnumSource(value = PromoSearchRequestDtoV2Sort.FieldEnum.class)
    void enumObjectMapperTest(PromoSearchRequestDtoV2Sort.FieldEnum fieldEnum) throws JsonProcessingException {
        assertNotEquals(String.format("\"%s\"", fieldEnum.getValue()), objectMapper.writeValueAsString(fieldEnum));
    }

    @ParameterizedTest
    @EnumSource(value = PromoSearchRequestDtoV2Sort.FieldEnum.class)
    void enumTestObjectMapperTest(PromoSearchRequestDtoV2Sort.FieldEnum fieldEnum) throws JsonProcessingException {
        assertEquals(String.format("\"%s\"", fieldEnum.getValue()), getTestObjectMapper().writeValueAsString(fieldEnum));
    }
}

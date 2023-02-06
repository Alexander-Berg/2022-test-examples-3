package ru.yandex.market.markup2.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Maps;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by alex-pekin on 06.12.2016.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class JsonUtilsTest extends TestCase {

    @Test
    public void testToJsonString() throws JsonProcessingException {
        Map<String, Object> map = Maps.newHashMap();
        map.put("key1", "value1");
        map.put("key2", 2);
        map.put("key3", 3.14);
        String str = JsonUtils.toJsonString(map);
        Assert.assertNotNull(str);
        Assert.assertEquals("{\"key1\":\"value1\",\"key2\":2,\"key3\":3.14}", str);
    }

    @Test
    public void testJsonArraySerialize() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule serializerModule = new SimpleModule();
        serializerModule.addSerializer(Container.class, new ColorsRefsSerializer());
        serializerModule.addDeserializer(Container.class, new ColorsRefsDeserializer());
        objectMapper.registerModule(serializerModule);

        Container obj = new Container();
        obj.intVal = 10;
        obj.intArr = new int[] {1, 2, 3};
        final String str = objectMapper.writeValueAsString(obj);
        Assert.assertNotNull(str);
        Assert.assertEquals("{\"anObj\":{\"intVal\":10,\"intArr\":[1,2,3]}}", str);

        Container obj2 = objectMapper.readValue(str, Container.class);
        Assert.assertNotNull(obj2);
        Assert.assertEquals(obj, obj2);
    }

    static class Container {
        int intVal;
        int[] intArr;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Container that = (Container) o;

            if (intVal != that.intVal) {
                return false;
            }
            return Arrays.equals(intArr, that.intArr);

        }

        @Override
        public int hashCode() {
            int result = intVal;
            result = 31 * result + Arrays.hashCode(intArr);
            return result;
        }
    }

    static class ColorsRefsSerializer extends JsonSerializer<Container> {

        @Override
        public void serialize(Container val, JsonGenerator gen, SerializerProvider serializers)
            throws IOException, JsonProcessingException {
            gen.writeStartObject();
            gen.writeObjectFieldStart("anObj");
            gen.writeNumberField("intVal", val.intVal);
            gen.writeArrayFieldStart("intArr");
            for (int i: val.intArr) {
                gen.writeNumber(i);
            }
            gen.writeEndArray();
            gen.writeEndObject();
            gen.writeEndObject();
        }
    }

    public static class ColorsRefsDeserializer extends JsonDeserializer<Container> {
        @Override
        public Container deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

            JsonNode root = p.getCodec().readTree(p);
            JsonNode node = root.get("anObj");
            Container fields = new Container();

            fields.intVal = node.get("intVal").intValue();
            ArrayNode arr = (ArrayNode) node.get("intArr");

            fields.intArr = new int[arr.size()];
            for (int i = 0; i < fields.intArr.length; i++) {
                fields.intArr[i] = arr.get(i).asInt();
            }

            return fields;
        }
    }
}

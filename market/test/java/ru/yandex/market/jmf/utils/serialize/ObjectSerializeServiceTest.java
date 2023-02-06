package ru.yandex.market.jmf.utils.serialize;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.utils.SerializationUtils;

public class ObjectSerializeServiceTest {

    ObjectSerializeService service;

    @BeforeEach
    public void setUp() {
        service = SerializationUtils.defaultObjectSerializeService();
    }

    @Test
    public void simpleInnerValue() {
        SimpleInnerClass inner = new SimpleInnerClass();
        inner.setValue(Randoms.string());

        SimpleType value = new SimpleType();
        value.setInnerValue(inner);

        SimpleType unserialized = serializeAndUnserialize(value, SimpleType.class);

        Assertions.assertNotNull(unserialized.getInnerValue());
        Assertions.assertEquals(inner.getValue(), unserialized.getInnerValue().getValue());
    }

    @Test
    public void simpleIntField() {
        SimpleType value = new SimpleType();
        value.setIntValue(ThreadLocalRandom.current().nextInt());

        SimpleType unserialized = serializeAndUnserialize(value, SimpleType.class);

        Assertions.assertEquals(value.getIntValue(), unserialized.getIntValue());
    }

    @Test
    public void simpleIntegerField() {
        SimpleType value = new SimpleType();
        value.setIntegerValue(ThreadLocalRandom.current().nextInt());

        SimpleType unserialized = serializeAndUnserialize(value, SimpleType.class);

        Assertions.assertEquals(value.getIntegerValue(), unserialized.getIntegerValue());
    }

    @Test
    public void simpleJsonIgnoreValue() {
        SimpleType value = new SimpleType();
        value.setJsonIgnoreValue(Randoms.string());

        SimpleType unserialized = serializeAndUnserialize(value, SimpleType.class);

        Assertions.assertNull(
                unserialized.getJsonIgnoreValue(), "Должны получить NULL т.к. поле помечено аннотацией @JsonIgnore");
    }

    @Test
    public void simpleMapValue() {
        String key = Randoms.string();
        String keyValue = Randoms.string();

        Map<String, Object> map = new HashMap<>();
        map.put(key, keyValue);

        SimpleType value = new SimpleType();
        value.setMapValue(map);

        SimpleType unserialized = serializeAndUnserialize(value, SimpleType.class);

        Assertions.assertNotNull(unserialized.getMapValue());
        Assertions.assertEquals(keyValue, unserialized.getMapValue().get(key));
    }

    @Test
    public void simpleStringField() {
        SimpleType value = new SimpleType();
        value.setStringValue(Randoms.string());

        SimpleType unserialized = serializeAndUnserialize(value, SimpleType.class);

        Assertions.assertEquals(value.getStringValue(), unserialized.getStringValue());
    }

    @Test
    public void simpleByteaField() {
        byte[] v = CrmStrings.getBytes(Randoms.string());

        SimpleType value = new SimpleType();
        value.setByteaValue(v);

        SimpleType unserialized = serializeAndUnserialize(value, SimpleType.class);

        Assertions.assertArrayEquals(v, unserialized.getByteaValue());
    }

    @Test
    public void simpleXmlTransientValue() {
        SimpleType value = new SimpleType();
        value.setXmlTransientValue(Randoms.string());

        SimpleType unserialized = serializeAndUnserialize(value, SimpleType.class);

        Assertions.assertNull(
                unserialized.getXmlTransientValue(), "Должны получить NULL т.к. поле помечено аннотацией " +
                        "@XmlTransient");
    }

    private <T> T serializeAndUnserialize(T value, Class<T> type) {
        byte[] serialized = service.serialize(value);
        System.out.println(CrmStrings.valueOf(serialized));
        return service.deserialize(serialized, type);
    }

    public static class SimpleInnerClass {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class SimpleType {
        private String stringValue;
        private int intValue;
        private Integer integerValue;

        @JsonIgnore
        private String jsonIgnoreValue;
        @XmlTransient
        private String xmlTransientValue;

        private Map<String, Object> mapValue;
        private SimpleInnerClass innerValue;
        private byte[] byteaValue;

        public SimpleInnerClass getInnerValue() {
            return innerValue;
        }

        public void setInnerValue(SimpleInnerClass innerValue) {
            this.innerValue = innerValue;
        }

        public int getIntValue() {
            return intValue;
        }

        public void setIntValue(int intValue) {
            this.intValue = intValue;
        }

        public Integer getIntegerValue() {
            return integerValue;
        }

        public void setIntegerValue(Integer integerValue) {
            this.integerValue = integerValue;
        }

        public String getJsonIgnoreValue() {
            return jsonIgnoreValue;
        }

        public void setJsonIgnoreValue(String jsonIgnoreValue) {
            this.jsonIgnoreValue = jsonIgnoreValue;
        }

        public Map<String, Object> getMapValue() {
            return mapValue;
        }

        public void setMapValue(Map<String, Object> mapValue) {
            this.mapValue = mapValue;
        }

        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        public String getXmlTransientValue() {
            return xmlTransientValue;
        }

        public void setXmlTransientValue(String xmlTransientValue) {
            this.xmlTransientValue = xmlTransientValue;
        }

        public byte[] getByteaValue() {
            return byteaValue;
        }

        public void setByteaValue(byte[] byteaValue) {
            this.byteaValue = byteaValue;
        }
    }
}

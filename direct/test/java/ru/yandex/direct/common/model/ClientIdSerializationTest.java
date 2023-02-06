package ru.yandex.direct.common.model;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Test;

import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.utils.JsonUtils;

import static org.junit.Assert.assertEquals;

public class ClientIdSerializationTest {

    private Object testCase;

    static class ObjectTestCase {
        @SuppressWarnings("unused") // сериализуется в json
        @JsonProperty
        private ClientId clientId;

        ObjectTestCase(ClientId clientId) {
            this.clientId = clientId;
        }
    }

    static class ListTestCase {
        @SuppressWarnings("unused") // сериализуется в json
        @JsonProperty
        private List<ClientId> clientIds;

        ListTestCase(ClientId... clientIds) {
            this.clientIds = Arrays.asList(clientIds);
        }
    }

    @Test
    public void SerializeValueToObject() {
        testCase = new ObjectTestCase(ClientId.fromLong(1));
        assertEquals("{\"clientId\":1}", JsonUtils.toJson(testCase));
    }

    @Test
    public void SerializeNullToObject() {
        testCase = new ObjectTestCase(ClientId.fromNullableLong(null));
        assertEquals("{\"clientId\":null}", JsonUtils.toJson(testCase));
    }

    @Test
    public void SerializeEmptyList() {
        testCase = new ListTestCase();
        assertEquals("{\"clientIds\":[]}", JsonUtils.toJson(testCase));
    }

    @Test
    public void SerializeValueToList() {
        testCase = new ListTestCase(ClientId.fromLong(2));
        assertEquals("{\"clientIds\":[2]}", JsonUtils.toJson(testCase));
    }

    @Test
    public void SerializeNullToList() {
        testCase = new ListTestCase(ClientId.fromNullableLong(null));
        assertEquals("{\"clientIds\":[null]}", JsonUtils.toJson(testCase));
    }

    @Test
    public void SerializeSeveralValuesToList() {
        testCase = new ListTestCase(ClientId.fromLong(3), ClientId.fromLong(4));
        assertEquals("{\"clientIds\":[3,4]}", JsonUtils.toJson(testCase));
    }

    @Test
    public void SerializeMixedValueAndNullToList() {
        testCase = new ListTestCase(ClientId.fromLong(5), ClientId.fromLong(6), ClientId.fromNullableLong(null));
        assertEquals("{\"clientIds\":[5,6,null]}", JsonUtils.toJson(testCase));
    }
}

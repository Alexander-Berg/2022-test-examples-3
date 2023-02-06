package ru.yandex.market.markup2.tasks.auto_formalization_parameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("checkstyle:MagicNumber")
public class AutoFormalizationParametersSerializersTest {

    @Test
    public void testIdentitySerialization() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        AutoFormalizationParametersTaskIdentity identity = new AutoFormalizationParametersTaskIdentity(10L);

        String serialized = objectMapper.writeValueAsString(identity);
        AutoFormalizationParametersTaskIdentity parsedIdentity =
            objectMapper.readValue(serialized, AutoFormalizationParametersTaskIdentity.class);

        Assert.assertEquals(identity, parsedIdentity);
    }

    @Test
    public void testAttributesSerialization() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        AutoFormalizationParametersDataAttributes attributes = createAttributes();

        String serialized = objectMapper.writeValueAsString(attributes);
        AutoFormalizationParametersDataAttributes parsedAttributes =
            objectMapper.readValue(serialized, AutoFormalizationParametersDataAttributes.class);

        Assert.assertEquals(attributes.getScTable(), parsedAttributes.getScTable());
    }

    @Test
    public void testPayloadSerialization() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        AutoFormalizationParametersPayload payload = new AutoFormalizationParametersPayload(1L, createAttributes());

        String serialized = objectMapper.writeValueAsString(payload);
        AutoFormalizationParametersPayload parsedPayload =
            objectMapper.readValue(serialized, AutoFormalizationParametersPayload.class);

        Assert.assertEquals(payload.getDataIdentifier(), parsedPayload.getDataIdentifier());
        Assert.assertEquals(payload.getAttributes().getScTable(), parsedPayload.getAttributes().getScTable());
    }

    @Test
    public void testRequestSerialization() throws IOException {
        JsonSerializer<? super TaskDataItem<AutoFormalizationParametersPayload, AutoFormalizationParametersResponse>>
            serializer = new AutoFormalizationParametersDataItemsProcessor().getRequestSerializer();

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule serializerModule = new SimpleModule().addSerializer((Class) TaskDataItem.class, serializer);
        objectMapper.registerModule(serializerModule);

        AutoFormalizationParametersPayload payload = new AutoFormalizationParametersPayload(1L, createAttributes());
        TaskDataItem<AutoFormalizationParametersPayload, AutoFormalizationParametersResponse> item =
            new TaskDataItem(100L, payload);

        String serialized = objectMapper.writeValueAsString(item);
        JsonNode jsonRoot = objectMapper.reader().readTree(serialized);
        List<String> keys = new ArrayList<>();
        jsonRoot.fields().forEachRemaining(e -> keys.add(e.getKey()));
        Assert.assertTrue(keys.containsAll(Arrays.asList("id", "sc_table")));
    }

    @Test
    public void testResponseSerialization() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        AutoFormalizationParametersResponse response = new AutoFormalizationParametersResponse("100", "mrfile",
            "cluster", 10);
        Assert.assertTrue(response.hasResult());

        String serialized = objectMapper.writeValueAsString(response);
        AutoFormalizationParametersResponse parsedResponse =
            objectMapper.readValue(serialized, AutoFormalizationParametersResponse.class);

        Assert.assertEquals(response.getId(), parsedResponse.getId());
        Assert.assertEquals(response.getResult(), parsedResponse.getResult());
        Assert.assertEquals(response.getCluster(), parsedResponse.getCluster());
        Assert.assertEquals(response.getFeedCount(), parsedResponse.getFeedCount());
    }

    private AutoFormalizationParametersDataAttributes createAttributes() {
        return AutoFormalizationParametersDataAttributes.newBuilder().setScTable("sctable").build();
    }
}

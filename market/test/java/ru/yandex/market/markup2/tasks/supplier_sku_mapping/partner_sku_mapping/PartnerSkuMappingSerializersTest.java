package ru.yandex.market.markup2.tasks.supplier_sku_mapping.partner_sku_mapping;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.MappingModerationStatus;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;

import java.io.IOException;

@SuppressWarnings("checkstyle:MagicNumber")
public class PartnerSkuMappingSerializersTest {
    private ObjectMapper mapper;

    @Before
    public void setUp() {
        PartnerSkuMappingDataItemsProcessor processor = new PartnerSkuMappingDataItemsProcessor();
        mapper = new ObjectMapper()
            .setSerializationInclusion(Include.NON_NULL)
            .registerModule(
                new SimpleModule()
                    .addSerializer(TaskDataItem.class, (JsonSerializer<TaskDataItem>) processor.getRequestSerializer())
            );
    }

    @Test
    public void testPayloadSerialization() throws IOException {
        PartnerSkuMappingDataPayload payload = new PartnerSkuMappingDataPayload(
            new PartnerSkuMappingDataIdentity(100355223028L, 100357222784L),
            new PartnerSkuMappingDataAttributes(90796, "catName", 100500L));
        String expectedSerialization =
            "{\"dataIdentifier\":{\"generated_sku_id\":100355223028,\"market_sku_id\":100357222784}," +
                "\"attributes\":{\"category_id\":90796,\"category_name\":\"catName\",\"supplier_id\":100500}}";
        String serializedPayload = mapper.writeValueAsString(payload);
        Assert.assertEquals(expectedSerialization, serializedPayload);

        PartnerSkuMappingDataPayload payloadCopy = mapper.readValue(
            expectedSerialization, PartnerSkuMappingDataPayload.class
        );
        Assert.assertEquals(payload.getAttributes(), payloadCopy.getAttributes());
        Assert.assertEquals(payload.getDataIdentifier(), payloadCopy.getDataIdentifier());
    }

    @Test
    public void testHitmanObjectSerrialization() throws JsonProcessingException {
        PartnerSkuMappingDataPayload payload = new PartnerSkuMappingDataPayload(
            new PartnerSkuMappingDataIdentity(100355223028L, 100357222784L),
            new PartnerSkuMappingDataAttributes(90796, "catName", 100500L));

        TaskDataItem<PartnerSkuMappingDataPayload, Object> taskDataItem = new TaskDataItem<>(1L, payload);
        JsonNode jsonNode = mapper.valueToTree(taskDataItem);
        String hitmanRequest = mapper.writeValueAsString(jsonNode);
        String expectedSerialization = "{\"id\":\"1\",\"generated_sku_id\":\"100355223028\"," +
            "\"supplier_mapping_info\":{\"sku_id\":\"100357222784\"},\"category_id\":\"90796\"," +
            "\"category_name\":\"catName\",\"supplier_name\":\"Прием данных {100500}\",\"supplier_type\":\"\"}";
        Assert.assertEquals(expectedSerialization, hitmanRequest);
    }

    @Test
    public void testResponseSerialization() throws IOException {
        PartnerSkuMappingResponse response = new PartnerSkuMappingResponse(
            1L, 100355223028L, "someWorkerId", MappingModerationStatus.ACCEPTED, 100357222784L
        );
        String serializedResponse = mapper.writeValueAsString(response);
        String expectedSerialization =
            "{\"req_id\":1,\"generated_sku_id\":100355223028,\"worker_id\":\"someWorkerId\",\"status\":\"ACCEPTED\"," +
                "\"msku\":100357222784}";
        Assert.assertEquals(expectedSerialization, serializedResponse);

        PartnerSkuMappingResponse responseCopy = mapper.readValue(serializedResponse, PartnerSkuMappingResponse.class);
        Assert.assertEquals(response, responseCopy);
    }

}

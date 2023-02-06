package ru.yandex.market.markup2.tasks.supplier_sku_mapping.partner_sku_validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.Comment;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.MappingModerationStatus;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;

import java.io.IOException;
import java.util.Collections;

/**
 * @author kravchenko-aa
 * @date 2019-09-20
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class PartnerSkuValidationSerializersTest {
    private ObjectMapper mapper;

    @Before
    public void setUp() {
        PartnerSkuValidationDataItemsProcessor processor = new PartnerSkuValidationDataItemsProcessor();
        mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(
                        new SimpleModule()
                                .addSerializer(TaskDataItem.class,
                                        (JsonSerializer<TaskDataItem>) processor.getRequestSerializer())
                );
    }

    @Test
    public void testPayloadSerialization() throws IOException {
        PartnerSkuValidationDataPayload payload = new PartnerSkuValidationDataPayload(
                new PartnerSkuValidationDataIdentity(100355223028L, 10035L, 100357222784L, 4242L),
                100500L);
        String expectedSerialization =
                "{\"dataIdentifier\":{\"partner_sku_id\":100355223028,\"partner_sku_category_id\":10035," +
                        "\"market_sku_id\":100357222784,\"market_sku_category_id\":4242}," +
                        "\"supplier_id\":100500}";
        String serializedPayload = mapper.writeValueAsString(payload);
        Assert.assertEquals(expectedSerialization, serializedPayload);

        PartnerSkuValidationDataPayload payloadCopy = mapper.readValue(
                expectedSerialization, PartnerSkuValidationDataPayload.class
        );
        Assert.assertEquals(payload.getSupplierId(), payloadCopy.getSupplierId());
        Assert.assertEquals(payload.getDataIdentifier(), payloadCopy.getDataIdentifier());
    }

    @Test
    public void testYangRequestSerialization() throws JsonProcessingException {
        PartnerSkuValidationDataPayload payload = new PartnerSkuValidationDataPayload(
                new PartnerSkuValidationDataIdentity(100355223028L, 10035L, 100357222784L, 4242L),
                100500L);

        TaskDataItem<PartnerSkuValidationDataPayload, Object> taskDataItem = new TaskDataItem<>(1L, payload);
        JsonNode jsonNode = mapper.valueToTree(taskDataItem);
        String yangRequest = mapper.writeValueAsString(jsonNode);
        String expectedSerialization =
                "{\"id\":\"1\",\"generated_sku_id\":\"100355223028\",\"category_id\":\"10035\"," +
                "\"supplier_mapping_info\":{\"sku_id\":\"100357222784\",\"category_id\":\"4242\"}," +
                "\"supplier_name\":\"Прием данных {100500}\",\"supplier_type\":\"\"}";
        Assert.assertEquals(expectedSerialization, yangRequest);
    }

    @Test
    public void testResponseSerialization() throws IOException {
        PartnerSkuValidationResponse response = new PartnerSkuValidationResponse(
            1L, 100355223028L, "someWorkerId", MappingModerationStatus.ACCEPTED, 100357222784L,
            Collections.singletonList(new Comment("COMMENT_TYPE", Collections.singletonList("ITEM1"))), false
        );
        String serializedResponse = mapper.writeValueAsString(response);
        String expectedSerialization =
                "{\"req_id\":1,\"generated_sku_id\":100355223028,\"worker_id\":\"someWorkerId\"," +
                        "\"status\":\"ACCEPTED\",\"msku\":100357222784," +
                    "\"content_comment\":[{\"type\":\"COMMENT_TYPE\",\"items\":[\"ITEM1\"]}],\"deleted\":false}";
        Assert.assertEquals(expectedSerialization, serializedResponse);

        PartnerSkuValidationResponse responseCopy =
                mapper.readValue(serializedResponse, PartnerSkuValidationResponse.class);
        Assert.assertEquals(response, responseCopy);
    }
}

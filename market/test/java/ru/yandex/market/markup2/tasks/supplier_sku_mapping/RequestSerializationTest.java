package ru.yandex.market.markup2.tasks.supplier_sku_mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.io.Resources;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;

import java.io.IOException;
import java.net.URL;

import static org.apache.commons.io.Charsets.UTF_8;

/**
 * @author galaev@yandex-team.ru
 * @since 15/01/2019.
 */
public class RequestSerializationTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        SupplierSkuMappingDataItemsProcessor processor = new SupplierSkuMappingDataItemsProcessor();
        objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(
                new SimpleModule()
                    .addSerializer(TaskDataItem.class, (JsonSerializer<TaskDataItem>) processor.getRequestSerializer())
            );
    }

    /**
     * Make sure all fields of AliasMaker.Offer serialized as strings
     * and are on the top level of json object.
     */
    @Test
    public void testRequestSerialization() throws IOException {
        SupplierOfferDataItemPayload payload = getTestPayload();
        String expectedHitmanRequest = getExpectedHitmanRequest();

        TaskDataItem<SupplierOfferDataItemPayload, Object> taskDataItem = new TaskDataItem<>(1L, payload);
        JsonNode jsonNode = objectMapper.valueToTree(taskDataItem);
        String hitmanRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);

        Assertions.assertThat(hitmanRequest).isEqualTo(expectedHitmanRequest);
    }

    private SupplierOfferDataItemPayload getTestPayload() throws IOException {
        URL jsonUrl = Resources.getResource("tasks/supplier_sku_mapping/supplier_sku_mapping_payload.json");
        return objectMapper.readValue(jsonUrl, SupplierOfferDataItemPayload.class);
    }

    private String getExpectedHitmanRequest() throws IOException {
        URL jsonUrl = Resources.getResource("tasks/supplier_sku_mapping/hitman_request.json");
        return Resources.toString(jsonUrl, UTF_8);
    }


}

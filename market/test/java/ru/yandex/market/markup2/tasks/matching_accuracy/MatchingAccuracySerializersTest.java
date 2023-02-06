package ru.yandex.market.markup2.tasks.matching_accuracy;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.markup2.utils.JsonUtils;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author york
 * @since 08.06.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MatchingAccuracySerializersTest {

    @Test
    public void testIdentitySerialization() throws IOException {
        ObjectMapper objectMapper = initSerializers(MatchingAccuracyTaskIdentity.class,
                new JsonUtils.DefaultJsonSerializer(),
                new JsonUtils.DefaultJsonDeserializer(MatchingAccuracyTaskIdentity.class)
        );
        MatchingAccuracyTaskIdentity ident = MatchingAccuracyTaskIdentity.of("offr", 10L);
        String val = objectMapper.writeValueAsString(ident);
        System.out.println(val);
        MatchingAccuracyTaskIdentity identCpy = objectMapper.readValue(val, MatchingAccuracyTaskIdentity.class);
        Assert.assertEquals(ident, identCpy);
    }

    @Test
    public void testAttributesSerialization() throws  IOException {
        ObjectMapper objectMapper = initSerializers(MatchingAccuracyDataAttributes.class,
            new JsonUtils.DefaultJsonSerializer(),
            new JsonUtils.DefaultJsonDeserializer(MatchingAccuracyDataAttributes.class)
        );
        MatchingAccuracyDataAttributes attributes = createAttributes();

        String val = objectMapper.writeValueAsString(attributes);
        System.out.println(val);
        MatchingAccuracyDataAttributes attributesCpy =
                objectMapper.readValue(val, MatchingAccuracyDataAttributes.class);
        compareAttributes(attributes, attributesCpy);
    }

    @Test
    public void testPayloadSerialization() throws  IOException {
        MatchingAccuracyPayload payload = new MatchingAccuracyPayload(
            1L,
            "offr",
            createAttributes()
        );
        ObjectMapper objectMapper = initSerializers(MatchingAccuracyPayload.class,
            new JsonUtils.DefaultJsonSerializer(),
            new JsonUtils.DefaultJsonDeserializer(MatchingAccuracyPayload.class)
        );
        String val = objectMapper.writeValueAsString(payload);
        System.out.println(val);
        MatchingAccuracyPayload payloadCpy = objectMapper.readValue(val, MatchingAccuracyPayload.class);
        Assert.assertEquals(payload.getDataIdentifier(), payloadCpy.getDataIdentifier());
        compareAttributes(payload.getAttributes(), payloadCpy.getAttributes());
    }


    @Test
    public void testRequestSerialization() throws  IOException {
        MatchingAccuracyPayload payload = new MatchingAccuracyPayload(
            1L,
            "offr",
            createAttributes()
        );
        TaskDataItem<MatchingAccuracyPayload, MatchingAccuracyResponse> item =
            new TaskDataItem(100L, payload);

        JsonSerializer<? super TaskDataItem<MatchingAccuracyPayload, MatchingAccuracyResponse>> ser =
            new MatchingAccuracyDataItemsProcessor(null).getRequestSerializer();
        ObjectMapper objectMapper = initSerializers(TaskDataItem.class,
            ser,
            null
        );
        String val = objectMapper.writeValueAsString(item);
        System.out.println(val);
        JsonNode tree = objectMapper.reader().readTree(val);
        List<String> keys = new ArrayList<>();
        tree.fields().forEachRemaining(e -> keys.add(e.getKey()));
        Assert.assertTrue(keys.containsAll(
            Arrays.asList("id", "instruction", "offer_name", "offer_image", "offer_descr", "offer_params",
                "model_name", "model_image", "model_descr")
        ));
    }

    @Test
    public void testResponseSerialization() throws  IOException {
        String responseFromHitman = "{\"req_id\":\"100\", \"result\":\"NODATA\"}";
        ObjectMapper objectMapper = initSerializers(MatchingAccuracyResponse.class,
            new JsonUtils.DefaultJsonSerializer(),
            new JsonUtils.DefaultJsonDeserializer(MatchingAccuracyResponse.class)
        );
        MatchingAccuracyResponse response = objectMapper.readValue(responseFromHitman, MatchingAccuracyResponse.class);
        Assert.assertEquals(response.getId(), 100);
        Assert.assertTrue(!response.hasResult());

        String val = objectMapper.writeValueAsString(response);
        MatchingAccuracyResponse responseCpy = objectMapper.readValue(val, MatchingAccuracyResponse.class);
    }

    @Test
    public void testMetricsSerialization() throws IOException {
        MatchingAccuracyMetricsData metrics = new MatchingAccuracyMetricsData(1, 1, 1, 1, 1, "types", 10);

        Class<MatchingAccuracyMetricsData> clazz = MatchingAccuracyMetricsData.class;
        ObjectMapper objectMapper = initSerializers(clazz,
            new JsonUtils.DefaultJsonSerializer(), new JsonUtils.DefaultJsonDeserializer(clazz));

        String value = objectMapper.writeValueAsString(metrics);
        System.out.println(value);
        MatchingAccuracyMetricsData metricsCopy = objectMapper.readValue(value, clazz);

        Assert.assertEquals(metrics.getCategoryId(), metricsCopy.getCategoryId());
        Assert.assertEquals(metrics.getResponsesCount(), metricsCopy.getResponsesCount());
        Assert.assertEquals(metrics.getCorrectCount(), metricsCopy.getCorrectCount());
        Assert.assertEquals(metrics.getTotalCount(), metricsCopy.getTotalCount());
        Assert.assertEquals(metrics.getCorrectPercentage(), metricsCopy.getCorrectPercentage(), 0);
        Assert.assertEquals(metrics.getCardTypes(), metricsCopy.getCardTypes());
        Assert.assertEquals(metrics.getNoData(), metricsCopy.getNoData());
    }

    private MatchingAccuracyDataAttributes createAttributes() {
        return MatchingAccuracyDataAttributes.newBuilder()
            .setOfferPicUrl("picturl")
            .setOfferParams("params")
            .setCardTitle("card title")
            .setCardDescription("card descr")
            .setCardPicUrl("card ipc")
            .setOfferDescription("offer desc")
            .setOfferTitle("tit")
            .setInstruction("intsr")
            .setCategoryName("categoryName")
            .setCategoryId("categoryId")
            .build();
    }


    private ObjectMapper initSerializers(Class clazz,
                                             JsonSerializer serializer,
                                             JsonDeserializer deserializer) {
        final ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule serializerModule = new SimpleModule()
            .addSerializer(clazz, serializer);
        if (deserializer != null) {
            serializerModule.addDeserializer(clazz, deserializer);
        }
        objectMapper.registerModule(serializerModule);
        return objectMapper;
    }

    private void compareAttributes(MatchingAccuracyDataAttributes attributes,
                                   MatchingAccuracyDataAttributes attributesCpy) {
        Assert.assertEquals(attributes.getCardDescription(), attributesCpy.getCardDescription());
        Assert.assertEquals(attributes.getCardPicUrl(), attributesCpy.getCardPicUrl());
        Assert.assertEquals(attributes.getCardTitle(), attributesCpy.getCardTitle());
        Assert.assertEquals(attributes.getInstruction(), attributesCpy.getInstruction());
        Assert.assertEquals(attributes.getOfferDescription(), attributesCpy.getOfferDescription());
        Assert.assertEquals(attributes.getOfferParams(), attributesCpy.getOfferParams());
        Assert.assertEquals(attributes.getOfferPicUrl(), attributesCpy.getOfferPicUrl());
        Assert.assertEquals(attributes.getOfferTitle(), attributesCpy.getOfferTitle());
        Assert.assertEquals(attributes.getCategoryName(), attributesCpy.getCategoryName());
        Assert.assertEquals(attributes.getCategoryId(), attributesCpy.getCategoryId());
    }
}

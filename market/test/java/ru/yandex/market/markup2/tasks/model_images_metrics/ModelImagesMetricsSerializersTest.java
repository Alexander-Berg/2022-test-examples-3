package ru.yandex.market.markup2.tasks.model_images_metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.markup2.utils.Markup2TestUtils;
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
public class ModelImagesMetricsSerializersTest {
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        JsonSerializer serializer = new ModelImagesMetricsHitmanDataProcessor(null).getRequestSerializer();
        objectMapper = Markup2TestUtils.defaultMapper(TaskDataItem.class, serializer, null,
                ModelImagesMetricsTaskIdentity.class, ModelImagesMetricsDataAttributes.class,
                ModelImagesMetricsTaskPayload.class, ModelImagesMetricsHitmanResponse.class);
    }

    @Test
    public void testIdentitySerialization() throws IOException {
        ModelImagesMetricsTaskIdentity ident = new ModelImagesMetricsTaskIdentity(100000L, "rul");
        String val = objectMapper.writeValueAsString(ident);
        System.out.println(val);
        ModelImagesMetricsTaskIdentity identCpy = objectMapper.readValue(val, ModelImagesMetricsTaskIdentity.class);
        Assert.assertEquals(ident, identCpy);
    }

    @Test
    public void testAttributesSerialization() throws  IOException {
        ModelImagesMetricsDataAttributes attributes = createAttributes();

        String val = objectMapper.writeValueAsString(attributes);
        System.out.println(val);
        ModelImagesMetricsDataAttributes attributesCpy =
                objectMapper.readValue(val, ModelImagesMetricsDataAttributes.class);
        Assert.assertEquals(attributes.getCategoryId(), attributesCpy.getCategoryId());
        Assert.assertEquals(attributes.getCategoryName(), attributesCpy.getCategoryName());
        Assert.assertEquals(attributes.getModelName(), attributesCpy.getModelName());
        Assert.assertEquals(attributes.getModelUrl(), attributesCpy.getModelUrl());
        Assert.assertEquals(attributes.getPicUrl(), attributesCpy.getPicUrl());
    }

    @Test
    public void testPayloadSerialization() throws  IOException {
        ModelImagesMetricsTaskPayload payload = new ModelImagesMetricsTaskPayload(
            new ModelImagesMetricsTaskIdentity(10000L, "url"),
            createAttributes()
        );
        String val = objectMapper.writeValueAsString(payload);
        System.out.println(val);
        ModelImagesMetricsTaskPayload payloadCpy = objectMapper.readValue(val, ModelImagesMetricsTaskPayload.class);
        Assert.assertEquals(payload.getDataIdentifier(), payloadCpy.getDataIdentifier());
    }

    @Test
    public void testRequestSerialization() throws  IOException {
        ModelImagesMetricsTaskPayload payload = new ModelImagesMetricsTaskPayload(
            new ModelImagesMetricsTaskIdentity(10000L, "picur"),
            createAttributes()
        );
        TaskDataItem<ModelImagesMetricsTaskPayload, ModelImagesMetricsHitmanResponse> item =
            new TaskDataItem<>(100L, payload);

        String val = objectMapper.writeValueAsString(item);
        System.out.println(val);
        JsonNode tree = objectMapper.reader().readTree(val);
        List<String> keys = new ArrayList<>();
        tree.fields().forEachRemaining(e -> keys.add(e.getKey()));
        Assert.assertTrue(keys.containsAll(
            Arrays.asList("id", "category_id", "category_name", "model_id", "model_url", "pic_url", "model_name")
        ));
    }

    @Test
    public void testResponseSerialization() throws  IOException {
        String responseFromHitman =  Resources.toString(
            Resources.getResource("model_images_metrics_response.json"), Charsets.UTF_8);

        ModelImagesMetricsHitmanResponse response = objectMapper.readValue(responseFromHitman,
            ModelImagesMetricsHitmanResponse.class);

        checkResponse(response);
        checkResponse(
                objectMapper.readValue(
                        objectMapper.writeValueAsString(response),
                        ModelImagesMetricsHitmanResponse.class)
        );
    }

    @Test
    public void testResponseSerializationV2() throws  IOException {
        String responseFromHitman =  Resources.toString(
            Resources.getResource("model_images_metrics_v2_response.json"), Charsets.UTF_8);

        ModelImagesMetricsHitmanResponse response = objectMapper.readValue(responseFromHitman,
            ModelImagesMetricsHitmanResponse.class);

        checkResponse(response);
        checkResponse(
                objectMapper.readValue(
                        objectMapper.writeValueAsString(response),
                        ModelImagesMetricsHitmanResponse.class)
        );
    }

    private void checkResponse(ModelImagesMetricsHitmanResponse response) {
        Assert.assertEquals(response.getId(), 1L);
        Assert.assertEquals((long) response.getModelId(), 107530L);
        Assert.assertTrue(response.isCropped());
        Assert.assertTrue(response.isBlur());
        Assert.assertFalse(response.isMulti());
        Assert.assertFalse(response.isWatermark());
        Assert.assertFalse(response.isRelevance());
    }

    private ModelImagesMetricsDataAttributes createAttributes() {
        return ModelImagesMetricsDataAttributes.Builder.newBuilder()
            .setCategoryId(1)
            .setCategoryName("categoryName")
            .setModelName("modelName")
            .setModelUrl("modelUrl")
            .setPicUrl("picurl")
            .build();
    }

}

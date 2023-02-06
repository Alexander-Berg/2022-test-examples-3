package ru.yandex.market.markup2.tasks.model_hand_images;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Assert;
import org.junit.Before;
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
public class ModelHandImagesSerializersTest {

    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        objectMapper = new ObjectMapper();
        ModelHandImageHitmanDataProcessor proc =
            new ModelHandImageHitmanDataProcessor(null, null);
        JsonSerializer reqSerializer = proc.getRequestSerializer();

        SimpleModule serializerModule = new SimpleModule()
            .addSerializer(ModelHandImageTaskIdentity.class, new JsonUtils.DefaultJsonSerializer())
            .addDeserializer(ModelHandImageTaskIdentity.class,
                new JsonUtils.DefaultJsonDeserializer(ModelHandImageTaskIdentity.class))
            .addSerializer(ModelHandImageDataAttributes.class, new JsonUtils.DefaultJsonSerializer())
            .addDeserializer(ModelHandImageDataAttributes.class,
                new JsonUtils.DefaultJsonDeserializer(ModelHandImageDataAttributes.class))
            .addSerializer(ModelHandImageTaskPayload.class, new JsonUtils.DefaultJsonSerializer())
            .addDeserializer(ModelHandImageTaskPayload.class,
                new JsonUtils.DefaultJsonDeserializer(ModelHandImageTaskPayload.class))
            .addSerializer(TaskDataItem.class, reqSerializer)
            .addSerializer(ModelHandImageResponse.class, new JsonUtils.DefaultJsonSerializer())
            .addDeserializer(ModelHandImageResponse.class, proc.getResponseDeserializer());
        objectMapper.registerModule(serializerModule);
    }

    @Test
    public void testIdentitySerialization() throws IOException {
        ModelHandImageTaskIdentity ident = new ModelHandImageTaskIdentity(100000L);
        String val = objectMapper.writeValueAsString(ident);
        System.out.println(val);
        ModelHandImageTaskIdentity identCpy = objectMapper.readValue(val, ModelHandImageTaskIdentity.class);
        Assert.assertEquals(ident, identCpy);
    }

    @Test
    public void testAttributesSerialization() throws  IOException {
        ModelHandImageDataAttributes attributes = createAttributes();

        String val = objectMapper.writeValueAsString(attributes);
        System.out.println(val);
        ModelHandImageDataAttributes attributesCpy = objectMapper.readValue(val, ModelHandImageDataAttributes.class);
        Assert.assertEquals(attributes.getCategoryName(), attributesCpy.getCategoryName());
        Assert.assertEquals(attributes.getModelName(), attributesCpy.getModelName());
        Assert.assertEquals(attributes.getShopUrl(), attributesCpy.getShopUrl());
        Assert.assertEquals(attributes.getVendorId(), attributesCpy.getVendorId());
        Assert.assertEquals(attributes.getVendorName(), attributesCpy.getVendorName());
        Assert.assertEquals(attributes.getVendorUrl(), attributesCpy.getVendorUrl());
    }

    @Test
    public void testPayloadSerialization() throws  IOException {
        ModelHandImageTaskPayload payload = new ModelHandImageTaskPayload(
            new ModelHandImageTaskIdentity(10000L),
            createAttributes()
        );
        String val = objectMapper.writeValueAsString(payload);
        System.out.println(val);
        ModelHandImageTaskPayload payloadCpy = objectMapper.readValue(val, ModelHandImageTaskPayload.class);
        Assert.assertEquals(payload.getDataIdentifier(), payloadCpy.getDataIdentifier());
    }

    @Test
    public void testRequestSerialization() throws  IOException {
        ModelHandImageTaskPayload payload = new ModelHandImageTaskPayload(
            new ModelHandImageTaskIdentity(10000L),
            createAttributes()
        );
        TaskDataItem<ModelHandImageTaskPayload, ModelHandImageResponse> item =
            new TaskDataItem(100L, payload);

        String val = objectMapper.writeValueAsString(item);
        System.out.println(val);
        JsonNode tree = objectMapper.reader().readTree(val);
        List<String> keys = new ArrayList<>();
        tree.fields().forEachRemaining(e -> keys.add(e.getKey()));
        List<String> requiredFields = new ArrayList<>(
            Arrays.asList("id", "model_id", "shop_url", "vendor_id", "model_name", "vendor_url",
            "category_id", "vendor_name", "category_name"));
        requiredFields.removeAll(keys);

        Assert.assertEquals("Absent " + requiredFields.toString(), 0, requiredFields.size());
    }

    @Test
    public void testResponseSerialization() throws  IOException {
        String responseFromHitman = "{\"req_id\":\"100\", \"photo\":\"http://wwww.hse\"}";
        ModelHandImageResponse response = objectMapper.readValue(responseFromHitman,
            ModelHandImageResponse.class);
        Assert.assertEquals(response.getReqId(), 100);
        Assert.assertTrue(response.hasResult());

        String val = objectMapper.writeValueAsString(response);
        ModelHandImageResponse responseCpy = objectMapper.readValue(val, ModelHandImageResponse.class);
        Assert.assertEquals(response.getId(), responseCpy.getId());
        Assert.assertEquals(response.getBestPhoto(), responseCpy.getBestPhoto());
    }

    private ModelHandImageDataAttributes createAttributes() {
        return ModelHandImageDataAttributes.Builder.newBuilder()
            .setCategoryId(100)
            .setModelName("model name")
            .setCategoryName("categoryName")
            .setVendorName("Vemndor")
            .setVendorUrl("vendor url")
            .setShopUrl("shop url")
            .setVendorId(101L)
            .build();
    }

}

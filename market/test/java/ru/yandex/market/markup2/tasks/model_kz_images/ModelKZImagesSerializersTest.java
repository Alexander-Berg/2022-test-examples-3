package ru.yandex.market.markup2.tasks.model_kz_images;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.markup2.tasks.model_images.ImagesFromOffers;
import ru.yandex.market.markup2.utils.JsonUtils;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * @author york
 * @since 08.06.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelKZImagesSerializersTest {
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        objectMapper = new ObjectMapper();
        ModelKZImageHitmanDataProcessor proc =
            new ModelKZImageHitmanDataProcessor(null, null);
        JsonSerializer reqSerializer = proc.getRequestSerializer();

        SimpleModule serializerModule = new SimpleModule()
            .addSerializer(ModelKZImageTaskIdentity.class, new JsonUtils.DefaultJsonSerializer())
            .addDeserializer(ModelKZImageTaskIdentity.class,
                new JsonUtils.DefaultJsonDeserializer(ModelKZImageTaskIdentity.class))
            .addSerializer(ModelKZImageDataAttributes.class, new JsonUtils.DefaultJsonSerializer())
            .addDeserializer(ModelKZImageDataAttributes.class,
                new JsonUtils.DefaultJsonDeserializer(ModelKZImageDataAttributes.class))
            .addSerializer(ModelKZImageTaskPayload.class, new JsonUtils.DefaultJsonSerializer())
            .addDeserializer(ModelKZImageTaskPayload.class,
                new JsonUtils.DefaultJsonDeserializer(ModelKZImageTaskPayload.class))
            .addSerializer(TaskDataItem.class, reqSerializer)
            .addSerializer(ModelKZImageResponse.class, new JsonUtils.DefaultJsonSerializer())
            .addDeserializer(ModelKZImageResponse.class, proc.getResponseDeserializer());
        objectMapper.registerModule(serializerModule);
    }

    @Test
    public void testIdentitySerialization() throws IOException {
        ModelKZImageTaskIdentity ident = new ModelKZImageTaskIdentity(100000L);
        String val = objectMapper.writeValueAsString(ident);
        System.out.println(val);
        ModelKZImageTaskIdentity identCpy = objectMapper.readValue(val, ModelKZImageTaskIdentity.class);
        Assert.assertEquals(ident, identCpy);
    }

    @Test
    public void testAttributesSerialization() throws  IOException {
        ModelKZImageDataAttributes attributes = createAttributes();

        String val = objectMapper.writeValueAsString(attributes);
        System.out.println(val);
        ModelKZImageDataAttributes attributesCpy = objectMapper.readValue(val, ModelKZImageDataAttributes.class);
        Assert.assertEquals(attributes.getCategoryName(), attributesCpy.getCategoryName());
        Assert.assertEquals(attributes.getModelName(), attributesCpy.getModelName());
        Assert.assertEquals(attributes.getVendor(), attributesCpy.getVendor());
        Assert.assertEquals(attributes.getOfferImages(), attributesCpy.getOfferImages());
    }

    @Test
    public void testPayloadSerialization() throws  IOException {
        ModelKZImageTaskPayload payload = new ModelKZImageTaskPayload(
            new ModelKZImageTaskIdentity(10000L),
            createAttributes()
        );
        String val = objectMapper.writeValueAsString(payload);
        System.out.println(val);
        ModelKZImageTaskPayload payloadCpy = objectMapper.readValue(val, ModelKZImageTaskPayload.class);
        Assert.assertEquals(payload.getDataIdentifier(), payloadCpy.getDataIdentifier());
    }

    @Test
    public void testRequestSerialization() throws  IOException {
        ModelKZImageTaskPayload payload = new ModelKZImageTaskPayload(
            new ModelKZImageTaskIdentity(10000L),
            createAttributes()
        );
        TaskDataItem<ModelKZImageTaskPayload, ModelKZImageResponse> item =
            new TaskDataItem(100L, payload);

        String val = objectMapper.writeValueAsString(item);
        System.out.println(val);
        JsonNode tree = objectMapper.reader().readTree(val);
        List<String> keys = new ArrayList<>();
        tree.fields().forEachRemaining(e -> keys.add(e.getKey()));
        Assert.assertTrue(keys.containsAll(
            Arrays.asList("id", "category_name", "model_id", "vendor", "model_name", "offer_images")
        ));
    }

    @Test
    public void testResponseSerialization() throws  IOException {
        String responseFromHitman =  Resources.toString(
            Resources.getResource("model_image_hitman_response.json"), Charsets.UTF_8);

        ModelKZImageResponse response = objectMapper.readValue(responseFromHitman,
            ModelKZImageResponse.class);
        Assert.assertEquals(response.getReqId(), 100);
        Assert.assertTrue(response.hasResult());

        String val = objectMapper.writeValueAsString(response);
        ModelKZImageResponse responseCpy = objectMapper.readValue(val, ModelKZImageResponse.class);
        Assert.assertEquals(response.getId(), responseCpy.getId());
        Assert.assertEquals(response.getBestPhoto(), responseCpy.getBestPhoto());
        Assert.assertEquals(response.getUrlsOk(), responseCpy.getUrlsOk());
        Assert.assertEquals(response.getUrlsPerfect(), responseCpy.getUrlsPerfect());
    }

    private ModelKZImageDataAttributes createAttributes() {
        return ModelKZImageDataAttributes.Builder.newBuilder()
            .setCategoryName("categoryName")
            .setModelName("modelName")
            .setVendor("Vemndor")
            .setOfferImages(new HashSet<>(
                Arrays.asList(
                    new ImagesFromOffers.OfferImage("offr1", "img1"),
                    new ImagesFromOffers.OfferImage("offr2", "img2"),
                    new ImagesFromOffers.OfferImage("offr3", "img3")
                )
            ))
            .build();
    }

}

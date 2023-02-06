package ru.yandex.market.markup2.tasks.model_more_images;

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
public class ModelMoreImagesSerializersTest {
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        objectMapper = new ObjectMapper();
        ModelMoreImagesHitmanProcessor proc = new ModelMoreImagesHitmanProcessor(null, null);
        JsonSerializer reqSerializer = proc.getRequestSerializer();

        SimpleModule serializerModule = new SimpleModule()
            .addSerializer(ModelMoreImagesTaskIdentity.class, new JsonUtils.DefaultJsonSerializer())
            .addDeserializer(ModelMoreImagesTaskIdentity.class,
                new JsonUtils.DefaultJsonDeserializer(ModelMoreImagesTaskIdentity.class))
            .addSerializer(ModelMoreImagesDataAttributes.class, new JsonUtils.DefaultJsonSerializer())
            .addDeserializer(ModelMoreImagesDataAttributes.class,
                new JsonUtils.DefaultJsonDeserializer(ModelMoreImagesDataAttributes.class))
            .addSerializer(ModelMoreImagesTaskPayload.class, new JsonUtils.DefaultJsonSerializer())
            .addDeserializer(ModelMoreImagesTaskPayload.class,
                new JsonUtils.DefaultJsonDeserializer(ModelMoreImagesTaskPayload.class))
            .addSerializer(TaskDataItem.class, reqSerializer)
            .addSerializer(ModelMoreImagesResponse.class, new JsonUtils.DefaultJsonSerializer())
            .addDeserializer(ModelMoreImagesResponse.class, proc.getResponseDeserializer());
        objectMapper.registerModule(serializerModule);
    }

    @Test
    public void testIdentitySerialization() throws IOException {
        ModelMoreImagesTaskIdentity ident = new ModelMoreImagesTaskIdentity(100000L);
        String val = objectMapper.writeValueAsString(ident);
        System.out.println(val);
        ModelMoreImagesTaskIdentity identCpy = objectMapper.readValue(val, ModelMoreImagesTaskIdentity.class);
        Assert.assertEquals(ident, identCpy);
    }

    @Test
    public void testAttributesSerialization() throws  IOException {
        ModelMoreImagesDataAttributes attributes = createAttributes();

        String val = objectMapper.writeValueAsString(attributes);
        System.out.println(val);
        ModelMoreImagesDataAttributes attributesCpy = objectMapper.readValue(val, ModelMoreImagesDataAttributes.class);
        Assert.assertEquals(attributes.getCategoryName(), attributesCpy.getCategoryName());
        Assert.assertEquals(attributes.getModelName(), attributesCpy.getModelName());
        Assert.assertEquals(attributes.getVendor(), attributesCpy.getVendor());
        Assert.assertEquals(attributes.getOfferImages(), attributesCpy.getOfferImages());
        Assert.assertEquals(attributes.getModelUrl(), attributesCpy.getModelUrl());
        Assert.assertEquals(new ArrayList<>(attributes.getModelImages()),
            new ArrayList<>(attributesCpy.getModelImages()));
    }

    @Test
    public void testPayloadSerialization() throws  IOException {
        ModelMoreImagesTaskPayload payload = new ModelMoreImagesTaskPayload(
            new ModelMoreImagesTaskIdentity(10000L),
            createAttributes()
        );
        String val = objectMapper.writeValueAsString(payload);
        System.out.println(val);
        ModelMoreImagesTaskPayload payloadCpy = objectMapper.readValue(val, ModelMoreImagesTaskPayload.class);
        Assert.assertEquals(payload.getDataIdentifier(), payloadCpy.getDataIdentifier());
    }

    @Test
    public void testRequestSerialization() throws  IOException {
        ModelMoreImagesTaskPayload payload = new ModelMoreImagesTaskPayload(
            new ModelMoreImagesTaskIdentity(10000L),
            createAttributes()
        );
        TaskDataItem<ModelMoreImagesTaskPayload, ModelMoreImagesResponse> item =
            new TaskDataItem(100L, payload);

        String val = objectMapper.writeValueAsString(item);
        System.out.println(val);
        JsonNode tree = objectMapper.reader().readTree(val);
        List<String> keys = new ArrayList<>();
        tree.fields().forEachRemaining(e -> keys.add(e.getKey()));
        Assert.assertTrue(keys.containsAll(
            Arrays.asList("id", "category_name", "model_id", "vendor", "model_name", "offer_images", "model_url",
                "model_images")
        ));
    }

    @Test
    public void testResponseSerialization() throws  IOException {
        String responseFromHitman =  Resources.toString(
            Resources.getResource("model_image_hitman_response.json"), Charsets.UTF_8);

        ModelMoreImagesResponse response = objectMapper.readValue(responseFromHitman,
            ModelMoreImagesResponse.class);
        Assert.assertEquals(response.getReqId(), 100);
        Assert.assertTrue(response.hasResult());

        String val = objectMapper.writeValueAsString(response);
        ModelMoreImagesResponse responseCpy = objectMapper.readValue(val, ModelMoreImagesResponse.class);
        Assert.assertEquals(response.getId(), responseCpy.getId());
        Assert.assertEquals(response.getBestPhoto(), responseCpy.getBestPhoto());
        Assert.assertEquals(response.getUrlsOk(), responseCpy.getUrlsOk());
        Assert.assertEquals(response.getUrlsPerfect(), responseCpy.getUrlsPerfect());
    }

    private ModelMoreImagesDataAttributes createAttributes() {
        return ModelMoreImagesDataAttributes.Builder.newBuilder()
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
            .setModelUrl("Model url")
            .setModelImages(Arrays.asList("IUmg1", "Img2"))
            .build();
    }

}

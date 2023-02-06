package ru.yandex.market.markup2.tasks.model_images;

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
import java.util.HashSet;
import java.util.List;

/**
 * @author york
 * @since 08.06.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelImageSerializersTest {
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        ModelImageHitmanDataProcessor proc =
            new ModelImageHitmanDataProcessor(null, null);
        JsonSerializer reqSerializer = proc.getRequestSerializer();

        objectMapper = Markup2TestUtils.defaultMapper(
                TaskDataItem.class, reqSerializer, null,
                ModelImageResponse.class,
                ModelImageTaskIdentity.class,
                ModelImageDataAttributes.class,
                ModelImageTaskPayload.class
        );
    }

    @Test
    public void testIdentitySerialization() throws IOException {
        ModelImageTaskIdentity ident = new ModelImageTaskIdentity(100000L);
        String val = objectMapper.writeValueAsString(ident);
        System.out.println(val);
        ModelImageTaskIdentity identCpy = objectMapper.readValue(val, ModelImageTaskIdentity.class);
        Assert.assertEquals(ident, identCpy);
    }

    @Test
    public void testAttributesSerialization() throws  IOException {
        ModelImageDataAttributes attributes = createAttributes();

        String val = objectMapper.writeValueAsString(attributes);
        System.out.println(val);
        ModelImageDataAttributes attributesCpy = objectMapper.readValue(val, ModelImageDataAttributes.class);
        Assert.assertEquals(attributes.getCategoryName(), attributesCpy.getCategoryName());
        Assert.assertEquals(attributes.getModelName(), attributesCpy.getModelName());
        Assert.assertEquals(attributes.getModelUrl(), attributesCpy.getModelUrl());
        Assert.assertEquals(attributes.getVendor(), attributesCpy.getVendor());
        Assert.assertEquals(attributes.getOfferImages(), attributesCpy.getOfferImages());
    }

    @Test
    public void testPayloadSerialization() throws  IOException {
        ModelImageTaskPayload payload = new ModelImageTaskPayload(
            new ModelImageTaskIdentity(10000L),
            createAttributes()
        );
        String val = objectMapper.writeValueAsString(payload);
        System.out.println(val);
        ModelImageTaskPayload payloadCpy = objectMapper.readValue(val, ModelImageTaskPayload.class);
        Assert.assertEquals(payload.getDataIdentifier(), payloadCpy.getDataIdentifier());
    }

    @Test
    public void testRequestSerialization() throws  IOException {
        ModelImageTaskPayload payload = new ModelImageTaskPayload(
            new ModelImageTaskIdentity(10000L),
            createAttributes()
        );
        TaskDataItem<ModelImageTaskPayload, ModelImageResponse> item =
            new TaskDataItem(100L, payload);

        String val = objectMapper.writeValueAsString(item);
        System.out.println(val);
        JsonNode tree = objectMapper.reader().readTree(val);
        List<String> keys = new ArrayList<>();
        tree.fields().forEachRemaining(e -> keys.add(e.getKey()));
        Assert.assertTrue(keys.containsAll(
            Arrays.asList("id", "category_name", "model_id", "model_url", "vendor", "model_name", "offer_images")
        ));
    }

    @Test
    public void testResponseSerialization() throws  IOException {
        String responseFromHitman =  Resources.toString(
            Resources.getResource("model_image_hitman_response.json"), Charsets.UTF_8);

        ModelImageResponse response = objectMapper.readValue(responseFromHitman, ModelImageResponse.class);
        Assert.assertEquals(response.getReqId(), 100);
        Assert.assertTrue(response.hasResult());

        String val = objectMapper.writeValueAsString(response);
        ModelImageResponse responseCpy = objectMapper.readValue(val, ModelImageResponse.class);
        Assert.assertEquals(response.getId(), responseCpy.getId());
        Assert.assertEquals(response.getBestPhoto(), responseCpy.getBestPhoto());
        Assert.assertEquals(response.getUrlsOk(), responseCpy.getUrlsOk());
        Assert.assertEquals(response.getUrlsPerfect(), responseCpy.getUrlsPerfect());
    }

    private ModelImageDataAttributes createAttributes() {
        return ModelImageDataAttributes.Builder.newBuilder()
            .setCategoryName("categoryName")
            .setModelName("modelName")
            .setModelUrl("modelUrl")
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

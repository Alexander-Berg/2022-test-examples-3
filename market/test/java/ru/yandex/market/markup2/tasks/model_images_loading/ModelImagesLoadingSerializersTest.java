package ru.yandex.market.markup2.tasks.model_images_loading;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.markup2.tasks.model_images.ModelBaseImageResponse;

import java.io.IOException;
import java.util.Collections;

/**
 * @author galaev@yandex-team.ru
 * @since 04/08/2017.
 */
public class ModelImagesLoadingSerializersTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testIdentitySerialization() throws IOException {
        ModelImagesLoadingTaskIdentity identity = new ModelImagesLoadingTaskIdentity(1);

        Class<ModelImagesLoadingTaskIdentity> clazz = ModelImagesLoadingTaskIdentity.class;

        String value = objectMapper.writeValueAsString(identity);
        System.out.println(value);
        ModelImagesLoadingTaskIdentity identityCopy = objectMapper.readValue(value, clazz);

        Assert.assertEquals(identity.getModelId(), identityCopy.getModelId());
    }

    @Test
    public void testAttributesSerialization() throws IOException {
        ModelImagesLoadingDataAttributes attributes = createAttributes();

        Class<ModelImagesLoadingDataAttributes> clazz = ModelImagesLoadingDataAttributes.class;

        String value = objectMapper.writeValueAsString(attributes);
        System.out.println(value);
        ModelImagesLoadingDataAttributes attributesCopy = objectMapper.readValue(value, clazz);

        compareAttributes(attributes, attributesCopy);
    }

    @Test
    public void testPayloadSerialization() throws IOException {
        ModelImagesLoadingTaskPayload payload = new ModelImagesLoadingTaskPayload(
            new ModelImagesLoadingTaskIdentity(1), createAttributes());

        Class<ModelImagesLoadingTaskPayload> clazz = ModelImagesLoadingTaskPayload.class;

        String value = objectMapper.writeValueAsString(payload);
        System.out.println(value);
        ModelImagesLoadingTaskPayload payloadCopy = objectMapper.readValue(value, clazz);

        Assert.assertEquals(payload.getDataIdentifier(), payloadCopy.getDataIdentifier());
        compareAttributes(payload.getAttributes(), payloadCopy.getAttributes());
    }

    @Test
    public void testResponseSerialization() throws IOException {
        ModelImagesLoadingDataAttributes attributes = createAttributes();
        ModelImagesLoadingResponse response = new ModelImagesLoadingResponse(1, 1,
            attributes.getUrlsOk(), attributes.getUrlsPerfect(), attributes.getBestPhoto());

        Class<ModelImagesLoadingResponse> clazz = ModelImagesLoadingResponse.class;

        String value = objectMapper.writeValueAsString(response);
        System.out.println(value);
        ModelImagesLoadingResponse responseCopy = objectMapper.readValue(value, clazz);

        Assert.assertEquals(response.getId(), responseCopy.getId());
        Assert.assertEquals(response.getReqId(), responseCopy.getReqId());
        Assert.assertEquals(response.getModelId(), responseCopy.getModelId());
        Assert.assertEquals(response.getUrlsOk(), responseCopy.getUrlsOk());
        Assert.assertEquals(response.getUrlsPerfect(), responseCopy.getUrlsPerfect());
        Assert.assertEquals(response.getBestPhoto(), responseCopy.getBestPhoto());
    }

    private ModelImagesLoadingDataAttributes createAttributes() {
        return new ModelImagesLoadingDataAttributes(
            Collections.singleton(new ModelBaseImageResponse.Image("url-ok-mds", "url-ok-original")),
            Collections.singleton(new ModelBaseImageResponse.Image("url-perfect-mds", "url-perfect-original")),
            new ModelBaseImageResponse.Image("url-best-mds", "url-best-original"));
    }

    private void compareAttributes(ModelImagesLoadingDataAttributes attributes,
                                   ModelImagesLoadingDataAttributes attributesCopy) {
        Assert.assertEquals(attributes.getUrlsOk(), attributesCopy.getUrlsOk());
        Assert.assertEquals(attributes.getUrlsPerfect(), attributesCopy.getUrlsPerfect());
        Assert.assertEquals(attributes.getBestPhoto(), attributesCopy.getBestPhoto());
    }
}

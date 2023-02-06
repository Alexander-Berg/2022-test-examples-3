package ru.yandex.market.markup2.tasks.vendor_sku_relevance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.markup2.utils.TaskDataItemPayloadRandomizer;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author galaev@yandex-team.ru
 * @since 08/02/2018.
 */
public class VendorSkuRelevanceSerializersTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final long RANDOM_SEED = 10312L;

    private EnhancedRandom enhancedRandom;

    @Before
    public void before() {
        enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(RANDOM_SEED)
            .randomize(
                TaskDataItemPayloadRandomizer.field(),
                TaskDataItemPayloadRandomizer.create(VendorSkuRelevanceIdentity.class, RANDOM_SEED)
            )
            .build();
    }

    @Test
    public void testIdentitySerialization() throws IOException {
        Class<VendorSkuRelevanceIdentity> clazz = VendorSkuRelevanceIdentity.class;
        VendorSkuRelevanceIdentity identity = enhancedRandom.nextObject(clazz);

        VendorSkuRelevanceIdentity identityCopy = encodeDecode(identity, clazz);

        assertThat(identityCopy.getCategoryId()).isEqualTo(identity.getCategoryId());
        assertThat(identityCopy.getSkuId()).isEqualTo(identity.getSkuId());
        assertThat(identityCopy.equals(identity)).isTrue();
    }

    @Test
    public void testAttributesSerialization() throws IOException {
        Class<VendorSkuRelevanceAttributes> clazz = VendorSkuRelevanceAttributes.class;
        VendorSkuRelevanceAttributes attributes = enhancedRandom.nextObject(clazz);

        VendorSkuRelevanceAttributes attributesCopy = encodeDecode(attributes, clazz);

        assertThat(attributesCopy).isEqualToComparingFieldByField(attributes);
    }

    @Test
    public void testPayloadSerialization() throws IOException {
        Class<VendorSkuRelevancePayload> clazz = VendorSkuRelevancePayload.class;
        VendorSkuRelevancePayload payload = enhancedRandom.nextObject(VendorSkuRelevancePayload.class);

        VendorSkuRelevancePayload payloadCopy = encodeDecode(payload, clazz);

        assertThat(payload.getDataIdentifier()).isEqualTo(payloadCopy.getDataIdentifier());
        assertThat(payloadCopy.getAttributes()).isEqualToComparingFieldByField(payload.getAttributes());
    }

    @Test
    public void testResponseSerialization() throws IOException {
        Class<VendorSkuRelevanceResponse> clazz = VendorSkuRelevanceResponse.class;
        VendorSkuRelevanceResponse response = enhancedRandom.nextObject(clazz);

        VendorSkuRelevanceResponse responseCopy = encodeDecode(response, clazz);

        Assert.assertEquals(response.getImageResponses(), responseCopy.getImageResponses());
        Assert.assertEquals(response.getParameterResponses(), responseCopy.getParameterResponses());
        Assert.assertEquals(response.isRelevant(), responseCopy.isRelevant());
        Assert.assertEquals(response.isWrongCategory(), responseCopy.isWrongCategory());
    }

    private <T> T encodeDecode(T value, Class<T> clazz) throws IOException {
        String serialized = objectMapper.writeValueAsString(value);
        return objectMapper.readValue(serialized, clazz);
    }

}

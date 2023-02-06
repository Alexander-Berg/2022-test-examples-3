package ru.yandex.market.markup2.tasks.logs_processing_old;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.markup2.utils.Markup2TestUtils;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 22.06.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class LogsProcessingSerializersTest {

    private static final String OFFER_ID = "offer_id";
    private static final Long CLUSTER_ID = 1L;

    private ObjectMapper mapper;

    @Before
    public void setup() {
        mapper = Markup2TestUtils.defaultMapper(LogsProcessingDataItemPayload.class, LogsProcessingResponse.class);
    }

    @Test
    public void requestSerialization() throws IOException {
        LogsProcessingDataAttributes attributes = LogsProcessingDataAttributes.newBuilder()
            .setCategoryId(42)
            .setCategoryName("category_name")
            .setGlobalVendorId(43L)
            .setVendorName("vendor")
            .setVendorUrl("vendor_url")
            .setInstruction("instruction")
            .setOfferUrl("offer_url")
            .setOfferTitle("offer_title")
            .setOfferPictureUrl("offer_picture_url")
            .setOfferParams("offer_params")
            .setOfferDescription("offer_description")
            .setModels(new LogsProcessingDataAttributes.ModelWrapper(
                Arrays.asList(
                    new LogsProcessingDataAttributes.Model(1L, "model1"),
                    new LogsProcessingDataAttributes.Model(2L, "model2"))))
            .setEtalonModels(Arrays.asList("emodel1", "emodel2"))
            .build();

        LogsProcessingDataItemPayload payload = new LogsProcessingDataItemPayload(OFFER_ID, CLUSTER_ID, attributes);
        String json = mapper.writeValueAsString(payload);
        System.out.println(json);

        LogsProcessingDataItemPayload deserialized =
            mapper.readerFor(LogsProcessingDataItemPayload.class).readValue(json);

        LogsProcessingDataIdentity identifier = deserialized.getDataIdentifier();
        LogsProcessingDataAttributes deserializedAttributes = deserialized.getAttributes();

        Assert.assertEquals(new LogsProcessingDataIdentity(OFFER_ID), identifier);
        Assert.assertEquals(CLUSTER_ID, payload.getClusterId());

        Assert.assertEquals(attributes.getCategoryId(), deserializedAttributes.getCategoryId());
        Assert.assertEquals(attributes.getGlobalVendorId(), deserializedAttributes.getGlobalVendorId());
        Assert.assertEquals(attributes.getOfferTitle(), deserializedAttributes.getOfferTitle());
        Assert.assertEquals(attributes.getOfferDescription(), deserializedAttributes.getOfferDescription());
        Assert.assertEquals(attributes.getVendorUrl(), deserializedAttributes.getVendorUrl());
        Assert.assertEquals(attributes.getInstruction(), deserializedAttributes.getInstruction());
        Assert.assertEquals(attributes.getEtalonModels(), deserializedAttributes.getEtalonModels());
        Assert.assertEquals(attributes.getVendorName(), deserializedAttributes.getVendorName());
        Assert.assertEquals(attributes.getCategoryName(), deserializedAttributes.getCategoryName());
        Assert.assertEquals(attributes.getOfferPictureUrl(), deserializedAttributes.getOfferPictureUrl());
        Assert.assertEquals(attributes.getOfferUrl(), deserializedAttributes.getOfferUrl());
        Assert.assertEquals(attributes.getOfferParams(), deserializedAttributes.getOfferParams());
        Assert.assertEquals(attributes.getModels(), deserializedAttributes.getModels());
    }

    @Test
    public void responseSerialization() throws IOException {
        LogsProcessingResponse response = new LogsProcessingResponse(
            42, LogsProcessingResponse.Result.ADD_NAME, "name", "url_shop", "url_vital", "alias_text", 1L);

        String json = mapper.writeValueAsString(response);
        System.out.println(json);

        LogsProcessingResponse deserialized = mapper.readerFor(LogsProcessingResponse.class).readValue(json);

        Assert.assertEquals(response.getId(), deserialized.getId());
        Assert.assertEquals(response.getNewModelId(), deserialized.getNewModelId());
        Assert.assertEquals(response.getAliasText(), deserialized.getAliasText());
        Assert.assertEquals(response.getName(), deserialized.getName());
        Assert.assertEquals(response.getResult(), deserialized.getResult());
        Assert.assertEquals(response.getUrlShop(), deserialized.getUrlShop());
        Assert.assertEquals(response.getUrlVital(), deserialized.getUrlVital());
    }

    @Test
    public void responseDeserialization() throws IOException {
        String json = Markup2TestUtils.getResource("tasks/logs_processing/response.json");
        mapper.readerFor(LogsProcessingResponse.class).readValue(json);
    }
}

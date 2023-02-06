package ru.yandex.market.markup2.tasks.vendor_logs_processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.googlecode.protobuf.format.JsonFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.market.markup2.utils.Markup2TestUtils;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.mbo.http.ModelStorage;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author sergtru
 * @since 25.10.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class VendorLogsProcessingDataItemsProcessorTest {

    private ObjectMapper mapper;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        JsonSerializer serializer = new VendorLogsProcessingDataItemsProcessor().getRequestSerializer();
        mapper = Markup2TestUtils.defaultMapper(
                TaskDataItem.class, serializer, null,
                VendorLogsProcessingIdentity.class,
                VendorLogsProcessingPayload.class,
                VendorLogsProcessingPayload.VendorLogsProcessingAttributes.class
        );
    }

    private VendorLogsProcessingPayload generatePayload() {
        VendorLogsProcessingPayload.VendorLogsProcessingAttributes attributes =
                new VendorLogsProcessingPayload.VendorLogsProcessingAttributes("title", "picture", "url");
        attributes.setCategoryTitle("category");
        attributes.setVendorModels(Arrays.asList(
                new ModelInfo(701, "attach to 1"),
                new ModelInfo(702, "attach to 2")
        ));
        return new VendorLogsProcessingPayload(new VendorLogsProcessingIdentity(10, 20), attributes);
    }

    @Test
    public void requestFieldsTest() throws Exception {
        Set<String> actualFields = new HashSet<>();
        TaskDataItem<VendorLogsProcessingPayload, Object> expectedDataItem =
                new TaskDataItem<>(1, generatePayload());
        JsonNode node = mapper.valueToTree(expectedDataItem);
        node.fieldNames().forEachRemaining(actualFields::add);

        Set<String> expected = CollectionFactory.set("id", "category_id", "category_title", "model_id",
                "pic_url", "title", "link_vendor", "known_models");
        Assert.assertEquals(expected, actualFields);

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if ("known_models".equals(field.getKey())) {
                assertEquals("Field " + field, JsonNodeType.ARRAY, field.getValue().getNodeType());
                assertEquals(2, field.getValue().size());
            } else {
                assertEquals("Field " + field, JsonNodeType.STRING, field.getValue().getNodeType());
            }
        }
    }

    @Test
    public void fillForModelTest() throws Exception {
        final String pictureUrl =
                "http://avatars.mds.yandex.net/get-market_toloka/62651/2a0000015f38f53cc5b3656dd1d07733395d/orig";
        final String vendorUrl = "http://makita-profi.ru/catalog/rubanki-elektricheskie/rubanok-makita-1002ba-P1709/";

        URL resource = Resources.getResource("vendor_model.json");
        ModelStorage.Model.Builder modelBuilder = ModelStorage.Model.newBuilder();
        JsonFormat.merge(Resources.toString(resource, Charsets.UTF_8), modelBuilder);
        VendorLogsProcessingPayload payload = VendorLogsProcessingPayload.forModel(modelBuilder.build(), () -> null);
        assertEquals(91660, payload.getDataIdentifier().getCategoryId());
        assertEquals(100104947768L, payload.getDataIdentifier().getModelId());
        VendorLogsProcessingPayload.VendorLogsProcessingAttributes attributes = payload.getAttributes();
        assertEquals("1002BA", attributes.getTitle());
        assertEquals(pictureUrl, attributes.getPicUrl());
        assertEquals(vendorUrl, attributes.getUrl());
    }

    @Test
    public void whenForModelEmptyPictureUrlThenGetFromSupplier() throws Exception {
        final String skuPictureUrl =
            "http://avatars.mds.yandex.net/get-market_toloka/sku_url";

        URL resource = Resources.getResource("vendor_model_with_absent_picture.json");
        ModelStorage.Model.Builder modelBuilder = ModelStorage.Model.newBuilder();
        JsonFormat.merge(Resources.toString(resource, Charsets.UTF_8), modelBuilder);
        VendorLogsProcessingPayload payload = VendorLogsProcessingPayload.forModel(modelBuilder.build(),
            () -> skuPictureUrl);
        VendorLogsProcessingPayload.VendorLogsProcessingAttributes attributes = payload.getAttributes();
        assertEquals(skuPictureUrl, attributes.getPicUrl());
    }

    @Test
    public void testPayloadSerialization() throws Exception {
        VendorLogsProcessingPayload expected = generatePayload();
        VendorLogsProcessingPayload actual = mapper.readValue(mapper.writeValueAsString(expected), expected.getClass());
        assertEquals(expected.getDataIdentifier().getCategoryId(), actual.getDataIdentifier().getCategoryId());
        assertEquals(expected.getDataIdentifier().getModelId(), actual.getDataIdentifier().getModelId());
        assertEquals(expected.getAttributes().getTitle(), actual.getAttributes().getTitle());
        assertEquals(expected.getAttributes().getPicUrl(), actual.getAttributes().getPicUrl());
        assertEquals(expected.getAttributes().getUrl(), actual.getAttributes().getUrl());
        assertEquals(expected.getAttributes().getCategoryTitle(), actual.getAttributes().getCategoryTitle());
        assertEquals(expected.getAttributes().getVendorModels(), actual.getAttributes().getVendorModels());
    }

    @Test
    public void testIdentitySerialization() throws Exception {
        String expected = "{\"category_id\":17,\"model_id\":31}";
        String actual = mapper.writeValueAsString(mapper.readValue(expected, VendorLogsProcessingIdentity.class));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testModelInfoSerialization() throws Exception {
        String expected = "{\"id\":10,\"title\":\"test_title\"}";
        String actual = mapper.writeValueAsString(mapper.readValue(expected, ModelInfo.class));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testResponse() throws Exception {
        String expected = "{\"req_id\":10,\"status\":\"OK\",\"new_category\":101,\"attach_to_model\":505}";
        String actual = mapper.writeValueAsString(mapper.readValue(expected, VendorLogsProcessingResponse.class));
        Assert.assertEquals(expected, actual);
    }
}

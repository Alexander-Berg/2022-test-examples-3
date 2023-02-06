package ru.yandex.market.markup2.tasks.fill_picture_params;

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
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class FillPictureParamsSerializersTest {

    @Test
    public void testIdentitySerialization() throws IOException {
        ObjectMapper objectMapper = initSerializers(FillPictureParamsTaskIdentity.class,
                new JsonUtils.DefaultJsonSerializer(),
                new JsonUtils.DefaultJsonDeserializer(FillPictureParamsTaskIdentity.class)
        );
        FillPictureParamsTaskIdentity ident = FillPictureParamsTaskIdentity.of(1L, "http://image.url");
        String val = objectMapper.writeValueAsString(ident);
        System.out.println(val);
        FillPictureParamsTaskIdentity identCpy = objectMapper.readValue(val, FillPictureParamsTaskIdentity.class);
        Assert.assertEquals(ident, identCpy);
    }

    @Test
    public void testAttributesSerialization() throws  IOException {
        ObjectMapper objectMapper = initSerializers(FillPictureParamsDataAttributes.class,
            new JsonUtils.DefaultJsonSerializer(),
            new JsonUtils.DefaultJsonDeserializer(FillPictureParamsDataAttributes.class)
        );
        FillPictureParamsDataAttributes attributes = createAttributes();

        String val = objectMapper.writeValueAsString(attributes);
        System.out.println(val);
        FillPictureParamsDataAttributes attributesCpy =
                objectMapper.readValue(val, FillPictureParamsDataAttributes.class);
        Assert.assertEquals(attributes.getCategoryId(), attributesCpy.getCategoryId());
        Assert.assertEquals(attributes.getModelName(), attributesCpy.getModelName());
        Assert.assertEquals(attributes.getVendorName(), attributesCpy.getVendorName());
        Assert.assertEquals(attributes.getVendorId(), attributesCpy.getVendorId());
        Assert.assertEquals(attributes.getVendorColorIds(), attributesCpy.getVendorColorIds());
        Assert.assertEquals(attributes.getVendorColors(), attributesCpy.getVendorColors());
    }

    @Test
    public void testRequestSerialization() throws  IOException {
        FillPictureParamsPayload payload = new FillPictureParamsPayload(
            1L,
            "http://image.url",
            createAttributes()
        );
        TaskDataItem<FillPictureParamsPayload, FillPictureParamsResponse> item =
            new TaskDataItem(100L, payload);

        JsonSerializer<TaskDataItem<FillPictureParamsPayload, FillPictureParamsResponse>> ser =
            new JsonUtils.DefaultHitmanTaskDataJsonSerializer<FillPictureParamsPayload, FillPictureParamsResponse,
                FillPictureParamsDataAttributes>() {
                @Override
                protected FillPictureParamsDataAttributes getHitmanObject(FillPictureParamsPayload payload) {
                    return payload.getAttributes();
                }
        };
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
            Arrays.asList("id", "category_id", "vendor_id", "vendor_name", "model_name", "model_publish_status",
                "vendor_color_ids", "vendor_colors")
        ));
    }

    private FillPictureParamsDataAttributes createAttributes() {
        return FillPictureParamsDataAttributes.newBuilder()
            .setCategoryId(100500)
            .setVendorColors(Arrays.asList(new VendorColor(1L, "Red"), new VendorColor(2L, "Blue")))
            .setVendorColorIds(Arrays.asList(1L, 2L))
            .setModelPublishStatus(false)
            .setModelName("Model1")
            .setVendorName("Vendor")
            .setVendorId(500L)
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
}

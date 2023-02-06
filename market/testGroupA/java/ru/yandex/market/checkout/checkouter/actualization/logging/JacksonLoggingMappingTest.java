package ru.yandex.market.checkout.checkouter.actualization.logging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.actualization.logging.mixins.DeliveryMixin;
import ru.yandex.market.checkout.checkouter.actualization.logging.mixins.FoundOfferMixin;
import ru.yandex.market.checkout.checkouter.actualization.logging.mixins.OrderItemMixin;
import ru.yandex.market.checkout.checkouter.actualization.logging.mixins.OrderMixin;
import ru.yandex.market.checkout.checkouter.config.CheckouterAnnotationJsonConfig;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.jackson.CheckouterModule;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.common.report.model.FoundOffer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class JacksonLoggingMappingTest extends AbstractServicesTestBase {

    private ObjectMapper objectMapperPrototype =
            CheckouterAnnotationJsonConfig.objectMapperPrototype(new SimpleFilterProvider().setFailOnUnknownId(false));

    @Resource(name = "actualizationLoggingMapper")
    private ObjectMapper actualizationLoggingMapper;

    @Resource(name = "checkouterAnnotationObjectMapper")
    private ObjectMapper checkouterAnnotationObjectMapper;

    @Test
    public void objectMapperPrototypeShouldHasMoscowTimezone() {
        assertThat(objectMapperPrototype.getSerializationConfig().getTimeZone().getID(), is("Europe/Moscow"));
        assertThat(objectMapperPrototype.getDeserializationConfig().getTimeZone().getID(), is("Europe/Moscow"));
    }

    @Test
    public void checkouterAnnotationObjectMapperShouldHasMoscowTimezone() {
        assertThat(checkouterAnnotationObjectMapper.getSerializationConfig().getTimeZone().getID(),
                is("Europe/Moscow"));
        assertThat(checkouterAnnotationObjectMapper.getDeserializationConfig().getTimeZone().getID(),
                is("Europe/Moscow"));
    }

    @Test
    public void actualizationLoggingMapperShouldHasMoscowTimezone() {
        assertThat(actualizationLoggingMapper.getSerializationConfig().getTimeZone().getID(),
                is("Europe/Moscow"));
        assertThat(actualizationLoggingMapper.getDeserializationConfig().getTimeZone().getID(),
                is("Europe/Moscow"));
    }

    @Test
    public void objectMapperPrototypeShouldBeDecorated() {
        assertNotEquals(objectMapperPrototype, actualizationLoggingMapper, "objectMapperPrototype must be prototype");

        assertNotEquals(objectMapperPrototype, checkouterAnnotationObjectMapper, "objectMapperPrototype must be " +
                "prototype");
    }

    @Test
    public void checkouterAnnotationObjectMapperShouldHaveCheckouterModule() {
        Assertions.assertTrue(checkouterAnnotationObjectMapper.getRegisteredModuleIds()
                .contains(CheckouterModule.class.getName()),
                "checkouterAnnotationObjectMapper should have CheckouterModule");
    }

    @Test
    public void actualizationLoggingMapperShouldHaveCheckouterModule() {
        Assertions.assertTrue(actualizationLoggingMapper.getRegisteredModuleIds()
                .contains(CheckouterModule.class.getName()),
                "actualizationLoggingMapper should have CheckouterModule");
    }

    @Test
    public void actualizationLoggingMapperShouldOverrideMixins() {
        Assertions.assertEquals(OrderItemMixin.class, actualizationLoggingMapper.findMixInClassFor(OrderItem.class),
                "OrderItem mixin should be overloaded");

        Assertions.assertEquals(OrderMixin.class, actualizationLoggingMapper.findMixInClassFor(Order.class),
                "Order mixin should be overloaded");

        Assertions.assertEquals(DeliveryMixin.class, actualizationLoggingMapper.findMixInClassFor(Delivery.class),
                "Delivery mixin should be overloaded");

        Assertions.assertEquals(FoundOfferMixin.class, actualizationLoggingMapper.findMixInClassFor(FoundOffer.class),
                "FoundOffer mixin should be overloaded");
    }

    @Test
    public void objectMapperPrototypeShouldMapJSONTypes() throws IOException {
        JSONObject jsonObject = generateJSONObject();
        String result = objectMapperPrototype.writeValueAsString(jsonObject);
        objectMapperPrototype.readValue(result, Map.class);
        Assertions.assertTrue(result.startsWith("{"), "JSON Object must starts with {");
        Assertions.assertTrue(result.endsWith("}"), "JSON Object must ends with }");

        assertJSONObject(jsonObject, result);

        JSONArray jsonArray = generateJSONArray();
        result = objectMapperPrototype.writeValueAsString(jsonArray);

        Assertions.assertTrue(result.startsWith("["), "JSON Array must starts with [");
        Assertions.assertTrue(result.endsWith("]"), "JSON Array must ends with ]");

        objectMapperPrototype.readValue(result, List.class);

        for (int i = 0; i < jsonArray.length(); i++) {
            assertJSONObject(jsonArray.getJSONObject(i), result);
        }
    }

    private void assertJSONObject(JSONObject jsonObject, String result) {
        for (String key : jsonObject.keySet()) {
            Assertions.assertTrue(result.contains(jsonObject.get(key).toString()));
        }
    }

    private JSONObject generateJSONObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("someProperty", UUID.randomUUID());
        jsonObject.put("anotherProperty", "i some value, dont push me");
        jsonObject.put("someNullObject", JSONObject.NULL);
        jsonObject.put("someEmptyObject", new JSONObject());
        jsonObject.put("someEmptyArray", new JSONArray());
        jsonObject.put("someInternalObject", new JSONObject(Collections.singletonMap("someInternalField", 123)));
        jsonObject.put("someInternalArray", new JSONArray(Collections.singletonList(123)));
        return jsonObject;
    }

    private JSONArray generateJSONArray() {
        int count = ThreadLocalRandom.current().nextInt(2, 5);
        List<JSONObject> objects = new ArrayList<>();
        while (count-- > 0) {
            objects.add(generateJSONObject());
        }
        return new JSONArray(objects);
    }
}

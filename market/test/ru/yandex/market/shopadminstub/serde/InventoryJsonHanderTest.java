package ru.yandex.market.shopadminstub.serde;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import ru.yandex.common.util.string.SimpleStringConverter;
import ru.yandex.market.checkout.checkouter.order.TaxSystem;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.common.ClassMapping;
import ru.yandex.market.checkout.common.json.JsonDeserializer;
import ru.yandex.market.checkout.common.json.jackson.JacksonJsonReader;
import ru.yandex.market.shopadminstub.model.inventory.Inventory;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

public class InventoryJsonHanderTest {
    @Test
    public void shouldReadInventoryJson() throws IOException {
        String json = "{   " +
                "  \"inventory\":[   " +
                "    {   " +
                "      \"feedId\":383182,   " +
                "      \"offerId\":\"1\",   " +
                "      \"price\":250,   " +
                "      \"count\":3   " +
                "    },   " +
                "    {   " +
                "      \"feedId\":383182,   " +
                "      \"offerId\":\"2\",   " +
                "      \"price\":125,   " +
                "      \"count\":5   " +
                "    }   " +
                "  ],   " +
                "  \"delivery\":[   " +
                "    {   " +
                "      \"id\": 123,   " +
                "      \"type\": \"DELIVERY\",   " +
                "      \"name\": \"Доставка курьером\",   " +
                "      \"price\": 222,   " +
                "      \"dayFrom\": 1,   " +
                "      \"dayTo\": 6   " +
                "    },   " +
                "    {   " +
                "      \"id\": 456,   " +
                "      \"type\": \"PICKUP\",   " +
                "      \"name\": \"Самовывоз\",   " +
                "      \"price\": 0,   " +
                "      \"outlets\" : [\"1\", \"11\", \"21\"],   " +
                "      \"dayFrom\" : 0,   " +
                "      \"dayTo\" : 0   " +
                "    }   " +
                "  ]," +
                "  \"payment_methods\": [\"YANDEX\"] " +
                "}";
        Inventory deserialize = readJson(json);
        assertNotNull(deserialize.getDelivery());
        assertNotNull(deserialize.getInventory());
        assertNotNull(deserialize.getPaymentMethods());
    }

    @Test
    public void shouldReadInventoryJsonWithDeliveryPaymentMethods() throws IOException {
        String json = "{   " +
                "  \"inventory\":[   " +
                "    {   " +
                "      \"feedId\":383182,   " +
                "      \"offerId\":\"1\",   " +
                "      \"price\":250,   " +
                "      \"count\":3   " +
                "    },   " +
                "    {   " +
                "      \"feedId\":383182,   " +
                "      \"offerId\":\"2\",   " +
                "      \"price\":125,   " +
                "      \"count\":5   " +
                "    }   " +
                "  ],   " +
                "  \"delivery\":[   " +
                "    {   " +
                "      \"id\": 123,   " +
                "      \"type\": \"DELIVERY\",   " +
                "      \"name\": \"Доставка курьером\",   " +
                "      \"price\": 222,   " +
                "      \"dayFrom\": 1,   " +
                "      \"dayTo\": 6,   " +
                "      \"taxSystem\": \"OSN\"," +
                "      \"vat\": \"VAT_10\"," +
                "      \"paymentMethods\": [\"YANDEX\"] " +
                "    },   " +
                "    {   " +
                "      \"id\": 456,   " +
                "      \"type\": \"PICKUP\",   " +
                "      \"name\": \"Самовывоз\",   " +
                "      \"price\": 0,   " +
                "      \"outlets\" : [\"1\", \"11\", \"21\"],   " +
                "      \"dayFrom\" : 0,   " +
                "      \"dayTo\" : 0," +
                "      \"paymentMethods\": [\"YANDEX\"] " +
                "    }   " +
                "  ]," +
                "  \"payment_methods\": [\"YANDEX\"] " +
                "}";

        Inventory deserialize = readJson(json);
        assertThat(deserialize.getDelivery(), hasSize(2));
        assertThat(deserialize.getDelivery().get(0).getPaymentMethods(), hasSize(1));
        assertThat(deserialize.getDelivery().get(0).getPaymentMethods(), hasItem(PaymentMethod.YANDEX));
        assertThat(deserialize.getDelivery().get(0).getVat(), equalTo(VatType.VAT_10));
        assertThat(deserialize.getDelivery().get(0).getTaxSystem(), equalTo(TaxSystem.OSN));
    }

    private Inventory readJson(String json) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);
        ClassMapping<JsonDeserializer> deserializers = new ClassMapping<>();
        deserializers.setMapping(ImmutableMap.of(Inventory.class, new InventoryJsonHandler()));
        JacksonJsonReader jacksonJsonReader = new JacksonJsonReader(objectMapper, jsonNode,
                new SimpleStringConverter(), deserializers);
        return new InventoryJsonHandler().deserialize(jacksonJsonReader);
    }
}

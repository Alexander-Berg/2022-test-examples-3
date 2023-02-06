package ru.yandex.market.checkout.checkouter.json;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.CartPresetInfo;
import ru.yandex.market.checkout.checkouter.order.PresetInfo;
import ru.yandex.market.checkout.test.providers.AddressProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class PresetInfoJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        PresetInfo presetInfo = new PresetInfo();
        presetInfo.setPresetId("sd456");
        presetInfo.setType(DeliveryType.PICKUP);
        presetInfo.setRegionId(5000000000L);
        presetInfo.setOutletId(6000000000L);
        presetInfo.setBuyerAddress(AddressProvider.getAddress());
        CartPresetInfo cartPresetInfoPickup = new CartPresetInfo();
        cartPresetInfoPickup.setLabel("pickup_label");
        cartPresetInfoPickup.setDeliveryAvailable(true);
        cartPresetInfoPickup.setTryingAvailable(true);
        presetInfo.setCarts(Collections.singletonList(cartPresetInfoPickup));

        String json = write(presetInfo);
        System.out.println(json);

        checkJson(json, "$." + "presetId", "sd456");
        checkJson(json, "$." + "type", "PICKUP");
        checkJson(json, "$." + "regionId", "5000000000");
        checkJson(json, "$." + "outletId", "6000000000");
        Assertions.assertNotNull(new JSONObject(json).optJSONObject("buyerAddress"));

        JSONArray cartsData = new JSONObject(json).getJSONArray("carts");
        Assertions.assertEquals(cartsData.length(), 1);
        String cartJson = cartsData.get(0).toString();
        checkJson(cartJson, "$." + "label", "pickup_label");
        checkJson(cartJson, "$." + "deliveryAvailable", true);
        checkJson(cartJson, "$." + "isTryingAvailable", true);
    }

    @Test
    public void deserialize() throws Exception {
        String json = "{\n" +
                "  \"presetId\": \"sd456\",\n" +
                "  \"type\": \"PICKUP\",\n" +
                "  \"carts\": [\n" +
                "    {\n" +
                "      \"label\": \"label\",\n" +
                "      \"deliveryAvailable\": true,\n" +
                "      \"isTryingAvailable\": true\n" +
                "    }\n" +
                "  ],\n" +
                "  \"regionId\": 5000000000,\n" +
                "  \"outletId\": 6000000000,\n" +
                "  \"buyerAddress\": {\n" +
                "    \"country\": \"country\",\n" +
                "    \"postcode\": \"postcode\",\n" +
                "    \"city\": \"city\",\n" +
                "    \"subway\": \"subway\",\n" +
                "    \"street\": \"street\",\n" +
                "    \"house\": \"house\",\n" +
                "    \"block\": \"block\",\n" +
                "    \"entrance\": \"entrance\",\n" +
                "    \"entryphone\": \"entryPhone\",\n" +
                "    \"floor\": \"floor\",\n" +
                "    \"apartment\": \"apartment\",\n" +
                "    \"recipient\": \"recipient\",\n" +
                "    \"phone\": \"phone\",\n" +
                "    \"language\": \"ENG\"\n" +
                "  }\n" +
                "}";
        PresetInfo presetInfo = read(PresetInfo.class, json);

        Assertions.assertEquals("sd456", presetInfo.getPresetId());
        Assertions.assertEquals(DeliveryType.PICKUP, presetInfo.getType());
        Assertions.assertEquals(5000000000L, presetInfo.getRegionId());
        Assertions.assertEquals(6000000000L, presetInfo.getOutletId());
        Assertions.assertNotNull(presetInfo.getBuyerAddress());
        assertThat(presetInfo.getCarts(), hasSize(1));

        CartPresetInfo cartPresetInfo = presetInfo.getCarts().get(0);
        Assertions.assertEquals("label", cartPresetInfo.getLabel());
        Assertions.assertEquals(true, cartPresetInfo.isDeliveryAvailable());
        Assertions.assertEquals(true, cartPresetInfo.getTryingAvailable());
    }
}

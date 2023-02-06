package ru.yandex.market.cashier.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author dzvyagin
 */
public class PassParamsTest {

    @Test
    public void testSetParam() {
        PassParams params = new PassParams();
        Assert.assertEquals("UNKNOWN", params.getMarketBlue3dsPolicy());
        params.setParam("market_blue_3ds_policy", "FORCE_3DS_OFF");
        Assert.assertEquals("FORCE_3DS_OFF", params.getMarketBlue3dsPolicy());
        params.setParam("yandexuid", "yandexuid_1");
        Assert.assertEquals("yandexuid_1", params.getYandexuid());
        params.setParam("uuid", "uuid_1");
        Assert.assertEquals("uuid_1", params.getUuid());

        params.setParam("delivery_postcode", "postcode_1");
        Assert.assertEquals("postcode_1", params.getDeliveryPostcode());
        params.setParam("delivery_city", "delivery_city_1");
        Assert.assertEquals("delivery_city_1", params.getDeliveryCity());
        params.setParam("delivery_street", "delivery_street_1");
        Assert.assertEquals("delivery_street_1", params.getDeliveryStreet());
        params.setParam("delivery_house", "delivery_house_1");
        Assert.assertEquals("delivery_house_1", params.getDeliveryHouse());
        params.setParam("delivery_building", "delivery_building_1");
        Assert.assertEquals("delivery_building_1", params.getDeliveryBuilding());
        params.setParam("delivery_apartment", "delivery_apartment_1");
        Assert.assertEquals("delivery_apartment_1", params.getDeliveryApartment());
        params.setParam("delivery_outlet_code", "delivery_outlet_code_1");
        Assert.assertEquals("delivery_outlet_code_1", params.getDeliveryOutletCode());
        params.setParam("experiments", "very_experiment");
        Assert.assertEquals("very_experiment", params.getExperiments());
        params.setParam("demo_flow", "appointment");
        Assert.assertEquals("appointment", params.getDemoFlow());
    }

}

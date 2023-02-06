package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class ShopOutletJsonHandlerTest extends AbstractJsonHandlerTestBase {

    private static final long INLET_ID = 123456L;

    @Test
    public void serialize() throws Exception {
        ShopOutlet shopOutlet = EntityHelper.getShopOutlet();

        String json = write(shopOutlet);
        checkJson(json, "$." + Names.ID, EntityHelper.ID);
        checkJson(json, "$." + Names.ShopOutlet.NAME, EntityHelper.NAME);
        checkJson(json, "$." + Names.REGION_ID, EntityHelper.REGION_ID);
        checkJson(json, "$." + Names.Address.CITY, EntityHelper.CITY);
        checkJson(json, "$." + Names.Address.STREET, EntityHelper.STREET);
        checkJson(json, "$." + Names.ShopOutlet.KM, EntityHelper.KM);
        checkJson(json, "$." + Names.ShopOutlet.HOUSE, EntityHelper.HOUSE);
        checkJson(json, "$." + Names.ShopOutlet.BUILDING, EntityHelper.BUILDING);
        checkJson(json, "$." + Names.ShopOutlet.ESTATE, EntityHelper.ESTATE);
        checkJson(json, "$." + Names.ShopOutlet.BLOCK, EntityHelper.BLOCK);
        checkJson(json, "$." + Names.ShopOutlet.PERSONAL_ADDRESS_ID, EntityHelper.PERSONAL_ADDRESS_ID);
        checkJson(json, "$." + Names.ShopOutlet.GPS, EntityHelper.GPS);
        checkJson(json, "$." + Names.ShopOutlet.PERSONAL_GPS_ID, EntityHelper.PERSONAL_GPS_ID);
        checkJson(json, "$." + Names.ShopOutlet.NOTES, EntityHelper.NOTES);
        checkJson(json, "$." + Names.ShopOutlet.PHONES, JsonPathExpectationsHelper::assertValueIsArray);
        checkJson(json, "$." + Names.ShopOutlet.INLET_ID, JsonPathExpectationsHelper::doesNotExist);
    }

    @Test
    public void deserialize() throws Exception {
        String json = "{" +
                "\"id\": 123," +
                "\"name\": \"name\"," +
                "\"regionId\": 456," +
                "\"city\": \"city\"," +
                "\"street\": \"street\"," +
                "\"km\": \"km\"," +
                "\"house\": \"HOUSE\"," +
                "\"building\": \"building\"," +
                "\"estate\": \"estate\"," +
                "\"block\": \"block\"," +
                "\"personalAddressId\": \"personalAddressId\"," +
                "\"gps\": \"gps\"," +
                "\"personalGpsId\": \"personalGpsId\"," +
                "\"notes\": \"notes\"," +
                "\"phones\": [" + ShopOutletPhoneJsonHandlerTest.JSON + "]," +
                "\"inletId\": 123456" +
                "}";

        ShopOutlet shopOutlet = read(ShopOutlet.class, json);

        Assertions.assertEquals(EntityHelper.ID, shopOutlet.getId().longValue());
        Assertions.assertEquals(EntityHelper.NAME, shopOutlet.getName());
        Assertions.assertEquals(EntityHelper.REGION_ID, shopOutlet.getRegionId());
        Assertions.assertEquals(EntityHelper.CITY, shopOutlet.getCity());
        Assertions.assertEquals(EntityHelper.STREET, shopOutlet.getStreet());
        Assertions.assertEquals(EntityHelper.KM, shopOutlet.getKm());
        Assertions.assertEquals(EntityHelper.HOUSE, shopOutlet.getHouse());
        Assertions.assertEquals(EntityHelper.BUILDING, shopOutlet.getBuilding());
        Assertions.assertEquals(EntityHelper.ESTATE, shopOutlet.getEstate());
        Assertions.assertEquals(EntityHelper.BLOCK, shopOutlet.getBlock());
        Assertions.assertEquals(EntityHelper.PERSONAL_ADDRESS_ID, shopOutlet.getPersonalAddressId());
        Assertions.assertEquals(EntityHelper.GPS, shopOutlet.getGps());
        Assertions.assertEquals(EntityHelper.PERSONAL_GPS_ID, shopOutlet.getPersonalGpsId());
        Assertions.assertEquals(EntityHelper.NOTES, shopOutlet.getNotes());
        assertThat(shopOutlet.getPhones(), hasSize(1));
        Assertions.assertEquals(INLET_ID, shopOutlet.getInletId().longValue());
    }
}

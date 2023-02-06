package steps.shopOutletSteps;

import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import steps.utils.DateUtils;

import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.json.Names;

public class ShopOutletSteps {
    private static final Long ID = 123L;
    private static final long SHOP_ID = 123L;
    private static final String NAME = "name";
    private static final long REGION_ID = 213L;
    private static final String CITY = "Москва";
    private static final String STREET = "Льва Толстого";
    private static final String KM = "101";
    private static final String HOUSE = "23а/1";
    private static final String BUILDING = "4";
    private static final String ESTATE = "5";
    private static final String BLOCK = "422";
    private static final String GPS = "115.7,22.1";
    private static final String NOTES = "notes";
    private static final Long INLET_ID = 123L;
    private static final Integer MIN_DELIVERY_DAYS = 3;
    private static final Integer MAX_DELIVERY_DAYS = 5;
    private static final Integer RANK = 33;
    private static final String POSTCODE = "119021";

    private ShopOutletSteps() {
    }

    public static ShopOutlet getShopOutlet() {
        ShopOutlet shopOutlet = new ShopOutlet();

        shopOutlet.setId(ID);
        shopOutlet.setShopId(SHOP_ID);
        shopOutlet.setName(NAME);
        shopOutlet.setRegionId(REGION_ID);
        shopOutlet.setCity(CITY);
        shopOutlet.setStreet(STREET);
        shopOutlet.setKm(KM);
        shopOutlet.setHouse(HOUSE);
        shopOutlet.setBuilding(BUILDING);
        shopOutlet.setEstate(ESTATE);
        shopOutlet.setBlock(BLOCK);
        shopOutlet.setGps(GPS);
        shopOutlet.setNotes(NOTES);
        shopOutlet.setPhones(PhonesSteps.getPhones());
        shopOutlet.setShipmentDate(DateUtils.getDate());
        shopOutlet.setInletId(INLET_ID);
        shopOutlet.setMinDeliveryDays(MIN_DELIVERY_DAYS);
        shopOutlet.setMaxDeliveryDays(MAX_DELIVERY_DAYS);
        shopOutlet.setRank(RANK);
        shopOutlet.setPostcode(POSTCODE);

        return shopOutlet;
    }

    public static JSONObject getShopOutletJson() throws JSONException {
        JSONObject shopOutletJson = new JSONObject();

        shopOutletJson.put(Names.ID, ID);
        shopOutletJson.put("shopId", SHOP_ID);
        shopOutletJson.put(Names.ShopOutlet.NAME, NAME);
        shopOutletJson.put("regionId", REGION_ID);
        shopOutletJson.put(Names.ShopOutlet.CITY, CITY);
        shopOutletJson.put(Names.ShopOutlet.STREET, STREET);
        shopOutletJson.put(Names.ShopOutlet.KM, KM);
        shopOutletJson.put(Names.ShopOutlet.HOUSE, HOUSE);
        shopOutletJson.put(Names.ShopOutlet.BUILDING, BUILDING);
        shopOutletJson.put(Names.ShopOutlet.ESTATE, ESTATE);
        shopOutletJson.put(Names.ShopOutlet.BLOCK, BLOCK);
        shopOutletJson.put(Names.ShopOutlet.GPS, GPS);
        shopOutletJson.put(Names.ShopOutlet.NOTES, NOTES);
        shopOutletJson.put(Names.ShopOutlet.PHONES, PhonesSteps.getPhonesJsonArray());
        shopOutletJson.put(Names.ShopOutlet.SCHEDULE, ScheduleSteps.getScheduleJsonArray());
        shopOutletJson.put("shipmentDate", DateUtils.getDateString());
        shopOutletJson.put("minDeliveryDays", MIN_DELIVERY_DAYS);
        shopOutletJson.put("maxDeliveryDays", MAX_DELIVERY_DAYS);
        shopOutletJson.put("rank", RANK);
        shopOutletJson.put("inletId", INLET_ID);

        return shopOutletJson;
    }

    public static List<ShopOutlet> getShopOutletsList() {
        return Collections.singletonList(getShopOutlet());
    }

    public static JSONArray getShopOutletJsonArray() throws JSONException {
        JSONArray shopOutletJsonArray = new JSONArray();

        shopOutletJsonArray.put(getShopOutletJson());

        return shopOutletJsonArray;
    }

}

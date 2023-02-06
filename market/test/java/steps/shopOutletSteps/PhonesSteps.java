package steps.shopOutletSteps;

import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletPhone;
import ru.yandex.market.checkout.checkouter.json.Names;

class PhonesSteps {
    private static final String COUNTRY_CODE = "8";
    private static final String CITY_CODE = "495";
    private static final String NUMBER = "1234567890";
    private static final String EXT_NUMBER = "0987654321";

    private PhonesSteps() {
    }

    static List<ShopOutletPhone> getPhones() {
        ShopOutletPhone phone = new ShopOutletPhone();

        phone.setCountryCode(COUNTRY_CODE);
        phone.setCityCode(CITY_CODE);
        phone.setNumber(NUMBER);
        phone.setExtNumber(EXT_NUMBER);

        return Collections.singletonList(phone);
    }

    static JSONArray getPhonesJsonArray() throws JSONException {
        JSONArray phonesJsonArray = new JSONArray();
        JSONObject phoneJson = new JSONObject();

        phoneJson.put(Names.ShopOutlet.Phone.COUNTRY_CODE, COUNTRY_CODE);
        phoneJson.put(Names.ShopOutlet.Phone.CITY_CODE, CITY_CODE);
        phoneJson.put(Names.ShopOutlet.Phone.NUMBER, NUMBER);
        phoneJson.put(Names.ShopOutlet.Phone.EXT_NUMBER, EXT_NUMBER);

        phonesJsonArray.put(phoneJson);
        return phonesJsonArray;
    }
}

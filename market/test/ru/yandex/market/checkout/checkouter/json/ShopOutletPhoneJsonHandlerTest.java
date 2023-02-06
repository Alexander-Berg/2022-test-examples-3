package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletPhone;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;

public class ShopOutletPhoneJsonHandlerTest extends AbstractJsonHandlerTestBase {

    public static final String JSON = "{" +
            "\"countryCode\": \"+7\", " +
            "\"cityCode\": \"495\", " +
            "\"number\": \"2234562\", " +
            "\"extNumber\": \"albatros\"" +
            "}";

    @Test
    public void serialize() throws Exception {
        ShopOutletPhone shopOutletPhone = EntityHelper.getShopOutletPhone();

        String json = write(shopOutletPhone);

        checkJson(json, "$." + Names.ShopOutlet.Phone.COUNTRY_CODE, "+7");
        checkJson(json, "$." + Names.ShopOutlet.Phone.CITY_CODE, "495");
        checkJson(json, "$." + Names.ShopOutlet.Phone.NUMBER, "2234562");
        checkJson(json, "$." + Names.ShopOutlet.Phone.EXT_NUMBER, "albatros");
    }

    @Test
    public void deserialize() throws Exception {
        ShopOutletPhone shopOutletPhone = read(ShopOutletPhone.class, JSON);

        Assertions.assertEquals("+7", shopOutletPhone.getCountryCode());
        Assertions.assertEquals("495", shopOutletPhone.getCityCode());
        Assertions.assertEquals("2234562", shopOutletPhone.getNumber());
        Assertions.assertEquals("albatros", shopOutletPhone.getExtNumber());
    }

}
